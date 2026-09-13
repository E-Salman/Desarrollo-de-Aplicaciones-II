package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Stateless
@jakarta.annotation.security.RolesAllowed("USUARIO")
public class CompraServiceBean implements CompraService {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CompraServiceBean.class.getName());
    @jakarta.annotation.PostConstruct public void iniciar() { LOG.info("CompraServiceBean inicializado"); }
    @jakarta.annotation.PreDestroy public void destruir() { LOG.info("CompraServiceBean destruido"); }
    @Inject private InstrumentoRepository instrumentos;
    @Inject private PortfolioActual portfolioActual;
    @Inject private OperacionRepository operaciones;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void registrarCompra(CompraRequest request) {
        if (request == null || request.ticker == null || request.ticker.isBlank()) throw new ReglaNegocioException("Debe seleccionar un instrumento.");
        if (request.cantidad == null || request.cantidad.signum() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (request.precioUnitario == null || request.precioUnitario.signum() <= 0) throw new ReglaNegocioException("El precio unitario debe ser mayor que cero.");
        if (request.fecha == null || request.fecha.isAfter(LocalDate.now())) throw new ReglaNegocioException("La fecha de compra es inválida.");
        validarDecimal(request.cantidad, 6, 13, "cantidad");
        validarDecimal(request.precioUnitario, 4, 15, "precio");
        if (request.cantidad.multiply(request.precioUnitario).setScale(4, RoundingMode.HALF_UP).signum() == 0)
            throw new ReglaNegocioException("El total es inferior a la precisión monetaria admitida");
        validarDecimal(request.cantidad.multiply(request.precioUnitario).setScale(4, RoundingMode.HALF_UP), 4, 15, "total");
        Instrumento instrumento = instrumentos.buscarPorTicker(request.ticker.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ReglaNegocioException("El instrumento seleccionado no existe."));
        Portfolio portfolio = portfolioActual.obtener();
        operaciones.guardar(new Operacion(portfolio, instrumento, request.cantidad, request.precioUnitario, request.fecha));
    }

    @Override
    public List<InstrumentoDto> listarInstrumentos() { return instrumentos.listar().stream().map(InstrumentoDto::new).toList(); }

    private void validarDecimal(BigDecimal valor, int escala, int enteros, String campo) {
        BigDecimal normal = valor.stripTrailingZeros();
        if (normal.scale() > escala || normal.precision() - normal.scale() > enteros)
            throw new ReglaNegocioException("Precisión o importe fuera de rango: " + campo);
    }
}
