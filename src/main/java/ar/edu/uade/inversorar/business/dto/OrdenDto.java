package ar.edu.uade.inversorar.business.dto;

import ar.edu.uade.inversorar.data.Orden;
import ar.edu.uade.inversorar.data.OrdenDetalle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Una orden tal como la ve el usuario: qué pidió y en qué estado está. */
public class OrdenDto {
    public Long id;
    public String tipo;
    public String estado;
    public String motivo;
    public String ticker;
    public String nombre;
    public BigDecimal cantidad;
    public BigDecimal precioUnitario;
    public BigDecimal total;
    public String moneda;
    public LocalDate fechaOperacion;
    public LocalDateTime cargadaEl;

    public static OrdenDto de(OrdenDetalle detalle) {
        Orden o = detalle.getOrden();
        var dto = new OrdenDto();
        dto.id = o.getId(); dto.tipo = o.getTipo().name(); dto.estado = o.getEstado().name(); dto.motivo = o.getMotivoRechazo();
        dto.ticker = detalle.getInstrumento().getTicker(); dto.nombre = detalle.getInstrumento().getNombre();
        dto.cantidad = detalle.getCantidad(); dto.precioUnitario = detalle.getPrecioUnitario(); dto.total = o.getTotal();
        dto.moneda = o.getMoneda(); dto.fechaOperacion = o.getFechaOperacion(); dto.cargadaEl = o.getFechaHora();
        return dto;
    }
}
