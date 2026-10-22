---
description: "Verificación automatizada (tests backend/vistas, paridad con GeneXus y gobernanza)"
agent: verificador-qa
subagent: true
---

Alcance a verificar: $ARGUMENTS

Ejecutá la verificación completa y reportá con evidencia.

1. Unitarios: `cd <raíz>` y `.\mvnw.cmd -B clean test`. Informá el resumen autoritativo
   (`Tests run: ...`).
   > Con `set JAVA_HOME=C:\Program Files\Java\jdk-17`: el `java` del PATH suele ser 1.8.
2. Integración contra la base real: si el alcance incluye repositorios o escrituras, corré las
   clases `*IT` con `ATP_DB_IT=true` y las variables de conexión cargadas desde `.env`:
   `.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"`.
   > En PowerShell los argumentos con punto van entrecomillados.
3. Vistas: que exista prueba por pantalla (render, binding, redirección, render ante error) y que
   por inspección los formularios de escritura usen `th:action` (CSRF), los errores de validación
   se muestren junto al campo, y no haya literales de estilo fuera del archivo de tokens.
4. Paridad: contrastá consultas y comportamiento contra la definición GeneXus, incluyendo
   `RTRIM` vs `trim`, `COLLATE`, acentos y ordenamientos.
5. Seguridad: sin secretos en texto plano, `ddl-auto=none`, sin DDL ni migraciones, CSRF activo;
   y si el alcance toca login, probá contra la BD real los casos: clave correcta, clave incorrecta,
   usuario inexistente, usuario sin clave legacy y usuario con `UsuIngreso=1`.
6. Gobernanza: archivos de configuración por debajo de 500 líneas y G-POSTMAN sincronizado con
   los endpoints expuestos.

Devolvé: veredicto `PASS` / `FAIL` / `BLOQUEADO`, y los hallazgos por severidad con evidencia
(ruta de log o cita del reporte), causa raíz probable y agente responsable del arreglo. Si algo no
se pudo ejecutar, decilo explícitamente en lugar de asumirlo en verde.
