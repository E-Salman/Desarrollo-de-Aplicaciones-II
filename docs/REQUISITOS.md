# Requisitos y checklist

## Funcionales

- RF-01: listar instrumentos de acciones, criptomonedas, bonos, CEDEARs y monedas.
- RF-02: permitir seleccionar un instrumento y completar ticker, cantidad, precio unitario y fecha.
- RF-03: registrar una compra en el portfolio del usuario autenticado.
- RF-04: registrar una venta de un instrumento que se posee, sin superar la cantidad poseída.
- RF-05: calcular el total de cada operación en el servidor.
- RF-06: consolidar compras y ventas del mismo ticker en una posición, a costo promedio ponderado.
- RF-07: mostrar capital invertido, patrimonio, ganancia (realizada + no realizada) y posiciones actualizadas.
- RF-08: acumular la ganancia realizada de las ventas y reflejarla en la ganancia total del portfolio, aun cuando una posición se venda por completo.
- RF-09: exponer compra, venta y resumen mediante API REST protegida por rol.

## Validaciones

- Cantidad y precio unitario deben ser mayores que cero, con la precisión soportada por las columnas (`cantidad` hasta 6 decimales/13 enteros, `precio`/`total` hasta 4 decimales/15 enteros).
- El ticker debe existir en el catálogo.
- La fecha es obligatoria y no puede ser futura.
- Una venta no puede superar la cantidad actualmente poseída del instrumento; si no se posee, se rechaza.
- Las entradas inválidas responden HTTP 400 con un mensaje para la interfaz; sin autenticación 401, sin rol 403.

## Checklist contra la consigna

| Requisito | Estado y evidencia |
|---|---|
| Compra funcional en capas | Implementada: REST/dashboard, interfaz EJB, validaciones y repositories |
| Venta funcional en capas | Implementada: `VentaService`/`VentaServiceBean`, valida cantidad poseída, consolidada en `PortfolioService` a costo promedio ponderado |
| Portfolio funcional en capas | Implementado: interfaz + stateless (Service Facade), DTOs, REST y dashboard |
| Tres componentes de entrega | Compra, Venta y Portfolio integrados a nivel de código y `mvn test`; falta una corrida de demo/smoke conjunta con Venta desplegada (ver [PRUEBAS.md](PRUEBAS.md)) |
| Al menos un stateless | `CompraServiceBean`, `VentaServiceBean` y `PortfolioServiceBean` |
| Al menos un stateful | `SimuladorPortfolioServiceBean` por sesión HTTP |
| Inicialización/destrucción | Compra, Venta y Simulador con `@PostConstruct`/`@PreDestroy` y logging; `@Remove` para cerrar la simulación |
| Patrones de diseño | Repository/DAO, Service Facade, Strategy; ver [ARQUITECTURA.md](ARQUITECTURA.md) |
| Autenticación y rol | `web.xml` BASIC + rol `USUARIO` y `@RolesAllowed` en los EJB; login definitivo del equipo pendiente |
| Transacciones | Compra y Venta con `TransactionAttribute.REQUIRED`, JTA y `ReglaNegocioException` con `rollback=true` |
| MySQL | Perfil/configuración preparados (`mvn -Pmysql package`); despliegue en el esquema definitivo del equipo pendiente |
| Documento de 5-8 páginas | `TECNICO.pdf`/`TECNICO.md` (redactado antes de integrar Venta; repasar antes de entregar) |
| Demo y defensa | [PRUEBAS.md](PRUEBAS.md) y [TECNICO.md](TECNICO.md) cubren Compra/Portfolio/Simulador; falta sumar el caso de Venta a esa evidencia |

No se incluyen historial/riesgo del simulador, datos históricos, pagos reales ni conversión multimoneda. El catálogo es ilustrativo.

## Fuera de alcance

Autenticación definitiva del equipo, cotizaciones en tiempo real, historial completo, simulador como reemplazo de Venta, y detalle por ticker quedan para instancias posteriores.
