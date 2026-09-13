package ar.edu.uade.inversorar.business.dto;
import java.math.BigDecimal;
import java.util.Map;
import ar.edu.uade.inversorar.data.TipoInstrumento;
public class SimulacionDto {
    public BigDecimal capital;
    public BigDecimal disponible;
    public Map<TipoInstrumento, BigDecimal> porcentajes;
    public Map<TipoInstrumento, BigDecimal> importes;
}
