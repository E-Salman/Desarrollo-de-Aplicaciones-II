package ar.edu.uade.inversorar.data;

/**
 * Ciclo de vida de una orden ejecutada por mensajería. COMPLETADA conserva el nombre que ya tienen las
 * órdenes existentes (sin migración). ERROR = el mensaje agotó sus reintentos y terminó en la DLQ.
 * CANCELADA = el usuario la canceló mientras esperaba en la cola; DESCARTADA = el usuario eliminó una
 * orden rechazada o con error (no se borra: queda en el historial).
 */
public enum EstadoOrden { PENDIENTE, COMPLETADA, RECHAZADA, ERROR, CANCELADA, DESCARTADA }
