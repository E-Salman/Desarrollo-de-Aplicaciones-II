package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "operaciones")
public class Operacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Portfolio portfolio;
    @ManyToOne(optional = false) private Instrumento instrumento;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoOperacion tipo;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal cantidad;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal total;
    @Column(nullable = false) private LocalDate fecha;
    protected Operacion() { }
    public Operacion(Portfolio portfolio, Instrumento instrumento, BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha) {
        this.portfolio = portfolio; this.instrumento = instrumento; this.tipo = TipoOperacion.COMPRA;
        this.cantidad = cantidad; this.precioUnitario = precioUnitario; this.total = cantidad.multiply(precioUnitario).setScale(4, java.math.RoundingMode.HALF_UP); this.fecha = fecha;
    }
    public TipoOperacion getTipo() { return tipo; }
    public Long getId() { return id; } public Portfolio getPortfolio() { return portfolio; }
    public Instrumento getInstrumento() { return instrumento; } public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; } public BigDecimal getTotal() { return total; }
    public LocalDate getFecha() { return fecha; }
}
