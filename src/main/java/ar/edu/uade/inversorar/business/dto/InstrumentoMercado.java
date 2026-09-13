package ar.edu.uade.inversorar.business.dto;
import java.math.BigDecimal;
public class InstrumentoMercado {
        public long id; public String simbolo,nombre,tipo,mercado,estado,moneda,fecha;
        public BigDecimal cierre;
        public InstrumentoMercado(Object[] r) {
            id=((Number)r[0]).longValue(); simbolo=(String)r[1]; nombre=(String)r[2]; tipo=(String)r[3];
            mercado=(String)r[4]; estado=(String)r[5]; moneda=(String)r[6]; cierre=(BigDecimal)r[7];
            fecha=r[8]==null?null:r[8].toString();
        }
    }
