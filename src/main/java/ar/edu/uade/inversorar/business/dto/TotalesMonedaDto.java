package ar.edu.uade.inversorar.business.dto;
import java.math.BigDecimal;
/** No se suman monedas diferentes sin un tipo de cambio. */
public class TotalesMonedaDto {
    public BigDecimal capitalInvertido = BigDecimal.ZERO;
    public BigDecimal patrimonioTotal = BigDecimal.ZERO;
    public BigDecimal gananciaTotal = BigDecimal.ZERO;
    public BigDecimal rendimientoPorcentaje = BigDecimal.ZERO;
}
