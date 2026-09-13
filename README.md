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
