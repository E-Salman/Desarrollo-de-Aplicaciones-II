# Integración con trabajo de otras ramas

Se inspeccionaron las ramas publicadas: `primeros_pasos` y `joaco` apuntaban a `ac4a73f`. El usuario indicó que Login y BD están siendo desarrollados por compañeros y podrían no estar publicados. No se incorporó ninguna rama ni se diseñaron pantallas de login, tablas de usuarios, importadores de Binance o Venta.

## Login

`PortfolioActual` es la frontera de identidad. La implementación de demostración toma `SessionContext.getCallerPrincipal()` y verifica el rol `USUARIO`. El principal debe ser una identidad estable, no un nombre editable. Un portfolio se aprovisiona en el primer acceso y se reutiliza por propietario. No existe fallback anónimo ni portfolio fijo.

TODO de integración: conectar el login a la identidad del contenedor (Elytron/Jakarta Security), mapear su rol al nombre `USUARIO` y reemplazar la búsqueda por principal por la relación Usuario-Portfolio definitiva. Una pantalla con una variable de sesión propia no satisface por sí sola la seguridad EJB. `web.xml` usa BASIC y `jboss-web.xml` usa el dominio estándar `other`; al integrar FORM u otro mecanismo del equipo, sustituir solamente la configuración de autenticación conservando las restricciones de rol.

`Portfolio.propietario` es una columna puente nullable/unique para no asignar automáticamente carteras viejas a una persona. Solo las carteras creadas por el resolver reciben propietario. Coordinar esta columna con el responsable de BD; no asumir que reemplaza a su FK Usuario. Las carteras antiguas sin dueño deben asignarse mediante una migración revisada, nunca por orden o ID 1. En dos primeros accesos concurrentes la restricción única evita duplicar dueño, pero uno puede fallar: el alta definitiva debe hacerse junto al registro del usuario. Reintentar la consulta si ocurre en la demo.

## Venta

Reutilizar PortfolioActual, InstrumentoRepository y OperacionRepository. Acordar tipo VENTA, stock disponible y política de costo promedio móvil; ordenar cronológicamente por fecha e ID para reducir costo al vender. La venta debe impedir saldo negativo dentro de una transacción y resolver la concurrencia con bloqueo/versionado. El resumen actual rechaza tipos no soportados; no suma una venta como si fuera compra. Definir ganancia realizada separada de la ganancia no realizada actual. No se introdujeron métodos de venta ni nuevas reglas contables sin el componente del compañero.

## MySQL

El proyecto usa JPA/JTA con datasource administrado; el WAR no contiene credenciales ni driver. El perfil `mysql` selecciona `java:jboss/datasources/InversorARDS` y mantiene generación de esquema en `none`. El perfil predeterminado conserva ExampleDS para desarrollo; también usa `none`. No se ejecutó una migración sobre la BD del equipo.

Pasos del responsable de BD:

1. Revisar entidades y esquema final, especialmente propietario, precisión monetaria e identidades. Crear/migrar tablas mediante su script versionado.
2. Instalar MySQL Connector/J compatible en WildFly (JAR desplegado como driver, o módulo administrado). Consultar el nombre registrado del driver en `/subsystem=datasources:installed-drivers-list`.
3. Crear datasource InversorARDS con JNDI `java:jboss/datasources/InversorARDS`, driver registrado, URL `jdbc:mysql://HOST:3306/BASE` y usuario de la aplicación. Guardar contraseña en credential-store de Elytron; no en Git ni en argumentos compartidos.
4. Probar conexión con `/subsystem=datasources/data-source=InversorARDS:test-connection-in-pool` y desplegar `mvn -Pmysql package`.
5. Verificar compra, consulta por usuario y conservación de datos al redesplegar. No usar `demo-init` contra la BD compartida.

Plantilla CLI sin secretos: `config/mysql-datasource.cli`. Requiere driver y alias del credential-store previamente creados. Los nombres de variables están documentados en el archivo.

El catálogo inicial conserva los precios ilustrativos de la rama base (supuesto USD). No importa los 1.267.911 precios ni consulta Binance. Al integrar Cotizacion, implementar otra CotizacionStrategy con moneda y fecha explícitas; no mezclar monedas al sumar patrimonio.
