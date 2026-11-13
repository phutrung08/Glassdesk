@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0push-wallpapers.ps1" %*
pause
