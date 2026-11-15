# ADR 002 — Service Facade para Portfolio

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** El dashboard y el simulador necesitan el resumen de la cartera y acceso a una planificación temporal por usuario. El cálculo combina movimientos y cotizaciones. El plan dura lo que dura la conversación.

**Decisión.** `PortfolioService` expone una fachada para la presentación. `PortfolioServiceBean` usa `@Stateful` y conserva el plan temporal. `PortfolioSesion` vincula una referencia a la sesión HTTP autenticada. Compra, Venta y Órdenes mantienen sus servicios stateless.

**Alternativas.** Consolidar en cada recurso REST duplicaba las reglas de negocio. Persistir el borrador del simulador introduciría almacenamiento y caducidad para un borrador que el diseño acota a la sesión.

**Consecuencias.** La fachada concentra la consolidación y mantiene el aislamiento de la conversación. Cada sesión usa recursos del contenedor y debe terminarse al salir. Las posiciones se recalculan desde los movimientos persistidos cuando se consultan.

**Evidencia.** `ComponentesTest`, `PortfolioSesionTest`, `SimulacionPlanTest` y `smoke_simulador.mjs`.
