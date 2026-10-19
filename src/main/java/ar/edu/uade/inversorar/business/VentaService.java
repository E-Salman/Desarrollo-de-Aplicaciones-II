package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.VentaRequest;

import jakarta.ejb.Local;

@Local
public interface VentaService {
    /** Con MySQL deja la orden PENDIENTE en la cola y la devuelve; en la demo H2 la registra en el momento y devuelve null. */
    ar.edu.uade.inversorar.business.dto.OrdenDto registrarVenta(VentaRequest request);
}
