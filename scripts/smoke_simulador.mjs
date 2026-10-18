// Entorno local de pruebas: crea dos cuentas aisladas. No registra compras ni ventas.
// node scripts/smoke_simulador.mjs http://127.0.0.1:8080/inversorar
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
const base = process.argv[2] || 'http://127.0.0.1:8080/inversorar';
const types = ['ACCION','CRIPTO','BONO','CEDEAR','MONEDA'];
const path = '/api/portfolio/simulacion';
class Client {
    cookie = '';
    async call(route, {method='GET', data, form=false, status=200}={}) {
        const headers = {Cookie:this.cookie};
        if(data) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json';
        const response = await fetch(base+route,{method,headers,redirect:'manual',body:data ? (form ? new URLSearchParams(data).toString() : JSON.stringify(data)) : undefined});
        const cookie = response.headers.getSetCookie().find(v=>v.startsWith('JSESSIONID='));
        if(cookie) this.cookie=cookie.split(';')[0];
        const body=await response.text();
        assert.equal(response.status,status,route+': '+body.slice(0,200));
        return body;
    }
    async csrf(route) { return (await this.call(route)).match(/name="csrf" value="([^"]+)"/)[1]; }
    async json(route,options) { return JSON.parse(await this.call(route,options)); }
    async create() {
        this.email=`sim-${randomUUID()}@example.com`; this.password=randomUUID();
        await this.call('/registro',{method:'POST',form:true,status:302,data:{nombre:'Simulador',apellido:'Prueba',email:this.email,password:this.password,confirmarPassword:this.password,csrf:await this.csrf('/registro')}});
        await this.login();
    }
    async login() { await this.call('/login',{method:'POST',form:true,status:302,data:{email:this.email,password:this.password,csrf:await this.csrf('/login')}}); }
    async save(capital,porcentajes,status=200) { return this.json(path+'/plan',{method:'PUT',data:{capital,porcentajes},status}); }
}
const guest=new Client();
await guest.call('/simulador',{status:302});
await guest.call(path+'/plan',{method:'PUT',data:{capital:10,porcentajes:{}},status:401});
const a=new Client(), b=new Client(); await a.create(); await b.create();
const page=await a.call('/simulador');
for(const type of types) assert.ok(page.includes(`data-tipo="${type}"`));
const before=await a.json('/api/portfolio/resumen');
for(const type of types) {
    const dto=await a.save('100000',{[type]:'100'});
    assert.equal(dto.importes[type],100000); assert.equal(dto.disponible,0);
}
let dto=await a.save('0.01',Object.fromEntries(types.map(t=>[t,'20'])));
for(const type of types) assert.equal(dto.importes[type],0.002);
assert.equal(dto.disponible,0);
dto=await a.save('100000',{ACCION:'40',BONO:'30',CRIPTO:'20'});
assert.equal(dto.disponible,10000);
const snapshot=await a.json(path);
for(const [capital,percentages] of [['-1',{}],['0.001',{}],['1000000000000',{}],['100',{ACCION:'100.0001'}],['100',{ACCION:'-1'}],['100',{BONO:'0.00001'}],['200',{ACCION:60,BONO:41}],['100',null]]) {
    await a.save(capital,percentages,400);
    assert.deepEqual(await a.json(path),snapshot);
}
await a.save('100',{DESCONOCIDO:10},400);
assert.deepEqual(await a.json(path),snapshot);
assert.equal((await b.json(path)).capital,0);
await a.call('/simulador'); assert.deepEqual(await a.json(path),snapshot);
dto=await a.save('999999999999.99',{MONEDA:'0.0001'});
assert.equal(dto.importes.MONEDA,999999.99999999);
dto=await a.save('0',{CEDEAR:100}); assert.equal(dto.importes.CEDEAR,0);
await a.call(path,{method:'DELETE',status:204});
assert.equal((await a.json(path)).capital,0);
assert.deepEqual(await a.json('/api/portfolio/resumen'),before);
await a.save('500',{CRIPTO:100});
await a.call('/logout',{method:'POST',form:true,status:302,data:{csrf:await a.csrf('/registro')}});
await a.call(path,{status:401}); await a.login();
assert.equal((await a.json(path)).capital,0);
console.log('OK: cinco tipos, precisión, límites, atomicidad, autenticación, aislamiento, recarga, reinicio, logout y cartera sin cambios.');
