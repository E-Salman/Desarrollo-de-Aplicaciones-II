package ar.edu.uade.inversorar.business;
import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.*;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import java.io.Serializable;
import java.util.logging.Logger;
import java.math.*;
import java.util.*;
@Stateful @RolesAllowed("USUARIO")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PortfolioServiceBean implements PortfolioService, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PortfolioServiceBean.class.getName());
    private final String conversacion = UUID.randomUUID().toString();
    // Sólo la planificación es conversacional; las posiciones se consultan en cada llamada.
    private BigDecimal capital = BigDecimal.ZERO;
    private final Map<TipoInstrumento, BigDecimal> porcentajes = new EnumMap<>(TipoInstrumento.class);
    @Inject private PortfolioActual portfolioActual;
    @Inject private OperacionRepository operaciones;
    @Inject private CotizacionStrategy cotizaciones;
    @PostConstruct public void iniciar() { LOG.info("Portfolio inicializado " + conversacion); }
    @PreDestroy public void destruir() { LOG.info("Portfolio destruido " + conversacion); }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void definirCapital(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(new BigDecimal("999999999999.99")) > 0 || valor.stripTrailingZeros().scale() > 2)
            throw new ReglaNegocioException("Capital inválido: máximo 999999999999.99 y dos decimales");
        capital = valor;
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void definirPorcentaje(TipoInstrumento tipo, BigDecimal valor) {
        if (tipo == null || valor == null || valor.signum() < 0 || valor.compareTo(BigDecimal.valueOf(100)) > 0 || valor.stripTrailingZeros().scale() > 4)
            throw new ReglaNegocioException("Porcentaje inválido");
        BigDecimal total = porcentajes.entrySet().stream().filter(e -> e.getKey() != tipo).map(Map.Entry::getValue).reduce(valor, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) throw new ReglaNegocioException("La distribución supera 100%");
        porcentajes.put(tipo, valor);
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public SimulacionDto calcularSimulacion() {
        SimulacionDto dto = new SimulacionDto(); dto.capital = capital;
        dto.porcentajes = new EnumMap<>(porcentajes); dto.importes = new EnumMap<>(TipoInstrumento.class);
        // Se conservan fracciones de centavo para que la suma nunca exceda el capital.
        porcentajes.forEach((tipo, valor) -> dto.importes.put(tipo, capital.multiply(valor).movePointLeft(2)));
        dto.disponible = capital.subtract(dto.importes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reiniciarSimulacion() { porcentajes.clear(); capital = BigDecimal.ZERO; }
    @Remove @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void cerrar() { reiniciarSimulacion(); }

    public List<PosicionDto> obtenerPosiciones() { return obtenerResumen().posiciones; }
    public ResumenPortfolioDto obtenerResumen() {
        return consolidar(operaciones.porPortfolio(portfolioActual.obtener().getId()));
    }
    // Service Facade: compone persistencia, consolidación y Strategy sin exponer entidades.
    ResumenPortfolioDto consolidar(List<Operacion> movimientos) {
        Map<String, PosicionDto> posiciones = new TreeMap<>();
        for (Operacion o : movimientos) {
            if (o.getTipo() != TipoOperacion.COMPRA)
                throw new ReglaNegocioException("Tipo de operación todavía no soportado por Portfolio");
            Instrumento i = o.getInstrumento();
            PosicionDto p = posiciones.computeIfAbsent(i.getTicker(), ticker -> {
                PosicionDto nueva = new PosicionDto(); nueva.ticker = ticker;
                nueva.nombre = i.getNombre(); nueva.tipo = i.getTipo().name();
                nueva.cantidad = BigDecimal.ZERO; nueva.invertido = BigDecimal.ZERO;
                nueva.precioActual = cotizaciones.cotizar(i);
                nueva.moneda = i.getMonedaCotizacion(); nueva.fechaCotizacion = i.getFechaCotizacion(); return nueva;
            });
            if (o.getOrdenDetalle() != null && !p.moneda.equals(o.getOrdenDetalle().getOrden().getMoneda()))
                throw new ReglaNegocioException("La moneda de cotización no coincide con la moneda de la orden registrada.");
            p.cantidad = p.cantidad.add(o.getCantidad()); p.invertido = p.invertido.add(o.getTotal());
        }
        ResumenPortfolioDto r = new ResumenPortfolioDto();
        r.capitalInvertido = BigDecimal.ZERO; r.patrimonioTotal = BigDecimal.ZERO;
        r.posiciones = new ArrayList<>(posiciones.values());
        r.posiciones.sort(Comparator.comparing(p -> p.nombre));
        for (PosicionDto p : r.posiciones) {
            p.precioPromedio = p.invertido.divide(p.cantidad, 10, RoundingMode.HALF_UP);
            p.actual = p.cantidad.multiply(p.precioActual).setScale(10, RoundingMode.HALF_UP);
            p.rendimiento = p.actual.subtract(p.invertido);
            p.rendimientoPorcentaje = porcentaje(p.rendimiento, p.invertido);
            r.capitalInvertido = r.capitalInvertido.add(p.invertido);
            r.patrimonioTotal = r.patrimonioTotal.add(p.actual);
            TotalesMonedaDto total = r.totalesPorMoneda.computeIfAbsent(p.moneda, m -> new TotalesMonedaDto());
            total.capitalInvertido = total.capitalInvertido.add(p.invertido);
            total.patrimonioTotal = total.patrimonioTotal.add(p.actual);
        }
        r.gananciaTotal = r.patrimonioTotal.subtract(r.capitalInvertido);
        r.rendimientoPorcentaje = porcentaje(r.gananciaTotal, r.capitalInvertido);
        r.totalesPorMoneda.forEach((moneda, total) -> {
            total.gananciaTotal = total.patrimonioTotal.subtract(total.capitalInvertido);
            total.rendimientoPorcentaje = porcentaje(total.gananciaTotal, total.capitalInvertido);
        });
        if (r.totalesPorMoneda.size() == 1) r.moneda = r.totalesPorMoneda.keySet().iterator().next();
        if (r.totalesPorMoneda.size() > 1) {
            r.moneda = null; r.capitalInvertido = null; r.patrimonioTotal = null;
            r.gananciaTotal = null; r.rendimientoPorcentaje = null;
        }
        return r;
    }
    private BigDecimal porcentaje(BigDecimal ganancia, BigDecimal costo) {
        return costo.signum() == 0 ? BigDecimal.ZERO : ganancia.multiply(BigDecimal.valueOf(100)).divide(costo, 4, RoundingMode.HALF_UP);
    }
}
