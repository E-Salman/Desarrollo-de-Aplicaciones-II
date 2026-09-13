package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Instrumento;
import ar.edu.uade.inversorar.data.Operacion;
import ar.edu.uade.inversorar.data.Portfolio;
import ar.edu.uade.inversorar.data.TipoOperacion;
import jakarta.enterprise.context.Dependent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Dependent
public class VentaStrategy implements OperacionStrategy {

    @Override
    public TipoOperacion getTipo() { return TipoOperacion.VENTA; }

    @Override
    public void validar(Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha, List<Operacion> historial) {
        if (cantidad == null || cantidad.signum() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (precioUnitario == null || precioUnitario.signum() <= 0) throw new ReglaNegocioException("El precio unitario debe ser mayor que cero.");
        if (fecha == null || fecha.isAfter(LocalDate.now())) throw new ReglaNegocioException("La fecha de venta es inválida.");
        BigDecimal poseida = cantidadPoseida(historial);
        if (poseida.signum() <= 0) throw new ReglaNegocioException("No tenés ese instrumento en tu portfolio.");
        if (cantidad.compareTo(poseida) > 0) throw new ReglaNegocioException("No podés vender más cantidad de la que tenés en cartera.");
    }

    @Override
    public Operacion crear(Portfolio portfolio, Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha) {
        return new Operacion(portfolio, instrumento, TipoOperacion.VENTA, cantidad, precioUnitario, fecha);
    }

    private BigDecimal cantidadPoseida(List<Operacion> historial) {
        BigDecimal poseida = BigDecimal.ZERO;
        for (Operacion o : historial) {
            poseida = o.getTipo() == TipoOperacion.VENTA ? poseida.subtract(o.getCantidad()) : poseida.add(o.getCantidad());
        }
        return poseida;
    }
}
