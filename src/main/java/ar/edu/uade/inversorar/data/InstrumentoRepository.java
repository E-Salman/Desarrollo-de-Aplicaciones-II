package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import ar.edu.uade.inversorar.business.ConfiguracionApp;

@Stateless
public class InstrumentoRepository {
    @PersistenceContext private EntityManager em;
    @Inject private ConfiguracionApp configuracion;
    @Inject private CatalogoMercadoRepository mercado;
    public List<Instrumento> listar() {
        if (configuracion.catalogoMysql()) return mercado.listar("",0,10000).stream().map(Instrumento::deMercado).toList();
        return em.createQuery("select i from Instrumento i order by i.tipo, i.nombre", Instrumento.class).getResultList();
    }
    public Optional<Instrumento> buscarPorTicker(String ticker) {
        var resultados=em.createQuery("select i from Instrumento i where i.ticker = :ticker", Instrumento.class).setParameter("ticker", ticker).setMaxResults(2).getResultList();
        if (resultados.size()>1) throw new ar.edu.uade.inversorar.business.ReglaNegocioException("Símbolo ambiguo entre mercados");
        if (configuracion.catalogoMysql() && !resultados.isEmpty()) mercado.actualizarCotizacion(resultados.get(0));
        return resultados.stream().findFirst();
    }
    public void guardar(Instrumento instrumento) { em.persist(instrumento); }
}
