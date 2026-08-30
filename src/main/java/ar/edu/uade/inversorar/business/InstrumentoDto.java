package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Instrumento;
import java.math.BigDecimal;

public class InstrumentoDto {
    public String nombre; public String ticker; public String tipo; public BigDecimal cotizacionActual;
    public InstrumentoDto() { }
    public InstrumentoDto(Instrumento i) { nombre = i.getNombre(); ticker = i.getTicker(); tipo = i.getTipo().name(); cotizacionActual = i.getCotizacionActual(); }
}
