@echo off
rem Doble clic: inicia WildFly en modo full para InversorAR (ver iniciar-wildfly.ps1).
title WildFly - InversorAR
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-wildfly.ps1" %*
