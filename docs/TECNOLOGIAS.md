# Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje y bytecode | Java 17, maven.compiler.release=17 |
| Plataforma | Jakarta EE Web Profile 10, API provided |
| Contenedor de prueba | WildFly 30.0.1.Final |
| Construcción | Maven 3.9.9, Compiler 3.13.0, WAR 3.4.0 |
| Pruebas | JUnit Jupiter 5.11.4, Mockito 5.15.2, Surefire 3.5.2 |
| Persistencia | JPA, transacciones JTA, MySQL 8.1.0 probado con Connector/J 8.4.0; H2 ExampleDS para demo |
| Presentación | JSP, Servlet, JavaScript, JAX-RS y JSON-B |
| Seguridad | BASIC sobre ApplicationRealm; autorización EJB por USUARIO |

Las APIs Jakarta son provistas por el servidor; JUnit y Mockito solo se usan en test. No se agregan frameworks de frontend ni servicios externos. Maven debe acceder por HTTPS a Central con una cadena de confianza válida; no desactivar la verificación TLS.

Referencias primarias: [WildFly Getting Started](https://docs.wildfly.org/30/Getting_Started_Guide.html), [Elytron](https://docs.wildfly.org/30/WildFly_Elytron_Security.html). La configuración final de datasource y autenticación depende del servidor del equipo. Registro/Login es el cuarto componente del proyecto, a cargo de Zoe; sus clases, tipo de EJB y patrones se documentarán al integrar su implementación. La autenticación BASIC actual es infraestructura provisional.
