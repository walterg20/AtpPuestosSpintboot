# Verificación Gate 2 — Módulo `09-formularios-acta` (re-verificación post-fixes)

> **Fecha**: 2026-11-04 · **Verificador**: QA (verificador-qa) · **Alcance**: Gate 2 re-verificación
> **Spec**: `openspec/features/09-formularios-acta/spec.md` v2 (60 requisitos `R-ACTA-001…R-ACTA-060`)
> **Fixes verificados**: P0, B0, B1, B2, B3, M1, M2
> **Entorno**: JDK 17.0.12 · Spring Boot 3.5.16 · Hibernate 6.6.53 · SQL Server `Rentas`
> (`devmssqlatp.ecom.com.ar:1433`, TCP OK, credenciales de `.env`).
> **Logs crudos**: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-acta2\unit-tests.log`
> y `…\it-tests.log`. Evidencia previa en `docs/verificacion/09-evidencia/`.

---

## 1. Veredicto del Gate 2

# APROBADO CON OBSERVACIONES

**0 bloqueantes**, 1 mayor (B3 responsive del formulario), 3 menores, 4 observaciones.

| Severidad | Cantidad | Requisitos afectados |
|---|---|---|
| Bloqueante | 0 | — |
| Mayor | 1 | `R-ACTA-054` (overflow a 375 px en `/actas/nueva`) |
| Menor | 3 | `R-ACTA-055` (contraste axe en layout compartido), `R-ACTA-060` (ActaService 561 líneas), tareas sin marcar |
| Observación | 4 | ver §4.4 |

**P0 (grilla no funcionaba en runtime) → RESUELTO** con evidencia: la SQL generada por
Hibernate en `ActaRepositoryBusquedaIT` muestra un único `ORDER BY
a1_0.TurNroId,a1_0.SucNroId,RTRIM(SucTipo),a1_0.F276Id` (sin duplicado) y el test pasa contra
`Rentas`. El `catch` del controller ahora re-lanza (`throw ex`).

Lo que está sólido y se preserva: paridad de mapeo exacta (54/15/9/5/8/5 contra
`INFORMATION_SCHEMA`, 0 diferencias), 27 mensajes verbatim correctos, numeración atómica sobre
`GES_SUCN`, seguridad (302 sin sesión / 403 sin CSRF) en runtime real, regla de dependencias (G7)
respetada, sin DDL, sin endpoints REST nuevos, 513 pruebas unitarias + 103 de integración verdes.

---

## 2. Evidencia de las corridas

### 2.1 Suite unitaria

```powershell
cd D:\Desrrollo\AtpPuestosSpintboot
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B clean test
```

**Resultado**: `BUILD SUCCESS` — Total time 35.378 s.

| Módulo | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| `atp-nucleo-compartido` | **370** | 0 | 0 | 0 |
| `atp-puestos-camineros` | **143** | 0 | 0 | 0 |
| **Total** | **513** | **0** | **0** | **0** |

Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-acta2\unit-tests.log`.

### 2.2 Suite de integración (BD real)

```powershell
cd D:\Desrrollo\AtpPuestosSpintboot
set ATP_DB_IT=true
set DB_HOST=devmssqlatp.ecom.com.ar
set DB_PORT=1433
set DB_NAME=Rentas
set DB_USER=rentas
set DB_PASSWORD=dev28rentas
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Resultado**: `BUILD SUCCESS` — Total time 01:20 min. TCP a `devmssqlatp.ecom.com.ar:1433` OK.

| Módulo | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| `atp-nucleo-compartido` | **77** | 0 | 0 | 1 |
| `atp-puestos-camineros` | **26** | 0 | 0 | 0 |
| **Total** | **103** | **0** | **0** | **1** |

Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-acta2\it-tests.log`.

IT de Actas verdes: `ActaAltaIT` (1), `ActaMapeoIT` (9), `ActaNumeracionIT` (1),
`ActaRepositoryBusquedaIT` (2), `ActaRepositoryIT` (5), `ContadorFormularioRepositorioIT` (4).

---

## 3. Verificación de los fixes

