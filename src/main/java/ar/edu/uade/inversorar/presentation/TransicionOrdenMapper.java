package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.data.TransicionOrdenInvalida;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransicionOrdenMapper implements ExceptionMapper<TransicionOrdenInvalida> {
    @Override
    public Response toResponse(TransicionOrdenInvalida error) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new CompraResource.ErrorDto(error.getMessage()))
                .build();
    }
}
