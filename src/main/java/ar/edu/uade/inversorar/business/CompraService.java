package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;

import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CompraService {
    List<ar.edu.uade.inversorar.business.dto.InstrumentoMercado> listarCatalogo(String busqueda, int pagina);
    List<ar.edu.uade.inversorar.business.dto.PrecioMercado> preciosHistoricos(long id, int limite);
    void registrarCompra(CompraRequest request);
    List<InstrumentoDto> listarInstrumentos();

}
