
(() => {

    // =========================
    // CONFIGURACION
    // =========================

    const ctx = window.APP_CONTEXT ||
        document.querySelector("#abrirModal")
            ?.closest("header")
            ?.querySelector("a[href*='/dashboard']")
            ?.getAttribute("href")
            ?.replace(/\/dashboard$/, "") || "";

    const $ = id => document.getElementById(id);

    const modalCompra = $("modalCompraCompartido");
    const modalVenta = $("modalVentaCompartido");

    if (!modalCompra || !modalVenta) return;

    let instrumentos = [];
    let posiciones = [];

    let monedaCompra = "";
    let monedaVenta = "";

    const tipos = {
        ACCION: "Acciones",
        BONO: "Bonos",
        CEDEAR: "CEDEARs",
        CRIPTO: "Criptomonedas",
        MONEDA: "Monedas"
    };

    const numero = n =>
        new Intl.NumberFormat("es-AR", {
            maximumFractionDigits: 10
        }).format(n);

    const dec = n => Number(n || 0);

    const money = (n, moneda) =>
        n == null
            ? "Sin dato"
            : numero(n) + " " +
              (moneda || "(moneda sin identificar)");

    // =========================
    // API
    // =========================

    async function api(path, options) {

        const response = await fetch(
            ctx + "/api" + path,
            options
        );

        if (response.status === 401) {
            window.location.assign(ctx + "/login");
            throw new Error("Tu sesión venció.");
        }

        if (!response.ok) {

            const error = await response.json()
                .catch(() => ({}));

            throw new Error(
                error.mensaje ||
                "No se pudo completar la operación (" +
                response.status + ")."
            );
        }

        return response.json();
    }

    // =========================
    // FECHA ACTUAL
    // =========================

    function fechaHoy() {

        const hoy = new Date();

        return new Date(
            hoy.getTime() -
            hoy.getTimezoneOffset() * 60000
        ).toISOString().slice(0, 10);
    }

    // =========================
    // CARGAR DATOS
    // =========================

    async function cargarInstrumentos() {

        instrumentos = await api("/instrumentos");

        renderInstrumentos();
    }

    async function cargarPosiciones() {

        const resumen = await api("/portfolio/resumen");

        posiciones = resumen.posiciones || [];

        renderPosiciones();
    }

    // =========================
    // MODAL COMPRA
    // =========================

    function renderInstrumentos() {

        const tipo = $("tipoCompraCompartido").value;

        const disponibles = instrumentos.filter(i =>
            !tipo || i.tipo === tipo
        );

        const select = $("instrumentoCompraCompartido");

        select.replaceChildren(
            new Option("Seleccioná un instrumento...", "")
        );

        const grupos = new Map();

        for (const i of disponibles) {

            if (!grupos.has(i.tipo)) {

                const grupo = document.createElement("optgroup");

                grupo.label = tipos[i.tipo] || i.tipo;

                grupos.set(i.tipo, grupo);
                select.appendChild(grupo);
            }

            const disponible =
                i.cotizacionActual != null &&
                i.moneda != null;

            const opcion = new Option(
                i.nombre + " (" + i.ticker + ")" +
                (disponible ? "" : " - Datos incompletos"),
                i.ticker
            );

            opcion.disabled = !disponible;

            grupos.get(i.tipo).appendChild(opcion);
        }

        $("disponibilidadCompraCompartida").textContent =
            disponibles.length
                ? ""
                : "No hay instrumentos de este tipo.";

        seleccionarCompra();
    }

    function seleccionarCompra() {

        const ticker =
            $("instrumentoCompraCompartido").value;

        const instrumento = instrumentos.find(i =>
            i.ticker === ticker
        );

        $("tickerCompraCompartido").value =
            instrumento?.ticker || "";

        $("precioCompraCompartido").value =
            instrumento?.cotizacionActual ?? "";

        monedaCompra = instrumento?.moneda || "";

        $("monedaCompraCompartida").textContent =
            monedaCompra;

        $("referenciaCompraCompartida").textContent =
            instrumento?.fechaCotizacion
                ? "Último cierre disponible: " +
                  instrumento.fechaCotizacion +
                  ". Podés ingresar el precio de compra."
                : "Ingresá el precio de tu compra.";

        $("guardarCompraCompartida").disabled =
            !instrumento ||
            instrumento.cotizacionActual == null ||
            instrumento.moneda == null;

        actualizarTotalCompra();
    }

    function actualizarTotalCompra() {

        const cantidad =
            dec($("cantidadCompraCompartida").value);

        const precio =
            dec($("precioCompraCompartido").value);

        $("totalCompraCompartido").value =
            money(cantidad * precio, monedaCompra);
    }

    async function abrirCompra() {

        $("formCompraCompartido").reset();

        $("errorCompraCompartida").textContent = "";

        $("fechaCompraCompartida").value = fechaHoy();
        $("fechaCompraCompartida").max = fechaHoy();

        $("tipoCompraCompartido").value = "";

        $("instrumentoCompraCompartido").replaceChildren(
            new Option("Cargando instrumentos...", "")
        );

        $("guardarCompraCompartida").disabled = true;

        modalCompra.showModal();

        try {
            await cargarInstrumentos();
        } catch (e) {
            $("errorCompraCompartida").textContent = e.message;
        }
    }

    // =========================
    // GUARDAR COMPRA
    // =========================

    async function guardarCompra(e) {

        e.preventDefault();

        const boton = $("guardarCompraCompartida");

        boton.disabled = true;

        $("errorCompraCompartida").textContent = "";

        try {

            const cantidad =
                Number($("cantidadCompraCompartida").value);

            const precio =
                Number($("precioCompraCompartido").value);

            if (!(cantidad > 0) || !(precio > 0)) {
                throw new Error(
                    "La cantidad y el precio deben ser mayores a cero."
                );
            }

            await api("/compras", {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({

                    ticker: $("tickerCompraCompartido").value,

                    cantidad: $("cantidadCompraCompartida").value,

                    precioUnitario: $("precioCompraCompartido").value,

                    fecha: $("fechaCompraCompartida").value
                })
            });

            modalCompra.close();

            // Refrescar los datos de la página actual
            window.location.reload();

        } catch (e) {

            $("errorCompraCompartida").textContent = e.message;

            boton.disabled = false;
        }
    }

    // =========================
    // MODAL VENTA
    // =========================

    function renderPosiciones() {

        const select = $("instrumentoVentaCompartido");

        select.replaceChildren(
            new Option("Seleccioná un instrumento...", "")
        );

        posiciones.forEach(p => {

            select.appendChild(
                new Option(
                    p.nombre + " (" + p.ticker + ")" +
                    " - Tenés " + numero(p.cantidad),
                    p.ticker
                )
            );
        });

        $("disponibilidadVentaCompartida").textContent =
            posiciones.length
                ? ""
                : "Todavía no tenés instrumentos para vender.";

        seleccionarVenta();
    }

    function seleccionarVenta() {

        const ticker =
            $("instrumentoVentaCompartido").value;

        const posicion = posiciones.find(p =>
            p.ticker === ticker
        );

        $("tickerVentaCompartido").value =
            posicion?.ticker || "";

        $("cantidadVentaCompartida").value = "";

        $("cantidadVentaCompartida").max =
            posicion?.cantidad ?? "";

        monedaVenta = posicion?.moneda || "";

        $("monedaVentaCompartida").textContent =
            monedaVenta;

        $("precioVentaCompartido").value =
            posicion && dec(posicion.cantidad) > 0
                ? (
                    dec(posicion.actual) /
                    dec(posicion.cantidad)
                ).toFixed(10)
                : "";

        $("guardarVentaCompartida").disabled = !posicion;

        actualizarTotalVenta();
    }

    function actualizarTotalVenta() {

        const cantidad =
            dec($("cantidadVentaCompartida").value);

        const precio =
            dec($("precioVentaCompartido").value);

        $("totalVentaCompartido").value =
            money(cantidad * precio, monedaVenta);
    }

    async function abrirVenta() {

        $("formVentaCompartido").reset();

        $("errorVentaCompartida").textContent = "";

        $("fechaVentaCompartida").value = fechaHoy();
        $("fechaVentaCompartida").max = fechaHoy();

        $("instrumentoVentaCompartido").replaceChildren(
            new Option("Cargando posiciones...", "")
        );

        $("guardarVentaCompartida").disabled = true;

        modalVenta.showModal();

        try {
            await cargarPosiciones();
        } catch (e) {
            $("errorVentaCompartida").textContent = e.message;
        }
    }

    // =========================
    // VENDER TODO
    // =========================

    function venderTodo() {

        const ticker =
            $("instrumentoVentaCompartido").value;

        const posicion = posiciones.find(p =>
            p.ticker === ticker
        );

        if (!posicion) return;

        $("cantidadVentaCompartida").value =
            posicion.cantidad;

        actualizarTotalVenta();
    }

    // =========================
    // GUARDAR VENTA
    // =========================

    async function guardarVenta(e) {

        e.preventDefault();

        const boton = $("guardarVentaCompartida");

        boton.disabled = true;

        $("errorVentaCompartida").textContent = "";

        try {

            const ticker =
                $("tickerVentaCompartido").value;

            const posicion = posiciones.find(p =>
                p.ticker === ticker
            );

            const cantidad =
                Number($("cantidadVentaCompartida").value);

            const precio =
                Number($("precioVentaCompartido").value);

            if (!posicion ||
                !(cantidad > 0) ||
                !(precio > 0) ||
                cantidad > Number(posicion.cantidad)) {

                throw new Error(
                    "Verificá la cantidad y el precio de venta."
                );
            }

            await api("/ventas", {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({

                    ticker,

                    cantidad: $("cantidadVentaCompartida").value,

                    precioUnitario: $("precioVentaCompartido").value,

                    fecha: $("fechaVentaCompartida").value
                })
            });

            modalVenta.close();

            // Refrescar la página actual
            window.location.reload();

        } catch (e) {

            $("errorVentaCompartida").textContent = e.message;

            boton.disabled = false;
        }
    }

    // =========================
    // EVENTOS
    // =========================

    $("abrirModal").addEventListener("click", abrirCompra);

    $("abrirModalVenta").addEventListener("click", abrirVenta);

    $("cerrarCompraCompartido").onclick = () =>
        modalCompra.close();

    $("cerrarVentaCompartida").onclick = () =>
        modalVenta.close();

    $("tipoCompraCompartido").onchange =
        renderInstrumentos;

    $("instrumentoCompraCompartido").onchange =
        seleccionarCompra;

    $("instrumentoVentaCompartido").onchange =
        seleccionarVenta;

    $("venderTodoCompartido").onclick =
        venderTodo;

    ["cantidadCompraCompartida", "precioCompraCompartido"]
        .forEach(id => {
            $(id).addEventListener(
                "input",
                actualizarTotalCompra
            );
        });

    ["cantidadVentaCompartida", "precioVentaCompartido"]
        .forEach(id => {
            $(id).addEventListener(
                "input",
                actualizarTotalVenta
            );
        });

    $("formCompraCompartido").addEventListener(
        "submit",
        guardarCompra
    );

    $("formVentaCompartido").addEventListener(
        "submit",
        guardarVenta
    );

})();
