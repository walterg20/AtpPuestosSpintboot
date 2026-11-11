@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo === 1/4: Matando procesos java.exe ===
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul