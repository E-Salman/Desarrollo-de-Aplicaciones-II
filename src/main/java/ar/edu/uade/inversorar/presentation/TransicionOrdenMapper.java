package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.data.TransicionOrdenInvalida;
import ar.edu.uade.inversorar.presentation.api.Problemas;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Cambio de estado no permitido (por ejemplo, cancelar una orden ya completada) → 409. */
@Provider
public class TransicionOrdenMapper implements ExceptionMapper<TransicionOrdenInvalida> {
    @Context private UriInfo uri;
    @Override public Response toResponse(TransicionOrdenInvalida error) {
        return Problemas.respuesta("transicion-invalida", "Cambio de estado no permitido", 409, error.getMessage(), uri);
    }
}