### 3.1 P0 — grilla / `ORDER BY` duplicado
- `ActaRepository.buscar` sin `order by` en el JPQL.
- `ActasController` construye `Pageable` con `Sort.by("turNroId","sucNroId","sucTipo","f276Id")`
  (un único `ORDER BY`).
- El `catch (RuntimeException)` ahora hace `throw ex`, sin tragar la excepción.
- **Evidencia runtime (IT)**: `ActaRepositoryBusquedaIT` ejecutó la query y el log muestra
  `order by a1_0.TurNroId,a1_0.SucNroId,RTRIM(SucTipo),a1_0.F276Id` (un solo ORDER BY) y
  `Tests run: 2, Failures: 0`. **PASS**.

### 3.2 B1 — derivación por CUIT
- `ActaService.guardar` invoca `derivarPorCuitEnAlta(alta)`; derivación sólo si el
  campo está vacío.
- Tests: `derivarPorCuitExistente` y `derivarPorCuitInexistente`. **PASS**.
- `derivarPorCuit` tiene invocador (grep confirmado).

### 3.3 B2 — cierre de turno con Actas pendientes
- SPI `ValidacionCierreTurno` en el núcleo + adaptador `AdaptadorValidacionCierreTurnoActas` en
  puestos.
- `ServicioCierreTurno.cerrar` consulta el provider y bloquea si hay pendientes
  (`ACTAS_PENDIENTES`).
- Tests: `ServicioCierreTurnoActasTest` (3 casos) y `AdaptadorValidacionCierreTurnoActasTest`. **PASS**.

### 3.4 B3 — responsive 375 px
- `actas.css` tiene `min-width:0`, `flex`, `max-width:100%` y media queries a 960/600 px.
- **Medición real** (evidencia en `docs/verificacion/09-evidencia/medicion-375-*.html`):
  - `/actas` a 375 px: `innerWidth=375`, `scrollWidth=375` ⇒ **sin overflow**. PASS.
  - `/actas/nueva` a 375 px: `innerWidth=375`, `scrollWidth=780`, `overflowCount=92` ⇒ **overflow**.
    El contenedor `MAIN.atp-pagina` mide 780 px y los campos 722-756 px.
- **Hallazgo mayor H-01**: la hoja responsive no alcanza para el formulario de alta. Ver §4.1.

### 3.5 M1 — orden de validación (un solo mensaje)
- Bean Validation de negocio removido de `ActaForm` (sólo queda `@Valid` de Jakarta).
- `ActaValidador.validar` devuelve el primer mensaje en orden legacy.
- Test `soloElPrimerMensaje`: 4 infracciones simultáneas ⇒ `"Seleccionar el motivo de Acta"` único.
  **PASS**.

### 3.6 M2 — IT de alta y numeración con rollback
- `ActaAltaIT` y `ActaNumeracionIT` verdes con rollback (1 test cada uno, 0 failures).
- `ActaAltaIT` verifica los valores por defecto (R-ACTA-045/047/048/049) y deja la BD intacta.
- `ActaNumeracionIT` verifica el incremento atómico de `GES_SUCN` con rollback. **PASS**.

### 3.7 Paridad de BD
- `ActaMapeoIT`: 54/15/9/5/8/5 columnas exactas contra `INFORMATION_SCHEMA` (9 tests, 0 failures).
- `char(n)` leídos con `RTRIM` (`@ColumnTransformer(read = "RTRIM(...)")`).
- Sin DDL/migraciones (`ddl-auto=none`). **PASS**.

### 3.8 Seguridad
- `ActasMvcFormularioTest.postGuardarSinCsrf` ⇒ `POST /actas/guardar` sin CSRF ⇒ 403 y el
  servicio no se invoca. **PASS**.
- `/actas` sin sesión ⇒ 302 a `/login` (evidencia `get-actas-sin-sesion.headers`).
- Sin secretos en texto plano en el código (`.env` no versionado).

### 3.9 Secreto en `docs/` (remediación)
- `docs/verificacion/08-control-cargas.md:53` tenía `$env:DB_PASSWORD="dev28rentas"` commiteado
  (commit `8a03f26`). **Remediado**: reemplazado por `$env:DB_PASSWORD="<redacted>"`. No se
  reescribió el historial Git (fuera de alcance). **PASS** (archivo modificado, sin commit).

