package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.business.EjecutorOrdenes;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumidor de la cola "ordenes". Procesa de a un mensaje (maxSession = 1): orden FIFO y sin dos ventas
 * del mismo usuario ejecutándose a la vez. Corre en una transacción del contenedor junto con el acuse del
 * mensaje: si el Ejecutor lanza una excepción, se deshace todo y el broker reintenta (3 veces cada 5 s,
 * después DLQ.ordenes). Un mensaje ilegible no se reintenta: reintentarlo no lo arregla.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Mensajeria.COLA_ORDENES),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
})
public class EjecutorOrdenesMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(EjecutorOrdenesMDB.class.getName());
    @Inject private EjecutorOrdenes ejecutor;

    @jakarta.annotation.PostConstruct public void iniciar() { LOG.info("EjecutorOrdenesMDB inicializado"); }
    @jakarta.annotation.PreDestroy public void destruir() { LOG.info("EjecutorOrdenesMDB destruido"); }

    @Override
    public void onMessage(Message mensaje) {
        OrdenMensaje orden;
        try {
            orden = OrdenMensaje.fromJson(mensaje.getBody(String.class));
        } catch (IllegalArgumentException | MessageFormatException e) {
            LOG.log(Level.SEVERE, "Mensaje de orden inválido, se descarta: " + e.getMessage());
            return;
        } catch (JMSException e) {
            throw new EJBException("No se pudo leer el mensaje de la cola ordenes", e);
        }
        LOG.info("Recibida " + orden + " (entrega " + entrega(mensaje) + ")");
        ejecutor.ejecutar(orden);
    }

    private static String entrega(Message mensaje) {
        try { return String.valueOf(mensaje.getIntProperty("JMSXDeliveryCount")); }
        catch (JMSException e) { return "?"; }
    }
}
