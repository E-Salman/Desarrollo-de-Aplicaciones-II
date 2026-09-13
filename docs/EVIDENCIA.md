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
