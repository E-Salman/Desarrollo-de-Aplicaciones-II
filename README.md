# InversorAR — Compra, Venta y Portfolio de Instrumentos

Componentes Jakarta EE 10 implementados en capas: **Compra** y **Venta** (EJB `@Stateless`) registran operaciones; **Portfolio** (EJB `@Stateful`, uno por sesión HTTP vía `PortfolioSesion`) las consolida y además conserva una planificación/simulación de distribución de capital mientras dura la sesión. Un perfil opcional de **MySQL** reemplaza el catálogo de demostración por un catálogo real con precios históricos y agrega el registro transaccional de órdenes de compra.

## Documentación

- [Tecnologías y versiones](docs/TECNOLOGIAS.md)
- [Arquitectura en capas](docs/ARQUITECTURA.md)
- [Requisitos y checklist](docs/REQUISITOS.md)
- [Funcionamiento de la venta](docs/VENTA.md)
- [Documento técnico](docs/TECNICO.md) (el PDF en `docs/TECNICO.pdf` quedó desactualizado; no se lo considera vigente)
- [Integración con Login, BD y otras ramas](docs/INTEGRACION.md)
- [Esquema MySQL completo](docs/MYSQL_ESQUEMA.md) y [configuración/pruebas MySQL](docs/MYSQL.md)
- [Pruebas y demo](docs/PRUEBAS.md)

## Arquitectura

`JSP/Servlet + JAX-RS` → `CompraService` / `VentaService` (`@Stateless`, registran operaciones) y `PortfolioService` (`@Stateful` por sesión HTTP vía `PortfolioSesion`, Service Facade que consolida con `CotizacionStrategy`) → `Repositories JPA` → `H2 ExampleDS` / `MySQL`.

La pantalla se abre en `/inversorar/dashboard`. Todas las rutas siguientes llevan prefijo `/inversorar/api` y requieren rol `USUARIO` (BASIC auth):

| Método y ruta | Resultado |
|---|---|
| GET `/instrumentos` | Catálogo disponible (demo H2, o vista rápida del catálogo MySQL) |
| GET `/catalogo?q=BTCUSDT&pagina=0` | Catálogo MySQL completo con búsqueda, 50 resultados por página |
| GET `/catalogo/{id}/precios?limite=90` | Precios históricos del instrumento, hasta 365 registros |
| POST `/compras` | Registra compra; 201 con resumen actualizado |
| POST `/ventas` | Registra venta; 201 con resumen actualizado |
| GET `/portfolio/resumen` | Capital invertido, patrimonio, ganancia (realizada + no realizada) y posiciones |
| GET `/portfolio/posiciones` | Cantidad, precio promedio, cotización, costo, valor y rendimiento por instrumento |
| GET `/portfolio/simulacion` | Estado de la planificación de la sesión HTTP |
| PUT `/portfolio/simulacion/capital` | `{"valor":1000000}` |
| PUT `/portfolio/simulacion/porcentajes/{tipo}` | `{"valor":40}`; tipos: ACCION, BONO, CRIPTO, CEDEAR, MONEDA |
| DELETE `/portfolio/simulacion` | Reinicia solo el plan temporal; 204 |
| DELETE `/portfolio/sesion` | Invalida la sesión HTTP y retira el EJB Portfolio mediante `@Remove`; 204 |

`/api/simulador/*` se mantiene como alias deprecado de `/api/portfolio/simulacion/*` (misma conversación, sin otro EJB).

La compra y la venta reciben `ticker`, `cantidad`, `precioUnitario` y `fecha`. El servidor valida los valores, calcula el total y, en una venta, además valida que no se supere la cantidad poseída del instrumento. El cliente no envía un portfolioId: la cartera se resuelve por identidad autenticada (`PortfolioActual` para persistir operaciones, `PortfolioSesion` para la conversación de Portfolio).

El resumen del portfolio consolida compras y ventas por ticker a costo promedio ponderado (una venta parcial reduce cantidad e invertido proporcionalmente, dejando `rendimiento` como la ganancia potencial sobre lo que sigue en cartera). La ganancia ya realizada en ventas se acumula aparte (`gananciaRealizada`) y se suma en el KPI `gananciaTotal` del portfolio, sin perderse cuando una posición se vende por completo. Con instrumentos en más de una moneda, los totales únicos quedan `null` y se exponen agrupados en `totalesPorMoneda` para no sumar monedas distintas. Ver [detalle y ejemplo numérico](docs/VENTA.md).

