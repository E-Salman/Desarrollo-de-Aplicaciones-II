# Arquitectura por capas

```text
JSP + JavaScript (dashboard)
            │
            ▼ REST (JAX-RS, requiere USUARIO)
CompraResource (instrumentos, compras, ventas)   CatalogoResource (catálogo/históricos MySQL)   PortfolioResource / SimulacionPortfolioResource
            │                                                  │                                        │
            ▼                                                  ▼                                        ▼
CompraService/CompraServiceBean   VentaService/VentaServiceBean        PortfolioSesion (CDI @SessionScoped)
   (@Stateless, JTA)                 (@Stateless, JTA)                        │ (una referencia @EJB por sesión HTTP)
            │                                  │                              ▼
            │                                  │                     PortfolioService/PortfolioServiceBean
            │                                  │                     (@Stateful: capital/porcentajes en memoria;
            │                                  │                      obtenerResumen/obtenerPosiciones con REQUIRED
            │                                  │                      releen operaciones en cada llamada + CotizacionStrategy)
            └──────────────┬───────────────────┴──────────────┬──────────────────────┘
                           ▼                                   ▼
                    PortfolioActual                    Repositories JPA (Instrumento, Portfolio, Operacion,
                           │                             Orden/OrdenDetalle, CatalogoMercadoRepository)
                           │                                    │
                           └────────────────┬───────────────────┘
                                             ▼
                          H2 ExampleDS de WildFly (orm-default.xml) / MySQL (orm-mysql.xml)
```

## Módulos lógicos

- `presentation`: Servlets, JSP y recursos JAX-RS. Reciben datos y devuelven HTTP/HTML/JSON; no contienen reglas de negocio ni SQL. `PortfolioSesion` (CDI `@SessionScoped`) es el único punto que resuelve la referencia al EJB stateful de Portfolio por sesión HTTP; ningún resource inyecta `PortfolioService` directamente. `CatalogoResource` expone el catálogo MySQL (búsqueda paginada e históricos) reutilizando `CompraService`.
- `business`: `CompraService`/`VentaService` validan y registran operaciones de compra y venta (no consolidan posiciones). `PortfolioService` es la Service Facade **stateful** que consolida compras y ventas, calcula cotización vía `CotizacionStrategy` y además guarda capital/porcentajes de una simulación mientras dura la sesión. `ConfiguracionApp` expone el flag `catalogo.mysql` que decide, en tiempo de arranque, si el catálogo y las cotizaciones vienen de MySQL o del demo H2.
- `business.dto`: contratos de entrada y salida entre negocio y presentación; las entidades nunca se exponen por REST. Incluye `InstrumentoMercado`/`PrecioMercado` (catálogo MySQL) y `TotalesMonedaDto` (totales por moneda del resumen).
- `data`: entidades JPA, enumeraciones y repositorios (Repository/DAO). No conoce REST. Incluye `Orden`/`OrdenDetalle` (registro transaccional de una compra en modo MySQL) y `CatalogoMercadoRepository` (lectura del esquema de mercado con SQL nativo).

La solicitud atraviesa únicamente la capa siguiente: presentación → negocio → datos. Una compra o venta se registra dentro de una transacción gestionada por el contenedor (`TransactionAttribute.REQUIRED`); el total se calcula en el servidor. `ReglaNegocioException` es `@ApplicationException(rollback = true)`, así que una regla fallida revierte los cambios de esa operación — en modo MySQL esto incluye la `Orden`/`OrdenDetalle` de la compra, en la misma transacción que la `Operacion`.

Compra y Venta solo registran movimientos; no consolidan posiciones ni conocen la simulación. Portfolio no escribe operaciones: solo lee y agrega. Compra/Venta resuelven la cartera mediante `PortfolioActual` (frontera de identidad basada en el principal autenticado, usada para persistir); Portfolio resuelve su conversación mediante `PortfolioSesion` (frontera de identidad de sesión HTTP, usada para reutilizar la instancia stateful). Ningún componente recibe IDs de cartera arbitrarios del navegador.

## Un mismo modelo Java, dos mapeos JPA

`Instrumento`, `Portfolio` y `Operacion` son las mismas clases Java en ambos perfiles; lo que cambia es el archivo de mapeo (`db.mapping.file`, filtrado por Maven): `orm-default.xml` para el demo H2 (mantiene `Instrumento.cotizacionActual` como columna propia) y `orm-mysql.xml` para el perfil `mysql` (mapea `ticker`→columna `simbolo` y vuelve `cotizacionActual` transient, porque el precio sale de la tabla `precios` vía `CatalogoMercadoRepository`; también hace transient `Portfolio.usuarioId` y `Operacion.ordenDetalle` en el demo H2, que no tiene esas columnas).

