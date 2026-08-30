(() => {
  const ctx = window.APP_CONTEXT, $ = (id) => document.getElementById(id), modal = $("modal"); let instrumentos = [];
  const money = (n) => new Intl.NumberFormat("es-AR", { style: "currency", currency: "USD" }).format(n || 0);
  const dec = (n) => Number(n || 0);
  function actualizarTotal() { $("total").value = money(dec($("cantidad").value) * dec($("precio").value)); }
  function renderResumen(r) { $("capital").textContent = money(r.capitalInvertido); $("patrimonio").textContent = money(r.patrimonioTotal); $("ganancia").textContent = money(r.gananciaTotal); $("posiciones").innerHTML = r.posiciones.length ? r.posiciones.map(p => `<article class="posicion"><div><b>${p.nombre}</b><small>${p.ticker} · ${p.tipo}</small></div><div><b>${money(p.actual)}</b><small class="${dec(p.rendimiento) >= 0 ? "positivo" : "negativo"}">${dec(p.rendimiento) >= 0 ? "+" : ""}${money(p.rendimiento)}</small></div></article>`).join("") : "<p>Aún no hay inversiones.</p>"; }
  async function cargarResumen() { const r = await fetch(ctx + "/api/portfolios/1/resumen"); renderResumen(await r.json()); }
  async function cargarInstrumentos() { instrumentos = await (await fetch(ctx + "/api/instrumentos")).json(); const grupos = instrumentos.reduce((a, i) => ((a[i.tipo] ||= []).push(i), a), {}); $("instrumento").innerHTML = '<option value="">Seleccioná un instrumento…</option>' + Object.entries(grupos).map(([tipo, lista]) => `<optgroup label="${tipo}">${lista.map(i => `<option value="${i.ticker}">${i.nombre} (${i.ticker})</option>`).join("")}</optgroup>`).join(""); }
  $("instrumento").addEventListener("change", e => { const i = instrumentos.find(x => x.ticker === e.target.value); $("ticker").value = i?.ticker || ""; $("precio").value = i?.cotizacionActual || ""; actualizarTotal(); });
  ["cantidad", "precio"].forEach(id => $(id).addEventListener("input", actualizarTotal));
  $("abrirModal").onclick = () => { $("formCompra").reset(); $("fecha").value = new Date().toISOString().slice(0, 10); $("error").textContent = ""; modal.showModal(); };
  $("cerrarModal").onclick = () => modal.close();
  $("formCompra").addEventListener("submit", async e => { e.preventDefault(); const response = await fetch(ctx + "/api/compras", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ticker: $("ticker").value, cantidad: dec($("cantidad").value), precioUnitario: dec($("precio").value), fecha: $("fecha").value }) }); const body = await response.json(); if (!response.ok) { $("error").textContent = body.mensaje || "No se pudo registrar la compra."; return; } renderResumen(body); modal.close(); });
  Promise.all([cargarInstrumentos(), cargarResumen()]).catch(() => { $("posiciones").innerHTML = "<p>No se pudo conectar con el componente.</p>"; });
})();
