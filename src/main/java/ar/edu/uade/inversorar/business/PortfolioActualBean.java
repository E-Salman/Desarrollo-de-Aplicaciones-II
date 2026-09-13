package ar.edu.uade.inversorar.business;
import ar.edu.uade.inversorar.data.*;
import jakarta.ejb.*;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
@Stateless @RolesAllowed("USUARIO")
public class PortfolioActualBean implements PortfolioActual {
    @Resource private SessionContext contexto;
    @Inject private PortfolioRepository portfolios;
    public Portfolio obtener() {
        if (!contexto.isCallerInRole("USUARIO")) throw new EJBAccessException("Usuario no autorizado");
        String usuario = contexto.getCallerPrincipal().getName();
        return portfolios.porPropietario(usuario).orElseGet(() -> {
            Portfolio nuevo = new Portfolio("Mi Portfolio", usuario);
            portfolios.guardar(nuevo);
            return nuevo;
        });
    }
}
