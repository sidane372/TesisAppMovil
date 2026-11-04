@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Sistema-Local.ps1" -Accion Estado
pause
