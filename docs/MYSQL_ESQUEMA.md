# Esquema MySQL utilizado por el equipo

**Actualización de autenticación:** el registro ahora guarda PBKDF2 real en
`usuarios.password` y vincula automáticamente `portfolios.usuario_id` al resolver
la cartera. Las notas históricas de login externo/marcadores más abajo describen
el estado anterior; ver [autenticación vigente](AUTENTICACION.md).

Estado verificado en `mysql_componente`, 13/09/2026. Son **11 tablas**. La definición SQL exacta, sin datos ni credenciales, está en [config/mysql-esquema.sql](../config/mysql-esquema.sql). Este documento se generó a partir de la estructura real de la base local y explica los acuerdos que debe conservar cada componente.

## Cómo registrar una compra

`Principal de WildFly → portfolios.usuario_id → usuarios → ordenes → orden_detalles → operaciones`

CompraServiceBean valida los datos y resuelve la cartera del usuario autenticado. OrdenRepository guarda la cabecera y el detalle; OperacionRepository guarda el movimiento enlazado. Las tres escrituras participan de la misma transacción REQUIRED; OrdenRepository exige MANDATORY para no crear una orden aislada fuera de esa transacción. Portfolio lee solamente operaciones, por lo que una compra se cuenta una vez. La orden conserva su moneda; si el catálogo pasa a indicar otra moneda, el resumen rechaza reinterpretar los importes.

## Diccionario de tablas

### `activos`

Activos o monedas que forman los pares. Se reutilizan para identificar la moneda BASE y la de COTIZACION.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `simbolo` | `varchar(30)` | No | Sin valor predeterminado |
| `nombre` | `varchar(255)` | No | Sin valor predeterminado |
| `tipo` | `varchar(20)` | No | Sin valor predeterminado |
| `coingecko_id` | `varchar(100)` | Sí | NULL |

Claves e índices: `PRIMARY` (id) — único; `simbolo` (simbolo) — único.


### `instrumentos`

Catálogo negociable. Su símbolo se expone como ticker en Java mediante orm-mysql.xml. El perfil MySQL no agrega instrumentos ficticios.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `simbolo` | `varchar(30)` | No | Sin valor predeterminado |
| `nombre` | `varchar(255)` | No | Sin valor predeterminado |
| `tipo` | `varchar(20)` | No | Sin valor predeterminado |
| `exchange` | `varchar(50)` | Sí | NULL |
| `estado` | `varchar(20)` | Sí | NULL |

Claves e índices: `PRIMARY` (id) — único; `uq_instrumento_simbolo_exchange` (simbolo, exchange) — único.


### `usuarios`

Identidad del propietario de una orden. El login definitivo del equipo debe utilizar esta tabla; la demo actual sigue autenticando en WildFly. Su registro local tiene un marcador no utilizable como contraseña de la aplicación.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `apellido` | `varchar(100)` | No | Sin valor predeterminado |
| `nombre` | `varchar(100)` | No | Sin valor predeterminado |
| `email` | `varchar(255)` | No | Sin valor predeterminado |
| `password` | `varchar(255)` | No | Sin valor predeterminado |

Claves e índices: `PRIMARY` (id) — único; `uk_usuario_email` (email) — único.


### `bonos`

Características específicas de instrumentos de tipo bono. La tabla recibida está vacía.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `fecha_emision` | `date` | Sí | NULL |
| `fecha_vencimiento` | `date` | Sí | NULL |
| `tasa_cupon` | `decimal(10,6)` | Sí | NULL |
| `valor_nominal` | `decimal(30,10)` | Sí | NULL |
| `moneda_nominal_id` | `bigint` | Sí | NULL |

Claves e índices: `fk_bonos_moneda` (moneda_nominal_id); `PRIMARY` (instrumento_id) — único.

- `instrumento_id` → `instrumentos.id`; al borrar: `CASCADE`.
- `moneda_nominal_id` → `activos.id`; al borrar: `NO ACTION`.