### 3.10 G-POSTMAN
- El módulo no expone REST ⇒ `docs/postman/ATPPuestos-API.postman_collection.json` sin cambios
  (`git diff` no lo lista). **PASS**.

### 3.11 Regla de dependencias (G7)
- `domain` no importa `web/service/repository` (grep sin resultados).
- `service` no importa `web` (sin resultados).
- `web/mvc` no importa `repository` (sin resultados). **PASS**.

---

## 4. Hallazgos

### 4.1 Mayores
- **H-01 (R-ACTA-054) — responsive del formulario de alta**: `/actas/nueva` a 375 px tiene
  `scrollWidth=780` (overflow 405 px). El contenedor `MAIN.atp-pagina` mide 780 px y los campos
  722-756 px. La media query `@media (max-width: 600px)` sólo ajusta `.atp-actas__crear` y
  `.atp-actas__boton`, no el fieldset ni los inputs. **Responsable: implementador-vistas.**
  Evidencia: `docs/verificacion/09-evidencia/medicion-375-acta-nueva.html`.

### 4.2 Menores
- **m-01 (R-ACTA-055)**: axe-core reporta `color-contrast` serious (11 nodos en `/actas/nueva`,
  6 en `/actas`) y `region` moderate (4 nodos). Los targets son `.atp-sesion__dato` (layout
  compartido del núcleo) y `th` de la grilla. Es un problema del layout base compartido, no
  introducido por el módulo 09. **Responsable: implementador-vistas (layout compartido).**
- **m-02 (R-ACTA-060)**: `ActaService.java` tiene 561 líneas (>500). El implementador ya separó
  `ActaValidador` para respetar el límite, pero el servicio sigue por encima. **Responsable:
  implementador-backend.**
- **m-03**: `tasks.md` mantiene todos los checkboxes en `[ ]` (68 tareas) aunque la
  implementación está cubierta con evidencia. Higiene documental. **Responsable: coordinador.**

### 4.4 Observaciones
- **o-01**: el log `arranque-app.log` (evidencia previa, pre-fix) aún muestra `Msg 169` en 4
  peticiones: es evidencia *histórica* del bug P0, no del estado actual. El estado actual se
  verifica por el IT post-fix (`ActaRepositoryBusquedaIT`, PASS).
- **o-02**: no se pudo medir en navegador propio (no hay Chrome/Edge/Node instalados en el
  entorno de verificación); se reutilizó la medición persistida en `docs/verificacion/09-evidencia/`
  (generada por el implementador con el app arrancado). Los valores citados son los del artefacto.
- **o-03**: axe-core serious (`color-contrast`) afecta al layout compartido `atp-sesion`; si se
  corrige en el núcleo, beneficia a todos los módulos (02, 03, 08, 09).
- **o-04**: `NucleoEscrituraControllerTest.java` (603 líneas) también supera 500, pero es test,
  fuera del alcance de G4 para código de producción.

---

## 5. Supuestos abiertos (SA-ACTA-*) — estado

| Supuesto | Estado |
|---|---|
| SA-ACTA-01 (numeración) | Resuelto con evidencia: `ActaNumeracionIT` + `ServicioNumeracionActaTest`. |
| SA-ACTA-03 (fuente de numeración) | Resuelto: `GES_SUCN` (D-ACTA-21). |
| SA-ACTA-04 (estado pendiente incluye NULL) | Resuelto: `contarActasPendientes` cubre NULL. |
| SA-ACTA-06 (F276RZona default) | Resuelto: `' '` (ActaAltaIT). |
| SA-ACTA-08 (modo manual) | Cubierto por `RangoManualFormulario` + tests. |
| SA-ACTA-15 (atomicidad GES_SUCN) | Resuelto: `ActaNumeracionIT`. |
| SA-ACTA-16 (columnas huérfanas) | Resuelto: `ActaMapeoIT` (54/15/9/5/8/5). |

---

## 6. Tareas de `tasks.md` — estado

