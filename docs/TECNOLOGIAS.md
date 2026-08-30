# Tecnologías y versiones

| Área | Tecnología | Versión utilizada | Motivo |
|---|---|---:|---|
| Lenguaje | Java | 17 LTS | Versión indicada en las diapositivas; el proyecto compila con `--release 17`. |
| Construcción | Apache Maven | 3.9.9 | Gestiona dependencias y genera el WAR. |
| Plataforma | Jakarta EE Web Profile | 10.0.0 | Incluye Servlet, JSP, JAX-RS, CDI, EJB, JPA y JTA. |
| Servidor | WildFly EE 10 | 41.0.0.Final | Contenedor Jakarta EE y despliegue local del WAR. |
| Persistencia | JPA | 3.1 (provista por WildFly) | Mapea entidades y consultas a la base. |
| Base de datos | H2 | 2.2 (provista por WildFly) | Base de demostración `ExampleDS`, sin instalación adicional. |
| Presentación | JSP, Servlet y JavaScript | Servlet 6.0 / JSP 3.1 | Sigue la tecnología de la materia y consume la API REST. |
| API | JAX-RS / RESTEasy | JAX-RS 3.1 | Expone el componente para pruebas o clientes futuros. |

## Dependencias

La única dependencia declarada en `pom.xml` es `jakarta.jakartaee-web-api:10.0.0` con alcance `provided`: el WAR no incluye el servidor ni sus APIs porque WildFly las provee durante el despliegue.

## Requisitos locales

1. JDK 17 LTS.
2. Maven 3.9+; en este equipo se incluye una copia portátil en `.tools/apache-maven-3.9.9`.
3. WildFly EE 10; en este equipo se incluye una copia portátil en `.tools/wildfly-ee-10-41.0.0.Final`.
4. Puerto local 8080 disponible.

Si Maven no reconoce el certificado de la red, usar `-Djavax.net.ssl.trustStoreType=Windows-ROOT` para que Java tome los certificados confiables de Windows.
