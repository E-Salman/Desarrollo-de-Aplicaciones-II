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
    @Test void monedasSinConversionQuedanAfueraDelTotalUsdPeroNoRompenElResumen() throws Exception {
        var eth = new Instrumento("ETH/BTC", "ETHBTC", TipoInstrumento.CRIPTO, n("0.03"));
        eth.actualizarCotizacion(n("0.03"), "BTC", "2026-08-31 00:00:00");
        var movimiento = new Operacion(cartera, eth, n("2"), n("0.02"), LocalDate.now());
        var r = portfolio().consolidar(List.of(compra("1", "180"), movimiento));
        // Sin catálogo MySQL inyectado no hay forma de convertir BTC; sólo USD (directo) entra al total.
        assertEquals("USD", r.moneda);
        igual("180", r.capitalInvertido); igual("195", r.patrimonioTotal); igual("15", r.gananciaTotal);
        igual("180", r.totalesPorMoneda.get("USD").capitalInvertido);
        igual("0.04", r.totalesPorMoneda.get("BTC").capitalInvertido);
        igual("0.02", r.totalesPorMoneda.get("BTC").gananciaTotal);
    }
    @Test void conservaPrecisionDePreciosCripto() throws Exception {
        var eth = new Instrumento("ETH/BTC", "ETHBTC", TipoInstrumento.CRIPTO, n("0.0000001234"));
        eth.actualizarCotizacion(n("0.0000001234"), "BTC", "2026-08-31 00:00:00");
        var r = portfolio().consolidar(List.of(new Operacion(cartera, eth, n("2"), n("0.0000001001"), LocalDate.now())));
        igual("0.0000002002", r.totalesPorMoneda.get("BTC").capitalInvertido);
        igual("0.0000002468", r.totalesPorMoneda.get("BTC").patrimonioTotal);
        igual("0.0000001001", r.posiciones.get(0).precioPromedio); assertEquals("BTC", r.posiciones.get(0).moneda);
    }
    @Test void convierteMonedaSinStablecoinSaltandoPorElCatalogo() throws Exception {
        var eth = new Instrumento("Cripto rara", "ETHXYZ", TipoInstrumento.CRIPTO, n("2"));
        eth.actualizarCotizacion(n("2"), "XYZ", "2026-08-31 00:00:00");
        var movimiento = new Operacion(cartera, eth, n("3"), n("1"), LocalDate.now());
        var bean = portfolio();
        var config = mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        var catalogoMercado = mock(CatalogoMercadoRepository.class); when(catalogoMercado.tasaAUsd("XYZ")).thenReturn(n("5"));
        inyectar(bean, "configuracion", config); inyectar(bean, "catalogo", catalogoMercado);
        var r = bean.consolidar(List.of(movimiento));
        assertEquals("USD", r.moneda);
        igual("15", r.capitalInvertido); igual("30", r.patrimonioTotal);
    }
    @Test void cadaPosicionTraeSuValorEnUsdParaElGraficoDeDistribucion() throws Exception {
        var cat = new Instrumento("A 1000CAT/USDC", "1000CATUSDC", TipoInstrumento.CRIPTO, n("0.5"));
        cat.actualizarCotizacion(n("0.5"), "USDC", "2026-10-18 00:00:00");
        var xyz = new Instrumento("B Cripto rara", "ETHXYZ", TipoInstrumento.CRIPTO, n("2"));
        xyz.actualizarCotizacion(n("2"), "XYZ", "2026-10-18 00:00:00");
        var btc = new Instrumento("C ETH/BTC", "ETHBTC", TipoInstrumento.CRIPTO, n("0.03"));
        btc.actualizarCotizacion(n("0.03"), "BTC", "2026-10-18 00:00:00");
        var bean = portfolio();
        var config = mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        var catalogoMercado = mock(CatalogoMercadoRepository.class); when(catalogoMercado.tasaAUsd("XYZ")).thenReturn(n("5"));
        inyectar(bean, "configuracion", config); inyectar(bean, "catalogo", catalogoMercado);
        var r = bean.consolidar(List.of(new Operacion(cartera, cat, n("200"), n("0.4"), LocalDate.now()),
                new Operacion(cartera, xyz, n("3"), n("1"), LocalDate.now()), new Operacion(cartera, btc, n("2"), n("0.02"), LocalDate.now())));
        igual("100", r.posiciones.get(0).actualUsd);   // stablecoin: 200 × 0,5 USDC = 100 USD
        igual("30", r.posiciones.get(1).actualUsd);    // por el catálogo: 3 × 2 XYZ × 5 = 30 USD
        assertNull(r.posiciones.get(2).actualUsd);     // BTC sin conversión: queda afuera del gráfico
        igual("130", r.patrimonioTotal);               // mismos valores que el patrimonio total
    }
    @Test void compraSinMonedaNoPersiste() throws Exception {
        var repo = mock(OperacionRepository.class); var bean = compraBean(repo);
        apple.actualizarCotizacion(n("195"), null, null);
        assertThrows(ReglaNegocioException.class, () -> bean.registrarCompra(request()));
        verifyNoInteractions(repo);
    }
    @Test void compraTotalDemasiadoPequenioNoPersiste() throws Exception {
        var repo = mock(OperacionRepository.class); var bean = compraBean(repo); var r = request();
        r.cantidad=n("0.0000000001"); r.precioUnitario=n("0.0000000001");
        assertThrows(ReglaNegocioException.class, () -> bean.registrarCompra(r)); verifyNoInteractions(repo);
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
    @Test void compraMysqlDejaOrdenPendienteYEnviaMensajeSinCrearOperacion() throws Exception {
        var repo=mock(OperacionRepository.class); var ordenes=mock(OrdenRepository.class); var productor=mock(ar.edu.uade.inversorar.mensajeria.ProductorOrdenes.class);
        var bean=compraBean(repo);
        var config=mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        inyectar(bean,"configuracion",config); inyectar(bean,"ordenes",ordenes); inyectar(bean,"productor",productor); inyectar(cartera,"usuarioId",42L);
        var pendiente=Orden.pendiente(42L,TipoOperacion.COMPRA,LocalDate.now(),"USD",n("360"));
        var detalle=new OrdenDetalle(pendiente,apple,n("2"),n("180"));
        when(ordenes.crearPendiente(eq(42L),eq(TipoOperacion.COMPRA),same(apple),any(),any(),any())).thenReturn(detalle);
        var dto=bean.registrarCompra(request());
        assertEquals("PENDIENTE",dto.estado); assertEquals("COMPRA",dto.tipo); igual("360",dto.total); igual("360",detalle.getSubtotal());
        var orden=inOrder(ordenes,productor); orden.verify(ordenes).crearPendiente(eq(42L),eq(TipoOperacion.COMPRA),same(apple),any(),any(),any());
        orden.verify(productor).enviar(pendiente,detalle);
        // La operación la crea el Ejecutor al consumir el mensaje, no la compra.
        verifyNoInteractions(repo);
    }
    @Test void ventaMysqlDejaOrdenPendienteTrasLaPrevalidacion() throws Exception {
        var bean=new VentaServiceBean(); var instrumentos=mock(InstrumentoRepository.class); var actual=mock(PortfolioActual.class);
        var repo=mock(OperacionRepository.class); var ordenes=mock(OrdenRepository.class); var productor=mock(ar.edu.uade.inversorar.mensajeria.ProductorOrdenes.class);
        var config=mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        when(instrumentos.buscarPorTicker("AAPL")).thenReturn(Optional.of(apple)); when(actual.obtener()).thenReturn(cartera);
        inyectar(cartera,"id",5L); inyectar(cartera,"usuarioId",42L);
        when(repo.porPortfolioYTicker(5L,"AAPL")).thenReturn(List.of(compra("10","180")));
        for (var campo : Map.of("instrumentos",instrumentos,"portfolioActual",actual,"operaciones",repo,"configuracion",config,"ordenes",ordenes,"productor",productor).entrySet())
            inyectar(bean,campo.getKey(),campo.getValue());
        var pendiente=Orden.pendiente(42L,TipoOperacion.VENTA,LocalDate.now(),"USD",n("1900"));
        var detalle=new OrdenDetalle(pendiente,apple,n("10"),n("190"));
        when(ordenes.crearPendiente(eq(42L),eq(TipoOperacion.VENTA),same(apple),any(),any(),any())).thenReturn(detalle);
        when(ordenes.ventasPendientes(42L,apple)).thenReturn(BigDecimal.ZERO);
        var r=new ar.edu.uade.inversorar.business.dto.VentaRequest(); r.ticker="AAPL"; r.cantidad=n("10"); r.precioUnitario=n("190"); r.fecha=LocalDate.now();
        assertEquals("PENDIENTE",bean.registrarVenta(r).estado);
        verify(productor).enviar(pendiente,detalle); verify(repo,never()).guardar(any());
        // La prevalidación sigue rechazando en el momento el caso obvio, sin crear orden.
        var demasiado=new ar.edu.uade.inversorar.business.dto.VentaRequest(); demasiado.ticker="AAPL"; demasiado.cantidad=n("11"); demasiado.precioUnitario=n("190"); demasiado.fecha=LocalDate.now();
        assertThrows(ReglaNegocioException.class,()->bean.registrarVenta(demasiado));
        verify(ordenes,times(1)).crearPendiente(any(),any(),any(),any(),any(),any());
    }
    @Test void ventaMysqlDescuentaLasVentasQueSiguenPendientes() throws Exception {
        // Tenés 96; ya hay 87 en una venta pendiente: vender 89 se rechaza en el momento, vender 9 se acepta.
        var bean=new VentaServiceBean(); var instrumentos=mock(InstrumentoRepository.class); var actual=mock(PortfolioActual.class);
        var repo=mock(OperacionRepository.class); var ordenes=mock(OrdenRepository.class); var productor=mock(ar.edu.uade.inversorar.mensajeria.ProductorOrdenes.class);
        var config=mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        when(instrumentos.buscarPorTicker("AAPL")).thenReturn(Optional.of(apple)); when(actual.obtener()).thenReturn(cartera);
        inyectar(cartera,"id",5L); inyectar(cartera,"usuarioId",42L);
        when(repo.porPortfolioYTicker(5L,"AAPL")).thenReturn(List.of(compra("96","180")));
        when(ordenes.ventasPendientes(42L,apple)).thenReturn(n("87"));
        for (var campo : Map.of("instrumentos",instrumentos,"portfolioActual",actual,"operaciones",repo,"configuracion",config,"ordenes",ordenes,"productor",productor).entrySet())
            inyectar(bean,campo.getKey(),campo.getValue());
        var venta=new ar.edu.uade.inversorar.business.dto.VentaRequest(); venta.ticker="AAPL"; venta.precioUnitario=n("190"); venta.fecha=LocalDate.now();
        venta.cantidad=n("89");
        var e=assertThrows(ReglaNegocioException.class,()->bean.registrarVenta(venta));
        assertTrue(e.getMessage().contains("podés vender hasta 9"),e.getMessage());
        verify(ordenes,never()).crearPendiente(any(),any(),any(),any(),any(),any()); verifyNoInteractions(productor);
        var pendiente=Orden.pendiente(42L,TipoOperacion.VENTA,LocalDate.now(),"USD",n("1710"));
        when(ordenes.crearPendiente(eq(42L),eq(TipoOperacion.VENTA),same(apple),any(),any(),any())).thenReturn(new OrdenDetalle(pendiente,apple,n("9"),n("190")));
        venta.cantidad=n("9");
        assertEquals("PENDIENTE",bean.registrarVenta(venta).estado);
    }
    @Test void cuentaSinUsuarioNoGeneraOrdenNiMovimiento() throws Exception {
        var repo=mock(OperacionRepository.class); var ordenes=mock(OrdenRepository.class); var bean=compraBean(repo);
        var config=mock(ConfiguracionApp.class); when(config.catalogoMysql()).thenReturn(true);
        inyectar(bean,"configuracion",config); inyectar(bean,"ordenes",ordenes);
        assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(request())); verifyNoInteractions(ordenes,repo);
    }
    @Test void portfolioNoSumaDetalleOtraVez() throws Exception {
        var o=compra("2","180"); o.vincularOrden(new OrdenDetalle(new Orden(42L,"USD",o.getTotal()),o));
        var r=portfolio().consolidar(List.of(o)); igual("360",r.capitalInvertido); igual("2",r.posiciones.get(0).cantidad);
    }
    @Test void cambioDeMonedaDeCatalogoNoReinterpretaLaOrden() throws Exception {
        var o=compra("2","180"); o.vincularOrden(new OrdenDetalle(new Orden(42L,"BTC",o.getTotal()),o));
        var bean=portfolio(); assertThrows(ReglaNegocioException.class,()->bean.consolidar(List.of(o)));
    }
    @Test void compraRechazaEntradasInvalidasSinPersistir() throws Exception {
        var repo=mock(OperacionRepository.class); var bean=compraBean(repo);
        assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(null));
        var cero=request(); cero.cantidad=BigDecimal.ZERO; assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(cero));
        var futuro=request(); futuro.fecha=LocalDate.now().plusDays(1); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(futuro));
        var desconocido=request(); desconocido.ticker="NOEXISTE"; assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(desconocido));
        var precision=request(); precision.cantidad=n("0.00000000001"); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(precision));
        var enorme=request(); enorme.precioUnitario=n("999999999999999"); assertThrows(ReglaNegocioException.class,()->bean.registrarCompra(enorme));
        verifyNoInteractions(repo);
    }
    @Test void portfolioConservaPlanificacionYNoComparteConversaciones() {
        var a=new PortfolioServiceBean(); var b=new PortfolioServiceBean();
        a.definirCapital(n("1000000")); a.definirPorcentaje(TipoInstrumento.ACCION,n("40")); a.definirPorcentaje(TipoInstrumento.BONO,n("30"));
        igual("400000",a.calcularSimulacion().importes.get(TipoInstrumento.ACCION)); igual("300000",a.calcularSimulacion().disponible);
        igual("0",b.calcularSimulacion().capital); assertTrue(b.calcularSimulacion().porcentajes.isEmpty());
        a.calcularSimulacion().porcentajes.clear(); assertEquals(2,a.calcularSimulacion().porcentajes.size());
        a.reiniciarSimulacion(); igual("0",a.calcularSimulacion().capital);
    }
    @Test void simuladorRechazaExcesoSinCambiarEstado() {
        var a=new PortfolioServiceBean(); a.definirPorcentaje(TipoInstrumento.ACCION,n("60"));
        assertThrows(ReglaNegocioException.class,()->a.definirPorcentaje(TipoInstrumento.BONO,n("41")));
        assertThrows(ReglaNegocioException.class,()->a.definirCapital(n("-1")));
        assertEquals(1,a.calcularSimulacion().porcentajes.size()); a.definirPorcentaje(TipoInstrumento.ACCION,n("100"));
        a.definirCapital(n("0.01")); igual("0",a.calcularSimulacion().disponible);
    }
    @Test void portfolioActualizaPosicionesSinPerderSimulacion() throws Exception {
        var bean=portfolio(); var actual=mock(PortfolioActual.class); var repo=mock(OperacionRepository.class);
        var propio=mock(Portfolio.class); when(propio.getId()).thenReturn(42L); when(actual.obtener()).thenReturn(propio);
        when(repo.porPortfolio(42L)).thenReturn(List.of(compra("10","180")), List.of(compra("10","180"),compra("5","210")));
        inyectar(bean,"portfolioActual",actual); inyectar(bean,"operaciones",repo);
        bean.definirCapital(n("1000")); bean.definirPorcentaje(TipoInstrumento.ACCION,n("40"));
        igual("1800",bean.obtenerResumen().capitalInvertido);
        igual("2850",bean.obtenerResumen().capitalInvertido);
        igual("400",bean.calcularSimulacion().importes.get(TipoInstrumento.ACCION));
        bean.reiniciarSimulacion(); igual("2850",bean.obtenerResumen().capitalInvertido);
        verify(repo,never()).guardar(any());
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
        var usuarios = mock(UsuarioRepository.class); var usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(42L); when(usuarios.buscarPorEmail("ana")).thenReturn(Optional.of(usuario));
        inyectar(bean,"usuarios",usuarios);
        assertSame(cartera,bean.obtener()); verify(repo,never()).guardar(any());
        assertEquals(42L, cartera.getUsuarioId());
    }
}
