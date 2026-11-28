package ar.edu.uade.inversorar.presentation.api;

import jakarta.json.Json;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Lógica de Problem Details, Idempotency-Key, ETag y límite de pedidos (sin servidor JAX-RS). */
class ApiRestTest {
    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-11-27T12:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    // ---- Problem Details ----

    @Test void unProblemaTieneLosCamposDelRfcYElMensajeCompatible() {
        var json = Json.createReader(new StringReader(
                Problema.de("regla-de-negocio", "Regla de negocio incumplida", 400, "Tenencia \"insuficiente\"", "/inversorar/api/ventas").json())).readObject();
        assertEquals("urn:inversorar:problema:regla-de-negocio", json.getString("type"));
        assertEquals("Regla de negocio incumplida", json.getString("title"));
        assertEquals(400, json.getInt("status"));
        assertEquals("Tenencia \"insuficiente\"", json.getString("detail"));
        assertEquals("Tenencia \"insuficiente\"", json.getString("mensaje"));
        assertEquals("/inversorar/api/ventas", json.getString("instance"));
    }
    @Test void unProblemaHttpGenericoUsaAboutBlankYElTituloDelCodigo() {
        var json = Json.createReader(new StringReader(Problema.http(429, null, null).json())).readObject();
        assertEquals("about:blank", json.getString("type"));
        assertEquals("Demasiados pedidos", json.getString("title"));
        assertFalse(json.containsKey("detail")); assertFalse(json.containsKey("instance"));
        assertEquals("Error del servidor", Problema.titulo(599)); assertEquals("Error en el pedido", Problema.titulo(418));
        assertEquals("application/problem+json", Problema.TIPO_MEDIO);
    }
    @Test void laPaginaDeErrorDaUnDetalleParaCadaCodigo() {
        assertEquals("Iniciá sesión para usar la API.", ErrorHttpServlet.detalle(401));
        assertTrue(ErrorHttpServlet.detalle(403).contains("permiso"));
        assertTrue(ErrorHttpServlet.detalle(503).contains("inesperado"));
        assertEquals("Precondición fallida.", ErrorHttpServlet.detalle(412));
    }

    // ---- Idempotency-Key ----

    RelojManual reloj = new RelojManual();
    RegistroIdempotencia registro = new RegistroIdempotencia(reloj);
    RegistroIdempotencia.Guardada creada = new RegistroIdempotencia.Guardada(202, "orden 7", "application/json", "ordenes/7");

