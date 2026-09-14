package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.UsuarioService;
import ar.edu.uade.inversorar.data.Usuario;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {

    @EJB
    private UsuarioService usuarioService;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request
            .getRequestDispatcher("/WEB-INF/views/registro.jsp")
            .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String nombre =
                request.getParameter("nombre");

        String apellido =
                request.getParameter("apellido");

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");

        Usuario usuario =
                usuarioService.registrar(
                        nombre,
                        apellido,
                        email,
                        password
                );

        if (usuario == null) {

            request.setAttribute(
                    "error",
                    "Ya existe una cuenta con ese email."
            );

            request
                .getRequestDispatcher("/WEB-INF/views/registro.jsp")
                .forward(request, response);

            return;
        }

        response.sendRedirect(
                request.getContextPath() + "/login"
        );
    }
}