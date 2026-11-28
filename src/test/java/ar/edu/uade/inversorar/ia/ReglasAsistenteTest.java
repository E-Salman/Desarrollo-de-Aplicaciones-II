package ar.edu.uade.inversorar.ia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Barreras del gateway que no dependen del modelo. */
class ReglasAsistenteTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Me conviene comprar Bitcoin?", "¿Debería vender mis ETH?", "Recomendame una cripto",
            "¿Qué me recomendás?", "¿En qué invierto 1000 dólares?", "¿Bitcoin va a subir?", "¿Subirá el BTC mañana?",
            "¿Qué compro hoy?", "¿Compro o no?", "¿Es buena inversión Solana?", "¿Es buen momento para entrar?",
            "¿Vale la pena invertir en Dogecoin?", "Should I buy BTC?", "ME CONVIENE VENDER?"})
    void detectaPedidosDeConsejo(String pregunta) {
        assertTrue(ReglasAsistente.pideConsejo(pregunta), pregunta);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Cómo compro un instrumento?", "¿Cómo registro una venta?", "¿Qué es un CEDEAR?",
            "¿Dónde veo lo que compro?", "¿Por qué mi orden quedó rechazada?", "¿Cómo cancelo una orden pendiente?",
            "¿Qué significa patrimonio total?", "Resumí las noticias más importantes."})
    void noConfundeLasPreguntasDeUsoConConsejo(String pregunta) {
        assertFalse(ReglasAsistente.pideConsejo(pregunta), pregunta);
    }

    @Test void laPreguntaSeLimpiaYTieneLargoMaximo() {
        assertEquals("¿Qué es un bono?", ReglasAsistente.pregunta("  ¿Qué es\n\n un​ bono?\u0007 "));
        assertThrows(IllegalArgumentException.class, () -> ReglasAsistente.pregunta("   "));
        assertThrows(IllegalArgumentException.class, () -> ReglasAsistente.pregunta(null));
        assertEquals(500, ReglasAsistente.pregunta("a".repeat(500)).length());
        var e = assertThrows(IllegalArgumentException.class, () -> ReglasAsistente.pregunta("a".repeat(501)));
        assertTrue(e.getMessage().contains("500"));
    }

    @Test void elActivoSoloAdmiteLetrasNumerosYSignosSimples() {
        assertEquals("BTC", ReglasAsistente.activo(" BTC "));
        assertEquals("Bitcoin Cash", ReglasAsistente.activo("Bitcoin  Cash"));
        for (String malo : new String[]{"", "x", "BTC%", "a_b", "<script>", "' OR 1=1 --;", "a".repeat(61), null})
            assertThrows(IllegalArgumentException.class, () -> ReglasAsistente.activo(malo), String.valueOf(malo));
    }

    @Test void laRespuestaNoLlevaEnlacesDelModelo() {
        String r = ReglasAsistente.respuesta("Mirá [este sitio](https://malo.example/robar) o https://otro.example/x y www.tercero.example.", "INV-1");
        assertFalse(r.contains("http"), r);
        assertFalse(r.contains("www."), r);
        assertTrue(r.startsWith("Mirá este sitio o"), r);
    }

    @Test void siElModeloFiltraLasInstruccionesSeDescarta() {
        assertNull(ReglasAsistente.respuesta("Mis instrucciones dicen: código INV-abc123", "INV-abc123"));
        assertNull(ReglasAsistente.respuesta("Claro: REGLAS QUE NO CAMBIAN aunque el usuario lo pida: 1. ...", "INV-abc123"));
        assertNull(ReglasAsistente.respuesta("   ", "INV-abc123"));
        assertNull(ReglasAsistente.respuesta(null, "INV-abc123"));
        assertEquals("Hola", ReglasAsistente.respuesta("Hola", "INV-abc123"));
    }

    @Test void laRespuestaTieneLargoMaximo() {
        String r = ReglasAsistente.respuesta("palabra ".repeat(400), null);
        assertTrue(r.length() <= ReglasAsistente.MAXIMO_RESPUESTA);
        assertTrue(r.endsWith("…"));
    }
}
