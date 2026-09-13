package ar.edu.uade.inversorar.presentation;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.annotation.PreDestroy;
import ar.edu.uade.inversorar.business.SimuladorPortfolioService;
import java.io.Serializable;
/** Una referencia EJB por sesión HTTP; nunca una referencia global compartida. */
@SessionScoped public class SimulacionSesion implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private SimuladorPortfolioService servicio;
    private String propietario;
    public SimuladorPortfolioService servicio(String usuario) {
        if (propietario == null) propietario = usuario;
        if (!propietario.equals(usuario))
            throw new jakarta.ws.rs.ForbiddenException("La simulación pertenece a otra identidad; cierre la sesión");
        return servicio;
    }
    @PreDestroy public void destruir() { servicio.cerrar(); }
}
