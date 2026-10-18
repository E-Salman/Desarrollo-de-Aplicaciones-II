# Simulador

Pantalla `/simulador`, con autenticación y rol USUARIO. Planifica la distribución
de un capital de referencia entre todos los valores de `TipoInstrumento`:
ACCION, CRIPTO, BONO, CEDEAR y MONEDA. No proyecta rendimientos, convierte monedas,
elige instrumentos ni registra compras o ventas.

El usuario elige ARS o USD como moneda de referencia. Capital, importes y disponible
se muestran con su código. La moneda se aplica y conserva junto con el plan en la
sesión; reiniciar vuelve a ARS. Cambiarla no convierte valores ni consulta un tipo
de cambio: 1000 ARS pasa a representar un plan de 1000 USD si se elige USD.
No se mezclan cotizaciones de los pares del catálogo.
El catálogo MySQL recibido contiene instrumentos CRIPTO. Un activo FIAT de la
tabla `activos` identifica una moneda de un par, no una categoría adicional.
Las categorías sin instrumentos siguen disponibles para planificar y se indican
como tales. La consulta a `/api/instrumentos` informa la disponibilidad; si falla,
se puede seguir simulando. No se agregaron tipos que el negocio no admite.

## Funcionamiento

1. Ingresar capital entre 0 y 999999999999.99, con hasta dos decimales.
2. Distribuir de 0 a 100 por ciento entre los tipos, con hasta cuatro decimales.
3. La vista previa muestra importes y capital sin asignar; una suma mayor a 100
   bloquea la aplicación del plan. Cero y una distribución parcial son válidos.
4. Aplicar simulación conserva el plan completo en la conversación Portfolio de
   la sesión. Recargar o navegar conserva lo aplicado; los cambios sin aplicar
   son solamente una vista previa.
5. Reiniciar elimina solo el plan de esa sesión. Cerrar sesión lo descarta.

Si se intenta navegar a otra pantalla con cambios sin aplicar, se pide confirmar
la salida. Cancelar conserva el formulario, incluidos datos inválidos. Al recargar,
cerrar la pestaña o usar Atrás, el navegador muestra su aviso estándar si detecta
cambios pendientes y hubo interacción del usuario; el texto depende del navegador.
No hay avisos si el formulario coincide con el plan aplicado. Una operación en
curso también activa el aviso. Los datos no aplicados no se guardan automáticamente.

La pantalla usa aritmética entera escalada con BigInt. El backend usa BigDecimal.
Con dos decimales de capital y cuatro de porcentaje, los importes pueden tener
hasta ocho decimales. Se muestran completos para que, por ejemplo, 0.01 repartido
en cinco partes iguales produzca cinco importes de 0.002, sin crear ni perder capital.
La suma de importes y disponible es igual al capital.

## Integración

- GET `/api/portfolio/simulacion` y DELETE de esa ruta se reutilizan sin cambios.
- PUT `/api/portfolio/simulacion/plan` acepta:

```json
{"capital":"100000","moneda":"ARS","porcentajes":{"ACCION":"30","CRIPTO":"20","BONO":"20","CEDEAR":"15","MONEDA":"10"}}
```

Responde con `SimulacionDto`: moneda, capital, porcentajes, importes y disponible.
El campo moneda se agrega sin cambiar los anteriores; las llamadas antiguas que
lo omiten conservan la moneda del plan (ARS en una sesión nueva).
Valida la entrada completa antes de reemplazar el estado, devuelve 400 para
reglas inválidas o tipos desconocidos y conserva el plan anterior ante rechazo.
No realiza escrituras JPA ni utiliza mensajería. Los endpoints anteriores de
capital y porcentajes mantienen sus contratos.

El servlet y los recursos nuevos usan la sesión y la seguridad existentes.
No se modifica el encabezado compartido: ya incluía el enlace Simulador. Los
botones globales Comprar/Vender se incluyen mediante el fragmento existente.
Los estilos nuevos están acotados a esta pantalla; no se cambian Dashboard,
Favoritos, navegación, autenticación, esquema MySQL ni configuración de broker.
Se agrega un método al contrato PortfolioService y su implementación para
actualizar el plan en una única invocación, sin estados parciales entre requests.

## Verificación

```powershell
mvn -Pmysql test package
node scripts/smoke_simulador.mjs http://127.0.0.1:8080/inversorar
```

El smoke necesita el WAR desplegado y una base local de pruebas; crea dos cuentas
aleatorias, sin registrar operaciones. Verifica todos los tipos, precisión,
límites, reemplazo completo, rechazo sin modificar estado, identidad separada,
recarga, reinicio, logout/reingreso y cartera sin cambios. Las pruebas unitarias
adicionales están en `SimulacionPlanTest`. No ejecutar los smokes contra producción.

El enlace de logout del encabezado compartido conserva su problema previo
(GET frente al POST que exige el servidor); este cambio no lo modifica. El smoke
usa el cierre POST con CSRF para verificar el ciclo de vida real del plan.
