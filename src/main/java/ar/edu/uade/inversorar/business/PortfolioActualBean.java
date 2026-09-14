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
    @EJB private UsuarioRepository usuarios;
    public Portfolio obtener() {
        if (!contexto.isCallerInRole("USUARIO")) throw new EJBAccessException("Usuario no autorizado");
        String usuario = contexto.getCallerPrincipal().getName();
        Usuario cuenta = usuarios.buscarPorEmail(usuario)
                .orElseThrow(() -> new EJBAccessException("La identidad no tiene una cuenta registrada"));
        // El vínculo explícito conserva carteras con un principal anterior.
        var vinculada = portfolios.porUsuarioId(cuenta.getId());
        if (vinculada.isPresent()) return vinculada.get();
        Portfolio portfolio = portfolios.porPropietario(usuario).orElseGet(() -> {
            Portfolio nuevo = new Portfolio("Mi Portfolio", usuario);
            portfolios.guardar(nuevo);
            return nuevo;
        });
        if (portfolio.getUsuarioId() != null && !portfolio.getUsuarioId().equals(cuenta.getId()))
            throw new EJBAccessException("La cartera pertenece a otra cuenta");
        portfolio.vincularUsuario(cuenta.getId());
        return portfolio;
    }
}
