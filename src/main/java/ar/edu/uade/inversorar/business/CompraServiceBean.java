package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Stateless
@jakarta.annotation.security.RolesAllowed("USUARIO")
public class CompraServiceBean implements CompraService {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CompraServiceBean.class.getName());
    @jakarta.annotation.PostConstruct public void iniciar() { LOG.info("CompraServiceBean inicializado"); }
    @jakarta.annotation.PreDestroy public void destruir() { LOG.info("CompraServiceBean destruido"); }
    @Inject private InstrumentoRepository instrumentos;
    @Inject private CatalogoMercadoRepository catalogo;
    @Inject private ConfiguracionApp configuracion;
    @Override public List<ar.edu.uade.inversorar.business.dto.InstrumentoMercado> listarCatalogo(String busqueda, int pagina) {
        if (!configuracion.catalogoMysql()) throw new ReglaNegocioException("Catálogo MySQL no configurado");
        if (busqueda == null || busqueda.length() > 80 || pagina < 0 || pagina > 10000)
            throw new ReglaNegocioException("Búsqueda inválida");
        return catalogo.listar(busqueda.trim(), pagina, 50);
    }
    @Override public List<ar.edu.uade.inversorar.business.dto.PrecioMercado> preciosHistoricos(long id, int limite) {
        if (!configuracion.catalogoMysql()) throw new ReglaNegocioException("Catálogo MySQL no configurado");
        if (id <= 0 || limite < 1 || limite > 365) throw new ReglaNegocioException("Rango inválido");
        return catalogo.precios(id, limite);
    }
    @Inject private PortfolioActual portfolioActual;
    @Inject private OperacionRepository operaciones;
    @Inject private OrdenRepository ordenes;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void registrarCompra(CompraRequest request) {
        if (request == null || request.ticker == null || request.ticker.isBlank()) throw new ReglaNegocioException("Debe seleccionar un instrumento.");
        if (request.cantidad == null || request.cantidad.signum() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (request.precioUnitario == null || request.precioUnitario.signum() <= 0) throw new ReglaNegocioException("El precio unitario debe ser mayor que cero.");
        if (request.fecha == null || request.fecha.isAfter(LocalDate.now())) throw new ReglaNegocioException("La fecha de compra es inválida.");
        validarDecimal(request.cantidad, 10, 13, "cantidad");
        validarDecimal(request.precioUnitario, 10, 15, "precio");
        if (request.cantidad.multiply(request.precioUnitario).setScale(10, RoundingMode.HALF_UP).signum() == 0)
            throw new ReglaNegocioException("El total es inferior a la precisión monetaria admitida");
        validarDecimal(request.cantidad.multiply(request.precioUnitario).setScale(10, RoundingMode.HALF_UP), 10, 15, "total");
        Instrumento instrumento = instrumentos.buscarPorTicker(request.ticker.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ReglaNegocioException("El instrumento seleccionado no existe."));
        if (instrumento.getCotizacionActual() == null || instrumento.getMonedaCotizacion() == null)
            throw new ReglaNegocioException("El instrumento no tiene precio o moneda de cotización para valorizar la compra.");
        Portfolio portfolio = portfolioActual.obtener();
        Operacion operacion = new Operacion(portfolio, instrumento, request.cantidad, request.precioUnitario, request.fecha);
        if (configuracion != null && configuracion.catalogoMysql()) {
            if (portfolio.getUsuarioId() == null)
                throw new ReglaNegocioException("La cuenta todavía no está vinculada a un usuario para registrar órdenes.");
            operacion.vincularOrden(ordenes.guardarCompra(operacion));
        }
        operaciones.guardar(operacion);
    }

    @Override
    public List<InstrumentoDto> listarInstrumentos() { return instrumentos.listar().stream().map(InstrumentoDto::new).toList(); }

    private void validarDecimal(BigDecimal valor, int escala, int enteros, String campo) {
        BigDecimal normal = valor.stripTrailingZeros();
        if (normal.scale() > escala || normal.precision() - normal.scale() > enteros)
            throw new ReglaNegocioException("Precisión o importe fuera de rango: " + campo);
    }
}
