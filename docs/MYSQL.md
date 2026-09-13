# Catálogo y compras en MySQL

Trabajo de la rama `mysql_componente`, creada desde `integracion_entrega` (`09c9b09`). De los cuatro componentes acordados (Compra, Venta, Portfolio y Registro/Login), integra Compra y Portfolio. No incorpora todavía Venta de Joaco ni Registro/Login de Zoe.

## Datos y alcance

Se utiliza el archivo `instrumentos.sql` recibido del equipo. La importación contiene 491 activos, 1.365 instrumentos, 1.918 relaciones instrumento-activo y 1.267.911 precios. Las tablas `bonos`, `cedears` y `usuarios` estaban vacías en el archivo recibido. Para las órdenes se vinculó explícitamente la cuenta local de demostración a un registro de `usuarios`; ese registro no se incluye en Git. Los instrumentos cargados son pares de criptomonedas. La aplicación no agrega acciones, bonos, usuarios ni precios ficticios automáticamente.

El script `config/mysql-compras.sql` agrega solamente `portfolios` y `operaciones`, con claves foráneas hacia el catálogo existente. No modifica las siete tablas recibidas ni importa datos automáticamente. Ejecutarlo sobre la base previamente importada; si ya existen tablas con estos nombres, revisar primero su estructura: `IF NOT EXISTS` no migra tablas incompatibles.

Las compras registran inversiones declaradas por el usuario; no son órdenes reales ni descuentan saldo de una billetera. El catálogo se puede consultar completo, incluso cuando faltan datos. Para registrar una compra se exige un cierre disponible y una única moneda de cotización identificada por `instrumento_activos.rol = 'COTIZACION'`.

## Tablas de órdenes agregadas por el equipo

`config/mysql-ordenes.sql` agrega `ordenes` y `orden_detalles` con el esquema solicitado: una orden pertenece a `usuarios`, sus detalles referencian `instrumentos` y se eliminan en cascada al borrar la orden. El total de la orden conserva `DECIMAL(30,10)` y los importes/cantidades de sus detalles `DECIMAL(30,15)`. El script se ejecuta una sola vez, después de importar las tablas originales.

Estas tablas ya participan del registro de compras. `mysql-vincular-ordenes.sql` agrega `portfolios.usuario_id` y `operaciones.orden_detalle_id`, ambos con FK y unicidad. Compra guarda orden, detalle y movimiento dentro de una única transacción; Portfolio suma solamente los movimientos. El detalle guarda el mismo importe de negocio que la orden y la operación. Ver el [diccionario completo de MySQL](MYSQL_ESQUEMA.md) y el script reproducible `config/mysql-esquema.sql`.

## Configuración reproducible

1. Importar el SQL del equipo en una base MySQL 8 previamente vacía. No volver a importar sobre datos existentes: el dump original contiene instrucciones de eliminación de tablas. Conservar ese archivo fuera de Git.
2. Ejecutar `config/mysql-compras.sql` y, para las tablas de órdenes solicitadas por el equipo, `config/mysql-ordenes.sql`, seguido por `config/mysql-vincular-ordenes.sql`, en esa misma base con una cuenta administradora. Revisar primero si ya existen tablas/columnas.
3. Crear una cuenta de aplicación con contraseña elegida localmente. Otorgar `SELECT` sobre la base y `INSERT`, `UPDATE`, `DELETE` solamente sobre `portfolios` y `operaciones`. Otorgar también INSERT en `ordenes` y `orden_detalles`. La compra no necesita modificar el catálogo ni `usuarios`.
4. Instalar Connector/J en WildFly. La prueba local usa MySQL 8.1.0 y Connector/J 8.4.0, desplegado como `mysql-connector-j-8.4.0.jar`.
5. Crear el datasource `InversorARDS`, JNDI `java:jboss/datasources/InversorARDS`, JTA habilitado. La plantilla `config/mysql-datasource.cli` requiere reemplazar el nombre del driver por el registrado y disponer del credential-store y su alias. Configurar URL y usuario como propiedades del servidor; contraseña en Elytron, fuera del repositorio.
6. Probar `/subsystem=datasources/data-source=InversorARDS:test-connection-in-pool`.
7. Compilar con `mvn -Pmysql clean test package` y desplegar `target/inversorar.war` en WildFly.
8. Crear un Application User en `ApplicationRealm`, grupo `USUARIO`, y entrar a `/inversorar/dashboard`. Asociar explícitamente `portfolios.usuario_id` al registro de `usuarios` correspondiente al principal antes de comprar. La demo local ya está vinculada. `usuarios` identifica al propietario de la orden, pero la autenticación todavía pertenece a WildFly.

