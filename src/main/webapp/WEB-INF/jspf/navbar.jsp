
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<header>
    <div class="brand">
        <span>✓</span>
        InversorAR
    </div>

    <%
        String ctx = request.getContextPath();

        String activePage =
            (String) request.getAttribute("activePage");

        if (activePage == null) {
            activePage = "";
        }

        String[][] items = {
            {"dashboard", "/dashboard", "Dashboard"},
            {"portfolio", "/portfolio", "Portfolio"},
            {"favoritos", "/favoritos", "Favoritos"},
            {"history", "/historial", "Historial"},
            {"simulator", "/simulador", "Simulador"}
        };
    %>

    <nav class="app-nav">

        <% for (String[] item : items) {

            boolean activo = item[0].equals(activePage);

            String clase = activo
                ? "nav-button active"
                : "nav-button";
        %>

            <a class="<%= clase %>"
               href="<%= ctx + item[1] %>"
               <%= activo ? "aria-current=\"page\"" : "" %>>
                <%= item[2] %>
            </a>

        <% } %>

    </nav>

    <div class="header-derecha">

        <button id="abrirModal" type="button">
            + Comprar
        </button>

        <button id="abrirModalVenta" type="button">
            - Vender
        </button>

        <details class="usuario-menu">

            <summary class="usuario-boton">
                <span>#</span>
                <span class="usuario-flecha">⌄</span>
            </summary>

            <div class="usuario-dropdown">

                <a class="menu-opcion"
                   href="<%= ctx %>/perfil.jsp">
                    Mi perfil
                </a>

                <%-- Lo muestra dashboard.js cuando las órdenes van por mensajería (perfil MySQL). --%>
                <button type="button" id="abrirTransacciones" class="menu-opcion" hidden>
                    Transacciones <span id="cantidadPendientes" class="contador contador-pendientes" hidden></span>
                </button>

                <div class="menu-separador"></div>

                <%-- POST con el token CSRF de la sesión: LogoutServlet rechaza un GET o un pedido sin token. --%>
                <form method="post" action="<%= ctx %>/logout">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <button type="submit" class="menu-opcion cerrar-sesion">Cerrar sesión</button>
                </form>

            </div>
        </details>

    </div>
</header>

<% if (!"dashboard".equals(activePage)) { %>

    <jsp:include page="/WEB-INF/jspf/modalesInversion.jsp" />

<% } %>
