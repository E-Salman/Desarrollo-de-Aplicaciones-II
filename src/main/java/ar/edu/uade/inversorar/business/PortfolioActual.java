package ar.edu.uade.inversorar.business;
import ar.edu.uade.inversorar.data.Portfolio;
import jakarta.ejb.Local;
/** Frontera de identidad compartida por Compra, Portfolio y la futura Venta. */
@Local public interface PortfolioActual { Portfolio obtener(); }
