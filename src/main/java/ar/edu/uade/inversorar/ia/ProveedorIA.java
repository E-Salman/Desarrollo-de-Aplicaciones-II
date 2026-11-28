package ar.edu.uade.inversorar.ia;

import java.time.Duration;

/**
 * Modelo de lenguaje detrás del gateway. Hay dos implementaciones intercambiables: {@link ProveedorGemini} (real)
 * y {@link ProveedorSimulado} (tests, CI y demo sin clave). El resto del asistente no sabe cuál está activa.
 */
public interface ProveedorIA {
    /** Nombre corto para la respuesta y los logs: "GEMINI" o "SIMULADO". */
    String nombre();

    /**
     * Devuelve el texto generado para la pregunta, siguiendo las instrucciones de sistema.
     *
     * @param plazo tiempo máximo de espera; si se supera, lanza {@link ServicioNoDisponible}
     * @throws ServicioNoDisponible si el proveedor no responde, responde con error o supera el plazo
     * @throws RespuestaBloqueada si el proveedor se niega a responder por sus filtros de seguridad
     */
    String responder(String instrucciones, String pregunta, Duration plazo) throws ServicioNoDisponible, RespuestaBloqueada;
}
