# Segunda entrega parcial del 16/11

La consigna de exposición del 16/11 pide demostrar en vivo las historias comprometidas en el diseño. El TP y el anexo aportan los requisitos de capas, ciclo de vida, patrones, documentación y seguimiento. La demo propuesta toma la orden de inversión del diseño asíncrono de InversorAR.

| Criterio | Evidencia y verificación |
|---|---|
| Componentes desplegados y flujo de punta a punta | Compra, Venta, Órdenes y Portfolio en el WAR. Compra y venta verificadas por HTTP, Artemis y MySQL. |
| Stateful y stateless | Portfolio por sesión HTTP. Compra y Venta stateless. Callbacks registrados por WildFly. |
| State en la entidad central | `Orden` delega en `EstadoOrden`. Cancelar, reintentar o descartar una orden completada devuelve 409. |
| Patrones y ADR | [Decisiones](adr/). State, Service Facade, Repository, Strategy, cola JMS y JWT. |
| JWT y control por rol | Cookie JWT de 15 minutos. `web.xml` y `@RolesAllowed("USUARIO")`. Pruebas unitarias de firma y expiración y smoke HTTP de autenticación y aislamiento. |
| Mensajería | Artemis en WildFly Full. El log registra el envío, la recepción por el MDB y el cambio de estado de compra y venta. |
| Tests unitarios | `mvn -Pmysql clean package` ejecuta JUnit. El smoke usa el WAR desplegado. |
| Jira al día | Pendiente de verificar en el tablero del equipo. Registrar o vincular tareas y evidencias con los identificadores reales de Jira. |
| Feedback del diseño | Pendiente de cotejar con la devolución docente y las historias aprobadas. |

## Preparar el entorno

1. Usar WildFly Full con el dominio de seguridad, datasource y broker que describe el [README](../README.md). En el entorno local de DDAII se puede usar `Iniciar-InversorAR.bat`.
2. Tras aplicar el esquema y vínculos de órdenes, ejecutar [mysql-ordenes-estado.sql](../config/mysql-ordenes-estado.sql) en la base de demo. El script es repetible. La cuenta del datasource necesita los permisos de consulta y escritura de órdenes, detalles y movimientos.
3. Compilar y desplegar. Esperar el marcador `inversorar.war.deployed`.

```powershell
mvn -Pmysql clean package
Copy-Item target/inversorar.war "$env:WILDFLY_HOME/standalone/deployments/"
node scripts/smoke_segunda_entrega.mjs http://127.0.0.1:8080/inversorar
node scripts/smoke_simulador.mjs http://127.0.0.1:8080/inversorar
```

Ejecutar los smoke en una base de demostración. Crean cuentas distintas y el smoke de entrega registra dos movimientos. El ticker predeterminado es BTCUSDT y se puede pasar un ticker alternativo como segundo argumento. Usar Node.js 20 o superior.

## Resultado local

El 15/11 se ejecutó el paquete MySQL con 70 pruebas unitarias, sin fallas ni errores. Los smoke de entrega y simulador pasaron sobre el WAR de WildFly 41, Java 21 y MySQL local. En la verificación posterior las órdenes 18 y 19 completaron el flujo por Artemis. El smoke también verificó 403 frente a una mutación iniciada desde otro origen del mismo sitio.

Se verificó en Chrome a 1360 y 390 px el alta, login, compra, venta, historial, navegación y logout. El escenario de navegador bloqueó el CDN del gráfico para comprobar que la cartera y la demo siguen funcionando con la leyenda de distribución.

## Guion de exposición

1. Iniciar sesión con una cuenta de demostración. Mostrar que la API sin sesión autenticada responde 401.
2. Comprar 0,002 BTCUSDT. Mostrar el 202 en la pestaña Network, el envío y consumo en el log y el estado COMPLETADA en Transacciones.
3. Vender 0,001 BTCUSDT después de que la compra complete. Mostrar la tenencia restante de 0,001 en la cartera.
4. Mostrar la salida del smoke para los tres 409 y el 404 al intentar acceder con otra cuenta. El smoke invoca la API incluso cuando la interfaz ya ocultó los botones que no corresponden al estado.
5. Abrir el simulador en dos sesiones. Mostrar que conserva el plan de cada sesión y se reinicia al volver a iniciar sesión. Relacionarlo con el callback de creación y destrucción de Portfolio en el log.

## Evidencia para vincular en Jira

- Seguridad web → [ADR 006](adr/006-jwt-web.md), `TokensAccesoTest` y los casos HTTP de JWT.
- Ciclo de órdenes → [ADR 001](adr/001-state-orden.md), `OrdenTest`, `OrdenServiceTest` y los tres 409.
- Flujo asincrónico → [ADR 005](adr/005-jms-ordenes.md), [diseño del equipo](Diseno_Flujo_Asincronico.docx) y los IDs que imprime el smoke.
- Documentación y demo → [documento técnico](TECNICO.pdf), este guion y devolución pendiente de cotejar.

### Verificación de autorización por rol

En un WAR temporal, solo el IdentityStore asignó LECTOR a una cuenta de prueba. Se conservaron el login, la firma y validación del JWT, las restricciones declarativas y los recursos de la aplicación. Se verificó que esa cuenta iniciara sesión y recibiera un JWT con LECTOR antes de probar las rutas.

| Petición | Sin sesión | JWT LECTOR | JWT USUARIO |
|---|---:|---:|---:|
| GET /api/ordenes | 401 | 403 | 200 |
| GET /dashboard | | 403 | 200 |
| POST /api/compras con cuerpo vacío | | 403 | 400 |
| POST /api/ventas con cuerpo vacío | | 403 | 400 |

El 400 de USUARIO confirma que la petición autorizada alcanzó la validación de negocio. El WAR temporal se retiró y se verificó que el despliegue principal siguiera operativo. El alta web habitual asigna USUARIO.

Los criterios de seguridad de la Clase 11 orientan las verificaciones de esta entrega. La autenticación no incluye renovación automática del JWT ni limitación de intentos de login.
