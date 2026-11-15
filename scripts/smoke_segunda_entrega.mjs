// Ejecutar contra una instancia MySQL de demostración. Crea dos cuentas y dos órdenes.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { setTimeout } from 'node:timers/promises';

const base = process.argv[2] ?? 'http://127.0.0.1:8080/inversorar';
const ticker = process.argv[3] ?? 'BTCUSDT';

class Cliente {
    cookies = new Map();

    async pedir(path, { method = 'GET', data, form = false, status = 200, cookie, fetchSite } = {}) {
        const headers = { Cookie: cookie ?? this.cookiesActuales() };
        if (fetchSite) headers['Sec-Fetch-Site'] = fetchSite;
        if (data) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json';
        const response = await fetch(base + path, {
            method, headers, redirect: 'manual',
            body: data ? (form ? new URLSearchParams(data) : JSON.stringify(data)) : undefined
        });
        for (const raw of response.headers.getSetCookie()) {
            const pair = raw.split(';')[0];
            this.cookies.set(pair.slice(0, pair.indexOf('=')), pair);
        }
        const body = await response.text();
        assert.equal(response.status, status, `${method} ${path}: ${body.slice(0, 300)}`);
        return response.headers.get('content-type')?.includes('application/json') ? JSON.parse(body) : body;
    }

    cookiesActuales() { return [...this.cookies.values()].join('; '); }

    async csrf(path) {
        const html = await this.pedir(path);
        return html.match(/name="csrf" value="([^"]+)"/)[1];
    }

    async altaYLogin() {
        const email = `entrega-${randomUUID()}@example.com`;
        const password = randomUUID();
        await this.pedir('/registro', {
            method: 'POST', form: true, status: 302,
            data: { email, password, confirmarPassword: password, nombre: 'Demo', apellido: 'Entrega', csrf: await this.csrf('/registro') }
        });
        await this.pedir('/login', {
            method: 'POST', form: true, status: 302, data: { email, password, csrf: await this.csrf('/login') }
        });
        assert.match(this.cookies.get('INVERSORAR_ACCESS'), /^INVERSORAR_ACCESS=[^.]+\.[^.]+\.[^.]+$/);
    }

    async esperarOrden(id) {
        for (let intento = 0; intento < 45; intento++) {
            const orden = await this.pedir(`/api/ordenes/${id}`);
            if (orden.estado !== 'PENDIENTE') return orden;
            await setTimeout(500);
        }
        assert.fail(`La orden ${id} sigue pendiente`);
    }
}

const ana = new Cliente();
const bruno = new Cliente();
await ana.pedir('/api/ordenes', { status: 401 });
await ana.altaYLogin();
await bruno.altaYLogin();
await ana.pedir('/dashboard');
const completa = ana.cookiesActuales();
await ana.pedir('/api/ordenes', { cookie: ana.cookies.get('JSESSIONID'), status: 401 });
await ana.pedir('/api/ordenes', { cookie: ana.cookies.get('INVERSORAR_ACCESS'), status: 401 });
const [cabecera, carga, firma] = ana.cookies.get('INVERSORAR_ACCESS').split('.');
const alterado = `${cabecera}.${carga}.${firma[0] === 'A' ? 'B' : 'A'}${firma.slice(1)}`;
await ana.pedir('/api/ordenes', { cookie: `${ana.cookies.get('JSESSIONID')}; ${alterado}`, status: 401 });

const ahora = new Date();
const fecha = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
const trade = { ticker, cantidad: 0.002, precioUnitario: 60000, fecha };
await ana.pedir('/api/compras', { method: 'POST', data: trade, status: 403, fetchSite: 'same-site' });
const comprar = await ana.pedir('/api/compras', { method: 'POST', data: trade, status: 202 });
assert.equal(comprar.estado, 'PENDIENTE');
assert.equal((await ana.esperarOrden(comprar.id)).estado, 'COMPLETADA');
await bruno.pedir(`/api/ordenes/${comprar.id}`, { status: 404 });
await bruno.pedir(`/api/ordenes/${comprar.id}/cancelar`, { method: 'POST', status: 404 });
for (const accion of ['cancelar', 'reintentar', 'descartar']) {
    const cambio = await ana.pedir(`/api/ordenes/${comprar.id}/${accion}`, { method: 'POST', status: 409 });
    assert.match(cambio.mensaje, /COMPLETADA/);
}
assert.equal((await ana.pedir(`/api/ordenes/${comprar.id}`)).estado, 'COMPLETADA');

const vender = await ana.pedir('/api/ventas', { method: 'POST', data: { ...trade, cantidad: 0.001 }, status: 202 });
assert.equal(vender.estado, 'PENDIENTE');
assert.equal((await ana.esperarOrden(vender.id)).estado, 'COMPLETADA');
const posiciones = await ana.pedir('/api/portfolio/posiciones');
assert.equal(Number(posiciones.find(p => p.ticker === ticker)?.cantidad), 0.001);
assert.equal((await bruno.pedir('/api/portfolio/posiciones')).length, 0);

await ana.pedir('/logout', { method: 'POST', form: true, status: 302, data: { csrf: await ana.csrf('/dashboard') } });
await ana.pedir('/api/ordenes', { cookie: completa, status: 401 });
console.log(`OK: compra #${comprar.id} y venta #${vender.id} vía JMS, portfolio, 401, 404, 409 y logout`);
