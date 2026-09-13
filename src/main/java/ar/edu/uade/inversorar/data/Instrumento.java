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
    @Transient private String monedaCotizacion = "USD";
    @Transient private String fechaCotizacion;
    protected Instrumento() { }
    public Instrumento(String nombre, String ticker, TipoInstrumento tipo, BigDecimal cotizacionActual) {
        this.nombre = nombre; this.ticker = ticker; this.tipo = tipo; this.cotizacionActual = cotizacionActual;
    }
    public Long getId() { return id; } public String getNombre() { return nombre; }
    public String getTicker() { return ticker; } public TipoInstrumento getTipo() { return tipo; }
    public BigDecimal getCotizacionActual() { return cotizacionActual; }
    public String getMonedaCotizacion() { return monedaCotizacion; }
    public String getFechaCotizacion() { return fechaCotizacion; }
    public void actualizarCotizacion(BigDecimal precio, String moneda, String fecha) {
        cotizacionActual=precio; monedaCotizacion=moneda; fechaCotizacion=fecha;
    }
    public static Instrumento deMercado(ar.edu.uade.inversorar.business.dto.InstrumentoMercado dato) {
        var i=new Instrumento(dato.nombre,dato.simbolo,TipoInstrumento.valueOf(dato.tipo),dato.cierre);
        i.id=dato.id; i.actualizarCotizacion(dato.cierre,dato.moneda,dato.fecha); return i;
    }
}