## Componentes y patrones

Stateful y Stateless son tipos de EJB; Strategy y Service Facade son patrones de diseño. No son alternativas entre sí.

| Componente | Tipo y responsabilidad | Patrones |
|---|---|---|
| Compra | `CompraServiceBean` `@Stateless`: catálogo, validación y registro de compras (y de la orden vinculada en modo MySQL) | Repository/DAO |
| Venta | `VentaServiceBean` `@Stateless`: valida cantidad poseída y registra la venta | Repository/DAO |
| Portfolio | `PortfolioServiceBean` `@Stateful`: consolida compras y ventas (costo promedio ponderado, ganancia realizada, multi-moneda) y guarda la planificación temporal por sesión | Service Facade, Repository/DAO y Strategy de cotización |

[Texto breve para el equipo](docs/RESUMEN_COMPONENTES.md), [decisiones técnicas](docs/TECNICO.md) y [evidencia real de ciclo de vida](docs/EVIDENCIA.md). Compra, Venta y Portfolio tienen callbacks `@PostConstruct`/`@PreDestroy`; Portfolio también tiene `@Remove`. Los registros del contenedor documentan su ejecución.

## MySQL

El perfil `mysql` reemplaza el catálogo de demostración por el catálogo real recibido del equipo (`CatalogoMercadoRepository`, tablas `instrumentos`/`precios`/`activos`) y guarda cada compra como una `Orden`/`OrdenDetalle` vinculada a la `Operacion`, en la misma transacción. Las tablas `portfolios` y `operaciones` se crean mediante `config/mysql-compras.sql`; las órdenes con `config/mysql-ordenes.sql`, vinculadas mediante `config/mysql-vincular-ordenes.sql`. No se agregan instrumentos ni cotizaciones de demostración en este perfil. El precio de una compra se expresa en la moneda del par (p. ej. BTCUSDT cotiza en USDT, no se presupone USD); los importes del portfolio se agrupan por moneda.

Estructura completa para el equipo: [diccionario de las 11 tablas y sus relaciones](docs/MYSQL_ESQUEMA.md) y [SQL de estructura sin datos](config/mysql-esquema.sql) (indica orden de migraciones, vinculación de usuarios y cómo migrar compras anteriores sin duplicarlas). Configuración, permisos mínimos, cambios de API y evidencia: [MySQL](docs/MYSQL.md).

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

## Patrones y componentes con ciclo de vida

Patrones presentes: Repository/DAO, Service Facade (`PortfolioService`) y Strategy (`CotizacionStrategy` de cotización). `CompraServiceBean` y `VentaServiceBean` son EJB stateless con transacción `REQUIRED`, `@RolesAllowed("USUARIO")` y callbacks `@PostConstruct`/`@PreDestroy` con logging. `PortfolioServiceBean` es el EJB stateful: conserva capital y porcentajes de la simulación por sesión HTTP, sus consultas de posiciones son `REQUIRED` y la planificación es `NOT_SUPPORTED`; se cierra explícitamente con `@Remove` (disparado por `PortfolioSesion` al terminar o invalidar la sesión).

## Prueba rápida de API

```json
POST /inversorar/api/compras
{
  "ticker": "AAPL",
  "cantidad": 10,
  "precioUnitario": 180,
  "fecha": "2026-08-30"
}
```

```json
POST /inversorar/api/ventas
{
  "ticker": "AAPL",
  "cantidad": 4,
  "precioUnitario": 195,
  "fecha": "2026-09-10"
}
```

Ejemplo con catálogo MySQL: `{"ticker":"BTCUSDT","cantidad":"0.002","precioUnitario":"70000","fecha":"2026-09-01"}` (AAPL corresponde al catálogo ficticio del perfil H2). La fecha debe ser válida y no futura. Errores de negocio: 400. Sin autenticación: 401. Sin rol: 403.

La base se recrea al desplegar con el perfil `demo-init` para que la demostración siempre comience con el catálogo inicial y carteras vacías; el perfil por defecto y el perfil `mysql` conservan los datos existentes.

## Documentación y entrega

**Entrega general pendiente:** integrar el login definitivo del equipo y acordar la relación definitiva con usuarios; ejecutar una demo/smoke conjunta que incluya Venta contra el WAR desplegado (ver [PRUEBAS.md](docs/PRUEBAS.md) y [REQUISITOS.md](docs/REQUISITOS.md) para el detalle de lo ya verificado y lo que falta).
