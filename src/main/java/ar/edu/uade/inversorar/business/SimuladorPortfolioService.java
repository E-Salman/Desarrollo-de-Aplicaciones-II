package ar.edu.uade.inversorar.business;
import jakarta.ejb.Local;
import java.math.BigDecimal;
import ar.edu.uade.inversorar.data.TipoInstrumento;
import ar.edu.uade.inversorar.business.dto.SimulacionDto;
@Local public interface SimuladorPortfolioService {
    void definirCapital(BigDecimal capital);
    void definirPorcentaje(TipoInstrumento tipo, BigDecimal porcentaje);
    SimulacionDto calcular();
    void cerrar();
}
