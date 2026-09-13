package ar.edu.uade.inversorar.presentation;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.servlet.http.HttpServletRequest;
import ar.edu.uade.inversorar.business.dto.SimulacionDto;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import java.math.BigDecimal;
@jakarta.enterprise.context.RequestScoped
@Path("simulador") @Produces(MediaType.APPLICATION_JSON)
public class SimuladorResource {
    @Inject private SimulacionSesion sesion;
    @Context private HttpServletRequest request;
    private ar.edu.uade.inversorar.business.SimuladorPortfolioService servicio() {
        request.getSession(true);
        return sesion.servicio(request.getUserPrincipal().getName());
    }
    @GET public SimulacionDto calcular() { return servicio().calcular(); }
    @PUT @Path("capital") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto capital(Valor entrada) { servicio().definirCapital(entrada == null ? null : entrada.valor); return calcular(); }
    @PUT @Path("porcentajes/{tipo}") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto porcentaje(@PathParam("tipo") TipoInstrumento tipo, Valor entrada) { servicio().definirPorcentaje(tipo, entrada == null ? null : entrada.valor); return calcular(); }
    @DELETE public Response cerrar() { if (request.getSession(false) != null) request.getSession(false).invalidate(); return Response.noContent().build(); }
    public static class Valor { public BigDecimal valor; }
}
