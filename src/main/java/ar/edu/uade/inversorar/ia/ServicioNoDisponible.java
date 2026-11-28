package ar.edu.uade.inversorar.ia;

/** El proveedor de IA o la fuente de noticias no respondió a tiempo o respondió con error. Activa el fallback. */
public class ServicioNoDisponible extends Exception {
    public ServicioNoDisponible(String mensaje) { super(mensaje); }
    public ServicioNoDisponible(String mensaje, Throwable causa) { super(mensaje, causa); }
}
