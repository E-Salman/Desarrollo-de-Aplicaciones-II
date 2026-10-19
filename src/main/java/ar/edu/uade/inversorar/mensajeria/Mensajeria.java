package ar.edu.uade.inversorar.mensajeria;

/** Nombres JNDI y propiedades JMS compartidos por el productor y los consumidores de órdenes. */
public final class Mensajeria {
    /** Cola punto a punto de órdenes a ejecutar (config/wildfly-messaging.cli). */
    public static final String COLA_ORDENES = "java:/jms/queue/ordenes";
    /** Cola de errores: mensajes de orden que agotaron sus reintentos. */
    public static final String DLQ_ORDENES = "java:/jms/queue/DLQ.ordenes";
    /** Fábrica de conexiones transaccional de WildFly: el envío se confirma junto con la base de datos. */
    public static final String FABRICA_TRANSACCIONAL = "java:/JmsXA";
    /** Propiedades JMS del mensaje, para filtrar y rastrear sin abrir el JSON. */
    public static final String PROPIEDAD_ORDEN_ID = "ordenId";
    public static final String PROPIEDAD_TIPO = "tipo";

    private Mensajeria() { }
}
