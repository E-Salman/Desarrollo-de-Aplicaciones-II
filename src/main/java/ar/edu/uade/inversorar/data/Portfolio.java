package ar.edu.uade.inversorar.data;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nombre;
    @Column(unique = true) private String propietario;
    @Column(name = "usuario_id", unique = true) private Long usuarioId;
    protected Portfolio() { }
    public Portfolio(String nombre) { this.nombre = nombre; }
    public Portfolio(String nombre, String propietario) { this.nombre = nombre; this.propietario = propietario; }
    public String getPropietario() { return propietario; }
    public Long getUsuarioId() { return usuarioId; }
    public void vincularUsuario(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
