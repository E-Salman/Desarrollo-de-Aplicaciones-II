package ar.edu.uade.inversorar.ia;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.logging.Logger;

/**
 * Arma el {@link Asistente} una vez por aplicación. Si el servidor tiene la variable de entorno
 * {@value ProveedorGemini#VARIABLE_CLAVE}, usa Gemini; si no, el proveedor simulado (tests, CI, demo sin clave).
 * La clave nunca se registra en el log ni sale del servidor.
 */
@ApplicationScoped
public class AsistenteGateway {
    private static final Logger LOG = Logger.getLogger(AsistenteGateway.class.getName());
    static final String GUIA = "/ia/guia.md";

    private Asistente asistente;

    @PostConstruct void iniciar() {
        String clave = System.getenv(ProveedorGemini.VARIABLE_CLAVE);
        ProveedorIA proveedor;
        if (clave != null && !clave.isBlank()) {
            var gemini = new ProveedorGemini(clave);
            LOG.info("Asistente de IA con Gemini (modelo " + gemini.modelo() + ")");
            proveedor = gemini;
        } else {
            LOG.info("Asistente de IA con el proveedor simulado: no está definida " + ProveedorGemini.VARIABLE_CLAVE);
            proveedor = new ProveedorSimulado();
        }
        asistente = new Asistente(proveedor, new NoticiasGoogle(), guia(), Clock.systemUTC());
    }

    public Asistente asistente() { return asistente; }

    static String guia() {
        try (InputStream in = AsistenteGateway.class.getResourceAsStream(GUIA)) {
            if (in == null) throw new IllegalStateException("Falta " + GUIA);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + GUIA, e);
        }
    }
}
