# InversorAR - Compra y Portfolio

Rama `comp_portafolio`, basada en `primeros_pasos` (`ac4a73f`). Componentes Jakarta EE 10 en capas: Compra y Portfolio persistentes, más Simulador conversacional como apoyo académico. Venta queda a cargo de su componente y no está implementada en esta rama.

## Ejecutar

Requiere JDK 17, Maven 3.9 y WildFly compatible con Jakarta EE 10. Verificación realizada con WildFly 30.0.1.Final.

```powershell
mvn test
mvn package
```

El WAR normal **no crea ni borra tablas**. Para una instalación nueva de demostración, con H2 ExampleDS vacío y aislado:

```powershell
mvn -Pdemo-init package
Copy-Item target/inversorar.war "$env:WILDFLY_HOME/standalone/deployments/"
```

Antes de entrar, ejecutar `bin/add-user.bat` de WildFly de forma interactiva: elegir **Application User**, realm `ApplicationRealm`, elegir usuario/contraseña y asignar grupo **USUARIO**. Abrir `/inversorar/dashboard` y autenticarse en el diálogo del navegador. No se agregaron pantallas de login ni contraseñas al repositorio. Se requiere HTTPS fuera de localhost.

El perfil `demo-init` usa `create`, solo para una BD vacía. No es una migración ni debe desplegarse sobre datos del equipo. H2 ExampleDS es una demostración en memoria; MySQL se prepara mediante `mvn -Pmysql package` y un datasource externo. Ver [integración y BD](docs/INTEGRACION.md).

## Funcionalidades y API

Todas las rutas siguientes llevan prefijo `/inversorar/api` y requieren `USUARIO`:

| Método y ruta | Resultado |
|---|---|
| GET `/instrumentos` | Catálogo disponible |
| POST `/compras` | Registra compra; 201 con resumen actualizado |
| GET `/portfolio/resumen` | Capital invertido, patrimonio, ganancia, rendimiento porcentual y posiciones |
| GET `/portfolio/posiciones` | Cantidad, precio promedio, cotización, costo, valor y rendimiento por instrumento |
| GET `/simulador` | Estado de la simulación de la sesión HTTP |
| PUT `/simulador/capital` | `{"valor":1000000}` |
| PUT `/simulador/porcentajes/ACCION` | `{"valor":40}`; tipos: ACCION, BONO, CRIPTO, CEDEAR, MONEDA |
| DELETE `/simulador` | Cierra sesión de simulación y destruye el EJB; 204 |

Ejemplo de compra: `{"ticker":"AAPL","cantidad":10,"precioUnitario":180,"fecha":"2026-09-01"}`. La fecha debe ser válida y no futura. Errores de negocio: 400. Sin autenticación: 401. Sin rol: 403.

El cliente no envía un portfolioId. La antigua ruta `/portfolios/1/resumen` se retiró para impedir consultas a carteras ajenas. El dashboard se actualizó al contrato nuevo. El simulador requiere conservar la cookie JSESSIONID y no persiste inversiones.

## Documentación y entrega

- [Documento técnico](docs/TECNICO.md), también en [PDF de seis páginas](docs/TECNICO.pdf).
- [Arquitectura](docs/ARQUITECTURA.md), [requisitos y checklist](docs/REQUISITOS.md), [tecnologías](docs/TECNOLOGIAS.md).
- [Integración con Login, BD y Venta](docs/INTEGRACION.md).
- [Pruebas y demo](docs/PRUEBAS.md).

Patrones presentes: Repository/DAO, Service Facade y Strategy de cotización. Compra usa EJB stateless y transacción REQUIRED; Simulador usa EJB stateful con callbacks visibles y cierre explícito.

**Entrega general pendiente:** integrar y demostrar Venta, integrar el login del equipo y ejecutar con el esquema MySQL definitivo. No se declara cumplida la entrega de tres componentes persistentes por contar al simulador como sustituto de Venta.
