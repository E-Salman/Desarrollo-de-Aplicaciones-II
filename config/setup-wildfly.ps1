<#
.SYNOPSIS
    Configura un WildFly en modo full para InversorAR (perfil MySQL + mensajería) y despliega el WAR.

.DESCRIPTION
    Requiere WildFly iniciado con:  bin\standalone.bat -c standalone-full.xml
    Pasos: propiedades JDBC, driver MySQL, datasource, seguridad, colas, compilación y despliegue.
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
    [switch]$SinCompilar
)

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

# ---------------------------------------------------------------------------
Paso "Verificando WildFly"
if (-not $WildflyHome) { Falla "Indicar la carpeta de WildFly con -WildflyHome o la variable WILDFLY_HOME." }
$script:cli = Join-Path $WildflyHome "bin\jboss-cli.bat"
if (-not (Test-Path $script:cli)) { Falla "No se encontró $script:cli. ¿La carpeta de WildFly es correcta (la que contiene bin\)?" }
if ((JbossCli @("--command=:read-attribute(name=server-state)")).Codigo -ne 0) {
    Falla "WildFly no está corriendo. Iniciarlo en otra ventana con:`n  & `"$WildflyHome\bin\standalone.bat`" -c standalone-full.xml"
}
if (-not (Existe "/subsystem=messaging-activemq")) {
    Falla "WildFly está corriendo sin mensajería. Detenerlo y reiniciarlo con -c standalone-full.xml"
}
Ok "WildFly en modo full, con broker de mensajería"
if (-not (Test-Path (Join-Path $WildflyHome "modules\com\mysql\main\module.xml"))) {
    Falla "Falta el módulo del driver MySQL (modules\com\mysql\main). Instalarlo con:`n  & `"$script:cli`" --command=`"module add --name=com.mysql --resources=C:\ruta\mysql-connector-j.jar --dependencies=jakarta.transaction.api`""
}
Ok "Módulo del driver MySQL instalado"

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
$ds = JbossCli @("--file=$repo\config\mysql-datasource.cli")
if ($ds.Codigo -ne 0 -or $ds.Salida -notmatch '\[true\]') {
    Write-Host $ds.Salida
    Falla "El datasource no pudo conectarse a MySQL. Revisar que MySQL esté corriendo, la URL, el usuario y la contraseña (-CambiarPassword)."
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
