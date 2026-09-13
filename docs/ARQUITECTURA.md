# Arquitectura por capas

```text
REST Compra -> CompraService @Stateless -> Repository/DAO -> JPA/JTA
     | (resumen posterior)
     v
PortfolioSesion CDI @SessionScoped -> PortfolioService @Stateful
     ^                                  | consultas: Repository + CotizacionStrategy
     |                                  | planificación: capital y porcentajes en memoria
REST Portfolio y su simulación          v
                                  PortfolioActual -> cartera del principal autenticado
```

Los tres componentes de negocio son Compra, Venta y Portfolio. En esta rama están implementados Compra y Portfolio; Venta se integra desde el trabajo del compañero. La simulación pertenece a Portfolio. Tener varios resources, DTOs o repositories no convierte cada clase en otro componente de negocio.

Presentación recibe HTTP y devuelve DTOs. Compra valida y registra operaciones. Portfolio es una Service Facade: reúne identidad, repositorios, consolidación, cotización y planificación detrás de su interfaz local. Datos encapsula JPA mediante Repository/DAO. CotizacionStrategy desacopla la fuente del precio; CotizacionCatalogo es la implementación actual.

## Estado y ciclo de vida

PortfolioServiceBean es @Stateful y serializable. Sólo capital y porcentajes se guardan entre invocaciones; no conserva entidades ni resúmenes como caché. obtenerResumen y obtenerPosiciones consultan las operaciones actuales con REQUIRED. La simulación usa NOT_SUPPORTED y nunca guarda operaciones.

PortfolioSesion es CDI @SessionScoped, serializable, y mantiene una referencia EJB por sesión HTTP. CompraResource valida la identidad de esa sesión antes de registrar una compra. Todos los resources de Portfolio utilizan el mismo holder, incluido el alias /simulador. Otra sesión del mismo usuario comparte sus inversiones persistentes, pero tiene una planificación independiente.

@PostConstruct y @PreDestroy registran el identificador de conversación de Portfolio. DELETE /api/portfolio/simulacion sólo limpia el plan; DELETE /api/portfolio/sesion invalida la sesión HTTP y su callback llama a cerrar con @Remove. Lo mismo ocurre al expirar la sesión (30 minutos). @PermitAll en cerrar permite liberar recursos después de terminar la autenticación; los endpoints HTTP siguen exigiendo USUARIO.

CompraServiceBean sigue siendo @Stateless, con callbacks propios. No mantiene conversaciones de clientes. Las futuras llamadas de Venta deben seguir resolviendo las posiciones reales desde persistencia, sin depender del capital simulado.
