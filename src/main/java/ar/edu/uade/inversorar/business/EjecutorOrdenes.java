package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.mensajeria.OrdenMensaje;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Ejecuta una orden PENDIENTE: crea la operación y la completa, o la rechaza con su motivo. Lo invocan
 * los MDB, que corren sin usuario autenticado, por eso no exige rol; no se expone a la web.
 * Participa de la transacción del MDB: si algo falla con una excepción, se deshace todo y el broker
 * reintenta el mensaje. Los rechazos de negocio no son excepciones: se guardan y el mensaje se confirma.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class EjecutorOrdenes {
    private static final Logger LOG = Logger.getLogger(EjecutorOrdenes.class.getName());
    public enum Resultado { COMPLETADA, RECHAZADA, IGNORADA }

    @Inject private OrdenRepository ordenes;
    @Inject private PortfolioRepository portfolios;
    @Inject private OperacionRepository operaciones;

    public Resultado ejecutar(OrdenMensaje mensaje) {
        // Bloqueada hasta el fin de la transacción: nadie la cancela o reintenta mientras se ejecuta.
        Orden orden = ordenes.bloquear(mensaje.ordenId).orElse(null);
        if (orden == null) { LOG.warning(mensaje + ": la orden no existe; se descarta el mensaje"); return Resultado.IGNORADA; }
        // Idempotencia: un mensaje duplicado, o de una orden cancelada, no vuelve a ejecutarla.
        if (!orden.estaPendiente()) { LOG.info(mensaje + ": la orden está " + orden.getEstado() + "; se descarta el mensaje"); return Resultado.IGNORADA; }

        OrdenDetalle detalle = ordenes.detalleDe(orden.getId()).orElseThrow(() -> new IllegalStateException("La orden #" + orden.getId() + " no tiene detalle"));
        Portfolio portfolio = portfolios.porUsuarioId(orden.getUsuarioId()).orElse(null);
        if (portfolio == null) return rechazar(orden, "La cuenta no tiene un portfolio para registrar la operación.");
        Instrumento instrumento = detalle.getInstrumento();

        if (orden.getTipo() == TipoOperacion.VENTA) {
            // Validación definitiva: la tenencia de este momento, con las ventas anteriores ya ejecutadas.
            BigDecimal poseida = BigDecimal.ZERO;
            for (Operacion o : operaciones.porPortfolioYTicker(portfolio.getId(), instrumento.getTicker()))
                poseida = o.getTipo() == TipoOperacion.VENTA ? poseida.subtract(o.getCantidad()) : poseida.add(o.getCantidad());
            if (detalle.getCantidad().compareTo(poseida) > 0)
                return rechazar(orden, "Tenencia insuficiente: tenés " + texto(poseida) + " de " + instrumento.getTicker()
                        + " y la orden quería vender " + texto(detalle.getCantidad()) + ".");
        }
        LocalDate fecha = orden.getFechaOperacion() != null ? orden.getFechaOperacion() : LocalDate.now();
        Operacion operacion = new Operacion(portfolio, instrumento, orden.getTipo(), detalle.getCantidad(), detalle.getPrecioUnitario(), fecha);
        operacion.vincularOrden(detalle);
        operaciones.guardar(operacion);
        orden.completar();
        LOG.info(mensaje + ": COMPLETADA");
        return Resultado.COMPLETADA;
    }

    /** Lo llama el MDB de la DLQ: el mensaje agotó sus reintentos. Solo cambia una orden que siga PENDIENTE. */
    public boolean marcarError(Long ordenId, String motivo) {
        Orden orden = ordenes.bloquear(ordenId).orElse(null);
        if (orden == null || !orden.estaPendiente()) {
            LOG.info("Orden #" + ordenId + " en la DLQ, pero " + (orden == null ? "no existe" : "está " + orden.getEstado()) + "; no se marca ERROR");
            return false;
        }
        orden.marcarError(motivo);
        LOG.warning("Orden #" + ordenId + " marcada ERROR: " + motivo);
        return true;
    }

    private Resultado rechazar(Orden orden, String motivo) {
        orden.rechazar(motivo);
        LOG.info("Orden #" + orden.getId() + ": RECHAZADA (" + motivo + ")");
        return Resultado.RECHAZADA;
    }

    private static String texto(BigDecimal cantidad) { return cantidad.stripTrailingZeros().toPlainString(); }
}
