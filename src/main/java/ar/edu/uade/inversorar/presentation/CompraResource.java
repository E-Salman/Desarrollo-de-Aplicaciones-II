package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.CompraService;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.enterprise.context.RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CompraResource {
    @Inject private CompraService compraService;
    @GET @Path("instrumentos") public Response instrumentos() { return Response.ok(compraService.listarInstrumentos()).build(); }
    @Inject private ar.edu.uade.inversorar.business.PortfolioService portfolioService;
    @POST @Path("compras") @Consumes(MediaType.APPLICATION_JSON)
    public Response comprar(CompraRequest request) {
        try { compraService.registrarCompra(request); return Response.status(Response.Status.CREATED).entity(portfolioService.obtenerResumen()).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    public static class ErrorDto { public String mensaje; public ErrorDto(String mensaje) { this.mensaje = mensaje; } }
}
