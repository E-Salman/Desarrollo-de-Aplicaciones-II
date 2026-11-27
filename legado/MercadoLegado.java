import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simulador del sistema legado "Mercado/ALyC": un servicio SOAP 1.1 que ejecuta órdenes.
 * Es un programa aparte de InversorAR (otro proceso y otro puerto), sin dependencias ni build:
 *
 *   powershell -ExecutionPolicy Bypass -File legado\iniciar-mercado.ps1     (o: java legado\MercadoLegado.java)
 *
 * Opciones: --puerto=8090  --modo=normal|fault|error|lento|xxe  --suspendidos=TICKER1,TICKER2
 * El modo se cambia en vivo desde el navegador: http://localhost:8090/modo/fault (o normal, error, lento, xxe).
 * Ctrl+C lo apaga: así se demuestra el timeout, los reintentos y la DLQ de InversorAR.
 */
public class MercadoLegado {
    static final String SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String NS = "urn:inversorar:mercado";
    static final Set<String> MODOS = Set.of("normal", "fault", "error", "lento", "xxe");

    static volatile String modo = "normal";
    static Set<String> suspendidos = Set.of();
    /** Idempotencia: si InversorAR reenvía la misma orden (por un reintento), devuelve la misma respuesta. */
    static final Map<String, String> respondidas = new ConcurrentHashMap<>();
    static final AtomicLong numerador = new AtomicLong(1000);

    public static void main(String[] args) throws IOException {
        int puerto = 8090;
        for (String a : args) {
            if (a.startsWith("--puerto=")) puerto = Integer.parseInt(a.substring(9));
            else if (a.startsWith("--modo=")) cambiarModo(a.substring(7));
            else if (a.startsWith("--suspendidos=")) suspendidos = Arrays.stream(a.substring(14).split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase).collect(Collectors.toSet());
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", puerto), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/mercado", ex -> cerrando(ex, MercadoLegado::mercado));
        server.createContext("/modo", ex -> cerrando(ex, MercadoLegado::modo));
        server.createContext("/", ex -> cerrando(ex, MercadoLegado::estado));
        server.start();
        log("Mercado legado (SOAP) escuchando en http://localhost:" + puerto + "/mercado   WSDL: /mercado?wsdl");
        log("Modo: " + modo + (suspendidos.isEmpty() ? "" : "   Suspendidos: " + suspendidos)
                + "   Cambiar en vivo: http://localhost:" + puerto + "/modo/{normal|fault|error|lento|xxe}   Ctrl+C para apagar");
    }

    interface Manejador { void manejar(HttpExchange ex) throws IOException; }
    /** Cierra siempre el intercambio, aunque el manejador falle (compatible con JDK 17). */
    static void cerrando(HttpExchange ex, Manejador m) throws IOException {
        try { m.manejar(ex); }
        catch (RuntimeException e) { log("Error atendiendo el pedido: " + e); throw e; }
        finally { ex.close(); }
    }

    // ---- Endpoint SOAP ----

