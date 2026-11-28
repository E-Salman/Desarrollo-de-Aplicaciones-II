package ar.edu.uade.inversorar.ia;

import ar.edu.uade.inversorar.integracion.ServidorFalso;
import jakarta.json.Json;
import java.io.StringReader;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Cliente de Gemini contra un servidor falso: formato del pedido, lectura de la respuesta, fallas y pausa. */
class ProveedorGeminiTest {
    static final String OK = """
            {"candidates":[{"content":{"role":"model","parts":[
              {"text":"pensando...","thought":true},{"text":"Un CEDEAR es "},{"text":"un certificado."}]},
              "finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":40}}""";

    ServidorFalso gemini;
    AsistenteTest.RelojManual reloj = new AsistenteTest.RelojManual();
    ProveedorGemini proveedor;

    @BeforeEach void preparar() throws Exception {
        gemini = new ServidorFalso();
        proveedor = new ProveedorGemini(gemini.url("/"), "gemini-3.5-flash-lite", "clave-de-prueba", reloj);
    }
    @AfterEach void cerrar() { gemini.close(); }

    @Test void mandaElPedidoConLaClaveEnElEncabezadoYLeeElTexto() throws Exception {
        gemini.cuerpo = OK;
        assertEquals("Un CEDEAR es un certificado.", proveedor.responder("Sos el asistente", "¿Qué es un CEDEAR?", Duration.ofSeconds(2)));
        String[] pedido = gemini.ultimoPedido.split("\n", 2);
        assertEquals("/v1beta/models/gemini-3.5-flash-lite:generateContent", pedido[0]);
        assertFalse(pedido[0].contains("clave-de-prueba"), "la clave nunca va en la URL");
        assertEquals("clave-de-prueba", gemini.ultimosEncabezados.getFirst("x-goog-api-key"));
        var json = Json.createReader(new StringReader(pedido[1])).readObject();
        assertEquals("Sos el asistente", json.getJsonObject("systemInstruction").getJsonArray("parts").getJsonObject(0).getString("text"));
        var contenido = json.getJsonArray("contents").getJsonObject(0);
        assertEquals("user", contenido.getString("role"));
        assertEquals("¿Qué es un CEDEAR?", contenido.getJsonArray("parts").getJsonObject(0).getString("text"));
        assertEquals("MINIMAL", json.getJsonObject("generationConfig").getJsonObject("thinkingConfig").getString("thinkingLevel"));
        assertEquals("GEMINI", proveedor.nombre());
    }

    @Test void unaFallaPausaElProveedorParaNoEsperarOtroTimeout() {
        gemini.estado = 429; gemini.cuerpo = "{\"error\":{\"code\":429}}";
        var e = assertThrows(ServicioNoDisponible.class, () -> proveedor.responder("i", "p", Duration.ofSeconds(2)));
        assertTrue(e.getMessage().contains("HTTP 429"));
        assertFalse(e.getMessage().contains("clave-de-prueba"));
        gemini.estado = 200; gemini.cuerpo = OK;
        assertThrows(ServicioNoDisponible.class, () -> proveedor.responder("i", "p", Duration.ofSeconds(2)));
        assertEquals(1, gemini.pedidos.get(), "en pausa no se vuelve a llamar");
        reloj.avanzar(ProveedorGemini.PAUSA_TRAS_FALLA.plusSeconds(1));
        assertDoesNotThrow(() -> proveedor.responder("i", "p", Duration.ofSeconds(2)));
    }

    @Test void siNoRespondeEnElPlazoCorta() {
        gemini.demoraMs = 1500; gemini.cuerpo = OK;
        long inicio = System.nanoTime();
        var e = assertThrows(ServicioNoDisponible.class, () -> proveedor.responder("i", "p", Duration.ofMillis(300)));
        assertTrue(e.getMessage().contains("sin respuesta"), e.getMessage());
        assertTrue((System.nanoTime() - inicio) / 1_000_000 < 1200, "no espera la respuesta lenta");
    }

    @Test void sinTiempoNiSiquieraLlama() {
        assertThrows(ServicioNoDisponible.class, () -> proveedor.responder("i", "p", Duration.ZERO));
        assertEquals(0, gemini.pedidos.get());
    }

    @Test void losErroresDelServidorYDeLaClaveSonNoDisponible() {
        for (int estado : new int[]{400, 403, 500, 503}) {
            var p = new ProveedorGemini(gemini.url(""), "gemini-3.5-flash-lite", "k", reloj);
            gemini.estado = estado;
            assertThrows(ServicioNoDisponible.class, () -> p.responder("i", "p", Duration.ofSeconds(2)), "HTTP " + estado);
        }
    }

    @Test void interpretaLasRespuestasBloqueadasVaciasEIlegibles() {
        assertThrows(RespuestaBloqueada.class, () -> ProveedorGemini.texto("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"));
        assertThrows(RespuestaBloqueada.class, () -> ProveedorGemini.texto(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"x\"}]},\"finishReason\":\"PROHIBITED_CONTENT\"}]}"));
        assertThrows(ServicioNoDisponible.class, () -> ProveedorGemini.texto("{\"candidates\":[]}"));
        assertThrows(ServicioNoDisponible.class, () -> ProveedorGemini.texto(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]},\"finishReason\":\"MAX_TOKENS\"}]}"));
        assertThrows(ServicioNoDisponible.class, () -> ProveedorGemini.texto("<html>no es json"));
        assertDoesNotThrow(() -> ProveedorGemini.texto(
                "{\"candidates\":[{\"content\":{\"parts\":[\"raro\",{\"text\":\"corte\"}]},\"finishReason\":\"MAX_TOKENS\"}]}"));
    }

    @Test void validaLaConfiguracion() {
        assertThrows(IllegalArgumentException.class, () -> new ProveedorGemini(" "));
        assertThrows(IllegalArgumentException.class, () -> new ProveedorGemini("http://x", "../../otro", "k", reloj));
        assertEquals(ProveedorGemini.MODELO_POR_DEFECTO, new ProveedorGemini("k").modelo());
    }
}
