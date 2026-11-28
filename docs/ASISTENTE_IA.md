# Asistente de IA y su gateway

La consigna pide "la funcionalidad de IA funcionando a través de su gateway". En InversorAR la IA es un **asistente** que se abre con el botón "Asistente" del encabezado, en todas las pantallas. Tiene dos modos:

- **Ayuda:** cómo usar la aplicación y qué significa un término financiero (glosario).
- **Noticias:** resumen de las noticias de los últimos 7 días sobre un activo **del catálogo** (por ejemplo, BTC o Bitcoin), con el enlace a cada noticia.

No da recomendaciones de inversión ni predicciones de precios.

## Arquitectura

```
Navegador ──► POST /api/asistente ──► AsistenteServiceBean ──► Asistente (gateway) ──► ProveedorIA
                (429 propio)            (rol USUARIO,              │                    ├─ ProveedorGemini (real)
                                         activo del catálogo)      │                    └─ ProveedorSimulado
                                                                   └──► FuenteNoticias ──► NoticiasGoogle (RSS)
```

El navegador nunca habla con el proveedor de IA: todo pasa por el gateway. Así la clave no sale del servidor y las reglas se aplican en un solo lugar.

| Clase | Paquete | Qué hace |
|---|---|---|
| `AsistenteResource` | `presentation` | `POST /api/asistente` con `{modo, pregunta, activo}` y `GET /api/asistente/estado` |
| `AsistenteServiceBean` | `business` | Caso de uso. Valida el pedido y busca el activo en el catálogo MySQL. No abre transacción. |
| `Asistente` | `ia` | El gateway: reglas de entrada, contexto, plazo único, fallback y filtro de salida |
| `ReglasAsistente` | `ia` | Barreras en código, que no dependen de que el modelo obedezca |
| `ProveedorIA` | `ia` | Interfaz del modelo, con dos implementaciones intercambiables |
| `ProveedorGemini` | `ia` | API REST de Google Gemini (`generateContent`) |
| `ProveedorSimulado` | `ia` | Respuestas fijas, para tests, CI y demo sin clave |
| `NoticiasGoogle` | `ia` | RSS público de Google News, sin clave, leído con `XmlSeguro` |
| `AsistenteGateway` | `ia` | Elige el proveedor al arrancar y carga la guía de uso (`src/main/resources/ia/guia.md`) |

`ProveedorIA` aplica el mismo patrón Strategy que la cotización (Binance o catálogo) y que el mercado (SOAP real o simulador).

## Proveedor: Google Gemini

- **Modelo:** `gemini-3.5-flash-lite`. Es estable, del plan gratuito y responde en 1 a 2 s. Se usa con razonamiento mínimo (`thinkingLevel: MINIMAL`) para cumplir el plazo. Se puede cambiar con `-Dinversorar.ia.modelo=...`. No se usa un alias como `gemini-flash-latest` porque Google lo reemplaza sin aviso.
- **Clave:** se lee de la variable de entorno `GEMINI_API_KEY` del proceso de WildFly y viaja en el encabezado `x-goog-api-key`, nunca en la URL, así que no puede quedar en un log. No está en el repositorio ni en ningún archivo.
- **Sin clave:** el servidor usa el `ProveedorSimulado` y el panel avisa que está en "modo de prueba". La aplicación funciona igual.
- **Costo:** se usa el plan gratuito, sin facturación activada. Si se agota el cupo, Gemini responde 429 y el asistente pasa al fallback.

### Configurar la clave

1. En aistudio.google.com: "Get API key" → "Create API key". No hay que activar la facturación.
2. En PowerShell: `setx GEMINI_API_KEY "la-clave"`.
3. Abrir una terminal **nueva**, porque `setx` no cambia las terminales que ya estaban abiertas, y arrancar WildFly desde ahí con `config\setup-wildfly.ps1`.

