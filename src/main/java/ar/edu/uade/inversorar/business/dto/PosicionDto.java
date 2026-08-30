package ar.edu.uade.inversorar.business.dto;

import java.math.BigDecimal;

/** Posición consolidada, calculada a partir de las operaciones. */
public class PosicionDto {
    public String nombre; public String ticker; public String tipo;
    public BigDecimal cantidad; public BigDecimal precioPromedio; public BigDecimal invertido; public BigDecimal actual; public BigDecimal rendimiento;
}
