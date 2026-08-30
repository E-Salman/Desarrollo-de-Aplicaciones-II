# Arquitectura del componente Compra de Instrumentos

```text
JSP + JavaScript / REST client
            │
            ▼
Servlet Dashboard + CompraResource (JAX-RS)
            │
            ▼
CompraService / CompraServiceBean (@Stateless, JTA)
            │
            ▼
Repositories JPA
            │
            ▼
H2 ExampleDS de WildFly
```

## Módulos lógicos

- `presentation`: Servlet, JSP y recurso JAX-RS. Recibe datos y devuelve HTTP/HTML/JSON; no contiene reglas ni SQL.
- `business`: contrato `CompraService`, implementación EJB, validaciones y consolidación de posiciones.
- `business.dto`: contratos de entrada y salida entre negocio y presentación; las entidades nunca se exponen por REST.
- `data`: entidades JPA, enumeraciones y repositorios. Sólo resuelve persistencia.

La solicitud atraviesa únicamente la capa siguiente: presentación → negocio → datos. Una compra se registra dentro de una transacción gestionada por el contenedor; el total se calcula en el servidor.
