package ar.edu.uade.inversorar.data;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class OrdenTest {
    Orden pendiente() { return Orden.pendiente(7L, TipoOperacion.VENTA, LocalDate.of(2026, 9, 3), "USDT", new BigDecimal("100")); }

    @Test void nacePendienteConTipoYFechaElegida() {
        var o = pendiente();
        assertEquals(EstadoOrden.PENDIENTE, o.getEstado()); assertTrue(o.estaPendiente());
        assertEquals(TipoOperacion.VENTA, o.getTipo()); assertEquals(LocalDate.of(2026, 9, 3), o.getFechaOperacion());
        assertNotNull(o.getFechaHora()); assertNull(o.getMotivoRechazo());
    }
    @Test void laCompraSincronaSigueNaciendoCompletada() {
        var o = new Orden(7L, "USD", new BigDecimal("10"));
        assertEquals(EstadoOrden.COMPLETADA, o.getEstado()); assertEquals(TipoOperacion.COMPRA, o.getTipo());
    }
    @Test void completarSoloUnaVez() {
        var o = pendiente(); o.completar();
        assertEquals(EstadoOrden.COMPLETADA, o.getEstado());
        // Un mensaje duplicado no puede volver a ejecutarla.
        assertThrows(IllegalStateException.class, o::completar);
        assertThrows(IllegalStateException.class, () -> o.rechazar("x"));
    }
    @Test void rechazoGuardaElMotivo() {
        var o = pendiente(); o.rechazar("Tenencia insuficiente: tenés 2, querías vender 8.");
        assertEquals(EstadoOrden.RECHAZADA, o.getEstado()); assertTrue(o.getMotivoRechazo().startsWith("Tenencia insuficiente"));
    }
    @Test void errorReintentarYDescartar() {
        var o = pendiente(); o.marcarError("Falla técnica al ejecutar");
        assertEquals(EstadoOrden.ERROR, o.getEstado());
        o.reintentar();
        assertEquals(EstadoOrden.PENDIENTE, o.getEstado()); assertNull(o.getMotivoRechazo());
        // Un segundo clic en Reintentar no encuentra una orden en ERROR.
        assertThrows(IllegalStateException.class, o::reintentar);
        o.marcarError("Falla técnica al ejecutar"); o.descartar();
        assertEquals(EstadoOrden.DESCARTADA, o.getEstado()); assertEquals("Falla técnica al ejecutar", o.getMotivoRechazo());
    }
    @Test void unaPendienteNoSeDescartaNiSeReintenta() {
        assertThrows(IllegalStateException.class, () -> pendiente().descartar());
        assertThrows(IllegalStateException.class, () -> pendiente().reintentar());
    }
    @Test void unaRechazadaSePuedeEnviarDeNuevoOEliminar() {
        var o = pendiente(); o.rechazar("Tenencia insuficiente");
        o.reintentar(); assertEquals(EstadoOrden.PENDIENTE, o.getEstado());
        o.rechazar("Tenencia insuficiente"); o.descartar();
        assertEquals(EstadoOrden.DESCARTADA, o.getEstado()); assertEquals("Tenencia insuficiente", o.getMotivoRechazo());
    }
    @Test void soloSeCancelaUnaPendiente() {
        var o = pendiente(); o.cancelar();
        assertEquals(EstadoOrden.CANCELADA, o.getEstado());
        // El Ejecutor, al recibir el mensaje, ya no la puede completar.
        assertThrows(IllegalStateException.class, o::completar);
        assertThrows(IllegalStateException.class, o::cancelar);
        assertThrows(IllegalStateException.class, o::reintentar);
    }
    @Test void motivoLargoSeRecortaAlTamanioDeLaColumna() {
        var o = pendiente(); o.rechazar("x".repeat(400));
        assertEquals(255, o.getMotivoRechazo().length());
    }
}
