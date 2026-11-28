package ar.edu.uade.inversorar.presentation.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** Arma la respuesta JAX-RS de un {@link Problema}. Todos los errores de /api pasan por acá. */
public final class Problemas {
    private Problemas() { }

    public static Response respuesta(Problema problema) {
        // Se envía el JSON ya armado: el proveedor de String escribe cualquier tipo de medio, incluido problem+json.
        return Response.status(problema.status).type(Problema.TIPO_MEDIO).entity(problema.json()).build();
    }

    public static Response respuesta(String codigo, String titulo, int status, String detalle, UriInfo uri) {
        return respuesta(Problema.de(codigo, titulo, status, detalle, instancia(uri)));
    }

    public static String instancia(UriInfo uri) {
        return uri == null ? null : uri.getRequestUri().getRawPath();
    }
}