Ejemplo local de URL JDBC: `jdbc:mysql://127.0.0.1:3307/instrumentos?serverTimezone=UTC&sslMode=REQUIRED`. El puerto 3307 es de MySQL y no se abre con un navegador. En la instancia local preparada, la web está en `http://127.0.0.1:18080/inversorar/dashboard`.

`sslMode=REQUIRED` cifra la conexión local; una conexión remota debe validar certificado y nombre de servidor con la configuración de confianza del equipo. Las credenciales, el dump y el directorio de datos no forman parte del WAR ni del repositorio.

## Mapeo y arquitectura

- Presentación: `CatalogoResource`, `CompraResource`, dashboard JSP/JavaScript.
- Negocio: `CompraServiceBean @Stateless` lista instrumentos y consulta históricos mediante Repository; valida y registra compras bajo transacción `REQUIRED` y rol `USUARIO`. `PortfolioServiceBean @Stateful` calcula posiciones y conserva la planificación temporal por sesión.
- Persistencia: `CatalogoMercadoRepository` consulta las tablas existentes usando parámetros y paginación. `InstrumentoRepository` adapta el catálogo; `OrdenRepository` guarda orden y detalle dentro de la transacción de Compra, con atributo MANDATORY. `OperacionRepository` y `PortfolioRepository` guardan los movimientos y la cartera. Se conservan DAO/Repository y Service Facade; `CotizacionCatalogo` implementa la Strategy de cotización mediante el último cierre disponible en MySQL.
- JPA: `orm-mysql.xml` mapea `Instrumento.ticker` a `instrumentos.simbolo` y excluye `cotizacionActual` de la entidad persistente. El precio se obtiene de `precios`. Así no se agregan columnas incompatibles al catálogo del compañero.
- `mvn package` mantiene el perfil H2 para desarrollo; `orm-default.xml` conserva el mapeo original. `mvn -Pmysql package` selecciona el datasource y el mapeo MySQL. La generación automática de esquema está en `none`; `DatosIniciales` no inserta datos de demo en este perfil.

La moneda y la fecha son metadatos de consulta, no columnas añadidas a `instrumentos`. Los cálculos monetarios usan `BigDecimal`; cantidades y precios de compra admiten hasta diez decimales, con control de magnitud y rechazo de totales que redondean a cero. La fecha histórica se devuelve como texto de la base, evitando convertir un `DATETIME` sin zona horaria a la zona de la JVM.

## API y pantalla

Prefijo `/inversorar/api`, autenticación y rol `USUARIO`:

| Método y ruta | Uso |
|---|---|
| GET `/catalogo/configuracion` | Indica si está activo el perfil MySQL |
| GET `/catalogo?q=BTCUSDT&pagina=0` | Búsqueda por símbolo/nombre, 50 instrumentos por página |
| GET `/catalogo/5/precios?limite=90` | Hasta 365 registros históricos, orden descendente; el ID debe obtenerse del catálogo |
| GET `/instrumentos` | Catálogo para el selector de compra, con moneda y fecha del cierre |
| POST `/compras` | Compra persistida; respuesta 201 con resumen actualizado |
| GET `/portfolio/resumen` | Posiciones y totales por moneda |

