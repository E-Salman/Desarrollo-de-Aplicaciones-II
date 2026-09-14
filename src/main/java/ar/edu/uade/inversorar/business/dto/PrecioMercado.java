package ar.edu.uade.inversorar.business.dto;
import java.math.BigDecimal;
public class PrecioMercado {
        public String fecha; public BigDecimal apertura,maximo,minimo,cierre,volumen;
        public PrecioMercado(Object[] r) {
            fecha=r[0].toString(); apertura=(BigDecimal)r[1]; maximo=(BigDecimal)r[2];
            minimo=(BigDecimal)r[3]; cierre=(BigDecimal)r[4]; volumen=(BigDecimal)r[5];
        }
    }
