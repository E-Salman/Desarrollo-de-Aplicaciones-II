<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ar.edu.uade.inversorar.data.TipoInstrumento" %>
<%!
    private String nombre(TipoInstrumento tipo) {
        switch (tipo) {
            case ACCION: return "Acciones";
            case BONO: return "Bonos";
            case CEDEAR: return "CEDEARs";
            case CRIPTO: return "Criptomonedas";
            case MONEDA: return "Monedas";
            default: return tipo.name();
        }
    }
    private String descripcion(TipoInstrumento tipo) {
        switch (tipo) {
            case ACCION: return "Participaciones en empresas.";
            case BONO: return "Instrumentos de deuda.";
            case CEDEAR: return "Certificados que representan activos del exterior.";
            case CRIPTO: return "Activos digitales negociados en pares.";
            case MONEDA: return "Posiciones en monedas.";
            default: return tipo.name();
        }
    }
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Simulador - InversorAR</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/dashboard.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/simulador.css">
    <script defer src="<%= request.getContextPath() %>/assets/simulador.js"></script>
</head>
<body>
<jsp:include page="/WEB-INF/jspf/navbar.jsp" />
<main class="sim-main" id="simulador" data-context="<%= request.getContextPath() %>">
    <div class="sim-heading"><div><p class="sim-eyebrow">PLANIFICÁ TU DISTRIBUCIÓN</p><h1>Simulador</h1>
        <p class="sim-intro">Explorá cómo repartir tu capital entre los tipos de inversión de InversorAR.</p></div>
        <span class="sim-session">Plan de esta sesión</span>
    </div>
    <p class="sim-notice">La simulación no registra compras ni modifica tu cartera. Calcula distribución de capital, sin proyectar ganancias.</p>
    <p id="sim-status" role="status" aria-live="polite">Cargando tu plan…</p>
    <div class="sim-layout">
        <form id="sim-form" class="sim-editor">
            <fieldset id="sim-fields" disabled>
                <div class="sim-capital">
                    <label for="sim-capital">Capital a distribuir</label>
                    <input id="sim-capital" name="capital" type="number" min="0" max="999999999999.99" step="0.01" required value="0" aria-describedby="sim-moneda">
                    <p id="sim-moneda">Usá una única moneda de referencia para todo el plan. Los importes se expresan en esa misma moneda; no se realizan conversiones.</p>
                </div>
                <div class="sim-table-heading"><h2>Distribución por tipo</h2><span>Porcentaje del capital</span></div>
                <div class="sim-types">
                <% for (TipoInstrumento tipo : (TipoInstrumento[]) request.getAttribute("tiposSimulacion")) { %>
                    <div class="sim-type" data-tipo="<%= tipo.name() %>" data-nombre="<%= nombre(tipo) %>">
                        <div class="sim-type-info"><label for="sim-<%= tipo.name() %>"><%= nombre(tipo) %></label>
                            <p><%= descripcion(tipo) %></p><small class="sim-catalogo">Consultando catálogo…</small></div>
                        <div class="sim-percentage"><input id="sim-<%= tipo.name() %>" type="number" min="0" max="100" step="0.0001" required value="0" aria-label="Porcentaje de <%= nombre(tipo) %>"><span aria-hidden="true">%</span></div>
                    </div>
                <% } %>
                </div>
                <div class="sim-actions"><button type="submit" id="sim-save">Aplicar simulación</button><button type="button" id="sim-reset">Reiniciar</button></div>
            </fieldset>
        </form>
        <aside class="sim-result" id="sim-result" aria-label="Resultado de la simulación">
            <h2>Así se distribuye tu capital</h2>
            <p id="sim-result-state">Sin cambios pendientes</p>
            <div class="sim-total"><span>Capital total</span><strong id="sim-total">0</strong></div>
            <div class="sim-bar" id="sim-bar" aria-hidden="true"></div>
            <dl id="sim-breakdown"></dl>
            <div class="sim-remaining"><span>Sin asignar</span><strong id="sim-remaining">0</strong><small id="sim-percent">100 % disponible</small></div>
            <p class="sim-footnote">Podés dejar parte del capital sin asignar. El total de porcentajes no puede superar el 100 %.</p>
            <p class="sim-footnote">El plan se conserva al navegar o recargar durante la sesión. Se reinicia al cerrar sesión.</p>
        </aside>
    </div>
    <noscript>Activá JavaScript para usar el simulador.</noscript>
</main>
</body>
</html>
