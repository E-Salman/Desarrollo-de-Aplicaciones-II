package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

@Stateless
public class UsuarioRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Usuario> buscarPorEmail(String email) {

        return em.createQuery(
                "select u from Usuario u where u.email = :email",
                Usuario.class
            )
            .setParameter("email", email)
            .getResultStream()
            .findFirst();
    }

    public void guardar(Usuario usuario) {
        em.persist(usuario);
        em.flush();
    }

    public Optional<Usuario> buscarPorId(Long id) {
    return Optional.ofNullable(
        em.find(Usuario.class, id)
    );
}
}
