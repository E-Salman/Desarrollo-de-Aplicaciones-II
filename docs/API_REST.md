# API REST: Problem Details, Idempotency-Key, ETag y 429

La consigna pide, dentro de resiliencia, **Idempotency-Key y ETag en la API REST** y **errores en formato Problem Details**. En las pruebas de seguridad de la entrega final pide **429 ante fuerza bruta**. Todo vive en `presentation.api` y se aplica a `/api` (y al login, en el caso del 429).

## Errores en formato Problem Details (RFC 9457)

Todo error de `/api` responde `Content-Type: application/problem+json` con este formato:

```json
{
  "type": "urn:inversorar:problema:regla-de-negocio",
  "title": "Regla de negocio incumplida",
  "status": 400,
  "detail": "Tenencia insuficiente: tenés 9 de BTCUSDT y la orden quería vender 89.",
  "instance": "/inversorar/api/ventas",
  "mensaje": "Tenencia insuficiente: tenés 9 de BTCUSDT y la orden quería vender 89."
}
```

`mensaje` es un miembro de extensión, igual a `detail`. Se mantiene para que el JavaScript que ya lo leía siga funcionando.

| Situación | Estado | `type` | Dónde se arma |
|---|---|---|---|
| Regla de negocio incumplida | 400 | `…:regla-de-negocio` | `ReglaNegocioMapper` |
| JSON ilegible en el cuerpo | 400 | `…:json-invalido` | `FiltroProblemas`: el mapper propio de RESTEasy para Jackson responde antes que `ErroresMapper`, y este filtro final lo convierte |
| Idempotency-Key con formato inválido | 400 | `…:idempotency-key-invalida` | `FiltroIdempotencia` |
| Sin sesión | 401 | `about:blank` | `ErrorHttpServlet` (página de error del contenedor) |
| Sin permiso (rol o `@RolesAllowed`) | 403 | `about:blank` | `ErrorHttpServlet` / `ErroresMapper` |
| Escritura desde otro sitio (CSRF) | 403 | `…:origen-no-permitido` | `Autenticacion` |
| Orden inexistente o ajena | 404 | `…:orden-no-encontrada` | `OrdenNoEncontradaMapper` |
| Ruta inexistente, método no permitido, 415… | 404, 405, 415 | `about:blank` | `ErroresMapper` (conserva el estado de JAX-RS) |
| Cambio de estado no permitido | 409 | `…:transicion-invalida` | `TransicionOrdenMapper` |
| Idempotency-Key en curso / con otro cuerpo | 409 / 422 | `…:idempotencia-en-curso` / `…:idempotencia-cuerpo-distinto` | `FiltroIdempotencia` |
| Demasiados pedidos | 429 | `…:demasiados-pedidos` | `FiltroLimitePedidos` |
| Error inesperado | 500 | `about:blank` | `ErroresMapper`: sin detalles internos; el stack va al log con un código que se muestra al usuario |

Como red de seguridad, `FiltroProblemas` convierte en Problem Details cualquier respuesta de error de `/api` que no haya salido con ese formato.

Los 401 y 403 los produce la seguridad antes de llegar a JAX-RS, así que los `ExceptionMapper` no los ven. Por eso `web.xml` define páginas de error (401, 403, 404, 405 y 500) que pasan por `ErrorHttpServlet`. Para `/api` ese servlet responde Problem Details y para las páginas, un HTML simple.

## Idempotency-Key

Aplica a `POST /api/compras`, `POST /api/ventas` y `POST /api/ordenes/{id}/reintentar`. El encabezado es opcional: sin él, el pedido se procesa como siempre.

| Caso | Respuesta |
|---|---|
| Primera vez con la clave | Se procesa normalmente y se guarda la respuesta: estado, cuerpo y `Location` |
| Misma clave y mismo pedido | La respuesta guardada, con `Idempotent-Replayed: true`. **No se crea otra orden.** |
| Misma clave mientras el primero sigue en proceso | 409 |
| Misma clave con otro cuerpo u otra ruta | 422 |
| El primero terminó en 5xx | La clave se libera y se puede reintentar con la misma |

