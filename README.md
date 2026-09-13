# InversorAR - Compra y Portfolio

Rama `integracion_entrega`. Componentes de negocio acordados: **Compra, Venta y Portfolio**. Compra es `@Stateless`; Portfolio es `@Stateful` y reúne la consulta de inversiones persistentes y una simulación temporal de distribución. La simulación es una funcionalidad de Portfolio, no un cuarto componente. Venta pertenece al trabajo del compañero y sigue pendiente de integración.

Portfolio conserva capital y porcentajes por sesión HTTP. Relee las operaciones en cada consulta: una compra actualiza las posiciones sin perder la simulación. `PortfolioSesion` mantiene una única referencia EJB por sesión y rechaza cambios de identidad sobre la misma cookie. No inyectar PortfolioService directamente en cada recurso REST: utilizar PortfolioSesion.

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
