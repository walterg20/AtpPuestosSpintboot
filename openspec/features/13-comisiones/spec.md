# Spec — Módulo de Comisiones `13-comisiones`

**Estado:** DRAFT (2026-11-05)
**Módulo:** `13-comisiones` — Puestos Camineros
**Fuente legacy (solo lectura):** KB `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13), carpeta `Root Module/Comisiones/`.
**Objetos legacy:** WebPanels `ComisionesListado` (listado), `Comisiones_ViewComisiones` (detalle con tabs General/Integrantes);
WebComponents `Comisiones_General` (detalle General), `Comisiones_IntegrantesWC` (grilla de integrantes).
**Análisis de origen:** `docs/analisis-ATPPuestos.md` §5 (capa web), §13 (G-DATA `COMISION`/`COMISIO1`), §15.1 (seguridad: `COMISION`).

**Alcance confirmado (decisiones del usuario, 2026-11-05):**
1. Solo LISTADO + DETALLE de comisiones. **Sin PDFs** (Imprimir / Form. Emitidos quedan fuera de alcance).
2. Estado de la columna "Estado" derivado de `CPCCierre`: `'S'`→"Cerrado", `'N'`→"Abierto".
3. Paginación server-side (default 10, configurable).
4. Integrantes: solo consulta (sin alta/baja).
5. Menú "Comisiones" ya existe en `ServicioAutorizacion` (no agregar).

---

## 1. Objetivo

Reimplementar la pantalla `ComisionesListado` (grilla paginada) y el detalle `Comisiones_ViewComisiones`
(tabs General e Integrantes) en Spring MVC + Thymeleaf, sobre las tablas `COMISION` y `COMISIO1` de
SQL Server, sin modificar la KB ni el esquema (regla de paridad de BD: `ddl-auto=none`, sin DDL,
sin migraciones). La única escritura posible (cierre de comisión) queda **fuera de alcance** en
esta iteración.

## 2. Modelo de datos (paridad de BD)

### 2.1 `COMISION` — PK compuesta (`PueCod`, `CPCId`)

| Atributo (Java) | Físico | Tipo físico | Nulable | Notas |
|---|---|---|---|---|
| `id.pueCod` | `PueCod` | `smallint` | NO | PK 1 — puesto caminerero |
| `id.cpcId` | `CPCId` | `int` | NO | PK 2 — número de comisión |
| `cpcFchDes` | `CPCFchDes` | `datetime` | SÍ | Fecha inicio (KB `DATE` ↔ BD `datetime`) |
| `cpcFchHas` | `CPCFchHas` | `datetime` | SÍ | Fecha fin |
| `cpcCierre` | `CPCCierre` | `char(1)` | SÍ | `'N'` abierta / `'S'` cerrada (trim al leer) |
| `cpcResId` | `CPCResId` | `int` | SÍ | Persona responsable (`0`/null si no hay) |
| `cpcHorDes` | `CPCHorDes` | `int` | SÍ | Hora inicio (formato `HHMM`) |
| `cpcHorHas` | `CPCHorHas` | `int` | SÍ | Hora fin (formato `HHMM`) |
| `cpcFeLog` | `CPCFeLog` | `datetime` | SÍ | **Columna huérfana** (no existe en la KB). Se mapea, **no se escribe**. |
| `cpcUsuLog` | `CPCUsuLog` | `char(10)` | SÍ | **Columna huérfana**. Se mapea, **no se escribe**. |
| `cpcEstado` | `CPCEstado` | `char(1)` | SÍ | `'A'`/`'P'`/`'C'` (usado por turnos; no se muestra en esta pantalla) |
| `cpcFchCie` | `CPCFchCie` | `datetime` | SÍ | Fecha de cierre (no se muestra en esta pantalla) |
| `cpcSnNum` | `CPCSnNum` | `decimal` | SÍ | **Columna huérfana**. Se mapea, **no se escribe**. |
| `cpcFePag` | `CPCFePag` | `datetime` | SÍ | **Columna huérfana**. Se mapea, **no se escribe**. |

### 2.2 `COMISIO1` — PK compuesta (`PueCod`, `CPCId`, `CPCPerId`)

| Atributo (Java) | Físico | Tipo físico | Nulable | Notas |
|---|---|---|---|---|
| `id.pueCod` | `PueCod` | `smallint` | NO | PK 1 |
| `id.cpcId` | `CPCId` | `int` | NO | PK 2 |
| `id.cpcPerId` | `CPCPerId` | `int` | NO | PK 3 — persona integrante |

### 2.3 `PERSONAS` — PK `PerId` (sólo lectura, JOIN)

| Atributo (Java) | Físico | Tipo físico | Nulable | Notas |
|---|---|---|---|---|
| `perId` | `PerId` | `int` | NO | PK |
| `perNomyApe` | `PerNomyApe` | `char(40)` | SÍ | Nombre y apellido (trim al leer) |
| `areaId` | `AreaId` | `decimal` | SÍ | No usado en esta pantalla |
| `perCodUsu` | `PerCodUsu` | `char(10)` | SÍ | No usado en esta pantalla |

### 2.4 Atributos calculados (NO son columnas)

- `CPCResNyA` (responsable) y `CPCPerNyA` (integrante) **no existen** en la tabla física
  (G-DATA 2026-10-22: 0 coincidencias en toda `Rentas`). Se resuelven por **JOIN a `PERSONAS`**
  (`PerNomyApe`) por `CPCResId` / `CPCPerId` (D-LOG-20, R-LOG-035/036). **Prohibido** declararlos
  como `@Column`. El mapeo ya está en `Comision` (entity) y `ComisionIntegrante` (entity) del núcleo;
  el JOIN se implementa en el servicio/repositorio.

### 2.5 Sin claves foráneas físicas

No existen FK físicas entre `COMISION`, `COMISIO1` y `PERSONAS`: las asociaciones son lógicas y las
reproduce el servicio (JOIN por `CPCResId`/`CPCPerId`).

## 3. Requisitos (EARS)

### 3.1 Listado

- **R-COM-001:** El sistema SHALL listar las comisiones filtradas por `PueCod = Context.Sucursal`
  (cuando no vacío) **y** `CPCResId = Context.PersonaId` (cuando no vacío), reproduciendo el filtro
  del grid `GridComisiones` del legacy `ComisionesListado` (paridad con el `For Each` del WebPanel).
- **R-COM-002:** El sistema SHALL ordenar el listado por `CPCId` descendente (paridad con el
  `Order` del grid del legacy; el usuario confirmó `CPCId desc`).
- **R-COM-003:** El sistema SHALL paginar el listado de a 10 filas por defecto (paridad con
  `MaxRows="10"` del grid del legacy), con tamaño de página configurable y navegación
  primero/anterior/siguiente/último (R-COM-007).
- **R-COM-004:** El sistema SHALL mostrar, con rótulos **verbatim**, las columnas visibles del
  legacy: `Id Comisión`, `Fecha Inicio`, `Hora Inicio`, `Fecha Fin`, `Hora Fin`, `Estado`,
  `Responsable`.
- **R-COM-005:** El sistema SHALL **no** mostrar la columna `PueCod` (oculta en el legacy) ni las
  columnas `Imprimir` y `Form. Emitidos` (enlaces a reportes PDF) en esta iteración: ambos quedan
  **fuera de alcance** (§6).
- **R-COM-006:** El sistema SHALL decodificar el campo `Estado` a partir de `CPCCierre`:
  `'S'` → `"Cerrado"`, `'N'` → `"Abierto"`, `null` → `"Abierto"` (decisión D-COM-003).
- **R-COM-007:** El sistema SHALL resolver el `Responsable` por JOIN a `PERSONAS` (`PerNomyApe`,
  con `trim`) usando `CPCResId`; si `CPCResId` es `null` o `0`, el sistema SHALL mostrar el valor
  vacío (paridad con el atributo calculado `CPCResNyA` del legacy).
- **R-COM-008:** El sistema SHALL formatear `Fecha Inicio`/`Fecha Fin` (`datetime`) y `Hora Inicio`/
  `Hora Fin` (`int` en formato `HHMM`) de forma legible para el usuario (Thymeleaf), sin alterar
  los valores persistidos.

### 3.2 Detalle General

- **R-COM-009:** El sistema SHALL mostrar el detalle de una comisión identificada por
  (`PueCod`, `CPCId`), con los campos del WebComponent `Comisiones_General`, con rótulos
  **verbatim**: `Código del Puesto`, `Puesto Caminero`, `Id Comisión`, `Fecha Inicio`,
  `Hora Inicio`, `Fecha Fin`, `Hora Fin`, `Estado`, `Id. Responsable`, `Responsable`.
- **R-COM-010:** El sistema SHALL resolver `Puesto Caminero` por JOIN a `PUEPPRIM` (`PueDesc`)
  cuando esté disponible; si no hay fila, mostrar vacío (paridad con el atributo calculado `PueDesc`
  del legacy).
- **R-COM-011:** El sistema SHALL resolver `Responsable` por JOIN a `PERSONAS` (`PerNomyApe`, trim)
  usando `CPCResId` (paridad con `CPCResNyA`).
- **R-COM-012:** El sistema SHALL decodificar `Estado` con la misma regla que R-COM-006.
- **R-COM-013:** El sistema SHALL mostrar un mensaje de "No se encontraron registros" y ocultar
  la navegación de tabs cuando la comisión no exista (paridad con `Comisiones_ViewComisiones`:
  `Form.Caption = "No se encontraron registros"`, `ViewAll.Visible = false`).

### 3.3 Integrantes (solo consulta)

- **R-COM-014:** El sistema SHALL listar los integrantes de una comisión (`COMISIO1` por
  `PueCod`, `CPCId`), mostrando `Personal Afectado` (CPCPerId) y `Ap. y Nombre Persona Comis.`
  (PerNomyApe vía JOIN a `PERSONAS`), con rótulos **verbatim**.
- **R-COM-015:** El sistema SHALL ordenar los integrantes por `PerNomyApe` (trim) ascendente.
- **R-COM-016:** El sistema SHALL **no** ofrecer alta ni baja de integrantes en esta iteración
  (fuera de alcance; §6).

### 3.4 Navegación e integración

- **R-COM-017:** El sistema SHALL exponer las rutas MVC `GET /comisiones` (listado) y
  `GET /comisiones/{pueCod}/{cpcId}` (detalle). **No** se agregan endpoints `/api/**` nuevos
  (G-POSTMAN no aplica; documentado en §5).
- **R-COM-018:** La vista de detalle SHALL mostrar tabs `General` e `Integrantes`, reproduciendo
  la estructura de `Comisiones_ViewComisiones` (TabbedView con `Comisiones_General` y
  `Comisiones_IntegrantesWC`).
- **R-COM-019:** El ítem de menú `Comisiones` (existente en `ServicioAutorizacion`) SHALL enlazar
  a `/comisiones`. Se corrige el ítem existente para agregar el link (hallazgo H-1 del QA: el link
  era `null`). No se agregan nuevos ítems.
- **R-COM-020:** La pantalla SHALL usar el layout base con header, menú horizontal y footer, y el
  diseño institucional estándar (Figma frame "Comision", id 2037:2).

### 3.5 Calidad (ISO/IEC 25030)

- **R-COM-021:** Adecuación funcional: paridad con el legacy en columnas, rótulos, orden,
  paginación (10 filas), decodificación de Estado y resolución de Responsable por JOIN.
- **R-COM-022:** Usabilidad: navegación por tabs, estados vacío/error visibles, paginación con
  controles habilitados/deshabilitados, formato legible de fechas/horas.
- **R-COM-023:** Mantenibilidad: reglas en `service/**`; `web/mvc` es adaptador sin reglas propias;
  entidades/repos del núcleo reutilizadas sin recrear.
- **R-COM-024:** Seguridad: acceso sólo con sesión autenticada (paridad con `IsAuthorized` del
  legacy); el menú ya filtra por programa.

## 4. Decisiones

- **D-COM-001:** Se replica el filtro del grid del legacy (`PueCod = SucNroId` y `CPCResId = PersonaId`),
  no sólo `PueCod`. Si alguno está vacío en la sesión, no se aplica ese filtro (paridad con el
  `when not empty` del WebPanel).
- **D-COM-002:** El detalle es de **solo lectura** (sin edición ni cierre de comisión). El cierre
  (`PPrim_CerrarComision`) y el alta quedan fuera de alcance (§6).
- **D-COM-003:** `CPCCierre` `null` → `"Abierto"` (paridad con la permisividad del legacy).
- **D-COM-004:** Los `char` de longitud fija se normalizan con `trim()` en el servicio (paridad con
  `ColumnTransformer read = RTRIM(...)` del núcleo).
- **D-COM-005:** La paginación es server-side con `Pageable` (Spring Data), tamaño de página 10 por
  defecto, configurable por constante del servicio.
- **D-COM-006:** Las columnas huérfanas (`CPCFeLog`, `CPCUsuLog`, `CPCSnNum`, `CPCFePag`) se
  mapean (ya están en la entity `Comision`? verificar — ver supuesto SA-COM-01) pero **no se
  escriben** ni se exponen en la UI.
- **D-COM-007:** Los enlaces a reportes PDF (`Imprimir` → `RProPri_TurXCom`, `Form. Emitidos` →
  `RPPriTur_TurnosForm`) quedan fuera de alcance (§6); los botones no se renderizan.

## 5. G-POSTMAN

Este feature **no agrega, modifica ni elimina endpoints `/api/**`** (sólo MVC). Por lo tanto, la
colección Postman (`docs/postman/ATPPuestos-API.postman_collection.json`) **no se modifica** en esta
iteración. Se documenta explícitamente que G-POSTMAN no aplica.

## 6. Fuera de alcance

- PDFs / reportes: `Imprimir` (`RProPri_TurXCom`) y `Form. Emitidos` (`RPPriTur_TurnosForm`).
- Cierre de comisión (`PPrim_CerrarComision`, evento `CerrarComision` del listado).
- Alta de comisión (no existe en el legacy para esta pantalla).
- Alta/baja de integrantes (sólo consulta).
- Edición de los campos del detalle General.
- Módulo `Comisiones_WWComisiones` (WorkWith completo) y sus tabs de F2276.
- `ComisionesListadoAbiertos` / selector de comisión abierta (módulo aparte).

## 7. Supuestos abiertos (resueltos por el coordinador, 2026-11-05)

- **SA-COM-01 — RESUELTO: MAPEAR.** Se agregan los 4 campos huérfanos (`cpcFeLog`, `cpcUsuLog`, `cpcSnNum`, `cpcFePag`) a `Comision` con `@Column` read-only (sin setters públicos), por paridad de BD (regla: las columnas huérfanas se mapean, no se rediseñan). Tarea T-COM-A2 se ejecuta.
- **SA-COM-02 — RESUELTO: mantener el filtro legacy.** El filtro `PueCod = SucNroId` y `CPCResId = PersonaId` (ambos `when not empty`) se reproduce tal cual. Si `PersonaId` es 0/null en la sesión, ese filtro se omite (paridad con el WebPanel). Documentar en el servicio.
- **SA-COM-03 — RESUELTO: NO crear entity `Puesto`.** Se resuelve `PueDesc` por consulta nativa en `ComisionRepository` (método `findPueDescByPueCod`) para evitar una nueva entity en el núcleo y mantener la regla de paridad (sólo SELECT). Tarea T-COM-A3 se convierte en "implementar consulta nativa" (no crear entity).
- **SA-COM-04 — RESUELTO: constante del servicio.** Tamaño de página = 10 por defecto, configurable por constante `ServicioComisiones.TAMANIO_PAGINA` (paridad con turnos).
- **SA-COM-05 — RESUELTO: mostrar como `HHMM` con cero a la izquierda (4 dígitos).** Paridad visual con el legacy (que muestra el número). Implementar en el servicio al formatear `horaInicio`/`horaFin`.
- **SA-COM-06 — RESUELTO: orden por `PerNomyApe` trim ascendente, sin orden secundario.** Confirmado.
