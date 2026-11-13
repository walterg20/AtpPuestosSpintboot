# Verificación QA — Cierre hallazgos menores SCREEN_21 (`actas-listado.html`)

**Veredicto**: PASS

**Comando exacto ejecutado**:
```
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; cd D:\Desrrollo\ATPPuestoLab\AtpPuestosSpintboot; .\mvnw.cmd -B -pl atp-puestos-camineros test
```
**Resultado**: `Tests run: 215, Failures: 0, Errors: 0, Skipped: 0` · `BUILD SUCCESS` (exit code 0).
**Log**: `C:\Users\walterg20\AppData\Local\Temp\opencode\qa-actas-test.log`

## Confirmación de los dos hallazgos menores

1. `actas-listado.html` usa `th:object="${filtroActas}"`:
   - Ruta: `atp-puestos-camineros/src/main/resources/templates/puestos/actas-listado.html`
   - Línea 69: `th:object="${filtroActas}">` ✓

2. `ActasRecursosTest` incluye `actas-listado.css` y `actas-listado.js`:
   - Ruta: `atp-puestos-camineros/src/test/java/ar/com/atp/puestos/puestos/web/mvc/ActasRecursosTest.java`
   - Línea 23: `"/static/css/actas-listado.css",` ✓
   - Línea 25: `"/static/js/actas-listado.js",` ✓
   - Ejecución de `ActasRecursosTest`: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` ✓

No se modificó código productivo ni tests.
