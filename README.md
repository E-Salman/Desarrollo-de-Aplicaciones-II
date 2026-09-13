# InversorAR - Compra y Portfolio

Rama `integracion_entrega`. Componentes de negocio acordados: **Compra, Venta y Portfolio**. Compra es `@Stateless`; Portfolio es `@Stateful` y reúne la consulta de inversiones persistentes y una simulación temporal de distribución. La simulación es una funcionalidad de Portfolio, no un cuarto componente. Venta pertenece al trabajo del compañero y sigue pendiente de integración.

Portfolio conserva capital y porcentajes por sesión HTTP. Relee las operaciones en cada consulta: una compra actualiza las posiciones sin perder la simulación. `PortfolioSesion` mantiene una única referencia EJB por sesión y rechaza cambios de identidad sobre la misma cookie. No inyectar PortfolioService directamente en cada recurso REST: utilizar PortfolioSesion.
# InversorAR — Compra, Venta y Portfolio de Instrumentos

Componentes Jakarta EE 10 implementados en capas: **Compra** y **Venta** registran operaciones, **Portfolio** las consolida, y un **Simulador** conversacional se suma como apoyo académico.

## Documentación

- [Tecnologías y versiones](docs/TECNOLOGIAS.md)
- [Arquitectura en capas](docs/ARQUITECTURA.md)
- [Requisitos y checklist](docs/REQUISITOS.md)
- [Funcionamiento de la venta](docs/VENTA.md)
- [Documento técnico](docs/TECNICO.md) (también en [PDF](docs/TECNICO.pdf))
- [Integración con Login, BD y otras ramas](docs/INTEGRACION.md)
- [Pruebas y demo](docs/PRUEBAS.md)

## Arquitectura

`JSP/Servlet + JAX-RS` → `CompraService` / `VentaService` (`@Stateless`) para registrar operaciones, y `PortfolioService` (`@Stateless`, Service Facade) para consolidarlas con `CotizacionStrategy` → `Repositories JPA` → `H2 ExampleDS` / `MySQL`.

La pantalla se abre en `/inversorar/dashboard`. Todas las rutas siguientes llevan prefijo `/inversorar/api` y requieren rol `USUARIO` (BASIC auth):

| Método y ruta | Resultado |
|---|---|
| GET `/instrumentos` | Catálogo disponible |
| POST `/compras` | Registra compra; 201 con resumen actualizado |
| POST `/ventas` | Registra venta; 201 con resumen actualizado |
| GET `/portfolio/resumen` | Capital invertido, patrimonio, ganancia (realizada + no realizada) y posiciones |
| GET `/portfolio/posiciones` | Cantidad, precio promedio, cotización, costo, valor y rendimiento por instrumento |
| GET `/simulador` | Estado de la simulación de la sesión HTTP |
| PUT `/simulador/capital` | `{"valor":1000000}` |
| PUT `/simulador/porcentajes/ACCION` | `{"valor":40}`; tipos: ACCION, BONO, CRIPTO, CEDEAR, MONEDA |
| DELETE `/simulador` | Cierra sesión de simulación y destruye el EJB; 204 |

La compra y la venta reciben `ticker`, `cantidad`, `precioUnitario` y `fecha`. El servidor valida los valores, calcula el total y, en una venta, además valida que no se supere la cantidad poseída del instrumento. El cliente no envía un portfolioId: la cartera se resuelve por identidad autenticada mediante `PortfolioActual`.

El resumen del portfolio consolida compras y ventas por ticker a costo promedio ponderado (una venta parcial reduce cantidad e invertido proporcionalmente, dejando `rendimiento` como la ganancia potencial sobre lo que sigue en cartera). La ganancia ya realizada en ventas se acumula aparte (`gananciaRealizada`) y se suma en el KPI `gananciaTotal` del portfolio, sin perderse cuando una posición se vende por completo. Ver [detalle y ejemplo numérico](docs/VENTA.md).

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
| POST `/compras` | Registra compra; 201 con resumen actualizado |
| GET `/portfolio/resumen` | Capital invertido, patrimonio, ganancia, rendimiento porcentual y posiciones |
| GET `/portfolio/posiciones` | Cantidad, precio promedio, cotización, costo, valor y rendimiento por instrumento |
| GET `/portfolio/simulacion` | Estado de la simulación de la sesión HTTP |
| PUT `/portfolio/simulacion/capital` | `{"valor":1000000}` |
| PUT `/portfolio/simulacion/porcentajes/ACCION` | `{"valor":40}`; tipos: ACCION, BONO, CRIPTO, CEDEAR, MONEDA |
| DELETE `/portfolio/simulacion` | Reinicia sólo el plan temporal; 204 |
| DELETE `/portfolio/sesion` | Invalida la sesión HTTP y retira el EJB Portfolio mediante @Remove; 204 |

Ejemplo de compra: `{"ticker":"AAPL","cantidad":10,"precioUnitario":180,"fecha":"2026-09-01"}`. La fecha debe ser válida y no futura. Errores de negocio: 400. Sin autenticación: 401. Sin rol: 403.

El cliente no envía un portfolioId. La antigua ruta `/portfolios/1/resumen` se retiró para impedir consultas a carteras ajenas. El dashboard se actualizó al contrato nuevo. La planificación requiere conservar JSESSIONID y no persiste inversiones. Las rutas anteriores `/api/simulador` se conservan como alias HTTP del mismo Portfolio, sin otro EJB. Su DELETE ahora reinicia el plan sin invalidar toda la sesión; para cerrar la conversación usar DELETE `/api/portfolio/sesion`. El cierre no borra operaciones de la base.

## Documentación y entrega

- [Decisiones técnicas actualizadas](docs/TECNICO.md). `docs/TECNICO.pdf` corresponde a la versión anterior; no refleja este refactor ni se presenta como documento vigente de entrega.
- [Arquitectura](docs/ARQUITECTURA.md), [requisitos y checklist](docs/REQUISITOS.md), [tecnologías](docs/TECNOLOGIAS.md).
- [Integración con Login, BD y Venta](docs/INTEGRACION.md).
- [Pruebas y demo](docs/PRUEBAS.md).

Patrones presentes: Repository/DAO, Service Facade y Strategy de cotización. Compra usa EJB stateless y transacción REQUIRED; Portfolio usa EJB stateful con callbacks visibles y cierre explícito; sus consultas tienen transacción REQUIRED y la planificación NOT_SUPPORTED.

**Entrega general pendiente:** integrar y demostrar Venta, integrar el login del equipo y ejecutar con el esquema MySQL definitivo. No se declara terminada Venta ni se cuenta la simulación como componente adicional.
## Patrones y componentes con ciclo de vida

Patrones presentes: Repository/DAO, Service Facade (`PortfolioService`) y Strategy (`CotizacionStrategy` de cotización). `CompraServiceBean` y `VentaServiceBean` son EJB stateless con transacción `REQUIRED`, `@RolesAllowed("USUARIO")` y callbacks `@PostConstruct`/`@PreDestroy` con logging. El Simulador usa un EJB stateful con callbacks visibles y cierre explícito (`@Remove`).

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

La base se recrea al desplegar con el perfil `demo-init` para que la demostración siempre comience con el catálogo inicial y carteras vacías; el perfil por defecto conserva los datos existentes.
