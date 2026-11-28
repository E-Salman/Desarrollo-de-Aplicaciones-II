package ar.edu.uade.inversorar.presentation.api;

import jakarta.json.Json;
import java.util.Map;

/**
 * Error en formato Problem Details (RFC 9457, antes RFC 7807), servido como {@value #TIPO_MEDIO}.
 * Campos estándar: type, title, status, detail e instance. Se agrega "mensaje" (igual a detail) como
 * miembro de extensión, por compatibilidad con el JavaScript que ya lo leía.
 * Es una clase sin dependencias de JAX-RS para poder usarla también desde filtros y servlets.
 */
public final class Problema {
    public static final String TIPO_MEDIO = "application/problem+json";
    private static final String PREFIJO = "urn:inversorar:problema:";

    public final String type;
    public final String title;
    public final int status;
    public final String detail;
    public final String instance;

    private Problema(String tipo, String titulo, int status, String detalle, String instancia) {
        this.type = tipo; this.title = titulo; this.status = status; this.detail = detalle; this.instance = instancia;
    }

    /** Problema con un tipo propio de InversorAR (urn:inversorar:problema:&lt;codigo&gt;). */
    public static Problema de(String codigo, String titulo, int status, String detalle, String instancia) {
        return new Problema(PREFIJO + codigo, titulo, status, detalle, instancia);
    }

    /** Problema genérico de HTTP: type "about:blank" y el título estándar del código. */
    public static Problema http(int status, String detalle, String instancia) {
        return new Problema("about:blank", titulo(status), status, detalle, instancia);
    }

    private static final Map<Integer, String> TITULOS = Map.ofEntries(
            Map.entry(400, "Pedido inválido"), Map.entry(401, "No autenticado"), Map.entry(403, "Acceso denegado"),
            Map.entry(404, "Recurso no encontrado"), Map.entry(405, "Método no permitido"), Map.entry(406, "Formato no aceptable"),
            Map.entry(409, "Conflicto"), Map.entry(412, "Precondición fallida"), Map.entry(415, "Tipo de contenido no soportado"),
            Map.entry(422, "Contenido no procesable"), Map.entry(429, "Demasiados pedidos"), Map.entry(500, "Error interno"),
            Map.entry(503, "Servicio no disponible"));

    public static String titulo(int status) {
        return TITULOS.getOrDefault(status, status >= 500 ? "Error del servidor" : "Error en el pedido");
    }

    public String json() {
        var b = Json.createObjectBuilder().add("type", type).add("title", title).add("status", status);
        if (detail != null) b.add("detail", detail).add("mensaje", detail);
        if (instance != null) b.add("instance", instance);
        return b.build().toString();
    }
}
