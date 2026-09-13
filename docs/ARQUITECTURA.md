# Arquitectura por capas

```text
Dashboard / REST de Compra -> CompraService @Stateless -> Repositories -> JPA/JTA
Dashboard / REST Portfolio -> PortfolioService @Stateless -> Repositories + CotizacionStrategy
                                  |                              |
                             PortfolioActual               H2 / MySQL
REST Simulador -> SimulacionSesion -> SimuladorPortfolioService @Stateful -> memoria temporal
```

Presentación valida el protocolo HTTP y devuelve DTOs. Negocio valida la compra, coordina transacciones y consolida posiciones. Datos contiene entidades y Repository/DAO; no conoce REST. PortfolioService es la Service Facade que compone operaciones y cotización para entregar un resumen. CotizacionStrategy desacopla el valor de mercado del algoritmo de agregación; CotizacionCatalogo es la estrategia activa.

Compra solo lista instrumentos y registra compras. Portfolio no escribe operaciones. Ambos resuelven la cartera mediante PortfolioActual, sin recibir IDs arbitrarios del navegador. Cada EJB stateless puede atender clientes distintos; no guarda identidad ni operaciones en campos conversacionales.

SimulacionSesion es un bean CDI SessionScoped serializable que conserva una referencia al EJB stateful por sesión. El EJB guarda capital y porcentajes entre peticiones. Cerrar la sesión invoca @Remove y luego @PreDestroy; @PermitAll en cerrar permite liberar recursos cuando ya terminó el contexto de autenticación. No hay endpoint público: todo /api requiere USUARIO. No se agrega una capa DAO ficticia al simulador porque no persiste datos.

Ver [documento técnico](TECNICO.md) para fórmulas, decisiones y defensa, e [integración](INTEGRACION.md) para los contratos con otras ramas.
