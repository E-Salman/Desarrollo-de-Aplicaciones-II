package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.security.TokensAcceso;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Inject private TokensAcceso tokens;

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!Csrf.valido(request)) { response.sendError(403); return; }
        request.logout();
        tokens.cerrar(request, response);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
