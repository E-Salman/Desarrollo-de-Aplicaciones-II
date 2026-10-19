package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Cabecera de una compra o venta. Con mensajería nace PENDIENTE y la completa o rechaza el Ejecutor;
 * las transiciones solo se permiten desde el estado indicado en cada método (idempotencia).
 */
@Entity @Table(name = "ordenes")
public class Orden {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TipoOperacion tipo;
    @Column(name = "fecha_hora", nullable = false) private LocalDateTime fechaHora;
    @Column(name = "fecha_operacion") private LocalDate fechaOperacion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EstadoOrden estado;
    @Column(name = "motivo_rechazo", length = 255) private String motivoRechazo;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(nullable = false, precision = 30, scale = 10) private BigDecimal total;
    protected Orden() { }
    /** Compra registrada en el momento (flujo síncrono anterior a la mensajería). */
    public Orden(Long usuarioId, String moneda, BigDecimal total) {
        this(usuarioId, TipoOperacion.COMPRA, LocalDate.now(), moneda, total, EstadoOrden.COMPLETADA);
    }
    private Orden(Long usuarioId, TipoOperacion tipo, LocalDate fechaOperacion, String moneda, BigDecimal total, EstadoOrden estado) {
        this.usuarioId = usuarioId; this.tipo = tipo; this.fechaOperacion = fechaOperacion;
        this.moneda = moneda; this.total = total; this.estado = estado; this.fechaHora = LocalDateTime.now();
    }
    /** Orden aceptada que espera al Ejecutor de órdenes. */
    public static Orden pendiente(Long usuarioId, TipoOperacion tipo, LocalDate fechaOperacion, String moneda, BigDecimal total) {
        return new Orden(usuarioId, tipo, fechaOperacion, moneda, total, EstadoOrden.PENDIENTE);
    }

    private static final Set<EstadoOrden> FALLIDAS = EnumSet.of(EstadoOrden.RECHAZADA, EstadoOrden.ERROR);

    public void completar() { pasar(EnumSet.of(EstadoOrden.PENDIENTE), EstadoOrden.COMPLETADA, null); }
    public void rechazar(String motivo) { pasar(EnumSet.of(EstadoOrden.PENDIENTE), EstadoOrden.RECHAZADA, motivo); }
    public void marcarError(String motivo) { pasar(EnumSet.of(EstadoOrden.PENDIENTE), EstadoOrden.ERROR, motivo); }
    /** El usuario la cancela mientras espera en la cola: el Ejecutor la va a encontrar no pendiente y la descarta. */
    public void cancelar() { pasar(EnumSet.of(EstadoOrden.PENDIENTE), EstadoOrden.CANCELADA, "Cancelada por el usuario"); }
    /** El usuario vuelve a enviar a la cola una orden rechazada o con error. */
    public void reintentar() { pasar(FALLIDAS, EstadoOrden.PENDIENTE, null); }
    /** El usuario elimina una orden rechazada o con error; se conserva el motivo. */
    public void descartar() { pasar(FALLIDAS, EstadoOrden.DESCARTADA, motivoRechazo); }
    private void pasar(Set<EstadoOrden> desde, EstadoOrden hacia, String motivo) {
        if (!desde.contains(estado)) throw new IllegalStateException("La orden " + id + " está " + estado + ", no " + desde + ".");
        estado = hacia;
        motivoRechazo = motivo == null ? null : motivo.length() > 255 ? motivo.substring(0, 255) : motivo;
    }

    public boolean estaPendiente() { return estado == EstadoOrden.PENDIENTE; }
    public boolean fallo() { return FALLIDAS.contains(estado); }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoOperacion getTipo() { return tipo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public LocalDate getFechaOperacion() { return fechaOperacion; }
    public EstadoOrden getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public String getMoneda() { return moneda; }
    public BigDecimal getTotal() { return total; }
}