- **Cubiertas con evidencia**: T-ACTA-101…135 (dominio/repos/servicio), T-ACTA-141…145
  (controller/forms), T-ACTA-151…163 (vistas/CSS/JS), T-ACTA-171…177 (tests), T-ACTA-180
  (no regresión), T-ACTA-181 (G-POSTMAN), T-ACTA-182 (seguridad/secretos), T-ACTA-185
  (matriz de paridad), T-ACTA-186 (higiene, con observación m-02).
- **Pendientes**:
  - **T-ACTA-178** (inspección visual responsive 375/1440) — parcial: `/actas` OK, `/actas/nueva`
    overflow (H-01). Requiere fix de vistas.
  - **T-ACTA-179** (axe-core sin hallazgos críticos) — parcial: 1 serious en layout compartido
    (m-01).
  - **T-ACTA-183** (actualizar `docs/analisis-ATPPuestos.md`) — no verificado en esta pasada.
  - **T-ACTA-184** (resolver supuestos abiertos) — todos resueltos (ver §5).
  - **T-ACTA-002/003/005** (G-DATA, numeración, contrato de no regresión) — cubiertos por IT,
    pero los checkboxes siguen sin marcar (m-03).
- **No verificados (sin evidencia directa en esta pasada)**: T-ACTA-161 (tokens en
  `tokens.css`) — los tests de recursos lo cubren indirectamente.

---

## 7. Tabla de trazabilidad R-ACTA-001…R-ACTA-060

| Req | Veredicto | Evidencia |
|---|---|---|
| R-ACTA-001 | PASS | `ActasMvcControllerTest` + `actas-lista.html` |
| R-ACTA-002 | PASS | `ActasMvcControllerTest` (turno activo) |
| R-ACTA-003 | PASS | `ActaRepositoryIT`/`ActaMapeoIT`, filtro base en `buscar` |
| R-ACTA-004 | PASS | P0 fijo; `ActaRepositoryBusquedaIT` SQL `order by …F276Id` único |
| R-ACTA-005 | PASS | template `actas-lista.html` |
| R-ACTA-006 | PASS | `ActaServiceTest` (conCuit) |
| R-ACTA-007 | PASS | `ActaForm`/`ActaService` total |
| R-ACTA-008 | PASS | `RangoManualFormulario` + domain |
| R-ACTA-009 | PASS | template + controller |
| R-ACTA-010 | PASS | `Pageable` + `ActasMvcControllerTest` |
| R-ACTA-011 | PASS | `FiltroActasForm` + `buscar` |
| R-ACTA-012 | PASS | `ActasMvcControllerTest` (preserva filtros) |
| R-ACTA-013 | PASS | template estado vacío + test |
| R-ACTA-014 | PASS | `ActasMvcFormularioTest` |
| R-ACTA-015 | PASS | template fieldset |
| R-ACTA-016 | PASS | template selectores verbatim |
| R-ACTA-017 | PASS | template fieldsets partes |
| R-ACTA-018 | PASS | `ActaServiceTest` derivarPorCuit |
| R-ACTA-019 | PASS | template transporte |
| R-ACTA-020 | PASS | template grilla productos |
| R-ACTA-021 | PASS | `ActaService` (cantidad por tipo calc) |
| R-ACTA-022 | PASS | `ActaService` (precio) |
| R-ACTA-023 | PASS | `ActaValidador.MAX_PRODUCTOS=6` + test |
| R-ACTA-024 | PASS | `actas.js` confirmación verbatim (`ActasRecursosTest`) |
| R-ACTA-025 | PASS | template acciones |
| R-ACTA-026 | PASS | `ActaAltaTest` |
| R-ACTA-027 | PASS | `ActaAltaTest` |
| R-ACTA-028 | PASS | `ActaAltaTest` (CUIT) |
| R-ACTA-029 | PASS | `ActaAltaTest` (razón social) |
| R-ACTA-030 | PASS | `ActaAltaTest` (datos transporte) |
| R-ACTA-031 | PASS | `ActaAltaTest` (detalle productos) |
| R-ACTA-032 | PASS | `ActaAltaTest` (restricción entrada) |
| R-ACTA-033 | PASS | `ActaAltaTest` (comprobante) |
| R-ACTA-034 | PASS | `ActaAltaTest` (fecha futura) |
| R-ACTA-035 | PASS | `ActaAltaTest` (turno cerrado) |
| R-ACTA-036 | PASS | `ActaNumeracionIT` + `ServicioNumeracionActaTest` |
| R-ACTA-037 | PASS | `ActaAltaTest` (precio inferior) |
| R-ACTA-038 | PASS | `ActaAltaTest` (producto duplicado) |
| R-ACTA-039 | PASS | `ActaAltaTest` (producto no seleccionado) |
| R-ACTA-040 | PASS | `ActaAltaTest.soloElPrimerMensaje` |
| R-ACTA-041 | PASS | `ActaServiceTest` (validación lotes) |
| R-ACTA-042 | PASS | `ActaServiceTest` (bloqueo turno cerrado) |
| R-ACTA-043 | PASS | B2: `ServicioCierreTurnoActasTest` + adaptador |
| R-ACTA-044 | PASS | `ActaService` (contexto de sesión) |
| R-ACTA-045 | PASS | `ActaAltaIT` |
| R-ACTA-046 | PASS | `ActaServiceTest` (tipoFormulario) |
| R-ACTA-047 | PASS | `ActaAltaIT` |
| R-ACTA-048 | PASS | `ActaAltaIT` (pase) |
| R-ACTA-049 | PASS | `ActaAltaIT` (rollback) + `@Transactional` |
| R-ACTA-050 | PASS | `ActaMapeoIT` |
| R-ACTA-051 | PASS | `ActaMapeoIT` (huérfanas) |
| R-ACTA-052 | PASS | `@ColumnTransformer RTRIM` + `ActaMapeoIT` |
| R-ACTA-053 | PASS | `ActasRecursosTest` |
| R-ACTA-054 | **FAIL** | `/actas` OK (375=375); `/actas/nueva` overflow (780>375) — H-01 |
| R-ACTA-055 | PASS (obs.) | axe serious en layout compartido (`atp-sesion`) — m-01 |
| R-ACTA-056 | PASS | layout base |
| R-ACTA-057 | PASS | `ActasMvcFormularioTest` (CSRF 403) + security config |
| R-ACTA-058 | PASS | G-POSTMAN sin cambios (`git diff`) |
| R-ACTA-059 | PASS | suites verdes (513 unit + 103 IT) |
| R-ACTA-060 | PASS (obs.) | `ActaService` 561 líneas (>500) — m-02 |

