package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.OrdenService;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.business.dto.OrdenDto;
import ar.edu.uade.inversorar.data.EstadoOrden;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Órdenes del usuario (pop-up de transacciones): consultar, cancelar pendientes y recuperar rechazadas. */
@jakarta.enterprise.context.RequestScoped
@Path("ordenes")
@Produces(MediaType.APPLICATION_JSON)
public class OrdenResource {
    @Inject private OrdenService ordenes;

    /** ?estado=PENDIENTE o varios separados por coma (?estado=RECHAZADA,ERROR); sin estado, todas. */
    @GET
    public List<OrdenDto> listar(@QueryParam("estado") String estado, @QueryParam("limite") @DefaultValue("20") int limite) {
        return ordenes.misOrdenes(estados(estado), limite);
    }

    /** Cancela una orden que todavía espera en la cola. */
    @POST @Path("{id}/cancelar")
    public OrdenDto cancelar(@PathParam("id") long id) { return ordenes.cancelar(id); }

    @GET @Path("{id}")
    public Response una(@PathParam("id") long id) {
        return ordenes.orden(id).map(o -> Response.ok(o).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).entity(new CompraResource.ErrorDto("La orden no existe.")).build());
    }

    /** 202: la orden vuelve a la cola y se ejecuta después. */
    @POST @Path("{id}/reintentar")
    public Response reintentar(@PathParam("id") long id) {
        return Response.status(Response.Status.ACCEPTED).entity(ordenes.reintentar(id)).build();
    }

    @POST @Path("{id}/descartar")
    public OrdenDto descartar(@PathParam("id") long id) { return ordenes.descartar(id); }

    private static Set<EstadoOrden> estados(String valor) {
        Set<EstadoOrden> estados = EnumSet.noneOf(EstadoOrden.class);
        if (valor == null || valor.isBlank()) return estados;
        for (String parte : valor.split(",")) {
            try { estados.add(EstadoOrden.valueOf(parte.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException e) { throw new ReglaNegocioException("Estado de orden inválido: " + parte.trim()); }
        }
        return estados;
    }
}
