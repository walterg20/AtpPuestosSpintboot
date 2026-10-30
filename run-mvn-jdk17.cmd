@echo off
REM ============================================================
REM  ATPPuestos - BUILD (compila y empaqueta el jar)
REM  Monorepo Maven: el artefacto desplegable es atp-puestos-camineros.
REM  Usa JDK 17 (el java del PATH suele ser 1.8).
REM
REM  Uso:
REM     run-mvn-jdk17.cmd                  -> -pl atp-puestos-camineros -am clean package -DskipTests
REM     run-mvn-jdk17.cmd test             -> corre los tests (H2, sin BD)
REM     run-mvn-jdk17.cmd clean verify     -> build completo + tests
REM  Genera: atp-puestos-camineros\target\atp-puestos-camineros-0.1.0-SNAPSHOT.jar
REM ============================================================
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

REM --- Guard: si ATPPuestos ya esta corriendo, el jar queda bloqueado y
REM     maven-clean-plugin no lo puede borrar (BUILD FAILURE). Cerramos la
REM     instancia previa (solo si en :8080 hay un java) antes de compilar.
set "RUNPID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /i /c:"LISTENING" ^| findstr /c:":8080"') do set "RUNPID=%%P"
if not defined RUNPID goto :build
for /f "tokens=1 delims=," %%N in ('tasklist /FI "PID eq %RUNPID%" /FO CSV /NH 2^>nul') do set "RUNEXE=%%~N"
if /i not "%RUNEXE%"=="java.exe" (
    echo ADVERTENCIA: el puerto 8080 esta ocupado por "%RUNEXE%" ^(PID %RUNPID%^), no por ATPPuestos.
    echo   No se cierra automaticamente. Si impide compilar, cierralo a mano.
    goto :build
)
echo La app ATPPuestos ya esta corriendo ^(java PID %RUNPID% en :8080^).
echo Cerrando esa instancia para poder recompilar...
taskkill /F /PID %RUNPID% >nul 2>&1
ping -n 2 127.0.0.1 >nul

:build
if "%~1"=="" (
    echo [build] -pl atp-puestos-camineros -am clean package -DskipTests
    call mvnw.cmd -pl atp-puestos-camineros -am clean package -DskipTests
) else (
    echo [build] %*
    call mvnw.cmd %*
)
set "RC=%ERRORLEVEL%"

endlocal & exit /b %RC%