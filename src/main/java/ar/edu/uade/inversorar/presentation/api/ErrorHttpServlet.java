package ar.edu.uade.inversorar.presentation.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Página de error del contenedor (web.xml). Los 401/403 los produce la seguridad antes de llegar a JAX-RS, así que
 * los ExceptionMapper no los ven: acá se completan. Para /api responde Problem Details; para las páginas, HTML simple.
 */
@WebServlet("/error-http")
public class ErrorHttpServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest pedido, HttpServletResponse respuesta) throws IOException {
        Object codigo = pedido.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = codigo instanceof Integer i ? i : respuesta.getStatus();
        String uri = (String) pedido.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String api = pedido.getContextPath() + "/api";
        respuesta.setStatus(status);
        respuesta.setCharacterEncoding("UTF-8");
        if (uri != null && (uri.equals(api) || uri.startsWith(api + "/"))) {
            respuesta.setContentType(Problema.TIPO_MEDIO);
            respuesta.getWriter().write(Problema.http(status, detalle(status), uri).json());
            return;
        }
        respuesta.setContentType("text/html; charset=UTF-8");
        respuesta.getWriter().write("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><title>" + Problema.titulo(status)
                + "</title></head><body><main><h1>" + Problema.titulo(status) + "</h1><p>" + detalle(status)
                + "</p><p><a href=\"" + pedido.getContextPath() + "/dashboard\">Volver al inicio</a></p></main></body></html>");
    }

    static String detalle(int status) {
        return switch (status) {
            case 401 -> "Iniciá sesión para usar la API.";
            case 403 -> "Tu cuenta no tiene permiso para este recurso.";
            case 404 -> "El recurso pedido no existe.";
            case 405 -> "El método HTTP no está permitido en este recurso.";
            default -> status >= 500 ? "Ocurrió un error inesperado en el servidor." : Problema.titulo(status) + ".";
        };
    }
}
