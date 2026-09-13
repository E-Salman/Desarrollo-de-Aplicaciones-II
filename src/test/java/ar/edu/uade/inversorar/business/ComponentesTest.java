package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.*;
import ar.edu.uade.inversorar.data.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentesTest {
    static BigDecimal n(String s) { return new BigDecimal(s); }
    static void igual(String esperado, BigDecimal actual) { assertEquals(0, n(esperado).compareTo(actual)); }
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    Instrumento apple = new Instrumento("Apple", "AAPL", TipoInstrumento.ACCION, n("195"));
    Portfolio cartera = new Portfolio("Prueba", "ana");
    Operacion compra(String cantidad, String precio) { return new Operacion(cartera, apple, n(cantidad), n(precio), LocalDate.now()); }
    PortfolioServiceBean portfolio() throws Exception {
        var bean = new PortfolioServiceBean(); inyectar(bean, "cotizaciones", new CotizacionCatalogo()); return bean;
    }
    @Test void promedioPonderadoYGanancia() throws Exception {
        var r = portfolio().consolidar(List.of(compra("10", "180"), compra("5", "210")));
        igual("2850", r.capitalInvertido); igual("2925", r.patrimonioTotal); igual("75", r.gananciaTotal);
        var p = r.posiciones.get(0); igual("190", p.precioPromedio); igual("15", p.cantidad); igual("2.6316", p.rendimientoPorcentaje);
    }
    @Test void vacioYPerdida() throws Exception {
        var r = portfolio().consolidar(List.of()); assertTrue(r.posiciones.isEmpty()); igual("0", r.rendimientoPorcentaje);
        igual("-5", portfolio().consolidar(List.of(compra("1", "200"))).gananciaTotal);
    }
    @Test void strategyEsIntercambiable() throws Exception {
        var bean = portfolio(); inyectar(bean, "cotizaciones", (CotizacionStrategy) i -> n("220"));
        igual("40", bean.consolidar(List.of(compra("2", "200"))).gananciaTotal);
    }
    @Test void portfolioConsultaSoloIdentidadResuelta() throws Exception {
        var bean = portfolio(); var actual = mock(PortfolioActual.class); var repo = mock(OperacionRepository.class);
        var propio = mock(Portfolio.class); when(propio.getId()).thenReturn(42L); when(actual.obtener()).thenReturn(propio);
        when(repo.porPortfolio(42L)).thenReturn(List.of()); inyectar(bean,"portfolioActual",actual); inyectar(bean,"operaciones",repo);
        bean.obtenerResumen(); verify(repo).porPortfolio(42L); verifyNoMoreInteractions(repo);
    }
    CompraRequest request() { var r = new CompraRequest(); r.ticker=" aapl "; r.cantidad=n("2"); r.precioUnitario=n("180"); r.fecha=LocalDate.now(); return r; }
    CompraServiceBean compraBean(OperacionRepository repo) throws Exception {
        var bean=new CompraServiceBean(); var instrumentos=mock(InstrumentoRepository.class); var actual=mock(PortfolioActual.class);
        when(instrumentos.buscarPorTicker("AAPL")).thenReturn(Optional.of(apple)); when(actual.obtener()).thenReturn(cartera);
        inyectar(bean,"instrumentos",instrumentos); inyectar(bean,"portfolioActual",actual); inyectar(bean,"operaciones",repo); return bean;
    }
    @Test void compraGuardaTotalYPortfolioActual() throws Exception {
        var repo=mock(OperacionRepository.class); compraBean(repo).registrarCompra(request());
        var captor=org.mockito.ArgumentCaptor.forClass(Operacion.class); verify(repo).guardar(captor.capture());
        igual("360",captor.getValue().getTotal()); assertSame(cartera,captor.getValue().getPortfolio());
    }
    @Test void compraRechazaEntradasInvalidasSinPersistir() throws Exception {
        var repo=mock(OperacionRepository.class); var bean=compraBean(repo);
        assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(null));
        var cero=request(); cero.cantidad=BigDecimal.ZERO; assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(cero));
        var futuro=request(); futuro.fecha=LocalDate.now().plusDays(1); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(futuro));
        var desconocido=request(); desconocido.ticker="NOEXISTE"; assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(desconocido));
        var precision=request(); precision.cantidad=n("0.0000001"); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(precision));
        var enorme=request(); enorme.precioUnitario=n("999999999999999"); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(enorme));
        verifyNoInteractions(repo);
    }
    @Test void simuladorConservaEstadoYNoComparteConversaciones() {
        var a=new SimuladorPortfolioServiceBean(); var b=new SimuladorPortfolioServiceBean();
        a.definirCapital(n("1000000")); a.definirPorcentaje(TipoInstrumento.ACCION,n("40")); a.definirPorcentaje(TipoInstrumento.BONO,n("30"));
        igual("400000",a.calcular().importes.get(TipoInstrumento.ACCION)); igual("300000",a.calcular().disponible);
        igual("0",b.calcular().capital); assertTrue(b.calcular().porcentajes.isEmpty());
        a.calcular().porcentajes.clear(); assertEquals(2,a.calcular().porcentajes.size());
        a.cerrar(); igual("0",a.calcular().capital);
    }
    @Test void simuladorRechazaExcesoSinCambiarEstado() {
        var a=new SimuladorPortfolioServiceBean(); a.definirPorcentaje(TipoInstrumento.ACCION,n("60"));
        assertThrows(ReglaNegocioException.class,()->a.definirPorcentaje(TipoInstrumento.BONO,n("41")));
        assertThrows(ReglaNegocioException.class,()->a.definirCapital(n("-1")));
        assertEquals(1,a.calcular().porcentajes.size()); a.definirPorcentaje(TipoInstrumento.ACCION,n("100"));
        a.definirCapital(n("0.01")); igual("0",a.calcular().disponible);
    }
    @Test void resolverNoPermiteLlamadorSinRol() throws Exception {
        var bean=new PortfolioActualBean(); var contexto=mock(jakarta.ejb.SessionContext.class); var repo=mock(PortfolioRepository.class);
        inyectar(bean,"contexto",contexto); inyectar(bean,"portfolios",repo);
        assertThrows(jakarta.ejb.EJBAccessException.class,bean::obtener); verifyNoInteractions(repo);
    }
    @Test void resolverUsaPrincipalYReutilizaPortfolio() throws Exception {
        var bean=new PortfolioActualBean(); var contexto=mock(jakarta.ejb.SessionContext.class); var repo=mock(PortfolioRepository.class);
        when(contexto.isCallerInRole("USUARIO")).thenReturn(true); when(contexto.getCallerPrincipal()).thenReturn(()->"ana");
        when(repo.porPropietario("ana")).thenReturn(Optional.of(cartera)); inyectar(bean,"contexto",contexto); inyectar(bean,"portfolios",repo);
        assertSame(cartera,bean.obtener()); verify(repo,never()).guardar(any());
    }
}
