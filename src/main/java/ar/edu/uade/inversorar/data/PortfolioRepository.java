package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PortfolioRepository {
    @PersistenceContext private EntityManager em;
    public Portfolio buscar(Long id) { return em.find(Portfolio.class, id); }
    public void guardar(Portfolio portfolio) { em.persist(portfolio); }
}
