package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.TipoInstrumento;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SimulacionPlanTest {
    private BigDecimal n(String s) { return new BigDecimal(s); }
    private void igual(String expected, BigDecimal actual) { assertEquals(0, n(expected).compareTo(actual)); }

    @Test void cubreTodosLosTiposSinPerderFraccionesNiCapital() {
        var bean = new PortfolioServiceBean();
        var plan = new EnumMap<TipoInstrumento, BigDecimal>(TipoInstrumento.class);
        for (var tipo : TipoInstrumento.values()) plan.put(tipo, n("20"));
        var dto = bean.definirPlan(n("0.01"), plan);
        assertEquals(Set.of(TipoInstrumento.values()), dto.importes.keySet());
        dto.importes.values().forEach(v -> igual("0.002", v));
        igual("0", dto.disponible);
        igual("0.01", dto.importes.values().stream().reduce(dto.disponible, BigDecimal::add));
    }

    @Test void reemplazaDistribucionCompletaSinExcesoIntermedio() {
        var bean = new PortfolioServiceBean();
        bean.definirPlan(n("100"), Map.of(TipoInstrumento.ACCION, n("100")));
        var dto = bean.definirPlan(n("200"), Map.of(TipoInstrumento.BONO, n("100")));
        igual("200", dto.importes.get(TipoInstrumento.BONO));
        assertFalse(dto.porcentajes.containsKey(TipoInstrumento.ACCION));
        igual("0", dto.disponible);
    }

    @Test void entradaInvalidaNoModificaPlanAnterior() {
        var bean = new PortfolioServiceBean();
        bean.definirPlan(n("100"), Map.of(TipoInstrumento.CRIPTO, n("40")));
        for (var capital : new BigDecimal[]{n("-1"),n("0.001"),n("1000000000000"),null})
            assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(capital, Map.of()));
        for (var porcentaje : new BigDecimal[]{n("-1"),n("100.0001"),n("0.00001")})
            assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(n("200"), Map.of(TipoInstrumento.BONO, porcentaje)));
        assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(n("200"), null));
        assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(n("200"), Map.of(TipoInstrumento.ACCION,n("60"),TipoInstrumento.BONO,n("41"))));
        var nullValue = new HashMap<TipoInstrumento,BigDecimal>(); nullValue.put(TipoInstrumento.ACCION,null);
        assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(n("200"),nullValue));
        var nullKey = new HashMap<TipoInstrumento,BigDecimal>(); nullKey.put(null,n("20"));
        assertThrows(ReglaNegocioException.class, () -> bean.definirPlan(n("200"),nullKey));
        igual("100", bean.calcularSimulacion().capital);
        igual("40", bean.calcularSimulacion().importes.get(TipoInstrumento.CRIPTO));
        igual("60", bean.calcularSimulacion().disponible);
    }

    @Test void admiteLimitesDistribucionParcialYCero() {
        var bean = new PortfolioServiceBean();
        var dto = bean.definirPlan(n("999999999999.99"), Map.of(TipoInstrumento.MONEDA,n("0.0001")));
        igual("999999.99999999", dto.importes.get(TipoInstrumento.MONEDA));
        igual("999999999999.99", dto.disponible.add(dto.importes.get(TipoInstrumento.MONEDA)));
        dto = bean.definirPlan(BigDecimal.ZERO,Map.of(TipoInstrumento.CEDEAR,n("100")));
        igual("0", dto.disponible); igual("0", dto.importes.get(TipoInstrumento.CEDEAR));
    }

    @Test void aislamientoReinicioYCopiaDefensiva() {
        var a = new PortfolioServiceBean(); var b = new PortfolioServiceBean();
        var plan = new EnumMap<TipoInstrumento,BigDecimal>(TipoInstrumento.class);
        plan.put(TipoInstrumento.ACCION,n("50"));
        var dto = a.definirPlan(n("100"),plan);
        plan.clear(); dto.porcentajes.clear(); dto.importes.clear();
        igual("50", a.calcularSimulacion().importes.get(TipoInstrumento.ACCION));
        igual("0", b.calcularSimulacion().capital);
        a.reiniciarSimulacion(); assertTrue(a.calcularSimulacion().porcentajes.isEmpty());
        igual("0", a.calcularSimulacion().capital);
    }
}
