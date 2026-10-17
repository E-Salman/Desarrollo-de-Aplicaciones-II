<#
.SYNOPSIS
    Inicia WildFly en modo full (standalone-full.xml) para InversorAR. Se usa con doble clic en
    config\iniciar-wildfly.bat.

.DESCRIPTION
    - Siempre arranca con -c standalone-full.xml (el único modo con broker de mensajería).
    - Si ya hay un WildFly corriendo, lo avisa y ofrece cerrarlo en vez de levantar un segundo.
    - La primera vez pregunta la carpeta de WildFly y la recuerda para este usuario de Windows
      (%APPDATA%\InversorAR\wildfly-home.txt). setup-wildfly.ps1 usa la misma carpeta recordada.
    - Acepta la carpeta de WildFly o la que la contiene (zip descomprimido con carpeta anidada).
    WildFly queda corriendo en esta ventana: se detiene con Ctrl+C.
#>
param(
    [string]$WildflyHome,
    [switch]$CambiarCarpeta   # vuelve a preguntar la carpeta de WildFly
)
$ErrorActionPreference = "Stop"
$recordada = Join-Path $env:APPDATA "InversorAR\wildfly-home.txt"

function Falla($texto) { Write-Host ""; Write-Host "ERROR: $texto" -ForegroundColor Red; Read-Host "Enter para cerrar"; exit 1 }
# Devuelve la carpeta que contiene bin\standalone.bat: la indicada o una subcarpeta directa.
function CarpetaWildfly([string]$ruta) {
    if (-not $ruta) { return $null }
    $ruta = $ruta.Trim().Trim('"')
    if (Test-Path (Join-Path $ruta "bin\standalone.bat")) { return (Resolve-Path $ruta).Path }
    $sub = Get-ChildItem $ruta -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName "bin\standalone.bat") } | Select-Object -First 1
    if ($sub) { return $sub.FullName }
    return $null
}

# 1. Carpeta: parámetro > recordada > WILDFLY_HOME > preguntar.
$wf = CarpetaWildfly $WildflyHome
if (-not $wf -and -not $CambiarCarpeta -and (Test-Path $recordada)) { $wf = CarpetaWildfly (Get-Content $recordada -Raw) }
if (-not $wf -and -not $CambiarCarpeta) { $wf = CarpetaWildfly $env:WILDFLY_HOME }
while (-not $wf) {
    $r = Read-Host "Carpeta de WildFly (donde está la carpeta bin)"
    $wf = CarpetaWildfly $r
    if (-not $wf) { Write-Host "  No se encontró bin\standalone.bat en '$r'. Probar de nuevo." -ForegroundColor Yellow }
}
New-Item -ItemType Directory -Force (Split-Path $recordada) | Out-Null
Set-Content $recordada $wf -Encoding UTF8
Write-Host "WildFly: $wf" -ForegroundColor Cyan

# 2. ¿Ya hay algo usando los puertos de WildFly?
$ocupados = Get-NetTCPConnection -LocalPort 8080, 9990 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
if ($ocupados) {
    foreach ($id in $ocupados) {
        $p = Get-Process -Id $id -ErrorAction SilentlyContinue
        Write-Host ""
        Write-Host "Ya hay un programa usando los puertos de WildFly (8080/9990): $($p.ProcessName), proceso $id." -ForegroundColor Yellow
        if ($p.ProcessName -ne "java") { Falla "No parece un WildFly. Cerrar ese programa y volver a intentar." }
        $resp = Read-Host "Probablemente es un WildFly ya iniciado. ¿Cerrarlo y arrancar de nuevo en modo full? (s/N)"
        if ($resp -notmatch '^[sS]') {
            Write-Host "No se inició otro WildFly. Se puede seguir usando el que ya está corriendo."
            Read-Host "Enter para cerrar"; exit 0
        }
        Stop-Process -Id $id -Force
    }
    for ($i = 0; $i -lt 15 -and (Get-NetTCPConnection -LocalPort 8080, 9990 -State Listen -ErrorAction SilentlyContinue); $i++) { Start-Sleep 1 }
    Write-Host "WildFly anterior cerrado." -ForegroundColor Green
}

# 3. Arrancar en modo full, en esta misma ventana.
Write-Host ""
Write-Host "Iniciando WildFly en modo full (standalone-full.xml). Para detenerlo: Ctrl+C." -ForegroundColor Cyan
Write-Host "Está listo cuando aparece 'started in'. Luego: http://localhost:8080/inversorar/dashboard" -ForegroundColor Cyan
Write-Host ""
& (Join-Path $wf "bin\standalone.bat") -c standalone-full.xml
