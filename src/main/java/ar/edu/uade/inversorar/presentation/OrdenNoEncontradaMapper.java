package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.OrdenNoEncontrada;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrdenNoEncontradaMapper implements ExceptionMapper<OrdenNoEncontrada> {
    @Override
    public Response toResponse(OrdenNoEncontrada error) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new CompraResource.ErrorDto(error.getMessage()))
                .build();
    }
}
