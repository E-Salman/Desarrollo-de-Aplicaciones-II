package ar.edu.uade.inversorar.ia;

/** El proveedor se negó a responder (filtros de seguridad propios del modelo). */
public class RespuestaBloqueada extends Exception {
    public RespuestaBloqueada(String motivo) { super(motivo); }
}
