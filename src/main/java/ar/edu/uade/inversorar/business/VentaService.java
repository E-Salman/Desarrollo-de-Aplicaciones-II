package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.business.dto.VentaRequest;

import jakarta.ejb.Local;

@Local
public interface VentaService {
    void registrarVenta(VentaRequest request);
}
