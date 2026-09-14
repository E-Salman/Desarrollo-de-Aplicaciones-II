package ar.edu.uade.inversorar.presentation;
import ar.edu.uade.inversorar.business.PortfolioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import ar.edu.uade.inversorar.business.dto.*;
import java.util.List;
@jakarta.enterprise.context.RequestScoped
@Path("portfolio") @Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {
    @Inject private PortfolioSesion sesion;
    @Context private HttpServletRequest request;
    private PortfolioService servicio() {
        request.getSession(true);
        return sesion.servicio(request.getUserPrincipal().getName());
    }
    @DELETE @Path("sesion") public Response cerrar() {
        if (request.getSession(false) != null) {
            servicio(); // Verifica la identidad antes de cerrar la conversación.
            request.getSession(false).invalidate();
        }
        return Response.noContent().build();
    }
    @GET @Path("resumen") public ResumenPortfolioDto resumen() { return servicio().obtenerResumen(); }
    @GET @Path("posiciones") public List<PosicionDto> posiciones() { return servicio().obtenerPosiciones(); }
}
