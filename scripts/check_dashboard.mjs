// Comprueba que el JSP contiene los controles usados al inicializar el JS.
// Ejecuta también carga de resumen, apertura de compra y venta con datos simulados.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync('src/main/webapp/WEB-INF/views/dashboard.jsp', 'utf8');
const code = fs.readFileSync('src/main/webapp/assets/dashboard.js', 'utf8');
const nodes = new Map([...html.matchAll(/\bid="([^"]+)"/g)].map(([, id]) => [id, {
    value: '', disabled: true, textContent: '', innerHTML: '',
    addEventListener() {}, appendChild() {}, replaceChildren() { this.value = ''; },
    reset() {}, showModal() { this.open = true; }, close() { this.open = false; }
}]));
const calls = [];
const resumen = { posiciones: [], capitalInvertido: 0, patrimonioTotal: 0, gananciaTotal: 0, moneda: 'USD' };
const context = {
    window: { APP_CONTEXT: '/inversorar' },
    document: { getElementById: id => nodes.get(id) ?? null, createElement: () => ({ appendChild() {} }) },
    Option: function(text, value) { this.text = text; this.value = value; },
    fetch: async (url, options) => {
        calls.push({ url, options });
        const body = url.endsWith('/configuracion') ? { mysql: true }
            : url.endsWith('/instrumentos') ? [{ ticker: 'AAPL', nombre: 'Apple', tipo: 'ACCION', cotizacionActual: 195, moneda: 'USD' }]
            : resumen;
        return { ok: true, json: async () => body };
    }
};
vm.runInNewContext(code, context);
await new Promise(resolve => setImmediate(resolve));
assert.equal(nodes.get('abrirModal').disabled, false);
assert.equal(nodes.get('abrirModalVenta').disabled, false);
assert.ok(calls.some(c => c.url.endsWith('/portfolio/resumen')));
assert.match(nodes.get('posiciones').innerHTML, /Aún no hay inversiones/);
nodes.get('abrirModal').onclick();
assert.equal(nodes.get('modal').open, true);
nodes.get('abrirModalVenta').onclick();
assert.equal(nodes.get('modalVenta').open, true);
assert.equal(nodes.get('guardarVenta').disabled, true);
assert.match(nodes.get('disponibilidadVenta').textContent, /compra primero/);
resumen.posiciones.push({ ticker: 'AAPL', nombre: 'Apple', cantidad: 10, actual: 1950, moneda: 'USD' });
nodes.get('abrirModalVenta').onclick();
nodes.get('instrumentoVenta').value = 'AAPL';
nodes.get('instrumentoVenta').onchange({ target: nodes.get('instrumentoVenta') });
assert.equal(nodes.get('guardarVenta').disabled, false);
assert.equal(nodes.get('cantidadVenta').max, 10);
nodes.get('venderTodo').onclick();
assert.equal(nodes.get('cantidadVenta').value, 10);
await nodes.get('formVenta').onsubmit({ preventDefault() {} });
const venta = calls.find(c => c.url.endsWith('/ventas'));
assert.equal(venta.options.method, 'POST');
assert.equal(JSON.parse(venta.options.body).ticker, 'AAPL');
console.log('OK: dashboard inicializa, carga datos y permite abrir compra y enviar venta.');