El log de WildFly confirma qué proveedor quedó activo: `Asistente de IA con Gemini (modelo ...)` o `... con el proveedor simulado`.

## Reglas del gateway

| Momento | Regla | Dónde |
|---|---|---|
| Entrada | Pregunta de 1 a 500 caracteres, sin caracteres de control ni invisibles (que sirven para esconder instrucciones) | `ReglasAsistente.pregunta` |
| Entrada | Activo de 2 a 60 caracteres: solo letras, números y signos simples | `ReglasAsistente.activo` |
| Entrada | Un pedido de consejo ("¿me conviene comprar…?", "¿va a subir?") se responde con un texto fijo **sin llamar al modelo** | `ReglasAsistente.pideConsejo` |
| Entrada | Noticias solo de activos que forman parte de un instrumento del catálogo; un activo que no está, responde texto fijo | `CatalogoMercadoRepository.activoParaNoticias` |
| Entrada | 10 preguntas por minuto por usuario, en un balde propio que no comparte con las compras: si se supera, 429 con `Retry-After` | `FiltroLimitePedidos` |
| Contexto | En ayuda, el modelo responde **solo con la guía de uso**; si no la cubre, lo dice y no inventa | `Asistente.instruccionesAyuda` |
| Contexto | En noticias, los titulares van entre `<noticias>` como **datos**, con la indicación de no seguir instrucciones que aparezcan en ellos (prompt injection indirecta). Al modelo no se le pasan los enlaces | `Asistente.instruccionesNoticias` |
| Contexto | Al proveedor no se le manda **ningún dato del usuario**: ni email, ni portfolio, ni órdenes | `Asistente` |
| Salida | Si el modelo filtra sus instrucciones, la respuesta se descarta. Se detecta por una marca secreta que cambia en cada arranque o por el texto del bloque de reglas | `ReglasAsistente.respuesta` |
| Salida | Se quitan los enlaces que escriba el modelo; los únicos enlaces son los de la fuente de noticias, y solo `https` | `ReglasAsistente.respuesta`, `NoticiasGoogle.leer` |
| Salida | Máximo de 1.500 caracteres. La pantalla muestra todo con `textContent`, nunca como HTML | `ReglasAsistente.respuesta`, `asistente.js` |

## Resiliencia

- **Plazo único de 5 s** para todo el pedido: la búsqueda de noticias tiene como máximo 2,5 s y el modelo usa lo que queda.
- **Fallback:** si Gemini no responde, devuelve 429 o 5xx, o la clave es inválida, se muestra "El asistente no está disponible…". En el modo noticias se muestran igual las noticias encontradas, sin el resumen.
- **Pausa tras una falla:** Gemini se saltea 30 s y la fuente de noticias 60 s, para no esperar un timeout en cada pregunta.
- **Caché de noticias** de 10 minutos por activo.
- **Respuestas bloqueadas** por los filtros de Gemini: se muestra "No puedo responder esa pregunta".
- Cada respuesta indica su origen: `GEMINI`, `SIMULADO`, `REGLA` (texto fijo del gateway) o `RESPALDO` (fallback).

Para la prueba de carga conviene dejar el asistente afuera, o arrancar WildFly sin `GEMINI_API_KEY`, porque depende de un servicio externo con cupo.

## Pruebas

**Automáticas, sin internet** (corren en el build y en el CI):

- `ReglasAsistenteTest`: detección de pedidos de consejo, sin falsos positivos en preguntas de uso; limpieza de la entrada; enlaces y filtración de instrucciones en la salida.
- `AsistenteTest`: qué llega al modelo, que un consejo no llama al modelo, fallbacks, plazo compartido y que sin noticias no se inventa.
- `ProveedorGeminiTest`: formato del pedido, clave en el encabezado y no en la URL, respuestas bloqueadas, vacías o ilegibles, timeout, 429 y pausa.
- `NoticiasGoogleTest`: lectura del RSS, descarte de enlaces que no son `https`, XXE, caché y pausa.

