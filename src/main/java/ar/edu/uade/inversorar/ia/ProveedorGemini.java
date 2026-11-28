package ar.edu.uade.inversorar.ia;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Proveedor real: API REST de Google Gemini ({@code POST /v1beta/models/{modelo}:generateContent}).
 * <ul>
 *   <li>La clave viaja en el encabezado {@code x-goog-api-key}, nunca en la URL: así no queda en logs ni en
 *       mensajes de error. Se lee de la variable de entorno {@value #VARIABLE_CLAVE}.</li>
 *   <li>Modelo fijo y configurable ({@value #PROPIEDAD_MODELO}); por defecto {@value #MODELO_POR_DEFECTO}, estable,
 *       del plan gratuito y con razonamiento mínimo para responder rápido.</li>
 *   <li>Tras una falla (timeout, 429 por cupo agotado, 5xx, clave inválida) se pausa {@link #PAUSA_TRAS_FALLA}:
 *       el asistente responde con el fallback sin esperar otro timeout en cada pregunta.</li>
 * </ul>
 */
public class ProveedorGemini implements ProveedorIA {
    private static final Logger LOG = Logger.getLogger(ProveedorGemini.class.getName());
    public static final String VARIABLE_CLAVE = "GEMINI_API_KEY";
    public static final String PROPIEDAD_MODELO = "inversorar.ia.modelo";
    public static final String PROPIEDAD_URL = "inversorar.ia.gemini.url";
    public static final String MODELO_POR_DEFECTO = "gemini-3.5-flash-lite";
    public static final String URL_POR_DEFECTO = "https://generativelanguage.googleapis.com";
    static final Duration PAUSA_TRAS_FALLA = Duration.ofSeconds(30);
    /** Incluye los tokens de razonamiento del modelo: no conviene ajustarlo demasiado. */
    static final int MAXIMO_TOKENS = 1024;
    private static final Set<String> FINALES_BLOQUEADOS =
            Set.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION");

    private final String base, modelo, clave;
    private final Clock reloj;
    private final HttpClient http;
    private volatile Instant pausadoHasta = Instant.MIN;

    public ProveedorGemini(String clave) {
        this(System.getProperty(PROPIEDAD_URL, URL_POR_DEFECTO), System.getProperty(PROPIEDAD_MODELO, MODELO_POR_DEFECTO),
                clave, Clock.systemUTC());
    }

    ProveedorGemini(String base, String modelo, String clave, Clock reloj) {
        if (clave == null || clave.isBlank()) throw new IllegalArgumentException("Falta la clave de Gemini");
        if (!modelo.matches("[a-z0-9.-]{3,60}")) throw new IllegalArgumentException("Modelo de Gemini inválido: " + modelo);
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.modelo = modelo; this.clave = clave.trim(); this.reloj = reloj;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override public String nombre() { return "GEMINI"; }
    public String modelo() { return modelo; }

    @Override
    public String responder(String instrucciones, String pregunta, Duration plazo) throws ServicioNoDisponible, RespuestaBloqueada {
        Instant ahora = reloj.instant();
        if (pausadoHasta.isAfter(ahora)) throw new ServicioNoDisponible("Gemini en pausa tras una falla reciente");
        if (plazo.isNegative() || plazo.isZero()) throw new ServicioNoDisponible("Sin tiempo para consultar a Gemini");

        HttpRequest pedido = HttpRequest.newBuilder(URI.create(base + "/v1beta/models/" + modelo + ":generateContent"))
                .timeout(plazo)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", clave)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo(instrucciones, pregunta), StandardCharsets.UTF_8))
                .build();
        var futuro = http.sendAsync(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpResponse<String> r;
        try {
            r = futuro.get(plazo.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw pausar("sin respuesta en " + plazo.toMillis() + " ms", ahora);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponible("Consulta interrumpida");
        } catch (Exception e) {
            throw pausar("error de conexión (" + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()) + ")", ahora);
        }
        if (r.statusCode() != 200) {
            // 400 modelo o pedido inválido, 401/403 clave inválida, 429 cupo gratuito agotado, 5xx caída.
            throw pausar("HTTP " + r.statusCode(), ahora);
        }
        return texto(r.body());
    }

    static String cuerpo(String instrucciones, String pregunta) {
        return Json.createObjectBuilder()
                .add("systemInstruction", Json.createObjectBuilder()
                        .add("parts", Json.createArrayBuilder().add(Json.createObjectBuilder().add("text", instrucciones))))
                .add("contents", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("role", "user")
                        .add("parts", Json.createArrayBuilder().add(Json.createObjectBuilder().add("text", pregunta)))))
                .add("generationConfig", Json.createObjectBuilder()
                        .add("maxOutputTokens", MAXIMO_TOKENS)
                        .add("temperature", 0.2)
                        .add("thinkingConfig", Json.createObjectBuilder().add("thinkingLevel", "MINIMAL")))
                .build().toString();
    }

    /** Extrae el texto de la respuesta de generateContent. */
    static String texto(String json) throws ServicioNoDisponible, RespuestaBloqueada {
        JsonObject raiz;
        try (var lector = Json.createReader(new StringReader(json))) {
            raiz = lector.readObject();
        } catch (RuntimeException e) {
            throw new ServicioNoDisponible("Respuesta de Gemini ilegible");
        }
        var candidatos = raiz.getJsonArray("candidates");
        if (candidatos == null || candidatos.isEmpty()) {
            var feedback = raiz.getJsonObject("promptFeedback");
            if (feedback != null && feedback.containsKey("blockReason"))
                throw new RespuestaBloqueada(feedback.getString("blockReason", "bloqueada"));
            throw new ServicioNoDisponible("Gemini no devolvió respuesta");
        }
        JsonObject candidato = candidatos.getJsonObject(0);
        String fin = candidato.getString("finishReason", "");
        if (FINALES_BLOQUEADOS.contains(fin)) throw new RespuestaBloqueada(fin);
        var sb = new StringBuilder();
        JsonObject contenido = candidato.getJsonObject("content");
        if (contenido != null && contenido.getJsonArray("parts") != null) {
            for (JsonValue v : contenido.getJsonArray("parts")) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) continue;
                JsonObject parte = v.asJsonObject();
                if (parte.getBoolean("thought", false)) continue; // razonamiento interno, no es respuesta
                sb.append(parte.getString("text", ""));
            }
        }
        String texto = sb.toString().trim();
        if (texto.isEmpty()) throw new ServicioNoDisponible("Gemini devolvió una respuesta vacía (" + fin + ")");
        return texto;
    }

    private ServicioNoDisponible pausar(String motivo, Instant ahora) {
        pausadoHasta = ahora.plus(PAUSA_TRAS_FALLA);
        LOG.warning("Gemini no disponible (" + motivo + "); el asistente usa el fallback durante " + PAUSA_TRAS_FALLA.toSeconds() + " s");
        return new ServicioNoDisponible("Gemini no disponible: " + motivo);
    }
}
