# InversorAR — Componente Compra/Venta de Instrumentos

Primer componente desplegable de la aplicación, implementado en capas con Jakarta EE 10.

## Documentación

- [Tecnologías y versiones](docs/TECNOLOGIAS.md)
- [Arquitectura en capas](docs/ARQUITECTURA.md)
- [Requisitos funcionales](docs/REQUISITOS.md)
- [Funcionamiento de la venta](docs/VENTA.md)

## Arquitectura

`JSP/Servlet + JAX-RS` → `OperacionService (@Stateless)` → `CompraStrategy/VentaStrategy` → `Repositories JPA` → `H2 de WildFly`.

La pantalla se abre en `/inversorar/dashboard`. La API expone:

- `GET /inversorar/api/instrumentos`
- `POST /inversorar/api/compras`
- `POST /inversorar/api/ventas`
- `GET /inversorar/api/portfolios/1/resumen`

La compra y la venta reciben `ticker`, `cantidad`, `precioUnitario` y `fecha`. El servidor valida los valores y calcula el total; en una venta, además, valida que no se supere la cantidad poseída del instrumento. El precio puede diferir de la cotización de catálogo porque representa el precio real de la operación.

El resumen del portfolio calcula, por posición, la cantidad e invertido a costo promedio ponderado (una venta parcial reduce ambos proporcionalmente, dejando `rendimiento` como la ganancia potencial sobre lo que sigue en cartera). La ganancia ya realizada en ventas se acumula aparte y se suma en el KPI `gananciaTotal` del portfolio, sin perderse cuando una posición se vende por completo.

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

```json
POST /inversorar/api/ventas
{
  "ticker": "AAPL",
  "cantidad": 4,
  "precioUnitario": 195,
  "fecha": "2026-09-10"
}
```

La base se recrea al desplegar para que la demostración siempre comience con el catálogo inicial y el portfolio de prueba vacío.