### `cedears`

Características específicas de CEDEARs. La tabla recibida está vacía.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `ratio` | `decimal(20,10)` | Sí | NULL |

Claves e índices: `PRIMARY` (instrumento_id) — único.

- `instrumento_id` → `instrumentos.id`; al borrar: `CASCADE`.

### `instrumento_activos`

Relaciona un instrumento con sus activos y su rol. Para valorizar una compra se exige una única relación COTIZACION.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `activo_id` | `bigint` | No | Sin valor predeterminado |
| `rol` | `varchar(30)` | No | Sin valor predeterminado |

Claves e índices: `fk_instrumento_activos_activo` (activo_id); `PRIMARY` (instrumento_id, activo_id, rol) — único.

- `activo_id` → `activos.id`; al borrar: `NO ACTION`.
- `instrumento_id` → `instrumentos.id`; al borrar: `CASCADE`.

### `precios`

Histórico de apertura, máximo, mínimo, cierre y volumen. Se toma el último cierre disponible; no representa una cotización en vivo.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `fecha_hora` | `datetime` | No | Sin valor predeterminado |
| `apertura` | `decimal(30,10)` | No | Sin valor predeterminado |
| `maximo` | `decimal(30,10)` | No | Sin valor predeterminado |
| `minimo` | `decimal(30,10)` | No | Sin valor predeterminado |
| `cierre` | `decimal(30,10)` | No | Sin valor predeterminado |
| `volumen` | `decimal(40,10)` | Sí | NULL |

Claves e índices: `idx_precios_instrumento_fecha` (instrumento_id, fecha_hora); `PRIMARY` (id) — único; `uk_precio_instrumento_fecha` (instrumento_id, fecha_hora) — único.

- `instrumento_id` → `instrumentos.id`; al borrar: `CASCADE`.

### `portfolios`

Cartera persistente. propietario es el principal autenticado del contenedor; usuario_id la vincula con usuarios. Cada usuario puede tener como máximo una cartera en este modelo.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `nombre` | `varchar(255)` | No | Sin valor predeterminado |
| `propietario` | `varchar(255)` | Sí | NULL |
| `usuario_id` | `bigint` | Sí | NULL |

Claves e índices: `PRIMARY` (id) — único; `uq_portfolio_propietario` (propietario) — único; `uq_portfolio_usuario` (usuario_id) — único.

- `usuario_id` → `usuarios.id`; al borrar: `NO ACTION`.

### `ordenes`

Cabecera de la compra registrada: propietario, instante de registro, estado, moneda y total. El flujo actual registra COMPLETADA porque guarda una inversión declarada; no ejecuta una compra en un mercado externo.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `usuario_id` | `bigint` | No | Sin valor predeterminado |
| `fecha_hora` | `datetime` | No | CURRENT_TIMESTAMP |
| `estado` | `varchar(30)` | No | COMPLETADA |
| `moneda` | `varchar(10)` | No | Sin valor predeterminado |
| `total` | `decimal(30,10)` | No | Sin valor predeterminado |

Claves e índices: `fk_orden_usuario` (usuario_id); `PRIMARY` (id) — único.

- `usuario_id` → `usuarios.id`; al borrar: `NO ACTION`.

### `orden_detalles`

Instrumentos de una orden. El esquema admite varios detalles; el formulario actual crea uno por compra.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `orden_id` | `bigint` | No | Sin valor predeterminado |
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `cantidad` | `decimal(30,15)` | No | Sin valor predeterminado |
| `precio_unitario` | `decimal(30,15)` | No | Sin valor predeterminado |
| `subtotal` | `decimal(30,15)` | No | Sin valor predeterminado |

Claves e índices: `fk_detalle_instrumento` (instrumento_id); `fk_detalle_orden` (orden_id); `PRIMARY` (id) — único.

