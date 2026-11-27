# Integraciones externas: SOAP, REST y resiliencia

La consigna pide integrarse con un **sistema legado por SOAP**, con manejo de Fault y protección contra XXE, y con un **sistema moderno por REST**. Pide además resiliencia: un **timeout de 5 segundos con fallback**. Este documento explica qué significa cada cosa, cómo está resuelto en InversorAR y cómo se demuestra.

## SOAP y REST en dos minutos

Son dos formas de que un programa le pida algo a otro por HTTP. Cambian el formato de los mensajes y las reglas.

**REST** (el sistema moderno). Se pide un recurso por URL con los métodos de HTTP y la respuesta suele venir en JSON. Los errores se indican con el código HTTP (404, 500…). Ejemplo:

```
GET https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT
→ {"symbol":"BTCUSDT","price":"97350.12000000"}
```

**SOAP** (el sistema legado). Es un protocolo más viejo y formal, típico de bancos y mercados. Todo viaja como XML dentro de un sobre (`Envelope` con un `Body`), en general por `POST` a una única URL. Hay un contrato estricto, el **WSDL**, que dice qué operaciones existen y qué campos lleva cada una.

- **Fault:** cuando algo sale mal, SOAP no avisa con un código HTTP sino con un elemento `<soap:Fault>` dentro del XML, que trae un código (`faultcode`) y un motivo (`faultstring`). Manejarlo significa leer ese Fault y reaccionar de forma coherente.
- **XXE (XML External Entity):** es un ataque propio de XML. Un documento puede declarar un `<!DOCTYPE>` con una entidad que apunta a un archivo local (`file:///C:/Windows/win.ini`) o a otro servidor. Si el lector de XML la resuelve, el contenido del archivo termina adentro de la respuesta. La protección consiste en configurar el lector para que **rechace cualquier DOCTYPE** y no resuelva entidades externas.

## Dónde encaja cada integración

```
Usuario crea orden ──► cola JMS "ordenes" ──► EjecutorOrdenesMDB ──► EjecutorOrdenes
                                                                         │ SOAP (5 s)
                                                                         ▼
                                                              Mercado legado (:8090)
                                                                         │
             EJECUTADA → COMPLETADA · Fault Client → RECHAZADA · timeout/caída/Fault Server/XXE → reintento → DLQ → ERROR

Dashboard ──► PortfolioService ──► CotizacionStrategy = CotizacionBinance ──REST (5 s)──► api.binance.com
                                                         └─ fallback ──► CotizacionCatalogo (último cierre en MySQL)
```

| Integración | Sistema | Clase | Fallback ante timeout de 5 s |
|---|---|---|---|
| SOAP | Mercado/ALyC (legado): ejecuta la orden | `integracion.MercadoLegadoCliente` | La transacción se deshace y el broker la intenta hasta 3 veces, con 5 s entre intentos. Después la DLQ marca la orden **ERROR**. El usuario la puede reenviar desde Transacciones. |
| REST | Binance (moderno): precio actual | `integracion.CotizacionBinance` | Último cierre guardado en MySQL (`CotizacionCatalogo`). El dashboard lo indica: "Binance no disponible · último cierre". |

## Por qué el mercado legado es un simulador

Ningún ALyC ni BYMA ofrece a estudiantes un servicio SOAP público para ejecutar órdenes, y uno real ejecutaría con dinero de verdad. Por eso el mercado se reemplaza por un **simulador**: `legado/MercadoLegado.java`.

Lo que se evalúa es **el código de InversorAR que habla con el mercado**, no el mercado. El simulador es falso, pero todo lo que está del lado de InversorAR es real:

| Falso (el simulador) | Real (en el WAR de InversorAR) |
|---|---|
| Decide si la orden sale bien o mal | Arma el sobre SOAP y lo manda por HTTP |
| Inventa un número de operación | Corta la espera a los 5 s |
| | Lee un `<soap:Fault>` y lo convierte en RECHAZADA con su motivo |
| | Rechaza un XML con `DOCTYPE` malicioso (XXE) |
| | Reintenta, manda a la DLQ y marca la orden ERROR |

Si un ALyC diera un endpoint SOAP real, el cliente funcionaría igual: alcanza con cambiar la URL (`"-Dinversorar.mercado.url=…"`, entre comillas en PowerShell) y adaptar los nombres de campo a su WSDL.

Además, con un sistema real no se podrían **provocar los errores** que la consigna pide demostrar. Nadie puede hacer que un mercado real responda lento, tire un Fault cuando uno quiera o devuelva un XML malicioso. Con el simulador sí, y se ve en vivo que InversorAR reacciona bien a cada caso. En la industria esto se llama *stub* o *mock server* y se usa siempre que se depende de un sistema externo que no se controla (bancos, AFIP, pasarelas de pago).

