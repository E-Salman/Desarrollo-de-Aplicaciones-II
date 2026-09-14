<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <meta
        name="viewport"
        content="width=device-width, initial-scale=1.0"
    >

    <title>InversorAR</title>

    <link
        rel="stylesheet"
        href="<%= request.getContextPath() %>/assets/dashboard.css"
    >
</head>

<body>

<header>

    <div class="brand">
        <span>✓</span>
        InversorAR
    </div>

    <nav>
        Dashboard　 Portfolio　 Rendimiento　 Historial　 Simulador
    </nav>

    <div class="header-derecha">

        <button
            type="button"
            id="abrirModal"
            disabled
        >
            ＋ Comprar
        </button>

        <button type="button" id="abrirModalVenta" disabled>
            − Vender
        </button>

        <details class="usuario-menu">

            <summary
                class="usuario-boton"
                aria-label="Abrir menú de usuario"
            >

                <svg
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                >
                    <circle cx="12" cy="8" r="4"></circle>
                    <path d="M4 21a8 8 0 0 1 16 0"></path>
                </svg>

                <svg
                    class="usuario-flecha"
                    width="11"
                    height="11"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                >
                    <path d="m6 9 6 6 6-6"></path>
                </svg>

            </summary>

            <div class="usuario-dropdown">

                <a
                    href="#"
                    class="menu-opcion"
                >
                    Configuración
                </a>

                <div class="menu-separador"></div>

                <form method="post" action="<%= request.getContextPath() %>/logout">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <button type="submit" class="menu-opcion cerrar-sesion">Cerrar sesión</button>
                </form>

            </div>

        </details>

    </div>

</header>


<main>

    <h1>Mi Portfolio</h1>

    <p
        class="sub"
        id="origen"
    >
        Cargando…
    </p>


    <section class="metricas">

        <article>
            <small>CAPITAL INVERTIDO</small>
            <strong id="capital">—</strong>
        </article>

        <article>
            <small>PATRIMONIO TOTAL</small>
            <strong id="patrimonio">—</strong>
        </article>

        <article>
            <small>GANANCIA TOTAL</small>
            <strong id="ganancia">—</strong>
        </article>

    </section>


    <div id="totalesMoneda"></div>


    <section class="panel">

        <h2>Mis inversiones</h2>

        <div
            id="posiciones"
            class="posiciones"
        >
            <p>Cargando inversiones…</p>
        </div>

    </section>

</main>


<!-- =========================
     CATALOGO DE INSTRUMENTOS
     ========================= -->

<dialog
    id="catalogoPanel"
    class="panel catalogo"
    aria-labelledby="tituloCatalogo"
>

    <div class="modalTitulo">

        <h2 id="tituloCatalogo">
            Elegí un instrumento
        </h2>

        <button
            id="cerrarCatalogo"
            class="cerrar"
            aria-label="Cerrar catálogo"
        >
            ×
        </button>

    </div>


    <p class="aviso">
        Precios históricos de la base compartida.
        Los instrumentos sin precio o sin moneda identificada
        se pueden consultar, pero no comprar.
    </p>


    <form
        id="busquedaCatalogo"
        class="buscador"
    >

        <label>
            Nombre o símbolo

            <input
                id="buscar"
                maxlength="80"
                placeholder="BTCUSDT, Ethereum…"
            >
        </label>

        <button class="secundario">
            Buscar
        </button>

    </form>


    <p
        id="catalogoError"
        class="error"
        role="alert"
    ></p>


    <div class="tabla">

        <table>

            <thead>
                <tr>
                    <th>Instrumento</th>
                    <th>Último cierre</th>
                    <th>Acciones</th>
                </tr>
            </thead>

            <tbody id="catalogoFilas"></tbody>

        </table>

    </div>


    <div class="paginacion">

        <button
            id="anterior"
            class="secundario"
        >
            Anterior
        </button>

        <span id="pagina"></span>

        <button
            id="siguiente"
            class="secundario"
        >
            Siguiente
        </button>

    </div>

</dialog>


<!-- =========================
     HISTORIAL
     ========================= -->

