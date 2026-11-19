@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-hemopet.ps1"
if errorlevel 1 (
    echo.
    echo Nao foi possivel iniciar o HemoPet. Veja a mensagem acima.
    pause
)
endlocal
