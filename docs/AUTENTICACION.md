# Registro y autenticación

El principal es el email de `usuarios`. El registro normaliza emails nuevos con
`strip()` y minúsculas, valida nombre/apellido, email y contraseña de 8 a 128
caracteres. Guarda PBKDF2-HMAC-SHA256 con 600.000 iteraciones, salt aleatorio de
32 bytes y clave de 32 bytes mediante `Pbkdf2PasswordHash` de Jakarta Security.
El formato incluye algoritmo, iteraciones, salt y hash y cabe en `varchar(255)`.
No se cambia la estructura MySQL de `usuarios`.

`UsuariosIdentityStore` consulta el repositorio JPA de `inversorarPU`: ExampleDS
en demo e InversorARDS con `-Pmysql`. Evita duplicar el datasource en una anotación.
`Autenticacion` implementa `HttpAuthenticationMechanism` y usa `IdentityStoreHandler`,
`notifyContainerAboutLogin`. Tras validar el formulario emite un JWT firmado de
15 minutos en la cookie HttpOnly `INVERSORAR_ACCESS`, con SameSite=Strict y Secure
sobre HTTPS. `JSESSIONID` mantiene la conversación del Portfolio y vincula esa
sesión con el identificador del JWT. La identidad y el rol se verifican en cada
petición a partir del token. El formulario redirige las vistas privadas al
login y las peticiones API sin autenticación devuelven 401.

## Preparar WildFly antes del despliegue

Ejecutar una vez (también puede repetirse):

```powershell
& "$env:WILDFLY_HOME/bin/jboss-cli.bat" --connect --file=config/wildfly-security.cli
mvn -Pmysql package
Copy-Item target/inversorar.war "$env:WILDFLY_HOME/standalone/deployments/"
```

El CLI agrega el mapeo `InversorAR` en Undertow **y EJB3**, ambos sobre
`ApplicationDomain`, y recarga el servidor. `jboss-web.xml` selecciona ese mapeo.
`integrated-jaspi=false` permite que Jakarta Security aporte identidades de la
BD sin exigir que existan también en el realm de archivos. No modifica `other`
ni agrega un realm personalizado. La cuenta MySQL necesita SELECT e INSERT en
`usuarios`, y SELECT, INSERT y UPDATE en `portfolios`, además de los permisos
del negocio existentes. El datasource del perfil debe estar preparado.

Referencias: [Jakarta Security](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/security/security-api/security-api.html)
y [JASPI en WildFly](https://docs.wildfly.org/38/feature-pack/doc/reference/subsystem/undertow/application-security-domain/index.html).

## Uso y migración

1. Abrir `/inversorar/registro`, crear una cuenta y luego iniciar sesión.
2. `/dashboard` y la API requieren INVERSORAR_ACCESS y JSESSIONID. Ya no se usa
   `curl -u` ni el diálogo BASIC.
3. La cartera se busca primero por `usuario_id`; si falta, se recupera por
   propietario y se vincula. Una vinculación a otra cuenta se rechaza. Las
   carteras históricas con FK correcta conservan propietario e inversiones.
4. “Cerrar sesión” hace POST con CSRF, `request.logout()`, borrado del JWT e invalidación. Un nuevo
   login crea otra sesión: las inversiones persisten y la simulación se reinicia.
   Login y registro también validan CSRF.

Las filas viejas con marcadores o texto plano **no permiten iniciar sesión**.
No convertir ese texto en una contraseña válida ni enlazar cuentas por nombres
parecidos: crear cuentas nuevas o establecer una contraseña nueva mediante un
procedimiento administrativo confiable. Revisar emails históricos antes de
normalizarlos, ya que podrían colisionar. No se modificaron ni borraron usuarios
del servidor ni datos MySQL. Retirar cuentas BASIC de prueba solo después de
validar el reemplazo y comprobar que otras aplicaciones no las necesitan.

La recuperación existente sigue siendo un prototipo: genera un enlace local
y no implementa entrega de correo/restablecimiento completo.

H2 ahora persiste `portfolios.usuario_id`. Usar `demo-init` solo con una base
vacía y aislada; una demo H2 persistente anterior necesita agregar esa columna.
El perfil normal no modifica esquemas.

## Verificación

```powershell
mvn test
# Contra una demo H2 aislada, ya desplegada con -Pdemo-init:
node scripts/smoke_auth.mjs http://127.0.0.1:8200/inversorar
```

El smoke crea dos cuentas aleatorias y movimientos: prueba redirección al login,
401 sin sesión (sin desafío BASIC), CSRF, confirmación, duplicados, contraseña
incorrecta, compras/ventas, aislamiento de carteras y simulaciones, logout y
reingreso. Ejecutado con éxito en WildFly 41.0.1.Final / Java 21 / H2.
`scripts/smoke.py` corresponde al flujo BASIC histórico y no aplica al login actual.

Todas las cuentas registradas reciben `USUARIO`; no hay alta de roles arbitrarios.
El usuario autenticado sin ese rol sigue sujeto al 403 declarativo, pero no está
ejercitado por este smoke. Las pruebas unitarias cubren rechazo en EJB.
La prueba MySQL de la segunda entrega se reproduce con [el smoke HTTP](../scripts/smoke_segunda_entrega.mjs).

Después de registrar una cuenta en MySQL, comprobar sin revelar el hash completo:

```sql
SELECT email, password LIKE 'PBKDF2WithHmacSHA256:600000:%' AS formato_pbkdf2,
       CHAR_LENGTH(password) AS longitud_hash
FROM usuarios WHERE email = 'EMAIL_DE_LA_CUENTA_REGISTRADA';
SELECT p.id, p.propietario, p.usuario_id
FROM portfolios p JOIN usuarios u ON u.id = p.usuario_id
WHERE u.email = 'EMAIL_DE_LA_CUENTA_REGISTRADA';
```
