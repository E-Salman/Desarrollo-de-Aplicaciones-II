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
                nueva.precioActual = cotizaciones.cotizar(i); return nueva;
            });
            p.cantidad = p.cantidad.add(o.getCantidad()); p.invertido = p.invertido.add(o.getTotal());
        }
        ResumenPortfolioDto r = new ResumenPortfolioDto();
        r.capitalInvertido = BigDecimal.ZERO; r.patrimonioTotal = BigDecimal.ZERO;
        r.posiciones = new ArrayList<>(posiciones.values());
        r.posiciones.sort(Comparator.comparing(p -> p.nombre));
        for (PosicionDto p : r.posiciones) {
            p.precioPromedio = p.invertido.divide(p.cantidad, 4, RoundingMode.HALF_UP);
            p.actual = p.cantidad.multiply(p.precioActual).setScale(4, RoundingMode.HALF_UP);
            p.rendimiento = p.actual.subtract(p.invertido);
            p.rendimientoPorcentaje = porcentaje(p.rendimiento, p.invertido);
            r.capitalInvertido = r.capitalInvertido.add(p.invertido);
            r.patrimonioTotal = r.patrimonioTotal.add(p.actual);
        }
        r.gananciaTotal = r.patrimonioTotal.subtract(r.capitalInvertido);
        r.rendimientoPorcentaje = porcentaje(r.gananciaTotal, r.capitalInvertido);
        return r;
    }
    private BigDecimal porcentaje(BigDecimal ganancia, BigDecimal costo) {
        return costo.signum() == 0 ? BigDecimal.ZERO : ganancia.multiply(BigDecimal.valueOf(100)).divide(costo, 4, RoundingMode.HALF_UP);
    }
}
