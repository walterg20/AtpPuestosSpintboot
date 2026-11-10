# Tasks — 12 Flujo de Carga Manual de Turno (Puestos Camineros)

> Checklist verificable del módulo `12-flujo-carga-manual-turno`. Cada tarea es **atómica** y
> trazable a un requisito de `spec.md` (`R-TURNO-*`) o a una sección de `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` + `plan.md` (puerta G-SPEC)** — G2.
> **Ninguna tarea está marcada como completada por el analista**: el cierre y la evidencia son del
> coordinador.
>
> Tareas **A** = verificación previa (KB + BD). **B** = repositorio. **C** = servicio.
> Tareas **D** = web MVC. **E** = vistas. **F** = pruebas. **G** = gobernanza / documentación.
>
> **Leyenda de dependencias**: `→` significa "depende de". `Req:` = requisito de `spec.md`.
> `Plan:` = sección de `plan.md`.

---

## A. Verificación previa

- [x] **T-TURNO-001** Releer por el MCP `genexus18mcp` (alias `atppuestos`, **sólo lectura**) el
  WebPanel `TurnosListadoAbiertos` (Events + WebForm), el WebPanel `PPActaDetalle` (Events,
  foco en `Sub 'Validar'`, `Sub 'BeforeInsert'`, `Sub 'VerificaActa'`,
  `Sub 'ObtenerTipoActaManual'` y `Event ctlF276Id.IsValid`), el Procedure `PPrim_TurnoEstado`
  (Rules + Source) y la DataProvider `DPDolphinMenuActas` (Source); confirmar los 2 eventos
  verbatim de `TurnosListadoAbiertos`, el `ControlWhere`/`ControlOrder` de `GridTurnos`, las
  diferencias del modo manual (`Tipo = 'M'`) y el enrutamiento del menú.
  *Sin dependencias.* · **Criterio de aceptación**: el volcado coincide carácter a carácter con
  `spec.md` §3.1 y §3.2; cualquier diferencia se corrige en `spec.md` **antes** de T-TURNO-041.
  **Req**: R-TURNO-001…R-TURNO-016. **Plan**: §3, §4.

- [x] **T-TURNO-002** **G-DATA — puerta de T-Bxx.** Con `INFORMATION_SCHEMA.COLUMNS` +
  `KEY_COLUMN_USAGE` de `Rentas` (**sólo `SELECT`**), confirmar el esquema físico de `PPRIM_TU`
  (12 columnas) y de `FORMSUCU` (8 columnas), incluida la **ausencia** de `ForRecFin` físico y
  que `ForRecFin = ForRecIni + ForRecCant - 1` es fórmula (verificar contra la transacción
  `FormSucursal` en la KB).
  *Sin dependencias.* · **Criterio de aceptación**: el resultado coincide con `spec.md` §3.6 y
  `plan.md` §2; si algún nombre o tipo difiere, se actualizan `spec.md` y `plan.md` **antes** de
  T-TURNO-041. **Req**: R-TURNO-004, R-TURNO-010, R-TURNO-011, R-TURNO-016.

- [x] **T-TURNO-003** Inventariar el entorno de destino: presencia de `templates/puestos/acta-formulario.html`,
  `ActaService`, `ActaForm`, `RangoManualFormulario`, `TurnoRepositorio`, `Turno` y la seguridad de
  `03-login` (CSRF, sesión). Confirmar que `09-formularios-acta` está implementado (o, si no, que
  los archivos de `09` existen como base).
  *Sin dependencias.* · **Criterio de aceptación**: el inventario queda registrado en `plan.md` §1
  y las tareas dependientes quedan marcadas con su dependencia.

