package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Instrumento;
import ar.edu.uade.inversorar.data.Operacion;
import ar.edu.uade.inversorar.data.Portfolio;
import ar.edu.uade.inversorar.data.TipoOperacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Estrategia de validación y creación de una operación (Strategy pattern). */
public interface OperacionStrategy {
    TipoOperacion getTipo();
    void validar(Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha, List<Operacion> historial);
    Operacion crear(Portfolio portfolio, Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha);
}
