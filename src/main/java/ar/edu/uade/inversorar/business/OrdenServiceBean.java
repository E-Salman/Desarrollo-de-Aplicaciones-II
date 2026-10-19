package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.OrdenDto;
import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.mensajeria.ProductorOrdenes;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
@RolesAllowed("USUARIO")
public class OrdenServiceBean implements OrdenService {
    private static final Logger LOG = Logger.getLogger(OrdenServiceBean.class.getName());
    @Inject private PortfolioActual portfolioActual;
    @Inject private OrdenRepository ordenes;
    @Inject private OperacionRepository operaciones;
    @Inject private ProductorOrdenes productor;

    @Override
    public List<OrdenDto> misOrdenes(Set<EstadoOrden> estados, int limite) {
        if (limite < 1 || limite > 100) throw new ReglaNegocioException("El límite debe estar entre 1 y 100.");
        return ordenes.detallesPorUsuario(portfolio().getUsuarioId(), estados, limite).stream().map(OrdenDto::de).toList();
    }

    @Override
    public Optional<OrdenDto> orden(long ordenId) {
        Long usuario = portfolio().getUsuarioId();
        return ordenes.detalleDe(ordenId).filter(d -> usuario.equals(d.getOrden().getUsuarioId())).map(OrdenDto::de);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenDto cancelar(long ordenId) {
        Orden orden = propia(ordenId);
        if (!orden.estaPendiente())
            throw new ReglaNegocioException("Solo se puede cancelar una orden pendiente; la orden #" + ordenId + " está " + orden.getEstado() + ".");
        // El mensaje sigue en la cola: el Ejecutor lo va a recibir, verá la orden CANCELADA y lo descartará.
        orden.cancelar();
        LOG.info("Orden #" + ordenId + " cancelada por el usuario");
        return OrdenDto.de(ordenes.detalleDe(ordenId).orElseThrow());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenDto reintentar(long ordenId) {
        Portfolio portfolio = portfolio();
        Orden orden = propiaFallida(ordenId, "enviar de nuevo");
        OrdenDetalle detalle = ordenes.detalleDe(ordenId).orElseThrow();
        if (orden.getTipo() == TipoOperacion.VENTA) validarDisponible(portfolio, detalle);
        orden.reintentar();
        // El mensaje nuevo se arma desde la base, no desde el que terminó en la DLQ.
        productor.enviar(orden, detalle);
        LOG.info("Orden #" + ordenId + " enviada de nuevo por el usuario: vuelve a PENDIENTE");
        return OrdenDto.de(detalle);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenDto descartar(long ordenId) {
        Orden orden = propiaFallida(ordenId, "eliminar");
        orden.descartar();
        LOG.info("Orden #" + ordenId + " descartada por el usuario");
        return OrdenDto.de(ordenes.detalleDe(ordenId).orElseThrow());
    }

    /** Una venta reenviada no puede superar lo que se tiene menos lo ya comprometido en otras ventas pendientes. */
    private void validarDisponible(Portfolio portfolio, OrdenDetalle detalle) {
        Instrumento instrumento = detalle.getInstrumento();
        BigDecimal poseida = BigDecimal.ZERO;
        for (Operacion o : operaciones.porPortfolioYTicker(portfolio.getId(), instrumento.getTicker()))
            poseida = o.getTipo() == TipoOperacion.VENTA ? poseida.subtract(o.getCantidad()) : poseida.add(o.getCantidad());
        BigDecimal disponible = poseida.subtract(ordenes.ventasPendientes(portfolio.getUsuarioId(), instrumento)).max(BigDecimal.ZERO);
        if (detalle.getCantidad().compareTo(disponible) > 0)
            throw new ReglaNegocioException("No se puede enviar de nuevo: querés vender " + texto(detalle.getCantidad()) + " de "
                    + instrumento.getTicker() + " y hoy podés vender hasta " + texto(disponible) + ".");
    }

    /** La orden bloqueada hasta el fin de la transacción: un doble clic, o el Ejecutor, no la cambian a la vez. */
    private Orden propia(long ordenId) {
        Long usuario = portfolio().getUsuarioId();
        return ordenes.bloquear(ordenId).filter(o -> usuario.equals(o.getUsuarioId()))
                .orElseThrow(() -> new ReglaNegocioException("La orden no existe."));
    }

    private Orden propiaFallida(long ordenId, String accion) {
        Orden orden = propia(ordenId);
        if (!orden.fallo())
            throw new ReglaNegocioException("Solo se puede " + accion + " una orden rechazada o con error; la orden #" + ordenId + " está " + orden.getEstado() + ".");
        return orden;
    }

    private Portfolio portfolio() {
        Portfolio portfolio = portfolioActual.obtener();
        if (portfolio.getUsuarioId() == null) throw new ReglaNegocioException("La cuenta todavía no está vinculada a un usuario para registrar órdenes.");
        return portfolio;
    }

    private static String texto(BigDecimal cantidad) { return cantidad.stripTrailingZeros().toPlainString(); }
}