- `instrumento_id` → `instrumentos.id`; al borrar: `NO ACTION`.
- `orden_id` → `ordenes.id`; al borrar: `CASCADE`.

### `operaciones`

Movimientos que utiliza Portfolio para calcular posiciones. Cada compra nueva se enlaza a un único detalle de orden. Se mantiene para integrar Venta y no se suman nuevamente los detalles al calcular el portfolio.

| Columna | Tipo MySQL | Permite NULL | Valor inicial / generación |
|---|---|---|---|
| `id` | `bigint` | No | AUTO_INCREMENT |
| `portfolio_id` | `bigint` | No | Sin valor predeterminado |
| `instrumento_id` | `bigint` | No | Sin valor predeterminado |
| `tipo` | `varchar(20)` | No | Sin valor predeterminado |
| `cantidad` | `decimal(30,10)` | No | Sin valor predeterminado |
| `precioUnitario` | `decimal(30,10)` | No | Sin valor predeterminado |
| `total` | `decimal(30,10)` | No | Sin valor predeterminado |
| `fecha` | `date` | No | Sin valor predeterminado |
| `orden_detalle_id` | `bigint` | Sí | NULL |

Claves e índices: `fk_operaciones_instrumento` (instrumento_id); `idx_operaciones_portfolio` (portfolio_id, fecha, id); `PRIMARY` (id) — único; `uq_operacion_detalle` (orden_detalle_id) — único.

- `instrumento_id` → `instrumentos.id`; al borrar: `NO ACTION`.
- `portfolio_id` → `portfolios.id`; al borrar: `NO ACTION`.
- `orden_detalle_id` → `orden_detalles.id`; al borrar: `NO ACTION`.

## Acuerdos de integración

- **Identidad:** las peticiones de compra no aceptan usuario_id ni portfolio_id del navegador. El registro/login debe crear o vincular la cartera con el usuario correcto y conservar un principal estable. No asignar por defecto todas las cuentas al ID 1. Una cuenta sin usuario_id puede consultar su cartera, pero no generar nuevas órdenes.
- **Autenticación:** usuarios.password no es la contraseña de WildFly. El login del compañero debe definir un formato seguro de hash y su integración con el principal/rol del contenedor. El registro local de demostración representa la cuenta externa; no implementa un nuevo login ni provee una contraseña reutilizable.
- **Importes:** órdenes.total y operaciones.total usan diez decimales. Los campos del detalle admiten quince según el esquema solicitado. La API actual acepta hasta diez decimales en cantidad y precio; calcula un importe de negocio redondeado a diez que se copia a total/subtotal. No se aprovechan todavía quince decimales de entrada. Una ampliación requiere cambiar validaciones y movimientos de forma conjunta.
- **Monedas:** una orden tiene una moneda. No mezclar monedas dentro de una orden ni sumar totales de distintas monedas en Portfolio. USD, USD1, USDT, USDC y BTC son identificadores diferentes; no hay conversión implícita.
- **Fechas:** operaciones.fecha es la fecha de inversión declarada; ordenes.fecha_hora registra la creación de una orden nueva. Para migrar una compra anterior se conserva la fecha conocida a las 00:00:00, sin inventar la hora original. DATETIME no almacena zona horaria; acordar zona del servidor/JVM para nuevos despliegues.
- **Integridad:** operaciones.orden_detalle_id es único. Cada movimiento enlazado debe coincidir con su detalle en instrumento, cantidad, precio y subtotal, y el usuario de la orden debe coincidir con el del portfolio. El servicio crea estos valores juntos. El esquema no valida por sí solo todas las igualdades entre tablas: no duplicar el flujo con INSERT independientes desde otra pantalla.
- **Borrado:** se conserva ON DELETE CASCADE de ordenes a orden_detalles. Si hay una operación enlazada, su FK impide borrar el detalle y, por tanto, la orden. Las ventas deben registrarse como movimientos; no borrar la compra anterior.
- **Venta:** el componente de Joaco debe acordar cómo crear su orden/movimiento VENTA y cómo reducir cantidades y costo. No cambiar COMPRA por VENTA en un registro anterior ni sumar ordenes y operaciones como si fueran inversiones distintas.
- **Catálogo:** la base recibida tiene 1.365 instrumentos CRIPTO y 1.267.911 precios. Acciones, Bonos, CEDEARs y Monedas se muestran como secciones vacías hasta que el equipo cargue datos. No inferir la moneda de cotización únicamente cortando el texto del símbolo.

