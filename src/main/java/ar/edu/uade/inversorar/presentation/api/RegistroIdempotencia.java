package ar.edu.uade.inversorar.presentation.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memoria de las claves Idempotency-Key ya usadas. La reserva es atómica (putIfAbsent): si llegan dos pedidos con
 * la misma clave a la vez (doble clic, reintento del navegador), solo uno se ejecuta y el otro recibe "en curso".
 * <p>
 * Vive en memoria del servidor: alcanza para una única instancia de WildFly. Con varias instancias habría que
 * guardarlo en la base o en un caché compartido. Las claves vencen a las 24 h y una reserva sin terminar a los 2 min.
 */
public final class RegistroIdempotencia {
    public static final Duration VIGENCIA = Duration.ofHours(24);
    static final Duration VIGENCIA_EN_CURSO = Duration.ofMinutes(2);
    static final int MAXIMO = 10_000;

    public enum Estado { NUEVA, EN_CURSO, CUERPO_DISTINTO, REPETIDA }

    /** Respuesta guardada para repetirla tal cual. */
    public record Guardada(int status, Object entidad, String tipo, String location) { }

    public record Resultado(Estado estado, Guardada respuesta) { }

    private static final class Entrada {
        final String huella; final Instant creada; volatile Guardada respuesta;
        Entrada(String huella, Instant creada) { this.huella = huella; this.creada = creada; }
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Clock reloj;

    public RegistroIdempotencia(Clock reloj) { this.reloj = reloj; }

    /** @param clave clave ya combinada con el usuario; @param huella resumen de método, ruta y cuerpo del pedido. */
    public Resultado reservar(String clave, String huella) {
        Instant ahora = reloj.instant();
        if (entradas.size() >= MAXIMO) depurar(ahora);
        while (true) {
            Entrada nueva = new Entrada(huella, ahora);
            Entrada previa = entradas.putIfAbsent(clave, nueva);
            if (previa == null) return new Resultado(Estado.NUEVA, null);
            if (vencida(previa, ahora)) {
                if (entradas.replace(clave, previa, nueva)) return new Resultado(Estado.NUEVA, null);
                continue; // otro pedido la reemplazó al mismo tiempo: volver a evaluar
            }
            if (!previa.huella.equals(huella)) return new Resultado(Estado.CUERPO_DISTINTO, null);
            Guardada r = previa.respuesta;
            return r == null ? new Resultado(Estado.EN_CURSO, null) : new Resultado(Estado.REPETIDA, r);
        }
    }

    public void completar(String clave, Guardada respuesta) {
        Entrada e = entradas.get(clave);
        if (e != null) e.respuesta = respuesta;
    }

    /** Ante un error del servidor (5xx) la clave se libera: el cliente puede reintentar con la misma. */
    public void liberar(String clave) { entradas.remove(clave); }

    int tamanio() { return entradas.size(); }

    private boolean vencida(Entrada e, Instant ahora) {
        Duration vigencia = e.respuesta == null ? VIGENCIA_EN_CURSO : VIGENCIA;
        return !e.creada.plus(vigencia).isAfter(ahora);
    }

    private void depurar(Instant ahora) {
        entradas.entrySet().removeIf(e -> vencida(e.getValue(), ahora));
        // Si siguen sobrando, se descartan las más viejas ya respondidas (nunca una en curso).
        int sobran = entradas.size() - MAXIMO * 9 / 10;
        if (sobran > 0)
            entradas.entrySet().stream().filter(e -> e.getValue().respuesta != null)
                    .sorted(Comparator.comparing(e -> e.getValue().creada)).limit(sobran).map(Map.Entry::getKey)
                    .toList().forEach(entradas::remove);
    }
}
