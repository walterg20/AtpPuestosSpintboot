# Verificación — Gate 2 — Módulo `12-flujo-carga-manual-turno`

**Veredicto:** `FAIL` (ver §6, hallazgo H-12-01)

**Fecha:** 2026-11-04
**Agente:** verificador-qa
**Alcance auditado:** `openspec/features/12-flujo-carga-manual-turno/{spec.md,plan.md}` (R-TURNO-001…R-TURNO-016).

**Logs:**
- Unitarios: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-unit-12.log`
- IT (BD real): `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-it-12.log`

---

## 1. Unitarios (MockMvc + JUnit 5)

**Comando exacto:**

```powershell
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B clean test
```

**Resultado:** `BUILD SUCCESS`.

Resumen autoritativo:
- `atp-nucleo-compartido`: `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0` (módulo; total acumulado del reactor: 143)
- `atp-puestos-camineros`: `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`
- Total reactor: **Tests run: 143, Failures: 0, Errors: 0, Skipped: 0**.

Clases relevantes en verde: `ActasMvcControllerTest`, `ActasMvcFormularioTest`, `ActaServiceTest`, `ActaAltaTest`, `CargaMvcControllerTest`, `ProductoMvcCsrfTest`, `PuestosApplicationTests`.

## 2. Integración contra BD real (IT)

**Comando exacto:**

```powershell
$env:ATP_DB_IT="true"
$env:DB_HOST="devmssqlatp.ecom.com.ar"
$env:DB_PORT="1433"
$env:DB_NAME="Rentas"
$env:DB_USER="rentas"
$env:DB_PASSWORD="dev28rentas"
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Resultado:** `BUILD SUCCESS`. `Tests run: 26, Failures: 0, Errors: 0, Skipped: 1` (el skipped es el caso de comisión inexistente de `ComisionRepositoryIT`, preexistente).

Suites en verde que cubren no-regresión de módulos previos: `TurnoRepositorioIT` (06-turnos), `ActaAltaIT`, `ActaMapeoIT`, `ActaNumeracionIT`, `ActaRepositoryBusquedaIT`, `ActaRepositoryIT`, `ContadorFormularioRepositorioIT`, `CargaMarcadoIT`, login (`T-304/T-305`).

## 3. No-regresión de módulos 01/02/03/06/08/09

- La corrida completa de unitarios (§1) y de IT (§2) queda en verde, sin fallos ni errores.
- No se detectan cambios de regresión en las suites de `09-formularios-acta` (`ActaServiceTest`, `ActasMvcFormularioTest`, `ActaAltaIT`, etc.).

## 4. Gobernanza estática

| Control | Resultado | Evidencia |
|---|---|---|
| G-POSTMAN (sin diff de colección) | PASS | `git status --short -- docs/postman` vacío; el módulo no agrega endpoints REST (D-TURNO-06). |
| G-SEC (sin secretos en texto plano) | PASS | `application.yml` usa `${DB_PASSWORD:}` / `${SPRING_DATASOURCE_PASSWORD:}`; `.env` no versionado; `ddl-auto=none`. |
| G-DATA (sin DDL ni migraciones) | PASS | No se encontraron `schema.sql`/`data.sql`/Flyway/Liquibase; `hibernate.ddl-auto: none`. |
| Archivos de configuración < 500 líneas | PASS (config) | `application.yml` < 500; no hay archivos de configuración > 500 en `src/`. |
| Gobernanza de código (observación) | INFO | `ActaService.java` tiene 678 líneas (>500) pero es código de servicio, no configuración; el guardarraíl aplica a archivos de configuración. |

## 5. Paridad de BD (inspección)

- Se reutilizan `nucleo.domain.Turno` (`PPRIM_TU`, 12 columnas) y `puestos.domain.RangoManualFormulario` (`FORMSUCU`, 8 columnas). `ForRecFin` no se mapea (fórmula) — coherente con `spec.md` §3.5/§3.6.
- `TurnoRepositorio.listarAbiertosPorSucursal(...)` agrega la consulta con `turEstado = 'A'`, filtro por `sucNroId`/`sucTipo` y orden `SucNroId, SucTipo, TurNroId` (paridad con `ControlWhere`/`ControlOrder`).
- `ddl-auto=none` confirmado.

## 6. Verificación por requisito (R-TURNO-*)

Estado: **por inspección de código/vista** (no hay pruebas automatizadas dedicadas al módulo 12; ver H-12-01).

