package ar.edu.uade.inversorar.data;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TransicionOrdenInvalida extends IllegalStateException {
    public TransicionOrdenInvalida(EstadoOrden estado, String accion) {
        super("No se puede " + accion + " una orden en estado " + estado + ".");
    }
}