- [x] **T-TURNO-004** Confirmar el **contrato de no regresión** con `09-formularios-acta`:
  `Acta`, `ActaProducto`, `ActaPase`, `ContadorFormulario`, `RangoManualFormulario` y
  `Formulario2276`/`Formulario2276Repositorio` **no** se modifican; sólo se extienden
  `ActaService`, `ActasController`, `ActaForm` y `TurnoRepositorio`.
  *→ T-TURNO-003* · **Criterio de aceptación**: queda escrito en `plan.md` §1 y el diff de
  implementación no altera las entidades de `09` ni del núcleo. **Req**: R-TURNO-007.

---

## B. Repositorio

- [x] **T-TURNO-011** Extender `nucleo.repository.TurnoRepositorio` con el método
  `listarAbiertosPorSucursal(Integer sucNroId, String sucTipo, Pageable pageable)` (consulta JPQL
  con `turEstado = 'A'`, ordenado por `SucNroId, SucTipo, TurNroId`), equivalente al
  `ControlWhere` de `TurnosListadoAbiertos`.
  *→ T-TURNO-002* · **Criterio de aceptación**: la consulta se genera correctamente y el `WHERE`
  base reproduce `TurEstado = 'A' AND SucNroId = ? AND SucTipo = ?`. **Req**: R-TURNO-002,
  R-TURNO-005. **Plan**: §3.4, D-TURNO-07.

- [x] **T-TURNO-012** Verificar (o agregar) en `puestos.repository.RangoManualFormularioRepositorio`
  el método que devuelve los rangos manuales por sucursal y tipo (`findBySucursalYFormulario` ya
  existe en `09`), y un helper `validarNumeroManual(sucNroId, sucTipo, f276Id, clasif)` que
  implemente `ForRecIni <= f276Id <= ForRecIni + ForRecCant - 1` con `ForRecTipo` acorde.
  *→ T-TURNO-002* · **Criterio de aceptación**: casos límite (`ForRecIni` y `ForRecIni + ForRecCant - 1`
  dentro; `ForRecIni - 1` y `ForRecIni + ForRecCant` fuera) verificados en test unitario.
  **Req**: R-TURNO-010. **Plan**: §3.3, D-TURNO-03.

---

## C. Servicio

- [x] **T-TURNO-021** Implementar en `ActaService` el método `esTurnoAbierto(sucNroId, sucTipo,
  turNroId)` que replica `PPrim_TurnoEstado` y devuelve `true` sólo si `TurEstado = 'A'`.
  *→ T-TURNO-011* · **Criterio de aceptación**: test unitario con turno `'A'` (true) y `'C'`
  (false); test con turno inexistente (false). **Req**: R-TURNO-004, R-TURNO-016.
  **Plan**: §3.3.

- [x] **T-TURNO-022** Implementar en `ActaService` los métodos `rangosManuales(...)`,
  `validarNumeroManual(...)` y `obtenerTipoActaManual(...)` (rango inclusivo; `ACTI`→`E`,
  `ACTS`→`S`), reproduciendo `VerificaActa` y `ObtenerTipoActaManual`.
  *→ T-TURNO-012* · **Criterio de aceptación**: tests parametrizados con rangos `ACTI` y `ACTS`
  que verifican la derivación y la validación (dentro/fuera de rango). **Req**: R-TURNO-010,
  R-TURNO-011. **Plan**: §3.3, D-TURNO-03.

- [x] **T-TURNO-023** Extender `ActaService.guardar(...)` para aceptar el flag `manual` (boolean):
  si `manual = true` ⇒ NO llamar a `ServicioNumeracionActa`, usar `ActaAlta.f276Id` como
  `F276Id`, tomar `TurNroId` de `ActaAlta.turNroId`, validar el número manual (R-TURNO-009,
  R-TURNO-010) y derivar `F276Clasif` (R-TURNO-011); si `manual = false` ⇒ comportamiento
  idéntico a `09` (R-TURNO-014).
  *→ T-TURNO-021, T-TURNO-022* · **Criterio de aceptación**: IT que crea un acta en modo manual y
  verifica `F276Id` ingresado, `TurNroId` del turno seleccionado y que `GES_SUCN.SucUltNum` no
  cambió; IT que crea en modo normal y verifica numeración automática. **Req**: R-TURNO-012,
  R-TURNO-013, R-TURNO-014. **Plan**: §3.3, D-TURNO-04, D-TURNO-05.

