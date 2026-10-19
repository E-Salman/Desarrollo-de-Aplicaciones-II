package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.dto.SimulacionDto;
import ar.edu.uade.inversorar.business.ReglaNegocioException;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.EnumMap;

/** Guardado completo de la planificación de la sesión, sin escribir en la cartera. */
@RequestScoped
@Path("portfolio/simulacion/plan")
@Produces(MediaType.APPLICATION_JSON)
public class PlanSimulacionResource {
    @Inject private PortfolioSesion sesion;
    @Context private HttpServletRequest request;

    @PUT @Consumes(MediaType.APPLICATION_JSON)
    public SimulacionDto guardar(Plan entrada) {
        if (entrada == null) throw new BadRequestException("Debe indicar el plan");
        if (entrada.porcentajes == null) throw new ReglaNegocioException("Debe indicar la distribución");
        var plan = new EnumMap<TipoInstrumento, BigDecimal>(TipoInstrumento.class);
        for (var item : entrada.porcentajes.entrySet()) {
            try { plan.put(TipoInstrumento.valueOf(item.getKey()), item.getValue()); }
            catch (IllegalArgumentException | NullPointerException e) {
                throw new ReglaNegocioException("Tipo de instrumento desconocido");
            }
        }
        request.getSession(true);
        var servicio = sesion.servicio(request.getUserPrincipal().getName());
        return entrada.moneda == null ? servicio.definirPlan(entrada.capital, plan)
                : servicio.definirPlan(entrada.capital, plan, entrada.moneda);
    }

    public static class Plan {
        public BigDecimal capital;
        public String moneda;
        public Map<String, BigDecimal> porcentajes;
    }
}
