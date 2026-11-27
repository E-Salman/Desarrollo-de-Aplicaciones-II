package ar.edu.uade.inversorar.integracion;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MercadoLegadoClienteTest {
    static final String SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    ServidorFalso mercado;
    MercadoLegadoCliente cliente;

    @BeforeEach void levantar() throws Exception {
        mercado = new ServidorFalso();
        cliente = new MercadoLegadoCliente(URI.create(mercado.url("/mercado")), Duration.ofMillis(400));
    }
    @AfterEach void apagar() { mercado.close(); }

    static String sobre(String body) {
        return "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"" + SOAP + "\"><soap:Body>" + body + "</soap:Body></soap:Envelope>";
    }
    static String ok(String numero) {
        return sobre("<m:ejecutarOrdenResponse xmlns:m=\"urn:inversorar:mercado\"><m:estado>EJECUTADA</m:estado><m:numeroOperacion>"
                + numero + "</m:numeroOperacion></m:ejecutarOrdenResponse>");
    }
    static String fault(String codigo, String motivo) {
        return sobre("<soap:Fault><faultcode>" + codigo + "</faultcode><faultstring>" + motivo + "</faultstring></soap:Fault>");
    }
    MercadoLegadoCliente.RespuestaMercado ejecutar() {
        return cliente.ejecutar(42L, "COMPRA", "BTCUSDT", new BigDecimal("0.002"), new BigDecimal("97350.12"));
    }

    @Test void elTimeoutPorDefectoEsDeCincoSegundos() {
        assertEquals(Duration.ofSeconds(5), new MercadoLegadoCliente().timeout());
    }
    @Test void unaRespuestaEjecutadaAceptaLaOrden() {
        mercado.cuerpo = ok("MKT-1001");
        var r = ejecutar();
        assertTrue(r.aceptada()); assertEquals("MKT-1001", r.numeroOperacion());
    }
    @Test void elPedidoEsUnSobreSoapConLosDatosDeLaOrden() {
        mercado.cuerpo = ok("MKT-1");
        ejecutar();
        String pedido = mercado.ultimoPedido;
        assertTrue(pedido.contains("<soap:Envelope xmlns:soap=\"" + SOAP + "\">"), pedido);
        for (String dato : new String[]{"<m:ordenId>42</m:ordenId>", "<m:tipo>COMPRA</m:tipo>", "<m:ticker>BTCUSDT</m:ticker>",
                "<m:cantidad>0.002</m:cantidad>", "<m:precio>97350.12</m:precio>"})
            assertTrue(pedido.contains(dato), "falta " + dato);
    }
    @Test void losValoresSeEscapanAntesDeArmarElXml() {
        mercado.cuerpo = ok("MKT-1");
        cliente.ejecutar(1L, "COMPRA", "A<B>&\"C", BigDecimal.ONE, BigDecimal.ONE);
        assertTrue(mercado.ultimoPedido.contains("<m:ticker>A&lt;B&gt;&amp;&quot;C</m:ticker>"), mercado.ultimoPedido);
    }
    @Test void unFaultClientConHttp500EsUnRechazoConElMotivoDelMercado() {
        mercado.estado = 500; mercado.cuerpo = fault("soap:Client", "Instrumento BTCUSDT suspendido por el mercado.");
        var r = ejecutar();
        assertFalse(r.aceptada()); assertEquals("Instrumento BTCUSDT suspendido por el mercado.", r.motivo());
    }
    @Test void unFaultServerEsUnaFallaTecnicaQueSeReintenta() {
        mercado.estado = 500; mercado.cuerpo = fault("soap:Server", "Error interno");
        var e = assertThrows(MercadoNoDisponible.class, this::ejecutar);
        assertTrue(e.getMessage().contains("Error interno"));
    }
    @Test void siNoRespondeATiempoCortaYLanza() {
        mercado.demoraMs = 3_000; mercado.cuerpo = ok("MKT-1");
        long inicio = System.nanoTime();
        var e = assertThrows(MercadoNoDisponible.class, this::ejecutar);
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(ms < 2_000, "cortó a los " + ms + " ms"); assertTrue(e.getMessage().contains("no respondió"));
    }
    @Test void siElMercadoEstaApagadoLanza() throws Exception {
        int libre; try (var s = new ServerSocket(0)) { libre = s.getLocalPort(); }
        var apagado = new MercadoLegadoCliente(URI.create("http://localhost:" + libre + "/mercado"), Duration.ofMillis(400));
        assertThrows(MercadoNoDisponible.class, () -> apagado.ejecutar(1L, "COMPRA", "BTCUSDT", BigDecimal.ONE, BigDecimal.ONE));
    }
    @Test void unaRespuestaQueNoEsSoapNoCompletaLaOrden() {
        mercado.cuerpo = "<html>Bad gateway</html>";
        assertThrows(MercadoNoDisponible.class, this::ejecutar);
        mercado.cuerpo = sobre("<m:ejecutarOrdenResponse xmlns:m=\"urn:inversorar:mercado\"><m:estado>PENDIENTE</m:estado></m:ejecutarOrdenResponse>");
        assertThrows(MercadoNoDisponible.class, this::ejecutar);
        mercado.estado = 404; mercado.cuerpo = ok("MKT-1");
        assertThrows(MercadoNoDisponible.class, this::ejecutar);
    }

    // ---- XXE ----

    @Test void unaRespuestaConEntidadExternaSeRechazaSinLeerElArchivo(@TempDir Path dir) throws Exception {
        Path secreto = Files.writeString(dir.resolve("secreto.txt"), "CLAVE-SUPER-SECRETA");
        mercado.cuerpo = "<?xml version=\"1.0\"?><!DOCTYPE soap:Envelope [<!ENTITY robo SYSTEM \"" + secreto.toUri() + "\">]>"
                + ok("&robo;").substring("<?xml version=\"1.0\"?>".length());
        var e = assertThrows(MercadoNoDisponible.class, this::ejecutar);
        assertTrue(e.getMessage().contains("DOCTYPE"), e.getMessage());
        assertFalse(String.valueOf(e.getMessage()).contains("CLAVE-SUPER-SECRETA"));
    }
    @Test void elLectorSeguroRechazaCualquierDoctype() {
        assertThrows(XmlSeguro.XmlInvalido.class, () -> XmlSeguro.leer(
                "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x \"y\"><!ENTITY x2 \"&x;&x;&x;\">]><a>&x2;</a>"));
        assertThrows(XmlSeguro.XmlInvalido.class, () -> XmlSeguro.leer("<!DOCTYPE a SYSTEM \"http://localhost:1/x.dtd\"><a/>"));
    }
    @Test void elLectorSeguroLeeUnXmlNormal() throws Exception {
        assertEquals("hola", XmlSeguro.leer("<a><b>hola</b></a>").getDocumentElement().getTextContent());
    }
}
