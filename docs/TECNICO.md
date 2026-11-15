# InversorAR — Segunda entrega parcial

## 1. Alcance y arquitectura

### Objetivo de la entrega

InversorAR permite registrar compras y ventas de inversiones y consultar su efecto en la cartera personal. El flujo central de esta entrega es una orden que empieza en el dashboard, se acepta mediante la API, pasa por una cola real y termina en una posición actualizada. Las operaciones registran inversiones declaradas por el usuario. El catálogo MySQL aporta instrumentos y precios históricos.

### Capas y dependencias

Presentación: JSP, JavaScript y recursos JAX-RS. La interfaz envía la solicitud y consulta el estado. Los recursos convierten solicitudes y resultados a HTTP y JSON.

Negocio: interfaces EJB para Compra, Venta, Órdenes y Portfolio. Los servicios validan el pedido, resuelven al propietario autenticado y coordinan transacciones. El ejecutor JMS procesa órdenes previamente aceptadas.

Datos: JPA y repositorios sobre el datasource InversorARDS. MySQL conserva usuarios, portfolios, órdenes, detalles y movimientos. El catálogo tiene un esquema propio y consultas encapsuladas.

### Entorno de despliegue

Se empaqueta un WAR Maven con Jakarta EE 10 y el perfil mysql. WildFly Full provee el contenedor, las transacciones JTA y Artemis. El mismo proyecto conserva H2 para pruebas y una demo síncrona aislada. La evidencia de mensajería se obtiene con MySQL.

### Material de la cátedra

Se tomaron como base el TP Integrador y su Anexo, la consigna específica del 16/11 y las clases sobre capas, SOA, mensajería, REST, seguridad y patrones. El diseño del equipo se conserva en Arquitectura_Integracion_SOA.docx y Diseno_Flujo_Asincronico.docx.

## 2. Componentes y ciclo de vida

### Compra y Venta

CompraResource expone POST /api/compras y POST /api/ventas. CompraService y VentaService validan fecha, cantidad, precio e instrumento. Venta verifica la cantidad disponible. Ambos servicios son @Stateless y el contenedor puede reutilizar sus instancias. Sus callbacks @PostConstruct y @PreDestroy registran el ciclo de vida.

### Órdenes

OrdenResource permite consultar, cancelar, reintentar o descartar según el estado. OrdenServiceBean es @Stateless. El repositorio filtra por propietario y usa bloqueo de fila para coordinar transiciones concurrentes. Orden y OrdenDetalle conservan el pedido. El movimiento asociado representa la ejecución.

### Portfolio y simulación

PortfolioServiceBean es @Stateful. PortfolioSesion mantiene una referencia por sesión HTTP y valida que el principal siga siendo el mismo. El bean conserva el plan de simulación y calcula posiciones a partir de los movimientos persistidos. La simulación se reinicia con otra sesión. El cierre de sesión destruye la conversación mediante @Remove.

### Evidencia del contenedor

El log local registró la inicialización de Compra, Venta y Portfolio durante la prueba HTTP. También registró la destrucción de la misma instancia de Portfolio luego del logout. La prueba del simulador verificó que dos sesiones conservan planes independientes y que un nuevo login inicia un plan vacío.

## 3. Patrones y decisiones

### State en la entidad central

Cada valor persistido de EstadoOrden implementa las transiciones que admite. Orden delega la decisión y aplica el nuevo estado y motivo. Los valores existentes se mantienen por nombre. Los estados sin una transición habilitada rechazan esa acción mediante TransicionOrdenInvalida, que JAX-RS convierte en 409 Conflict.

### Transiciones

PENDIENTE puede pasar a COMPLETADA, RECHAZADA, ERROR o CANCELADA. RECHAZADA y ERROR pueden volver a PENDIENTE o pasar a DESCARTADA. COMPLETADA, CANCELADA y DESCARTADA son finales. El ejecutor conserva su comportamiento idempotente ante una entrega repetida.

### Patrones que ya estructuran la aplicación

Service Facade concentra el acceso a la cartera y la simulación en PortfolioService. Repository encapsula la persistencia y las consultas del dominio. Strategy separa el cálculo de posiciones de la obtención de su cotización mediante CotizacionStrategy. La cola punto a punto separa al productor del ejecutor.

### ADR

