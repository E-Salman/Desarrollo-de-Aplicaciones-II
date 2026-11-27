# Levanta el simulador del mercado legado (SOAP) en esta consola. Ctrl+C lo apaga.
# Usa el JDK de JAVA_HOME (el "java" del PATH puede ser una versión vieja que no corre archivos .java sueltos).
# Uso:  powershell -ExecutionPolicy Bypass -File legado\iniciar-mercado.ps1 [--modo=fault] [--suspendidos=BTCUSDT]
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
if (-not (Get-Command $java -ErrorAction SilentlyContinue)) { Write-Host "No se encontró Java. Apuntar JAVA_HOME a un JDK 17 o superior." -ForegroundColor Red; exit 1 }
& $java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" (Join-Path $PSScriptRoot "MercadoLegado.java") @args