    static void mercado(HttpExchange ex) throws IOException {
        {
            if ("GET".equals(ex.getRequestMethod()) && "wsdl".equalsIgnoreCase(ex.getRequestURI().getQuery())) {
                responder(ex, 200, "text/xml; charset=utf-8", WSDL); return;
            }
            if (!"POST".equals(ex.getRequestMethod())) { responder(ex, 405, "text/plain; charset=utf-8", "Usar POST con un sobre SOAP"); return; }
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Element pedido;
            try { pedido = leerPedido(cuerpo); }
            catch (Exception e) {
                log("Pedido rechazado (XML inválido o con DOCTYPE): " + e.getMessage());
                responder(ex, 500, "text/xml; charset=utf-8", fault("soap:Client", "XML inválido: " + e.getMessage())); return;
            }
            String ordenId = texto(pedido, "ordenId"), tipo = texto(pedido, "tipo"), ticker = texto(pedido, "ticker");
            String cantidad = texto(pedido, "cantidad"), precio = texto(pedido, "precio");
            log("ejecutarOrden #" + ordenId + ": " + tipo + " " + cantidad + " " + ticker + " a " + precio + "  [modo " + modo + "]");

            switch (modo) {
                case "lento" -> { log("  ...modo lento: demoro 10 s en responder"); dormir(10_000); }
                case "xxe" -> {
                    log("  ...modo xxe: respondo un XML con DOCTYPE que intenta leer C:\\Windows\\win.ini");
                    responder(ex, 200, "text/xml; charset=utf-8", XXE); return;
                }
                case "error" -> {
                    log("  -> Fault soap:Server (falla interna; InversorAR debe reintentar)");
                    responder(ex, 500, "text/xml; charset=utf-8", fault("soap:Server", "Error interno del sistema de compensación")); return;
                }
                case "fault" -> {
                    log("  -> Fault soap:Client (mercado cerrado)");
                    responder(ex, 500, "text/xml; charset=utf-8", fault("soap:Client", "Mercado cerrado: la orden llegó fuera del horario de negociación.")); return;
                }
                default -> { }
            }
            String previa = ordenId == null ? null : respondidas.get(ordenId);
            if (previa != null) { log("  -> orden ya procesada, devuelvo la misma respuesta (idempotencia)"); responder(ex, 200, "text/xml; charset=utf-8", previa); return; }

            String motivo = validar(ticker, cantidad, precio);
            if (motivo != null) {
                log("  -> Fault soap:Client: " + motivo);
                responder(ex, 500, "text/xml; charset=utf-8", fault("soap:Client", motivo)); return;
            }
            String numero = "MKT-" + numerador.incrementAndGet();
            String ok = sobre("<m:ejecutarOrdenResponse xmlns:m=\"" + NS + "\"><m:estado>EJECUTADA</m:estado><m:numeroOperacion>"
                    + numero + "</m:numeroOperacion></m:ejecutarOrdenResponse>");
            if (ordenId != null) respondidas.put(ordenId, ok);
            log("  -> EJECUTADA como " + numero);
            responder(ex, 200, "text/xml; charset=utf-8", ok);
        }
    }

    static String validar(String ticker, String cantidad, String precio) {
        if (ticker == null || ticker.isBlank()) return "Falta el instrumento.";
        if (suspendidos.contains(ticker.toUpperCase())) return "Instrumento " + ticker + " suspendido por el mercado.";
        try {
            if (new BigDecimal(cantidad).signum() <= 0) return "La cantidad debe ser mayor a cero.";
            if (new BigDecimal(precio).signum() <= 0) return "El precio debe ser mayor a cero.";
        } catch (RuntimeException e) { return "Cantidad o precio con formato inválido."; }
        return null;
    }

    /** Lector de XML endurecido contra XXE: rechaza cualquier DOCTYPE y no resuelve entidades externas. */
    static Element leerPedido(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        var nodos = doc.getElementsByTagNameNS(NS, "ejecutarOrden");
        if (nodos.getLength() == 0) throw new IllegalArgumentException("falta ejecutarOrden en el Body");
        return (Element) nodos.item(0);
    }

    static String texto(Element padre, String nombre) {
        var n = padre.getElementsByTagNameNS(NS, nombre);
        return n.getLength() == 0 ? null : n.item(0).getTextContent().trim();
    }

    // ---- Control de la demo ----

    static void modo(HttpExchange ex) throws IOException {
        {
            String pedido = ex.getRequestURI().getPath().replaceFirst("^/modo/?", "");
            if (!pedido.isEmpty() && !cambiarModo(pedido)) { responder(ex, 400, "text/plain; charset=utf-8", "Modo desconocido. Opciones: " + MODOS); return; }
            if (!pedido.isEmpty()) log("Modo cambiado a: " + modo);
            responder(ex, 200, "text/plain; charset=utf-8", "Modo actual: " + modo);
        }
    }

