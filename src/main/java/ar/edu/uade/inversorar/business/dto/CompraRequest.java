package ar.edu.uade.inversorar.business.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de entrada de la operación de compra. */
public class CompraRequest {
    public String ticker;
    public BigDecimal cantidad;
    public BigDecimal precioUnitario;
    public LocalDate fecha;
}
