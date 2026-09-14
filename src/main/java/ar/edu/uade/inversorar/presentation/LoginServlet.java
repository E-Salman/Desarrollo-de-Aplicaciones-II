package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.UsuarioService;
import ar.edu.uade.inversorar.data.Usuario;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @EJB
    private UsuarioService usuarioService;


    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request
            .getRequestDispatcher("/WEB-INF/views/login.jsp")
            .forward(request, response);
    }


    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");


        Usuario usuario =
                usuarioService.login(email, password);


        if (usuario == null) {

            request.setAttribute(
                "error",
                "Email o contraseña incorrectos."
            );

            request
                .getRequestDispatcher("/WEB-INF/views/login.jsp")
                .forward(request, response);

            return;
        }


        HttpSession session =
                request.getSession();

        session.setAttribute(
                "usuarioId",
                usuario.getId()
        );

        session.setAttribute(
                "nombreUsuario",
                usuario.getNombre()
        );

        session.setAttribute(
                "emailUsuario",
                usuario.getEmail()
        );


        response.sendRedirect(
                request.getContextPath() + "/dashboard"
        );
    }
}