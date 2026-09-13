package ar.edu.uade.inversorar.presentation;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import jakarta.ws.rs.ext.*;
import jakarta.ws.rs.core.*;
@Provider public class ReglaNegocioMapper implements ExceptionMapper<ReglaNegocioException> {
    public Response toResponse(ReglaNegocioException e) { return Response.status(400).entity(new CompraResource.ErrorDto(e.getMessage())).build(); }
}
