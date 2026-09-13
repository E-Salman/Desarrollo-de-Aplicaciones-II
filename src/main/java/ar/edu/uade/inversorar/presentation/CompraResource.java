package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.CompraService;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.business.VentaService;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.VentaRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.servlet.http.HttpServletRequest;

@jakarta.enterprise.context.RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CompraResource {
    @Inject private CompraService compraService;
    @Inject private VentaService ventaService;
    @GET @Path("instrumentos") public Response instrumentos() { return Response.ok(compraService.listarInstrumentos()).build(); }
    @Inject private PortfolioSesion portfolioSesion;
    @Context private HttpServletRequest httpRequest;
    @POST @Path("compras") @Consumes(MediaType.APPLICATION_JSON)
    public Response comprar(CompraRequest request) {
        // Validar la identidad de la conversación antes de persistir la compra.
        httpRequest.getSession(true);
        var portfolioService = portfolioSesion.servicio(httpRequest.getUserPrincipal().getName());
        try { compraService.registrarCompra(request); return Response.status(Response.Status.CREATED).entity(portfolioService.obtenerResumen()).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    @POST @Path("ventas") @Consumes(MediaType.APPLICATION_JSON)
    public Response vender(VentaRequest request) {
        try { ventaService.registrarVenta(request); return Response.status(Response.Status.CREATED).entity(portfolioService.obtenerResumen()).build(); }
        catch (ReglaNegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build(); }
    }
    public static class ErrorDto { public String mensaje; public ErrorDto(String mensaje) { this.mensaje = mensaje; } }
}
