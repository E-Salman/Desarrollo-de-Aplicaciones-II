package ar.edu.uade.inversorar.integracion;

import ar.edu.uade.inversorar.business.ConfiguracionApp;
import ar.edu.uade.inversorar.business.CotizacionCatalogo;
import ar.edu.uade.inversorar.business.CotizacionStrategy;
import ar.edu.uade.inversorar.data.Instrumento;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.json.Json;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Strategy de cotización REST contra el sistema moderno: la API pública de Binance
 * (GET /api/v3/ticker/price?symbol=BTCUSDT, sin clave). Reemplaza a {@link CotizacionCatalogo} como
 * {@link CotizacionStrategy} activa y la usa como fallback: si Binance no responde en 5 s, falla o no lista el
 * símbolo, el precio es el último cierre guardado en MySQL.
 * <p>
 * Resiliencia: precios en caché 15 s; tras una falla de red o un timeout, Binance se saltea 30 s (circuito abierto)
 * para que un portfolio con N posiciones no espere N timeouts. Solo actúa con el catálogo MySQL, cuyos tickers son
 * pares de Binance; en el demo H2 delega en el catálogo. La URL base se cambia con {@value #PROPIEDAD_URL}.
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class CotizacionBinance implements CotizacionStrategy {
    private static final Logger LOG = Logger.getLogger(CotizacionBinance.class.getName());
    public static final String PROPIEDAD_URL = "inversorar.binance.url";
    public static final String URL_POR_DEFECTO = "https://api.binance.com";
    public static final Duration TIMEOUT = Duration.ofSeconds(5);
    static final Duration VIGENCIA_PRECIO = Duration.ofSeconds(15);
    static final Duration PAUSA_TRAS_FALLA = Duration.ofSeconds(30);
    static final Duration VIGENCIA_SIMBOLO_INEXISTENTE = Duration.ofMinutes(10);
    private static final Pattern SIMBOLO = Pattern.compile("[A-Z0-9]{2,20}");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject private CotizacionCatalogo catalogo;
    @Inject private ConfiguracionApp configuracion;

    private final String base;
    private final Duration timeout;
    private final Clock reloj;
    private final HttpClient http;
    private final Map<String, Precio> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> inexistentes = new ConcurrentHashMap<>();
    private volatile Instant pausadoHasta = Instant.MIN;

    public CotizacionBinance() { this(System.getProperty(PROPIEDAD_URL, URL_POR_DEFECTO), TIMEOUT, Clock.systemDefaultZone()); }

    CotizacionBinance(String base, Duration timeout, Clock reloj) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.timeout = timeout; this.reloj = reloj;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    Duration timeout() { return timeout; }

    @Override
    public BigDecimal cotizar(Instrumento instrumento) {
        BigDecimal respaldo = catalogo.cotizar(instrumento); // también fija moneda y fecha del último cierre
        if (configuracion == null || !configuracion.catalogoMysql()) return respaldo;
        Precio vivo = precio(instrumento.getTicker());
        if (vivo == null) {
            instrumento.marcarFuenteCotizacion("RESPALDO");
            return respaldo;
        }
        instrumento.usarCotizacion(vivo.valor, LocalDateTime.ofInstant(vivo.obtenido, reloj.getZone()).format(FECHA), "BINANCE");
        return vivo.valor;
    }

    /** Precio en vivo, o null si hay que usar el fallback. Nunca lanza: el portfolio debe mostrarse igual. */
    Precio precio(String ticker) {
        if (ticker == null || !SIMBOLO.matcher(ticker).matches()) return null;
        Instant ahora = reloj.instant();
        Precio enCache = cache.get(ticker);
        if (enCache != null && enCache.obtenido.plus(VIGENCIA_PRECIO).isAfter(ahora)) return enCache;
        Instant inexistente = inexistentes.get(ticker);
        if (inexistente != null && inexistente.plus(VIGENCIA_SIMBOLO_INEXISTENTE).isAfter(ahora)) return null;
        if (pausadoHasta.isAfter(ahora)) return null;

        HttpRequest pedido = HttpRequest.newBuilder(URI.create(base + "/api/v3/ticker/price?symbol=" + ticker))
                .timeout(timeout).header("Accept", "application/json").GET().build();
        var futuro = http.sendAsync(pedido, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> r = futuro.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (r.statusCode() == 200) {
                try (var lector = Json.createReader(new StringReader(r.body()))) {
                    var precio = new Precio(new BigDecimal(lector.readObject().getString("price")), ahora);
                    cache.put(ticker, precio);
                    return precio;
                }
            }
            if (r.statusCode() >= 400 && r.statusCode() < 500 && r.statusCode() != 429) {
                // Símbolo que Binance no lista (o dado de baja): no es una caída del proveedor.
                inexistentes.put(ticker, ahora);
                LOG.info("Binance no cotiza " + ticker + " (HTTP " + r.statusCode() + "); se usa el último cierre guardado");
                return null;
            }
            return pausar("HTTP " + r.statusCode(), ahora);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            return pausar("sin respuesta en " + timeout.toMillis() + " ms", ahora);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return pausar(String.valueOf(e.getCause() != null ? e.getCause() : e), ahora);
        }
    }

    private Precio pausar(String motivo, Instant ahora) {
        pausadoHasta = ahora.plus(PAUSA_TRAS_FALLA);
        LOG.warning("Binance no disponible (" + motivo + "): se usa el último cierre de MySQL durante "
                + PAUSA_TRAS_FALLA.toSeconds() + " s");
        return null;
    }

    record Precio(BigDecimal valor, Instant obtenido) { }
}
