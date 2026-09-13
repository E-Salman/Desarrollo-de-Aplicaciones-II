# Arquitectura del componente Compra/Venta de Instrumentos

```text
JSP + JavaScript / REST client
            │
            ▼
Servlet Dashboard + OperacionResource (JAX-RS)
            │
            ▼
OperacionService / OperacionServiceBean (@Stateless, JTA)
            │
            ▼
CompraStrategy / VentaStrategy (Strategy) + Repositories JPA
            │
            ▼
H2 ExampleDS de WildFly
```

## Módulos lógicos

- `presentation`: Servlet, JSP y recurso JAX-RS. Recibe datos y devuelve HTTP/HTML/JSON; no contiene reglas ni SQL.
- `business`: contrato `OperacionService`, implementación EJB, validaciones y consolidación de posiciones.
- `business` (patrón Strategy): `OperacionStrategy` define el contrato de validación/creación de una operación; `CompraStrategy` y `VentaStrategy` lo implementan con las reglas propias de cada tipo (una compra siempre es válida en cantidad/precio/fecha; una venta además no puede superar la cantidad poseída). `OperacionServiceBean` elige la estrategia según `TipoOperacion` en lugar de tener `if/else` por tipo de operación.
- `business.dto`: contratos de entrada y salida entre negocio y presentación; las entidades nunca se exponen por REST.
- `data`: entidades JPA, enumeraciones y repositorios. Sólo resuelve persistencia.

La solicitud atraviesa únicamente la capa siguiente: presentación → negocio → datos. Una compra o venta se registra dentro de una transacción gestionada por el contenedor; el total se calcula en el servidor.

## Ciclo de vida gestionado por el contenedor

- `DatosIniciales` (`@Singleton @Startup`) carga el catálogo de instrumentos y el portfolio demo en su callback `@PostConstruct`, ejecutado una única vez por el contenedor al arrancar la aplicación.
- `OperacionServiceBean` (`@Stateless`) arma en su callback `@PostConstruct` el mapa `TipoOperacion → OperacionStrategy` a partir de las estrategias que el contenedor le inyecta, y libera ese estado en `@PreDestroy` cuando el contenedor destruye la instancia del bean — evidencia concreta de que el ciclo de vida del componente lo maneja el contenedor EJB, no la aplicación.
