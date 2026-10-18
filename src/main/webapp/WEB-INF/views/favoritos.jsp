
<%@ page contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<% request.setAttribute("activePage", "favoritos"); %>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Favoritos - InversorAR</title>

    <link rel="stylesheet"
          href="<%= request.getContextPath() %>/assets/dashboard.css">

    <link rel="stylesheet"
          href="<%= request.getContextPath() %>/assets/favoritos.css">
</head>

<body>

    <jsp:include page="/WEB-INF/jspf/navbar.jsp" />

    <main class="favoritos-main">

        <div class="favoritos-header">
            <h1>Favoritos</h1>

            <p class="sub" id="contadorFavoritos">
                Cargando...
            </p>
        </div>

        <!-- BUSCAR INSTRUMENTOS PARA AGREGAR -->

        <div class="buscador-agregar">

            <span class="buscador-icono">⌕</span>

            <input
                type="search"
                id="buscarFavorito"
                placeholder="Buscar instrumentos para agregar..."
                autocomplete="off"
            >

        </div>

        <div id="resultadosBusqueda"></div>

        <!-- TUS FAVORITOS -->

        <section class="favoritos-seccion">

            <div class="favoritos-titulo-fila">

                <h2 class="seccion-titulo">
                    TUS FAVORITOS
                </h2>

                <div class="filtro-favoritos">

                    <input
                        type="search"
                        id="filtrarFavoritos"
                        placeholder="Filtrar favoritos..."
                        autocomplete="off"
                        hidden
                    >

                    <button
                        type="button"
                        id="botonFiltrarFavoritos"
                        class="boton-lupa"
                        aria-label="Buscar entre mis favoritos"
                        aria-expanded="false"
                    >
                        <svg width="17" height="17"
                             viewBox="0 0 24 24"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             stroke-linecap="round">

                            <circle cx="11" cy="11" r="8"></circle>
                            <path d="m21 21-4.35-4.35"></path>
                        </svg>
                    </button>

                </div>
            </div>

            <div class="panel" id="listaFavoritos">
                <p class="favoritos-mensaje">
                    Cargando favoritos...
                </p>
            </div>

        </section>

    </main>

    <script>
        window.APP_CONTEXT =
            '<%= request.getContextPath() %>';
    </script>

    <script src="<%= request.getContextPath() %>/assets/favoritos.js?v=1"></script>

</body>
</html>
