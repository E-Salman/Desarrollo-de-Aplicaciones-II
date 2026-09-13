# InversorAR - Compra, Venta, Portfolio y Registro/Login

Rama `mysql_componente`, creada desde `integracion_entrega`. Los cuatro componentes acordados son **Compra, Venta, Portfolio y Registro/Login**. Registro y Login forman un único componente, a cargo de Zoe. Compra es `@Stateless`; Portfolio es `@Stateful` y reúne la consulta de inversiones persistentes y una simulación temporal de distribución. La simulación es una funcionalidad de Portfolio, no un componente adicional. Venta (Joaco) y Registro/Login (Zoe) siguen pendientes de integración en esta rama. La consigna exige al menos tres componentes; el proyecto define cuatro.

Portfolio conserva capital y porcentajes por sesión HTTP. Relee las operaciones en cada consulta: una compra actualiza las posiciones sin perder la simulación. `PortfolioSesion` mantiene una única referencia EJB por sesión y rechaza cambios de identidad sobre la misma cookie. No inyectar PortfolioService directamente en cada recurso REST: utilizar PortfolioSesion.

## Componentes y patrones

Stateful y Stateless son tipos de EJB; Strategy es un patrón de diseño. No son alternativas entre sí.

| Componente | Tipo y responsabilidad | Patrones |
|---|---|---|
| Compra | CompraServiceBean @Stateless: catálogo, validación y registro de compras | Repository/DAO |
| Portfolio | PortfolioServiceBean @Stateful: consultas persistentes y planificación temporal por sesión | Service Facade, Repository/DAO y Strategy de cotización |
| Venta, fuera de esta rama | En joaco (88f85f5), OperacionServiceBean @Stateless delega las reglas en VentaStrategy; pendiente de integración | Repository/DAO y Strategy de operación |
| Registro/Login, Zoe | Alta de usuarios, autenticación y cierre de sesión; pendiente de integrar y revisar su implementación | Tipo EJB y patrones pendientes de verificar con Zoe |

[Texto breve para el equipo](docs/RESUMEN_COMPONENTES.md), [decisiones técnicas](docs/TECNICO.md) y [evidencia real de ciclo de vida](docs/EVIDENCIA.md). Compra y Portfolio tienen callbacks @PostConstruct/@PreDestroy; Portfolio también tiene @Remove. Los registros del contenedor documentan su ejecución.

## MySQL en esta rama

El perfil MySQL utiliza el catálogo recibido del equipo y guarda las compras en la misma base. Las tablas `portfolios` y `operaciones` se crean mediante `config/mysql-compras.sql`. Las órdenes se crean con `config/mysql-ordenes.sql` y se vinculan mediante `config/mysql-vincular-ordenes.sql`. Cada compra guarda una orden, su detalle y un movimiento enlazado en la misma transacción. No agrega instrumentos ni cotizaciones de demostración. Comprar abre el formulario compacto con selección por tipo e instrumento; el catálogo y los históricos son una consulta opcional. Criptomonedas tiene los datos recibidos; Acciones, Bonos, CEDEARs y Monedas permanecen visibles y vacíos. Los importes se agrupan por moneda.

Estructura completa para el equipo: [diccionario de las 11 tablas y sus relaciones](docs/MYSQL_ESQUEMA.md) y [SQL de estructura sin datos](config/mysql-esquema.sql). El primero indica el orden de migraciones, la vinculación de usuarios y cómo migrar compras anteriores sin duplicarlas.

Configuración, permisos mínimos, cambios de API y evidencia: [MySQL](docs/MYSQL.md). El precio de una compra se expresa en la moneda del par; BTCUSDT cotiza en USDT, no se presupone USD. Las compras son registros, sin saldo de billetera ni ejecución en mercados.

## Ejecutar

Requiere JDK 17, Maven 3.9 y WildFly compatible con Jakarta EE 10. Verificación realizada con WildFly 30.0.1.Final.

```powershell
mvn test
mvn package
```

El WAR normal **no crea ni borra tablas**. Para una instalación nueva de demostración, con H2 ExampleDS vacío y aislado:

```powershell
mvn -Pdemo-init package
Copy-Item target/inversorar.war "$env:WILDFLY_HOME/standalone/deployments/"
```

