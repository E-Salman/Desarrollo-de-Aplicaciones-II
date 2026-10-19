package ar.edu.uade.inversorar.mensajeria;

import ar.edu.uade.inversorar.data.Orden;
import ar.edu.uade.inversorar.data.OrdenDetalle;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Contrato del mensaje de la cola "ordenes" (TextMessage con JSON). El mensaje avisa qué orden ejecutar;
 * la fuente de verdad es la base: el Ejecutor busca la orden por ordenId y usa los datos guardados.
 * El resto de los campos sirve para trazabilidad (log, consola de administración).
 */
public class OrdenMensaje {
    public static final int VERSION_ACTUAL = 1;
    private static final Jsonb JSON = JsonbBuilder.create();

    public int version = VERSION_ACTUAL;
    public Long ordenId;
    public String tipo;
    public Long usuarioId;
    public String ticker;
    public BigDecimal cantidad;
    public BigDecimal precioUnitario;
    public LocalDate fecha;
    public String enviadoEn;

    public OrdenMensaje() { }

    /** Arma el mensaje desde la orden guardada (al crearla o al reintentarla). */
    public static OrdenMensaje desde(Orden orden, OrdenDetalle detalle) {
        var m = new OrdenMensaje();
        m.ordenId = orden.getId(); m.tipo = orden.getTipo().name(); m.usuarioId = orden.getUsuarioId();
        m.ticker = detalle.getInstrumento().getTicker(); m.cantidad = detalle.getCantidad();
        m.precioUnitario = detalle.getPrecioUnitario(); m.fecha = orden.getFechaOperacion();
        m.enviadoEn = Instant.now().toString();
        return m;
    }

    public String toJson() { return JSON.toJson(this); }

    /** Lee y valida el JSON. Un mensaje ilegible o de otra versión lanza IllegalArgumentException. */
    public static OrdenMensaje fromJson(String json) {
        OrdenMensaje m;
        try { m = JSON.fromJson(json, OrdenMensaje.class); }
        catch (JsonbException e) { throw new IllegalArgumentException("Mensaje de orden ilegible: " + e.getMessage(), e); }
        if (m == null || m.ordenId == null) throw new IllegalArgumentException("Mensaje de orden sin ordenId.");
        if (m.version != VERSION_ACTUAL) throw new IllegalArgumentException("Versión de mensaje no soportada: " + m.version);
        return m;
    }

    @Override public String toString() { return "Orden #" + ordenId + " (" + tipo + " " + cantidad + " " + ticker + ")"; }
}
