# Verificación Gate 2 — Módulo 08 Control de Cargas (corrección v2, 2026-11-03)

Verificador: `verificador-qa`. Alcance: corrección de Control de Cargas (`08-control-cargas`).
Ramas/commit base: `feature/control-cargas` (HEAD `94f1432`). Cambios sin commitear (working tree dirty) auditados tal cual.

## 1. Estado de los gates

| Gate | Estado | Evidencia |
|---|---|---|
| Gate 1 — Spec aprobada | PASS (preexistente) | `openspec/features/08-control-cargas/{spec,plan,tasks,spec-anexo}.md` |
| Gate 2 — Unitarios | **PASS** | `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-build.log` — `BUILD SUCCESS`, `Tests run: 400, Failures: 0, Errors: 0, Skipped: 0` |
| Gate 2 — IT contra BD real | **PASS** | `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-it.log` — `BUILD SUCCESS`, `Tests run: 81, Failures: 0, Errors: 0, Skipped: 1` |
| Gate 2 — Paridad de BD (G5) | PASS | `ddl-auto=none` en núcleo main/test y puestos test; sin schema.sql/data.sql/Flyway/Liquibase |
| Gate 2 — G-POSTMAN | PASS | `git diff --stat` sobre la colección = vacío (sin endpoints nuevos) |
| Gate 2 — Gobernanza | PASS (con 1 observación menor) | ver §5 |
| Gate 2 — Tests de vistas T-311..T-317 | **PENDIENTE** | no existen; ver §6 |

### Conteo por módulo (unitarios)
- `atp-nucleo-compartido`: 367 tests, 0 fallos, 0 errores, 0 skipped.
- `atp-puestos-camineros`: 33 tests (CargaServiceTest 9 + CargaMvcControllerTest 19 + ProductoMvcCsrfTest 4 + PuestosApplicationTests 1), 0 fallos.
- **Total: 400 tests.**

### Conteo IT (BD real `devmssqlatp.ecom.com.ar:1433` / `Rentas`)
- `atp-nucleo-compartido`: 77 tests, 0 fallos, 1 skipped.
- `atp-puestos-camineros`: 4 tests (`CargaMarcadoIT`), 0 fallos.
- **Total: 81 tests, 0 fallos.** La BD respondió; no hay bloqueo por red.

## 2. Qué se hizo y con qué evidencia

### Backend (implementador-backend)
- `CargaService.calcularTotalGeneral(DetalleProductoPrimario)` existe (`CargaService.java:47`) y se invoca en `:42` (`d.setTotalGeneral(...)`).
- `DetalleProductoPrimario.totalGeneral` anotado `@Transient` (`DetalleProductoPrimario.java:20-25`) con getter/setter `:213-214`.
- `CargaForm.mostrarModal` (Boolean) con getter/setter (`CargaForm.java:17,37-38`).
- Controller: `/marcar` (`:85`) setea `mostrarModal=true` (`:122`); `/detalle` (`:130`) setea `mostrarModal=true` (`:143`); `/buscar` (`:53`) no setea el flag.

### Vistas (implementador-vistas)
- Modal gated por `cargaForm.mostrarModal` en `control-cargas.html:104`.
- 5 hidden inputs de PK por fila en `control-cargas.html:76-80` (`conCuit`, `dppSucu`, `calPerFisc`, `dppSecu`, `dppNroForm`).
- CUIT y Nro Guía clickeables hacia `/cargas/detalle` (`control-cargas.html:62,68`).
- Tokens `--atp-cargas-*` en `tokens.css:78-82`.
- `control-cargas.css` sin literales hex (grep `#[0-9a-fA-F]{3,6}` → 0).
- `control-cargas.js`: `cerrarModal()` + listener `Escape` (`:9,16-18`), expone `window.cerrarModal` (`:31`). Sin `XMLHttpRequest` ni `eval`; la única coincidencia de `fetch` es en el comentario de cabecera (línea 1), no en código.

