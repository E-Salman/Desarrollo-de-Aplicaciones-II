package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.OperacionService;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.VentaRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class OperacionResource {
    @Inject private OperacionService operacionService;
    @GET @Path("instrumentos") public Response instrumentos() { return Response.ok(operacionService.listarInstrumentos()).build(); }
    @GET @Path("portfolios/{id}/resumen") public Response resumen(@PathParam("id") Long id) { return Response.ok(operacionService.obtenerResumen(id)).build(); }
    @POST @Path("compras") @Consumes(MediaType.APPLICATION_JSON)
    public Response comprar(CompraRequest request) {
        try { operacionService.registrarCompra(request); return Response.status(Response.Status.CREATED).entity(operacionService.obtenerResumen(1L)).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    @POST @Path("ventas") @Consumes(MediaType.APPLICATION_JSON)
    public Response vender(VentaRequest request) {
        try { operacionService.registrarVenta(request); return Response.status(Response.Status.CREATED).entity(operacionService.obtenerResumen(1L)).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    public static class ErrorDto { public String mensaje; public ErrorDto(String mensaje) { this.mensaje = mensaje; } }
}
