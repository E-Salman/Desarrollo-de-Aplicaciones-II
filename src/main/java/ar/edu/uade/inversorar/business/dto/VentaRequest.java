package ar.edu.uade.inversorar.business.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de entrada de la operación de venta. */
public class VentaRequest {
    public String ticker;
    public BigDecimal cantidad;
    public BigDecimal precioUnitario;
    public LocalDate fecha;
}
