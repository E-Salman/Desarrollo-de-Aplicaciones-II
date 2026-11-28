# Guía de uso de InversorAR (contexto del asistente)

InversorAR es una aplicación web para registrar las inversiones que hace el usuario y seguir cuánto valen.
Las compras y ventas que se registran no mueven dinero real ni descuentan saldo de una billetera.

## Cuenta
- Crear cuenta: en la pantalla de inicio de sesión, ir a registrarse. Se piden nombre, apellido, email y una
  contraseña de entre 8 y 128 caracteres, escrita dos veces ("Confirmar contraseña").
- Iniciar sesión: email y contraseña. El botón del ojo muestra u oculta la contraseña.
- Si se equivoca muchas veces seguidas, la aplicación pide esperar unos segundos antes de volver a intentar
  (protección contra robo de cuentas).
- Olvidé mi contraseña: desde el inicio de sesión, ir a recuperar la contraseña, ingresar el email y seguir el enlace
  "Restablecer contraseña".
- Por seguridad la sesión vence; cuando pasa, aparece "Tu sesión venció. Iniciá sesión nuevamente.".
- Cerrar sesión: menú de usuario (botón "#" arriba a la derecha) → "Cerrar sesión".

## Menú principal (arriba)
Dashboard, Portfolio, Favoritos, Historial y Simulador. A la derecha están los botones "+ Comprar" y "- Vender" y el
menú de usuario, que tiene "Transacciones" (con la cantidad de órdenes pendientes) y "Cerrar sesión".

## Dashboard ("Mi Portfolio")
- Arriba: "Capital invertido" (lo que se pagó), "Patrimonio total" (lo que vale hoy) y "Ganancia total" (la diferencia).
- "Distribución": gráfico de cómo se reparte el portfolio entre los instrumentos.
- "Mis inversiones": cada posición con cantidad, precio de compra promedio, valor actual y rendimiento (en dinero y en
  porcentaje).
- El precio de las criptomonedas se toma en vivo de Binance ("Precio en vivo (Binance)"). Si Binance no responde, se usa
  el último cierre guardado ("Binance no disponible · último cierre").

## Comprar
1. Botón "+ Comprar".
2. Elegir el "Tipo de instrumento" (Todos, Favoritos, Acciones, Bonos, CEDEARs, Criptomonedas o Monedas) y el
   "Instrumento". Hoy el catálogo tiene sobre todo pares de criptomonedas (por ejemplo BTCUSDT: Bitcoin cotizado en USDT).
3. "Consultar precios e historial" abre el catálogo: se busca por nombre o símbolo, se ve el último cierre y el
   historial (hasta 90 registros) y se puede elegir "Comprar" desde ahí.
4. Completar "Cantidad", "Precio por unidad" y "Fecha de compra". El "Total invertido" se calcula solo.
5. "Agregar al portfolio".
- Solo se pueden comprar instrumentos que tengan precio y una moneda identificada.
- La cantidad y el precio tienen que ser mayores a cero.

## Vender
1. Botón "- Vender".
2. Elegir el "Instrumento de tu portfolio". "Vender todo" completa la cantidad que se tiene.
3. Completar "Cantidad", "Precio por unidad" y "Fecha de venta", y tocar "Registrar venta".
- No se puede vender más de lo que se tiene. Si todavía no hay instrumentos, primero hay que registrar una compra.

## Órdenes y Transacciones
- Cada compra o venta se envía como una orden: aparece el aviso "Orden #N ... enviada" y queda "Pendiente" hasta que
  el mercado la ejecuta, normalmente en unos segundos. La pantalla se actualiza sola.
- Para verlas: menú de usuario → "Transacciones" (o "Historial" en el menú principal). Hay tres pestañas:
  - "Pendientes": se pueden "Cancelar".
  - "Completadas": ya impactaron en el portfolio.
  - "Rechazadas": muestran el motivo (por ejemplo, tenencia insuficiente o rechazo del mercado). Se pueden
    "Enviar de nuevo" o "Eliminar".
- Estados posibles: Pendiente, Completada, Rechazada, Error técnico (el mercado no respondió; se puede reenviar),
  Cancelada y Eliminada.
- Si se toca dos veces el botón de compra o se corta internet, la aplicación evita registrar la orden dos veces.

## Favoritos
- En "Favoritos", el buscador "Buscar instrumentos para agregar..." permite encontrar instrumentos y marcarlos con
  "Agregar a favoritos"; "Quitar de favoritos" los saca.
- La lupa filtra la lista de favoritos. Los que ya están en el portfolio dicen "En cartera" y muestran "Mi posición".
- En la compra, el tipo "Favoritos" muestra solo los instrumentos marcados.

## Simulador
- Sirve para planificar cómo repartir un capital entre tipos de inversión. No registra compras ni modifica la cartera,
  y no proyecta ganancias.
- Se elige la "Moneda de referencia" (ARS o USD), el "Capital a distribuir" y el porcentaje para cada tipo. El total
  no puede superar el 100 % y se puede dejar una parte sin asignar.
- "Aplicar simulación" guarda el plan; "Reiniciar" lo borra. El plan se conserva durante la sesión y se reinicia al
  cerrar sesión. Cambiar de ARS a USD cambia la referencia, sin convertir el capital.

## Asistente
- "Ayuda": preguntas sobre cómo usar InversorAR y términos financieros.
- "Noticias": resumen de noticias de los últimos 7 días sobre un activo del catálogo, con el enlace a cada noticia.
- No da recomendaciones de inversión.

## Glosario
- Acción: parte del capital de una empresa.
- Bono: deuda que emite un gobierno o una empresa; paga intereses (cupones) y devuelve el capital al vencimiento.
- CEDEAR: certificado que se negocia en la bolsa argentina, en pesos, y representa acciones de empresas del exterior.
- Criptomoneda: moneda digital, como Bitcoin (BTC) o Ethereum (ETH).
- Par: dos activos que se intercambian; en BTCUSDT, el precio de 1 BTC se expresa en USDT.
- Stablecoin (USDT, USDC): criptomoneda que busca valer lo mismo que un dólar.
- Ticker o símbolo: código corto que identifica un instrumento (BTCUSDT).
- Cotización: precio actual de un instrumento. Último cierre: el último precio registrado en el historial.
- Precio promedio de compra: lo que se pagó en promedio por unidad, considerando todas las compras.
- Capital invertido, patrimonio y ganancia: lo pagado, lo que vale hoy y la diferencia entre ambos.
- Diversificar: repartir el dinero entre distintos instrumentos para no depender de uno solo.
- Orden: pedido de compra o venta. Pendiente: todavía no se ejecutó. Rechazada: el mercado no la aceptó.
- Dólar MEP: dólar que se obtiene comprando y vendiendo bonos en la bolsa argentina, de forma legal.
