(() => {
  const ctx = window.APP_CONTEXT, $ = (id) => document.getElementById(id), modal = $("modal"), modalVenta = $("modalVenta");
  let instrumentos = [], ultimoResumen = { posiciones: [] };
  const money = (n) => new Intl.NumberFormat("es-AR", { style: "currency", currency: "USD" }).format(n || 0);
  const dec = (n) => Number(n || 0);
  function actualizarTotal() { $("total").value = money(dec($("cantidad").value) * dec($("precio").value)); }
  function actualizarTotalVenta() { $("totalVenta").value = money(dec($("cantidadVenta").value) * dec($("precioVenta").value)); }
  function renderResumen(r) { ultimoResumen = r; $("capital").textContent = money(r.capitalInvertido); $("patrimonio").textContent = money(r.patrimonioTotal); $("ganancia").textContent = money(r.gananciaTotal); $("posiciones").innerHTML = r.posiciones.length ? r.posiciones.map(p => `<article class="posicion"><div><b>${p.nombre}</b><small>${p.ticker} · ${p.tipo}</small></div><div><b>${money(p.actual)}</b><small class="${dec(p.rendimiento) >= 0 ? "positivo" : "negativo"}">${dec(p.rendimiento) >= 0 ? "+" : ""}${money(p.rendimiento)}</small></div></article>`).join("") : "<p>Aún no hay inversiones.</p>"; }
  async function cargarResumen() { const r = await fetch(ctx + "/api/portfolios/1/resumen"); renderResumen(await r.json()); }
  async function cargarInstrumentos() { instrumentos = await (await fetch(ctx + "/api/instrumentos")).json(); const grupos = instrumentos.reduce((a, i) => ((a[i.tipo] ||= []).push(i), a), {}); $("instrumento").innerHTML = '<option value="">Seleccioná un instrumento…</option>' + Object.entries(grupos).map(([tipo, lista]) => `<optgroup label="${tipo}">${lista.map(i => `<option value="${i.ticker}">${i.nombre} (${i.ticker})</option>`).join("")}</optgroup>`).join(""); }
  $("instrumento").addEventListener("change", e => { const i = instrumentos.find(x => x.ticker === e.target.value); $("ticker").value = i?.ticker || ""; $("precio").value = i?.cotizacionActual || ""; actualizarTotal(); });
  ["cantidad", "precio"].forEach(id => $(id).addEventListener("input", actualizarTotal));
  $("abrirModal").onclick = () => { $("formCompra").reset(); $("fecha").value = new Date().toISOString().slice(0, 10); $("error").textContent = ""; modal.showModal(); };
  $("cerrarModal").onclick = () => modal.close();
  $("formCompra").addEventListener("submit", async e => { e.preventDefault(); const response = await fetch(ctx + "/api/compras", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ticker: $("ticker").value, cantidad: dec($("cantidad").value), precioUnitario: dec($("precio").value), fecha: $("fecha").value }) }); const body = await response.json(); if (!response.ok) { $("error").textContent = body.mensaje || "No se pudo registrar la compra."; return; } renderResumen(body); modal.close(); });

  function cargarSelectVenta() { $("instrumentoVenta").innerHTML = '<option value="">Seleccioná un instrumento…</option>' + ultimoResumen.posiciones.map(p => `<option value="${p.ticker}">${p.nombre} (${p.ticker}) — tenés ${p.cantidad}</option>`).join(""); }
  $("abrirModalVenta").onclick = () => { cargarSelectVenta(); $("formVenta").reset(); $("fechaVenta").value = new Date().toISOString().slice(0, 10); $("errorVenta").textContent = ""; modalVenta.showModal(); };
  $("cerrarModalVenta").onclick = () => modalVenta.close();
  $("instrumentoVenta").addEventListener("change", e => { const p = ultimoResumen.posiciones.find(x => x.ticker === e.target.value); $("tickerVenta").value = p?.ticker || ""; $("cantidadVenta").max = p?.cantidad || ""; $("precioVenta").value = p ? dec(p.actual) / dec(p.cantidad) : ""; actualizarTotalVenta(); });
  ["cantidadVenta", "precioVenta"].forEach(id => $(id).addEventListener("input", actualizarTotalVenta));
  $("venderTodo").onclick = () => { const p = ultimoResumen.posiciones.find(x => x.ticker === $("tickerVenta").value); if (!p) return; $("cantidadVenta").value = p.cantidad; actualizarTotalVenta(); };
  $("formVenta").addEventListener("submit", async e => { e.preventDefault(); const response = await fetch(ctx + "/api/ventas", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ticker: $("tickerVenta").value, cantidad: dec($("cantidadVenta").value), precioUnitario: dec($("precioVenta").value), fecha: $("fechaVenta").value }) }); const body = await response.json(); if (!response.ok) { $("errorVenta").textContent = body.mensaje || "No se pudo registrar la venta."; return; } renderResumen(body); modalVenta.close(); });

  Promise.all([cargarInstrumentos(), cargarResumen()]).catch(() => { $("posiciones").innerHTML = "<p>No se pudo conectar con el componente.</p>"; });
})();
