package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.mensajeria.ProductorOrdenes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdenServiceTest {
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    static BigDecimal n(String s) { return new BigDecimal(s); }
    OrdenServiceBean bean = new OrdenServiceBean();
    OrdenRepository ordenes = mock(OrdenRepository.class);
    OperacionRepository operaciones = mock(OperacionRepository.class);
    ProductorOrdenes productor = mock(ProductorOrdenes.class);
    Instrumento btc = new Instrumento("Bitcoin", "BTCUSDT", TipoInstrumento.CRIPTO, n("67000"));
    Portfolio cartera = new Portfolio("Prueba", "ana");
    Orden orden; OrdenDetalle detalle;

    @BeforeEach void preparar() throws Exception {
        inyectar(cartera, "id", 5L); inyectar(cartera, "usuarioId", 7L);
        var actual = mock(PortfolioActual.class); when(actual.obtener()).thenReturn(cartera);
        inyectar(bean, "portfolioActual", actual); inyectar(bean, "ordenes", ordenes);
        inyectar(bean, "operaciones", operaciones); inyectar(bean, "productor", productor);
        usar(Orden.pendiente(7L, TipoOperacion.COMPRA, LocalDate.now(), "USDT", n("33500")), n("0.5"));
    }
    void usar(Orden o, BigDecimal cantidad) throws Exception {
        orden = o; inyectar(orden, "id", 42L);
        detalle = new OrdenDetalle(orden, btc, cantidad, n("67000"));
        when(ordenes.bloquear(42L)).thenReturn(Optional.of(orden)); when(ordenes.detalleDe(42L)).thenReturn(Optional.of(detalle));
    }

    @Test void reintentarUnaOrdenConErrorLaReenviaDesdeLaBase() {
        orden.marcarError("Falla técnica al ejecutar");
        var dto = bean.reintentar(42L);
        assertEquals("PENDIENTE", dto.estado); assertEquals(EstadoOrden.PENDIENTE, orden.getEstado());
        verify(productor).enviar(orden, detalle);
    }
    @Test void unaRechazadaTambienSeEnviaDeNuevo() {
        orden.rechazar("Tenencia insuficiente");
        assertEquals("PENDIENTE", bean.reintentar(42L).estado);
        verify(productor).enviar(orden, detalle);
    }
    @Test void unaPendienteNoSeReenvia() {
        // Segundo clic: la orden ya está PENDIENTE y no se reenvía.
        var e = assertThrows(ReglaNegocioException.class, () -> bean.reintentar(42L));
        assertTrue(e.getMessage().contains("PENDIENTE"), e.getMessage());
        verifyNoInteractions(productor);
    }
    @Test void cancelarUnaPendienteNoEnviaNada() {
        assertEquals("CANCELADA", bean.cancelar(42L).estado);
        verifyNoInteractions(productor);
        // Ya no está pendiente: un segundo intento se rechaza.
        assertThrows(ReglaNegocioException.class, () -> bean.cancelar(42L));
    }
    @Test void eliminarNoReenvia() {
        orden.marcarError("Falla técnica al ejecutar");
        assertEquals("DESCARTADA", bean.descartar(42L).estado);
        verifyNoInteractions(productor);
    }
    @Test void unaVentaSoloSeReenviaSiTodaviaAlcanza() throws Exception {
        // Tenés 96 y ya hay 87 en otra venta pendiente: reenviar una venta rechazada de 89 no se permite.
        var venta = Orden.pendiente(7L, TipoOperacion.VENTA, LocalDate.now(), "USDT", n("1"));
        venta.rechazar("Tenencia insuficiente"); usar(venta, n("89"));
        when(operaciones.porPortfolioYTicker(5L, "BTCUSDT")).thenReturn(List.of(new Operacion(cartera, btc, n("96"), n("60000"), LocalDate.now())));
        when(ordenes.ventasPendientes(7L, btc)).thenReturn(n("87"));
        var e = assertThrows(ReglaNegocioException.class, () -> bean.reintentar(42L));
        assertTrue(e.getMessage().contains("podés vender hasta 9"), e.getMessage());
        assertEquals(EstadoOrden.RECHAZADA, venta.getEstado());
        verifyNoInteractions(productor);
    }
    @Test void laOrdenDeOtroUsuarioNoExiste() throws Exception {
        inyectar(orden, "usuarioId", 99L);
        assertThrows(ReglaNegocioException.class, () -> bean.cancelar(42L));
        assertTrue(bean.orden(42L).isEmpty());
        verifyNoInteractions(productor);
    }
}
