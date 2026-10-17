<#
.SYNOPSIS
    Configura un WildFly en modo full para InversorAR (perfil MySQL + mensajería) y despliega el WAR.

.DESCRIPTION
    Requiere WildFly iniciado con:  bin\standalone.bat -c standalone-full.xml
    Se ejecuta desde el repositorio (config\setup-wildfly.ps1), con F5 en PowerShell ISE o por consola.
    Pasos: driver MySQL (lo instala si falta), propiedades JDBC, datasource, seguridad, colas,
    compilación y despliegue.
    Driver: si WildFly no tiene el módulo com.mysql, usa -DriverJar, o busca mysql-connector-j*.jar
    en standalone\deployments y en el repositorio de Maven (.m2), o lo descarga de Maven Central
    verificando su SHA-1.
    Se puede repetir: lo que ya existe no se duplica.
    Conexión a MySQL: la primera vez pregunta servidor, puerto, base y usuario (Enter acepta el valor
    sugerido); después conserva lo cargado, salvo que se pasen parámetros o -CambiarConexion.
    La contraseña se pide oculta y nunca se guarda en el repositorio.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File config\setup-wildfly.ps1 -WildflyHome C:\wildfly-41.0.1.Final

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File config\setup-wildfly.ps1 -WildflyHome C:\wildfly -BaseDatos inversorar -Usuario app
#>
param(
    [string]$WildflyHome = $env:WILDFLY_HOME,
    [string]$Servidor,        # host de MySQL (sugerido: 127.0.0.1)
    [string]$Puerto,          # sugerido: 3306
    [string]$BaseDatos,       # sugerido: instrumentos
    [string]$Usuario,         # sugerido: root
    [string]$JdbcUrl,         # avanzado: URL JDBC completa; reemplaza servidor, puerto y base
    [switch]$CambiarConexion, # vuelve a preguntar servidor, puerto, base, usuario y contraseña
    [switch]$CambiarPassword,
    [string]$DriverJar,       # ruta a un mysql-connector-j*.jar, si no se quiere buscar ni descargar
    [switch]$SinCompilar
)
$DriverVersion = "8.4.0"

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$env:NOPAUSE = "true"   # evita el "Presione una tecla" de los .bat de WildFly

function Paso($texto) { Write-Host ""; Write-Host "==> $texto" -ForegroundColor Cyan }
function Ok($texto) { Write-Host "    OK: $texto" -ForegroundColor Green }
function Falla($texto) { Write-Host ""; Write-Host "ERROR: $texto" -ForegroundColor Red; exit 1 }

# Ejecuta comandos jboss-cli contra el servidor iniciado. Devuelve la salida como texto.
function JbossCli([string[]]$argumentos) {
    $salida = & $script:cli --connect @argumentos | Out-String
    return @{ Salida = $salida; Codigo = $LASTEXITCODE }
}
# Ejecuta un texto como script .cli temporal (evita problemas de comillas de PowerShell 5.1).
function JbossCliTexto([string]$contenido) {
    $tmp = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText($tmp, $contenido)
        return JbossCli @("--file=$tmp")
    } finally { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
}
function Existe([string]$recurso) { (JbossCli @("--command=$($recurso):read-resource")).Codigo -eq 0 }
# Escapa un valor para ponerlo entre comillas en un comando jboss-cli.
function Comillas([string]$v) { '"' + ($v -replace '\\', '\\' -replace '"', '\"') + '"' }
function EsperarServidor {
    for ($i = 0; $i -lt 60; $i++) {
        $r = JbossCli @("--command=:read-attribute(name=server-state)")
        if ($r.Codigo -eq 0 -and $r.Salida -match '"running"') { return }
        Start-Sleep -Seconds 2
    }
    Falla "WildFly no volvió a estado 'running' después de recargar."
}

