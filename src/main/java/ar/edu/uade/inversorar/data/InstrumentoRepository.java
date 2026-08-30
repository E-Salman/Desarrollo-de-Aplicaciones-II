package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class InstrumentoRepository {
    @PersistenceContext private EntityManager em;
    public List<Instrumento> listar() { return em.createQuery("select i from Instrumento i order by i.tipo, i.nombre", Instrumento.class).getResultList(); }
    public Optional<Instrumento> buscarPorTicker(String ticker) { return em.createQuery("select i from Instrumento i where i.ticker = :ticker", Instrumento.class).setParameter("ticker", ticker).getResultStream().findFirst(); }
    public void guardar(Instrumento instrumento) { em.persist(instrumento); }
}
