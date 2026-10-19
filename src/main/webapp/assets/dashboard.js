(() => {
  const ctx = window.APP_CONTEXT, $ = id => document.getElementById(id), modal = $("modal"), modalVenta = $("modalVenta");
  let instrumentos = [], pagina = 0, busqueda = "", filas = [], moneda = "USD", monedaVenta = "", mysql = false, ultimoResumen = { posiciones: [] };
  const dec = n => Number(n || 0);
  const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  const numero = n => new Intl.NumberFormat("es-AR", { maximumFractionDigits: 10 }).format(n);
  const money = (n, m) => n == null ? "Sin dato" : numero(n) + " " + (m || "(moneda sin identificar)");
  async function pedir(path, options) {
    const response = await fetch(ctx + "/api" + path, options);
    if (!response.ok) {
      if (response.status === 401) {
        window.location.assign(ctx + "/login");
        throw new Error("Tu sesión venció. Iniciá sesión nuevamente.");
      }
      const error = await response.json().catch(() => ({}));
      throw new Error(error.mensaje || "No se pudo completar la operación (" + response.status + ").");
    }
    return { status: response.status, body: await response.json() };
  }
  const api = async (path, options) => (await pedir(path, options)).body;
  function actualizarTotal() { $("total").value = money(dec($("cantidad").value) * dec($("precio").value), moneda); }
  function actualizarTotalVenta() { $("totalVenta").value = money(dec($("cantidadVenta").value) * dec($("precioVenta").value), monedaVenta); }
  function renderResumen(r) {
    ultimoResumen = r;
    for (const [id, campo] of [["capital", "capitalInvertido"], ["patrimonio", "patrimonioTotal"], ["ganancia", "gananciaTotal"]])
      $(id).textContent = money(r[campo], r.moneda);
    $("posiciones").innerHTML = r.posiciones.length ? r.posiciones.map(p =>
      '<article class="posicion"><div><b>' + esc(p.nombre) + '</b><small>' + esc(p.ticker) + ' · ' + esc(p.tipo) +
      '</small><small>Cantidad: ' + numero(p.cantidad) + ' · Promedio: ' + esc(money(p.precioPromedio,p.moneda)) +
      '</small><small>' + (p.fechaCotizacion ? 'Cierre histórico: ' + esc(p.fechaCotizacion) : "Cotización de demostración") +
      '</small></div><div><b>' + esc(money(p.actual,p.moneda)) + '</b><small class="' + (dec(p.rendimiento) >= 0 ? "positivo" : "negativo") +
      '">' + esc(money(p.rendimiento,p.moneda)) + ' (' + numero(p.rendimientoPorcentaje) + '%)</small></div></article>').join("") : "<p>Aún no hay inversiones.</p>";
  }
  const tipos = { ACCION: "Acciones", BONO: "Bonos", CEDEAR: "CEDEARs", CRIPTO: "Criptomonedas", MONEDA: "Monedas" };
  async function cargarInstrumentos() {
    instrumentos = await api("/instrumentos");
    renderInstrumentos();
  }
  function renderInstrumentos() {
    const tipo = $("tipoInstrumento").value;
    const disponibles = instrumentos.filter(i => !tipo || i.tipo === tipo);
    const select = $("instrumento"); select.replaceChildren(new Option("Seleccioná un instrumento…", ""));
    const grupos = new Map();
    for (const i of disponibles) {
      if (!grupos.has(i.tipo)) {
        const grupo = document.createElement("optgroup"); grupo.label = tipos[i.tipo] || i.tipo;
        grupos.set(i.tipo, grupo); select.appendChild(grupo);
      }
      const disponible = i.cotizacionActual != null && i.moneda != null;
      const option = new Option(i.nombre + " (" + i.ticker + ")" + (disponible ? "" : " — datos incompletos"), i.ticker);
      option.disabled = !disponible; grupos.get(i.tipo).appendChild(option);
    }
    $("disponibilidadTipo").textContent = disponibles.length ? "" : "Todavía no hay instrumentos de este tipo cargados.";
    select.disabled = !disponibles.length;
    seleccionar();
  }
  function seleccionar() {
    const i = instrumentos.find(x => x.ticker === $("instrumento").value);
    $("ticker").value = i?.ticker || ""; $("precio").value = i?.cotizacionActual ?? "";
    moneda = i?.moneda || ""; $("monedaCompra").textContent = moneda;
    $("referenciaPrecio").textContent = i?.fechaCotizacion ? "Referencia: último cierre disponible (" + i.fechaCotizacion + "). Podés ingresar el precio de tu compra en " + moneda + "." : "Ingresá el precio de tu compra.";
    $("guardarCompra").disabled = !i || i.cotizacionActual == null || i.moneda == null;
    actualizarTotal();
  }
  function abrirCompra(ticker = "") {
    $("formCompra").reset();
    const hoy = new Date(); $("fecha").value = new Date(hoy.getTime() - hoy.getTimezoneOffset()*60000).toISOString().slice(0,10);
    $("fecha").max = $("fecha").value; $("error").textContent = "";
    $("tipoInstrumento").value = instrumentos.find(i => i.ticker === ticker)?.tipo || "";
    renderInstrumentos(); $("instrumento").value = ticker; seleccionar(); modal.showModal();
  }
  async function cargarCatalogo() {
    $("catalogoError").textContent = "";
    $("anterior").disabled = $("siguiente").disabled = true;
    try {
      filas = await api("/catalogo?q=" + encodeURIComponent(busqueda) + "&pagina=" + pagina);
      $("catalogoFilas").innerHTML = filas.length ? filas.map((i,index) => '<tr><td><b>' + esc(i.simbolo) +
        '</b><small>' + esc(i.nombre) + ' · ' + esc(i.mercado) + ' · ' + esc(i.estado) + '</small></td><td>' +
        esc(money(i.cierre,i.moneda)) + '<small>' + esc(i.fecha || "Sin precios históricos") +
        '</small></td><td><button class="secundario" data-historial="' + index + '">Historial</button> <button class="secundario" data-comprar="' +
        index + '" ' + (i.cierre == null || i.moneda == null ? "disabled" : "") + '>Comprar</button></td></tr>').join("") :
        '<tr><td colspan="3">No hay instrumentos para esta búsqueda.</td></tr>';
      $("pagina").textContent = "Página " + (pagina + 1);
      $("anterior").disabled = pagina === 0; $("siguiente").disabled = filas.length < 50;
    } catch (e) { $("catalogoError").textContent = e.message; }
  }
  $("catalogoFilas").onclick = async e => {
    const comprar = e.target.closest("[data-comprar]");
    if (comprar) {
      const ticker = filas[comprar.dataset.comprar].simbolo;
      $("catalogoPanel").close();
      $("tipoInstrumento").value = instrumentos.find(i => i.ticker === ticker)?.tipo || "";
      renderInstrumentos(); $("instrumento").value = ticker; seleccionar(); return;
    }
    const historial = e.target.closest("[data-historial]"); if (!historial) return;
    const i = filas[historial.dataset.historial];
    $("tituloHistorial").textContent = "Historial de " + i.simbolo;
    $("historialFilas").innerHTML = '<tr><td colspan="3">Cargando…</td></tr>'; $("historial").showModal();
    try {
      const precios = await api("/catalogo/" + i.id + "/precios?limite=90");
      $("historialFilas").innerHTML = precios.length ? precios.map(p => '<tr><td>' + esc(p.fecha) +
        '</td><td>' + esc(money(p.cierre,i.moneda)) + '</td><td>' + numero(p.volumen) + '</td></tr>').join("") :
        '<tr><td colspan="3">Este instrumento no tiene precios cargados.</td></tr>';
    } catch(e) { $("historialFilas").innerHTML = '<tr><td colspan="3">' + esc(e.message) + '</td></tr>'; }
  };
  $("busquedaCatalogo").onsubmit = e => { e.preventDefault(); pagina = 0; busqueda = $("buscar").value.trim(); cargarCatalogo(); };
  $("anterior").onclick = () => { if (pagina > 0) { pagina--; cargarCatalogo(); } };
  $("siguiente").onclick = () => { pagina++; cargarCatalogo(); };
  $("cerrarHistorial").onclick = () => $("historial").close();
  $("instrumento").onchange = seleccionar;
  $("tipoInstrumento").onchange = renderInstrumentos;
  ["cantidad", "precio"].forEach(id => $(id).addEventListener("input", actualizarTotal));
  $("abrirModal").onclick = () => abrirCompra();
  $("abrirCatalogo").onclick = () => { $("catalogoPanel").showModal(); cargarCatalogo(); };
  $("cerrarCatalogo").onclick = () => $("catalogoPanel").close();
  $("cerrarModal").onclick = () => modal.close();
  $("formCompra").onsubmit = async e => {
    e.preventDefault(); const boton = $("guardarCompra"); boton.disabled = true; $("error").textContent = "";
    try {
      const r = await pedir("/compras", { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ticker: $("ticker").value, cantidad: $("cantidad").value, precioUnitario: $("precio").value, fecha: $("fecha").value }) });
      modal.close(); await ordenRegistrada(r);
    } catch(e) { $("error").textContent = e.message; }
    finally { boton.disabled = false; }
  };

  // ---- Órdenes por mensajería: 202 = la orden quedó en la cola y se ejecuta después ----
  const estados = { PENDIENTE: "Pendiente", COMPLETADA: "Completada", RECHAZADA: "Rechazada", ERROR: "Error técnico", CANCELADA: "Cancelada", DESCARTADA: "Eliminada" };
  const vistas = { pendientes: "PENDIENTE", completadas: "COMPLETADA", rechazadas: "RECHAZADA,ERROR" };
  const vacias = { pendientes: "No tenés órdenes pendientes.", completadas: "Todavía no hay órdenes completadas.", rechazadas: "No tenés órdenes rechazadas." };
  let vistaOrdenes = "pendientes", sondeo = null, sondeosRestantes = 0, pendientes = new Set();
  const transacciones = $("transacciones");
  async function ordenRegistrada(r) {
    if (r.status !== 202) { renderResumen(r.body); return; }
    const o = r.body;
    avisar("Orden #" + o.id + " de " + (o.tipo === "VENTA" ? "venta" : "compra") + " enviada: queda pendiente hasta que se ejecute. La podés seguir en Transacciones.");
    seguirPendientes([o.id]); await actualizarContadores();
  }
  function avisar(texto) { $("avisoOrden").textContent = texto; $("avisoOrden").hidden = false; }
  function filaOrden(o) {
    let acciones = "";
    if (o.estado === "PENDIENTE") acciones = '<button class="secundario" data-cancelar="' + o.id + '">Cancelar</button>';
    if (o.estado === "RECHAZADA" || o.estado === "ERROR") acciones = '<button class="secundario" data-reintentar="' + o.id + '">Enviar de nuevo</button>' +
      '<button class="secundario" data-descartar="' + o.id + '">Eliminar</button>';
    return '<article class="orden"><div><b>#' + o.id + ' · ' + (o.tipo === "VENTA" ? "Venta" : "Compra") + ' de ' + numero(o.cantidad) + ' ' + esc(o.ticker) +
      '</b><small>' + esc(money(o.total, o.moneda)) + ' · fecha ' + esc(o.fechaOperacion || "") + '</small>' +
      (o.motivo ? '<small class="motivo">' + esc(o.motivo) + '</small>' : "") +
      '</div><div class="acciones-orden"><span class="estado estado-' + o.estado.toLowerCase() + '">' + (estados[o.estado] || esc(o.estado)) + '</span>' + acciones + '</div></article>';
  }
  async function cargarVista() {
    const lista = await api("/ordenes?estado=" + encodeURIComponent(vistas[vistaOrdenes]) + "&limite=50");
    $("ordenes").innerHTML = lista.length ? lista.map(filaOrden).join("") : "<p>" + vacias[vistaOrdenes] + "</p>";
  }
  function contador(id, n) { $(id).textContent = n; $(id).hidden = !n; }
  // Contadores del botón y de las pestañas. Devuelve las pendientes.
  async function actualizarContadores() {
    const [pend, rech] = await Promise.all([api("/ordenes?estado=PENDIENTE&limite=100"), api("/ordenes?estado=" + encodeURIComponent("RECHAZADA,ERROR") + "&limite=100")]);
    contador("cantidadPendientes", pend.length); contador("contadorPendientes", pend.length); contador("contadorRechazadas", rech.length);
    return pend;
  }
  // Consulta cada 3 s mientras haya órdenes pendientes (hasta 2 minutos); al terminar alguna, refresca el portfolio.
  function seguirPendientes(ids) {
    ids.forEach(id => pendientes.add(id)); sondeosRestantes = 40;
    if (!sondeo && pendientes.size) sondeo = setInterval(sondear, 3000);
  }
  async function sondear() {
    try {
      const siguen = new Set((await actualizarContadores()).map(o => o.id));
      const terminaron = [...pendientes].some(id => !siguen.has(id));
      pendientes = siguen;
      if (terminaron) renderResumen(await api("/portfolio/resumen"));
      if (transacciones.open) await cargarVista();
    } catch (e) { /* un sondeo fallido no corta los siguientes */ }
    if (!pendientes.size || --sondeosRestantes <= 0) { clearInterval(sondeo); sondeo = null; }
  }
  function mostrarVista(vista) {
    vistaOrdenes = vista; $("errorOrdenes").textContent = "";
    for (const tab of transacciones.querySelectorAll("[data-vista]")) {
      tab.classList.toggle("activa", tab.dataset.vista === vista); tab.setAttribute("aria-selected", tab.dataset.vista === vista);
    }
    $("ordenes").innerHTML = "<p>Cargando…</p>";
    cargarVista().catch(e => { $("errorOrdenes").textContent = e.message; });
  }
  for (const tab of transacciones.querySelectorAll("[data-vista]")) tab.onclick = () => mostrarVista(tab.dataset.vista);
  // Al abrir, lo primero que se ve son las pendientes.
  $("abrirTransacciones").onclick = e => {
    e.currentTarget.closest("details")?.removeAttribute("open"); // cierra el menú de usuario
    transacciones.showModal(); mostrarVista("pendientes"); actualizarContadores().catch(() => {});
  };
  $("cerrarTransacciones").onclick = () => transacciones.close();
  $("ordenes").onclick = async e => {
    const boton = e.target.closest("[data-cancelar], [data-reintentar], [data-descartar]"); if (!boton) return;
    const { cancelar, reintentar, descartar } = boton.dataset;
    if (cancelar && !confirm("¿Cancelar la orden #" + cancelar + "? No se va a ejecutar.")) return;
    if (descartar && !confirm("¿Eliminar la orden #" + descartar + " de la lista?")) return;
    boton.disabled = true; $("errorOrdenes").textContent = "";
    try {
      if (cancelar) { await api("/ordenes/" + cancelar + "/cancelar", { method: "POST" }); avisar("Orden #" + cancelar + " cancelada."); }
      if (reintentar) {
        await api("/ordenes/" + reintentar + "/reintentar", { method: "POST" });
        avisar("Orden #" + reintentar + " enviada de nuevo: queda pendiente hasta que se ejecute."); seguirPendientes([Number(reintentar)]);
      }
      if (descartar) { await api("/ordenes/" + descartar + "/descartar", { method: "POST" }); avisar("Orden #" + descartar + " eliminada de la lista."); }
      await Promise.all([cargarVista(), actualizarContadores()]);
    } catch (err) { $("errorOrdenes").textContent = err.message; boton.disabled = false; }
  };

  function cargarSelectVenta() {
    $("instrumentoVenta").innerHTML = '<option value="">Seleccioná un instrumento…</option>' +
      ultimoResumen.posiciones.map(p => '<option value="' + esc(p.ticker) + '">' + esc(p.nombre) + ' (' + esc(p.ticker) + ') — tenés ' + numero(p.cantidad) + '</option>').join("");
    $("disponibilidadVenta").textContent = ultimoResumen.posiciones.length ? "" : "Todavía no tenés instrumentos para vender. Registrá una compra primero.";
  }
  $("abrirModalVenta").onclick = () => {
    cargarSelectVenta(); $("formVenta").reset();
    const hoy = new Date(); $("fechaVenta").value = new Date(hoy.getTime() - hoy.getTimezoneOffset()*60000).toISOString().slice(0,10);
    $("fechaVenta").max = $("fechaVenta").value; $("errorVenta").textContent = "";
    $("instrumentoVenta").onchange({ target: $("instrumentoVenta") });
    modalVenta.showModal();
  };
  $("cerrarModalVenta").onclick = () => modalVenta.close();
  $("instrumentoVenta").onchange = e => {
    const p = ultimoResumen.posiciones.find(x => x.ticker === e.target.value);
    $("guardarVenta").disabled = !p;
    $("tickerVenta").value = p?.ticker || ""; $("cantidadVenta").max = p?.cantidad || "";
    monedaVenta = p?.moneda || ""; $("monedaVenta").textContent = monedaVenta;
    $("precioVenta").value = p && dec(p.cantidad) ? (dec(p.actual) / dec(p.cantidad)).toFixed(10) : "";
    actualizarTotalVenta();
  };
  ["cantidadVenta", "precioVenta"].forEach(id => $(id).addEventListener("input", actualizarTotalVenta));
  $("venderTodo").onclick = () => { const p = ultimoResumen.posiciones.find(x => x.ticker === $("tickerVenta").value); if (!p) return; $("cantidadVenta").value = p.cantidad; actualizarTotalVenta(); };
  $("formVenta").onsubmit = async e => {
    e.preventDefault(); const boton = $("guardarVenta"); boton.disabled = true; $("errorVenta").textContent = "";
    try {
      const r = await pedir("/ventas", { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ticker: $("tickerVenta").value, cantidad: $("cantidadVenta").value, precioUnitario: $("precioVenta").value, fecha: $("fechaVenta").value }) });
      modalVenta.close(); await ordenRegistrada(r);
    } catch(e) { $("errorVenta").textContent = e.message; }
    finally { boton.disabled = false; }
  };

  async function iniciar() {
    mysql = (await api("/catalogo/configuracion")).mysql;
    $("origen").textContent = mysql ? "Valuación según el último cierre histórico disponible; no son precios en vivo." : "Portfolio de demostración";
    await Promise.all([cargarInstrumentos(), api("/portfolio/resumen").then(renderResumen)]);
    $("abrirCatalogo").hidden = !mysql;
    $("abrirModal").disabled = false;
    $("abrirModalVenta").disabled = false;
    if (mysql) {
      $("abrirTransacciones").hidden = false;
      seguirPendientes((await actualizarContadores()).map(o => o.id));
    }
  }
  iniciar().catch(e => { $("posiciones").textContent = e.message; });
})();
