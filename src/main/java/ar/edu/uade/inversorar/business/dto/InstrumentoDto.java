package ar.edu.uade.inversorar.business.dto;

import ar.edu.uade.inversorar.data.Instrumento;
import java.math.BigDecimal;

/** Representación segura de un instrumento para la capa de presentación. */
public class InstrumentoDto {
    public String nombre; public String ticker; public String tipo; public BigDecimal cotizacionActual;
    public String moneda; public String fechaCotizacion;
    public InstrumentoDto() { }
    public InstrumentoDto(Instrumento i) { nombre = i.getNombre(); ticker = i.getTicker(); tipo = i.getTipo().name(); cotizacionActual = i.getCotizacionActual(); moneda=i.getMonedaCotizacion(); fechaCotizacion=i.getFechaCotizacion(); }
}
