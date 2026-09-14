package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(
            Long usuarioId,
            String token,
            LocalDateTime fechaExpiracion
    ) {
        this.usuarioId = usuarioId;
        this.token = token;
        this.fechaExpiracion = fechaExpiracion;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public boolean estaVencido() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }
}