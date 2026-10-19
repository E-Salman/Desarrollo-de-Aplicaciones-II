package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.OrdenDto;
import ar.edu.uade.inversorar.data.EstadoOrden;
import jakarta.ejb.Local;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Consulta y gestión de las órdenes del usuario autenticado (pop-up de transacciones). */
@Local
public interface OrdenService {
    /** Las órdenes del usuario, más recientes primero; con estados vacío, todas. */
    List<OrdenDto> misOrdenes(Set<EstadoOrden> estados, int limite);
    /** La orden, solo si es del usuario. */
    Optional<OrdenDto> orden(long ordenId);
    /** Cancela una orden PENDIENTE: el Ejecutor la va a descartar sin ejecutarla. */
    OrdenDto cancelar(long ordenId);
    /** Vuelve a enviar a la cola una orden RECHAZADA o con ERROR. */
    OrdenDto reintentar(long ordenId);
    /** Elimina de la vista una orden RECHAZADA o con ERROR; queda en el historial como DESCARTADA. */
    OrdenDto descartar(long ordenId);
}
