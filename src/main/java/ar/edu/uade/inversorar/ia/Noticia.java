package ar.edu.uade.inversorar.ia;

/** Una noticia de la fuente externa. El enlace se muestra tal cual vino de la fuente, nunca uno escrito por el modelo. */
public record Noticia(String titulo, String fuente, String fecha, String url) { }
