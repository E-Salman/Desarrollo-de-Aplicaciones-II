package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CompraResource {
    @Inject private CompraService compraService;
    @GET @Path("instrumentos") public Response instrumentos() { return Response.ok(compraService.listarInstrumentos()).build(); }
    @GET @Path("portfolios/{id}/resumen") public Response resumen(@PathParam("id") Long id) { return Response.ok(compraService.obtenerResumen(id)).build(); }
    @POST @Path("compras") @Consumes(MediaType.APPLICATION_JSON)
    public Response comprar(CompraRequest request) {
        try { compraService.registrarCompra(request); return Response.status(Response.Status.CREATED).entity(compraService.obtenerResumen(1L)).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    public static class ErrorDto { public String mensaje; public ErrorDto(String mensaje) { this.mensaje = mensaje; } }
}
