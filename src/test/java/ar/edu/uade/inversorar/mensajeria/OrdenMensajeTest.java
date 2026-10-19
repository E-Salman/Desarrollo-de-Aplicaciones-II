package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.data.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class OrdenMensajeTest {
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    OrdenMensaje mensaje() throws Exception {
        var btc = new Instrumento("Bitcoin", "BTCUSDT", TipoInstrumento.CRIPTO, new BigDecimal("67000"));
        var orden = Orden.pendiente(7L, TipoOperacion.COMPRA, LocalDate.of(2026, 10, 17), "USDT", new BigDecimal("33500"));
        inyectar(orden, "id", 42L);
        return OrdenMensaje.desde(orden, new OrdenDetalle(orden, btc, new BigDecimal("0.5"), new BigDecimal("67000")));
    }

    @Test void seArmaDesdeLaOrdenGuardada() throws Exception {
        var m = mensaje();
        assertEquals(42L, m.ordenId); assertEquals("COMPRA", m.tipo); assertEquals(7L, m.usuarioId);
        assertEquals("BTCUSDT", m.ticker); assertEquals(LocalDate.of(2026, 10, 17), m.fecha);
        assertEquals(OrdenMensaje.VERSION_ACTUAL, m.version); assertNotNull(m.enviadoEn);
    }
    @Test void idaYVueltaPorJson() throws Exception {
        var json = mensaje().toJson();
        assertTrue(json.contains("\"ordenId\":42"), json);
        var leido = OrdenMensaje.fromJson(json);
        assertEquals(42L, leido.ordenId); assertEquals("BTCUSDT", leido.ticker);
        assertEquals(0, new BigDecimal("0.5").compareTo(leido.cantidad));
        assertEquals(0, new BigDecimal("67000").compareTo(leido.precioUnitario));
        assertEquals(LocalDate.of(2026, 10, 17), leido.fecha);
    }
    @Test void rechazaMensajesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> OrdenMensaje.fromJson("no es json"));
        assertThrows(IllegalArgumentException.class, () -> OrdenMensaje.fromJson("{\"version\":1}"));
        assertThrows(IllegalArgumentException.class, () -> OrdenMensaje.fromJson("{\"version\":2,\"ordenId\":42}"));
    }
}
