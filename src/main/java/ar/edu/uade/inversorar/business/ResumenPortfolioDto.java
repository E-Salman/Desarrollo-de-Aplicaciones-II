package ar.edu.uade.inversorar.business;

import java.math.BigDecimal;
import java.util.List;

public class ResumenPortfolioDto {
    public BigDecimal capitalInvertido; public BigDecimal patrimonioTotal; public BigDecimal gananciaTotal;
    public List<PosicionDto> posiciones;
}
