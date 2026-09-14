package ar.edu.uade.inversorar.presentation;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;

import java.io.IOException;

@WebServlet("/recuperar")
public class RecuperarServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request
            .getRequestDispatcher("/WEB-INF/views/recuperar.jsp")
            .forward(request, response);
    }
}