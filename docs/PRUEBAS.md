# Pruebas y demo reproducible

## Unitarias

`mvn test`: diez pruebas JUnit/Mockito en ComponentesTest, sin fallos ni errores. `mvn package`: BUILD SUCCESS y WAR generado. Java 17, Maven 3.9.9. También se probó el empaquetado `demo-init`. El perfil `mysql` se verifica como configuración del WAR, sin conexión a la BD del equipo.

Cobertura: promedio ponderado, vacío, pérdida, Strategy sustituible, consulta por identidad, compra y total, entradas inválidas sin persistencia, simulador aislado y copias defensivas, suma de porcentajes y resolución por principal/rol. Estas pruebas no reemplazan las del contenedor.

## Integración real

Se desplegó el WAR en WildFly 30.0.1.Final con H2 vacío, usando un servidor separado en localhost:18080. Se crearon cuentas efímeras de prueba fuera del repositorio. No se usó la BD del equipo.

Para reproducir, preparar una instancia de WildFly nueva, asignar con add-user los usuarios `ana` y `bruno` al grupo `USUARIO`, y `lector` al grupo `LECTOR`. Desplegar `mvn -Pdemo-init package` sobre ExampleDS vacío. Definir las variables de entorno `SMOKE_ANA_PASSWORD`, `SMOKE_BRUNO_PASSWORD`, `SMOKE_LECTOR_PASSWORD` y opcionalmente `SMOKE_BASE_URL` (por defecto http://127.0.0.1:8080/inversorar), sin guardar sus valores en Git. Ejecutar:

```powershell
python scripts/smoke.py
```

El script crea una compra y requiere portfolios vacíos. No repetir sobre datos reales ni interpretar su condición inicial como un pedido de borrado. Comprueba:

- 401 sin autenticación y 403 de compra con rol incorrecto.
- Dashboard 200, portfolio vacío y compra inválida 400.
- Compra AAPL 10 x 180: 201, costo 1800, patrimonio 1950, ganancia 150.
- Otra cuenta mantiene su cartera vacía; posiciones muestran promedio y 8,3333%.
- Ruta vieja con ID responde 404.
- Simulador conserva capital y porcentajes, calcula 400000 para 40% de 1000000.
- Distribución que supera 100% devuelve 400 sin alterar el estado previo.
- Otra sesión no comparte capital; otra identidad usando la misma cookie no accede (403).
- DELETE devuelve 204, destruye el EJB y una nueva conversación comienza vacía.

## Evidencia de ciclo de vida

En standalone/log/server.log filtrar `CompraServiceBean inicializado`, `CompraServiceBean destruido`, `Simulador inicializado` y `Simulador destruido`. Los identificadores del simulador permiten correlacionar creación y cierre. La destrucción de Compra se observa al retirar o reemplazar el despliegue, no tras cada compra.

Las evidencias sanitizadas de esta ejecución están en `EVIDENCIA.md`. Se verificó además que redesplegar el WAR normal (schema action none) conserva las operaciones en la misma instancia H2. H2 en memoria no asegura conservar datos al apagar el servidor.

## Pendiente fuera de esta rama

Probar migración al esquema MySQL del compañero, login definitivo y Venta. Completar casos de venta parcial/total, concurrencia de stock, costo remanente y ganancias realizadas después de integrar esos componentes. La entrega general de tres componentes no se declara completa aquí.
