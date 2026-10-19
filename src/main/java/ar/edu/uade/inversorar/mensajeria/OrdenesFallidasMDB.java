package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.business.EjecutorOrdenes;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.*;
import java.util.logging.Logger;

/**
 * Consumidor de "DLQ.ordenes": mensajes de orden que fallaron 3 veces por un error técnico. Marca la
 * orden ERROR para que el usuario la vea en Transacciones y decida si la envía de nuevo o la elimina.
 * Si MySQL sigue caído, este MDB también falla y el broker lo reintenta hasta poder marcarla.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Mensajeria.DLQ_ORDENES),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
})
public class OrdenesFallidasMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(OrdenesFallidasMDB.class.getName());
    static final String MOTIVO = "Falla técnica al ejecutar: la orden agotó sus reintentos.";
    @Inject private EjecutorOrdenes ejecutor;

    @Override
    public void onMessage(Message mensaje) {
        Long ordenId = ordenId(mensaje);
        if (ordenId == null) { LOG.severe("Mensaje en DLQ.ordenes sin ordenId; se descarta"); return; }
        ejecutor.marcarError(ordenId, MOTIVO);
    }

    /** El id sale de la propiedad JMS; si faltara, del JSON. */
    static Long ordenId(Message mensaje) {
        try {
            if (mensaje.propertyExists(Mensajeria.PROPIEDAD_ORDEN_ID)) return mensaje.getLongProperty(Mensajeria.PROPIEDAD_ORDEN_ID);
            return OrdenMensaje.fromJson(mensaje.getBody(String.class)).ordenId;
        } catch (JMSException | IllegalArgumentException e) {
            return null;
        }
    }
}
