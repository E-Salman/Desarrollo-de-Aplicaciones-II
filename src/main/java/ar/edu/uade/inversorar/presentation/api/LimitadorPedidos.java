package ar.edu.uade.inversorar.presentation.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de pedidos por clave (usuario, IP o email) con un "balde de fichas": cada clave tiene {@code porMinuto}
 * fichas que se recargan de forma continua. Un pedido gasta una; sin fichas, se rechaza e informa cuánto esperar.
 * Permite ráfagas cortas (abrir el dashboard) pero corta un ataque de fuerza bruta. Vive en memoria del servidor.
 */
public final class LimitadorPedidos {
    static final int MAXIMO_CLAVES = 50_000;

    private final double capacidad;
    private final double fichasPorNano;
    private final Clock reloj;
    private final Map<String, Balde> baldes = new ConcurrentHashMap<>();

    public LimitadorPedidos(int porMinuto, Clock reloj) {
        if (porMinuto < 1) throw new IllegalArgumentException("El límite debe ser de al menos 1 pedido por minuto");
        this.capacidad = porMinuto;
        this.fichasPorNano = porMinuto / (double) Duration.ofMinutes(1).toNanos();
        this.reloj = reloj;
    }

    public int porMinuto() { return (int) capacidad; }

    /** @return null si el pedido se permite; si no, cuánto falta para la próxima ficha (para Retry-After). */
    public Duration consumir(String clave) {
        Instant ahora = reloj.instant();
        if (baldes.size() >= MAXIMO_CLAVES) depurar(ahora);
        return baldes.computeIfAbsent(clave, k -> new Balde(capacidad, ahora)).consumir(ahora);
    }

    private final class Balde {
        double fichas; Instant ultima;
        Balde(double fichas, Instant ultima) { this.fichas = fichas; this.ultima = ultima; }

        synchronized Duration consumir(Instant ahora) {
            recargar(ahora);
            if (fichas >= 1) { fichas -= 1; return null; }
            long nanos = (long) Math.ceil((1 - fichas) / fichasPorNano);
            return Duration.ofNanos(Math.max(nanos, 1));
        }
        synchronized boolean lleno(Instant ahora) { recargar(ahora); return fichas >= capacidad; }
        private void recargar(Instant ahora) {
            long nanos = Duration.between(ultima, ahora).toNanos();
            if (nanos > 0) { fichas = Math.min(capacidad, fichas + nanos * fichasPorNano); ultima = ahora; }
        }
    }

    /** Un balde lleno no guarda nada útil: se descarta para no crecer sin límite. */
    private void depurar(Instant ahora) { baldes.entrySet().removeIf(e -> e.getValue().lleno(ahora)); }

    int claves() { return baldes.size(); }
}
