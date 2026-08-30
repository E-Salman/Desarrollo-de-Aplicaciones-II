package ar.edu.uade.inversorar.business;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CompraRequest {
    public String ticker;
    public BigDecimal cantidad;
    public BigDecimal precioUnitario;
    public LocalDate fecha;
}
