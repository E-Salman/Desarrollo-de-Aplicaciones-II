package ar.edu.uade.inversorar.business;
import java.math.BigDecimal;
import ar.edu.uade.inversorar.data.Instrumento;
public interface CotizacionStrategy { BigDecimal cotizar(Instrumento instrumento); }
