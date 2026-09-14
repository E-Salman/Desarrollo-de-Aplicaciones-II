package ar.edu.uade.inversorar.presentation;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class Csrf {
    private Csrf() { }
    public static void preparar(HttpServletRequest request) {
        var session = request.getSession();
        if (session.getAttribute("csrf") == null) session.setAttribute("csrf", UUID.randomUUID().toString());
    }
    public static boolean valido(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null && session.getAttribute("csrf") instanceof String token
                && token.equals(request.getParameter("csrf"));
    }
}
