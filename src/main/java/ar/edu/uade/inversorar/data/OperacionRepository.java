package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class OperacionRepository {
    @PersistenceContext private EntityManager em;
    public void guardar(Operacion operacion) { em.persist(operacion); }
    public List<Operacion> porPortfolio(Long portfolioId) { return em.createQuery("select o from Operacion o join fetch o.instrumento where o.portfolio.id = :id order by o.fecha desc, o.id desc", Operacion.class).setParameter("id", portfolioId).getResultList(); }
    public List<Operacion> porPortfolioYTicker(Long portfolioId, String ticker) { return em.createQuery("select o from Operacion o join fetch o.instrumento where o.portfolio.id = :id and o.instrumento.ticker = :ticker order by o.fecha asc, o.id asc", Operacion.class).setParameter("id", portfolioId).setParameter("ticker", ticker).getResultList(); }
}
