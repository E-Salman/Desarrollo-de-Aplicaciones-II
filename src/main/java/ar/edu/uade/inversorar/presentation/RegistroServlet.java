package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.*;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {
    @EJB private UsuarioService usuarioService;

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Csrf.preparar(request);
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher("/WEB-INF/views/registro.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!Csrf.valido(request)) { response.sendError(403); return; }
        try {
            String password = request.getParameter("password");
            if (password == null || !password.equals(request.getParameter("confirmarPassword")))
                throw new RegistroException("Las contraseñas no coinciden.");
            var usuario = usuarioService.registrar(request.getParameter("nombre"),
                    request.getParameter("apellido"), request.getParameter("email"), password);
            if (usuario == null) throw new RegistroException("Ya existe una cuenta con ese email.");
            response.sendRedirect(request.getContextPath() + "/login?registrado=1");
            return;
        } catch (RegistroException e) {
            response.setStatus(400);
            request.setAttribute("error", e.getMessage());
        } catch (EJBException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "No se pudo registrar la cuenta", e);
            response.setStatus(503);
            request.setAttribute("error", "No pudimos crear la cuenta. Intentá nuevamente más tarde.");
        }
        doGet(request, response);
    }
}
