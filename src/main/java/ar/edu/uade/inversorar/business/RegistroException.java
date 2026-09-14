package ar.edu.uade.inversorar.business;

import jakarta.ejb.ApplicationException;

@ApplicationException
public class RegistroException extends RuntimeException {
    public RegistroException(String message) { super(message); }
}
