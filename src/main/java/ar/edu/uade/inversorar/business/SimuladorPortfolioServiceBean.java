package ar.edu.uade.inversorar.business;
import jakarta.ejb.*;
import jakarta.annotation.*;
import jakarta.annotation.security.RolesAllowed;
import java.math.*;
import java.util.*;
import java.util.logging.Logger;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import ar.edu.uade.inversorar.business.dto.SimulacionDto;
@Stateful @RolesAllowed("USUARIO")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SimuladorPortfolioServiceBean implements SimuladorPortfolioService {
    private static final Logger LOG = Logger.getLogger(SimuladorPortfolioServiceBean.class.getName());
    private final String conversacion = UUID.randomUUID().toString();
    private BigDecimal capital = BigDecimal.ZERO;
    private final Map<TipoInstrumento, BigDecimal> porcentajes = new EnumMap<>(TipoInstrumento.class);
    @PostConstruct public void iniciar() { LOG.info("Simulador inicializado " + conversacion); }
    @PreDestroy public void destruir() { LOG.info("Simulador destruido " + conversacion); }
    public void definirCapital(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(new BigDecimal("999999999999.99")) > 0 || valor.stripTrailingZeros().scale() > 2)
            throw new ReglaNegocioException("Capital inválido: máximo 999999999999.99 y dos decimales");
        capital = valor;
    }
    public void definirPorcentaje(TipoInstrumento tipo, BigDecimal valor) {
        if (tipo == null || valor == null || valor.signum() < 0 || valor.compareTo(BigDecimal.valueOf(100)) > 0 || valor.stripTrailingZeros().scale() > 4)
            throw new ReglaNegocioException("Porcentaje inválido");
        BigDecimal total = porcentajes.entrySet().stream().filter(e -> e.getKey() != tipo).map(Map.Entry::getValue).reduce(valor, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) throw new ReglaNegocioException("La distribución supera 100%");
        porcentajes.put(tipo, valor);
    }
    public SimulacionDto calcular() {
        SimulacionDto dto = new SimulacionDto(); dto.capital = capital;
        dto.porcentajes = new EnumMap<>(porcentajes); dto.importes = new EnumMap<>(TipoInstrumento.class);
        // Se conservan fracciones de centavo para que la suma nunca exceda el capital.
        porcentajes.forEach((tipo, valor) -> dto.importes.put(tipo, capital.multiply(valor).movePointLeft(2)));
        dto.disponible = capital.subtract(dto.importes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }
    @Remove @jakarta.annotation.security.PermitAll public void cerrar() { porcentajes.clear(); capital = BigDecimal.ZERO; }
}
