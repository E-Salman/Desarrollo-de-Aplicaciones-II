# Pruebas y demo reproducible

## Unitarias

`mvn test`: 21 pruebas JUnit/Mockito en ComponentesTest y PortfolioSesionTest, sin fallos ni errores. `mvn package`: BUILD SUCCESS y WAR generado. Java 17, Maven 3.9.9. También se probó el empaquetado `demo-init`. El perfil `mysql` también se desplegó y verificó contra la importación local del SQL del equipo.

Cobertura: promedio ponderado, vacío, pérdida, Strategy sustituible, consulta por identidad, compra y total, entradas inválidas sin persistencia, simulador aislado y copias defensivas, suma de porcentajes y resolución por principal/rol. Se agregaron casos para separación de monedas, precios de diez decimales y rechazo de compras sin datos suficientes. Estas pruebas no reemplazan las del contenedor.

## Integración MySQL vigente

38 verificaciones HTTP/SQL correctas: catálogo completo, búsqueda, históricos, fecha sin desplazamiento horario, compra válida, validaciones, promedio ponderado, totales por moneda, identidad/rol y persistencia al cerrar la sesión. Se comprobó además que las compras sobreviven a un redespliegue explícito del WAR. El SQL original conservó sus conteos; se limpiaron solamente los registros de las identidades QA. La aplicación local usa MySQL en 18080.

La comprobación de navegador más reciente verificó que Comprar abre directamente el formulario compacto, con Acciones, Bonos, CEDEARs y Monedas visibles pero vacíos, y 1.365 instrumentos bajo Criptomonedas. El catálogo y los históricos son opcionales. El script de abajo es específico para la demo H2: usa AAPL y precios ficticios, por lo que no corresponde ejecutarlo sin adaptar contra MySQL.

Para probar MySQL en otra máquina: seguir MYSQL.md, usar cuentas de prueba separadas, registrar un instrumento con precio y moneda disponibles, comprobar la fila en operaciones, cerrar la sesión y volver a consultar. No usar las cuentas ni las compras reales como datos de pruebas destructivas. El extracto de resultados y callbacks está en EVIDENCIA.md.

## Integración histórica H2

Se desplegó el WAR en WildFly 30.0.1.Final con H2 vacío, usando un servidor separado en localhost:28080. Se crearon cuentas efímeras de prueba fuera del repositorio. No se usó la BD del equipo.

La ejecución posterior al refactor pasó las 37 comprobaciones HTTP. Se redesplegó además el WAR normal con generación de esquema none y se verificaron USD 2.850 invertidos y USD 75 de ganancia, conservando las dos compras. El servidor QA se apagó al finalizar; no se reemplazó la app de demostración que ya estaba abierta en 18080.

Para reproducir, preparar una instancia de WildFly nueva, asignar con add-user los usuarios `ana` y `bruno` al grupo `USUARIO`, y `lector` al grupo `LECTOR`. Desplegar `mvn -Pdemo-init package` sobre ExampleDS vacío. Definir las variables de entorno `SMOKE_ANA_PASSWORD`, `SMOKE_BRUNO_PASSWORD`, `SMOKE_LECTOR_PASSWORD` y opcionalmente `SMOKE_BASE_URL` (por defecto http://127.0.0.1:8080/inversorar), sin guardar sus valores en Git. Ejecutar:

```powershell
python scripts/smoke.py
```

El script crea dos compras y requiere portfolios vacíos. No repetir sobre datos reales ni interpretar su condición inicial como un pedido de borrado. Comprueba:

- 401 sin autenticación y 403 de compra con rol incorrecto.
- Dashboard 200, portfolio vacío y compra inválida 400.
- Compra AAPL 10 x 180: 201, costo 1800, patrimonio 1950, ganancia 150.
- Otra cuenta mantiene su cartera vacía; posiciones muestran promedio y 8,3333%.
- Ruta vieja con ID responde 404.
- Simulador conserva capital y porcentajes, calcula 400000 para 40% de 1000000.
- Distribución que supera 100% devuelve 400 sin alterar el estado previo.
- Otra sesión no comparte capital; otra identidad usando la misma cookie no accede (403).
- Las rutas /portfolio/simulacion y el alias /simulador comparten el mismo estado.
- Una nueva compra actualiza las posiciones manteniendo el capital simulado.
- Otra sesión de la misma cuenta ve las inversiones guardadas, con un plan propio.
- Cambiar de identidad en la misma cookie no permite leer, comprar ni cerrar la sesión ajena.
- DELETE /portfolio/simulacion reinicia el plan sin borrar compras ni invalidar la sesión.
- DELETE /portfolio/sesion devuelve 204, retira el EJB y una nueva conversación empieza vacía, con las compras conservadas.

## Evidencia de ciclo de vida

En standalone/log/server.log filtrar `CompraServiceBean inicializado`, `CompraServiceBean destruido`, `Portfolio inicializado` y `Portfolio destruido`. Los identificadores de Portfolio permiten correlacionar creación y cierre. La destrucción de Compra se observa al retirar o reemplazar el despliegue, no tras cada compra.

Las evidencias sanitizadas de esta ejecución están en `EVIDENCIA.md`. Se verificó además que redesplegar el WAR normal (schema action none) conserva las operaciones en la misma instancia H2. H2 en memoria no asegura conservar datos al apagar el servidor.

## Pendiente fuera de esta rama

Integrar Registro/Login (Zoe) y Venta (Joaco); mantener los enlaces con usuarios, órdenes y movimientos al incorporar esas funciones. Completar casos de venta parcial/total, concurrencia de stock, costo remanente y ganancias realizadas después de integrar esos componentes. El proyecto define cuatro componentes: Compra, Venta, Portfolio y Registro/Login. La consigna pide al menos tres desplegados; la integración de los cuatro no se declara completa aquí. Para Registro/Login, verificar alta válida, usuario duplicado, credenciales inválidas, rol, vínculo con portfolio y cierre/cambio de sesión sin reutilizar otra identidad.

## Registro de órdenes

21 pruebas unitarias en la última construcción y 20 verificaciones HTTP/SQL específicas de órdenes. Se verificó rechazo de cuentas sin usuario vinculado; creación atómica de orden, detalle y movimiento; precisión y moneda; ausencia de doble conteo; aislamiento entre usuarios y persistencia tras cerrar sesión. Un trigger temporal limitado al portfolio QA forzó un error en la escritura de operaciones: la transacción revirtió las dos inserciones anteriores. El trigger se retiró inmediatamente y se limpiaron sólo los registros QA.

La migración de compras previas se ejecutó dos veces: la segunda encontró cero pendientes. Se comprobó que los importes originales y el resumen se conservaron. El script de estructura también fue ejecutado en una base temporal vacía, con 11 tablas y 13 FK.