| Requisito | Estado | Evidencia (código) |
|---|---|---|
| R-TURNO-001 | PASS (inspección) | `fragments/menu.html` enruta "Carga Manual" a `/actas/carga-manual`. |
| R-TURNO-002 | PASS (inspección) | `CargaManualController.cargaManual` + `turnos-abiertos.html` título "Seleccione un Turno", paginación 10. |
| R-TURNO-003 | PASS (inspección) | `ActasController.nueva(turNroId, manual)` + `ActaForm.turNroId`. |
| R-TURNO-004 | PASS (inspección) | `ActaService.esTurnoAbierto` + validación en `guardar(..., manual=true)`. |
| R-TURNO-005 | PASS (inspección) | `TurnoRepositorio.listarAbiertosPorSucursal` filtra `SucNroId`/`SucTipo`. |
| R-TURNO-006 | PASS (inspección) | `turnos-abiertos.html` estado vacío sin grilla. |
| R-TURNO-007 | PASS (inspección) | Reutiliza `ActaService`, `acta-formulario.html`. |
| R-TURNO-008 | PASS (inspección) | Campo `F276Id` editable con `th:if="${manual}"`. |
| R-TURNO-009 | PASS (inspección) | `guardar` rechaza `F276Id == 0` con `MSG_NRO_FORMULARIO_GENERADO`. |
| R-TURNO-010 | PASS (inspección) | `validarNumeroManual` contra rango `[ForRecIni, ForRecIni+ForRecCant-1]`. |
| R-TURNO-011 | PASS (inspección) | `obtenerTipoActaManual` deriva `'E'`/`'S'`. |
| R-TURNO-012 | PASS (inspección) | `acta.setTurNroId(turNroIdSeleccionado)` (turno seleccionado). |
| R-TURNO-013 | PASS (inspección) | En modo manual no llama `ServicioNumeracionActa`. |
| R-TURNO-014 | PASS (inspección) | Validaciones/persistencia reutilizadas. |
| R-TURNO-015 | PASS (inspección) | Estado vacío sin filas seleccionables. |
| R-TURNO-016 | PASS (inspección) | Rechazo con `MSG_TURNO_CERRADO` verbatim y sin escritura. |

## 7. Hallazgos

### H-12-01 — Ausencia de pruebas automatizadas del módulo 12 (Severidad: Alta)

- **Síntoma:** no existe clase de test para `CargaManualController` ni escenarios `manual=true` en `ActasMvcFormularioTest`/`ActaServiceTest`. Las suites existentes (143 unitarios + 26 IT) no ejercitan R-TURNO-001…R-TURNO-016. La spec exige verificación por `@WebMvcTest`/IT (`MvcMenuCargaManual`, `MvcTurnosAbiertosVacio`, `MvcFormularioManual`, `ActaManualIT`, `ActaServiceTest#turnoCerrado`/`#manual`), ninguna de las cuales existe.
- **Evidencia:** búsqueda de `carga-manual|turnos-abiertos|CargaManual|turnosAbiertos` en `atp-puestos-camineros/src/test` → sin resultados; `grep` de `manual`/`Manual` en tests → sin referencias a la ruta/modo manual.
- **Causa raíz probable:** implementación entregada sin las tareas de prueba del plan (tasks.md §F, todas en `[ ]`).
- **Agente responsable:** implementador-vistas / implementador-backend (agregar tests) — fuera del alcance de este verificador.

### H-12-02 — Spec/plan/tasks en estado BORRADOR (Severidad: Media)

- **Síntoma:** `spec.md`, `plan.md` y `tasks.md` declaran estado `BORRADOR (v1, 2026-11-04)` pendiente de aprobación humana (G-SPEC); todas las tareas de `tasks.md` están en `[ ]`. El código fue implementado sobre un spec no aprobado.
- **Evidencia:** `openspec/features/12-flujo-carga-manual-turno/spec.md` línea 3; `tasks.md` (todas las casillas sin marcar).
- **Agente responsable:** coordinador (aprobación humana + cierre de tasks).

## 8. Conclusión

La implementación del módulo 12 está presente y es coherente con la spec por inspección, y las suites existentes (unitarias + IT contra BD real) pasan en verde sin regresión. Sin embargo, **no hay evidencia automatizada** de los requisitos R-TURNO-001…R-TURNO-016, por lo que el Gate 2 no puede declararse `PASS`. Veredicto: **`FAIL`** hasta que se agreguen y ejecuten las pruebas del módulo (H-12-01) y se cierre el spec (H-12-02).
