# Tasks — 09 "Carga Manual de Actas" (Formulario 2276 · alta)

> Cada tarea tiene id `T-ACTA-NNN`, dependencias y criterio de "hecho" observable.
> Las tareas marcadas `[x]` ya están implementadas y se cierran con la evidencia citada
> (build `./mvnw clean test` → 215/0/0, rama `feature/design`, 2026-11-11).
> Las tareas `[ ]` son las que quedan por hacer (ninguna bloqueante; sólo aprobación G-SPEC).

## Tareas ya implementadas (evidencia: verificación final §1-§5)

- [x] **T-ACTA-001** — Crear plantilla `templates/puestos/carga-manual-actas.html` con los 6 fieldsets (Datos del Acta, Vendedor, Destinatario, Transportista, Datos del Transporte, Productos).
  - Deps: ninguna.
  - Hecho cuando: `GET /actas/nueva` → 200 + vista `puestos/carga-manual-actas` + 6 fieldsets.
  - Evidencia: `ActasMvcFormularioTest.getNuevaRenderizaLosFieldsets` (PASS).

- [x] **T-ACTA-002** — Extender `layout/base-stitch` sin duplicar cabecera ni pie.
  - Deps: T-ACTA-001.
  - Hecho cuando: 1 `<header>` y 1 `<footer>` en el HTML renderizado.
  - Evidencia: `ActasMvcFormularioTest.getNuevaNoDuplicaCabeceraNiPie` (PASS).

- [x] **T-ACTA-003** — Campo "Nro. Acta" (`f276Id`) sólo en modo manual (V-11).
  - Deps: T-ACTA-001.
  - Hecho cuando: modo turno no contiene `Nro. Acta` ni `name="f276Id"`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaNoRenderizaElNumeroDeActa` (PASS).

- [x] **T-ACTA-004** — Fecha del Acta en sólo lectura, precargada por el servidor (V-10).
  - Deps: T-ACTA-001.
  - Hecho cuando: `id="f276FchMov" type="date" readonly` con `value="2020-01-15"`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaRenderizaLaFechaEnSoloLectura` (PASS).

- [x] **T-ACTA-005** — Tríptico de actores (15 controles) con labels asociados.
  - Deps: T-ACTA-001.
  - Hecho cuando: los 15 ids presentes con `for="..."`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaRenderizaLosControlesDeLasPartes` (PASS).

- [x] **T-ACTA-006** — Localidad y Provincia en sólo lectura; popup deshabilitado si derivado (V-12).
  - Deps: T-ACTA-005.
  - Hecho cuando: `readonly` en `f276VenLoc`, `f276VenProv`, `f276DestLoc`, `f276TranProv`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaLocalidadYProvinciaEnSoloLectura` (PASS).

- [x] **T-ACTA-007** — Grilla de productos con botón "+" y fragmento `acta-productos` (R-ACTA-020).
  - Deps: T-ACTA-001.
  - Hecho cuando: `id="acta-agregar-producto"`, `id="acta-productos"`, columnas Código/Descripción/Tn.
  - Evidencia: `ActasMvcFormularioTest.getNuevaRenderizaLaGrillaDeProductos` (PASS).

- [x] **T-ACTA-008** — Errores por campo con `th:errorclass` y `th:errors` con `role="alert"` (V-9).
  - Deps: T-ACTA-001.
  - Hecho cuando: cada control tiene su párrafo de error condicional.
  - Evidencia: inspección del HTML + tests de binding.

- [x] **T-ACTA-009** — Bloqueo por lote/turno con motivo (V-17).
  - Deps: T-ACTA-001.
  - Hecho cuando: con bloqueo no se renderizan `f276ActaTi`, `f276CuitVe`, `acta-agregar-producto`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaBloqueadaNoRenderizaElFormulario` (PASS).

- [x] **T-ACTA-010** — Barra de acciones con Confirmar/Cancelar y confirmación verbatim.
  - Deps: T-ACTA-001.
  - Hecho cuando: contiene `¿Confirma que desea enviar el Formulario de Acta?`, `Confirmar`, `Cancelar`, `href="/actas"`.
  - Evidencia: `ActasMvcFormularioTest.getNuevaMuestraLaConfirmacionYCancelar` (PASS).

- [x] **T-ACTA-011** — Controller `ActasController` con rutas `/actas`, `/actas/nueva`, `/actas/guardar`.
  - Deps: ninguna.
  - Hecho cuando: `@RequestMapping("/actas")` + métodos GET/POST.
  - Evidencia: `ActasMvcFormularioTest` (GET/POST) + `ActasMvcControllerTest` (PASS).

- [x] **T-ACTA-012** — `ActaForm` con binding y traducción a `ActaAlta` (`aAlta()`).
  - Deps: T-ACTA-011.
  - Hecho cuando: round-trip binding → `ActaAlta`.
  - Evidencia: `ActasMvcFormularioTest.postGuardarReRenderizaLaGrillaDeProductos` (PASS).

- [x] **T-ACTA-013** — Validación Bean Validation + servicio (primer mensaje, R-ACTA-040).
  - Deps: T-ACTA-012.
  - Hecho cuando: POST con error de negocio re-renderiza con banner verbatim.
  - Evidencia: `ActasMvcFormularioTest.postGuardarConErrorDeNegocio` (PASS).

- [x] **T-ACTA-014** — CSRF obligatorio en POST.
  - Deps: T-ACTA-011.
  - Hecho cuando: POST sin token → 403.
  - Evidencia: `ActasMvcFormularioTest.postGuardarSinCsrf` (PASS).

- [x] **T-ACTA-015** — Turno/sucursal tomados del contexto, no del formulario.
  - Deps: T-ACTA-011.
  - Hecho cuando: `turNroId=9999` en el form no cambia el turno del contexto (7).
  - Evidencia: `ActasMvcFormularioTest.postGuardarUsaElContextoDeSesion` (PASS).

- [x] **T-ACTA-016** — Alta exitosa → redirect `/actas` con flash verbatim.
  - Deps: T-ACTA-011.
  - Hecho cuando: 302 + `flash mensajeExito = "El acta se ha creado correctamente"`.
  - Evidencia: `ActasMvcFormularioTest.postGuardarExitoso` (PASS).

- [x] **T-ACTA-017** — Hoja de estilos `static/css/actas-forms.css` con sólo tokens `--stitch-*`.
  - Deps: ninguna.
  - Hecho cuando: 0 matches de `#[0-9a-fA-F]{3,8}` y `rgba?\s*\(`.
  - Evidencia: verificación §3 (PASS).

