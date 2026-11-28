package ar.edu.uade.inversorar.ia;

import java.util.List;

/**
 * Respuesta del asistente.
 *
 * @param respuesta texto para mostrar (texto plano, sin enlaces)
 * @param origen    GEMINI o SIMULADO (generada por el proveedor), REGLA (respuesta fija del gateway, sin llamar al
 *                  modelo) o RESPALDO (fallback porque el proveedor o la fuente no respondieron)
 * @param noticias  noticias usadas, con el enlace de la fuente; vacía en el modo ayuda
 */
public record RespuestaAsistente(String respuesta, String origen, List<Noticia> noticias) {
    public static final String REGLA = "REGLA", RESPALDO = "RESPALDO";

    static RespuestaAsistente regla(String texto) { return new RespuestaAsistente(texto, REGLA, List.of()); }
    static RespuestaAsistente respaldo(String texto, List<Noticia> noticias) { return new RespuestaAsistente(texto, RESPALDO, noticias); }
}
