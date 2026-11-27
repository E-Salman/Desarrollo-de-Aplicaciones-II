package ar.edu.uade.inversorar.integracion;

import ar.edu.uade.inversorar.business.ConfiguracionApp;
import ar.edu.uade.inversorar.business.CotizacionCatalogo;
import ar.edu.uade.inversorar.data.Instrumento;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotizacionBinanceTest {
    static void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f = objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto, valor);
    }
    /** Reloj que avanza a mano, para probar caché y pausa sin esperar. */
    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-11-26T15:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("America/Argentina/Buenos_Aires"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    ServidorFalso binance;
    RelojManual reloj = new RelojManual();
    CotizacionCatalogo catalogo = mock(CotizacionCatalogo.class);
    ConfiguracionApp configuracion = mock(ConfiguracionApp.class);
    CotizacionBinance cotizacion;
    Instrumento btc = new Instrumento("BTC/USDT", "BTCUSDT", TipoInstrumento.CRIPTO, null);

    @BeforeEach void preparar() throws Exception {
        binance = new ServidorFalso();
        cotizacion = new CotizacionBinance(binance.url(""), Duration.ofMillis(400), reloj);
        inyectar(cotizacion, "catalogo", catalogo); inyectar(cotizacion, "configuracion", configuracion);
        when(configuracion.catalogoMysql()).thenReturn(true);
        // El catálogo MySQL fija el último cierre, su moneda y su fecha.
        when(catalogo.cotizar(any())).thenAnswer(inv -> {
            Instrumento i = inv.getArgument(0);
            i.actualizarCotizacion(new BigDecimal("90000"), "USDT", "2026-11-20 00:00:00");
            return new BigDecimal("90000");
        });
        binance.cuerpo = "{\"symbol\":\"BTCUSDT\",\"price\":\"97350.12000000\"}";
    }
    @AfterEach void apagar() { binance.close(); }

    @Test void elTimeoutPorDefectoEsDeCincoSegundos() {
        assertEquals(Duration.ofSeconds(5), new CotizacionBinance().timeout());
    }
    @Test void usaElPrecioEnVivoYConservaLaMonedaDelCatalogo() {
        assertEquals(new BigDecimal("97350.12000000"), cotizacion.cotizar(btc));
        assertTrue(binance.ultimoPedido.startsWith("/api/v3/ticker/price?symbol=BTCUSDT"), binance.ultimoPedido);
        assertEquals(new BigDecimal("97350.12000000"), btc.getCotizacionActual());
        assertEquals("USDT", btc.getMonedaCotizacion());
        assertEquals("BINANCE", btc.getFuenteCotizacion());
        assertEquals("2026-11-26 12:00:00", btc.getFechaCotizacion(), "hora local de Buenos Aires");
    }
    @Test void siBinanceNoRespondeATiempoUsaElUltimoCierre() {
        binance.demoraMs = 3_000;
        long inicio = System.nanoTime();
        assertEquals(new BigDecimal("90000"), cotizacion.cotizar(btc));
        assertTrue((System.nanoTime() - inicio) / 1_000_000 < 2_000);
        assertEquals("RESPALDO", btc.getFuenteCotizacion());
        assertEquals("2026-11-20 00:00:00", btc.getFechaCotizacion());
    }
    @Test void trasUnaFallaNoVuelveAPreguntarDuranteTreintaSegundos() {
        binance.estado = 503;
        cotizacion.cotizar(btc); cotizacion.cotizar(btc); cotizacion.cotizar(btc);
        assertEquals(1, binance.pedidos.get(), "un portfolio con N posiciones no espera N timeouts");
        reloj.avanzar(Duration.ofSeconds(31)); binance.estado = 200;
        assertEquals(new BigDecimal("97350.12000000"), cotizacion.cotizar(btc));
        assertEquals(2, binance.pedidos.get());
    }
    @Test void unSimboloQueBinanceNoListaUsaElCierreSinPausarAlProveedor() {
        binance.estado = 400; binance.cuerpo = "{\"code\":-1121,\"msg\":\"Invalid symbol.\"}";
        assertEquals(new BigDecimal("90000"), cotizacion.cotizar(btc));
        binance.estado = 200; binance.cuerpo = "{\"symbol\":\"ETHUSDT\",\"price\":\"3000\"}";
        assertEquals(new BigDecimal("3000"), cotizacion.cotizar(new Instrumento("ETH/USDT", "ETHUSDT", TipoInstrumento.CRIPTO, null)));
    }
    @Test void elPrecioQuedaEnCacheQuinceSegundos() {
        cotizacion.cotizar(btc); cotizacion.cotizar(btc);
        assertEquals(1, binance.pedidos.get());
        reloj.avanzar(Duration.ofSeconds(16));
        cotizacion.cotizar(btc);
        assertEquals(2, binance.pedidos.get());
    }
    @Test void unaRespuestaIlegibleUsaElCierre() {
        binance.cuerpo = "no es json";
        assertEquals(new BigDecimal("90000"), cotizacion.cotizar(btc));
        assertEquals("RESPALDO", btc.getFuenteCotizacion());
    }
    @Test void enElDemoH2NoSaleAInternet() {
        when(configuracion.catalogoMysql()).thenReturn(false);
        assertEquals(new BigDecimal("90000"), cotizacion.cotizar(btc));
        assertEquals(0, binance.pedidos.get()); assertNull(btc.getFuenteCotizacion());
    }
    @Test void unTickerConCaracteresRarosNoSeEnvia() {
        assertEquals(new BigDecimal("90000"), cotizacion.cotizar(new Instrumento("x", "BTC&x=1", TipoInstrumento.CRIPTO, null)));
        assertEquals(0, binance.pedidos.get());
    }
}
