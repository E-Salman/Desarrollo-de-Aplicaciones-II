package ar.edu.uade.inversorar.presentation.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * 429 Too Many Requests con Retry-After, contra fuerza bruta y abuso de la API.
 * <ul>
 *   <li>Login (POST /login): {@value #LOGIN_POR_EMAIL_DEFECTO} intentos por minuto por email y
 *       {@value #LOGIN_POR_IP_DEFECTO} por IP (frena probar muchas contraseñas o muchas cuentas).</li>
 *   <li>/api: {@value #LECTURAS_DEFECTO} consultas y {@value #ESCRITURAS_DEFECTO} operaciones (POST/PUT/DELETE)
 *       por minuto por usuario. Alcanza para el dashboard con sondeo cada 3 s y varias pestañas.</li>
 *   <li>POST /api/asistente: {@value #ASISTENTE_DEFECTO} preguntas por minuto por usuario, en un balde propio.</li>
 * </ul>
 * Los límites se cambian con propiedades del sistema (inversorar.limite.login.email, .login.ip, .lecturas,
 * .escrituras), por ejemplo para la prueba de carga. La API responde Problem Details; el login, una página HTML.
 */
@WebFilter(urlPatterns = {"/api/*", "/login"})
public class FiltroLimitePedidos extends HttpFilter {
    private static final Logger LOG = Logger.getLogger(FiltroLimitePedidos.class.getName());
    static final int LOGIN_POR_EMAIL_DEFECTO = 5, LOGIN_POR_IP_DEFECTO = 20, LECTURAS_DEFECTO = 300, ESCRITURAS_DEFECTO = 30,
            ASISTENTE_DEFECTO = 10;
    static final String RUTA_ASISTENTE = "/api/asistente";

    private final LimitadorPedidos loginPorEmail = limitador("inversorar.limite.login.email", LOGIN_POR_EMAIL_DEFECTO);
    private final LimitadorPedidos loginPorIp = limitador("inversorar.limite.login.ip", LOGIN_POR_IP_DEFECTO);
    private final LimitadorPedidos lecturas = limitador("inversorar.limite.lecturas", LECTURAS_DEFECTO);
    private final LimitadorPedidos escrituras = limitador("inversorar.limite.escrituras", ESCRITURAS_DEFECTO);
    /** Preguntas al asistente de IA: cuidan el cupo gratuito del proveedor y no comparten balde con las compras. */
    private final LimitadorPedidos asistente = limitador("inversorar.limite.asistente", ASISTENTE_DEFECTO);

    private static LimitadorPedidos limitador(String propiedad, int defecto) {
        int valor = defecto;
        try { valor = Integer.parseInt(System.getProperty(propiedad, String.valueOf(defecto)).trim()); }
        catch (NumberFormatException e) { LOG.warning(propiedad + " inválida; se usa " + defecto); }
        return new LimitadorPedidos(Math.max(valor, 1), Clock.systemUTC());
    }

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        String ruta = pedido.getRequestURI().substring(pedido.getContextPath().length());
        String ip = pedido.getRemoteAddr();
        if (ruta.equals("/login")) {
            if ("POST".equals(pedido.getMethod())) {
                pedido.setCharacterEncoding("UTF-8"); // antes de leer parámetros, igual que LoginServlet
                String email = pedido.getParameter("email");
                Duration espera = loginPorIp.consumir("ip:" + ip);
                if (espera == null && email != null)
                    espera = loginPorEmail.consumir("email:" + email.trim().toLowerCase(Locale.ROOT));
                if (espera != null) { rechazarLogin(respuesta, espera, ip); return; }
            }
        } else {
            var usuario = pedido.getUserPrincipal();
            String clave = usuario != null ? "usuario:" + usuario.getName() : "ip:" + ip;
            String metodo = pedido.getMethod();
            boolean consulta = metodo.equals("GET") || metodo.equals("HEAD") || metodo.equals("OPTIONS");
            LimitadorPedidos limite = consulta ? lecturas : ruta.equals(RUTA_ASISTENTE) ? asistente : escrituras;
            Duration espera = limite.consumir(clave);
            if (espera != null) { rechazarApi(pedido, respuesta, espera, limite, consulta ? "consultas" : limite == asistente ? "preguntas al asistente" : "operaciones"); return; }
        }
        cadena.doFilter(pedido, respuesta);
    }

    static long segundos(Duration espera) { return Math.max(1, (espera.toMillis() + 999) / 1000); }

    private void rechazarApi(HttpServletRequest pedido, HttpServletResponse respuesta, Duration espera,
                             LimitadorPedidos limite, String que) throws IOException {
        long s = segundos(espera);
        LOG.info("429 en " + pedido.getMethod() + " " + pedido.getRequestURI() + " (reintentar en " + s + " s)");
        var problema = Problema.de("demasiados-pedidos", "Demasiados pedidos", 429,
                "Superaste el límite de " + limite.porMinuto() + " " + que + " por minuto. Probá de nuevo en " + s + " s.",
                pedido.getRequestURI());
        respuesta.setStatus(429);
        respuesta.setHeader("Retry-After", String.valueOf(s));
        respuesta.setContentType(Problema.TIPO_MEDIO);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(problema.json());
    }

    private void rechazarLogin(HttpServletResponse respuesta, Duration espera, String ip) throws IOException {
        long s = segundos(espera);
        LOG.warning("Demasiados intentos de inicio de sesión desde " + ip + "; bloqueado " + s + " s");
        respuesta.setStatus(429);
        respuesta.setHeader("Retry-After", String.valueOf(s));
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setContentType("text/html; charset=UTF-8");
        respuesta.getOutputStream().write(("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><title>Demasiados intentos</title></head>"
                + "<body><main><h1>Demasiados intentos de inicio de sesión</h1><p>Por seguridad, esperá " + s
                + " segundos antes de volver a intentar.</p><p><a href=\"login\">Volver al inicio de sesión</a></p></main></body></html>")
                .getBytes(StandardCharsets.UTF_8));
    }
}
