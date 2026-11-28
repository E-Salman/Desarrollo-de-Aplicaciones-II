package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.CatalogoMercadoRepository;
import ar.edu.uade.inversorar.ia.Asistente;
import ar.edu.uade.inversorar.ia.AsistenteGateway;
import ar.edu.uade.inversorar.ia.ReglasAsistente;
import ar.edu.uade.inversorar.ia.RespuestaAsistente;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * Caso de uso del asistente de IA. Valida el pedido, resuelve el activo en el catálogo (las noticias solo se buscan
 * para activos del catálogo) y delega en el gateway. Sin transacción: la consulta al modelo puede tardar hasta 5 s
 * y no hay nada que escribir.
 */
@Stateless
@RolesAllowed("USUARIO")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AsistenteServiceBean {
    @Inject private AsistenteGateway gateway;
    @Inject private CatalogoMercadoRepository catalogo;
    @Inject private ConfiguracionApp configuracion;

    public RespuestaAsistente preguntar(String modo, String pregunta, String activo) {
        try {
            Asistente asistente = gateway.asistente();
            if (modo == null || modo.equals("ayuda")) return asistente.ayuda(pregunta);
            if (!modo.equals("noticias")) throw new IllegalArgumentException("Modo inválido: usá ayuda o noticias.");
            String buscado = ReglasAsistente.activo(activo);
            Asistente.Activo encontrado = configuracion.catalogoMysql()
                    ? catalogo.activoParaNoticias(buscado).map(r -> new Asistente.Activo(r[0], r[1])).orElse(null)
                    : null;
            return asistente.noticias(pregunta, encontrado);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
    }

    /** GEMINI o SIMULADO, para que la pantalla avise cuando no hay IA real. */
    public String proveedor() { return gateway.asistente().proveedor(); }
}
