# ADR 004 — Strategy para cotizar posiciones

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** Para calcular una posición se requiere una cotización, cuya fuente depende del perfil. Las pruebas necesitan precios deterministas.

**Decisión.** Portfolio depende de la interfaz `CotizacionStrategy`. `CotizacionCatalogo` realiza la consulta de referencia disponible en el perfil activo. Las pruebas proporcionan su propia estrategia.

**Alternativas.** Acoplar Portfolio al catálogo o usar un condicional de proveedor en cada consolidación haría que cambios en la fuente de cotización requieran cambios en el cálculo. Un proveedor externo en esta instancia introduciría una dependencia en la demo para una función cubierta por el catálogo local.

**Consecuencias.** El cálculo queda separado de la obtención del precio. Incorporar otra implementación requiere seleccionar el bean CDI que se usará. Las cotizaciones del catálogo son históricas.

**Evidencia.** `CotizacionStrategy`, `CotizacionCatalogo` y las pruebas de consolidación en `ComponentesTest`.
