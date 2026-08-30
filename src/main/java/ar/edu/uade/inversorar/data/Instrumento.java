package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "instrumentos", uniqueConstraints = @UniqueConstraint(columnNames = "ticker"))
public class Instrumento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false, updatable = false) private String ticker;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoInstrumento tipo;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal cotizacionActual;
    protected Instrumento() { }
    public Instrumento(String nombre, String ticker, TipoInstrumento tipo, BigDecimal cotizacionActual) {
        this.nombre = nombre; this.ticker = ticker; this.tipo = tipo; this.cotizacionActual = cotizacionActual;
    }
    public Long getId() { return id; } public String getNombre() { return nombre; }
    public String getTicker() { return ticker; } public TipoInstrumento getTipo() { return tipo; }
    public BigDecimal getCotizacionActual() { return cotizacionActual; }
}