- [x] **T-TURNO-024** Extender `ActaService.prepararFormulario(...)` (o agregar variante) para
  soportar el modo manual: no validar lotes/turno de la misma forma si aplica; armar el `ActaForm`
  con `manual=true` y `turNroId` del parámetro.
  *→ T-TURNO-023* · **Criterio de aceptación**: `@WebMvcTest` con `manual=true` que verifica que el
  view model lleva `manual=true` y `turNroId`. **Req**: R-TURNO-003, R-TURNO-008.

---

## D. Web MVC

- [x] **T-TURNO-031** Crear (o extender) `CargaManualController` con `GET /actas/carga-manual`:
  puebla `contexto` (vía `GestorContextoSesion`), `programas` y `rutaActual` (vía
  `ProveedorNavegacion`); delega en `TurnoRepositorio.listarAbiertosPorSucursal(...)` y vuelca
  `turnos` al `Model`; devuelve la vista `puestos/turnos-abiertos`.
  *→ T-TURNO-011, T-TURNO-003* · **Criterio de aceptación**: `@WebMvcTest` comprueba
  `GET /actas/carga-manual` → 200, vista `puestos/turnos-abiertos`, atributos `turnos`/`contexto`;
  el controller **no** importa repositorios de `F2276`. **Req**: R-TURNO-001, R-TURNO-002,
  R-TURNO-005. **Plan**: §3.1, D-TURNO-01.

- [x] **T-TURNO-032** Extender `ActasController.GET /actas/nueva` para aceptar los parámetros
  opcionales `turNroId` (Long) y `manual` (boolean): si `manual=true`, usar `turNroId` del
  parámetro; si no, usar el del contexto (compatibilidad con `09`). Delegar en
  `ActaService.prepararFormulario(...)` y volcar `manual` y `turNroId` al `Model`.
  *→ T-TURNO-024* · **Criterio de aceptación**: `@WebMvcTest` con y sin parámetros; en el caso
  manual verifica que `turNroId` se propaga. **Req**: R-TURNO-003, R-TURNO-008.

- [x] **T-TURNO-033** Extender `ActasController.POST /actas/guardar` para aceptar los campos ocultos
  `manual` y `turNroId` de `ActaForm`: pasarlos a `ActaService.guardar(..., manual)` y, en modo
  manual, validar el número (R-TURNO-009, R-TURNO-010) antes de persistir.
  *→ T-TURNO-032* · **Criterio de aceptación**: `@WebMvcTest` con los 3 caminos (binding inválido,
  error de negocio, éxito) en modo manual y en modo normal. **Req**: R-TURNO-012, R-TURNO-013,
  R-TURNO-014.

- [x] **T-TURNO-034** Extender el form object `ActaForm` (de `09`) con los campos `manual`
  (boolean) y `turNroId` (Integer), y asegurar que `f276Id` se bindee en modo manual.
  *Sin dependencias.* · **Criterio de aceptación**: los campos compilan y `th:field` puede
  enlazarlos. **Req**: R-TURNO-008. **Plan**: §3.2.

- [x] **T-TURNO-035** Verificar que el controller **no** accede a repositorios ni a la BD
  directamente (regla `web → service → repository → domain`).
  *→ T-TURNO-031, T-TURNO-032, T-TURNO-033* · **Criterio de aceptación**: `grep` del paquete
  `puestos.web.mvc` sin imports de `puestos.repository` ni de `nucleo.repository` (salvo los ya
  existentes de `09`). **Req**: R-TURNO-001. **Plan**: §1.1.

---

## E. Vistas

