package ar.edu.uade.inversorar.business;
import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.*;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import java.math.*;
import java.util.*;
@Stateless @RolesAllowed("USUARIO")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PortfolioServiceBean implements PortfolioService {
    @Inject private PortfolioActual portfolioActual;
    @Inject private OperacionRepository operaciones;
    @Inject private CotizacionStrategy cotizaciones;
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
        BigDecimal capitalInvertido = BigDecimal.ZERO, patrimonioTotal = BigDecimal.ZERO, gananciaRealizada = BigDecimal.ZERO;
        for (List<Operacion> grupo : porTicker.values()) {
            grupo.sort(POR_FECHA_Y_CARGA);
            Instrumento i = grupo.get(0).getInstrumento();
            BigDecimal cantidad = BigDecimal.ZERO, invertido = BigDecimal.ZERO;
            for (Operacion o : grupo) {
                if (o.getTipo() == TipoOperacion.VENTA) {
                    BigDecimal promedioVigente = cantidad.signum() > 0 ? invertido.divide(cantidad, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
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
            p.cantidad = cantidad; p.invertido = invertido; p.precioActual = cotizaciones.cotizar(i);
            p.precioPromedio = p.invertido.divide(p.cantidad, 4, RoundingMode.HALF_UP);
            p.actual = p.cantidad.multiply(p.precioActual).setScale(4, RoundingMode.HALF_UP);
            p.rendimiento = p.actual.subtract(p.invertido);
            p.rendimientoPorcentaje = porcentaje(p.rendimiento, p.invertido);
            posiciones.add(p);
            capitalInvertido = capitalInvertido.add(p.invertido);
            patrimonioTotal = patrimonioTotal.add(p.actual);
        }
        posiciones.sort(Comparator.comparing(p -> p.nombre));
        ResumenPortfolioDto r = new ResumenPortfolioDto();
        r.capitalInvertido = capitalInvertido; r.patrimonioTotal = patrimonioTotal;
        r.gananciaRealizada = gananciaRealizada;
        r.posiciones = posiciones;
        r.gananciaTotal = r.patrimonioTotal.subtract(r.capitalInvertido).add(gananciaRealizada);
        r.rendimientoPorcentaje = porcentaje(r.gananciaTotal, r.capitalInvertido);
        return r;
    }
    private BigDecimal porcentaje(BigDecimal ganancia, BigDecimal costo) {
        return costo.signum() == 0 ? BigDecimal.ZERO : ganancia.multiply(BigDecimal.valueOf(100)).divide(costo, 4, RoundingMode.HALF_UP);
    }
}
