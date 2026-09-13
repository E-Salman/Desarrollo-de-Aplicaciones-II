package ar.edu.uade.inversorar.business.dto;

import java.math.BigDecimal;
import java.util.List;

/** Respuesta de resumen consumida por JSP y API REST. */
public class ResumenPortfolioDto {
    public String moneda;
    public java.util.Map<String, TotalesMonedaDto> totalesPorMoneda = new java.util.TreeMap<>();
    public BigDecimal rendimientoPorcentaje;
    public BigDecimal capitalInvertido; public BigDecimal patrimonioTotal; public BigDecimal gananciaTotal;
    public List<PosicionDto> posiciones;
}
