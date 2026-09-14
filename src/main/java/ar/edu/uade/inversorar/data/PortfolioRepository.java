package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PortfolioRepository {
    @PersistenceContext private EntityManager em;
    public java.util.Optional<Portfolio> porPropietario(String propietario) {
        return em.createQuery("select p from Portfolio p where p.propietario = :propietario", Portfolio.class)
            .setParameter("propietario", propietario).getResultStream().findFirst();
    }
    public Portfolio buscar(Long id) { return em.find(Portfolio.class, id); }
    public void guardar(Portfolio portfolio) { em.persist(portfolio); }
}
