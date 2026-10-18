package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.data.TipoInstrumento;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/simulador")
@ServletSecurity(@HttpConstraint(rolesAllowed = "USUARIO"))
public class SimuladorServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Csrf.preparar(request);
        response.setHeader("Cache-Control", "no-store");
        request.setAttribute("tiposSimulacion", TipoInstrumento.values());
        request.setAttribute("activePage", "simulator");
        request.getRequestDispatcher("/WEB-INF/views/simulador.jsp").forward(request, response);
    }
}
