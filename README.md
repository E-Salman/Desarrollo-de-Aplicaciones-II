# InversorAR — Componente Compra de Instrumentos

Primer componente desplegable de la aplicación, implementado en capas con Jakarta EE 10.

## Documentación

- [Tecnologías y versiones](docs/TECNOLOGIAS.md)
- [Arquitectura en capas](docs/ARQUITECTURA.md)
- [Requisitos funcionales](docs/REQUISITOS.md)

## Arquitectura

`JSP/Servlet + JAX-RS` → `CompraService (@Stateless)` → `Repositories JPA` → `H2 de WildFly`.

La pantalla se abre en `/inversorar/dashboard`. La API expone:

- `GET /inversorar/api/instrumentos`
- `POST /inversorar/api/compras`
- `GET /inversorar/api/portfolios/1/resumen`

La compra recibe `ticker`, `cantidad`, `precioUnitario` y `fecha`. El servidor valida los valores y calcula el total. El precio puede diferir de la cotización de catálogo porque representa el precio real de la operación.

## Requisitos y despliegue

- JDK 17 LTS
- Maven 3.9+
- WildFly compatible con Jakarta EE 10 (por ejemplo, WildFly 30+)

WildFly incluye la fuente H2 de demostración `java:jboss/datasources/ExampleDS`, que utiliza este proyecto. Compilar y desplegar:

```powershell
mvn clean package
Copy-Item .\target\inversorar.war $env:WILDFLY_HOME\standalone\deployments\
```

Después del log de despliegue, abrir `http://localhost:8080/inversorar/`.

## Prueba rápida de API

```json
POST /inversorar/api/compras
{
  "ticker": "AAPL",
  "cantidad": 10,
  "precioUnitario": 180,
  "fecha": "2026-08-30"
}
```

La base se recrea al desplegar para que la demostración siempre comience con el catálogo inicial y el portfolio de prueba vacío.
