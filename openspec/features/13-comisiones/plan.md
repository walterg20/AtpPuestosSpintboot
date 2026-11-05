# Plan — Módulo de Comisiones `13-comisiones`

## 1. Fases

### Fase A — Dominio y persistencia (reutilización del núcleo)
- Las entidades `Comision` (`@Table(name="COMISION")`) y `ComisionIntegrante` (`@Table(name="COMISIO1")`)
  ya existen en `atp-nucleo-compartido` (PK compuestas). **No recrear.**
- `ComisionRepository` (núcleo) ya expone `findComisionConResponsable(pueCod, cpcId)` (JOIN a
  `PERSONAS`) y `ComisionIntegranteRepository` (JpaRepository). **Reutilizar.**
- **SA-COM-01 RESUELTO (MAPEAR):** agregar los 4 campos huérfanos a `Comision` (sólo lectura,
  sin setters públicos): `cpcFeLog`, `cpcUsuLog`, `cpcSnNum`, `cpcFePag`.
- **SA-COM-03 RESUELTO (NO crear entity):** resolver `PueDesc` por consulta nativa en
  `ComisionRepository` (método `findPueDescByPueCod`), no crear entity `Puesto`.

### Fase B — Servicios de negocio (en `atp-puestos-camineros`)
- `ServicioComisiones` (nuevo, en `atp-puestos-camineros`):
  - `listar(pueCod, personaId, pageable)` → `Page<ComisionFilaView>` con filtro legacy
    (D-COM-001), orden `CPCId desc`, paginación.
  - `obtenerDetalle(pueCod, cpcId)` → `ComisionDetalleView` (o `null` si no existe).
  - `listarIntegrantes(pueCod, cpcId)` → `List<IntegranteView>` ordenado por `PerNomyApe` trim.