- [x] **T-TURNO-041** Crear `templates/puestos/turnos-abiertos.html` que **extienda** `layout/base`
  e implemente: título `Seleccione un Turno`, grilla con las columnas `Turno`, fecha de apertura y
  `Seleccionar`, filas con `th:each`, estado vacío y (si aplica) paginación.
  *→ T-TURNO-031* · **Criterio de aceptación**: test de vista que verifica el título, los
  encabezados, el enlace `Seleccionar` con `href` a `/actas/nueva?turNroId=...&manual=true` y el
  estado vacío. **Req**: R-TURNO-002, R-TURNO-005, R-TURNO-006, R-TURNO-015.
  **Plan**: §4.2, SA-TURNO-02, SA-TURNO-03.

- [x] **T-TURNO-042** Ajustar `templates/puestos/acta-formulario.html` para renderizar, sólo si
  `manual = true`, el campo `Nro. Acta` (`F276Id`) editable con `label` y `th:errors`, y agregar
  los campos ocultos `manual` y `turNroId`.
  *→ T-TURNO-032* · **Criterio de aceptación**: test de vista con `manual=true` que verifica la
  presencia del control `F276Id` editable y los campos ocultos; test con `manual=false` que
  verifica su ausencia. **Req**: R-TURNO-008. **Plan**: §4.3, SA-TURNO-04.

- [x] **T-TURNO-043** Ajustar el menú (DPDolphinMenuActas equivalente en Thymeleaf) para que el
  ítem "Carga Manual" apunte a `/actas/carga-manual`.
  *→ T-TURNO-031* · **Criterio de aceptación**: test de vista del menú que verifica el `href` del
  ítem "Carga Manual". **Req**: R-TURNO-001.

- [x] **T-TURNO-044** Ajustar `static/js/actas.js` (si aplica) para mostrar/ocultar el campo
  `F276Id` según el modo; sin `fetch`/`XMLHttpRequest`/`eval`.
  *→ T-TURNO-042* · **Criterio de aceptación**: `grep` sin `fetch`/`XMLHttpRequest`/`eval`.
  **Req**: R-TURNO-008. **Plan**: G6.

---

## F. Pruebas

- [x] **T-TURNO-051** Suite `@WebMvcTest` de `CargaManualController` (`TurnosAbiertosMvcTest`):
  render de `/actas/carga-manual` (con turnos y vacío), verificación de los encabezados, del
  enlace `Seleccionar` y del estado vacío.
  *→ T-TURNO-031, T-TURNO-041* · **Criterio de aceptación**: `./mvnw clean test` en verde.
  **Req**: R-TURNO-002, R-TURNO-005, R-TURNO-006, R-TURNO-015.

- [x] **T-TURNO-052** Suite `@WebMvcTest` del flujo manual en `ActasController`:
  `GET /actas/nueva?manual=true&turNroId=42` (render con `F276Id` editable) y
  `POST /actas/guardar` en modo manual (binding inválido, número inválido `0`, número fuera de
  rango, éxito con flash).
  *→ T-TURNO-032, T-TURNO-033, T-TURNO-042* · **Criterio de aceptación**: suites en verde.
  **Req**: R-TURNO-008, R-TURNO-009, R-TURNO-010, R-TURNO-012.

- [x] **T-TURNO-053** Suite unitaria de `ActaService` (extensiones): `esTurnoAbierto` (2 casos),
  `validarNumeroManual` (dentro/fuera de rango, límites), `obtenerTipoActaManual` (`ACTI`/`ACTS`),
  y `guardar(..., manual=true)` (no incrementa `GES_SUCN`, persiste `F276Id` y `TurNroId`
  seleccionado).
  *→ T-TURNO-021, T-TURNO-022, T-TURNO-023* · **Criterio de aceptación**: suites en verde y los
  mensajes comparados carácter a carácter contra las constantes de `MensajesActa`.
  **Req**: R-TURNO-004, R-TURNO-009, R-TURNO-010, R-TURNO-011, R-TURNO-013, R-TURNO-016.

