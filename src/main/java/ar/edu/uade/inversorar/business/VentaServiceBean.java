package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.OrdenDto;
import ar.edu.uade.inversorar.business.dto.VentaRequest;
import ar.edu.uade.inversorar.mensajeria.ProductorOrdenes;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Stateless
@jakarta.annotation.security.RolesAllowed("USUARIO")
public class VentaServiceBean implements VentaService {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(VentaServiceBean.class.getName());
    @jakarta.annotation.PostConstruct public void iniciar() { LOG.info("VentaServiceBean inicializado"); }
    @jakarta.annotation.PreDestroy public void destruir() { LOG.info("VentaServiceBean destruido"); }
    @Inject private InstrumentoRepository instrumentos;
    @Inject private PortfolioActual portfolioActual;
    @Inject private OperacionRepository operaciones;
    @Inject private ConfiguracionApp configuracion;
    @Inject private OrdenRepository ordenes;
    @Inject private ProductorOrdenes productor;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenDto registrarVenta(VentaRequest request) {
        if (request == null || request.ticker == null || request.ticker.isBlank()) throw new ReglaNegocioException("Debe seleccionar un instrumento.");
        if (request.cantidad == null || request.cantidad.signum() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (request.precioUnitario == null || request.precioUnitario.signum() <= 0) throw new ReglaNegocioException("El precio unitario debe ser mayor que cero.");
        if (request.fecha == null || request.fecha.isAfter(LocalDate.now())) throw new ReglaNegocioException("La fecha de venta es inválida.");
        validarDecimal(request.cantidad, 10, 13, "cantidad");
        validarDecimal(request.precioUnitario, 10, 15, "precio");
        String ticker = request.ticker.trim().toUpperCase(Locale.ROOT);
        Instrumento instrumento = instrumentos.buscarPorTicker(ticker)
                .orElseThrow(() -> new ReglaNegocioException("El instrumento seleccionado no existe."));
        Portfolio portfolio = portfolioActual.obtener();
        List<Operacion> historial = operaciones.porPortfolioYTicker(portfolio.getId(), ticker);
        BigDecimal poseida = BigDecimal.ZERO;
        for (Operacion o : historial) poseida = o.getTipo() == TipoOperacion.VENTA ? poseida.subtract(o.getCantidad()) : poseida.add(o.getCantidad());
        // Con mensajería esta validación es orientativa (avisa en el momento el caso obvio);
        // la definitiva la hace el Ejecutor al ejecutar, porque otra venta pudo ejecutarse antes.
        if (poseida.signum() <= 0) throw new ReglaNegocioException("No tenés ese instrumento en tu portfolio.");
        if (request.cantidad.compareTo(poseida) > 0) throw new ReglaNegocioException("No podés vender más cantidad de la que tenés en cartera.");
        if (configuracion != null && configuracion.catalogoMysql()) {
            if (portfolio.getUsuarioId() == null)
                throw new ReglaNegocioException("La cuenta todavía no está vinculada a un usuario para registrar órdenes.");
            // Lo ya comprometido en ventas que todavía esperan en la cola no se puede volver a vender.
            BigDecimal comprometida = ordenes.ventasPendientes(portfolio.getUsuarioId(), instrumento);
            BigDecimal disponible = poseida.subtract(comprometida).max(BigDecimal.ZERO);
            if (request.cantidad.compareTo(disponible) > 0)
                throw new ReglaNegocioException("Tenés " + texto(poseida) + " de " + ticker + ", pero " + texto(comprometida)
                        + " ya están en ventas pendientes: podés vender hasta " + texto(disponible) + ".");
            OrdenDetalle detalle = ordenes.crearPendiente(portfolio.getUsuarioId(), TipoOperacion.VENTA, instrumento,
                    request.cantidad, request.precioUnitario, request.fecha);
            productor.enviar(detalle.getOrden(), detalle);
            return OrdenDto.de(detalle);
        }
        operaciones.guardar(new Operacion(portfolio, instrumento, TipoOperacion.VENTA, request.cantidad, request.precioUnitario, request.fecha));
        return null;
    }

    private static String texto(BigDecimal cantidad) { return cantidad.stripTrailingZeros().toPlainString(); }

    private void validarDecimal(BigDecimal valor, int escala, int enteros, String campo) {
        BigDecimal normal = valor.stripTrailingZeros();
        if (normal.scale() > escala || normal.precision() - normal.scale() > enteros)
            throw new ReglaNegocioException("Precisión o importe fuera de rango: " + campo);
    }
}
