package ar.edu.uade.inversorar.data;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Participa de la transacción de Compra; no confirma una orden por su cuenta. */
@Stateless @RolesAllowed("USUARIO")
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class OrdenRepository {
    @PersistenceContext private EntityManager em;
    public OrdenDetalle guardarCompra(Operacion operacion) {
        Orden orden = new Orden(operacion.getPortfolio().getUsuarioId(),
                operacion.getInstrumento().getMonedaCotizacion(), operacion.getTotal());
        em.persist(orden);
        OrdenDetalle detalle = new OrdenDetalle(orden, operacion);
        em.persist(detalle);
        return detalle;
    }
}