- [x] **T-TURNO-054** IT contra `Rentas` (`ATP_DB_IT=true`, con rollback) `TurnosAbiertosIT`: el
  listado paginado devuelve **exactamente** los turnos con `TurEstado = 'A'` de la sucursal y tipo
  del contexto, respetando el orden.
  *→ T-TURNO-011* · **Criterio de aceptación**: `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` en verde y
  la BD sin cambios. **Req**: R-TURNO-002, R-TURNO-005.

- [x] **T-TURNO-055** IT contra `Rentas` `ActaManualIT`: alta completa de un acta en modo manual
  con 2 productos; verificación de la cabecera (`F276Id` ingresado, `TurNroId` del turno
  seleccionado, `F276GenWeb='A'`), del detalle y del pase; verificación de que `GES_SUCN.SucUltNum`
  **no** cambió; y verificación de que un error intermedio revierte toda la operación.
  *→ T-TURNO-023* · **Criterio de aceptación**: conteos iguales antes y después del test fallido;
  la BD queda sin cambios. **Req**: R-TURNO-012, R-TURNO-013, R-TURNO-014.

- [x] **T-TURNO-056** **No regresión**: `./mvnw clean test` deja en verde las suites de
  `01-nucleo-datos`, `02-abm-productos`, `03-login`, `06-turnos`, `08-control-cargas` y
  `09-formularios-acta`; y el diff **no** toca las entidades de `09` ni del núcleo (salvo la
  extensión de `TurnoRepositorio`).
  *→ T-TURNO-051…T-TURNO-055* · **Criterio de aceptación**: build en verde y `git diff` sin cambios
  indebidos. **Req**: R-TURNO-007.

---

## G. Gobernanza, documentación y cierre

- [x] **T-TURNO-060** **G-POSTMAN**: al no agregarse endpoints REST nuevos, confirmar que
  `docs/postman/ATPPuestos-API.postman_collection.json` **no** requiere cambios y dejar constancia en
  `docs/postman/README.md`.
  *→ T-TURNO-031* · **Criterio de aceptación**: la colección queda sin diff y el `README` registra
  la decisión. **Req**: R-TURNO-001 (y D-TURNO-06).

- [x] **T-TURNO-061** **G-SEC**: escaneo de secretos sobre el diff; CSRF activo en el `POST` de
  alta manual; sin scripts o estilos inline con lógica.
  *→ T-TURNO-033, T-TURNO-044* · **Criterio de aceptación**: escaneo sin hallazgos.

- [x] **T-TURNO-062** **G-SEC/G-DATA**: verificar que no se introdujo DDL, migraciones ni cambios
  en `spring.jpa.hibernate.ddl-auto` (debe seguir en `none`).
  *→ T-TURNO-011, T-TURNO-012* · **Criterio de aceptación**: `git diff` sin `CREATE TABLE`/`ALTER TABLE`.

- [x] **T-TURNO-063** Actualizar `docs/analisis-ATPPuestos.md` con el resultado del G-DATA
  re-ejecutado (`PPRIM_TU` = 12 columnas, `FORMSUCU` = 8 columnas sin `ForRecFin` físico,
  `ForRecFin` fórmula) y registrar el flujo verificado (`DPDolphinMenuActas` →
  `TurnosListadoAbiertos` → `PPActaDetalle(TurNroId,'M')`).
  *→ T-TURNO-002* · **Criterio de aceptación**: el documento refleja la evidencia de
  `INFORMATION_SCHEMA` y de la KB.

