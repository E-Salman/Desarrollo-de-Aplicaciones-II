(() => {

    const ctx = window.APP_CONTEXT;
    const $ = (id) => document.getElementById(id);

    const modal = $("modal");

    let instrumentos = [];


    /* =========================
       UTILIDADES
       ========================= */

    const money = (n) => {

        return new Intl.NumberFormat(
            "es-AR",
            {
                style: "currency",
                currency: "USD"
            }
        ).format(n || 0);

    };


    const dec = (n) => Number(n || 0);


    /* =========================
       TOTAL DE COMPRA
       ========================= */

    function actualizarTotal() {

        const cantidad =
            dec($("cantidad").value);

        const precio =
            dec($("precio").value);

        $("total").value =
            money(cantidad * precio);

    }


    /* =========================
       RESUMEN PORTFOLIO
       ========================= */

    function renderResumen(r) {

        $("capital").textContent =
            money(r.capitalInvertido);

        $("patrimonio").textContent =
            money(r.patrimonioTotal);

        $("ganancia").textContent =
            money(r.gananciaTotal);


        $("posiciones").innerHTML =
            r.posiciones.length

                ? r.posiciones.map(p => {

                    const rendimiento =
                        dec(p.rendimiento);

                    const clase =
                        rendimiento >= 0
                            ? "positivo"
                            : "negativo";

                    const signo =
                        rendimiento >= 0
                            ? "+"
                            : "";

                    return `
                        <article class="posicion">

                            <div>

                                <b>
                                    ${p.nombre}
                                </b>

                                <small>
                                    ${p.ticker} · ${p.tipo}
                                </small>

                            </div>

                            <div>

                                <b>
                                    ${money(p.actual)}
                                </b>

                                <small class="${clase}">
                                    ${signo}${money(rendimiento)}
                                </small>

                            </div>

                        </article>
                    `;

                }).join("")

                : "<p>Aún no hay inversiones.</p>";

    }


    /* =========================
       CARGAR RESUMEN
       ========================= */

    async function cargarResumen() {

        const response =
            await fetch(
                ctx + "/api/portfolios/1/resumen"
            );

        const resumen =
            await response.json();

        renderResumen(resumen);

    }


    /* =========================
       CARGAR INSTRUMENTOS
       ========================= */

    async function cargarInstrumentos() {

        const response =
            await fetch(
                ctx + "/api/instrumentos"
            );

        instrumentos =
            await response.json();


        const grupos =
            instrumentos.reduce(
                (acumulador, instrumento) => {

                    if (!acumulador[instrumento.tipo]) {

                        acumulador[instrumento.tipo] = [];

                    }

                    acumulador[instrumento.tipo]
                        .push(instrumento);

                    return acumulador;

                },
                {}
            );


        $("instrumento").innerHTML =
            `
                <option value="">
                    Seleccioná un instrumento…
                </option>
            `
            +
            Object.entries(grupos)
                .map(([tipo, lista]) => {

                    return `
                        <optgroup label="${tipo}">

                            ${lista.map(i => `
                                <option value="${i.ticker}">
                                    ${i.nombre} (${i.ticker})
                                </option>
                            `).join("")}

                        </optgroup>
                    `;

                })
                .join("");

    }


    /* =========================
       SELECCION DE INSTRUMENTO
       ========================= */

    $("instrumento").addEventListener(
        "change",
        (event) => {

            const instrumento =
                instrumentos.find(
                    i =>
                        i.ticker ===
                        event.target.value
                );


            $("ticker").value =
                instrumento?.ticker || "";

            $("precio").value =
                instrumento?.cotizacionActual || "";

            actualizarTotal();

        }
    );


    /* =========================
       CANTIDAD Y PRECIO
       ========================= */

    ["cantidad", "precio"].forEach(
        id => {

            $(id).addEventListener(
                "input",
                actualizarTotal
            );

        }
    );


    /* =========================
       ABRIR MODAL
       ========================= */

    $("abrirModal").addEventListener(
        "click",
        () => {

            $("formCompra").reset();

            $("fecha").value =
                new Date()
                    .toISOString()
                    .slice(0, 10);

            $("error").textContent = "";

            modal.showModal();

        }
    );


    /* =========================
       CERRAR MODAL
       ========================= */

    $("cerrarModal").addEventListener(
        "click",
        () => {

            modal.close();

        }
    );


    /* =========================
       REGISTRAR COMPRA
       ========================= */

    $("formCompra").addEventListener(
        "submit",
        async (event) => {

            event.preventDefault();


            const response =
                await fetch(
                    ctx + "/api/compras",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body: JSON.stringify({

                            ticker:
                                $("ticker").value,

                            cantidad:
                                dec(
                                    $("cantidad").value
                                ),

                            precioUnitario:
                                dec(
                                    $("precio").value
                                ),

                            fecha:
                                $("fecha").value
                        })

                    }
                );


            const body =
                await response.json();


            if (!response.ok) {

                $("error").textContent =
                    body.mensaje ||
                    "No se pudo registrar la compra.";

                return;

            }


            renderResumen(body);

            modal.close();

        }
    );


    /* =========================
       CARGA INICIAL
       ========================= */

    Promise.all([
        cargarInstrumentos(),
        cargarResumen()
    ])
    .catch(() => {

        $("posiciones").innerHTML =
            "<p>No se pudo conectar con el componente.</p>";

    });

})();