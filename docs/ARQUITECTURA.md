# Arquitectura por capas

```text
JSP + JavaScript (dashboard)
            │
            ▼ REST (JAX-RS, requiere USUARIO)
CompraResource (instrumentos, compras, ventas)      PortfolioResource (resumen, posiciones)      SimuladorResource
            │                                                  │                                        │
            ▼                                                  ▼                                        ▼
CompraService/CompraServiceBean    VentaService/VentaServiceBean    PortfolioService/PortfolioServiceBean    SimulacionSesion -> SimuladorPortfolioService
   (@Stateless, JTA)                  (@Stateless, JTA)              (@Stateless, Service Facade + CotizacionStrategy)         (@Stateful)
            │                                  │                                  │
            └────────────────┬─────────────────┴──────────────┬───────────────────┘
                              ▼                                ▼
                       PortfolioActual                  Repositories JPA (Instrumento, Portfolio, Operacion)
                              │                                │
                              └────────────────┬───────────────┘
                                                ▼
                                     H2 ExampleDS de WildFly / MySQL
```

## Módulos lógicos

- `presentation`: Servlets, JSP y recursos JAX-RS. Reciben datos y devuelven HTTP/HTML/JSON; no contienen reglas de negocio ni SQL.
- `business`: `CompraService`/`VentaService` validan y registran operaciones de compra y venta; `PortfolioService` es la Service Facade que las consolida en un resumen sin exponer entidades. `CotizacionStrategy` desacopla el valor de mercado del algoritmo de agregación (`CotizacionCatalogo` es la estrategia activa).
- `business.dto`: contratos de entrada y salida entre negocio y presentación; las entidades nunca se exponen por REST.
- `data`: entidades JPA, enumeraciones y repositorios (Repository/DAO). No conoce REST.

La solicitud atraviesa únicamente la capa siguiente: presentación → negocio → datos. Una compra o venta se registra dentro de una transacción gestionada por el contenedor (`TransactionAttribute.REQUIRED`); el total se calcula en el servidor. `ReglaNegocioException` es `@ApplicationException(rollback = true)`, así que una regla fallida revierte los cambios de esa operación.

Compra y Venta solo registran movimientos; no consolidan posiciones. Portfolio no escribe operaciones, solo las lee y las agrega. Los tres resuelven la cartera mediante `PortfolioActual` (frontera de identidad basada en el principal autenticado), sin recibir IDs arbitrarios del navegador — cada EJB stateless puede atender clientes distintos sin guardar identidad en campos propios.

## Consolidación de posiciones (Compra + Venta)

`PortfolioServiceBean.consolidar()` agrupa las operaciones de cada ticker y las procesa en orden cronológico (fecha y luego id) llevando cantidad e invertido a **costo promedio ponderado**: una compra suma cantidad e invertido; una venta descuenta ambos proporcionalmente al precio promedio vigente en ese momento (no al precio de venta) y acumula la diferencia como ganancia realizada. El resumen expone `gananciaRealizada` por separado y `gananciaTotal = (patrimonioTotal − capitalInvertido) + gananciaRealizada`, de modo que la ganancia de una posición vendida por completo no desaparece del total aunque la posición ya no se liste. Ver [docs/VENTA.md](VENTA.md) para el detalle y un ejemplo numérico paso a paso.

## Ciclo de vida gestionado por el contenedor

- `DatosIniciales` (`@Singleton @Startup`) carga el catálogo de instrumentos en su callback `@PostConstruct`, ejecutado una única vez al arrancar la aplicación.
- `CompraServiceBean` y `VentaServiceBean` (`@Stateless`) registran mensajes en `@PostConstruct`/`@PreDestroy` — evidencia de que el contenedor crea y destruye las instancias, no la aplicación.
- `SimuladorPortfolioServiceBean` (`@Stateful`) conserva capital y porcentajes por conversación HTTP; `SimulacionSesion` (CDI `@SessionScoped`) guarda la referencia por sesión y cierra la conversación invocando `@Remove`, que dispara `@PreDestroy`.

## Patrones de diseño

1. **Repository/DAO**: `InstrumentoRepository`, `PortfolioRepository`, `OperacionRepository` encapsulan `EntityManager` y consultas.
2. **Service Facade**: `PortfolioService` compone identidad, persistencia y cotización detrás de un contrato pequeño (`obtenerResumen`/`obtenerPosiciones`).
3. **Strategy**: `CotizacionStrategy` desacopla cómo se obtiene el precio de mercado de un instrumento; se puede sustituir sin tocar la consolidación (ver tests en `ComponentesTest`).

## Seguridad

`web.xml` protege `/api/*` y `/dashboard` con BASIC auth sobre `ApplicationRealm`, exigiendo el rol `USUARIO`. Los EJB de Compra, Venta, Portfolio y la resolución de identidad (`PortfolioActualBean`) exigen `@RolesAllowed("USUARIO")` — la identidad llega desde el contenedor, nunca desde un campo del request.

Ver [documento técnico](TECNICO.md) para fórmulas, decisiones y defensa, e [integración](INTEGRACION.md) para los contratos con otras ramas (Login y MySQL definitivos).
