# Evidencia adicional: órdenes integradas

Verificación final previa al PR, 13/09/2026: `mvn -Pmysql test package`, 21 pruebas, cero fallos/errores y BUILD SUCCESS. Registro local: `work/mysql-pr-build.log`.

13/09/2026, mysql_componente. `work/ordenes-build.log`: 21 pruebas unitarias, sin errores, BUILD SUCCESS. `work/ordenes-qa-results.txt`: 20 verificaciones HTTP/SQL correctas, incluida una falla controlada al guardar el movimiento que revirtió también orden y detalle. El trigger y los registros QA se retiraron al terminar; no se alteraron compras del usuario en esas pruebas.

Las compras previas del usuario se vincularon a sus órdenes mediante una migración transaccional: dos compras migradas, cero pendientes al repetirla, sin cambios en cantidad, precio, importe ni resumen. El SQL completo del esquema se probó en una base temporal vacía, generando 11 tablas y 13 claves foráneas. Esa base temporal se retiró.

Los apartados siguientes conservan la evidencia de etapas previas y del ciclo de vida EJB; sus conteos de pruebas corresponden a esas ejecuciones.

# Evidencia anterior a la integración de órdenes: mysql_componente

13 de septiembre de 2026. Se revisaron el código y los registros locales de WildFly; no se provocó un redespliegue nuevo para generar esta documentación.

## Construcción e integración

- Construcción de esa etapa: `mvn -Pmysql package`, 17 pruebas JUnit/Mockito, cero fallos/errores y BUILD SUCCESS. También se ejecutó `mvn -Pmysql clean test package`.
- MySQL 8.1.0 y Connector/J 8.4.0: 38 verificaciones HTTP/SQL en una instancia aislada de WildFly 30.0.1.Final. Catálogo completo de 1.365 instrumentos, 1.267.911 precios, autenticación/roles, compras, precisión, monedas, aislamiento y conservación de operaciones al cerrar sesión.
- Comprobación adicional: tras redesplegar expresamente el WAR MySQL, la cartera QA conservó sus dos posiciones y 213 USDT de costo en esa moneda.
- Navegador: formulario compacto de compra, categorías sin datos visibles y vacías, selección de criptomonedas, catálogo opcional e históricos. En esa última comprobación de interfaz no se crearon compras.
- Las cuatro compras de las identidades QA se retiraron al terminar. No son las compras posteriores del usuario de la app. El servidor de pruebas se apagó y la aplicación local continúa en 18080 con MySQL.

Fuentes locales de esta tarea, fuera del repositorio: `work/compra-estetica-package.log`, `work/mysql-component-final.log`, `work/mysql-qa-results.txt` y `work/mysql-qa/log/server.log`. Los logs completos se excluyen de Git; este extracto conserva únicamente mensajes técnicos sin credenciales.

## Callbacks realmente observados

Extracto literal de `work/mysql-qa/log/server.log`:

```text
2026-09-13 19:23:29,101 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (default task-1) CompraServiceBean inicializado
2026-09-13 19:30:49,942 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (ServerService Thread Pool -- 102) CompraServiceBean destruido
2026-09-13 19:24:45,828 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado 1c5720c1-12e0-48dc-8251-00f28104ec6a
2026-09-13 19:24:46,180 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio destruido 1c5720c1-12e0-48dc-8251-00f28104ec6a
```

`CompraServiceBean.iniciar()` tiene `@PostConstruct` y `destruir()` tiene `@PreDestroy`. El contenedor inicializó instancias y ejecutó su destrucción al redesplegar. Los mensajes de Compra no incluyen identificador por instancia: prueban que se ejecutaron los callbacks, pero no permiten correlacionar individualmente todas las instancias del pool.

`PortfolioServiceBean.iniciar()` y `destruir()` tienen los mismos callbacks y registran un UUID por instancia. El UUID `1c5720c1-12e0-48dc-8251-00f28104ec6a` aparece tanto al iniciar como al destruir. La prueba HTTP cerró la conversación mediante `DELETE /api/portfolio/sesion`: se invalidó la sesión web, `PortfolioSesion.@PreDestroy` llamó al método `cerrar()` marcado `@Remove`, y el contenedor retiró el EJB. Las compras siguieron disponibles desde una nueva sesión.

