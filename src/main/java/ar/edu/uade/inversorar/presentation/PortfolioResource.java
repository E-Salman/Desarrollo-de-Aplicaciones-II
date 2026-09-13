package ar.edu.uade.inversorar.presentation;
import ar.edu.uade.inversorar.business.PortfolioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ar.edu.uade.inversorar.business.dto.*;
import java.util.List;
@jakarta.enterprise.context.RequestScoped
@Path("portfolio") @Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {
    @Inject private PortfolioService servicio;
    @GET @Path("resumen") public ResumenPortfolioDto resumen() { return servicio.obtenerResumen(); }
    @GET @Path("posiciones") public List<PosicionDto> posiciones() { return servicio.obtenerPosiciones(); }
}
