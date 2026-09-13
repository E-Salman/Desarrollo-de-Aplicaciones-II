package ar.edu.uade.inversorar.business;
import jakarta.ejb.Local;
import ar.edu.uade.inversorar.business.dto.*;
import java.util.List;
import java.math.BigDecimal;
import ar.edu.uade.inversorar.data.TipoInstrumento;
@Local public interface PortfolioService {
    ResumenPortfolioDto obtenerResumen();
    List<PosicionDto> obtenerPosiciones();
    void definirCapital(BigDecimal capital);
    void definirPorcentaje(TipoInstrumento tipo, BigDecimal porcentaje);
    SimulacionDto calcularSimulacion();
    void reiniciarSimulacion();
    void cerrar();
}
