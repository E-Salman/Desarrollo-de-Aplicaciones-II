package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.math.BigDecimal;

@Singleton @Startup
public class DatosIniciales {
    @Inject private PortfolioRepository portfolios;
    @Inject private InstrumentoRepository instrumentos;
    @PostConstruct public void cargar() {
        if (portfolios.buscar(1L) == null) portfolios.guardar(new Portfolio("Mi Portfolio"));
        if (!instrumentos.listar().isEmpty()) return;
        alta("Apple", "AAPL", TipoInstrumento.ACCION, "195"); alta("Microsoft", "MSFT", TipoInstrumento.ACCION, "420"); alta("Alphabet", "GOOGL", TipoInstrumento.ACCION, "175"); alta("Amazon", "AMZN", TipoInstrumento.ACCION, "185"); alta("Tesla", "TSLA", TipoInstrumento.ACCION, "250"); alta("Nvidia", "NVDA", TipoInstrumento.ACCION, "498"); alta("Meta", "META", TipoInstrumento.ACCION, "520");
        alta("Bitcoin", "BTC", TipoInstrumento.CRIPTO, "67200"); alta("Ethereum", "ETH", TipoInstrumento.CRIPTO, "3100"); alta("Solana", "SOL", TipoInstrumento.CRIPTO, "145"); alta("BNB", "BNB", TipoInstrumento.CRIPTO, "580"); alta("Cardano", "ADA", TipoInstrumento.CRIPTO, "0.45"); alta("XRP", "XRP", TipoInstrumento.CRIPTO, "0.55"); alta("Dogecoin", "DOGE", TipoInstrumento.CRIPTO, "0.12");
        alta("Bono AL30", "AL30", TipoInstrumento.BONO, "395"); alta("Bono GD30", "GD30", TipoInstrumento.BONO, "65"); alta("Bono AL35", "AL35", TipoInstrumento.BONO, "60"); alta("Bono AE38", "AE38", TipoInstrumento.BONO, "57");
        alta("MercadoLibre", "MELI", TipoInstrumento.CEDEAR, "1800"); alta("Globant", "GLOB", TipoInstrumento.CEDEAR, "210"); alta("Banco BBVA", "BBAR", TipoInstrumento.CEDEAR, "12"); alta("Grupo Galicia", "GGAL", TipoInstrumento.CEDEAR, "25");
        alta("Dólar MEP", "USD", TipoInstrumento.MONEDA, "1"); alta("Euro", "EUR", TipoInstrumento.MONEDA, "1.08"); alta("Real brasileño", "BRL", TipoInstrumento.MONEDA, "0.18");
    }
    private void alta(String nombre, String ticker, TipoInstrumento tipo, String precio) { instrumentos.guardar(new Instrumento(nombre, ticker, tipo, new BigDecimal(precio))); }
}
