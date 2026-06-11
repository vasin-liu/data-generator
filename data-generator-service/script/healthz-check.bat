@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0healthz-check.ps1" %*
exit /b %ERRORLEVEL%
