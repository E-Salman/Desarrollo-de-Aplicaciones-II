package ar.edu.uade.inversorar.presentation;

import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Inject private SecurityContext security;

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getUserPrincipal() != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        Csrf.preparar(request);
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!Csrf.valido(request)) { response.sendError(403); return; }
        if (request.getUserPrincipal() != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        if (email != null && email.length() <= 255 && password != null && password.length() <= 128) {
            var credential = new UsernamePasswordCredential(email, password);
            try {
                status = security.authenticate(request, response,
                        AuthenticationParameters.withParams().newAuthentication(true).credential(credential));
            } finally { credential.getPassword().clear(); }
        }
        if (status == AuthenticationStatus.SUCCESS) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else if (status != AuthenticationStatus.SEND_CONTINUE) {
            response.setStatus(401);
            request.setAttribute("error", "Email o contraseña incorrectos.");
            doGet(request, response);
        }
    }
}
