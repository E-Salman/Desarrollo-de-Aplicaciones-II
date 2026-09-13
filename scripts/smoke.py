"""Prueba del WAR sobre una base de demo VACÍA. Crea una compra de prueba.
Requiere ana/bruno con USUARIO y lector con LECTOR; contraseñas por entorno.
Usar solo en una instalación aislada. No reinicia ni borra ninguna base.
"""
import urllib.request,urllib.error,http.cookiejar,base64,json,datetime,os
from pathlib import Path
creds={u:os.environ['SMOKE_'+u.upper()+'_PASSWORD'] for u in ['ana','bruno','lector']}
base=os.environ.get('SMOKE_BASE_URL','http://127.0.0.1:8080/inversorar')
def client(): return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
ana,bruno=client(),client()
checks=[]
def call(opener,method,path,user=None,body=None,expected=200):
 headers={}
 if user: headers['Authorization']='Basic '+base64.b64encode((user+':'+creds[user]).encode()).decode()
 if body is not None: headers['Content-Type']='application/json'
 req=urllib.request.Request(base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
 try:
  with opener.open(req,timeout=20) as r: status,data=r.status,r.read()
 except urllib.error.HTTPError as r: status,data=r.code,r.read()
 assert status==expected,(method,path,status,data[:1200])
 checks.append(f'{method} {path}: {status} ({user or "anonimo"})')
 return json.loads(data) if data and data.lstrip().startswith((b'{',b'[')) else data
call(client(),'GET','/api/portfolio/resumen',expected=401)
call(client(),'POST','/api/compras','lector',{},403)
call(ana,'GET','/dashboard','ana')
r=call(ana,'GET','/api/portfolio/resumen','ana'); assert r['capitalInvertido']==0
call(ana,'POST','/api/compras','ana',{'ticker':'AAPL','cantidad':-1,'precioUnitario':180,'fecha':str(datetime.date.today())},400)
r=call(ana,'POST','/api/compras','ana',{'ticker':'AAPL','cantidad':10,'precioUnitario':180,'fecha':str(datetime.date.today())},201)
assert r['capitalInvertido']==1800 and r['patrimonioTotal']==1950 and r['gananciaTotal']==150,r
assert call(bruno,'GET','/api/portfolio/resumen','bruno')['capitalInvertido']==0
p=call(ana,'GET','/api/portfolio/posiciones','ana')[0]; assert p['precioPromedio']==180 and abs(p['rendimientoPorcentaje']-8.3333)<.00001
call(ana,'GET','/api/portfolios/1/resumen','ana',expected=404)
call(ana,'PUT','/api/simulador/capital','ana',{'valor':1000000})
r=call(ana,'PUT','/api/simulador/porcentajes/ACCION','ana',{'valor':40}); assert r['importes']['ACCION']==400000
call(ana,'PUT','/api/simulador/porcentajes/BONO','ana',{'valor':70},400)
assert call(ana,'GET','/api/simulador','ana')['importes']['ACCION']==400000
assert call(bruno,'GET','/api/simulador','bruno')['capital']==0
call(ana,'GET','/api/simulador','bruno',expected=403)
call(ana,'DELETE','/api/simulador','ana',expected=204)
assert call(ana,'GET','/api/simulador','ana')['capital']==0
call(ana,'DELETE','/api/simulador','ana',expected=204)
call(bruno,'DELETE','/api/simulador','bruno',expected=204)
print('Todas las aserciones correctas.')
print('\n'.join(checks))
