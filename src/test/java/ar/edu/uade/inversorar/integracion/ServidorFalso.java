package ar.edu.uade.inversorar.integracion;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Servidor HTTP local para los tests: hace de mercado legado o de Binance sin salir a internet. */
class ServidorFalso implements AutoCloseable {
    volatile int estado = 200;
    volatile String cuerpo = "";
    volatile long demoraMs = 0;
    volatile String ultimoPedido;
    final AtomicInteger pedidos = new AtomicInteger();
    private final HttpServer server;

    ServidorFalso() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", ex -> {
            try {
                pedidos.incrementAndGet();
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

    String url(String camino) { return "http://localhost:" + server.getAddress().getPort() + camino; }

    @Override public void close() { server.stop(0); }
}
