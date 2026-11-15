# ADR 005 — Cola JMS para ejecutar órdenes

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** La aceptación de una orden y la ejecución tienen tiempos y fallos distintos. La ejecución debe preservar la relación entre orden y movimiento.

**Decisión.** Guardar la orden pendiente y publicar en `ordenes` bajo la transacción JTA. Artemis entrega a `EjecutorOrdenesMDB`. El consumidor lee y bloquea la orden persistida y confirma la ejecución junto con el cambio de estado. La restricción única sobre el detalle vinculado protege la unicidad del movimiento. La DLQ recibe mensajes después de los intentos configurados.

**Alternativas.** La ejecución síncrona obliga a esperar y ata la respuesta al tiempo del procesamiento. Un tópico publica a varios consumidores y no representa la asignación de una orden a un ejecutor.

**Consecuencias.** La API devuelve 202 antes del resultado final. El dashboard consulta el estado y actualiza la cartera al terminar. La demo requiere el perfil MySQL y Artemis en WildFly Full. La entrega del broker puede repetirse y el consumidor ignora transiciones que ya no corresponden.

**Evidencia.** `wildfly-messaging.cli`, `EjecutorOrdenesTest`, `ConsumidoresTest` y `smoke_segunda_entrega.mjs`.
