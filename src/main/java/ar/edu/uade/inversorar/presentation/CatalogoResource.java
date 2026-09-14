package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.ConfiguracionApp;
import ar.edu.uade.inversorar.business.CompraService;
import ar.edu.uade.inversorar.business.dto.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RequestScoped @Path("catalogo") @Produces(MediaType.APPLICATION_JSON)
public class CatalogoResource {
    @Inject private CompraService catalogo;
    @Inject private ConfiguracionApp configuracion;
    @GET @Path("configuracion") public Map<String,Boolean> configuracion() {
        return Map.of("mysql",configuracion.catalogoMysql());
    }
    private void verificarMysql() {
        if (!configuracion.catalogoMysql()) throw new NotFoundException("Catálogo MySQL no configurado");
    }
    @GET public List<InstrumentoMercado> listar(
            @DefaultValue("") @QueryParam("q") String busqueda,
            @DefaultValue("0") @QueryParam("pagina") int pagina) {
        verificarMysql();
        if (pagina<0 || pagina>10000 || busqueda.length()>80) throw new BadRequestException("Búsqueda inválida");
        return catalogo.listarCatalogo(busqueda,pagina);
    }
    @GET @Path("{id}/precios") public List<PrecioMercado> precios(
            @PathParam("id") long id, @DefaultValue("90") @QueryParam("limite") int limite) {
        verificarMysql();
        if (id<=0 || limite<1 || limite>365) throw new BadRequestException("Rango inválido");
        return catalogo.preciosHistoricos(id,limite);
    }
}