    static void estado(HttpExchange ex) throws IOException {
        {
            responder(ex, 200, "text/plain; charset=utf-8", "Mercado legado InversorAR (simulador SOAP)\nModo: " + modo
                    + "\nSuspendidos: " + suspendidos + "\nÓrdenes procesadas: " + respondidas.size()
                    + "\nEndpoint: POST /mercado   WSDL: GET /mercado?wsdl\nCambiar modo: /modo/{normal|fault|error|lento|xxe}\n");
        }
    }

    static boolean cambiarModo(String nuevo) {
        if (!MODOS.contains(nuevo)) return false;
        modo = nuevo; return true;
    }

    // ---- Utilidades ----

    static String sobre(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"" + SOAP + "\"><soap:Body>" + body + "</soap:Body></soap:Envelope>";
    }
    static String fault(String codigo, String mensaje) {
        return sobre("<soap:Fault><faultcode>" + codigo + "</faultcode><faultstring>" + escapar(mensaje) + "</faultstring></soap:Fault>");
    }
    static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    static void responder(HttpExchange ex, int estado, String tipo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(estado, bytes.length);
        ex.getResponseBody().write(bytes);
    }
    static void dormir(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    static void log(String s) { System.out.println("[" + LocalTime.now().withNano(0) + "] " + s); }

    /** Respuesta maliciosa del modo xxe: si el cliente resolviera la entidad, el contenido del archivo viajaría en la respuesta. */
    static final String XXE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE soap:Envelope [<!ENTITY robo SYSTEM \"file:///C:/Windows/win.ini\">]>"
            + "<soap:Envelope xmlns:soap=\"" + SOAP + "\"><soap:Body><m:ejecutarOrdenResponse xmlns:m=\"" + NS + "\">"
            + "<m:estado>EJECUTADA</m:estado><m:numeroOperacion>&robo;</m:numeroOperacion></m:ejecutarOrdenResponse></soap:Body></soap:Envelope>";

    static final String WSDL = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://schemas.xmlsoap.org/wsdl/" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="urn:inversorar:mercado"
                     targetNamespace="urn:inversorar:mercado" name="MercadoLegado">
          <types>
            <xsd:schema targetNamespace="urn:inversorar:mercado" elementFormDefault="qualified">
              <xsd:element name="ejecutarOrden"><xsd:complexType><xsd:sequence>
                <xsd:element name="ordenId" type="xsd:long"/>
                <xsd:element name="tipo" type="xsd:string"/>
                <xsd:element name="ticker" type="xsd:string"/>
                <xsd:element name="cantidad" type="xsd:decimal"/>
                <xsd:element name="precio" type="xsd:decimal"/>
              </xsd:sequence></xsd:complexType></xsd:element>
              <xsd:element name="ejecutarOrdenResponse"><xsd:complexType><xsd:sequence>
                <xsd:element name="estado" type="xsd:string"/>
                <xsd:element name="numeroOperacion" type="xsd:string"/>
              </xsd:sequence></xsd:complexType></xsd:element>
            </xsd:schema>
          </types>
          <message name="ejecutarOrdenRequest"><part name="parameters" element="tns:ejecutarOrden"/></message>
          <message name="ejecutarOrdenResponse"><part name="parameters" element="tns:ejecutarOrdenResponse"/></message>
          <portType name="MercadoPort">
            <operation name="ejecutarOrden"><input message="tns:ejecutarOrdenRequest"/><output message="tns:ejecutarOrdenResponse"/></operation>
          </portType>
          <binding name="MercadoBinding" type="tns:MercadoPort">
            <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
            <operation name="ejecutarOrden"><soap:operation soapAction="urn:inversorar:mercado#ejecutarOrden"/>
              <input><soap:body use="literal"/></input><output><soap:body use="literal"/></output></operation>
          </binding>
          <service name="MercadoLegado">
            <port name="MercadoPort" binding="tns:MercadoBinding"><soap:address location="http://localhost:8090/mercado"/></port>
          </service>
        </definitions>
        """;
}