La evidencia es de EJB Stateless/Stateful administrados por WildFly. Strategy no define un ciclo de vida. `CotizacionCatalogo` es CDI `@ApplicationScoped`; no tiene estos callbacks y no se presenta como evidencia del requisito EJB. En `joaco` se observaron callbacks de `OperacionServiceBean` escritos con nivel FINE, pero no se ejecutó Venta en esta rama ni se atribuyen registros de ejecución a ese componente.

Para repetir la demostración, usar una sesión de prueba, consultar Portfolio, definir un capital y porcentaje mediante `/api/portfolio/simulacion`, consultar nuevamente y cerrar con `DELETE /api/portfolio/sesion`. Correlacionar el UUID en `server.log`. Para ver `PreDestroy` de Compra, retirar o redesplegar el WAR únicamente en un servidor de prueba. Las pruebas con `new Bean()` no demuestran el ciclo de vida del contenedor. No se comprobó una secuencia de pasivación/activación ni se promete persistencia de la planificación temporal tras reiniciar el servidor.

## Evidencia histórica de etapas anteriores (H2)

Los apartados siguientes se conservan como antecedentes: sus conteos y referencias a MySQL pendiente describen esas ejecuciones anteriores, no el estado actual.

# Evidencia de Portfolio stateful

13 de septiembre de 2026. Rama integracion_entrega.

- mvn test y mvn package: 13 pruebas sin fallos ni errores; WAR generado.
- WildFly 30.0.1.Final aislado en 28080: 37 comprobaciones HTTP correctas.
- Rutas nuevas y alias usan la misma conversación; otra sesión tiene un plan independiente.
- Nueva compra conserva el plan y actualiza el resumen. Cambio de identidad rechazado antes de guardar.
- Reiniciar el plan conserva inversiones; cerrar la sesión retira Portfolio con @Remove.
- Redespliegue con schema action none conserva las dos compras (costo 2850, ganancia 75).
- WAR verificado sin SimuladorPortfolioServiceBean. Servidor QA apagado al finalizar.

## Callbacks observados en el contenedor

    2026-09-13 18:33:21,818 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado b60ed55c-2f5b-40c7-800a-9aacd8cf1b50
    2026-09-13 18:33:22,121 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (default task-1) CompraServiceBean inicializado
    2026-09-13 18:33:22,181 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado 2ca642eb-b4ce-4ff9-bfaf-94349329a9e4
    2026-09-13 18:33:22,426 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado ae07b650-9a1d-4888-90c2-27fbb6dd92df
    2026-09-13 18:33:22,500 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio destruido b60ed55c-2f5b-40c7-800a-9aacd8cf1b50
    2026-09-13 18:33:22,505 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado 0239f8f3-aefe-4def-8e85-de5ff924ecb4
    2026-09-13 18:33:22,525 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio destruido 0239f8f3-aefe-4def-8e85-de5ff924ecb4
    2026-09-13 18:33:22,530 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio destruido 2ca642eb-b4ce-4ff9-bfaf-94349329a9e4
    2026-09-13 18:33:22,535 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio destruido ae07b650-9a1d-4888-90c2-27fbb6dd92df
    2026-09-13 18:36:32,736 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (ServerService Thread Pool -- 94) CompraServiceBean destruido
    2026-09-13 18:37:12,894 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (default task-1) Portfolio inicializado 1b8dc167-f885-4ed7-95cc-900bb21ae0a6
    2026-09-13 18:37:23,673 INFO  [ar.edu.uade.inversorar.business.PortfolioServiceBean] (ServerService Thread Pool -- 93) Portfolio destruido 1b8dc167-f885-4ed7-95cc-900bb21ae0a6

## Evidencia histórica anterior al refactor

Los registros siguientes pertenecen a la arquitectura anterior y se conservan como historial.

# Evidencia de ejecución - 13/09/2026

JDK 17.0.12, Maven 3.9.9, WildFly 30.0.1.Final, H2 de una instalación aislada en localhost. Ninguna base externa fue modificada. Se cerró el servidor al finalizar.

## Construcción

```
mvn test       -> Tests run: 10, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS
mvn package    -> BUILD SUCCESS; target/inversorar.war
mvn -Pdemo-init package -> BUILD SUCCESS
mvn -Pmysql package     -> BUILD SUCCESS
```