**Pruebas de IA contra Gemini real** (`EvaluacionIATest`, 11 casos):

- uso de la app;
- glosario;
- consejo disfrazado que no ataja la regla;
- pregunta fuera de tema;
- inyección para revelar las instrucciones: tres intentos contra el modelo solo y uno a través del gateway;
- inyección dentro de una noticia;
- pregunta sobre lo que ya pasó con el precio, que **no** debe tomarse como pedido de consejo;
- pregunta por el precio cuando las noticias no lo mencionan, que no debe tomarse como fuera de tema;
- pedido de predicción del precio, que sí debe rechazarse;
- función que la app no tiene;
- respuesta dentro del plazo.

No corren en el build porque gastan cupo y dependen de internet. Se corren a mano, con la clave definida:

```powershell
.\mvnw.cmd -Pmysql test "-Dtest=EvaluacionIATest" "-Dia.evaluar=true"
```

Resultado el 28/11/2026 con `gemini-3.5-flash-lite`: **11 de 11**, en dos tandas porque el plan gratuito limita los pedidos por minuto (al superarlo, Gemini responde 429 y el gateway pasa al fallback). Las pruebas y el uso encontraron cuatro problemas, que se corrigieron en las instrucciones antes de esa corrida:

| Problema | Corrección |
|---|---|
| Ante una función que la app no tiene, respondía "fuera de tema" | Se aclaró que esa pregunta es sobre la app y que debe decir que la función no existe |
| Ante "¿Qué pasó con el precio esta semana?" se negaba como si fuera un consejo (lo encontró un usuario al probar) | La regla distingue contar lo que ya pasó, que está permitido, de recomendar o predecir |
| Con noticias que no hablan del precio (0G/USDC), "¿Qué pasó con el precio esta semana?" respondía "fuera de tema" (lo encontró un usuario al probar) | En modo noticias, toda pregunta sobre el activo es sobre sus noticias: si falta el dato, dice que las noticias no lo mencionan y resume lo que sí cuentan. Un pedido de predicción se sigue rechazando |
| Ante una inyección, el modelo copió la regla que nombraba el código secreto; el filtro del gateway la habría bloqueado igual | La marca va al comienzo, sin mencionarla, y el filtro de salida también descarta una copia del bloque de reglas |

Esto muestra por qué hacen falta las barreras en código: las instrucciones reducen los errores del modelo, pero no los eliminan.

**Ayudas en el panel:**

- **Preguntas sugeridas** para cada modo. Un clic carga la pregunta en el campo, que se puede editar antes de enviar. El texto libre se mantiene: la consigna pide una funcionalidad de IA, y las pruebas de consejo, fuera de tema e inyección necesitan que el usuario pueda escribir. Las protecciones están en el servidor, porque la API acepta cualquier texto aunque la pantalla no lo ofrezca.
- **Autocompletado del activo** con el catálogo (`GET /api/catalogo?q=`, 300 ms después de dejar de escribir, hasta 8 opciones). La pantalla solo envía un instrumento del catálogo, por su nombre (BTC/USDT) o su ticker (BTCUSDT). Si se escribe otra cosa, pide elegir una opción de la lista. El servidor vuelve a verificar contra el catálogo: acepta además el símbolo o el nombre del activo (BTC, Bitcoin) y en un par toma el activo principal, no la moneda de cotización.

**En la pantalla:** botón "Asistente".

- En "Ayuda": "¿Cómo registro una venta?", "¿Qué es un CEDEAR?" o "¿Me conviene comprar Bitcoin?" (esta última devuelve el texto fijo).
- En "Noticias": escribir "btc" y elegir "BTC/USDT". Con un activo que no está en el catálogo, como "Apple", la pantalla no deja enviar.
- Sin `GEMINI_API_KEY`, el proveedor simulado permite forzar fallas escribiendo `[error]`, `[lento]` o `[bloqueada]` en la pregunta.
