package ar.edu.uade.inversorar.business;
import jakarta.ejb.Local;
import ar.edu.uade.inversorar.business.dto.*;
import java.util.List;
@Local public interface PortfolioService {
    ResumenPortfolioDto obtenerResumen();
    List<PosicionDto> obtenerPosiciones();
}