- [x] **T-ACTA-018** — Script `static/js/actas-forms.js` vanilla (sin import/fetch/CDN).
  - Deps: ninguna.
  - Hecho cuando: 0 matches de `import`/`require(`/`fetch(` / `https?://`.
  - Evidencia: verificación §4 (PASS).

- [x] **T-ACTA-019** — Alta/baja de filas en la grilla (JS) con renumeración.
  - Deps: T-ACTA-018.
  - Hecho cuando: `agregarFila`/`quitarFila`/`renumerar` actualizan `name`/`id`/`for`.
  - Evidencia: revisión del JS + binding indexado en test.

- [x] **T-ACTA-020** — Sincronización por producto (descripción + cantidad forzada a 1 si TipCal=3).
  - Deps: T-ACTA-019.
  - Hecho cuando: `sincronizarFila` completa descripción y fuerza cantidad.
  - Evidencia: revisión del JS (R-ACTA-021).

- [x] **T-ACTA-021** — Cálculo de totales en el cliente (`#acta-total-toneladas`).
  - Deps: T-ACTA-019.
  - Hecho cuando: `updateTotals` suma cantidades y muestra con 4 decimales.
  - Evidencia: revisión del JS.

- [x] **T-ACTA-022** — Búsqueda de CUIT placeholder (`window.alert`).
  - Deps: T-ACTA-018.
  - Hecho cuando: click en `[data-cuit-search]` muestra alert.
  - Evidencia: revisión del JS (D-ACTA-05).

- [x] **T-ACTA-023** — Confirmación antes de enviar (`window.confirm`).
  - Deps: T-ACTA-018.
  - Hecho cuando: submit interceptado con `MENSAJE_CONFIRMACION`.
  - Evidencia: revisión del JS + `ActasMvcFormularioTest` (PASS).

- [x] **T-ACTA-024** — Impresión (`window.print()`).
  - Deps: T-ACTA-018.
  - Hecho cuando: click en `#actas-imprimir` invoca `window.print()`.
  - Evidencia: revisión del JS.

- [x] **T-ACTA-025** — `ddl-auto=none` mantenido; sin DDL ni migraciones.
  - Deps: ninguna.
  - Hecho cuando: `application.yml` y `application-test.yml` con `ddl-auto=none`; sin `.sql` en el diff.
  - Evidencia: verificación §5 (PASS).

- [x] **T-ACTA-026** — Sin cambios en entidades JPA (`Acta`, `ActaProducto`) ni repositorios.
  - Deps: ninguna.
  - Hecho cuando: `git diff` sin cambios en `domain/` ni `repository/`.
  - Evidencia: verificación §5 (PASS).

- [x] **T-ACTA-027** — Sin cambios en `stitch-tokens.css`, layout base ni `actas.css`.
  - Deps: ninguna.
  - Hecho cuando: `git diff` sin cambios en archivos globales.
  - Evidencia: verificación §5 (PASS).

- [x] **T-ACTA-028** — Suite completa en verde (215/0/0) sin regresión.
  - Deps: T-ACTA-001…T-ACTA-027.
  - Hecho cuando: `./mvnw clean test` → BUILD SUCCESS, 215 tests, 0 failures, 0 errors, 0 skipped.
  - Evidencia: verificación §1 (PASS).

## Tareas pendientes (coordinador)

- [ ] **T-ACTA-029** — Aprobar esta spec en la puerta G-SPEC (cierre del hallazgo H-1).
  - Deps: T-ACTA-028.
  - Hecho cuando: estado de la spec pasa a `APROBADO` y el verificador re-ejecuta G-SPEC → PASS.
  - Evidencia: spec.md con estado `APROBADO` + re-verificación.

- [ ] **T-ACTA-030** — (Opcional) Implementar búsqueda de CUIT con endpoint propio.
  - Deps: T-ACTA-022.
  - Hecho cuando: el botón de búsqueda consulta al servidor y completa los campos del actor.
  - Evidencia: nuevo endpoint + test.
  - Nota: fuera de alcance actual (SA-ACTA-01); se registra como backlog.

## Resumen de cobertura

| Grupo | Tareas | Estado |
|---|---|---|
| Vista (HTML/CSS) | T-ACTA-001…T-ACTA-010, T-ACTA-017 | ✅ 11/11 |
| Backend MVC (controller/form) | T-ACTA-011…T-ACTA-016 | ✅ 6/6 |
| JS puntual | T-ACTA-018…T-ACTA-024 | ✅ 7/7 |
| Paridad BD / gobernanza | T-ACTA-025…T-ACTA-028 | ✅ 4/4 |
| Aprobación / backlog | T-ACTA-029…T-ACTA-030 | ⏳ 0/2 |