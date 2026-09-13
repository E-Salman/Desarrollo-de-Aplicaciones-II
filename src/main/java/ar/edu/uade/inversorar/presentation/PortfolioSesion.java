package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.PortfolioService;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.NoSuchEJBException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ws.rs.ForbiddenException;
import java.io.Serializable;
import java.util.logging.Logger;

/** Una referencia a Portfolio por sesión HTTP, compartida por todos sus recursos REST. */
@SessionScoped
public class PortfolioSesion implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PortfolioSesion.class.getName());
    @EJB private PortfolioService servicio;
    private String propietario;

    public synchronized PortfolioService servicio(String usuario) {
        if (usuario == null || usuario.isBlank()) throw new ForbiddenException("Se requiere una identidad autenticada");
        if (propietario == null) propietario = usuario;
        if (!propietario.equals(usuario))
            throw new ForbiddenException("El portfolio de esta sesión pertenece a otra identidad; cierre la sesión");
        return servicio;
    }

    @PreDestroy public void destruir() {
        if (servicio != null) {
            try { servicio.cerrar(); }
            catch (NoSuchEJBException e) { LOG.fine("La conversación de Portfolio ya fue retirada por el contenedor"); }
        }
    }
}