- **Reserva atómica:** se hace con `putIfAbsent`. Si llegan dos pedidos con la misma clave a la vez (doble clic), solo uno pasa. Hay un test con 64 hilos que lo comprueba.
- **Por usuario:** la clave se combina con el usuario, así que la de otro usuario no choca con la propia.
- **Huella:** se calcula con SHA-256 sobre método, ruta y cuerpo.
- **Vencimiento:** las claves vencen a las 24 h, y una reserva que quedó sin terminar, a los 2 min. Se guardan como máximo 10.000.
- **Navegador:** `dashboard.js` y `operaciones.js` generan la clave con `crypto.randomUUID` y la reusan mientras el pedido no tenga respuesta. Si se corta la red y el usuario reintenta la misma operación, el servidor devuelve la orden ya creada en lugar de duplicarla.
- **Límite:** el registro vive en memoria, lo que alcanza para un único WildFly. Con varias instancias habría que guardarlo en la base o en un caché compartido.

Esto complementa la idempotencia que ya había en el consumidor de la cola: un mensaje repetido no vuelve a ejecutar una orden que ya no está PENDIENTE.

## ETag y 304 Not Modified

Toda consulta `GET` de `/api` que responde 200 lleva tres encabezados:

- un `ETag` débil (`W/"…"`, un resumen SHA-256 del JSON);
- `Cache-Control: private, no-cache`, que obliga a revalidar siempre y evita que un proxy compartido guarde datos de un usuario;
- `Vary: Cookie`.

Si el cliente manda `If-None-Match` con ese ETag y el contenido no cambió, la respuesta es **304 sin cuerpo**. `If-None-Match` acepta `*`, listas separadas por coma y etiquetas con o sin `W/`.

El navegador revalida solo: el JavaScript no manda `If-None-Match` a mano, y `fetch` recibe el 200 con el cuerpo que ya tenía en caché. Lo hace `FiltroEtag`.

## 429 Too Many Requests

`FiltroLimitePedidos` implementa un "balde de fichas" por clave. Permite ráfagas cortas, como abrir el dashboard, y frena el abuso. Responde 429 con `Retry-After` (en segundos).

| Dónde | Límite por defecto | Clave | Propiedad para cambiarlo |
|---|---|---|---|
| `POST /login` | 5 intentos/min | email | `inversorar.limite.login.email` |
| `POST /login` | 20 intentos/min | IP | `inversorar.limite.login.ip` |
| `/api` consultas (GET) | 300/min | usuario | `inversorar.limite.lecturas` |
| `/api` operaciones (POST/PUT/DELETE) | 30/min | usuario | `inversorar.limite.escrituras` |
| `POST /api/asistente` (preguntas a la IA) | 10/min | usuario | `inversorar.limite.asistente` |

El dashboard hace unos 10 pedidos al abrirse y 2 cada 3 s mientras hay órdenes pendientes, unos 50 por minuto por pestaña. El límite de consultas deja margen para varias pestañas.

Para la prueba de carga se suben los límites al arrancar WildFly, entre comillas en PowerShell: `"-Dinversorar.limite.lecturas=100000"`.

## Cómo verificarlo

- **Tests automáticos (`ApiRestTest`):**
  - formato de los problemas;
  - idempotencia: nueva, repetida, en curso, cuerpo distinto, por usuario, liberación ante 5xx, vencimiento, 64 pedidos simultáneos y tope de tamaño;
  - comparación de ETag;
  - límite, recarga de fichas y tope de claves.
- **Sin sesión, con curl** (verificado en WildFly el 27/11):
  - `GET /api/ordenes` → 401 `application/problem+json`;
  - 7 intentos de login fallidos seguidos con la misma cuenta → los 5 primeros 401, después **429 con `Retry-After`**.
- **Con sesión:** iniciar sesión en el dashboard, abrir la consola del navegador (F12), pegar [`scripts/probar-api-rest.js`](../scripts/probar-api-rest.js) y Enter. Prueba:
  - 400 de regla de negocio, 404 de ruta y JSON roto en Problem Details;
  - ETag → 304;
  - misma Idempotency-Key → misma orden;
  - otro cuerpo → 422;
  - doble clic simultáneo → una sola orden;
  - ráfaga → 429.

  Crea una o dos órdenes chicas de BTCUSDT, que después se pueden eliminar desde Transacciones.

  Verificado en WildFly el 27/11: las 8 pruebas dieron OK. La compra repetida devolvió la misma orden (#43, con `Idempotent-Replayed: true`), el doble clic dio 202 + 409 y el 429 llegó en el pedido 24 con `Retry-After` de 2 s.
