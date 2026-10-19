package ar.edu.uade.inversorar.data;

import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de órdenes. Las escrituras participan de la transacción de quien llama (Compra, Venta,
 * el Ejecutor de órdenes o el MDB de la DLQ) y no confirman nada por su cuenta. No exige rol: el
 * Ejecutor corre sin usuario autenticado; el control de acceso está en la capa de negocio.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class OrdenRepository {
    @PersistenceContext private EntityManager em;

    /** Orden aceptada que queda PENDIENTE hasta que la procese el Ejecutor. Devuelve su detalle. */
    public OrdenDetalle crearPendiente(Long usuarioId, TipoOperacion tipo, Instrumento instrumento,
                                       BigDecimal cantidad, BigDecimal precioUnitario, LocalDate fecha) {
        BigDecimal total = cantidad.multiply(precioUnitario).setScale(10, java.math.RoundingMode.HALF_UP);
        Orden orden = Orden.pendiente(usuarioId, tipo, fecha, instrumento.getMonedaCotizacion(), total);
        em.persist(orden);
        OrdenDetalle detalle = new OrdenDetalle(orden, instrumento, cantidad, precioUnitario);
        em.persist(detalle);
        return detalle;
    }

    /**
     * La orden con bloqueo de escritura hasta el fin de la transacción: dos consumidores, o dos clics en
     * "Reintentar", no pueden cambiar la misma orden a la vez.
     */
    public Optional<Orden> bloquear(Long ordenId) {
        return Optional.ofNullable(em.find(Orden.class, ordenId, LockModeType.PESSIMISTIC_WRITE));
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<Orden> buscar(Long ordenId) { return Optional.ofNullable(em.find(Orden.class, ordenId)); }

    /** El detalle (instrumento, cantidad, precio) de una orden. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<OrdenDetalle> detalleDe(Long ordenId) {
        return em.createQuery("select d from OrdenDetalle d join fetch d.instrumento where d.orden.id = :id", OrdenDetalle.class)
                .setParameter("id", ordenId).getResultStream().findFirst();
    }

    /** Cantidad del instrumento ya comprometida en ventas PENDIENTE del usuario (todavía no ejecutadas). */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public BigDecimal ventasPendientes(Long usuarioId, Instrumento instrumento) {
        BigDecimal suma = em.createQuery("select sum(d.cantidad) from OrdenDetalle d where d.orden.usuarioId = :u"
                + " and d.instrumento = :i and d.orden.tipo = :t and d.orden.estado = :e", BigDecimal.class)
                .setParameter("u", usuarioId).setParameter("i", instrumento)
                .setParameter("t", TipoOperacion.VENTA).setParameter("e", EstadoOrden.PENDIENTE).getSingleResult();
        return suma == null ? BigDecimal.ZERO : suma;
    }

    /**
     * Órdenes de un usuario con su detalle, las más recientes primero. Con estados vacío trae todas;
     * si no, las que están en alguno de esos estados (por ejemplo RECHAZADA y ERROR).
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrdenDetalle> detallesPorUsuario(Long usuarioId, Collection<EstadoOrden> estados, int maximo) {
        boolean filtra = estados != null && !estados.isEmpty();
        // Sin alias en los join fetch: WildFly usa JPQL estricto, que no los admite.
        var consulta = em.createQuery("select d from OrdenDetalle d join fetch d.orden join fetch d.instrumento"
                + " where d.orden.usuarioId = :u" + (filtra ? " and d.orden.estado in :e" : "")
                + " order by d.orden.fechaHora desc, d.orden.id desc", OrdenDetalle.class).setParameter("u", usuarioId);
        if (filtra) consulta.setParameter("e", estados);
        return consulta.setMaxResults(maximo).getResultList();
    }
}