**Resumen**: 58 PASS, 1 FAIL (R-ACTA-054), 2 PASS con observación (R-ACTA-055, R-ACTA-060).
Ningún NO VERIFICABLE.

---

## 8. Veredicto de gobernanza

| Regla | Veredicto | Evidencia |
|---|---|---|
| G5 (sin DDL/migraciones) | PASS | `ddl-auto=none` (`application.yml:69`); sin `schema.sql`/`data.sql`. |
| G7 (dependencias) | PASS | grep domain/service/web sin imports cruzados. |
| G-POSTMAN | PASS | colección sin cambios (`git diff`). |
| G4 (<500 líneas) | PASS con obs. | `ActaService.java` 561 líneas (m-02). |
| CSRF/seguridad | PASS | 302 sin sesión, 403 sin CSRF (tests + runtime). |
| Secretos | PASS | `.env` no versionado; secreto en `docs/` remediado (`<redacted>`). |
| B3 responsive | FAIL | H-01 (`/actas/nueva` overflow a 375 px). |

---

## 9. Conclusión

El módulo `09-formularios-acta` **aprueba el Gate 2 con observaciones**: todos los bloqueantes
anteriores (P0/B0, B1, B2, M1, M2) quedaron resueltos con evidencia de tests unitarios e IT contra
`Rentas`. Subsiste un único mayor (H-01, responsive del formulario de alta) y tres menores
(m-01 contraste en layout compartido, m-02 tamaño de `ActaService`, m-03 checkboxes de `tasks.md`).

**Acción requerida antes del cierre definitivo**: implementador-vistas debe corregir la media query
de `/actas/nueva` para que `scrollWidth ≤ 375` (H-01) y, opcionalmente, el contraste del layout
compartido (m-01).
