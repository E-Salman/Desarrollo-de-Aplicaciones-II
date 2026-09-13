package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;
import ar.edu.uade.inversorar.business.dto.ResumenPortfolioDto;
import ar.edu.uade.inversorar.business.dto.VentaRequest;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface OperacionService {
    void registrarCompra(CompraRequest request);
    void registrarVenta(VentaRequest request);
    List<InstrumentoDto> listarInstrumentos();
    ResumenPortfolioDto obtenerResumen(Long portfolioId);
}
