// Prueba en vivo de Problem Details, Idempotency-Key, ETag y 429 de la API REST de InversorAR.
// Uso: iniciar sesión, abrir http://localhost:8080/inversorar/dashboard, F12 → Consola, pegar este archivo y Enter.
// Crea UNA orden de compra chica (la misma dos veces, para probar la idempotencia); después se puede eliminar
// desde Transacciones. Al final la API queda limitada ~1 minuto por la prueba de 429.
(async () => {
  const api = location.pathname.split("/").slice(0, 2).join("/") + "/api";
  const resultados = [];
  const ok = (nombre, cumple, detalle) => resultados.push({ prueba: nombre, resultado: cumple ? "OK" : "FALLA", detalle });
  const json = { "Content-Type": "application/json" };
  const clave = () => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2));

  // 1. Problem Details ante una regla de negocio incumplida
  let r = await fetch(api + "/compras", { method: "POST", headers: json, body: JSON.stringify({ ticker: "BTCUSDT", cantidad: -1, precioUnitario: 1 }) });
  let b = await r.json().catch(() => ({}));
  ok("400 regla de negocio → Problem Details", r.status === 400 && r.headers.get("content-type").startsWith("application/problem+json") && b.type && b.title && b.status === 400 && b.detail,
     r.status + " " + r.headers.get("content-type") + " " + JSON.stringify(b));

  // 2. Problem Details ante una ruta inexistente y un JSON roto
  r = await fetch(api + "/no-existe"); b = await r.json().catch(() => ({}));
  ok("404 ruta inexistente → Problem Details", r.status === 404 && b.status === 404, r.status + " " + JSON.stringify(b));
  r = await fetch(api + "/compras", { method: "POST", headers: json, body: "{esto no es json" }); b = await r.json().catch(() => ({}));
  ok("400 JSON inválido → Problem Details", r.status === 400 && b.status === 400, r.status + " " + JSON.stringify(b));

  // 3. ETag: la segunda consulta con If-None-Match responde 304 sin cuerpo
  r = await fetch(api + "/ordenes?limite=5", { cache: "no-store" });
  const etag = r.headers.get("etag");
  const r304 = await fetch(api + "/ordenes?limite=5", { cache: "no-store", headers: { "If-None-Match": etag } });
  ok("ETag + If-None-Match → 304", !!etag && r304.status === 304, "ETag " + etag + " → " + r304.status + ", Cache-Control: " + r.headers.get("cache-control"));

  // 4. Idempotency-Key: la misma compra dos veces crea una sola orden
  const btc = (await (await fetch(api + "/catalogo?q=BTCUSDT")).json()).find(i => i.simbolo === "BTCUSDT") || {};
  const hoy = new Date().toLocaleDateString("sv"); // AAAA-MM-DD en hora local
  const compra = JSON.stringify({ ticker: "BTCUSDT", cantidad: "0.00001", precioUnitario: String(btc.cierre || 60000), fecha: hoy });
  const k = clave();
  const r1 = await fetch(api + "/compras", { method: "POST", headers: { ...json, "Idempotency-Key": k }, body: compra });
  const o1 = await r1.json().catch(() => ({}));
  const r2 = await fetch(api + "/compras", { method: "POST", headers: { ...json, "Idempotency-Key": k }, body: compra });
  const o2 = await r2.json().catch(() => ({}));
  ok("Idempotency-Key repetida → misma orden", r1.status === 202 && r2.status === 202 && o1.id === o2.id && r2.headers.get("idempotent-replayed") === "true",
     "1º " + r1.status + " orden #" + o1.id + " · 2º " + r2.status + " orden #" + o2.id + " (Idempotent-Replayed: " + r2.headers.get("idempotent-replayed") + ")");
  const r3 = await fetch(api + "/compras", { method: "POST", headers: { ...json, "Idempotency-Key": k }, body: compra.replace("0.00001", "0.00002") });
  ok("Misma clave con otro cuerpo → 422", r3.status === 422, String(r3.status));
  // Dos a la vez con la misma clave (doble clic): una sola orden
  const k2 = clave();
  const [p1, p2] = await Promise.all([1, 2].map(() => fetch(api + "/compras", { method: "POST", headers: { ...json, "Idempotency-Key": k2 }, body: compra.replace("0.00001", "0.00003") })));
  const estados = [p1.status, p2.status].sort();
  ok("Doble clic simultáneo → una orden", (estados[0] === 202 && (estados[1] === 409 || estados[1] === 202)), "estados " + estados.join(", ") + " (202 + 409, o 202 + 202 repetida)");

  // 5. 429: más de 30 operaciones por minuto (con cuerpo inválido, no crean órdenes)
  let primero429 = null, retry = null;
  for (let i = 1; i <= 40 && !primero429; i++) {
    const x = await fetch(api + "/compras", { method: "POST", headers: json, body: JSON.stringify({ ticker: "BTCUSDT", cantidad: -1, precioUnitario: 1 }) });
    if (x.status === 429) { primero429 = i; retry = x.headers.get("retry-after"); b = await x.json().catch(() => ({})); }
  }
  ok("Ráfaga de operaciones → 429 con Retry-After", !!primero429 && !!retry && b.status === 429, "429 en el pedido " + primero429 + ", Retry-After " + retry + " s · " + (b.detail || ""));

  console.table(resultados);
  console.log("Órdenes creadas por la prueba: #" + o1.id + (p1.ok ? " y la del doble clic" : "") + ". Se pueden eliminar desde Transacciones.");
})();
