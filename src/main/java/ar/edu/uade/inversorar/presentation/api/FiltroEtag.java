package ar.edu.uade.inversorar.presentation.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * ETag en todas las consultas GET de /api que responden 200 (portfolio, órdenes, instrumentos, catálogo…).
 * Si el cliente manda If-None-Match con el mismo ETag, se responde 304 Not Modified sin cuerpo: el navegador reusa
 * lo que ya tiene. Cache-Control "private, no-cache" obliga a revalidar siempre (los datos cambian y son de cada
 * usuario) y evita que un proxy compartido los guarde.
 */
@Provider
public class FiltroEtag implements ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(FiltroEtag.class.getName());
    private static final Jsonb JSON = JsonbBuilder.create();

    @Override
    public void filter(ContainerRequestContext pedido, ContainerResponseContext respuesta) {
        if (!"GET".equals(pedido.getMethod()) || respuesta.getStatus() != 200 || !respuesta.hasEntity()) return;
        if (respuesta.getHeaders().containsKey(HttpHeaders.ETAG)) return;
        byte[] representacion;
        try {
            Object entidad = respuesta.getEntity();
            representacion = (entidad instanceof String s ? s : JSON.toJson(entidad)).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            LOG.fine("No se pudo calcular el ETag de " + pedido.getUriInfo().getPath() + ": " + e);
            return;
        }
        String etag = Etags.calcular(representacion);
        respuesta.getHeaders().putSingle(HttpHeaders.ETAG, etag);
        respuesta.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "private, no-cache");
        respuesta.getHeaders().putSingle(HttpHeaders.VARY, "Cookie");
        if (Etags.coincide(pedido.getHeaderString(HttpHeaders.IF_NONE_MATCH), etag)) {
            respuesta.setStatus(304);
            respuesta.setEntity(null);
        }
    }
}
