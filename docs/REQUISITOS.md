# Requisitos y checklist

## Funcionales

- RF-01: listar instrumentos de acciones, criptomonedas, bonos, CEDEARs y monedas (catálogo de demostración H2, o catálogo real de MySQL con el perfil `mysql`).
- RF-02: permitir seleccionar un instrumento y completar ticker, cantidad, precio unitario y fecha.
- RF-03: registrar una compra en el portfolio del usuario autenticado.
- RF-04: registrar una venta de un instrumento que se posee, sin superar la cantidad poseída.
- RF-05: calcular el total de cada operación en el servidor.
- RF-06: consolidar compras y ventas del mismo ticker en una posición, a costo promedio ponderado.
- RF-07: mostrar capital invertido, patrimonio, ganancia (realizada + no realizada) y posiciones actualizadas, agrupando por moneda cuando hay más de una.
- RF-08: acumular la ganancia realizada de las ventas y reflejarla en la ganancia total del portfolio, aun cuando una posición se venda por completo.
- RF-09: exponer compra, venta y resumen mediante API REST protegida por rol.
- RF-10: permitir planificar una distribución de capital por tipo de instrumento durante la sesión, sin persistirla ni mezclarla con las inversiones reales.
- RF-11: en modo MySQL, exponer un catálogo real con búsqueda paginada y precios históricos, y registrar cada compra como una orden transaccional vinculada al movimiento.

## Validaciones

- Cantidad y precio unitario deben ser mayores que cero, con la precisión soportada por las columnas (`cantidad`, `precio` y `total` hasta 10 decimales / 13-15 enteros según el campo).
- El ticker debe existir en el catálogo.
- La fecha es obligatoria y no puede ser futura.
- Una venta no puede superar la cantidad actualmente poseída del instrumento; si no se posee, se rechaza.
- Una operación con orden vinculada no puede tener una moneda distinta a la moneda de cotización del instrumento.
- Las entradas inválidas responden HTTP 400 con un mensaje para la interfaz; sin autenticación 401, sin rol 403.

## Segunda entrega parcial

La [guía de entrega del 16/11](ENTREGA_2.md) vincula cada criterio con su evidencia.
La API de órdenes devuelve 202 al aceptar una orden para el broker en MySQL,
404 si la orden no existe o pertenece a otro usuario, y 409 si el estado actual
impide la transición. La autenticación exige JWT y el contenedor verifica el rol.
La simulación es una funcionalidad de Portfolio, no otro componente. No se calcula riesgo ni rendimiento histórico de la simulación; tampoco hay precios en vivo ni pagos reales. El catálogo de demostración es ilustrativo; el catálogo MySQL usa los datos reales recibidos del equipo.

## Fuera de alcance

Cotizaciones en tiempo real, historial completo del lado de la simulación y detalle por ticker más allá del histórico de precios quedan para instancias posteriores.
