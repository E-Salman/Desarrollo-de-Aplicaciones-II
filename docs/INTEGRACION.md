# Integración con trabajo de otras ramas

Se inspeccionaron las ramas publicadas: `primeros_pasos` y `joaco` apuntaban a `ac4a73f`. El usuario indicó que Login y BD están siendo desarrollados por compañeros y podrían no estar publicados. El refactor previo se realizó sobre integracion_entrega, que ya contiene Compra y Portfolio. No se incorporó ninguna rama adicional ni se diseñaron pantallas de login, tablas de usuarios, importadores de Binance o Venta.

## Login

`PortfolioActual` es la frontera de identidad. La implementación de demostración toma `SessionContext.getCallerPrincipal()` y verifica el rol `USUARIO`. El principal debe ser una identidad estable, no un nombre editable. Un portfolio se aprovisiona en el primer acceso y se reutiliza por propietario. No existe fallback anónimo ni portfolio fijo.

TODO de integración: conectar el login a la identidad del contenedor (Elytron/Jakarta Security), mapear su rol al nombre `USUARIO` y mantener el vínculo portfolios.usuario_id con el usuario registrado. Una pantalla con una variable de sesión propia no satisface por sí sola la seguridad EJB. `web.xml` usa BASIC y `jboss-web.xml` usa el dominio estándar `other`; al integrar FORM u otro mecanismo del equipo, sustituir solamente la configuración de autenticación conservando las restricciones de rol.

`Portfolio.propietario` conserva el principal; `usuario_id` agrega la relación nullable/unique hacia usuarios. Solo las carteras creadas por el resolver reciben propietario. La FK a usuarios se agregó con mysql-vincular-ordenes.sql; el login debe completar ese enlace de forma explícita. Las carteras antiguas sin dueño deben asignarse mediante una migración revisada, nunca por orden o ID 1. En dos primeros accesos concurrentes la restricción única evita duplicar dueño, pero uno puede fallar: el alta definitiva debe hacerse junto al registro del usuario. Reintentar la consulta si ocurre en la demo.

## Portfolio conversacional

PortfolioService ahora es stateful. En presentación, inyectar PortfolioSesion y obtener servicio(principal), conservando la cookie JSESSIONID. No inyectar una nueva referencia a PortfolioService en cada petición ni compartirla globalmente. Las consultas de posiciones releen la BD; la simulación sólo mantiene capital y porcentajes temporales.

DELETE /api/portfolio/simulacion reinicia el plan sin cerrar el login. DELETE /api/portfolio/sesion invalida la sesión HTTP completa y libera el EJB con @Remove. El login del equipo debe invalidar la sesión al salir o cambiar de cuenta para impedir reutilizar una conversación de otra identidad. Todo acceso sigue requiriendo USUARIO. /api/simulador permanece como alias de compatibilidad, no como componente separado.

## Venta

Durante la revisión final se publicó `joaco` en `88f85f5`. Esa rama agrupa Compra y Venta en `OperacionServiceBean @Stateless`, con CompraStrategy y VentaStrategy; también calcula el resumen y todavía usa el portfolio demo 1. No se incorporó a integracion_entrega. Para mantener los tres componentes acordados, revisar esa separación al integrar: conservar Compra, dejar la consolidación y planificación en Portfolio y ubicar la venta en su componente, reutilizando las reglas del compañero. No reemplazar este Portfolio stateful ni su resolución por identidad con el resumen antiguo.

Reutilizar PortfolioActual, InstrumentoRepository y OperacionRepository. Acordar tipo VENTA, stock disponible y política de costo promedio móvil; ordenar cronológicamente por fecha e ID para reducir costo al vender. La venta debe impedir saldo negativo dentro de una transacción y resolver la concurrencia con bloqueo/versionado. El resumen actual rechaza tipos no soportados; no suma una venta como si fuera compra. Definir ganancia realizada separada de la ganancia no realizada actual. No se introdujeron métodos de venta ni nuevas reglas contables sin el componente del compañero.

## MySQL

En `mysql_componente` se integró el esquema recibido del equipo: las siete tablas originales se conservan y se agregan `portfolios` y `operaciones` para persistir compras. El perfil MySQL adapta `simbolo` a `ticker` mediante XML de JPA, consulta el último cierre en `precios` y agrupa los totales por moneda. H2 se mantiene como perfil de desarrollo independiente.

Ver [configuración reproducible, permisos y pruebas](MYSQL.md). No usar la carga de demostración ni reimportar el dump sobre una base con datos. La demo local está vinculada con un registro de usuario de autenticación externa; no se implementaron contraseñas ni pantallas de login del compañero. Esa integración sigue pendiente. Venta debe respetar las monedas y los diez decimales de las operaciones.

## Contrato de órdenes

Compra crea ordenes, orden_detalles y operaciones dentro de REQUIRED; OrdenRepository exige MANDATORY. Cada operación apunta a un detalle único, y el usuario de la orden proviene de portfolios.usuario_id. Las compras anteriores se migran sin duplicar movimientos mediante scripts/migrar_compras_a_ordenes.py. Venta debe conservar esta asociación y definir su tipo de movimiento; no registrar por separado otra inversión por cada detalle.

El esquema compartido, sus once tablas, trece FK y pasos de instalación están en MYSQL_ESQUEMA.md y config/mysql-esquema.sql.
