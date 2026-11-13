@echo off
REM ============================================================
REM  ATPPuestos - RUN (arranca el jar ya compilado)
REM  Perfil dev -> SQL Server RENTAS.
REM
REM  Requiere el jar: target\atp-puestos-camineros-0.1.0-SNAPSHOT.jar
REM  Si no existe, compilarlo primero con:  run-mvn-jdk17.cmd
REM  Para detener: Ctrl+C
REM ============================================================
setlocal enableextensions

REM --- JDK 17 (el java del PATH suele ser 1.8; forzamos el 17) ---
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM --- Cargar credenciales desde .env (raiz del repo) ---
REM  La app NO trae cargador de .env: exportamos las variables a esta sesion.
REM  Se ignora toda linea con valor vacio para no anular los defaults de
REM  application.yml (p.ej. SPRING_DATASOURCE_URL= vacio).
call :load_env "%~dp0.env"

REM --- Credenciales: NUNCA en este archivo (regla AGENTS.md) ---
REM  application.yml lee DB_USER / DB_PASSWORD (o SPRING_DATASOURCE_USERNAME /
REM  SPRING_DATASOURCE_PASSWORD). Precedencia: la sesion gana sobre los archivos.
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

REM --- Verificacion: el jar debe existir ---
set "JAR=target\atp-puestos-camineros-0.1.0-SNAPSHOT.jar"
if not exist "%JAR%" (
    echo ERROR: no existe %JAR%
    echo Compilalo primero:  run-mvn-jdk17.cmd
    exit /b 1
)

echo Levantando servicio en http://localhost:8080 ...
echo   JDK : %JAVA_HOME%
echo   jar : %JAR%
if defined DB_USER echo   BD  : %DB_USER%@%DB_HOST%:%DB_PORT%/%DB_NAME%
echo.

java -jar "%JAR%" --spring.profiles.active=dev
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
