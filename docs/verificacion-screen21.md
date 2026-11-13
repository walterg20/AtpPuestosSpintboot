# Verificacion QA - SCREEN_21 actas-listado.html

## Veredicto: PASS

Comando ejecutado:
```
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -pl atp-puestos-camineros test
```
Resultado: Tests run: 215, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS.
Log completo: C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-actas.log.

Comando focalizado (controller):
```
.\mvnw.cmd -pl atp-puestos-camineros test "-Dtest=ActasMvcControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```
Resultado: Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS.
Log: C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-actas-controller.log.

## Checklist SCREEN_21

1. GET /actas devuelve 200 y vista puestos/actas-listado - OK.
   Evidencia: ActasMvcControllerTest.getActasDevuelveLaVista (lineas 86-90) pasa; controller
   ActasController.VISTA_LISTA = "puestos/actas-listado" (linea 57) y @GetMapping (linea 101)
   retorna esa constante (linea 135). Render real verificado por @WebMvcTest (15 tests verdes).

2. actas-listado.html existe y sin frameworks CSS externos - OK.
   Ruta: atp-puestos-camineros/src/main/resources/templates/puestos/actas-listado.html.
   Grep de bootstrap|tailwind|cdn.|unpkg|jsdelivr|node_modules y de <link remoto: sin coincidencias.
   Unico <link rel="stylesheet"> apunta a @{/css/actas-listado.css} (linea 184).

3. actas-listado.css y actas-listado.js existen - OK.
   Rutas:
   - atp-puestos-camineros/src/main/resources/static/css/actas-listado.css
   - atp-puestos-camineros/src/main/resources/static/js/actas-listado.js

4. Preservacion de th:object y th:each para actas - PARCIAL/OBSERVACION.
   - th:each para actas: PRESENTE - linea 133: <tr th:each="a : ${actasPage.content}">.
   - th:each para paginador: PRESENTE - linea 164.
   - th:object: el template usa th:object="${filtros}" (linea 69), NO th:object="${filtroActas}".
     El controller expone AMBOS atributos al modelo (filtroActas linea 127 y filtros linea 128)
     apuntando al mismo objeto, por lo que el binding funciona (tests getActasRenderizaLosFiltros
     verdes). Es una diferencia de nombre respecto al checklist, no un defecto funcional.
     Se sugiere alinear a th:object="${filtroActas}" si se quiere paridad literal con el legacy
     y con el criterio del checklist, pero no bloquea.

5. resumenMetricas se pasa al modelo - OK.
   Controller linea 134: model.addAttribute("resumenMetricas", actaService.resumenMetricas(contexto));
   Consumido en la vista (lineas 48, 53, 58, 63) con guardas null. No hay test dedicado que assert
   el atributo, pero el render @WebMvcTest pasa.

6. Tests pasan - OK. 215/215, 0 fallos, BUILD SUCCESS.

## Hallazgos (ordenados por severidad)

- BAJA - Nombre de binding del filtro en la vista nueva. actas-listado.html usa
  th:object="${filtros}" mientras el criterio/checklist y el legacy (actas-lista.html)
  usan ${filtroActas}. Ambos atributos existen en el modelo (controller lineas 127-128), por lo
  que no hay impacto funcional. Agente responsable: implementador-vistas (opcional, alineacion).
- BAJA - Cobertura de higiene de recursos para los nuevos archivos. ActasRecursosTest
  (lineas 21-26) lista los recursos viejos (actas.css, actas.js, actas-lista.html) y no
  incluye actas-listado.css, actas-listado.js ni actas-listado.html. Los nuevos recursos
  pasan la inspeccion manual (sin frameworks/CDN), pero no estan cubiertos por la suite. Agente
  responsable: implementador-vistas / tests.

## Confirmacion final
- Veredicto: PASS.
- Log autoritativo: C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-actas.log
  (Tests run: 215, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS).
- No se modifico codigo productivo ni tests.
