# Decisiones técnicas de Compra, Venta, Portfolio y Registro/Login

Estado de mysql_componente al 13 de septiembre de 2026. Este texto actualiza la arquitectura; el PDF anterior conservado en el repositorio es histórico. El Word del equipo debe reflejar estos cambios antes de la entrega.

## Componentes del proyecto

Se definen cuatro componentes: **Compra, Venta, Portfolio y Registro/Login**. Registro y Login forman un único componente a cargo de Zoe. La consigna exige al menos tres componentes completamente implementados y desplegados; ese mínimo no limita el proyecto a tres. En esta rama están integrados Compra y Portfolio. Venta (Joaco) y Registro/Login (Zoe) siguen pendientes de integración.

| Componente | EJB | Responsabilidad | Patrones aplicados |
|---|---|---|---|
| Compra | Stateless | Listar instrumentos y validar/registrar compras | Repository/DAO |
| Portfolio | Stateful | Consultar posiciones y rendimientos, mantener un plan temporal de distribución | Service Facade, Repository/DAO y Strategy |
| Venta (rama joaco, 88f85f5) | OperacionServiceBean Stateless; pendiente de integrar | Validar disponibilidad y registrar ventas | Repository/DAO y OperacionStrategy con VentaStrategy |
| Registro/Login (Zoe) | Pendiente de verificar con su implementación | Alta de usuarios, autenticación, cierre de sesión y vínculo usuario-portfolio | Pendientes de verificar; no se atribuyen patrones sin revisar el código |

La simulación es una función de Portfolio. No existe un SimuladorPortfolioServiceBean independiente. Los recursos HTTP de consulta y simulación representan entradas al mismo componente y a la misma conversación por sesión.

## Arquitectura en capas

Presentación: JSP, JavaScript, Servlet y recursos JAX-RS. Negocio: interfaces locales, EJB, reglas y DTOs. Datos: entidades y Repository/DAO con JPA/JTA. El servidor inyecta dependencias y administra las transacciones. No se construyen EJB con new en producción.

CompraResource valida primero que la conversación pertenezca al principal actual. Después CompraService valida ticker, cantidad, precio y fecha y registra una Orden, su OrdenDetalle y una Operacion enlazada en el portfolio resuelto por PortfolioActual. En MySQL la cartera debe estar vinculada a usuarios; ninguna de esas identidades se toma de un ID enviado por el navegador. Finalmente obtiene el resumen del PortfolioService de esa sesión y responde 201. Escritura y consulta son llamadas distintas: una falla posterior de lectura no revierte una compra confirmada. La idempotencia de reintentos queda para una integración posterior.

## Compra stateless

Compra no retiene información de un cliente entre invocaciones. @Stateless permite al contenedor reutilizar instancias. @PostConstruct y @PreDestroy escriben logs de creación y destrucción; estos callbacks no ocurren en cada compra.

registrarCompra usa REQUIRED. Los repositories participan de la transacción del contenedor. OrdenRepository usa MANDATORY: cabecera, detalle y movimiento se confirman juntos; si falla uno se revierten los tres. ReglaNegocioException es @ApplicationException(rollback=true). Cantidad y precio deben ser positivos; fecha válida y no futura; ticker existente. Se valida precisión de persistencia y el total redondeado a diez decimales. Los errores de negocio responden HTTP 400.

## Portfolio stateful

PortfolioServiceBean conserva un capital hipotético y un mapa de porcentajes por TipoInstrumento. Esto permite definir capital, asignar porcentajes en sucesivas llamadas y consultar los importes resultantes. No conserva posiciones ni entidades como estado conversacional: obtenerResumen y obtenerPosiciones leen la base en cada llamada con REQUIRED. Comprar actualiza las posiciones sin perder el plan.

PortfolioSesion es CDI SessionScoped y mantiene una referencia al EJB por sesión HTTP. CompraResource, PortfolioResource y SimulacionPortfolioResource utilizan ese holder. Se rechaza con 403 que otra identidad use la misma conversación, incluso antes de persistir una compra. Dos sesiones de una misma cuenta ven su misma cartera en BD y planes temporales independientes.

La planificación usa NOT_SUPPORTED: no necesita una transacción ni escribe inversiones. Capital no negativo, máximo 999999999999,99 y dos decimales. Porcentajes entre 0 y 100, hasta cuatro decimales; suma menor o igual a 100. Las distribuciones parciales devuelven dinero disponible. Importe por tipo = capital × porcentaje / 100. Se conservan fracciones de centavo para no sobredistribuir y se devuelven copias de los mapas.

DELETE /api/portfolio/simulacion limpia solamente el plan. DELETE /api/portfolio/sesion invalida la sesión HTTP; PortfolioSesion llama a cerrar con @Remove y el contenedor ejecuta @PreDestroy. También se libera al expirar la sesión de 30 minutos. @PermitAll en cerrar habilita la limpieza al terminar la autenticación; no vuelve públicos los endpoints protegidos. @PostConstruct y @PreDestroy registran un UUID que identifica la conversación. La capacidad de pasivación queda habilitada por defecto; no se promete persistencia del plan después de reiniciar el servidor.

## Cálculos del portfolio persistente

Por ticker se suman cantidades e importes de compra. Precio promedio = capital invertido / cantidad; valor actual = cantidad × cotización; ganancia no realizada = valor actual − capital invertido; rendimiento porcentual = ganancia × 100 / capital invertido. Para cartera vacía se devuelven ceros. El patrimonio suma el valor de las posiciones por moneda, sin cuenta de efectivo. Con varias monedas no se devuelve un total general; se utiliza totalesPorMoneda.

