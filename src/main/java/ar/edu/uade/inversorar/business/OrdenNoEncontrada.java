package ar.edu.uade.inversorar.business;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class OrdenNoEncontrada extends RuntimeException {
    public OrdenNoEncontrada() { super("La orden no existe."); }
}
