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
    private static final Comparator<Operacion> POR_FECHA_Y_CARGA = Comparator.comparing(Operacion::getFecha)
            .thenComparing(Operacion::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

    // Service Facade: compone persistencia, consolidación y Strategy sin exponer entidades.
    ResumenPortfolioDto consolidar(List<Operacion> movimientos) {
        Map<String, List<Operacion>> porTicker = new TreeMap<>();
        for (Operacion o : movimientos) porTicker.computeIfAbsent(o.getInstrumento().getTicker(), k -> new ArrayList<>()).add(o);
        List<PosicionDto> posiciones = new ArrayList<>();
        BigDecimal gananciaRealizada = BigDecimal.ZERO;
        for (List<Operacion> grupo : porTicker.values()) {
            grupo.sort(POR_FECHA_Y_CARGA);
            Instrumento i = grupo.get(0).getInstrumento();
            String moneda = i.getMonedaCotizacion();
            BigDecimal cantidad = BigDecimal.ZERO, invertido = BigDecimal.ZERO;
            for (Operacion o : grupo) {
                if (o.getOrdenDetalle() != null && !moneda.equals(o.getOrdenDetalle().getOrden().getMoneda()))
                    throw new ReglaNegocioException("La moneda de cotización no coincide con la moneda de la orden registrada.");
                if (o.getTipo() == TipoOperacion.VENTA) {
                    BigDecimal promedioVigente = cantidad.signum() > 0 ? invertido.divide(cantidad, 10, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    BigDecimal costoVendido = o.getCantidad().multiply(promedioVigente);
                    gananciaRealizada = gananciaRealizada.add(o.getTotal().subtract(costoVendido));
                    invertido = invertido.subtract(costoVendido);
                    cantidad = cantidad.subtract(o.getCantidad());
                } else {
                    cantidad = cantidad.add(o.getCantidad());
                    invertido = invertido.add(o.getTotal());
                }
            }
            if (cantidad.signum() <= 0) continue;
            PosicionDto p = new PosicionDto();
            p.ticker = i.getTicker(); p.nombre = i.getNombre(); p.tipo = i.getTipo().name();
            p.moneda = moneda; p.fechaCotizacion = i.getFechaCotizacion();
            p.cantidad = cantidad; p.invertido = invertido; p.precioActual = cotizaciones.cotizar(i);
            p.precioPromedio = p.invertido.divide(p.cantidad, 10, RoundingMode.HALF_UP);
            p.actual = p.cantidad.multiply(p.precioActual).setScale(10, RoundingMode.HALF_UP);
            p.rendimiento = p.actual.subtract(p.invertido);
            p.rendimientoPorcentaje = porcentaje(p.rendimiento, p.invertido);
            posiciones.add(p);
        }
        posiciones.sort(Comparator.comparing(p -> p.nombre));
        ResumenPortfolioDto r = new ResumenPortfolioDto();
        r.posiciones = posiciones;
        BigDecimal capitalInvertido = BigDecimal.ZERO, patrimonioTotal = BigDecimal.ZERO;
        for (PosicionDto p : posiciones) {
            capitalInvertido = capitalInvertido.add(p.invertido);
            patrimonioTotal = patrimonioTotal.add(p.actual);
            TotalesMonedaDto total = r.totalesPorMoneda.computeIfAbsent(p.moneda, m -> new TotalesMonedaDto());
            total.capitalInvertido = total.capitalInvertido.add(p.invertido);
            total.patrimonioTotal = total.patrimonioTotal.add(p.actual);
        }
        r.capitalInvertido = capitalInvertido; r.patrimonioTotal = patrimonioTotal;
        r.gananciaRealizada = gananciaRealizada;
        r.gananciaTotal = patrimonioTotal.subtract(capitalInvertido).add(gananciaRealizada);
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
