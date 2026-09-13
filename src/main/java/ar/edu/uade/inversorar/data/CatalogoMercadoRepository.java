package ar.edu.uade.inversorar.data;

import jakarta.annotation.security.RolesAllowed;
import ar.edu.uade.inversorar.business.dto.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

/** Lectura del esquema recibido del equipo; no crea tablas ni modifica cotizaciones. */
@Stateless @RolesAllowed("USUARIO")
public class CatalogoMercadoRepository {
    @PersistenceContext private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<InstrumentoMercado> listar(String busqueda, int pagina, int cantidad) {
        var query = em.createNativeQuery("""
            SELECT i.id, i.simbolo, i.nombre, i.tipo, i.exchange, i.estado,
                   (SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.simbolo) ELSE NULL END
                    FROM instrumento_activos ia JOIN activos a ON a.id=ia.activo_id
                    WHERE ia.instrumento_id=i.id AND ia.rol='COTIZACION'), p.cierre, CAST(p.fecha_hora AS CHAR)
            FROM instrumentos i
            LEFT JOIN precios p ON p.id=(SELECT px.id FROM precios px
                WHERE px.instrumento_id=i.id ORDER BY px.fecha_hora DESC LIMIT 1)
            WHERE LOWER(i.simbolo) LIKE :busqueda OR LOWER(i.nombre) LIKE :busqueda
            ORDER BY i.simbolo, i.exchange, i.id
            """);
        query.setParameter("busqueda", "%" + busqueda.toLowerCase(java.util.Locale.ROOT) + "%");
        query.setFirstResult(pagina*cantidad); query.setMaxResults(cantidad);
        List<Object[]> rows=query.getResultList();
        return rows.stream().map(InstrumentoMercado::new).toList();
    }

    @SuppressWarnings("unchecked")
    public List<PrecioMercado> precios(long instrumentoId, int limite) {
        List<Object[]> rows=em.createNativeQuery("""
            SELECT CAST(fecha_hora AS CHAR), apertura, maximo, minimo, cierre, volumen
            FROM precios WHERE instrumento_id=:id ORDER BY fecha_hora DESC
            """).setParameter("id",instrumentoId).setMaxResults(limite).getResultList();
        return rows.stream().map(PrecioMercado::new).toList();
    }

    public void actualizarCotizacion(Instrumento instrumento) {
        @SuppressWarnings("unchecked") List<Object[]> rows=em.createNativeQuery("""
            SELECT p.cierre, CAST(p.fecha_hora AS CHAR),
              (SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.simbolo) ELSE NULL END
               FROM instrumento_activos ia JOIN activos a ON a.id=ia.activo_id
               WHERE ia.instrumento_id=:id AND ia.rol='COTIZACION')
            FROM precios p WHERE p.instrumento_id=:id ORDER BY p.fecha_hora DESC
            """).setParameter("id",instrumento.getId()).setMaxResults(1).getResultList();
        if (rows.isEmpty()) instrumento.actualizarCotizacion(null,null,null);
        else { var row=rows.get(0); instrumento.actualizarCotizacion((BigDecimal)row[0],(String)row[2],row[1].toString()); }
    }

}