### Gobernanza verificada
- `ddl-auto=none`: `application.yml:69` (núcleo main), `application-test.yml:16` (núcleo y puestos).
- Sin `schema.sql`/`data.sql`/Flyway/Liquibase en el árbol de fuentes.
- `.env` no versionado (`git ls-files .env` → vacío).
- Archivos de config tocados < 500 líneas (application.yml 101, application-test.yml 16/16).
- Sin atajo `web → repository`: 0 imports de `repository.` en `web/**` (monolito modular respetado).
- Colección Postman sin cambios en esta corrección (regla G-POSTMAN cumplida: no se agregaron endpoints nuevos).

## 3. Riesgos y bloqueos

- **No hay bloqueo de entorno**: la BD real respondió; IT verde.
- **Tests de vistas pendientes (T-311..T-317)**: no existen en el árbol (ver §6). Riesgo de regresión silenciosa en la vista (modal, 7 columnas, CSS sin hex, PK completa, Total General). Mitigación temporal: verificación por inspección realizada en §2, pero no hay prueba automatizada.
- Observación menor: `tokens.css` contiene literales hex dentro de los tokens `--atp-cargas-*` (esperado, es el archivo de tokens; la regla "sin literales fuera del archivo de tokens" se cumple).
- `control-cargas.js` referencia `fetch` sólo en comentario; aceptable, pero si un linter futuro busca la palabra literal, conviene reformular el comentario.

## 4. Próximo paso recomendado

1. Que el **coordinador** asigne al **implementador-vistas** (o implementador-backend para T-315) la creación de los tests T-311..T-317 pendientes, conforme a `tasks.md` (T-311..T-317), y luego re-ejecutar Gate 2.
2. Lista exacta de tests faltantes (§6).
3. Tras cerrar T-311..T-317 y T-318 (mem_save), re-correr `.\mvnw.cmd -B clean test` y los IT para dejar evidencia final.

## 5. Gobernanza — detalle

Ver §2 (Gobernanza verificada). Todos los checks de §5 de la consigna pasan.

## 6. Tests de vistas T-311..T-317 — PENDIENTE

No existen en el repositorio (búsqueda de `T-31[0-9]` en `src/test` → 0 coincidencias; `CargaMvcControllerTest` no contiene casos para modal/7 columnas/PK completa/Total General/CSS hex). Tampoco hay test unitario de `CargaService.calcularTotalGeneral`.

Tests faltantes (según `tasks.md`):
- **T-311** MockMvc: Buscar NO renderiza el modal (`mostrarModal=false`); `/detalle` y `/marcar` SÍ.
- **T-312** MockMvc: click en "Nro Guia" y en "Cuit" renderizan el modal con los 4 datos.
- **T-313** MockMvc: la grilla renderiza 7 columnas en el orden indicado (R-CC-20).
- **T-314** MockMvc: el form de Marcar incluye los 5 hidden inputs de PK por fila (R-CC-21).
- **T-315** Unitario `CargaService`: Total General = suma de los 6 componentes con `@Transient`.
- **T-316** Test de CSS: `control-cargas.css` no contiene literales hexadecimales (R-CC-24).
- **T-317** MockMvc: el modal incluye control de cierre y desaparece al activarlo (R-CC-19).

No se marcó `tasks.md` (read-only sobre código y spec; el coordinador gestiona el marcado).

## Comandos ejecutados (autoritativos)
- `set JAVA_HOME=C:\Program Files\Java\jdk-17` + `.\mvnw.cmd -B clean test` → log `gate2-build.log`, BUILD SUCCESS, 400 tests.
- `$env:ATP_DB_IT="true"` (vars DB desde `.env`) + `.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"` → log `gate2-it.log`, BUILD SUCCESS, 81 tests, 1 skipped.
- Inspecciones: `Select-String`/`git diff --stat`/`git ls-files` documentadas arriba.
