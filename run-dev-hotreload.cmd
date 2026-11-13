@echo off
REM ============================================================
REM  ATPPuestos - RUN DEV (hot reload con spring-boot-devtools)
REM  Fuerza JDK 17, carga .env y arranca el modulo con perfil dev.
REM  Para detener: Ctrl+C
REM ============================================================
setlocal enableextensions

REM --- JDK 17 (el java del PATH suele ser 1.8; forzamos el 17) ---
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM --- Cargar credenciales desde .env (raiz del repo) ---
call :load_env "%~dp0.env"

REM --- Credenciales: NUNCA en este archivo (regla AGENTS.md) ---
if not defined DB_USER if not defined SPRING_DATASOURCE_USERNAME (
    echo ERROR: no hay credenciales de base de datos.
    echo.
    echo   Opcion 1: completar DB_USER / DB_PASSWORD en el .env de la raiz
    echo             %~dp0.env     ^(plantilla: .env.example^)
    echo.
    echo   Opcion 2: definirlas en esta sesion y reintentar
    echo             set DB_USER=...
    echo             set DB_PASSWORD=...
    exit /b 1
)

cd /d "%~dp0"

REM --- Perfil dev (hot reload de vistas/recursos) ---
set "SPRING_PROFILES_ACTIVE=dev"

echo Levantando servicio en http://localhost:8080 (hot reload dev) ...
echo   JDK : %JAVA_HOME%
if defined DB_USER echo   BD  : %DB_USER%@%DB_HOST%:%DB_PORT%/%DB_NAME%
echo   Para detener: Ctrl+C
echo.

call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
set "RC=%ERRORLEVEL%"

endlocal & exit /b %RC%

REM ------------------------------------------------------------
REM  :load_env <archivo>  ->  exporta KEY=VALUE de un .env.
REM  Omite comentarios (eol=#) y lineas con valor vacio.
REM ------------------------------------------------------------
:load_env
if not exist %1 exit /b 0
for /f "usebackq eol=# tokens=1* delims==" %%A in (%1) do (
    if not "%%A"=="" if not "%%B"=="" set "%%A=%%B"
)
exit /b 0

