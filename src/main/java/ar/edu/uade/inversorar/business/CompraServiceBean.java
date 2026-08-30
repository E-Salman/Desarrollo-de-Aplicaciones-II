package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class CompraServiceBean implements CompraService {
    private static final Long PORTFOLIO_DEMO_ID = 1L;
    @Inject private InstrumentoRepository instrumentos;
    @Inject private PortfolioRepository portfolios;
    @Inject private OperacionRepository operaciones;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void registrarCompra(CompraRequest request) {
        if (request == null || request.ticker == null || request.ticker.isBlank()) throw new ReglaNegocioException("Debe seleccionar un instrumento.");
        if (request.cantidad == null || request.cantidad.signum() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (request.precioUnitario == null || request.precioUnitario.signum() <= 0) throw new ReglaNegocioException("El precio unitario debe ser mayor que cero.");
        if (request.fecha == null || request.fecha.isAfter(LocalDate.now())) throw new ReglaNegocioException("La fecha de compra es inválida.");
        Instrumento instrumento = instrumentos.buscarPorTicker(request.ticker.trim().toUpperCase())
                .orElseThrow(() -> new ReglaNegocioException("El instrumento seleccionado no existe."));
        Portfolio portfolio = portfolios.buscar(PORTFOLIO_DEMO_ID);
        if (portfolio == null) throw new ReglaNegocioException("No existe el portfolio de demostración.");
        operaciones.guardar(new Operacion(portfolio, instrumento, request.cantidad, request.precioUnitario, request.fecha));
    }

    @Override
    public List<InstrumentoDto> listarInstrumentos() { return instrumentos.listar().stream().map(InstrumentoDto::new).toList(); }

    @Override
    public ResumenPortfolioDto obtenerResumen(Long portfolioId) {
        List<Operacion> compras = operaciones.porPortfolio(portfolioId);
        Map<String, List<Operacion>> porTicker = compras.stream().collect(Collectors.groupingBy(o -> o.getInstrumento().getTicker()));
        List<PosicionDto> posiciones = new ArrayList<>();
        BigDecimal invertido = BigDecimal.ZERO, actual = BigDecimal.ZERO;
        for (List<Operacion> grupo : porTicker.values()) {
            Operacion primera = grupo.get(0); BigDecimal cantidad = BigDecimal.ZERO, total = BigDecimal.ZERO;
            for (Operacion o : grupo) { cantidad = cantidad.add(o.getCantidad()); total = total.add(o.getTotal()); }
            PosicionDto p = new PosicionDto(); p.nombre = primera.getInstrumento().getNombre(); p.ticker = primera.getInstrumento().getTicker(); p.tipo = primera.getInstrumento().getTipo().name();
            p.cantidad = cantidad; p.invertido = total; p.precioPromedio = total.divide(cantidad, 4, RoundingMode.HALF_UP);
            p.actual = cantidad.multiply(primera.getInstrumento().getCotizacionActual()); p.rendimiento = p.actual.subtract(p.invertido);
            posiciones.add(p); invertido = invertido.add(p.invertido); actual = actual.add(p.actual);
        }
        posiciones.sort(Comparator.comparing(p -> p.nombre));
        ResumenPortfolioDto resumen = new ResumenPortfolioDto(); resumen.capitalInvertido = invertido; resumen.patrimonioTotal = actual; resumen.gananciaTotal = actual.subtract(invertido); resumen.posiciones = posiciones;
        return resumen;
    }
}
