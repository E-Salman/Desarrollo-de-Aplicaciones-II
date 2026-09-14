package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.UsuarioService;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/recuperar")
public class RecuperarServlet extends HttpServlet {

    @EJB
    private UsuarioService usuarioService;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/views/recuperar.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String email = request.getParameter("email");

        String token =
                usuarioService.generarTokenRecuperacion(email);

        if (token == null) {
            request.setAttribute(
                    "error",
                    "No existe una cuenta con ese email."
            );

            request.getRequestDispatcher("/WEB-INF/views/recuperar.jsp")
                   .forward(request, response);

            return;
        }

        String enlace =
                request.getScheme()
                + "://"
                + request.getServerName()
                + ":"
                + request.getServerPort()
                + request.getContextPath()
                + "/restablecer?token="
                + token;

        request.setAttribute(
                "mensaje",
                "Se generó correctamente el enlace de recuperación."
        );

        // Temporal, solo para probar antes de mandar el mail real
        request.setAttribute(
                "enlaceRecuperacion",
                enlace
        );

        request.getRequestDispatcher("/WEB-INF/views/recuperar.jsp")
               .forward(request, response);
    }
}