Los seis ADR en docs/adr documentan State, Service Facade, Repository, Strategy de cotización, cola JMS y JWT. Cada uno incluye contexto, decisión, alternativas, consecuencias y evidencia. La justificación de los patrones sigue el formato trabajado en la Clase 12.

## 4. Seguridad y contrato REST

### Emisión y validación de JWT

El login valida credenciales mediante Jakarta Security y el repositorio de usuarios. Las contraseñas existentes usan PBKDF2-HMAC-SHA256 con sal y 600.000 iteraciones. Un login correcto rota la sesión y emite un JWT HS256 de 15 minutos. Incluye emisor, audiencia, sujeto, identificador y roles. La firma se verifica en cada petición con java-jwt.

### Sesión de navegador y autorización

La cookie INVERSORAR_ACCESS usa HttpOnly, SameSite=Strict y Secure sobre HTTPS. JSESSIONID permite comprobar el identificador del JWT y acceder a la conversación del Portfolio. El logout borra la cookie e invalida la sesión, de modo que repetir ambas cookies tras salir devuelve 401. La clave de firma se genera para el despliegue local.

El contenedor recibe el sujeto y los grupos del token verificado. web.xml protege las rutas privadas y los EJB sensibles mantienen @RolesAllowed(USUARIO). Todas las cuentas del alta pública reciben USUARIO. Las mutaciones de órdenes resuelven al propietario en el servidor.

### Resultados observables

La API devuelve 401 si no puede autenticar, 403 al autenticado sin el rol requerido, 404 para una orden inexistente o ajena, 409 para una transición inválida y 400 para una solicitud o regla de negocio inválida. La prueba unitaria verifica también el rechazo criptográfico de un token vencido o sin firma.

## 5. Mensajería y persistencia

### Aceptación de una orden

Compra o Venta valida la solicitud y escribe la orden PENDIENTE con su detalle. ProductorOrdenes publica en la cola ordenes dentro de la transacción de aceptación. La API devuelve 202 con el identificador que permite consultar el resultado.

### Ejecución y consistencia

EjecutorOrdenesMDB recibe el mensaje, recupera el pedido de MySQL y bloquea la fila. Ejecuta solo si continúa PENDIENTE. Completa la orden con su movimiento o la rechaza con motivo. El movimiento queda vinculado al detalle con una restricción única. Si el pedido es una venta, se vuelve a verificar la tenencia efectiva en la ejecución.

### Reentregas y cola de errores

La configuración de WildFly define tres entregas y una demora de cinco segundos. Las fallas técnicas que agotan esos intentos pasan a DLQ.ordenes. OrdenesFallidasMDB puede marcar ERROR si la orden todavía está pendiente. Las pruebas unitarias cubren delegación del consumidor, órdenes repetidas y marcado de errores.

### Evolución de la base

config/mysql-ordenes-estado.sql agrega las columnas necesarias si faltan y recupera fechas disponibles de los movimientos anteriores. Los registros existentes conservan su estado. Se aplica después de las migraciones de órdenes y vínculos. El perfil mysql no ejecuta DDL al desplegar.

## 6. Pruebas y demostración

### Preparación

Configurar WildFly Full con Java 21, datasource MySQL, dominio de seguridad y colas. Aplicar mysql-ordenes-estado.sql sobre la base de demo. Compilar y probar con mvn -Pmysql clean package. Desplegar target/inversorar.war. El README y ENTREGA_2.md incluyen el procedimiento.

### Demostración en vivo

Registrar una cuenta, iniciar sesión y comprar 0,002 BTCUSDT. Mostrar 202 y la orden enviada a Artemis. Confirmar el estado COMPLETADA y el cambio en la cartera. Vender 0,001 y comprobar que queda 0,001. Intentar cancelar la compra completada y observar 409. Usar una segunda cuenta para verificar aislamiento.

### Evidencia reproducible

smoke_segunda_entrega.mjs ejecuta el flujo y verifica la respuesta de la orden, la posición, el aislamiento, JWT ausente o alterado y revocación tras logout. En la primera corrida local las órdenes 6 y 7 fueron recibidas por el MDB y completadas en el primer intento. smoke_simulador.mjs verificó aislamiento, precisión, validaciones y ciclo de la conversación.

### Seguimiento del equipo

ENTREGA_2.md relaciona la consigna con las pruebas y deja identificados los puntos que requieren documentación externa. Falta conciliar las historias de diseño aprobadas, la devolución docente y el tablero Jira con los integrantes que tienen acceso a esos materiales.
