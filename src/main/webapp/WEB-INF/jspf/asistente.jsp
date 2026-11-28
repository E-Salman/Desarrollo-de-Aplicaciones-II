<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Panel del asistente de IA. Lo incluye navbar.jsp; lo abre el botón "Asistente" del encabezado. --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/asistente.css?v=2">

<dialog id="asistente" class="asistente" aria-labelledby="tituloAsistente">
    <div class="modalTitulo">
        <h2 id="tituloAsistente">Asistente</h2>
        <button type="button" id="cerrarAsistente" class="cerrar" aria-label="Cerrar asistente">×</button>
    </div>

    <div class="asistente-pestanias" role="tablist" aria-label="Tipo de consulta">
        <button type="button" role="tab" id="pestaniaAyuda" aria-selected="true" aria-controls="formAsistente" data-modo="ayuda">Ayuda</button>
        <button type="button" role="tab" id="pestaniaNoticias" aria-selected="false" aria-controls="formAsistente" data-modo="noticias">Noticias</button>
    </div>

    <p id="asistenteModoPrueba" class="asistente-aviso" hidden>
        Modo de prueba: el servidor no tiene configurada la IA, las respuestas son fijas.
    </p>

    <form id="formAsistente" role="tabpanel" aria-labelledby="pestaniaAyuda" novalidate>
        <p id="asistenteDescripcion" class="asistente-descripcion">
            Preguntá cómo usar InversorAR o qué significa un término.
        </p>

        <div id="campoActivo" hidden>
            <label for="asistenteActivo">Activo del catálogo</label>
            <input id="asistenteActivo" maxlength="60" autocomplete="off" placeholder="Escribí y elegí de la lista: BTC/USDT, ETH/USDT…"
                   list="asistenteActivos" aria-describedby="ayudaActivo">
            <datalist id="asistenteActivos"></datalist>
            <p id="ayudaActivo" class="asistente-contador asistente-ayuda-activo">Solo instrumentos del catálogo: escribí parte del símbolo y elegí una opción.</p>
        </div>

        <label for="asistentePregunta" id="etiquetaPregunta">Tu pregunta</label>
        <textarea id="asistentePregunta" maxlength="500" rows="3" aria-describedby="contadorPregunta"
                  placeholder="¿Cómo registro una venta?"></textarea>
        <p id="contadorPregunta" class="asistente-contador">0 de 500 caracteres</p>

        <div class="asistente-sugerencias" role="group" aria-labelledby="tituloSugerencias">
            <span id="tituloSugerencias">Sugerencias:</span>
            <div id="asistenteSugerencias"></div>
        </div>

        <p id="asistenteError" class="error" role="alert"></p>
        <button type="submit" id="enviarAsistente" class="asistente-enviar">Preguntar</button>
    </form>

    <section id="asistenteRespuesta" class="asistente-respuesta" aria-live="polite" aria-label="Respuesta del asistente" hidden></section>

    <p class="asistente-pie">
        Las respuestas las genera una IA y pueden tener errores. No son recomendaciones de inversión.
    </p>
</dialog>

<script src="<%= request.getContextPath() %>/assets/asistente.js?v=3" defer></script>
