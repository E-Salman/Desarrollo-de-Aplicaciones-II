package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cabecera de una compra registrada. La autenticación sigue a cargo del contenedor. */
@Entity @Table(name = "ordenes")
public class Orden {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(name = "fecha_hora", nullable = false) private LocalDateTime fechaHora;
    @Column(nullable = false, length = 30) private String estado;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(nullable = false, precision = 30, scale = 10) private BigDecimal total;
    protected Orden() { }
    public Orden(Long usuarioId, String moneda, BigDecimal total) {
        this.usuarioId = usuarioId; this.moneda = moneda; this.total = total;
        this.fechaHora = LocalDateTime.now(); this.estado = "COMPLETADA";
    }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getMoneda() { return moneda; }
    public BigDecimal getTotal() { return total; }
    public String getEstado() { return estado; }
}
