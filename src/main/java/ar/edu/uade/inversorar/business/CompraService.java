package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.CompraRequest;
import ar.edu.uade.inversorar.business.dto.InstrumentoDto;

import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CompraService {
    List<ar.edu.uade.inversorar.business.dto.InstrumentoMercado> listarCatalogo(String busqueda, int pagina);
    List<ar.edu.uade.inversorar.business.dto.PrecioMercado> preciosHistoricos(long id, int limite);
    /** Con MySQL deja la orden PENDIENTE en la cola y la devuelve; en la demo H2 la registra en el momento y devuelve null. */
    ar.edu.uade.inversorar.business.dto.OrdenDto registrarCompra(CompraRequest request);
    List<InstrumentoDto> listarInstrumentos();

}
