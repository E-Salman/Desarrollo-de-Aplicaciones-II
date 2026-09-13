# Checklist contra la consigna

| Requisito | Estado y evidencia |
|---|---|
| Compra funcional en capas | Implementada: REST/dashboard, interfaz EJB, validaciones y repositories |
| Portfolio funcional en capas | Implementado: interfaz + stateless, DTOs, REST y dashboard |
| Tres componentes de entrega | Pendiente integrar/desplegar Venta; simulador es apoyo y no se presenta como reemplazo |
| Al menos un stateless | CompraServiceBean y PortfolioServiceBean |
| Al menos un stateful | SimuladorPortfolioServiceBean por sesión HTTP |
| Inicialización/destrucción | Compra y Simulador con PostConstruct/PreDestroy, logging; Remove para cerrar simulación |
| Tres patrones diferentes | Repository/DAO, Service Facade, Strategy; justificados en TECNICO |
| Autenticación y rol | web.xml BASIC + USUARIO y RolesAllowed en EJB; login definitivo del compañero pendiente |
| Transacciones | Compra REQUIRED, JTA y ApplicationException rollback=true |
| MySQL | Perfil/configuración preparados; despliegue en esquema del equipo pendiente |
| Documento de 5-8 páginas | TECNICO.pdf de seis páginas, fuente TECNICO.md |
| Demo y defensa | PRUEBAS.md y TECNICO.md; completar demo general al integrar Venta |

No se incluyen historial/riesgo del simulador, datos históricos, pagos reales ni conversión multimoneda. El catálogo es ilustrativo. Capital invertido significa costo de las posiciones compradas; ganancia es no realizada. La integración de ventas exige definir costo remanente y ganancia realizada.