    @Test void laPrimeraVezSeProcesaYLaSegundaRepiteLaRespuesta() {
        assertEquals(RegistroIdempotencia.Estado.NUEVA, registro.reservar("ana\nk1", "h").estado());
        registro.completar("ana\nk1", creada);
        var otra = registro.reservar("ana\nk1", "h");
        assertEquals(RegistroIdempotencia.Estado.REPETIDA, otra.estado());
        assertSame(creada, otra.respuesta());
    }
    @Test void mientrasElPrimeroNoTerminaLaMismaClaveEstaEnCurso() {
        registro.reservar("ana\nk1", "h");
        assertEquals(RegistroIdempotencia.Estado.EN_CURSO, registro.reservar("ana\nk1", "h").estado());
    }
    @Test void laMismaClaveConOtroCuerpoSeRechaza() {
        registro.reservar("ana\nk1", "h"); registro.completar("ana\nk1", creada);
        assertEquals(RegistroIdempotencia.Estado.CUERPO_DISTINTO, registro.reservar("ana\nk1", "otra").estado());
    }
    @Test void lasClavesSonPorUsuario() {
        registro.reservar("ana\nk1", "h"); registro.completar("ana\nk1", creada);
        assertEquals(RegistroIdempotencia.Estado.NUEVA, registro.reservar("beto\nk1", "h").estado());
    }
    @Test void unErrorDelServidorLiberaLaClave() {
        registro.reservar("ana\nk1", "h"); registro.liberar("ana\nk1");
        assertEquals(RegistroIdempotencia.Estado.NUEVA, registro.reservar("ana\nk1", "h").estado());
    }
    @Test void lasClavesVencen() {
        registro.reservar("ana\nk1", "h"); registro.completar("ana\nk1", creada);
        reloj.avanzar(RegistroIdempotencia.VIGENCIA.plusSeconds(1));
        assertEquals(RegistroIdempotencia.Estado.NUEVA, registro.reservar("ana\nk1", "otro cuerpo").estado());
        // Una reserva que quedó colgada (el servidor se cayó a mitad) se libera a los 2 minutos.
        reloj.avanzar(RegistroIdempotencia.VIGENCIA_EN_CURSO.plusSeconds(1));
        assertEquals(RegistroIdempotencia.Estado.NUEVA, registro.reservar("ana\nk1", "h").estado());
    }
    @Test void dosPedidosSimultaneosConLaMismaClaveSoloProcesanUno() throws Exception {
        var nuevas = new AtomicInteger(); var largada = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(16); var tareas = new ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < 64; i++) tareas.add(pool.submit(() -> {
            largada.await();
            if (registro.reservar("ana\ndoble-clic", "h").estado() == RegistroIdempotencia.Estado.NUEVA) nuevas.incrementAndGet();
            return null;
        }));
        largada.countDown();
        for (var t : tareas) t.get();
        pool.shutdown();
        assertEquals(1, nuevas.get());
    }
    @Test void elRegistroNoCreceSinLimite() {
        for (int i = 0; i < RegistroIdempotencia.MAXIMO + 50; i++) {
            registro.reservar("u\n" + i, "h"); registro.completar("u\n" + i, creada);
        }
        assertTrue(registro.tamanio() <= RegistroIdempotencia.MAXIMO, "tamaño " + registro.tamanio());
    }
    @Test void laHuellaDistingueRutaYCuerpo() {
        byte[] cuerpo = "{\"ticker\":\"BTCUSDT\"}".getBytes(StandardCharsets.UTF_8);
        assertEquals(FiltroIdempotencia.huella("POST /api/compras", cuerpo), FiltroIdempotencia.huella("POST /api/compras", cuerpo.clone()));
        assertNotEquals(FiltroIdempotencia.huella("POST /api/compras", cuerpo), FiltroIdempotencia.huella("POST /api/ventas", cuerpo));
        assertNotEquals(FiltroIdempotencia.huella("POST /api/compras", cuerpo), FiltroIdempotencia.huella("POST /api/compras", "{}".getBytes(StandardCharsets.UTF_8)));
    }

    // ---- ETag ----

    @Test void elEtagDependeDelContenido() {
        String a = Etags.calcular("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        assertTrue(a.startsWith("W/\"") && a.endsWith("\""), a);
        assertEquals(a, Etags.calcular("{\"x\":1}".getBytes(StandardCharsets.UTF_8)));
        assertNotEquals(a, Etags.calcular("{\"x\":2}".getBytes(StandardCharsets.UTF_8)));
    }
    @Test void ifNoneMatchUsaComparacionDebil() {
        String etag = "W/\"abc\"";
        assertTrue(Etags.coincide("W/\"abc\"", etag));
        assertTrue(Etags.coincide("\"abc\"", etag), "sin W/ también coincide (comparación débil)");
        assertTrue(Etags.coincide("\"zzz\", W/\"abc\"", etag), "lista separada por comas");
        assertTrue(Etags.coincide("*", etag));
        assertFalse(Etags.coincide("W/\"otro\"", etag));
        assertFalse(Etags.coincide(null, etag));
    }

    // ---- 429 ----

    @Test void elLimitadorCortaAlSuperarElLimiteYDiceCuantoEsperar() {
        var limite = new LimitadorPedidos(5, reloj);
        for (int i = 0; i < 5; i++) assertNull(limite.consumir("email:ana@x.com"), "intento " + (i + 1));
        Duration espera = limite.consumir("email:ana@x.com");
        assertNotNull(espera, "el sexto intento en el mismo minuto se rechaza");
        assertEquals(12, FiltroLimitePedidos.segundos(espera), "una ficha cada 12 s");
        assertNull(limite.consumir("email:beto@x.com"), "otra clave tiene su propio límite");
    }
    @Test void lasFichasSeRecargan() {
        var limite = new LimitadorPedidos(60, reloj);
        for (int i = 0; i < 60; i++) limite.consumir("u");
        assertNotNull(limite.consumir("u"));
        reloj.avanzar(Duration.ofSeconds(1));
        assertNull(limite.consumir("u"), "a los 60 por minuto, se recarga una ficha por segundo");
        assertNotNull(limite.consumir("u"));
        reloj.avanzar(Duration.ofMinutes(5));
        for (int i = 0; i < 60; i++) assertNull(limite.consumir("u"), "no acumula más que la capacidad");
        assertNotNull(limite.consumir("u"));
    }
    @Test void elLimitadorDescartaBaldesLlenosParaNoCrecer() {
        var limite = new LimitadorPedidos(10, reloj);
        for (int i = 0; i < LimitadorPedidos.MAXIMO_CLAVES; i++) limite.consumir("ip:" + i);
        reloj.avanzar(Duration.ofMinutes(1));
        limite.consumir("ip:nueva");
        assertTrue(limite.claves() < 10, "quedaron " + limite.claves());
        assertThrows(IllegalArgumentException.class, () -> new LimitadorPedidos(0, reloj));
        assertEquals(1, FiltroLimitePedidos.segundos(Duration.ofMillis(10)));
    }
    @Test void losLimitesPorDefectoFrenanFuerzaBrutaSinCortarElDashboard() {
        assertEquals(5, FiltroLimitePedidos.LOGIN_POR_EMAIL_DEFECTO);
        // Dashboard: ~10 pedidos al abrir + 2 cada 3 s de sondeo = ~50 por minuto por pestaña.
        assertTrue(FiltroLimitePedidos.LECTURAS_DEFECTO >= 4 * 50);
    }
}
