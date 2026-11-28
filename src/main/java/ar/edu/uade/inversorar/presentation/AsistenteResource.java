package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.AsistenteServiceBean;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.ia.RespuestaAsistente;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Gateway de IA expuesto a la pantalla: {@code POST /api/asistente} con {@code {modo, pregunta, activo}}.
 * Modo "ayuda" (uso de la app y glosario) o "noticias" (activo del catálogo). Límite propio de pedidos por minuto
 * en {@link ar.edu.uade.inversorar.presentation.api.FiltroLimitePedidos}.
 */
@RequestScoped
@Path("asistente")
@Produces(MediaType.APPLICATION_JSON)
public class AsistenteResource {
    @Inject private AsistenteServiceBean asistente;

    public static class Pedido {
        public String modo;
        public String pregunta;
        public String activo;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public RespuestaAsistente preguntar(Pedido pedido) {
        if (pedido == null) throw new ReglaNegocioException("Falta el cuerpo del pedido.");
        return asistente.preguntar(pedido.modo, pedido.pregunta, pedido.activo);
    }

    @GET
    @Path("estado")
    public Map<String, String> estado() { return Map.of("proveedor", asistente.proveedor()); }
}
