# Requisitos del primer componente

## Funcionales

- RF-01: listar instrumentos de acciones, criptomonedas, bonos, CEDEARs y monedas.
- RF-02: permitir seleccionar un instrumento y completar ticker, cantidad, precio unitario y fecha.
- RF-03: registrar una compra en el portfolio de demostración.
- RF-04: calcular el total de la operación en el servidor.
- RF-05: consolidar compras del mismo ticker en una posición con cantidad y precio promedio.
- RF-06: mostrar capital invertido, patrimonio, ganancia y posiciones actualizadas.
- RF-07: exponer las operaciones mediante API REST.

## Validaciones

- Cantidad y precio unitario deben ser mayores que cero.
- El ticker debe existir en el catálogo.
- La fecha es obligatoria y no puede ser futura.
- Las entradas inválidas responden HTTP 400 con un mensaje para la interfaz.

## Fuera de alcance

Venta, autenticación, cotizaciones en tiempo real, historial completo, simulador y detalle por ticker quedan para componentes posteriores.
