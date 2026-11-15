# ADR 006 — JWT vinculado a la sesión web

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** La consigna pide JWT y autorización por rol. La aplicación ya usa Jakarta Security, JSP y una conversación Portfolio por sesión. El cierre de sesión debe revocar el acceso.

**Decisión.** El login valida las credenciales en MySQL y emite un JWT HS256 de 15 minutos con emisor, audiencia, sujeto, identificador y roles. La cookie de acceso es HttpOnly, SameSite=Strict y Secure sobre HTTPS. El identificador del JWT se vincula a la nueva sesión. Cada petición verifica el JWT y restituye el principal y los grupos en el contenedor. Se preservan las restricciones de URL y `@RolesAllowed`; el logout elimina la cookie e invalida la sesión.

**Alternativas.** La identidad cacheada solo en la sesión no cumplía JWT. Un JWT sin estado almacenado en JavaScript requería rediseñar la navegación JSP y resolver la revocación por separado. Un proveedor externo añade infraestructura y gestión de claves para una demo de un solo nodo.

**Consecuencias.** Un redespliegue invalida los accesos porque la clave se genera en memoria. Al vencer se vuelve a iniciar sesión. La verificación y firma se delegan a java-jwt. Para múltiples nodos habría que coordinar claves y sesiones.

**Evidencia.** `TokensAccesoTest`, `smoke_segunda_entrega.mjs` y [documentación de java-jwt](https://github.com/auth0/java-jwt).
