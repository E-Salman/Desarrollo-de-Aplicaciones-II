package ar.edu.uade.inversorar.business;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String message) { super(message); }
}
