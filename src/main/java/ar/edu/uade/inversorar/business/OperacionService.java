package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;
import ar.edu.uade.inversorar.business.dto.ResumenPortfolioDto;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CompraService {
    void registrarCompra(CompraRequest request);
    List<InstrumentoDto> listarInstrumentos();
    ResumenPortfolioDto obtenerResumen(Long portfolioId);
}
