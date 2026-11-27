package ar.edu.uade.inversorar.integracion;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Cliente SOAP 1.1 del sistema legado Mercado/ALyC (operación ejecutarOrden, contrato en GET /mercado?wsdl).
 * <ul>
 *   <li>Respuesta EJECUTADA → {@link RespuestaMercado#aceptada}.</li>
 *   <li>Fault soap:Client (el mercado rechaza la orden) → {@link RespuestaMercado#rechazada} con su motivo.</li>
 *   <li>Timeout de 5 s, conexión rechazada, Fault soap:Server, XML inválido o con DOCTYPE (XXE)
 *       → {@link MercadoNoDisponible}: el broker reintenta y, agotados los reintentos, la orden queda ERROR.</li>
 * </ul>
 * La URL se configura con la propiedad del sistema {@value #PROPIEDAD_URL} (por defecto {@value #URL_POR_DEFECTO}).
 * No exige rol: lo usa el Ejecutor desde el MDB, que corre sin usuario autenticado.
 */
@ApplicationScoped
public class MercadoLegadoCliente {
    private static final Logger LOG = Logger.getLogger(MercadoLegadoCliente.class.getName());
    public static final String PROPIEDAD_URL = "inversorar.mercado.url";
    public static final String URL_POR_DEFECTO = "http://localhost:8090/mercado";
    public static final Duration TIMEOUT = Duration.ofSeconds(5);
    static final String SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String NS = "urn:inversorar:mercado";

    private final URI url;
    private final Duration timeout;
    private final HttpClient http;

    public MercadoLegadoCliente() { this(URI.create(System.getProperty(PROPIEDAD_URL, URL_POR_DEFECTO)), TIMEOUT); }

    MercadoLegadoCliente(URI url, Duration timeout) {
        this.url = url; this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).version(HttpClient.Version.HTTP_1_1).build();
    }

    Duration timeout() { return timeout; }

    public RespuestaMercado ejecutar(long ordenId, String tipo, String ticker, BigDecimal cantidad, BigDecimal precio) {
        String sobre = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"" + SOAP + "\"><soap:Body><m:ejecutarOrden xmlns:m=\"" + NS + "\">"
                + "<m:ordenId>" + ordenId + "</m:ordenId>"
                + "<m:tipo>" + XmlSeguro.escapar(tipo) + "</m:tipo>"
                + "<m:ticker>" + XmlSeguro.escapar(ticker) + "</m:ticker>"
                + "<m:cantidad>" + cantidad.toPlainString() + "</m:cantidad>"
                + "<m:precio>" + precio.toPlainString() + "</m:precio>"
                + "</m:ejecutarOrden></soap:Body></soap:Envelope>";
        HttpRequest pedido = HttpRequest.newBuilder(url).timeout(timeout)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"" + NS + "#ejecutarOrden\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8)).build();
        return interpretar(enviar(pedido, ordenId));
    }

    /** El timeout cubre el pedido completo (conexión, encabezados y cuerpo), no solo la espera de encabezados. */
    private HttpResponse<String> enviar(HttpRequest pedido, long ordenId) {
        CompletableFuture<HttpResponse<String>> futuro = http.sendAsync(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            return futuro.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw falla("Orden #" + ordenId + ": el mercado legado no respondió en " + timeout.toSeconds() + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw falla("Orden #" + ordenId + ": se interrumpió la espera del mercado legado", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof java.net.http.HttpTimeoutException t && !(t instanceof java.net.http.HttpConnectTimeoutException))
                throw falla("Orden #" + ordenId + ": el mercado legado no respondió en " + timeout.toSeconds() + " s", t);
            Throwable causa = e.getCause() instanceof IOException ? e.getCause() : e;
            throw falla("Orden #" + ordenId + ": no se pudo conectar con el mercado legado en " + url + " (" + causa + ")", causa);
        }
    }

    private RespuestaMercado interpretar(HttpResponse<String> respuesta) {
        // SOAP 1.1 devuelve los Fault con HTTP 500: hay que leer el cuerpo antes de tratarlo como error de transporte.
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 500)
            throw falla("El mercado legado respondió HTTP " + respuesta.statusCode(), null);
        Document doc;
        try { doc = XmlSeguro.leer(respuesta.body()); }
        catch (XmlSeguro.XmlInvalido e) { throw falla("Respuesta inválida del mercado legado (" + e.getMessage() + ")", e); }

        Element body = primero(doc.getElementsByTagNameNS(SOAP, "Body"));
        if (body == null) throw falla("La respuesta del mercado legado no es un sobre SOAP", null);
        Element fault = primero(body.getElementsByTagNameNS(SOAP, "Fault"));
        if (fault != null) {
            String codigo = textoHijo(fault, "faultcode"), motivo = textoHijo(fault, "faultstring");
            String local = codigo == null ? "" : codigo.substring(codigo.indexOf(':') + 1);
            if ("Client".equals(local)) {
                LOG.info("El mercado legado rechazó la orden: " + motivo);
                return RespuestaMercado.rechazada(motivo == null || motivo.isBlank() ? "Rechazada por el mercado (sin motivo)." : motivo);
            }
            throw falla("Falla interna del mercado legado (" + codigo + ": " + motivo + ")", null);
        }
        Element ok = primero(body.getElementsByTagNameNS(NS, "ejecutarOrdenResponse"));
        String estado = ok == null ? null : textoHijo(ok, "estado");
        String numero = ok == null ? null : textoHijo(ok, "numeroOperacion");
        if (!"EJECUTADA".equals(estado) || numero == null || numero.isBlank())
            throw falla("Respuesta inesperada del mercado legado (estado " + estado + ")", null);
        return RespuestaMercado.aceptada(numero);
    }

    private static MercadoNoDisponible falla(String mensaje, Throwable causa) {
        LOG.warning(mensaje);
        return new MercadoNoDisponible(mensaje, causa);
    }

    private static Element primero(NodeList nodos) { return nodos.getLength() == 0 ? null : (Element) nodos.item(0); }

    /** Los hijos de un Fault (faultcode, faultstring) van sin namespace; los de la respuesta, con el del servicio. */
    private static String textoHijo(Element padre, String nombre) {
        for (var n = padre.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element e && nombre.equals(e.getLocalName())) return e.getTextContent().trim();
        return null;
    }

    public record RespuestaMercado(boolean aceptada, String numeroOperacion, String motivo) {
        public static RespuestaMercado aceptada(String numeroOperacion) { return new RespuestaMercado(true, numeroOperacion, null); }
        public static RespuestaMercado rechazada(String motivo) { return new RespuestaMercado(false, null, motivo); }
    }
}
