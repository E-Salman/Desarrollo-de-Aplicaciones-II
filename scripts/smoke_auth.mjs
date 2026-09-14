// Solo demo H2 aislada: crea dos cuentas y movimientos. Requiere Node 18+.
// node scripts/smoke_auth.mjs http://127.0.0.1:8200/inversorar
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
const base = process.argv[2] || 'http://127.0.0.1:8080/inversorar';
const password = randomUUID();
const suffix = randomUUID();
class Client {
    cookie = '';
    async call(path, { method = 'GET', data, expected = 200, form = false } = {}) {
        const headers = { Cookie: this.cookie };
        if (data) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json';
        let r = await fetch(base + path, { method, headers, redirect: 'manual',
            body: data ? (form ? new URLSearchParams(data).toString() : JSON.stringify(data)) : undefined });
        const cookie = r.headers.getSetCookie().find(c => c.startsWith('JSESSIONID='));
        if (cookie) this.cookie = cookie.split(';')[0];
        assert.equal(r.headers.get('www-authenticate'), null);
        if (r.status === 302 || r.status === 303) {
            return this.call(new URL(r.headers.get('location'), base).pathname.replace('/inversorar', '')
                + new URL(r.headers.get('location'), base).search, { expected });
        }
        const body = await r.text();
        assert.equal(r.status, expected, `${path}: ${body.slice(0, 300)}`);
        return body;
    }
    async csrf(path) { return (await this.call(path)).match(/name="csrf" value="([^"]+)"/)[1]; }
    async json(path, options) { return JSON.parse(await this.call(path, options)); }
}
const ana = new Client(), bruno = new Client();
assert.match(await ana.call('/dashboard'), /action="\/inversorar\/login"/);
await new Client().call('/api/portfolio/resumen', { expected: 401 });
await ana.call('/login', { method: 'POST', data: {}, form: true, expected: 403 });
for (const [c, name] of [[ana, 'ana'], [bruno, 'bruno']]) {
    const data = { nombre: name, apellido: 'Prueba', email: `${name}-${suffix}@example.com`,
        password, confirmarPassword: password, csrf: await c.csrf('/registro') };
    if (name === 'ana') await c.call('/registro', { method: 'POST', data: { ...data, confirmarPassword: 'otra' }, form: true, expected: 400 });
    await c.call('/registro', { method: 'POST', data, form: true });
    await c.call('/api/portfolio/resumen', { expected: 401 });
    await c.call('/registro', { method: 'POST', data, form: true, expected: 400 });
    const login = { email: data.email, password: 'incorrecta', csrf: await c.csrf('/login') };
    await c.call('/login', { method: 'POST', data: login, form: true, expected: 401 });
    await c.call('/login', { method: 'POST', data: { ...login, password }, form: true });
    assert.equal((await c.json('/api/portfolio/resumen')).capitalInvertido, 0);
}
const compra = { ticker: 'AAPL', cantidad: 10, precioUnitario: 180, fecha: new Date().toISOString().slice(0, 10) };
assert.equal((await ana.json('/api/compras', { method: 'POST', data: compra, expected: 201 })).capitalInvertido, 1800);
assert.equal((await bruno.json('/api/portfolio/resumen')).capitalInvertido, 0);
await ana.call('/api/ventas', { method: 'POST', data: { ...compra, cantidad: 2, precioUnitario: 200 }, expected: 201 });
assert.equal((await ana.json('/api/portfolio/resumen')).capitalInvertido, 1440);
await ana.call('/api/portfolio/simulacion/capital', { method: 'PUT', data: { valor: 1000 } });
assert.equal((await bruno.json('/api/portfolio/simulacion')).capital, 0);
await ana.call('/logout', { method: 'POST', data: { csrf: await ana.csrf('/dashboard') }, form: true });
await ana.call('/api/portfolio/resumen', { expected: 401 });
await ana.call('/login', { method: 'POST', form: true,
    data: { email: `ana-${suffix}@example.com`, password, csrf: await ana.csrf('/login') } });
assert.equal((await ana.json('/api/portfolio/resumen')).capitalInvertido, 1440);
assert.equal((await ana.json('/api/portfolio/simulacion')).capital, 0);
console.log('OK: registro, duplicados, confirmación, CSRF, login, 401, compras, ventas, aislamiento y logout.');