El simulador es **otro programa**: otro proceso, otro puerto, y se prende y apaga por separado. No forma parte del build ni del WAR; es un único archivo Java que se ejecuta sin compilar y usa solo el JDK. Con REST pasa lo contrario: Binance es real y público, así que no hace falta simularlo.

## Contrato SOAP

El WSDL está en `GET http://localhost:8090/mercado?wsdl`. La operación es `ejecutarOrden`, con namespace `urn:inversorar:mercado`.

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <m:ejecutarOrden xmlns:m="urn:inversorar:mercado">
      <m:ordenId>42</m:ordenId><m:tipo>COMPRA</m:tipo><m:ticker>BTCUSDT</m:ticker>
      <m:cantidad>0.002</m:cantidad><m:precio>97350.12</m:precio>
    </m:ejecutarOrden>
  </soap:Body>
</soap:Envelope>
```

La respuesta OK trae `<m:estado>EJECUTADA</m:estado><m:numeroOperacion>MKT-1001</m:numeroOperacion>`. Un rechazo es un Fault con HTTP 500, como manda SOAP 1.1:

```xml
<soap:Fault><faultcode>soap:Client</faultcode><faultstring>Instrumento BTCUSDT suspendido por el mercado.</faultstring></soap:Fault>
```

Cómo interpreta cada respuesta `MercadoLegadoCliente`:

| Respuesta | Resultado en la orden |
|---|---|
| `EJECUTADA` con número de operación | COMPLETADA; el número queda en el log |
| Fault `soap:Client`: el mercado rechaza la orden | RECHAZADA con motivo "Rechazada por el mercado: …" |
| Fault `soap:Server`: falla interna del mercado | Falla técnica: reintento → DLQ → ERROR |
| Sin respuesta en 5 s, conexión rechazada o HTTP distinto de 200/500 | Falla técnica: reintento → DLQ → ERROR |
| XML inválido, con `DOCTYPE`, o un estado distinto de EJECUTADA | Falla técnica: nunca se completa una orden sin un OK explícito |

**Idempotencia:** el pedido lleva el `ordenId`. Si el mercado ejecutó la orden pero la transacción de InversorAR se deshizo después, por ejemplo porque se cayó MySQL al guardar, el broker reenvía la orden. En ese caso el simulador devuelve la misma respuesta en lugar de ejecutarla dos veces.

**Protección contra XXE (`integracion.XmlSeguro`):** el lector sigue la guía de OWASP para JAXP:

- `disallow-doctype-decl = true`;
- entidades externas generales y de parámetro desactivadas, y sin carga de DTD externos;
- `FEATURE_SECURE_PROCESSING`, `ACCESS_EXTERNAL_DTD` y `ACCESS_EXTERNAL_SCHEMA` vacíos;
- sin XInclude ni expansión de entidades.

Los valores que se insertan en el sobre se escapan. El simulador usa la misma configuración para leer los pedidos que recibe.

**Timeout:** `HttpClient` con `connectTimeout` de 5 s, más una espera total de 5 s sobre el pedido completo (`sendAsync(...).get(5 s)`), que incluye conexión, encabezados y cuerpo.

## Cotización REST con Binance

`CotizacionBinance` es la implementación activa de la Strategy `CotizacionStrategy`: un bean `@Alternative @Priority` que reemplaza a `CotizacionCatalogo` sin tocar `PortfolioServiceBean`.

1. Primero consulta el catálogo MySQL, que fija la **moneda** y el último cierre. La moneda se conserva, porque la usan las órdenes y la conversión a USD.
2. Después pide `GET /api/v3/ticker/price?symbol=<ticker>` a Binance con un timeout de 5 s. Si responde, usa el precio en vivo y el dashboard muestra "Precio en vivo (Binance)".
3. Si no responde, falla o no lista el símbolo (HTTP 400), usa el último cierre de MySQL y el dashboard muestra "Binance no disponible · último cierre".

Además:

- **Caché de 15 s por símbolo**, para no consultar a Binance en cada refresco.
- **Circuito abierto de 30 s** tras una falla de red o un timeout. Mientras dura, los siguientes precios salen directo de MySQL, así que un portfolio con 10 posiciones no espera 10 timeouts.
- Un símbolo que Binance no lista se recuerda 10 minutos y **no** abre el circuito.
- Solo actúa con el perfil MySQL, porque los tickers del demo H2 (`AAPL`, `BTC`) no son pares de Binance. Los tickers se validan (`[A-Z0-9]{2,20}`) antes de armar la URL.
- La URL base se cambia con `"-Dinversorar.binance.url=…"` (entre comillas en PowerShell). Sirve para los tests y para la demo si la red bloquea `api.binance.com`.

## Cómo levantar el simulador

En otra consola, desde la raíz del repo:

```powershell
powershell -ExecutionPolicy Bypass -File legado\iniciar-mercado.ps1
```

El script usa el Java de `JAVA_HOME`, porque el `java` del PATH puede ser una versión vieja que no ejecuta archivos `.java` sueltos. Escucha en `http://localhost:8090/mercado`, muestra cada pedido en la consola y se apaga con Ctrl+C.

