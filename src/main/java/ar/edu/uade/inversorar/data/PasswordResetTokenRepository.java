package ar.edu.uade.inversorar.data;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

@Stateless
public class PasswordResetTokenRepository {

    @PersistenceContext
    private EntityManager em;

    public void guardar(PasswordResetToken token) {
        em.persist(token);
    }

    public Optional<PasswordResetToken> buscarPorToken(String token) {
        return em.createQuery(
                "select t from PasswordResetToken t where t.token = :token",
                PasswordResetToken.class
            )
            .setParameter("token", token)
            .getResultStream()
            .findFirst();
    }

    public void eliminar(PasswordResetToken token) {
        if (em.contains(token)) {
            em.remove(token);
        } else {
            em.remove(em.merge(token));
        }
    }

    public void eliminarPorUsuario(Long usuarioId) {
        em.createQuery(
                "delete from PasswordResetToken t where t.usuarioId = :usuarioId"
            )
            .setParameter("usuarioId", usuarioId)
            .executeUpdate();
    }
}