# Crea el módulo com.mysql de WildFly (carpeta modules\com\mysql\main con el .jar y su module.xml).
function InstalarModuloMysql([string]$jar) {
    $dir = Join-Path $WildflyHome "modules\com\mysql\main"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $nombre = Split-Path -Leaf $jar
    Copy-Item $jar (Join-Path $dir $nombre) -Force
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="com.mysql">
    <resources>
        <resource-root path="$nombre"/>
    </resources>
    <dependencies>
        <module name="java.naming"/>
        <module name="java.sql"/>
        <module name="java.transaction.xa"/>
        <module name="java.logging"/>
        <module name="java.security.sasl"/>
        <module name="java.xml"/>
    </dependencies>
</module>
"@
    [IO.File]::WriteAllText((Join-Path $dir "module.xml"), $xml)
}
# Busca un Connector/J ya presente en la máquina; si no hay, lo descarga de Maven Central.
function ObtenerDriverJar {
    if ($DriverJar) {
        if (-not (Test-Path $DriverJar)) { Falla "No existe el archivo indicado en -DriverJar: $DriverJar" }
        return (Resolve-Path $DriverJar).Path
    }
    $lugares = @((Join-Path $WildflyHome "standalone\deployments"), (Join-Path $env:USERPROFILE ".m2\repository\com\mysql"))
    $encontrado = $lugares | Where-Object { Test-Path $_ } |
        ForEach-Object { Get-ChildItem $_ -Recurse -Filter "mysql-connector-j-*.jar" -ErrorAction SilentlyContinue } |
        Where-Object { $_.Name -notmatch "sources|javadoc" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($encontrado) { Ok "Driver encontrado en $($encontrado.FullName)"; return $encontrado.FullName }

    $url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$DriverVersion/mysql-connector-j-$DriverVersion.jar"
    $destino = Join-Path ([IO.Path]::GetTempPath()) "mysql-connector-j-$DriverVersion.jar"
    Write-Host "    Descargando Connector/J $DriverVersion de Maven Central..."
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest $url -OutFile $destino -UseBasicParsing
        $esperado = (Invoke-WebRequest "$url.sha1" -UseBasicParsing).Content.ToString().Trim().Split(" ")[0].ToLower()
    } catch {
        Falla "No se pudo descargar el driver ($($_.Exception.Message)).`n  Descargarlo a mano de $url y volver a ejecutar con -DriverJar <ruta del .jar>"
    }
    if ((Get-FileHash $destino -Algorithm SHA1).Hash.ToLower() -ne $esperado) {
        Remove-Item $destino -Force -ErrorAction SilentlyContinue
        Falla "El driver descargado no coincide con el SHA-1 publicado por Maven Central. Reintentar más tarde."
    }
    Ok "Driver descargado y verificado (SHA-1)"
    return $destino
}

# ---------------------------------------------------------------------------
Paso "Verificando repositorio y WildFly"
if (-not (Test-Path (Join-Path $repo "pom.xml")) -or -not (Test-Path (Join-Path $repo "config\mysql-datasource.cli"))) {
    Falla "Este script tiene que ejecutarse desde la carpeta config del repositorio (no una copia suelta).`n  Abrir <repo>\config\setup-wildfly.ps1, en la rama mensajeria."
}
Ok "Repositorio: $repo"
if (-not $WildflyHome) {
    $WildflyHome = (Read-Host "    Carpeta de WildFly (la que contiene bin\)").Trim().Trim('"')
    if (-not $WildflyHome) { Falla "Indicar la carpeta de WildFly con -WildflyHome o la variable WILDFLY_HOME." }
}
$script:cli = Join-Path $WildflyHome "bin\jboss-cli.bat"
if (-not (Test-Path $script:cli)) { Falla "No se encontró $script:cli. ¿La carpeta de WildFly es correcta (la que contiene bin\)?" }
if ((JbossCli @("--command=:read-attribute(name=server-state)")).Codigo -ne 0) {
    Falla "WildFly no está corriendo. Iniciarlo en otra ventana con:`n  & `"$WildflyHome\bin\standalone.bat`" -c standalone-full.xml"
}
if (-not (Existe "/subsystem=messaging-activemq")) {
    Falla "WildFly está corriendo sin mensajería. Detenerlo y reiniciarlo con -c standalone-full.xml"
}
Ok "WildFly en modo full, con broker de mensajería"
if (Test-Path (Join-Path $WildflyHome "modules\com\mysql\main\module.xml")) {
    Ok "Módulo del driver MySQL ya instalado"
} else {
    Write-Host "    Falta el módulo del driver MySQL: instalándolo..."
    InstalarModuloMysql (ObtenerDriverJar)
    Ok "Módulo com.mysql instalado en $WildflyHome\modules\com\mysql\main"
}

# ---------------------------------------------------------------------------
Paso "Propiedades de conexión a MySQL"
function FijarPropiedad([string]$nombre, [string]$valor) {
    $op = if (Existe "/system-property=$nombre") { "write-attribute(name=value,value=$(Comillas $valor))" } else { "add(value=$(Comillas $valor))" }
    $r = JbossCliTexto "/system-property=$($nombre):$op`n"
    if ($r.Codigo -ne 0) { Falla "No se pudo cargar la propiedad $nombre." }
}
function LeerPropiedad([string]$nombre) {
    $r = JbossCli @("--command=/system-property=$($nombre):read-attribute(name=value)")
    if ($r.Codigo -eq 0 -and $r.Salida -match '"result" => "(.*)"') { return $Matches[1] }
    return $null
}
function Preguntar([string]$texto, [string]$sugerido) {
    $v = Read-Host "    $texto [$sugerido]"
    if ([string]::IsNullOrWhiteSpace($v)) { return $sugerido } else { return $v.Trim() }
}
$urlActual = LeerPropiedad "inversorar.jdbc.url"
$usuarioActual = LeerPropiedad "inversorar.jdbc.user"

