@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo === 1/4: Matando procesos java.exe ===
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo === 2/4: Recompilando e instalando nucleo (base-stitch + imagenes) ===
call mvnw.cmd -pl atp-nucleo-compartido clean install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR en nucleo. Abortando.
    pause
    exit /b 1
)

echo === 3/4: Recompilando puestos ===
call mvnw.cmd -pl atp-puestos-camineros clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR en puestos. Abortando.
    pause
    exit /b 1
)

echo === 4/4: Arrancando la app ===
start "ATPPuestos" cmd /c "java -jar atp-puestos-camineros\target\atp-puestos-camineros-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev"