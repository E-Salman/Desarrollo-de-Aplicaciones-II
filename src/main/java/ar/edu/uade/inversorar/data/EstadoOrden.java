package ar.edu.uade.inversorar.data;

/** Estados persistidos por nombre. Cada estado define las transiciones que admite. */
public enum EstadoOrden {
    PENDIENTE {
        @Override EstadoOrden completar() { return COMPLETADA; }
        @Override EstadoOrden rechazar() { return RECHAZADA; }
        @Override EstadoOrden marcarError() { return ERROR; }
        @Override EstadoOrden cancelar() { return CANCELADA; }
    },
    COMPLETADA,
    RECHAZADA {
        @Override EstadoOrden reintentar() { return PENDIENTE; }
        @Override EstadoOrden descartar() { return DESCARTADA; }
        @Override boolean fallo() { return true; }
    },
    ERROR {
        @Override EstadoOrden reintentar() { return PENDIENTE; }
        @Override EstadoOrden descartar() { return DESCARTADA; }
        @Override boolean fallo() { return true; }
    },
    CANCELADA,
    DESCARTADA;

    EstadoOrden completar() { throw invalida("completar"); }
    EstadoOrden rechazar() { throw invalida("rechazar"); }
    EstadoOrden marcarError() { throw invalida("marcar como error"); }
    EstadoOrden cancelar() { throw invalida("cancelar"); }
    EstadoOrden reintentar() { throw invalida("reintentar"); }
    EstadoOrden descartar() { throw invalida("descartar"); }
    boolean fallo() { return false; }

    private TransicionOrdenInvalida invalida(String accion) {
        return new TransicionOrdenInvalida(this, accion);
    }
}