Diez AAPL a USD 180 y cinco a USD 210 producen 15 unidades, USD 2.850 de costo y promedio USD 190. Con precio ilustrativo USD 195, el valor es USD 2.925, la ganancia USD 75 y el rendimiento 2,6316 %. Este ejemplo pertenece al perfil H2 de demostración. En MySQL se usa la moneda del par y la fecha del último cierre; no son precios de mercado en vivo.

El cálculo acepta COMPRA y rechaza tipos no soportados. Antes de integrar Venta deben acordarse descuento de cantidades/costo, orden de movimientos y ganancia realizada. La revisión local de joaco (88f85f5) contiene OperacionServiceBean @Stateless, OperacionStrategy y VentaStrategy @Dependent; se describen como código externo, no como funcionalidad integrada.

## Registro/Login: cuarto componente

Registro y Login se consideran un único componente porque reúnen el alta y acceso a la cuenta. Zoe es responsable de su implementación. La responsabilidad acordada incluye registrar usuarios, autenticar, cerrar sesión y mantener la asociación entre usuario, principal, rol y portfolio. Se integra con Compra, Venta y Portfolio mediante una identidad estable del contenedor.

La arquitectura prevista separa presentación (pantallas y recursos de acceso), negocio (reglas de registro y autenticación) y datos (persistencia de usuarios). Esta descripción define el contrato a integrar, no clases ya verificadas. El tipo de EJB, los callbacks y los patrones de Registro/Login deben completarse al revisar la implementación de Zoe. No se asume que sea Stateless ni que ya aplique Strategy.

La autenticación BASIC y la autorización declarativa actuales permiten probar Compra y Portfolio; no equivalen al componente Registro/Login terminado. La entrega debe demostrar el alta, el acceso, el rechazo de credenciales inválidas y el cierre de sesión una vez integrado.

## Tres patrones y su justificación

**Repository/DAO:** InstrumentoRepository, CatalogoMercadoRepository, OrdenRepository, OperacionRepository y PortfolioRepository encapsulan EntityManager y consultas. Compra y Portfolio reutilizan esta capa sin duplicar persistencia ni incluir SQL en la presentación. Se cuenta como un patrón, no dos.

**Service Facade:** PortfolioService reúne consulta y planificación de la cartera detrás de un contrato. Coordina identidad, operaciones, agrupación y cotización para que los clientes trabajen con DTOs y no con sus dependencias internas.

**Strategy:** CotizacionStrategy define cotizar(Instrumento); CotizacionCatalogo es la implementación activa, un bean CDI @ApplicationScoped: obtiene el último cierre de MySQL o la referencia ilustrativa del perfil H2. No es un EJB @Stateless. El cálculo depende de la interfaz, y una prueba reemplaza el precio con otra estrategia. Un futuro proveedor puede incorporarse sin cambiar la consolidación, seleccionando su implementación mediante CDI.

Stateful y stateless son modelos de EJB, no dos patrones extra. PortfolioSesion administra la referencia en la capa web; el alias /simulador preserva compatibilidad y tampoco se cuenta como un componente nuevo.

## Seguridad y persistencia

web.xml protege /api/* y /dashboard con BASIC y rol USUARIO en ApplicationRealm. Compra y Portfolio usan @RolesAllowed. Sin credenciales: 401; sin rol: 403. Fuera de localhost se requiere HTTPS. PortfolioActual obtiene SessionContext.getCallerPrincipal y busca la cartera del propietario: no se acepta un ID arbitrario ni hay fallback al portfolio 1.

Login debe establecer el principal/rol del contenedor e invalidar la sesión al salir o cambiar de identidad. La columna propietario conserva el principal y usuario_id ya vincula la cartera con usuarios. El login del equipo debe mantener consistente esta asociación. El primer aprovisionamiento concurrente puede fallar por unicidad; el registro definitivo debe crear la cartera junto al usuario.

persistence.xml usa JTA. El build normal conserva ExampleDS y schema action none; demo-init usa create sólo sobre una BD vacía y aislada. El perfil mysql usa InversorARDS con none. Driver y credenciales pertenecen al servidor; no al WAR ni Git. MySQL está conectado: el catálogo recibido se conserva y las compras se guardan en operaciones. Las tablas ordenes y orden_detalles forman parte de cada compra nueva y están enlazadas con operaciones; Portfolio no suma los dos conjuntos. Las pantallas y el alta del login definitivo deben completar el vínculo con usuarios; la FK y la asociación de la demo local ya están implementadas.

## Pruebas y defensa

21 pruebas JUnit/Mockito verifican cálculos, validaciones, identidad, independencia de planes y posiciones actualizadas sin perder la simulación. La prueba real del WAR en WildFly verifica además roles, cookies, transacciones, rutas antiguas/nuevas y retiro de EJB. Ver PRUEBAS.md y EVIDENCIA.md para resultados.

Para la defensa, registrar una compra, mostrar el resumen y planificar porcentajes en la misma sesión. Registrar otra compra: cambia la cartera y permanece el plan. Abrir otra sesión: misma cartera para la misma cuenta, plan independiente. Reiniciar el plan y luego cerrar la sesión, explicando la diferencia y mostrando los callbacks. La integración de los cuatro componentes necesita incorporar y demostrar Venta (Joaco) y Registro/Login (Zoe), incluyendo registro, acceso y cierre de sesión. El mínimo académico sigue siendo al menos tres componentes desplegados.

Referencias: [Stateful](https://jakarta.ee/specifications/enterprise-beans/4.0/apidocs/jakarta/ejb/stateful), [Remove](https://jakarta.ee/specifications/enterprise-beans/4.0/apidocs/jakarta/ejb/remove) y [WildFly](https://docs.wildfly.org/30/Getting_Started_Guide.html).
