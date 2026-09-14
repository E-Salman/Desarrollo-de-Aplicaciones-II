# Checklist contra la consigna

| Requisito | Estado y evidencia |
|---|---|
| Compra funcional en capas | REST/dashboard, interfaz EJB stateless, validaciones y repositories |
| Portfolio funcional en capas | REST/dashboard, interfaz EJB stateful, DTOs, repositories y planificación temporal |
| Tres componentes de entrega | Compra, Venta y Portfolio; falta integrar y demostrar Venta |
| Al menos un stateless | CompraServiceBean |
| Al menos un stateful | PortfolioServiceBean por sesión HTTP |
| Inicialización y destrucción | Compra y Portfolio con PostConstruct/PreDestroy; Remove al cerrar Portfolio |
| Tres patrones diferentes | Repository/DAO, Service Facade y Strategy; justificados en TECNICO.md |
| Autenticación y rol | BASIC del contenedor y RolesAllowed USUARIO; login definitivo pendiente |
| Transacciones | Compra y consultas de Portfolio REQUIRED; planificación NOT_SUPPORTED |
| MySQL | Datasource probado, catálogo e históricos del equipo disponibles y compras persistidas; órdenes y detalles enlazados a movimientos; relación de usuario explícita |
| Documento de 5 a 8 páginas | El Word del equipo debe actualizarse a esta organización. El PDF anterior queda histórico |
| Demo y defensa | PRUEBAS.md, EVIDENCIA.md y explicación de decisiones en TECNICO.md |

La simulación es una funcionalidad de Portfolio, no otro componente. Se consulta el histórico de precios recibido. No se calcula riesgo ni rendimiento histórico de la simulación; tampoco hay precios en vivo, pagos reales o conversión multimoneda. Ganancia actual significa ganancia no realizada; integrar Venta exige acordar costo remanente y ganancia realizada.
