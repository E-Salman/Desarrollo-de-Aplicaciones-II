
(() => {

    const ctx = window.APP_CONTEXT;
    const $ = id => document.getElementById(id);

    let instrumentos = [];
    let posiciones = [];
    let favoritos = [];

    // Guardado temporal en el navegador
    const STORAGE_KEY = "inversorar-favoritos-demo";

    const tipos = {
        ACCION: "Acción",
        BONO: "Bono",
        CEDEAR: "CEDEAR",
        CRIPTO: "Cripto",
        MONEDA: "Moneda"
    };

    const numero = n =>
        new Intl.NumberFormat("es-AR", {
            maximumFractionDigits: 2
        }).format(n);

    const money = (n, moneda) =>
        n == null
            ? "Sin cotización"
            : numero(n) + " " + (moneda || "");

    // Proteger los textos que vienen de la API
    const esc = s => String(s ?? "").replace(
        /[&<>"']/g,
        c => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[c]
    );

    // =========================
    // CONSULTAR API
    // =========================

    async function api(path) {

        const response = await fetch(ctx + "/api" + path);

        if (response.status === 401) {
            window.location.assign(ctx + "/login");
            throw new Error("Tu sesión venció.");
        }

        if (!response.ok) {
            throw new Error(
                "No se pudieron cargar los datos."
            );
        }

        return response.json();
    }

    // =========================
    // GUARDAR FAVORITOS
    // =========================

    function cargarFavoritos() {

        try {
            const datos = JSON.parse(
                localStorage.getItem(STORAGE_KEY) || "[]"
            );

            favoritos = Array.isArray(datos)
                ? datos.filter(t => typeof t === "string")
                : [];

        } catch {
            favoritos = [];
        }
    }

    function guardarFavoritos() {

        try {
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify(favoritos)
            );

            return true;

        } catch {
            alert("No se pudieron guardar los favoritos.");
            return false;
        }
    }

    function esFavorito(ticker) {
        return favoritos.includes(ticker);
    }

    function cambiarFavorito(ticker) {

        const anteriores = [...favoritos];

        if (esFavorito(ticker)) {
            favoritos = favoritos.filter(t => t !== ticker);
        } else {
            favoritos.push(ticker);
        }

        if (!guardarFavoritos()) {
            favoritos = anteriores;
            return;
        }

        renderFavoritos();
        renderBusqueda();
    }

    // =========================
    // CREAR FILA
    // =========================

    function crearFila(instrumento) {

        const favorito = esFavorito(instrumento.ticker);

        const posicion = posiciones.find(
            p => p.ticker === instrumento.ticker
        );

        const articulo = document.createElement("article");
        articulo.className = "favorito-item";

        // ESTRELLA

        const boton = document.createElement("button");
        boton.type = "button";

        boton.className = "favorito-estrella" +
            (favorito ? " activo" : "");

        boton.textContent = favorito ? "★" : "☆";

        boton.setAttribute(
            "aria-label",
            favorito
                ? "Quitar de favoritos"
                : "Agregar a favoritos"
        );

        boton.onclick = () =>
            cambiarFavorito(instrumento.ticker);

        articulo.appendChild(boton);

        // INFORMACION

        const info = document.createElement("div");
        info.className = "favorito-info";

        info.innerHTML = `
            <b class="favorito-nombre">
                ${esc(instrumento.nombre)}
            </b>

            <div class="favorito-detalles">

                <span>
                    ${esc(instrumento.ticker)}
                </span>

                <span class="favorito-tipo">
                    ${esc(tipos[instrumento.tipo] || instrumento.tipo)}
                </span>

                ${posicion
                    ? '<span class="favorito-en-cartera">En cartera</span>'
                    : ''}

            </div>
        `;

        articulo.appendChild(info);

        // PRECIO

        const derecha = document.createElement("div");
        derecha.className = "favorito-precio";

        const precio = document.createElement("div");

        precio.textContent = money(
            instrumento.cotizacionActual,
            instrumento.moneda
        );

        derecha.appendChild(precio);

        // RENDIMIENTO DE MI POSICION

        if (posicion) {

            const rendimiento = document.createElement("span");

            rendimiento.className =
                "favorito-posicion " +
                (Number(posicion.rendimiento) >= 0
                    ? "positivo"
                    : "negativo");

            rendimiento.textContent =
                "Mi posición: " +
                numero(posicion.rendimientoPorcentaje) + "%";

            derecha.appendChild(rendimiento);
        }

        articulo.appendChild(derecha);

        return articulo;
    }

    // =========================
    // MOSTRAR MIS FAVORITOS
    // =========================

    function renderFavoritos() {

        const lista = $("listaFavoritos");
        lista.replaceChildren();

        const filtro = $("filtrarFavoritos")
            .value.trim().toLowerCase();

        // Todos los favoritos existentes
        const todos = instrumentos.filter(i =>
            esFavorito(i.ticker)
        );

        // Solo los que coinciden con el filtro
        const guardados = todos.filter(i =>
            String(i.ticker).toLowerCase().includes(filtro) ||
            String(i.nombre).toLowerCase().includes(filtro)
        );

        $("contadorFavoritos").textContent =
            todos.length + " tickers marcados";

        if (!guardados.length) {

            lista.innerHTML = `
                <p class="favoritos-mensaje">
                    ${filtro
                        ? "No hay favoritos que coincidan con tu búsqueda."
                        : "Todavía no agregaste favoritos."}
                </p>
            `;

            return;
        }

        guardados.forEach(i => {
            lista.appendChild(crearFila(i));
        });
    }

    // =========================
    // BUSCAR PARA AGREGAR
    // =========================

    function renderBusqueda() {

        const texto = $("buscarFavorito")
            .value.trim().toLowerCase();

        const resultados = $("resultadosBusqueda");

        resultados.replaceChildren();

        // Si el buscador está vacío, no mostrar lista
        if (!texto) {
            return;
        }

        const encontrados = instrumentos.filter(i =>
            String(i.ticker).toLowerCase().includes(texto) ||
            String(i.nombre).toLowerCase().includes(texto)
        );

        // Mostrar máximo 10 instrumentos
        const visibles = encontrados.slice(0, 10);

        if (!visibles.length) {

            resultados.innerHTML = `
                <p class="favoritos-mensaje">
                    No se encontraron instrumentos.
                </p>
            `;

            return;
        }

        visibles.forEach(i => {
            resultados.appendChild(crearFila(i));
        });

        if (encontrados.length > 10) {

            const aviso = document.createElement("p");
            aviso.className = "favoritos-mensaje";

            aviso.textContent =
                "Hay más resultados. Escribí una búsqueda más específica.";

            resultados.appendChild(aviso);
        }
    }

    // =========================
    // LUPA DE MIS FAVORITOS
    // =========================

    function configurarFiltro() {

        const boton = $("botonFiltrarFavoritos");
        const input = $("filtrarFavoritos");

        boton.onclick = () => {

            const abrir = input.hidden;

            input.hidden = !abrir;

            boton.classList.toggle("activo", abrir);

            boton.setAttribute(
                "aria-expanded",
                String(abrir)
            );

            if (abrir) {
                input.focus();
            } else {
                input.value = "";
                renderFavoritos();
            }
        };

        input.addEventListener(
            "input",
            renderFavoritos
        );
    }

    // =========================
    // INICIAR PAGINA
    // =========================

    async function iniciar() {

        cargarFavoritos();
        configurarFiltro();

        $("buscarFavorito").addEventListener(
            "input",
            renderBusqueda
        );

        const [datosInstrumentos, resumen] = await Promise.all([
            api("/instrumentos"),
            api("/portfolio/resumen")
        ]);

        instrumentos = datosInstrumentos;
        posiciones = resumen.posiciones || [];

        renderFavoritos();
    }

    iniciar().catch(error => {
        $("listaFavoritos").textContent = error.message;
    });

})();
