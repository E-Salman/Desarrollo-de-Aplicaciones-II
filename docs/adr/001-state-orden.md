# ADR 001 — State para el ciclo de vida de Orden

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** La misma orden se puede modificar desde la API y desde el consumidor JMS. Las transiciones deben respetar el ciclo de vida incluso cuando el usuario y el consumidor actúan al mismo tiempo.

**Decisión.** `Orden` delega las transiciones en `EstadoOrden`. Cada constante implementa las acciones permitidas y el comportamiento por defecto lanza `TransicionOrdenInvalida`. El estado se persiste por nombre, con los mismos valores históricos. Los servicios bloquean la fila antes de operar. El mapper REST convierte esa excepción en HTTP 409. Para una orden inexistente o ajena se devuelve 404.

**Alternativas.** Repetir condicionales en repositorios, servicios y recursos dispersaba las reglas. Una máquina externa de estados agregaba una dependencia para seis estados y obligaba a mantener otro modelo junto al mapeo JPA.

**Consecuencias.** Una transición inválida revierte la transacción. El ejecutor conserva su salida idempotente para mensajes de órdenes que ya salieron de PENDIENTE. Al agregar una transición hay que actualizar el estado y su prueba.

**Evidencia.** `OrdenTest`, `OrdenServiceTest`, `EjecutorOrdenesTest` y `smoke_segunda_entrega.mjs`.
