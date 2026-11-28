package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.OrdenNoEncontrada;
import ar.edu.uade.inversorar.presentation.api.Problemas;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** La orden no existe o es de otro usuario (no se distingue, para no revelar órdenes ajenas) → 404. */
@Provider
public class OrdenNoEncontradaMapper implements ExceptionMapper<OrdenNoEncontrada> {
    @Context private UriInfo uri;
    @Override public Response toResponse(OrdenNoEncontrada error) {
        return Problemas.respuesta("orden-no-encontrada", "Orden no encontrada", 404, error.getMessage(), uri);
    }
}
