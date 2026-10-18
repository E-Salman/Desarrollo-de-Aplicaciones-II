package ar.edu.uade.inversorar.presentation;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;

@WebServlet("/favoritos")
public class FavoritosServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        Csrf.preparar(request);

        response.setHeader("Cache-Control", "no-store");

        request.getRequestDispatcher(
            "/WEB-INF/views/favoritos.jsp"
        ).forward(request, response);
    }
}