package ar.edu.uade.inversorar.ia;

import java.time.Duration;

/**
 * Proveedor sin IA real: lo usan los tests, el CI y cualquier instalación sin {@code GEMINI_API_KEY}.
 * Responde textos fijos y permite simular fallas escribiendo una marca en la pregunta:
 * {@code [error]} (proveedor caído), {@code [lento]} (timeout) y {@code [bloqueada]} (filtro de seguridad).
 */
public class ProveedorSimulado implements ProveedorIA {
    static final String RESPUESTA_AYUDA = "Respuesta de prueba: el asistente funciona sin un proveedor de IA real. "
            + "Configurá GEMINI_API_KEY en el servidor para obtener respuestas generadas.";
    static final String RESPUESTA_NOTICIAS = "Respuesta de prueba: abajo están las noticias encontradas, "
            + "con el enlace a cada una. Configurá GEMINI_API_KEY para obtener un resumen generado.";

    @Override public String nombre() { return "SIMULADO"; }

    @Override
    public String responder(String instrucciones, String pregunta, Duration plazo) throws ServicioNoDisponible, RespuestaBloqueada {
        if (pregunta.contains("[error]")) throw new ServicioNoDisponible("Falla simulada del proveedor");
        if (pregunta.contains("[lento]")) throw new ServicioNoDisponible("Timeout simulado: sin respuesta en " + plazo.toMillis() + " ms");
        if (pregunta.contains("[bloqueada]")) throw new RespuestaBloqueada("SAFETY");
        return instrucciones.contains(Asistente.MARCA_NOTICIAS) ? RESPUESTA_NOTICIAS : RESPUESTA_AYUDA;
    }
}
