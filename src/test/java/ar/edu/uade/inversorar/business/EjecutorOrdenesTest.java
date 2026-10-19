package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.EjecutorOrdenes.Resultado;
import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.mensajeria.OrdenMensaje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EjecutorOrdenesTest {
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    static BigDecimal n(String s) { return new BigDecimal(s); }
    EjecutorOrdenes ejecutor = new EjecutorOrdenes();
    OrdenRepository ordenes = mock(OrdenRepository.class);
    PortfolioRepository portfolios = mock(PortfolioRepository.class);
    OperacionRepository operaciones = mock(OperacionRepository.class);
    Instrumento cat = new Instrumento("1000CAT/USDC", "1000CATUSDC", TipoInstrumento.CRIPTO, n("0.0018"));
    Portfolio cartera = new Portfolio("Prueba", "ana");
    Orden orden; OrdenDetalle detalle;

    @BeforeEach void preparar() throws Exception {
        inyectar(ejecutor, "ordenes", ordenes); inyectar(ejecutor, "portfolios", portfolios); inyectar(ejecutor, "operaciones", operaciones);
        inyectar(cartera, "id", 5L); inyectar(cartera, "usuarioId", 7L);
        when(portfolios.porUsuarioId(7L)).thenReturn(Optional.of(cartera));
    }
    OrdenMensaje orden(TipoOperacion tipo, String cantidad) throws Exception {
        orden = Orden.pendiente(7L, tipo, LocalDate.of(2026, 10, 1), "USDC", n(cantidad).multiply(n("0.0018"))); inyectar(orden, "id", 30L);
        detalle = new OrdenDetalle(orden, cat, n(cantidad), n("0.0018"));
        when(ordenes.bloquear(30L)).thenReturn(Optional.of(orden)); when(ordenes.detalleDe(30L)).thenReturn(Optional.of(detalle));
        return OrdenMensaje.desde(orden, detalle);
    }
    void tengo(String cantidad) {
        when(operaciones.porPortfolioYTicker(5L, "1000CATUSDC")).thenReturn(List.of(new Operacion(cartera, cat, n(cantidad), n("0.0018"), LocalDate.now())));
    }

    @Test void compraCreaLaOperacionVinculadaYCompleta() throws Exception {
        assertEquals(Resultado.COMPLETADA, ejecutor.ejecutar(orden(TipoOperacion.COMPRA, "654")));
        var captor = ArgumentCaptor.forClass(Operacion.class); verify(operaciones).guardar(captor.capture());
        var op = captor.getValue();
        assertEquals(TipoOperacion.COMPRA, op.getTipo()); assertSame(detalle, op.getOrdenDetalle()); assertSame(cartera, op.getPortfolio());
        assertEquals(LocalDate.of(2026, 10, 1), op.getFecha(), "usa la fecha que eligió el usuario");
        assertEquals(EstadoOrden.COMPLETADA, orden.getEstado());
    }
    @Test void ventaConTenenciaSuficienteCompleta() throws Exception {
        tengo("96");
        assertEquals(Resultado.COMPLETADA, ejecutor.ejecutar(orden(TipoOperacion.VENTA, "87")));
        verify(operaciones).guardar(argThat(op -> op.getTipo() == TipoOperacion.VENTA));
    }
    @Test void ventaSinTenenciaSeRechazaConMotivoYSinOperacion() throws Exception {
        tengo("9");
        assertEquals(Resultado.RECHAZADA, ejecutor.ejecutar(orden(TipoOperacion.VENTA, "89")));
        assertEquals(EstadoOrden.RECHAZADA, orden.getEstado());
        assertEquals("Tenencia insuficiente: tenés 9 de 1000CATUSDC y la orden quería vender 89.", orden.getMotivoRechazo());
        verify(operaciones, never()).guardar(any());
    }
    @Test void unaOrdenCanceladaNoSeEjecuta() throws Exception {
        var mensaje = orden(TipoOperacion.VENTA, "89"); orden.cancelar();
        assertEquals(Resultado.IGNORADA, ejecutor.ejecutar(mensaje));
        assertEquals(EstadoOrden.CANCELADA, orden.getEstado()); verify(operaciones, never()).guardar(any());
    }
    @Test void unMensajeDuplicadoNoEjecutaDosVeces() throws Exception {
        var mensaje = orden(TipoOperacion.COMPRA, "10");
        assertEquals(Resultado.COMPLETADA, ejecutor.ejecutar(mensaje));
        assertEquals(Resultado.IGNORADA, ejecutor.ejecutar(mensaje));
        verify(operaciones, times(1)).guardar(any());
    }
    @Test void unaOrdenInexistenteSeIgnora() {
        var mensaje = new OrdenMensaje(); mensaje.ordenId = 999L;
        when(ordenes.bloquear(999L)).thenReturn(Optional.empty());
        assertEquals(Resultado.IGNORADA, ejecutor.ejecutar(mensaje));
        verifyNoInteractions(operaciones);
    }
    @Test void laDlqSoloMarcaErrorUnaOrdenPendiente() throws Exception {
        orden(TipoOperacion.COMPRA, "10");
        assertTrue(ejecutor.marcarError(30L, "Falla técnica"));
        assertEquals(EstadoOrden.ERROR, orden.getEstado());
        // Ya no está pendiente: un segundo aviso de la DLQ no la toca.
        assertFalse(ejecutor.marcarError(30L, "Falla técnica"));
    }
}