# URL: parámetro completo > partes indicadas o preguntadas > la ya cargada.
if ($JdbcUrl) {
    $url = $JdbcUrl
} elseif ($Servidor -or $Puerto -or $BaseDatos -or $CambiarConexion -or -not $urlActual) {
    if (-not $Servidor) { $Servidor = Preguntar "Servidor de MySQL" "127.0.0.1" }
    if (-not $Puerto) { $Puerto = Preguntar "Puerto" "3306" }
    if (-not $BaseDatos) { $BaseDatos = Preguntar "Base de datos" "instrumentos" }
    $url = "jdbc:mysql://$($Servidor):$Puerto/$($BaseDatos)?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"
} else {
    $url = $urlActual
}
# Usuario: parámetro > preguntado (primera vez o -CambiarConexion) > el ya cargado.
if ($Usuario) { $user = $Usuario }
elseif ($CambiarConexion -or -not $usuarioActual) { $user = Preguntar "Usuario de MySQL" "root" }
else { $user = $usuarioActual }

FijarPropiedad "inversorar.jdbc.url" $url
FijarPropiedad "inversorar.jdbc.user" $user
Ok "Conexión: $user @ $(($url -replace '^jdbc:mysql://', '') -replace '\?.*$', '')"
$cambioUsuario = $usuarioActual -and $usuarioActual -ne $user
if ($CambiarPassword -or $CambiarConexion -or $cambioUsuario -or -not (Existe "/system-property=inversorar.jdbc.password")) {
    $segura = Read-Host "    Contraseña de MySQL para '$user' (no se muestra)" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try { FijarPropiedad "inversorar.jdbc.password" ([Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    Ok "Contraseña cargada en la configuración local de WildFly"
} else {
    Ok "Contraseña ya cargada (usar -CambiarPassword para reemplazarla)"
}

# ---------------------------------------------------------------------------
Paso "Driver y datasource"
if ((JbossCli @("--file=$repo\config\mysql-driver.cli")).Codigo -ne 0) { Falla "No se pudo registrar el driver MySQL." }
Ok "Driver 'mysql' registrado"
# Un datasource que no use las propiedades del script (p. ej. de un standalone-full.xml copiado de
# otra máquina) se reemplaza por el del repositorio.
$dsRecurso = "/subsystem=datasources/data-source=InversorARDS"
$def = JbossCli @("--command=$($dsRecurso):read-resource")
if ($def.Codigo -eq 0) {
    $propio = $def.Salida -match [regex]::Escape('"connection-url" => expression "${inversorar.jdbc.url}"') -and
              $def.Salida -match [regex]::Escape('"user-name" => expression "${inversorar.jdbc.user}"') -and
              $def.Salida -match [regex]::Escape('"password" => expression "${inversorar.jdbc.password}"') -and
              $def.Salida -match '"driver-name" => "mysql"'
    if (-not $propio) {
        if ((JbossCli @("--command=$($dsRecurso):remove")).Codigo -ne 0) { Falla "No se pudo quitar el datasource InversorARDS anterior." }
        Write-Host "    Se reemplazó un datasource InversorARDS con otra configuración (por ejemplo, copiado de otra máquina)."
    }
}
function ProbarDatasource { (JbossCli @("--command=$($dsRecurso):test-connection-in-pool")).Salida }
$ds = JbossCli @("--file=$repo\config\mysql-datasource.cli")
$prueba = $ds.Salida
if ($prueba -notmatch '\[true\]') {
    # Un datasource definido antes de que existiera el driver (o recién recreado) no arranca hasta recargar.
    Write-Host "    El datasource todavía no responde: recargando WildFly y probando de nuevo..."
    JbossCli @("--command=:reload") | Out-Null
    EsperarServidor
    $prueba = ProbarDatasource
}
if ($prueba -notmatch '\[true\]') {
    Write-Host $prueba
    Falla ("El datasource no pudo conectarse a MySQL. Revisar:`n" +
        "  - que exista la base y que el usuario tenga permisos sobre ella (docs/MYSQL.md);`n" +
        "  - servidor, puerto, base y usuario: volver a ejecutar con -CambiarConexion;`n" +
        "  - la contraseña: volver a ejecutar con -CambiarPassword.")
}
Ok "Datasource InversorARDS conectado a MySQL"

# ---------------------------------------------------------------------------
Paso "Seguridad y mensajería"
if ((JbossCli @("--file=$repo\config\wildfly-security.cli")).Codigo -ne 0) { Falla "No se pudo aplicar config/wildfly-security.cli." }
EsperarServidor
Ok "Seguridad del login aplicada (servidor recargado)"
if ((JbossCli @("--file=$repo\config\wildfly-messaging.cli")).Codigo -ne 0) { Falla "No se pudieron crear las colas." }
Ok "Colas 'ordenes' y 'DLQ.ordenes'"

# ---------------------------------------------------------------------------
if ($SinCompilar) {
    Paso "Compilación omitida (-SinCompilar)"
} else {
    Paso "Compilando con el perfil MySQL"
    Push-Location $repo
    try { & mvn -q -Pmysql clean package } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { Falla "Falló la compilación o los tests (mvn -Pmysql clean package)." }
    Ok "target\inversorar.war generado"
}

Paso "Desplegando"
$war = Join-Path $repo "target\inversorar.war"
if (-not (Test-Path $war)) { Falla "No existe $war. Ejecutar sin -SinCompilar." }
$deploy = Join-Path $WildflyHome "standalone\deployments"
# WildFly identifica el contenido desplegado por su SHA-1: el despliegue terminó cuando corre
# exactamente este WAR con estado OK. Si ya estaba desplegado igual, no se vuelve a copiar.
function HashDesplegado {
    $r = JbossCli @("--command=/deployment=inversorar.war:read-attribute(name=content)")
    if ($r.Codigo -ne 0) { return "" }
    return (([regex]::Matches($r.Salida, '0x([0-9a-f]{2})') | ForEach-Object { $_.Groups[1].Value }) -join "")
}
function EstadoDespliegue { (JbossCli @("--command=/deployment=inversorar.war:read-attribute(name=status)")).Salida }
$hashWar = (Get-FileHash $war -Algorithm SHA1).Hash.ToLower()
if ((HashDesplegado) -eq $hashWar -and (EstadoDespliegue) -match '"OK"') {
    Ok "inversorar.war ya estaba desplegado (mismo WAR, sin cambios)"
} else {
    Copy-Item $war $deploy -Force
    $listo = $false
    for ($i = 0; $i -lt 60 -and -not $listo; $i++) {
        Start-Sleep -Seconds 2
        if ((HashDesplegado) -eq $hashWar) {
            $estado = EstadoDespliegue
            if ($estado -match '"FAILED"') { Falla "El despliegue falló. Ver el log en $WildflyHome\standalone\log\server.log" }
            $listo = $estado -match '"OK"'
        }
    }
    if (-not $listo) { Falla "El despliegue no terminó a tiempo. Ver el log en $WildflyHome\standalone\log\server.log" }
    Ok "inversorar.war desplegado"
}

Write-Host ""
Write-Host "Listo. Abrir http://localhost:8080/inversorar/dashboard" -ForegroundColor Green
