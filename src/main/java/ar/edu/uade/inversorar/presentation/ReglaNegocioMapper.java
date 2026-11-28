package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.presentation.api.Problemas;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Una regla de negocio incumplida (cantidad inválida, tenencia insuficiente…) → 400 Problem Details. */
@Provider
public class ReglaNegocioMapper implements ExceptionMapper<ReglaNegocioException> {
    @Context private UriInfo uri;
    @Override public Response toResponse(ReglaNegocioException e) {
        return Problemas.respuesta("regla-de-negocio", "Regla de negocio incumplida", 400, e.getMessage(), uri);
    }
}
