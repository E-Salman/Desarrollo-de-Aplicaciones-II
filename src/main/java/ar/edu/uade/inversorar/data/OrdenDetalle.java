package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "orden_detalles")
public class OrdenDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "orden_id", nullable = false) private Orden orden;
    @ManyToOne(optional = false) @JoinColumn(name = "instrumento_id", nullable = false) private Instrumento instrumento;
    @Column(nullable = false, precision = 30, scale = 15) private BigDecimal cantidad;
    @Column(name = "precio_unitario", nullable = false, precision = 30, scale = 15) private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 30, scale = 15) private BigDecimal subtotal;
    protected OrdenDetalle() { }
    public OrdenDetalle(Orden orden, Operacion operacion) {
        this.orden = orden; this.instrumento = operacion.getInstrumento();
        this.cantidad = operacion.getCantidad(); this.precioUnitario = operacion.getPrecioUnitario();
        // Mismo importe de negocio, redondeado a diez decimales, en orden, detalle y movimiento.
        this.subtotal = operacion.getTotal();
    }
    /** Detalle de una orden pendiente: la operación todavía no existe, la crea el Ejecutor. */
    public OrdenDetalle(Orden orden, Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario) {
        this.orden = orden; this.instrumento = instrumento; this.cantidad = cantidad; this.precioUnitario = precioUnitario;
        this.subtotal = cantidad.multiply(precioUnitario).setScale(10, java.math.RoundingMode.HALF_UP);
    }
    public Long getId() { return id; }
    public Orden getOrden() { return orden; }
    public Instrumento getInstrumento() { return instrumento; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}
