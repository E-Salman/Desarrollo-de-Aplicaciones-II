package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.business.EjecutorOrdenes;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumidoresTest {
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    EjecutorOrdenes ejecutor = mock(EjecutorOrdenes.class);

    TextMessage texto(String json) throws Exception {
        var m = mock(TextMessage.class); when(m.getBody(String.class)).thenReturn(json); return m;
    }

    @Test void elMdbEjecutaLaOrdenDelMensaje() throws Exception {
        var mdb = new EjecutorOrdenesMDB(); inyectar(mdb, "ejecutor", ejecutor);
        mdb.onMessage(texto("{\"version\":1,\"ordenId\":42,\"tipo\":\"COMPRA\"}"));
        verify(ejecutor).ejecutar(argThat(m -> m.ordenId == 42L));
    }
    @Test void unMensajeIlegibleSeDescartaSinReintentar() throws Exception {
        var mdb = new EjecutorOrdenesMDB(); inyectar(mdb, "ejecutor", ejecutor);
        assertDoesNotThrow(() -> mdb.onMessage(texto("no es json")));
        verifyNoInteractions(ejecutor);
    }
    @Test void laDlqMarcaErrorConElIdDeLaPropiedad() throws Exception {
        var mdb = new OrdenesFallidasMDB(); inyectar(mdb, "ejecutor", ejecutor);
        var m = texto("ilegible"); when(m.propertyExists("ordenId")).thenReturn(true); when(m.getLongProperty("ordenId")).thenReturn(42L);
        mdb.onMessage(m);
        verify(ejecutor).marcarError(42L, OrdenesFallidasMDB.MOTIVO);
    }
    @Test void laDlqUsaElJsonSiFaltaLaPropiedad() throws Exception {
        var mdb = new OrdenesFallidasMDB(); inyectar(mdb, "ejecutor", ejecutor);
        mdb.onMessage(texto("{\"version\":1,\"ordenId\":7}"));
        verify(ejecutor).marcarError(7L, OrdenesFallidasMDB.MOTIVO);
    }
}
