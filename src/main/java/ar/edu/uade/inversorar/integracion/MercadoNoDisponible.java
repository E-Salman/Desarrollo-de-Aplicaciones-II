package ar.edu.uade.inversorar.integracion;

/**
 * El mercado legado no respondió a tiempo, no se pudo conectar, tuvo una falla interna (Fault soap:Server)
 * o devolvió un XML inválido. Es una falla técnica, no un rechazo de negocio: es una excepción de sistema
 * a propósito, para que se deshaga la transacción del MDB y el broker reintente (3 veces, después DLQ → ERROR).
 */
public class MercadoNoDisponible extends RuntimeException {
    public MercadoNoDisponible(String mensaje) { super(mensaje); }
    public MercadoNoDisponible(String mensaje, Throwable causa) { super(mensaje, causa); }
}
