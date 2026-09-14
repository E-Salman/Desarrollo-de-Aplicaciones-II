package ar.edu.uade.inversorar.data;

import jakarta.annotation.security.RolesAllowed;
import ar.edu.uade.inversorar.business.dto.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Lectura del esquema recibido del equipo; no crea tablas ni modifica cotizaciones. */
@Stateless @RolesAllowed("USUARIO")
public class CatalogoMercadoRepository {
    private static final Set<String> MONEDAS_USD = Set.of("USD", "USDT", "USDC", "BUSD", "DAI", "TUSD", "USDP");
    private static final int SALTOS_MAXIMOS = 4;
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

    /**
     * Tasa de conversión de {@code simbolo} a USD, saltando entre pares del catálogo hasta encontrar
     * un stablecoin (o USD) si no hay una cotización directa. Devuelve null si no encuentra ningún camino
     * dentro de {@link #SALTOS_MAXIMOS} saltos.
     */
    public BigDecimal tasaAUsd(String simbolo) {
        if (simbolo == null) return null;
        if (MONEDAS_USD.contains(simbolo)) return BigDecimal.ONE;
        Map<String, BigDecimal> visitados = new HashMap<>();
        visitados.put(simbolo, BigDecimal.ONE);
        List<String> frontera = List.of(simbolo);
        for (int salto = 0; salto < SALTOS_MAXIMOS && !frontera.isEmpty(); salto++) {
            List<String> siguiente = new ArrayList<>();
            for (String actual : frontera) {
                BigDecimal tasaActual = visitados.get(actual);
                for (Object[] fila : vecinos(actual)) {
                    String vecino = (String) fila[0];
                    BigDecimal tasaVecino = (BigDecimal) fila[1];
                    if (vecino == null || tasaVecino == null || visitados.containsKey(vecino)) continue;
                    BigDecimal acumulada = tasaActual.multiply(tasaVecino);
                    if (MONEDAS_USD.contains(vecino)) return acumulada.setScale(15, RoundingMode.HALF_UP);
                    visitados.put(vecino, acumulada);
                    siguiente.add(vecino);
                }
            }
            frontera = siguiente;
        }
        return null;
    }

    /** Monedas vinculadas a {@code simbolo} por algún par del catálogo, con la tasa "1 simbolo = tasa vecino". */
    @SuppressWarnings("unchecked")
    private List<Object[]> vecinos(String simbolo) {
        return em.createNativeQuery("""
            SELECT ac.simbolo, p.cierre
            FROM activos ab
            JOIN instrumento_activos ba ON ba.activo_id = ab.id AND ba.rol = 'BASE'
            JOIN instrumento_activos ca ON ca.instrumento_id = ba.instrumento_id AND ca.rol = 'COTIZACION'
            JOIN activos ac ON ac.id = ca.activo_id
            JOIN precios p ON p.id = (SELECT px.id FROM precios px WHERE px.instrumento_id = ba.instrumento_id ORDER BY px.fecha_hora DESC LIMIT 1)
            WHERE ab.simbolo = :simbolo
            UNION ALL
            SELECT ab.simbolo, 1 / p.cierre
            FROM activos ac
            JOIN instrumento_activos ca ON ca.activo_id = ac.id AND ca.rol = 'COTIZACION'
            JOIN instrumento_activos ba ON ba.instrumento_id = ca.instrumento_id AND ba.rol = 'BASE'
            JOIN activos ab ON ab.id = ba.activo_id
            JOIN precios p ON p.id = (SELECT px.id FROM precios px WHERE px.instrumento_id = ca.instrumento_id ORDER BY px.fecha_hora DESC LIMIT 1)
            WHERE ac.simbolo = :simbolo AND p.cierre <> 0
            """).setParameter("simbolo", simbolo).getResultList();
    }
}