Se inspeccionó el persistence.xml dentro del WAR MySQL: JNDI InversorARDS y schema-generation.database.action=none. No se abrió conexión MySQL. El WAR no contiene JUnit ni Mockito. Al terminar se volvió a construir el perfil normal.

El entorno restringido inicialmente impidió resolver clases locales al compilador; la misma construcción con JDK 17 fuera de ese entorno pasó. La confianza TLS se configuró con los certificados raíz del sistema, sin desactivar verificación. Son condiciones de esta máquina, no cambios de producción.

## HTTP en el WAR final

La primera ejecución partió de carteras vacías y registró AAPL 10 x 180. Se redesplegó el WAR normal con generación de esquema none. La segunda ejecución confirmó que los 1800 invertidos anteriores seguían presentes y registró otra compra igual: costo total 3600, patrimonio 3900 y ganancia 300. El segundo usuario continuó vacío. La ruta de consulta no acepta portfolioId.

```
GET /api/portfolio/resumen: 401 (anonimo)
POST /api/compras: 403 (lector)
GET /dashboard: 200 (ana)
GET /api/portfolio/resumen: 200 (ana)
POST /api/compras: 400 (ana)
POST /api/compras: 201 (ana)
GET /api/portfolio/resumen: 200 (bruno)
GET /api/portfolio/posiciones: 200 (ana)
GET /api/portfolios/1/resumen: 404 (ana)
PUT /api/simulador/capital: 200 (ana)
PUT /api/simulador/porcentajes/ACCION: 200 (ana)
PUT /api/simulador/porcentajes/BONO: 400 (ana)
GET /api/simulador: 200 (ana)
GET /api/simulador: 200 (bruno)
GET /api/simulador: 403 (bruno)
DELETE /api/simulador: 204 (ana)
GET /api/simulador: 200 (ana)
DELETE /api/simulador: 204 (ana)
DELETE /api/simulador: 204 (bruno)
Todas las aserciones correctas.
```

El segundo GET /simulador de bruno que devuelve 403 reutiliza intencionalmente la cookie de ana: la identidad distinta no puede recuperar su conversación. Son 19 peticiones en la regresión final, con aserciones sobre sus resultados.

## Callbacks registrados por el contenedor

Extracto sin credenciales:

```
2026-09-13 17:08:22,534 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (default task-1) CompraServiceBean inicializado
2026-09-13 17:08:22,634 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado 0368e2a8-4166-4185-8137-db5f99be7b8d
2026-09-13 17:08:22,684 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado 71137e41-7651-4e47-aa16-32547b921963
2026-09-13 17:08:22,697 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido 0368e2a8-4166-4185-8137-db5f99be7b8d
2026-09-13 17:08:22,703 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado a640f5d7-564d-4746-bbc5-ca20bbe9a82f
2026-09-13 17:08:22,713 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido a640f5d7-564d-4746-bbc5-ca20bbe9a82f
2026-09-13 17:08:22,717 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido 71137e41-7651-4e47-aa16-32547b921963
2026-09-13 17:14:01,883 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (ServerService Thread Pool -- 80) CompraServiceBean destruido
2026-09-13 17:14:56,376 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (default task-1) CompraServiceBean inicializado
2026-09-13 17:14:56,468 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado 9f03e75f-6ef7-4805-837f-7e6dec80373b
2026-09-13 17:14:56,510 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado 28b643f0-2515-4c80-b20c-01ba4fac9f66
2026-09-13 17:14:56,531 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido 9f03e75f-6ef7-4805-837f-7e6dec80373b
2026-09-13 17:14:56,537 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador inicializado ba600101-b6e7-4d2e-9a20-bf50281d1bd2
2026-09-13 17:14:56,546 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido ba600101-b6e7-4d2e-9a20-bf50281d1bd2
2026-09-13 17:14:56,552 INFO  [ar.edu.uade.inversorar.business.SimuladorPortfolioServiceBean] (default task-1) Simulador destruido 28b643f0-2515-4c80-b20c-01ba4fac9f66
2026-09-13 17:16:20,477 INFO  [ar.edu.uade.inversorar.business.CompraServiceBean] (ServerService Thread Pool -- 96) CompraServiceBean destruido
```

La correlación de cada UUID confirma el cierre de la instancia stateful. El reemplazo/retiro del despliegue dispara PreDestroy del stateless. Las pruebas de `new Bean()` no se presentaron como evidencia de estos callbacks.
