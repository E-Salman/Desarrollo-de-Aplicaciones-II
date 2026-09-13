package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;
import ar.edu.uade.inversorar.business.dto.PosicionDto;
import ar.edu.uade.inversorar.business.dto.ResumenPortfolioDto;
import ar.edu.uade.inversorar.business.dto.VentaRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class OperacionServiceBean implements OperacionService {
    private static final Logger LOG = Logger.getLogger(OperacionServiceBean.class.getName());
    private static final Long PORTFOLIO_DEMO_ID = 1L;
    @Inject private InstrumentoRepository instrumentos;
    @Inject private PortfolioRepository portfolios;
    @Inject private OperacionRepository operaciones;
    @Inject private CompraStrategy compraStrategy;
    @Inject private VentaStrategy ventaStrategy;
    private Map<TipoOperacion, OperacionStrategy> estrategiasPorTipo;

    /** El contenedor invoca este callback luego de inyectar las dependencias y antes de servir el primer pedido. */
    @PostConstruct
    public void init() {
        estrategiasPorTipo = new EnumMap<>(TipoOperacion.class);
        estrategiasPorTipo.put(compraStrategy.getTipo(), compraStrategy);
        estrategiasPorTipo.put(ventaStrategy.getTipo(), ventaStrategy);
        LOG.fine(() -> "OperacionServiceBean inicializado con estrategias: " + estrategiasPorTipo.keySet());
    }

    /** El contenedor invoca este callback antes de destruir la instancia del bean. */
    @PreDestroy
    public void destruir() {
        LOG.fine("OperacionServiceBean destruido por el contenedor.");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void registrarCompra(CompraRequest request) {
        String ticker = request == null ? null : request.ticker;
        BigDecimal cantidad = request == null ? null : request.cantidad;
        BigDecimal precioUnitario = request == null ? null : request.precioUnitario;
        LocalDate fecha = request == null ? null : request.fecha;
        registrar(TipoOperacion.COMPRA, ticker, cantidad, precioUnitario, fecha);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void registrarVenta(VentaRequest request) {
        String ticker = request == null ? null : request.ticker;
        BigDecimal cantidad = request == null ? null : request.cantidad;
        BigDecimal precioUnitario = request == null ? null : request.precioUnitario;
        LocalDate fecha = request == null ? null : request.fecha;
        registrar(TipoOperacion.VENTA, ticker, cantidad, precioUnitario, fecha);
    }

    private void registrar(TipoOperacion tipo, String ticker, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha) {
        if (ticker == null || ticker.isBlank()) throw new ReglaNegocioException("Debe seleccionar un instrumento.");
        String tickerNormalizado = ticker.trim().toUpperCase();
        Instrumento instrumento = instrumentos.buscarPorTicker(tickerNormalizado)
                .orElseThrow(() -> new ReglaNegocioException("El instrumento seleccionado no existe."));
        Portfolio portfolio = portfolios.buscar(PORTFOLIO_DEMO_ID);
        if (portfolio == null) throw new ReglaNegocioException("No existe el portfolio de demostración.");
        List<Operacion> historial = operaciones.porPortfolioYTicker(PORTFOLIO_DEMO_ID, tickerNormalizado);
        OperacionStrategy estrategia = estrategiasPorTipo.get(tipo);
        estrategia.validar(instrumento, cantidad, precioUnitario, fecha, historial);
        operaciones.guardar(estrategia.crear(portfolio, instrumento, cantidad, precioUnitario, fecha));
    }

    @Override
    public List<InstrumentoDto> listarInstrumentos() { return instrumentos.listar().stream().map(InstrumentoDto::new).toList(); }

    @Override
    public ResumenPortfolioDto obtenerResumen(Long portfolioId) {
        List<Operacion> historial = operaciones.porPortfolio(portfolioId);
        Map<String, List<Operacion>> porTicker = historial.stream().collect(Collectors.groupingBy(o -> o.getInstrumento().getTicker()));
        List<PosicionDto> posiciones = new ArrayList<>();
        BigDecimal invertidoTotal = BigDecimal.ZERO, actualTotal = BigDecimal.ZERO, gananciaRealizadaTotal = BigDecimal.ZERO;
        for (List<Operacion> grupo : porTicker.values()) {
            List<Operacion> cronologico = grupo.stream()
                    .sorted(Comparator.comparing(Operacion::getFecha).thenComparing(Operacion::getId))
                    .toList();
            Instrumento instrumento = cronologico.get(0).getInstrumento();
            BigDecimal cantidad = BigDecimal.ZERO, invertido = BigDecimal.ZERO;
            for (Operacion o : cronologico) {
                if (o.getTipo() == TipoOperacion.VENTA) {
                    BigDecimal precioPromedioVigente = cantidad.signum() > 0 ? invertido.divide(cantidad, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    BigDecimal costoVendido = o.getCantidad().multiply(precioPromedioVigente);
                    gananciaRealizadaTotal = gananciaRealizadaTotal.add(o.getTotal().subtract(costoVendido));
                    invertido = invertido.subtract(costoVendido);
                    cantidad = cantidad.subtract(o.getCantidad());
                } else {
                    cantidad = cantidad.add(o.getCantidad());
                    invertido = invertido.add(o.getTotal());
                }
            }
            if (cantidad.signum() <= 0) continue;
            PosicionDto p = new PosicionDto();
            p.nombre = instrumento.getNombre(); p.ticker = instrumento.getTicker(); p.tipo = instrumento.getTipo().name();
            p.cantidad = cantidad; p.invertido = invertido; p.precioPromedio = invertido.divide(cantidad, 4, RoundingMode.HALF_UP);
            p.actual = cantidad.multiply(instrumento.getCotizacionActual()); p.rendimiento = p.actual.subtract(p.invertido);
            posiciones.add(p); invertidoTotal = invertidoTotal.add(p.invertido); actualTotal = actualTotal.add(p.actual);
        }
        posiciones.sort(Comparator.comparing(p -> p.nombre));
        ResumenPortfolioDto resumen = new ResumenPortfolioDto();
        resumen.capitalInvertido = invertidoTotal; resumen.patrimonioTotal = actualTotal;
        resumen.gananciaRealizada = gananciaRealizadaTotal;
        resumen.gananciaTotal = actualTotal.subtract(invertidoTotal).add(gananciaRealizadaTotal);
        resumen.posiciones = posiciones;
        return resumen;
    }
}