- `DecodificadorEstadoComision` (o método estático) centraliza la regla R-COM-006/D-COM-003.
- Mensajes verbatim centralizados en una clase `MensajesComision` (sólo el de "No se encontraron
  registros" del detalle; el listado no tiene mensajes de escritura).

### Fase C — Presentación (Thymeleaf, en `atp-puestos-camineros`)
- `templates/puestos/comisiones.html` (listado, siguiendo el diseño Figma frame "Comision").
- `templates/puestos/comision-detalle.html` (detalle con tabs General/Integrantes).
- `ComisionMvcController`: `GET /comisiones`, `GET /comisiones/{pueCod}/{cpcId}`.
- Layout base (`layout/base` del núcleo) reutilizado.

### Fase D — Integración con el menú
- **Se modifica** `ServicioAutorizacion`: se agrega la constante `RUTA_COMISIONES = "/comisiones"` y se
  setea el link en los ítems `Comisiones` de `MENU_PUE` y `MENU_COM` (hallazgo H-1: el link era `null`).
- Se actualiza `AuthMvcHomeTest` para reflejar el nuevo link.

### Fase E — Verificación
- Tests unitarios de servicio (filtro, orden, decodificación, JOIN responsable/integrantes).
- Tests MVC (render listado, render detalle, tabs, estado vacío, error de carga, paginación).
- Tests de integración contra `Rentas` (`*IT`, gated con `ATP_DB_IT=true`).
- QA: paridad de BD, tokens, accesibilidad, G-POSTMAN (no aplica).

## 2. Dependencias

- `03-login` (sesión y `ContextoSesion`: `sucursal()`, `personaId()`).
- `05-ui-moderno` (layout base y menú).
- `01-nucleo-datos` (entidades/repos de `Comision`, `ComisionIntegrante`, `Persona`).
- Regla de paridad: `ddl-auto=none`, sin DDL, sin migraciones.

## 3. Tabla de paridad legacy → JPA

### 3.1 `COMISION` (entity `Comision`, ya existente en núcleo)

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Notas |
|---|---|---|---|---|---|
| `id.pueCod` | `PueCod` | `Integer` | NO | — | PK 1, `smallint` |
| `id.cpcId` | `CPCId` | `Integer` | NO | — | PK 2, `int` |
| `cpcFchDes` | `CPCFchDes` | `LocalDateTime` | SÍ | — | `datetime` (trim no aplica) |
| `cpcFchHas` | `CPCFchHas` | `LocalDateTime` | SÍ | — | `datetime` |
| `cpcCierre` | `CPCCierre` | `String` | SÍ | 1 | `char(1)` → `@ColumnTransformer read = RTRIM(...)` |
| `cpcResId` | `CPCResId` | `Integer` | SÍ | — | `int` (0/null si no hay responsable) |
| `cpcHorDes` | `CPCHorDes` | `Integer` | SÍ | — | `int` (`HHMM`) |
| `cpcHorHas` | `CPCHorHas` | `Integer` | SÍ | — | `int` (`HHMM`) |
| `cpcEstado` | `CPCEstado` | `String` | SÍ | 1 | `char(1)` → trim |
| `cpcFchCie` | `CPCFchCie` | `LocalDateTime` | SÍ | — | `datetime` |
| `cpcFeLog` | `CPCFeLog` | `LocalDateTime` | SÍ | — | huérfana, sólo lectura (SA-COM-01) |
| `cpcUsuLog` | `CPCUsuLog` | `String` | SÍ | 10 | huérfana, sólo lectura (SA-COM-01) |
| `cpcSnNum` | `CPCSnNum` | `BigDecimal` | SÍ | — | huérfana, sólo lectura (SA-COM-01) |
| `cpcFePag` | `CPCFePag` | `LocalDateTime` | SÍ | — | huérfana, sólo lectura (SA-COM-01) |

### 3.2 `COMISIO1` (entity `ComisionIntegrante`, ya existente en núcleo)

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Notas |
|---|---|---|---|---|---|
| `id.pueCod` | `PueCod` | `Integer` | NO | — | PK 1, `smallint` |
| `id.cpcId` | `CPCId` | `Integer` | NO | — | PK 2, `int` |
| `id.cpcPerId` | `CPCPerId` | `Integer` | NO | — | PK 3, `int` |
| `perNomyApe` (transient) | — (calculado) | `String` | — | — | JOIN a `PERSONAS.PerNomyApe` (trim) |

### 3.3 `PERSONAS` (entity `Persona`, ya existente en núcleo)

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Notas |
|---|---|---|---|---|---|
| `perId` | `PerId` | `Integer` | NO | — | PK, `int` |
| `perNomyApe` | `PerNomyApe` | `String` | SÍ | 40 | `char(40)` → `@ColumnTransformer read = RTRIM(...)` |
| `areaId` | `AreaId` | `BigDecimal` | SÍ | — | `decimal` (no usado) |
| `perCodUsu` | `PerCodUsu` | `String` | SÍ | 10 | `char(10)` (no usado) |

### 3.4 `PUEPPRIM` (entity a crear si SA-COM-03 = "mapear")

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Notas |
|---|---|---|---|---|---|
| `pueCod` | `PueCod` | `Integer` | NO | — | PK, `smallint` |
| `pueDesc` | `PueDesc` | `String` | SÍ | 20 | `char(20)` → trim (atributo calculado `PueDesc` del legacy) |

> Si no se crea la entity, resolver `PueDesc` por consulta nativa en el servicio (decisión del
> coordinador).

### 3.5 Default de aplicación para `NOT NULL` sin contraparte en el DTO

No hay columnas `NOT NULL` huérfanas en `COMISION`/`COMISIO1`/`PERSONAS` que requieran default de
aplicación (todas las `NOT NULL` son PK). Las columnas huérfanas son todas nulables. **No se
requiere default de aplicación.**

## 4. Contratos MVC (sin endpoints REST nuevos)

| Método | Ruta | Descripción | Vista |
|---|---|---|---|
| `GET` | `/comisiones` | Listado paginado (filtros legacy, orden `CPCId desc`) | `puestos/comisiones` |
| `GET` | `/comisiones/{pueCod}/{cpcId}` | Detalle (tabs General/Integrantes) | `puestos/comision-detalle` |

**No se agregan endpoints `/api/**`.** G-POSTMAN no aplica (§5 de spec.md).

Parámetros de URL del listado:
- `pagina` (0-based, opcional, default 0) — página actual.
- `pueCod`/`cpcId` no son parámetros del listado; la selección de fila viaja por la ruta del detalle.

## 5. Contrato de vistas (Thymeleaf)

### 5.1 Vista de listado — `puestos/comisiones.html`

**Ruta de página:** `GET /comisiones`
**Nombre de plantilla:** `puestos/comisiones`
**Layout base:** `layout/base` (header, sesión, navegación, pie).

**View model (atributos del modelo):**

| Atributo | Tipo | Origen |
|---|---|---|
| `comisiones` | `List<ComisionFilaView>` | Servicio (página actual) |
| `paginaActual` | `int` | `Page.getNumber()` |
| `cantidadPaginas` | `int` | `Page.getTotalPages()` |
| `totalComisiones` | `long` | `Page.getTotalElements()` |
| `hayPaginaAnterior` | `boolean` | `Page.hasPrevious()` |
| `hayPaginaSiguiente` | `boolean` | `Page.hasNext()` |
| `mensajeError` | `String` (nullable) | Error de carga |
| `mensajeExito` | `String` (flash, nullable) | No usado en esta iteración (sin escrituras) |

**`ComisionFilaView` (campos):**

| Campo | Tipo | Origen legacy |
|---|---|---|
| `pueCod` | `Integer` | `PueCod` (oculto, usado para el link al detalle) |
| `cpcId` | `Integer` | `CPCId` ("Id Comisión") |
| `fechaInicio` | `String` (formateado) | `CPCFchDes` ("Fecha Inicio") |
| `horaInicio` | `String` (formateado) | `CPCHorDes` ("Hora Inicio", `HHMM`) |
| `fechaFin` | `String` (formateado) | `CPCFchHas` ("Fecha Fin") |
| `horaFin` | `String` (formateado) | `CPCHorHas` ("Hora Fin", `HHMM`) |
| `estado` | `String` | Decodificado de `CPCCierre` (R-COM-006) |
| `responsable` | `String` | `PerNomyApe` (trim) vía JOIN (R-COM-007) |

**Reglas de validación visibles:** ninguna (sólo lectura).

**Comportamiento ante error:** si la carga falla, mostrar `mensajeError` y estado vacío (paridad con
turnos: "No se pudieron mostrar las comisiones.").

**Paginación (Figma/legacy):** controles `First`/`Prev`/`Next`/`Last` con habilitado/deshabilitado
según `hayPaginaAnterior`/`hayPaginaSiguiente`, igual que `turnos.html`.

**Tabla:** 7 columnas visibles con rótulos verbatim: `Id Comisión`, `Fecha Inicio`, `Hora Inicio`,
`Fecha Fin`, `Hora Fin`, `Estado`, `Responsable`. Cada fila enlaza a `/comisiones/{pueCod}/{cpcId}`.

### 5.2 Vista de detalle — `puestos/comision-detalle.html`

**Ruta de página:** `GET /comisiones/{pueCod}/{cpcId}`
**Nombre de plantilla:** `puestos/comision-detalle`
**Layout base:** `layout/base`.

**View model (atributos del modelo):**

| Atributo | Tipo | Origen |
|---|---|---|
| `existe` | `boolean` | Si la comisión existe |
| `general` | `ComisionGeneralView` (nullable) | Campos del WebComponent `Comisiones_General` |
| `integrantes` | `List<IntegranteView>` | Grilla de `Comisiones_IntegrantesWC` |
| `tabActiva` | `String` | `"General"` o `"Integrantes"` (parámetro `tab`/`TabCode`) |
| `mensajeError` | `String` (nullable) | Error de carga |

**`ComisionGeneralView` (campos):**

| Campo | Tipo | Rótulo verbatim |
|---|---|---|
| `pueCod` | `Integer` | `Código del Puesto` |
| `pueDesc` | `String` | `Puesto Caminero` |
| `cpcId` | `Integer` | `Id Comisión` |
| `fechaInicio` | `String` | `Fecha Inicio` |
| `horaInicio` | `String` | `Hora Inicio` |
| `fechaFin` | `String` | `Fecha Fin` |
| `horaFin` | `String` | `Hora Fin` |
| `estado` | `String` | `Estado` (decodificado) |
| `cpcResId` | `Integer` | `Id. Responsable` |
| `responsable` | `String` | `Responsable` (PerNomyApe trim) |

**`IntegranteView` (campos):**

| Campo | Tipo | Rótulo verbatim |
|---|---|---|
| `cpcPerId` | `Integer` | `Personal Afectado` |
| `perNomyApe` | `String` | `Ap. y Nombre Persona Comis.` (trim) |

**Tabs:** `General` e `Integrantes`, reproduciendo `Comisiones_ViewComisiones` (TabbedView). La tab
activa se controla por parámetro `tab` (default `General`).

**Comportamiento ante inexistencia:** si la comisión no existe, mostrar "No se encontraron registros"
y ocultar las tabs (R-COM-013).

## 6. Dónde vive el código

| Capa | Ubicación | Notas |
|---|---|---|
| Domain (entidades) | `atp-nucleo-compartido/.../nucleo/domain/` (`Comision`, `ComisionIntegrante`, `ComisionId`, `ComisionIntegranteId`, `Persona`, `Puesto` si SA-COM-03) | Ya existe; no recrear |
| Repositories | `atp-nucleo-compartido/.../nucleo/repository/` (`ComisionRepository`, `ComisionIntegranteRepository`) | Ya existe; reutilizar |
| Service | `atp-puestos-camineros/.../puestos/service/ServicioComisiones.java` (nuevo) | Dominio específico del módulo desplegable |
| MVC Controller | `atp-puestos-camineros/.../puestos/web/mvc/ComisionMvcController.java` (nuevo) | Adaptador, sin reglas propias |
| View models | `atp-puestos-camineros/.../puestos/web/mvc/comision/` (`ComisionFilaView`, `ComisionGeneralView`, `IntegranteView`) | Nuevos |
| Templates | `atp-puestos-camineros/src/main/resources/templates/puestos/comisiones.html`, `comision-detalle.html` | Nuevos |
| Mensajes | `atp-puestos-camineros/.../puestos/service/MensajesComision.java` (nuevo) | Centralizados |

## 7. Riesgos

- `char` de longitud fija: comparación y `LIKE` con padding (mitigación: `trim()` en el servicio y
  `@ColumnTransformer` en el núcleo).
- Volumen de la tabla (3 120 filas en `COMISION`): el listado filtra por puesto y responsable;
  paginación server-side limita el resultado.
- `PueDesc` (Puesto Caminero) requiere JOIN a `PUEPPRIM` o consulta nativa (SA-COM-03).
- Las columnas huérfanas no se escriben: riesgo de que Hibernate intente validarlas si se mapean
  sin setters (mitigación: `@Column` read-only, sin setters públicos).
- `BigDecimal` para `CPCSnNum`: no se expone en la UI (fuera de alcance); sólo se mapea.

## 8. Supuestos abiertos (resueltos por el coordinador, 2026-11-05)

- **SA-COM-01 — RESUELTO: MAPEAR.** Agregar los 4 campos huérfanos (`cpcFeLog`, `cpcUsuLog`, `cpcSnNum`, `cpcFePag`) a `Comision` con `@Column` read-only (sin setters públicos). T-COM-A2 se ejecuta.
- **SA-COM-02 — RESUELTO: mantener filtro legacy.** Filtro `PueCod = SucNroId` y `CPCResId = PersonaId` (ambos `when not empty`); si `PersonaId` es 0/null, se omite ese filtro.
- **SA-COM-03 — RESUELTO: NO crear entity `Puesto`.** Resolver `PueDesc` por consulta nativa en `ComisionRepository` (`findPueDescByPueCod`). T-COM-A3 se convierte en "implementar consulta nativa".
- **SA-COM-04 — RESUELTO: constante del servicio.** `TAMANIO_PAGINA = 10`.
- **SA-COM-05 — RESUELTO: mostrar como `HHMM` con cero a la izquierda (4 dígitos).**
- **SA-COM-06 — RESUELTO: orden por `PerNomyApe` trim ascendente, sin orden secundario.**
