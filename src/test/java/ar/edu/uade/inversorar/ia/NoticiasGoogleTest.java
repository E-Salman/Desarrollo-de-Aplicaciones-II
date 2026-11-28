package ar.edu.uade.inversorar.ia;

import ar.edu.uade.inversorar.integracion.ServidorFalso;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** RSS de Google News contra un servidor falso: lectura, enlaces seguros, XXE, caché y pausa. */
class NoticiasGoogleTest {
    static String item(String titulo, String fuente, String link, String fecha) {
        return "<item><title>" + titulo + "</title><link>" + link + "</link><pubDate>" + fecha
                + "</pubDate><source url=\"https://x\">" + fuente + "</source></item>";
    }
    static String rss(String... items) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>t</title>"
                + String.join("", items) + "</channel></rss>";
    }

    ServidorFalso google;
    AsistenteTest.RelojManual reloj = new AsistenteTest.RelojManual();
    NoticiasGoogle noticias;

    @BeforeEach void preparar() throws Exception {
        google = new ServidorFalso();
        noticias = new NoticiasGoogle(google.url(""), reloj);
    }
    @AfterEach void cerrar() { google.close(); }

    @Test void leeTitulosFuentesFechasYEnlaces() throws Exception {
        google.cuerpo = rss(
                item("Bitcoin sube 5% - Ámbito", "Ámbito", "https://news.google.com/a/1", "Fri, 27 Nov 2026 02:30:00 GMT"),
                item("Peligro &amp; truco", "Sitio", "javascript:alert(1)", "Fri, 27 Nov 2026 10:00:00 GMT"),
                item("Ethereum &lt;b&gt;baja&lt;/b&gt;", "Clarín", "https://news.google.com/a/2", "fecha rara"));
        var lista = noticias.buscar("Bitcoin", Duration.ofSeconds(2));
        assertEquals(2, lista.size(), "el enlace javascript: se descarta");
        assertEquals(new Noticia("Bitcoin sube 5%", "Ámbito", "26/11/2026", "https://news.google.com/a/1"), lista.get(0),
                "sin el sufijo de la fuente y con fecha de Argentina");
        assertEquals("Ethereum <b>baja</b>", lista.get(1).titulo(), "texto plano: la pantalla lo muestra con textContent");
        assertEquals("", lista.get(1).fecha());
        String uri = URLDecoder.decode(google.ultimoPedido.split("\n")[0], StandardCharsets.UTF_8);
        assertTrue(uri.startsWith("/rss/search?q=\"Bitcoin\" when:7d&hl=es-419&gl=AR"), uri);
    }

    @Test void devuelveComoMaximoCincoNoticias() throws Exception {
        String[] items = new String[8];
        for (int i = 0; i < items.length; i++) items[i] = item("N" + i, "F", "https://n/" + i, "");
        google.cuerpo = rss(items);
        assertEquals(NoticiasGoogle.MAXIMO, noticias.buscar("Bitcoin", Duration.ofSeconds(2)).size());
    }

    @Test void rechazaUnRssConDoctypeContraXxe() {
        google.cuerpo = "<?xml version=\"1.0\"?><!DOCTYPE rss [<!ENTITY x SYSTEM \"file:///c:/windows/win.ini\">]>"
                + "<rss><channel><item><title>&x;</title><link>https://a</link></item></channel></rss>";
        var e = assertThrows(ServicioNoDisponible.class, () -> noticias.buscar("Bitcoin", Duration.ofSeconds(2)));
        assertTrue(e.getMessage().contains("RSS inválido"));
    }

    @Test void guardaLasNoticiasDiezMinutos() throws Exception {
        google.cuerpo = rss(item("A", "F", "https://n/1", ""));
        noticias.buscar("Bitcoin", Duration.ofSeconds(2));
        noticias.buscar("bitcoin ", Duration.ofSeconds(2));
        assertEquals(1, google.pedidos.get());
        reloj.avanzar(NoticiasGoogle.VIGENCIA.plusSeconds(1));
        noticias.buscar("Bitcoin", Duration.ofSeconds(2));
        assertEquals(2, google.pedidos.get());
    }

    @Test void trasUnaFallaSePausa() throws Exception {
        google.estado = 503;
        assertThrows(ServicioNoDisponible.class, () -> noticias.buscar("Bitcoin", Duration.ofSeconds(2)));
        google.estado = 200; google.cuerpo = rss();
        assertThrows(ServicioNoDisponible.class, () -> noticias.buscar("Ethereum", Duration.ofSeconds(2)));
        assertEquals(1, google.pedidos.get());
        reloj.avanzar(NoticiasGoogle.PAUSA_TRAS_FALLA.plusSeconds(1));
        assertTrue(noticias.buscar("Ethereum", Duration.ofSeconds(2)).isEmpty());
    }

    @Test void siNoRespondeEnElPlazoCorta() {
        google.demoraMs = 1500;
        assertThrows(ServicioNoDisponible.class, () -> noticias.buscar("Bitcoin", Duration.ofMillis(300)));
        assertThrows(ServicioNoDisponible.class, () -> new NoticiasGoogle(google.url(""), reloj).buscar("Bitcoin", Duration.ZERO));
    }
}