## Consolidación de posiciones (Compra + Venta + multi-moneda)

`PortfolioServiceBean.consolidar()` agrupa las operaciones de cada ticker y las procesa en orden cronológico (fecha y luego id) llevando cantidad e invertido a **costo promedio ponderado**: una compra suma cantidad e invertido; una venta descuenta ambos proporcionalmente al precio promedio vigente en ese momento (no al precio de venta) y acumula la diferencia como ganancia realizada. Si una operación tiene una `Orden` vinculada, se valida que la moneda de esa orden coincida con la moneda de cotización del instrumento, para no reinterpretar un importe ya registrado si el catálogo cambia de moneda.

El resumen expone `gananciaRealizada` por separado y `gananciaTotal = (patrimonioTotal − capitalInvertido) + gananciaRealizada`, de modo que la ganancia de una posición vendida por completo no desaparece del total aunque la posición ya no se liste. Los totales también se agrupan por moneda (`totalesPorMoneda`); si el portfolio tiene posiciones en más de una moneda, los totales únicos (`capitalInvertido`, `patrimonioTotal`, `gananciaTotal`, `rendimientoPorcentaje`, `moneda`) quedan `null` para no sumar monedas distintas, y solo se muestran los totales por moneda.

Esta consolidación se recalcula desde cero en cada llamada (no es conversacional); solo la simulación de distribución vive en memoria del bean stateful. Ver [docs/VENTA.md](VENTA.md) para el detalle y un ejemplo numérico paso a paso de Venta.

## Ciclo de vida gestionado por el contenedor

- `DatosIniciales` (`@Singleton @Startup`) carga el catálogo de instrumentos de demostración en su callback `@PostConstruct`, ejecutado una única vez al arrancar la aplicación (no hace nada si el perfil MySQL está activo).
- `CompraServiceBean` y `VentaServiceBean` (`@Stateless`) registran mensajes en `@PostConstruct`/`@PreDestroy` — el contenedor crea y destruye instancias según demanda, sin identidad de cliente en sus campos.
- `PortfolioServiceBean` (`@Stateful`) es la conversación por sesión: `@PostConstruct`/`@PreDestroy` registran un identificador de conversación; `PortfolioSesion` (CDI `@SessionScoped`) guarda la única referencia `@EJB` por sesión HTTP y dispara `cerrar()` (anotado `@Remove @PermitAll`) en su propio `@PreDestroy`, ya sea por `DELETE /api/portfolio/sesion` o por expiración de la sesión (30 minutos).

## Patrones de diseño

1. **Repository/DAO**: `InstrumentoRepository`, `PortfolioRepository`, `OperacionRepository`, `OrdenRepository` y `CatalogoMercadoRepository` encapsulan `EntityManager` y consultas (nativas, en el caso del catálogo MySQL).
2. **Service Facade**: `PortfolioService` compone identidad, persistencia, cotización y planificación detrás de un contrato pequeño (`obtenerResumen`/`obtenerPosiciones`/`calcularSimulacion`).
3. **Strategy**: `CotizacionStrategy` desacopla cómo se obtiene el precio de mercado de un instrumento (catálogo H2 de demo o último cierre de MySQL); se puede sustituir sin tocar la consolidación (ver tests en `ComponentesTest`).

## Seguridad

`web.xml` protege `/api/*` y `/dashboard` con BASIC auth sobre `ApplicationRealm`, exigiendo el rol `USUARIO`. Los EJB de Compra, Venta, Portfolio, catálogo y la resolución de identidad (`PortfolioActualBean`) exigen `@RolesAllowed("USUARIO")` — la identidad llega desde el contenedor, nunca desde un campo del request. `PortfolioSesion.servicio(usuario)` además rechaza con `ForbiddenException` (403) si la sesión HTTP ya está vinculada a otra identidad, evitando que una cookie compartida cruce carteras.

Ver [documento técnico](TECNICO.md) para fórmulas, decisiones y defensa, [MySQL](MYSQL.md)/[esquema](MYSQL_ESQUEMA.md) para el detalle del catálogo real, e [integración](INTEGRACION.md) para los contratos con otras ramas (Login definitivo).
