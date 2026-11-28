package ar.edu.uade.inversorar.presentation.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * Red de seguridad: si una respuesta de error de /api no salió como Problem Details, la convierte. Cubre los
 * errores que responde RESTEasy con sus propios mappers (por ejemplo, el de Jackson ante un JSON ilegible), que
 * tienen prioridad sobre {@link ErroresMapper} por ser más específicos.
 */
@Provider
@Priority(Priorities.AUTHENTICATION) // los filtros de respuesta corren de mayor a menor prioridad: este, al final
public class FiltroProblemas implements ContainerResponseFilter {
    static final MediaType PROBLEMA = MediaType.valueOf(Problema.TIPO_MEDIO);

    @Override
    public void filter(ContainerRequestContext pedido, ContainerResponseContext respuesta) {
        int status = respuesta.getStatus();
        if (status < 400) return;
        MediaType tipo = respuesta.getMediaType();
        if (tipo != null && tipo.isCompatible(PROBLEMA)) return;
        String instancia = pedido.getUriInfo().getRequestUri().getRawPath();
        Problema problema = status == 400
                ? Problema.de("json-invalido", "Cuerpo del pedido inválido", 400,
                        "El cuerpo del pedido no es un JSON válido para esta operación.", instancia)
                : Problema.http(status, ErrorHttpServlet.detalle(status), instancia);
        respuesta.setEntity(problema.json(), null, PROBLEMA);
    }
}
