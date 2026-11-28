package ar.edu.uade.inversorar.ia;

import java.time.Duration;
import java.util.List;

/** Fuente de noticias recientes. La implementación real es {@link NoticiasGoogle}. */
public interface FuenteNoticias {
    /** Noticias recientes sobre el tema, de la más relevante a la menos; lista vacía si no hay. */
    List<Noticia> buscar(String tema, Duration plazo) throws ServicioNoDisponible;
}
