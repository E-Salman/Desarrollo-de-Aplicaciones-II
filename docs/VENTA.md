# Funcionamiento de la venta de instrumentos

Documenta cómo se registra una venta y cómo impacta en el resumen del portfolio. Complementa [ARQUITECTURA.md](ARQUITECTURA.md) (capas y patrones) y [REQUISITOS.md](REQUISITOS.md) (RF-04/RF-08).

## Flujo end-to-end

```text
dashboard.jsp/js (modal "Vender")
        │  POST /api/ventas { ticker, cantidad, precioUnitario, fecha }
        ▼
CompraResource.vender()
        │
        ▼
VentaServiceBean.registrarVenta()
        │  resuelve la cartera (PortfolioActual) y arma historial del ticker (OperacionRepository.porPortfolioYTicker)
        │  valida cantidad/precio/fecha y que no supere lo poseído
        ▼
OperacionRepository.guardar(Operacion tipo=VENTA)
        │
        ▼
PortfolioServiceBean.obtenerResumen() (recalcula todo, incluida la ganancia realizada)
```

La respuesta, tanto en éxito (201) como en error de negocio (400), es el mismo `ResumenPortfolioDto` / `ErrorDto` que usa el flujo de compra — el front simplemente re-renderiza el resumen o muestra el mensaje de error en el modal.

## Validaciones (`VentaServiceBean`)

| Regla | Mensaje si falla |
|---|---|
| Cantidad y precio unitario > 0 | "La cantidad debe ser mayor que cero." / "El precio unitario debe ser mayor que cero." |
| Fecha obligatoria y no futura | "La fecha de venta es inválida." |
| El instrumento debe tener posición actual (compras − ventas previas) mayor a 0 | "No tenés ese instrumento en tu portfolio." |
| La cantidad a vender no puede superar la cantidad poseída | "No podés vender más cantidad de la que tenés en cartera." |

No se permiten ventas en corto: si no hay posición o la cantidad pedida excede lo poseído, la operación se rechaza con HTTP 400 y no se persiste nada (transacción `REQUIRED`, se revierte por completo ante `ReglaNegocioException`, marcada `@ApplicationException(rollback = true)`).

## Costo promedio ponderado y ganancia realizada

`PortfolioServiceBean.consolidar()` recorre las operaciones de cada ticker en orden cronológico (fecha, luego id) y mantiene dos acumuladores por posición: `cantidad` e `invertido`.

- **Compra**: `cantidad += cantidadComprada`, `invertido += total` (cantidad × precio de esa compra).
- **Venta**: se calcula el precio promedio vigente (`invertido / cantidad` en ese momento), y con eso:
  - `costoVendido = cantidadVendida × precioPromedioVigente`
  - `gananciaRealizada += totalVenta − costoVendido` (esto es lo que efectivamente se ganó o perdió en esa venta puntual)
  - `invertido -= costoVendido`
  - `cantidad -= cantidadVendida`

Si al terminar de procesar todas las operaciones de un ticker `cantidad` quedó en 0 (se vendió todo), la posición **no aparece** en `posiciones`, pero su ganancia realizada ya quedó sumada al acumulador global.

### Por qué el precio promedio y no el precio de venta

`invertido` representa el costo de lo que sigue en cartera. Si se descontara al precio de venta en lugar del costo promedio, una venta con ganancia "inflaría" artificialmente el capital invertido restante (o lo desinflaría una venta con pérdida), distorsionando el `precioPromedio` y el `rendimiento` de lo que queda invertido.

## Qué ve cada sección del dashboard

- **"Mis inversiones" (por posición)**: `rendimiento = actual − invertido` de **lo que queda en cartera hoy**. Es ganancia potencial/no realizada; al vender una parte, tanto `invertido` como `actual`/`rendimiento` bajan proporcionalmente porque cae la cantidad remanente.
- **KPI "Ganancia total" del portfolio**: `gananciaTotal = (patrimonioTotal − capitalInvertido) + gananciaRealizada`. Es no realizada + realizada, así que la ganancia de una venta ya cerrada no desaparece del total aunque la posición ya no se liste.

## Ejemplo numérico

Comprás 10 AAPL a $100 (invertido = $1.000, precioPromedio = $100). AAPL cotiza a $150 (patrimonio = $1.500, ganancia no realizada = $500).

Vendés 4 AAPL a $150:

- `precioPromedioVigente` al momento de la venta = $100.
- `costoVendido` = 4 × $100 = $400.
- `gananciaRealizada` de esta venta = 4 × $150 − $400 = **$200**.
- Posición resultante: `cantidad` = 6, `invertido` = $1.000 − $400 = $600.
- `actual` = 6 × $150 = $900 → `rendimiento` (no realizado) = $900 − $600 = **$300**.
- KPI Ganancia total = $300 (no realizado) + $200 (realizado) = **$500** — coincide con la ganancia total original antes de vender nada.

## Contrato API

```json
POST /inversorar/api/ventas
{
  "ticker": "AAPL",
  "cantidad": 4,
  "precioUnitario": 150,
  "fecha": "2026-09-10"
}
```

- `201 Created` → devuelve el `ResumenPortfolioDto` actualizado.
- `400 Bad Request` → `{ "mensaje": "..." }` con el motivo del rechazo (ver tabla de validaciones).

## UI (`dashboard.jsp` / `dashboard.js`)

- El botón "− Vender" abre un modal cuyo `<select>` sólo lista instrumentos con `cantidad > 0` en el último resumen cargado (no el catálogo completo).
- Al elegir un instrumento, el campo Cantidad recibe un `max` igual a lo poseído y el Precio se precarga con la cotización actual (`actual / cantidad` de la posición).
- El link "Vender todo" carga la cantidad poseída completa en el campo, sin enviar el formulario — se puede seguir editando antes de confirmar.
- Al confirmar, se hace `POST /api/ventas`; con éxito se re-renderiza el resumen y se cierra el modal, con error se muestra el mensaje dentro del modal sin cerrarlo.
