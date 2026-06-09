@echo off
cd /d "%~dp0"
start "travel-ticket-service" /B cmd /c ""%~dp0run-local.cmd" > "%~dp0backend-run.log" 2> "%~dp0backend-run.err.log""
