package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.PortfolioService;
import ar.edu.uade.inversorar.business.dto.SimulacionDto;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;

/** Funcionalidad de planificación del componente Portfolio. */
@RequestScoped
@Path("portfolio/simulacion") @Produces(MediaType.APPLICATION_JSON)
public class SimulacionPortfolioResource {
    @Inject private PortfolioSesion sesion;
    @Context private HttpServletRequest request;
    private PortfolioService servicio() {
        request.getSession(true);
        return sesion.servicio(request.getUserPrincipal().getName());
    }
    @GET public SimulacionDto calcular() { return servicio().calcularSimulacion(); }
    @PUT @Path("capital") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto capital(Valor entrada) {
        var portfolio = servicio();
        portfolio.definirCapital(entrada == null ? null : entrada.valor);
        return portfolio.calcularSimulacion();
    }
    @PUT @Path("porcentajes/{tipo}") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto porcentaje(@PathParam("tipo") TipoInstrumento tipo, Valor entrada) {
        var portfolio = servicio();
        portfolio.definirPorcentaje(tipo, entrada == null ? null : entrada.valor);
        return portfolio.calcularSimulacion();
    }
    @DELETE public Response reiniciar() {
        servicio().reiniciarSimulacion();
        return Response.noContent().build();
    }
    public static class Valor { public BigDecimal valor; }
}