Ejemplo de compra: `{"ticker":"BTCUSDT","cantidad":"0.002","precioUnitario":"70000","fecha":"2026-09-01"}`. Los valores también pueden enviarse como números JSON. El precio se expresa en la moneda del par: USDT para BTCUSDT, BTC para ETHBTC. No hay conversión implícita a USD.

Cada posición expone `moneda` y `fechaCotizacion`. `totalesPorMoneda` agrupa capital invertido, patrimonio, ganancia/pérdida y rendimiento. Si toda la cartera tiene una sola moneda, se mantienen los totales generales y se indica `moneda`. Si contiene varias monedas, los campos generales quedan nulos (JSON-B puede omitirlos); el cliente debe mostrar los subtotales. Nunca sumar BTC, USDT y USD sin una conversión explícita.

El dashboard muestra el resumen y las inversiones. Al presionar Comprar se abre el formulario compacto con selección por tipo e instrumento. El catálogo con búsqueda, paginación e históricos queda como consulta opcional dentro del formulario. Los tipos sin instrumentos cargados se indican explícitamente. Los símbolos sin precio o moneda aparecen con compra deshabilitada. El servicio también aplica esa validación. Las referencias de precios son históricas y muestran su fecha; no representan una integración en tiempo real con Binance.

## Integración pendiente y preservación de datos

Login debe conservar una identidad estable del contenedor y el rol `USUARIO`; `Portfolio.propietario` identifica al principal y `usuario_id` ya es una FK hacia `usuarios`. El alta definitiva debe crear ambas identidades y vincular la cartera, sin asignar el usuario de demostración a otras cuentas. Venta debe reutilizar esa identidad, las operaciones y su precisión decimal; el Portfolio actual aún rechaza operaciones VENTA hasta integrar sus reglas de costo. No agregar directamente el resumen antiguo de la rama del compañero.

Las compras ficticias de la demo H2 no se migran automáticamente al nuevo catálogo: sus identificadores y símbolos pueden representar otros instrumentos. La ejecución local conserva una copia del WAR anterior y del resumen consultado antes del cambio, fuera de Git. Las nuevas compras quedan en el directorio persistente de MySQL, no en H2. Reiniciar el servidor o cerrar una sesión no debe eliminar esas operaciones.

## Verificación realizada

- 21 pruebas unitarias: incluye precios con diez decimales, separación de monedas y rechazo de compras sin datos suficientes.
- 38 comprobaciones HTTP/SQL en WildFly con MySQL: catálogo completo sin duplicados, históricos, fechas, autenticación, rol incorrecto, compra, promedio ponderado, validaciones, aislamiento entre usuarios y persistencia después de cerrar la sesión/EJB.
- Prueba de navegador: buscar BTCUSDT, abrir historial, comprar, recargar y verificar la cartera, sin errores JavaScript. Revisión de pantalla de escritorio y móvil.
- Verificación adicional de persistencia después de redesplegar el WAR. Perfil H2 comprobado por separado con sus 25 instrumentos de demostración.
- Verificación directa en MySQL de las operaciones y de los conteos originales. Los registros de prueba pertenecen a identidades aisladas y se retiran al finalizar las pruebas locales.

Falta integrar y probar Venta (Joaco) y el componente Registro/Login (Zoe) en esta configuración. Esta rama no declara completa la entrega general.

## Órdenes y migración de compras anteriores

Se verificaron 20 casos específicos HTTP/SQL, incluyendo un fallo intencional al insertar el movimiento: JTA revirtió también la cabecera y el detalle. Los enlaces se validaron directamente en MySQL. La migración `scripts/migrar_compras_a_ordenes.py` conserva cantidades, precios, importes y fechas; al repetirla no crea duplicados. Las compras existentes de la demo están vinculadas y el resumen conserva sus importes.

La estructura SQL fue probada en una base temporal vacía: 11 tablas y 13 claves foráneas, sin datos. Detalles de instalación y acuerdos del equipo en MYSQL_ESQUEMA.md.