<dialog id="historial">

    <div class="modalTitulo">

        <h2 id="tituloHistorial">
            Historial
        </h2>

        <button
            id="cerrarHistorial"
            class="cerrar"
            aria-label="Cerrar historial"
        >
            ×
        </button>

    </div>


    <p class="aviso">
        Hasta 90 registros, del más reciente al más antiguo.
    </p>


    <div class="tabla">

        <table>

            <thead>
                <tr>
                    <th>Fecha</th>
                    <th>Cierre</th>
                    <th>Volumen</th>
                </tr>
            </thead>

            <tbody id="historialFilas"></tbody>

        </table>

    </div>

</dialog>


<!-- =========================
     MODAL COMPRA
     ========================= -->

<dialog id="modal">

    <form id="formCompra">

        <div class="modalTitulo">

            <h2>
                Comprar instrumento
            </h2>

            <button
                type="button"
                id="cerrarModal"
                class="cerrar"
            >
                ×
            </button>

        </div>


        <label>
            Tipo de instrumento

            <select id="tipoInstrumento">

                <option value="">
                    Todos los tipos
                </option>

                <option value="ACCION">
                    Acciones
                </option>

                <option value="BONO">
                    Bonos
                </option>

                <option value="CEDEAR">
                    CEDEARs
                </option>

                <option value="CRIPTO">
                    Criptomonedas
                </option>

                <option value="MONEDA">
                    Monedas
                </option>

            </select>

        </label>


        <label>
            Instrumento

            <select
                id="instrumento"
                required
            >
                <option value="">
                    Seleccioná un instrumento…
                </option>
            </select>

        </label>


        <p
            id="disponibilidadTipo"
            class="sub"
            role="status"
        ></p>


        <button
            type="button"
            id="abrirCatalogo"
            class="enlace"
            hidden
        >
            Consultar precios e historial
        </button>


        <div class="dos">

            <label>
                Ticker

                <input
                    id="ticker"
                    readonly
                >
            </label>


            <label>
                Cantidad

                <input
                    id="cantidad"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>

        </div>


        <div class="dos">

            <label>
                Precio por unidad
                <span id="monedaCompra"></span>

                <input
                    id="precio"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>


            <label>
                Total invertido

                <input
                    id="total"
                    readonly
                >
            </label>

        </div>


        <p
            id="referenciaPrecio"
            class="aviso"
        ></p>


        <label>
            Fecha de compra

            <input
                id="fecha"
                type="date"
                required
            >
        </label>


        <p
            id="error"
            class="error"
            role="alert"
        ></p>


        <button
            id="guardarCompra"
            class="comprar"
            type="submit"
        >
            Agregar al portfolio
        </button>

    </form>

</dialog>


<dialog id="modalVenta" aria-labelledby="tituloVenta">
    <form id="formVenta">
        <div class="modalTitulo">
            <h2 id="tituloVenta">Vender instrumento</h2>
            <button type="button" id="cerrarModalVenta" class="cerrar" aria-label="Cerrar venta">×</button>
        </div>
        <p id="disponibilidadVenta" class="sub" role="status"></p>
        <label>Instrumento de tu portfolio
            <select id="instrumentoVenta" required>
                <option value="">Seleccioná un instrumento…</option>
            </select>
        </label>
        <div class="dos">
            <label>Ticker <input id="tickerVenta" readonly></label>
            <label>Cantidad <button type="button" id="venderTodo" class="enlace">Vender todo</button>
                <input id="cantidadVenta" type="number" min="0" step="any" required>
            </label>
        </div>
        <div class="dos">
            <label>Precio por unidad <span id="monedaVenta"></span>
                <input id="precioVenta" type="number" min="0" step="any" required>
            </label>
            <label>Total de la venta <input id="totalVenta" readonly></label>
        </div>
        <label>Fecha de venta <input id="fechaVenta" type="date" required></label>
        <p id="errorVenta" class="error" role="alert"></p>
        <button id="guardarVenta" class="comprar" type="submit" disabled>Registrar venta</button>
    </form>
</dialog>

<script>
    window.APP_CONTEXT = '<%= request.getContextPath() %>';
</script>

<script src="<%= request.getContextPath() %>/assets/dashboard.js?v=20260914-ventas"></script>

</body>

</html>