## Cómo preparar o actualizar la base

1. **Desde el dump original:** importar instrumentos.sql en una base vacía. Luego ejecutar, en orden, `config/mysql-compras.sql`, `config/mysql-ordenes.sql` y `config/mysql-vincular-ordenes.sql`. Las dos últimas migraciones son de una sola ejecución; comprobar antes si tablas/columnas ya existen.
2. **Sólo estructura desde cero:** alternativamente, ejecutar `config/mysql-esquema.sql` en una base vacía. Ya incluye las once tablas y todos los enlaces. No ejecutar después las migraciones anteriores. No incluye el catálogo ni las cotizaciones: el equipo debe cargarlos como datos, sin volver a eliminar/recrear las tablas.
3. **Base local ya preparada en esta tarea:** los tres scripts incrementales están aplicados y las compras anteriores se vincularon. No reimportar el dump sobre ella: el dump original incluye eliminación de tablas.
4. **Identidad antes de comprar:** dar de alta el usuario mediante el proceso del equipo y asociar explícitamente `portfolios.usuario_id` al usuario correcto, según el principal autenticado. El ID se obtiene de la tabla; no se hardcodea en Java. La cuenta local demo ya está asociada.
5. **Compras anteriores:** ejecutar primero el script siguiente en modo revisión; después usar `--apply`. No crea usuarios ni infiere propietarios. Si falta usuario, moneda o el tipo no es COMPRA, se detiene. Bloquea los movimientos seleccionados, crea los enlaces en una transacción y omite los ya vinculados al repetirlo.

```powershell
python scripts/migrar_compras_a_ordenes.py --defaults-file "RUTA/cliente-privado.cnf" --database instrumentos
python scripts/migrar_compras_a_ordenes.py --defaults-file "RUTA/cliente-privado.cnf" --database instrumentos --apply
```

El archivo privado usa las opciones del cliente MySQL y permanece fuera de Git. Si mysql no está en PATH, indicar `--mysql "RUTA/mysql.exe"`. Respaldar la base antes de migrar y ejecutar con una cuenta administradora.

## Permisos y configuración de la app

El datasource es `java:jboss/datasources/InversorARDS`. La cuenta de aplicación necesita SELECT sobre las tablas utilizadas, INSERT en ordenes y orden_detalles y los permisos existentes sobre portfolios/operaciones. El alta de usuarios y su vinculación quedan a cargo del login/administración; el flujo de compra no necesita modificar usuarios. No dar permisos de modificación del catálogo para registrar inversiones. Driver y credenciales pertenecen a WildFly, fuera del WAR y Git.

Compilar con `mvn -Pmysql package`. H2 sigue siendo un perfil de demostración independiente: no exige los enlaces a usuario/orden de MySQL.

## Qué compartir y cómo mantener este documento

Compartir este documento, `config/mysql-esquema.sql`, los scripts incrementales y el script de migración junto al código. El SQL de estructura no contiene compras, nombres, correos ni contraseñas del entorno. Compartir el dump de catálogo por el canal acordado por el equipo; no subir la carpeta de datos, credenciales, backups o logs a Git.

Cuando cambie una tabla, agregar una migración incremental y actualizar el diccionario y el SQL de estructura en el mismo PR. Conservar las migraciones anteriores como pasos históricos; no editar una migración ya ejecutada para hacer cambios nuevos.