Antes de entrar, ejecutar `bin/add-user.bat` de WildFly de forma interactiva: elegir **Application User**, realm `ApplicationRealm`, elegir usuario/contraseña y asignar grupo **USUARIO**. Abrir `/inversorar/dashboard` y autenticarse en el diálogo del navegador. No se agregaron pantallas de login ni contraseñas al repositorio. Se requiere HTTPS fuera de localhost.

El perfil `demo-init` usa `create`, solo para una BD vacía. No es una migración ni debe desplegarse sobre datos del equipo. H2 ExampleDS es una demostración en memoria; MySQL se prepara mediante `mvn -Pmysql package` y un datasource externo. Ver [integración y BD](docs/INTEGRACION.md).

## Funcionalidades y API

Todas las rutas siguientes llevan prefijo `/inversorar/api` y requieren `USUARIO`:

| Método y ruta | Resultado |
|---|---|
| GET `/instrumentos` | Catálogo disponible |
| GET `/catalogo?q=BTCUSDT&pagina=0` | Catálogo MySQL, 50 resultados por página |
| GET `/catalogo/{id}/precios?limite=90` | Precios históricos, hasta 365 registros |
| POST `/compras` | Registra compra; 201 con resumen actualizado |
| GET `/portfolio/resumen` | Capital invertido, patrimonio, ganancia, rendimiento porcentual y posiciones |
| GET `/portfolio/posiciones` | Cantidad, precio promedio, cotización, costo, valor y rendimiento por instrumento |
| GET `/portfolio/simulacion` | Estado de la simulación de la sesión HTTP |
| PUT `/portfolio/simulacion/capital` | `{"valor":1000000}` |
| PUT `/portfolio/simulacion/porcentajes/ACCION` | `{"valor":40}`; tipos: ACCION, BONO, CRIPTO, CEDEAR, MONEDA |
| DELETE `/portfolio/simulacion` | Reinicia sólo el plan temporal; 204 |
| DELETE `/portfolio/sesion` | Invalida la sesión HTTP y retira el EJB Portfolio mediante @Remove; 204 |

Ejemplo MySQL: `{"ticker":"BTCUSDT","cantidad":"0.002","precioUnitario":"70000","fecha":"2026-09-01"}`. AAPL corresponde al catálogo ficticio del perfil H2. La fecha debe ser válida y no futura. Errores de negocio: 400. Sin autenticación: 401. Sin rol: 403.

El cliente no envía un portfolioId. La antigua ruta `/portfolios/1/resumen` se retiró para impedir consultas a carteras ajenas. El dashboard se actualizó al contrato nuevo. La planificación requiere conservar JSESSIONID y no persiste inversiones. Las rutas anteriores `/api/simulador` se conservan como alias HTTP del mismo Portfolio, sin otro EJB. Su DELETE ahora reinicia el plan sin invalidar toda la sesión; para cerrar la conversación usar DELETE `/api/portfolio/sesion`. El cierre no borra operaciones de la base.

## Documentación y entrega

- [Decisiones técnicas actualizadas](docs/TECNICO.md). `docs/TECNICO.pdf` corresponde a la versión anterior; no refleja este refactor ni se presenta como documento vigente de entrega.
- [Arquitectura](docs/ARQUITECTURA.md), [requisitos y checklist](docs/REQUISITOS.md), [tecnologías](docs/TECNOLOGIAS.md).
- [Integración con Registro/Login, BD y Venta](docs/INTEGRACION.md).
- [Pruebas y demo](docs/PRUEBAS.md).

Patrones presentes: Repository/DAO, Service Facade y Strategy de cotización. Compra usa EJB stateless y transacción REQUIRED; Portfolio usa EJB stateful con callbacks visibles y cierre explícito; sus consultas tienen transacción REQUIRED y la planificación NOT_SUPPORTED.

**Entrega general pendiente:** integrar y demostrar Venta, integrar el login del equipo y acordar con el equipo la relación definitiva con usuarios. MySQL ya se usa para el catálogo recibido y el guardado de compras; ver [configuración y pruebas](docs/MYSQL.md). No se declara terminada Venta ni se cuenta la simulación como componente adicional.
