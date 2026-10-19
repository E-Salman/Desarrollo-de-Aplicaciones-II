package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.data.Orden;
import ar.edu.uade.inversorar.data.OrdenDetalle;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.*;
import java.util.logging.Logger;

/**
 * Envía a la cola "ordenes" el aviso de una orden PENDIENTE. Usa la fábrica transaccional de WildFly
 * (JmsXA): el envío participa de la misma transacción que guardó la orden, así que el broker recién lo
 * entrega cuando la transacción se confirma, y si la transacción se deshace el mensaje no sale.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class ProductorOrdenes {
    private static final Logger LOG = Logger.getLogger(ProductorOrdenes.class.getName());
    @Resource(lookup = Mensajeria.FABRICA_TRANSACCIONAL) private ConnectionFactory fabrica;
    @Resource(lookup = Mensajeria.COLA_ORDENES) private Queue cola;

    public void enviar(Orden orden, OrdenDetalle detalle) {
        OrdenMensaje mensaje = OrdenMensaje.desde(orden, detalle);
        try (JMSContext contexto = fabrica.createContext()) {
            TextMessage texto = contexto.createTextMessage(mensaje.toJson());
            texto.setLongProperty(Mensajeria.PROPIEDAD_ORDEN_ID, orden.getId());
            texto.setStringProperty(Mensajeria.PROPIEDAD_TIPO, orden.getTipo().name());
            texto.setJMSCorrelationID(String.valueOf(orden.getId()));
            contexto.createProducer().setDeliveryMode(DeliveryMode.PERSISTENT).send(cola, texto);
        } catch (JMSException | JMSRuntimeException e) {
            // Sin mensaje no hay orden: la excepción deshace también la orden guardada.
            throw new EJBException("No se pudo enviar la orden #" + orden.getId() + " a la cola", e);
        }
        LOG.info(mensaje + " enviada a la cola ordenes; se entrega al confirmar la transacción");
    }
}
