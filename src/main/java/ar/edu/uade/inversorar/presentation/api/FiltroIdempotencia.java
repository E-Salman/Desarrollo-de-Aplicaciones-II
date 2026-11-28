package ar.edu.uade.inversorar.presentation.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Idempotency-Key en los POST marcados con {@link Idempotente} (compras, ventas y reenvío de órdenes).
 * <ul>
 *   <li>Sin el encabezado, el pedido se procesa como siempre.</li>
 *   <li>Primera vez con una clave: se procesa y se guarda la respuesta (status, cuerpo y Location).</li>
 *   <li>Misma clave y mismo pedido: se devuelve la respuesta guardada, con {@code Idempotent-Replayed: true},
 *       sin volver a crear la orden.</li>
 *   <li>Misma clave mientras el primero sigue procesándose → 409. Misma clave con otro cuerpo u otra ruta → 422.</li>
 *   <li>Si el primero terminó en 5xx, la clave se libera para poder reintentar.</li>
 * </ul>
 * Las claves son por usuario: la de otro usuario no choca ni se puede reutilizar.
 */
@Provider
@Idempotente
@Priority(Priorities.USER)
public class FiltroIdempotencia implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String ENCABEZADO = "Idempotency-Key";
    public static final String REPETIDA = "Idempotent-Replayed";
    private static final String PROPIEDAD = FiltroIdempotencia.class.getName() + ".clave";
    private static final Pattern CLAVE_VALIDA = Pattern.compile("[A-Za-z0-9._:-]{8,100}");
    static final RegistroIdempotencia REGISTRO = new RegistroIdempotencia(Clock.systemUTC());

    @Override
    public void filter(ContainerRequestContext pedido) throws IOException {
        String clave = pedido.getHeaderString(ENCABEZADO);
        if (clave == null) return;
        String instancia = pedido.getUriInfo().getRequestUri().getRawPath();
        if (!CLAVE_VALIDA.matcher(clave).matches()) {
            pedido.abortWith(Problemas.respuesta(Problema.de("idempotency-key-invalida", "Idempotency-Key inválida", 400,
                    "La Idempotency-Key debe tener entre 8 y 100 caracteres (letras, números, punto, guion, dos puntos o guion bajo).", instancia)));
            return;
        }
        byte[] cuerpo = pedido.hasEntity() ? pedido.getEntityStream().readAllBytes() : new byte[0];
        pedido.setEntityStream(new ByteArrayInputStream(cuerpo)); // el recurso lo vuelve a leer
        var usuario = pedido.getSecurityContext() == null ? null : pedido.getSecurityContext().getUserPrincipal();
        String ambito = (usuario == null ? "-" : usuario.getName()) + "\n" + clave;
        String huella = huella(pedido.getMethod() + " " + instancia, cuerpo);

        var resultado = REGISTRO.reservar(ambito, huella);
        switch (resultado.estado()) {
            case NUEVA -> pedido.setProperty(PROPIEDAD, ambito);
            case EN_CURSO -> pedido.abortWith(Problemas.respuesta(Problema.de("idempotencia-en-curso", "Pedido en curso", 409,
                    "Ya se está procesando un pedido con esta Idempotency-Key. Esperá su respuesta.", instancia)));
            case CUERPO_DISTINTO -> pedido.abortWith(Problemas.respuesta(Problema.de("idempotencia-cuerpo-distinto",
                    "Idempotency-Key reutilizada", 422,
                    "Esta Idempotency-Key ya se usó con otro pedido. Generá una clave nueva para una operación distinta.", instancia)));
            case REPETIDA -> {
                var g = resultado.respuesta();
                Response.ResponseBuilder r = Response.status(g.status()).entity(g.entidad()).header(REPETIDA, "true");
                if (g.tipo() != null) r.type(g.tipo());
                if (g.location() != null) r.header("Location", g.location());
                pedido.abortWith(r.build());
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext pedido, ContainerResponseContext respuesta) {
        Object ambito = pedido.getProperty(PROPIEDAD);
        if (!(ambito instanceof String clave)) return; // sin clave, o respuesta repetida/abortada
        if (respuesta.getStatus() >= 500) { REGISTRO.liberar(clave); return; }
        var tipo = respuesta.getMediaType();
        REGISTRO.completar(clave, new RegistroIdempotencia.Guardada(respuesta.getStatus(), respuesta.getEntity(),
                tipo == null ? null : tipo.toString(), respuesta.getHeaderString("Location")));
    }

    static String huella(String metodoYRuta, byte[] cuerpo) {
        try {
            var sha = MessageDigest.getInstance("SHA-256");
            sha.update(metodoYRuta.getBytes(StandardCharsets.UTF_8));
            sha.update((byte) 0);
            sha.update(cuerpo);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sha.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
