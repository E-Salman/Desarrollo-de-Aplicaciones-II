package ar.edu.uade.inversorar.business;

import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CompraService {
    void registrarCompra(CompraRequest request);
    List<InstrumentoDto> listarInstrumentos();
    ResumenPortfolioDto obtenerResumen(Long portfolioId);
}
