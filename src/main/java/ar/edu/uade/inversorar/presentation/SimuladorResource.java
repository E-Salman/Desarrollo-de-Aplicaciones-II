package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.dto.SimulacionDto;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Alias HTTP anterior; delega en la misma conversación del componente Portfolio. */
@Deprecated
@RequestScoped
@Path("simulador") @Produces(MediaType.APPLICATION_JSON)
public class SimuladorResource {
    @Inject private SimulacionPortfolioResource delegado;
    @GET public SimulacionDto calcular() { return delegado.calcular(); }
    @PUT @Path("capital") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto capital(SimulacionPortfolioResource.Valor entrada) { return delegado.capital(entrada); }
    @PUT @Path("porcentajes/{tipo}") @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto porcentaje(@PathParam("tipo") TipoInstrumento tipo, SimulacionPortfolioResource.Valor entrada) { return delegado.porcentaje(tipo, entrada); }
    @DELETE public Response reiniciar() { return delegado.reiniciar(); }
}
