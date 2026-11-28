package ar.edu.uade.inversorar.ia;

import ar.edu.uade.inversorar.integracion.XmlSeguro;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Noticias de los últimos 7 días desde el RSS público de Google News (sin clave), en español de Argentina.
 * El XML se lee con {@link XmlSeguro} (sin DOCTYPE: protegido contra XXE). Solo se aceptan enlaces https.
 * Resultados en caché {@link #VIGENCIA}; tras una falla, la fuente se saltea {@link #PAUSA_TRAS_FALLA}.
 */
public class NoticiasGoogle implements FuenteNoticias {
    private static final Logger LOG = Logger.getLogger(NoticiasGoogle.class.getName());
    public static final String PROPIEDAD_URL = "inversorar.noticias.url";
    public static final String URL_POR_DEFECTO = "https://news.google.com";
    static final int MAXIMO = 5;
    static final Duration VIGENCIA = Duration.ofMinutes(10);
    static final Duration PAUSA_TRAS_FALLA = Duration.ofSeconds(60);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final String base;
    private final Clock reloj;
    private final HttpClient http;
    private final Map<String, Guardadas> cache = new ConcurrentHashMap<>();
    private volatile Instant pausadoHasta = Instant.MIN;

    public NoticiasGoogle() { this(System.getProperty(PROPIEDAD_URL, URL_POR_DEFECTO), Clock.systemUTC()); }

    NoticiasGoogle(String base, Clock reloj) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.reloj = reloj;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public List<Noticia> buscar(String tema, Duration plazo) throws ServicioNoDisponible {
        String clave = tema.trim().toLowerCase(java.util.Locale.ROOT);
        Instant ahora = reloj.instant();
        Guardadas guardadas = cache.get(clave);
        if (guardadas != null && guardadas.obtenidas.plus(VIGENCIA).isAfter(ahora)) return guardadas.noticias;
        if (pausadoHasta.isAfter(ahora)) throw new ServicioNoDisponible("Fuente de noticias en pausa tras una falla reciente");
        if (plazo.isNegative() || plazo.isZero()) throw new ServicioNoDisponible("Sin tiempo para buscar noticias");

        String consulta = URLEncoder.encode("\"" + tema.trim() + "\" when:7d", StandardCharsets.UTF_8);
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(base + "/rss/search?q=" + consulta + "&hl=es-419&gl=AR&ceid=AR:es-419"))
                .timeout(plazo).header("Accept", "application/rss+xml, application/xml").GET().build();
        var futuro = http.sendAsync(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            HttpResponse<String> r = futuro.get(plazo.toMillis(), TimeUnit.MILLISECONDS);
            if (r.statusCode() != 200) throw pausar("HTTP " + r.statusCode(), ahora);
            List<Noticia> noticias = leer(r.body());
            cache.put(clave, new Guardadas(noticias, ahora));
            if (cache.size() > 500) cache.entrySet().removeIf(e -> e.getValue().obtenidas.plus(VIGENCIA).isBefore(ahora));
            return noticias;
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw pausar("sin respuesta en " + plazo.toMillis() + " ms", ahora);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponible("Búsqueda interrumpida");
        } catch (XmlSeguro.XmlInvalido e) {
            throw pausar("RSS inválido", ahora);
        } catch (ServicioNoDisponible e) {
            throw e;
        } catch (Exception e) {
            throw pausar("error de conexión (" + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()) + ")", ahora);
        }
    }

    /** Lee los primeros {@value #MAXIMO} ítems del RSS. */
    static List<Noticia> leer(String xml) throws XmlSeguro.XmlInvalido {
        NodeList items = XmlSeguro.leer(xml).getElementsByTagName("item");
        List<Noticia> noticias = new ArrayList<>();
        for (int i = 0; i < items.getLength() && noticias.size() < MAXIMO; i++) {
            Element item = (Element) items.item(i);
            String url = hijo(item, "link");
            if (!url.startsWith("https://")) continue; // nada de javascript:, http: ni rutas raras
            String fuente = hijo(item, "source");
            String titulo = hijo(item, "title");
            if (!fuente.isEmpty() && titulo.endsWith(" - " + fuente)) titulo = titulo.substring(0, titulo.length() - fuente.length() - 3);
            if (titulo.isEmpty()) continue;
            noticias.add(new Noticia(recortar(titulo, 250), recortar(fuente, 80), fecha(hijo(item, "pubDate")), url));
        }
        return List.copyOf(noticias);
    }

    private static String hijo(Element item, String nombre) {
        NodeList nodos = item.getElementsByTagName(nombre);
        return nodos.getLength() == 0 ? "" : ReglasAsistente.sinControl(nodos.item(0).getTextContent()).trim();
    }

    static String fecha(String rfc1123) {
        try {
            return ZonedDateTime.parse(rfc1123, DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ARGENTINA).format(FECHA);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private static String recortar(String texto, int maximo) { return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…"; }

    private ServicioNoDisponible pausar(String motivo, Instant ahora) {
        pausadoHasta = ahora.plus(PAUSA_TRAS_FALLA);
        LOG.warning("Fuente de noticias no disponible (" + motivo + "); se saltea durante " + PAUSA_TRAS_FALLA.toSeconds() + " s");
        return new ServicioNoDisponible("Noticias no disponibles: " + motivo);
    }

    private record Guardadas(List<Noticia> noticias, Instant obtenidas) { }
}
