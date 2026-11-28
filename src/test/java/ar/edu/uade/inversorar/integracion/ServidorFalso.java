package ar.edu.uade.inversorar.integracion;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Servidor HTTP local para los tests: hace de mercado legado, Binance, Gemini o Google News sin salir a internet. */
public class ServidorFalso implements AutoCloseable {
    public volatile int estado = 200;
    public volatile String cuerpo = "";
    public volatile long demoraMs = 0;
    /** URI del último pedido, una línea en blanco y su cuerpo. */
    public volatile String ultimoPedido;
    public volatile Headers ultimosEncabezados;
    public final AtomicInteger pedidos = new AtomicInteger();
    private final HttpServer server;

    public ServidorFalso() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", ex -> {
            try {
                pedidos.incrementAndGet();
                ultimosEncabezados = ex.getRequestHeaders();
                ultimoPedido = ex.getRequestURI() + "\n" + new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                if (demoraMs > 0) Thread.sleep(demoraMs);
                byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(estado, b.length);
                ex.getResponseBody().write(b);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignorada) {
                // el cliente cortó por timeout
            } finally { ex.close(); }
        });
        server.start();
    }

    public String url(String camino) { return "http://localhost:" + server.getAddress().getPort() + camino; }

    @Override public void close() { server.stop(0); }
}