- [x] **T-TURNO-064** Revisar y resolver los supuestos abiertos SA-TURNO-01…SA-TURNO-05 con los
  responsables; actualizar `spec.md` §6 y `plan.md` §7 con la resolución (o la decisión de
  postergar con motivo y fecha).
  *→ T-TURNO-001…T-TURNO-055* · **Criterio de aceptación**: cada supuesto tiene una resolución
  escrita o una decisión de postergar con motivo y fecha.

- [x] **T-TURNO-065** Cerrar la matriz de paridad de `spec.md` §5 y `plan.md` §5 con la evidencia
  de cada prueba, y guardar el resumen en Engram (`mem_save`, `topic_key: spec/12-flujo-carga-manual-turno`)
  con los objetos inventariados, las decisiones `D-TURNO-*` y los supuestos abiertos.
  *→ T-TURNO-060…T-TURNO-064* · **Criterio de aceptación**: cada fila de la matriz referencia su
  prueba y su resultado; observación guardada en Engram.

- [x] **T-TURNO-066** Verificar que todos los archivos tocados quedan por debajo de **500 líneas**
  (G4) y que no se versionaron credenciales ni cadenas de conexión (G3).
  *→ T-TURNO-056* · **Criterio de aceptación**: conteo de líneas y escaneo de secretos sin
  hallazgos.

---

## Orden de ejecución y paralelismo

```text
T-TURNO-001, T-TURNO-002, T-TURNO-003   (paralelos; T-TURNO-002 bloquea T-TURNO-011/012)
        │  T-TURNO-004 (verificación de no regresión)
        ▼
T-TURNO-011 ──┬── T-TURNO-012
        ▼
T-TURNO-021 ──┬── T-TURNO-022 ──┬── T-TURNO-023 ── T-TURNO-024
        ▼
T-TURNO-031 ──┬── T-TURNO-032 ── T-TURNO-033
              └── T-TURNO-034
              └── T-TURNO-035
        ▼
T-TURNO-041 ── T-TURNO-042 ── T-TURNO-043 ── T-TURNO-044
        ▼
T-TURNO-051…T-TURNO-055  →  T-TURNO-056  →  T-TURNO-060…T-TURNO-066
```

### Qué puede hacer `implementador-backend` y `implementador-vistas` en paralelo

| Implementador | Escribe **sólo** en | Puede empezar cuando |
|---|---|---|
| `implementador-backend` | `puestos/service/ActaService.java`, `puestos/web/mvc/ActasController.java`, `puestos/web/mvc/CargaManualController.java`, `puestos/web/mvc/form/ActaForm.java`, `nucleo/repository/TurnoRepositorio.java` | T-TURNO-002 cerrada (T-TURNO-011…T-TURNO-035) |
| `implementador-vistas` | `templates/puestos/turnos-abiertos.html`, `templates/puestos/acta-formulario.html` (ajuste), menú, `static/js/actas.js` | T-TURNO-032 cerrada y el contrato de vista definido (T-TURNO-041…T-TURNO-044) |

### Reglas de coordinación

| Regla | Detalle |
|---|---|
| 1 | Ninguna tarea de código arranca antes de la **aprobación de `spec.md` + `plan.md`** (puerta G-SPEC). |
| 2 | **T-TURNO-002 es una puerta**: sin el esquema físico confirmado, T-TURNO-011/012 quedan bloqueadas. |
| 3 | La lógica de negocio va **siempre** en `service/**`; el controller MVC no implementa reglas. |
| 4 | Los **mensajes verbatim** no se tocan, traducen ni capitalizan. |
| 5 | La KB es de **solo lectura**: T-TURNO-001/002 son de lectura; nunca compilar, guardar, regenerar ni borrar objetos. |
| 6 | `tasks.md` se actualiza (con evidencia) **por el coordinador**, no por los implementadores. |
| 7 | **Sin DDL**: T-TURNO-062 verifica que no hay cambios en `spring.jpa.hibernate.ddl-auto`. |
| 8 | **No regresión**: T-TURNO-056 verifica que las suites de `09-formularios-acta` siguen en verde. |