package ar.edu.uade.inversorar.presentation.api;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Último recurso: cualquier error de /api que no tenga un mapper propio sale como Problem Details.
 * <ul>
 *   <li>WebApplicationException (404 de ruta inexistente, 405, 415, BadRequest de los recursos…): conserva su estado.</li>
 *   <li>JSON ilegible en el cuerpo → 400.</li>
 *   <li>Acceso denegado por un EJB (@RolesAllowed) → 403.</li>
 *   <li>Cualquier otra cosa → 500 sin detalles internos; el detalle va al log con un identificador.</li>
 * </ul>
 */
@Provider
public class ErroresMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(ErroresMapper.class.getName());
    @Context private UriInfo uri;

    @Override
    public Response toResponse(Throwable error) {
        String instancia = Problemas.instancia(uri);
        if (error instanceof WebApplicationException w && w.getResponse() != null && w.getResponse().getStatus() < 500) {
            int status = w.getResponse().getStatus();
            Response.ResponseBuilder r = Response.fromResponse(Problemas.respuesta(Problema.http(status, mensajePara(status, w), instancia)));
            String permitidos = w.getResponse().getHeaderString("Allow");
            if (permitidos != null) r.header("Allow", permitidos);
            return r.build();
        }
        if (esJsonIlegible(error))
            return Problemas.respuesta(Problema.de("json-invalido", "Cuerpo JSON inválido", 400,
                    "El cuerpo del pedido no es un JSON válido para esta operación.", instancia));
        if (causa(error, EJBAccessException.class))
            return Problemas.respuesta(Problema.http(403, "Tu cuenta no tiene permiso para esta operación.", instancia));

        String id = UUID.randomUUID().toString().substring(0, 8);
        LOG.log(Level.SEVERE, "Error no controlado en " + instancia + " (id " + id + ")", error);
        return Problemas.respuesta(Problema.http(500, "Ocurrió un error inesperado. Si se repite, informá el código " + id + ".", instancia));
    }

    private static String mensajePara(int status, WebApplicationException w) {
        return switch (status) {
            case 404 -> w.getClass() == jakarta.ws.rs.NotFoundException.class && esMensajePropio(w) ? w.getMessage() : "El recurso pedido no existe.";
            case 405 -> "El método HTTP no está permitido en este recurso.";
            case 406 -> "No se puede responder en el formato pedido; usar application/json.";
            case 415 -> "El tipo de contenido no es soportado; enviar application/json.";
            default -> esMensajePropio(w) ? w.getMessage() : Problema.titulo(status) + ".";
        };
    }

    /** Los mensajes por defecto de JAX-RS ("HTTP 404 Not Found", "RESTEASY003210…") no se muestran al usuario. */
    private static boolean esMensajePropio(WebApplicationException w) {
        String m = w.getMessage();
        return m != null && !m.startsWith("HTTP ") && !m.startsWith("RESTEASY");
    }

    private static boolean esJsonIlegible(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ProcessingException) return true;
            String nombre = t.getClass().getName();
            if (nombre.startsWith("jakarta.json.") || nombre.endsWith("JsonbException")) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }

    private static boolean causa(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }
}
