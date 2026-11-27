package ar.edu.uade.inversorar.business.dto;

import java.math.BigDecimal;

/** Posición consolidada, calculada a partir de las operaciones. */
public class PosicionDto {
    public String moneda;
    public String fechaCotizacion;
    /** Origen del precio actual: null (catálogo), "BINANCE" (en vivo, REST) o "RESPALDO" (Binance no respondió: último cierre). */
    public String fuenteCotizacion;
    public BigDecimal precioActual; public BigDecimal rendimientoPorcentaje;
    public String nombre; public String ticker; public String tipo;
    public BigDecimal cantidad; public BigDecimal precioPromedio; public BigDecimal invertido; public BigDecimal actual; public BigDecimal rendimiento;
    /** Valor actual convertido a USD con la misma tasa que el patrimonio total; null si la moneda no tiene conversión. */
    public BigDecimal actualUsd;
}