Si no está corriendo, InversorAR levanta igual. Las órdenes terminan en ERROR tras los reintentos, que es el comportamiento esperado ante un mercado caído.

Opciones al arrancar: `--puerto=8090`, `--modo=normal|fault|error|lento|xxe` y `--suspendidos=BTCUSDT,ETHUSDT`. El modo también se cambia en vivo desde el navegador:

| URL | Qué hace el mercado | Qué se ve en InversorAR |
|---|---|---|
| `http://localhost:8090/modo/normal` | Ejecuta la orden | COMPLETADA |
| `http://localhost:8090/modo/fault` | Fault `soap:Client` "Mercado cerrado…" | RECHAZADA con ese motivo |
| `--suspendidos=BTCUSDT` | Fault `soap:Client` solo para esos tickers | RECHAZADA "Instrumento … suspendido" |
| `http://localhost:8090/modo/error` | Fault `soap:Server` | 3 intentos → ERROR |
| `http://localhost:8090/modo/lento` | Tarda 10 s en responder | Corte a los 5 s en cada intento → ERROR |
| `http://localhost:8090/modo/xxe` | Responde un XML con entidad a `C:\Windows\win.ini` | El XML se rechaza sin leer el archivo → ERROR |
| Ctrl+C en la consola | Mercado apagado | Conexión rechazada → ERROR |

Con 3 intentos separados 5 s (`max-delivery-attempts=3`), una orden tarda unos 10 s en pasar a ERROR, o unos 25 s en modo lento. Mientras tanto se ve PENDIENTE en Transacciones.

## Guion de demo sugerido

1. Levantar WildFly, con `config\setup-wildfly.ps1`, y el simulador en otra consola.
2. **REST:** abrir el dashboard. Las posiciones dicen "Precio en vivo (Binance)".
3. **SOAP OK:** comprar. La consola del simulador muestra el pedido y "EJECUTADA". La orden queda COMPLETADA.
4. **Fault:** abrir `/modo/fault` y comprar de nuevo. La orden queda RECHAZADA con "Rechazada por el mercado: Mercado cerrado…".
5. **XXE:** abrir `/modo/xxe` y comprar. La consola muestra el XML malicioso, el log de WildFly "DOCTYPE no está permitido" y la orden pasa a ERROR sin haber leído el archivo.
6. **Timeout y fallback:** cerrar el simulador con Ctrl+C y comprar. Se ven los reintentos en el log y la orden pasa a ERROR. Volver a levantarlo y reenviarla desde Transacciones: queda COMPLETADA.
7. **Fallback REST:** reiniciar WildFly con `"-Dinversorar.binance.url=http://localhost:1"` (o cortar la red) y abrir el dashboard: "Binance no disponible · último cierre".

## Pruebas automáticas

No necesitan internet ni el simulador: cada test levanta su propio servidor HTTP local (`ServidorFalso`), así que el CI de GitHub Actions las corre igual.

- `MercadoLegadoClienteTest` cubre:
  - OK y contenido del sobre, con valores escapados;
  - Fault Client con HTTP 500 → rechazo; Fault Server → falla técnica;
  - timeout cortado antes de tiempo y default de 5 s;
  - mercado apagado y respuestas que no son SOAP;
  - XXE con una entidad a un archivo temporal "secreto", que se rechaza y cuyo contenido nunca aparece; entidades en cadena y DTD externos.
- `CotizacionBinanceTest` cubre:
  - precio en vivo conservando la moneda del catálogo;
  - timeout → fallback y circuito de 30 s;
  - símbolo inexistente sin abrir el circuito;
  - caché de 15 s, JSON ilegible y demo H2 sin salir a internet.
- `EjecutorOrdenesTest`:
  - la orden se envía al mercado con sus datos;
  - un Fault la rechaza sin crear la operación;
  - la falla técnica propaga la excepción para el reintento;
  - las órdenes canceladas, duplicadas o inexistentes nunca llaman al mercado.
