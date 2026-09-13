package ar.edu.uade.inversorar.business;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import ar.edu.uade.inversorar.data.Instrumento;
import ar.edu.uade.inversorar.data.CatalogoMercadoRepository;
import jakarta.inject.Inject;
/** Strategy activa: último cierre del catálogo MySQL o precio ilustrativo de la demo H2. */
@ApplicationScoped public class CotizacionCatalogo implements CotizacionStrategy {
    @Inject private ConfiguracionApp configuracion;
    @Inject private CatalogoMercadoRepository mercado;
    public BigDecimal cotizar(Instrumento instrumento) {
        if (configuracion!=null && configuracion.catalogoMysql()) mercado.actualizarCotizacion(instrumento);
        if (instrumento.getCotizacionActual()==null || instrumento.getMonedaCotizacion()==null)
            throw new ReglaNegocioException("El instrumento no tiene precio o moneda de cotización disponible");
        return instrumento.getCotizacionActual();
    }
}
