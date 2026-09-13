package ar.edu.uade.inversorar.business;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import ar.edu.uade.inversorar.data.Instrumento;
/** Strategy activa: cotización de catálogo, expresada en USD para esta demo. */
@ApplicationScoped public class CotizacionCatalogo implements CotizacionStrategy {
    public BigDecimal cotizar(Instrumento instrumento) { return instrumento.getCotizacionActual(); }
}
