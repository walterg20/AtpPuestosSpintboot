# Spec — Módulo de Turnos `06-turnos`

**Estado:** APPROVED (2026-10-23)  
**Módulo:** `06-turnos` — Puestos Camineros / Receptorías  
**Fuente legacy (solo lectura):** KB `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13), carpeta `Root Module/Turnos/`.  
**Análisis de origen:** Engram `topic_key: spec/turnos` (obs #163) — objetos `TurnosListado`,
`TurnosListadoAbiertos`, `PPrim_Turnos`, `PPrim_TurnosNue`, `WorkWithWebPPrim_Turnos`,
`PPrim_CerrarTurno`, `PPrim_EstadoDeForm`, `PPrim_NumTur`, `TurnoGenerarSN`.

**Alcance:** listado de turnos, **alta** de turno y **cierre** de turno, con paridad de reglas y
mensajes verbatim del legacy.

---

## 1. Objetivo

Reimplementar la pantalla `TurnosListado` del legacy (grilla + "Nuevo Turno" + "Cerrar Turno")
en Spring MVC + Thymeleaf, sobre la tabla `PPRIM_TU` de SQL Server, sin modificar la KB ni el
esquema (regla de paridad de BD: `ddl-auto=none`, sin DDL, sin migraciones).

## 2. Modelo de datos (paridad de BD)

### 2.1 `PPRIM_TU` — PK `(SucNroId, SucTipo, TurNroId)`

| Atributo | Físico | Tipo físico | Notas |
|---|---|---|---|
| `SucNroId` | `SucNroId` | `smallint` NOT NULL | PK 1 — sucursal del turno |
| `SucTipo` | `SucTipo` | `char(3)` NOT NULL | PK 2 — `PUE` / `REC` |
| `TurNroId` | `TurNroId` | `int` NOT NULL | PK 3 — nro de turno |
| `TurUsuario` | `TurUsuario` | `char(10)` NOT NULL | usuario que abrió el turno |
| `TurFchApe` | `TurFchApe` | `datetime` NOT NULL | KB `DATE` ↔ BD `datetime` |
| `TurFchCie` | `TurFchCie` | `datetime` NOT NULL | vacío = `1753-01-01 00:00:00` |
| `TurEstado` | `TurEstado` | `char(1)` NOT NULL | `A` abierto / `C` cerrado |
| `TurHraApe` | `TurHraApe` | `datetime` NOT NULL | |
| `TurHraCie` | `TurHraCie` | `datetime` NOT NULL | vacío = `1753-01-01 00:00:00` |
| `TurCPCId` | `TurCPCId` | `smallint` NULL | KB `NOT NULL`; default de app `0` |
| `SNSinNum` | `SNSinNum` | `decimal(10,0)` NULL | SN del volante de pago (sólo REC) |
| `TurFchPago` | `TurFchPago` | `datetime` NULL | vacío = `NULL` **o** `1753-01-01` |

PK compuesta ⇒ `@EmbeddedId`/`@IdClass`.

### 2.2 Tablas de apoyo (sólo lectura)
- `COMISION` (PK `PueCod`,`CPCId`): `CPCEstado` (`A`/`P`/`C`/NULL), `CPCCierre`, `CPCFchCie`.
  `CPCResNyA` existe en la KB pero **no** en la tabla física → **no mapear**.
- `PUEPPRIM` (PK `PueCod`): `PueDesc`.
- `GES_SUCU`: `SucNroId`, `SucTipo`, `SucDescr`.
- `LOTES227` (PK `LF276ID`): `LF276Estad` (`P`/`C`), `LF276FchHa`.
- `F2276` (PK `F276Id`): `SucNroId`, `SucTipo`, `TurNroId`, `F276NCpbte`, `LF276ID`,
  `F276Estad`, `F276FchMov`, `F276ImIBTot`, `F276ImpuFl`, `F276ImCMTot`, `F276AdCMTo`,
  `F276AdicFl`, `F276AdIBTo`.
- `SNSINDEV` (PK `SNSinNum`): destino del SN del volante de pago.

## 3. Requisitos (EARS)

### 3.1 Listado
- **R-TUR-001:** El sistema SHALL listar los turnos de la sucursal del usuario autenticado
  (`Context.Sucursal` = `SucNroId`, `Context.Tipo` = `SucTipo`), ordenados por
  `SucNroId, SucTipo, TurNroId`.
- **R-TUR-002:** La grilla SHALL mostrar, con rótulos **verbatim**, las columnas visibles del
  legacy: `Id. Turno`, `Comisión`, `Fecha Apertura`, `Fecha Cierre`, `Estado`; y SHALL ocultar
  `SucNroId`, `SucTipo` y `SNSinNum`.
- **R-TUR-003:** La grilla SHALL paginar de a 10 filas (paridad con `MaxRows="10"`).
- **R-TUR-004:** La pantalla SHALL ofrecer las acciones `Nuevo Turno` y `Cerrar Turno`.
- **R-TUR-005:** El botón `Cerrar Turno` SHALL pedir confirmación con el texto verbatim
  `¿Confirma que desea cerrar el Turno?`.

### 3.2 Alta
- **R-TUR-006:** El alta SHALL asignar `TurNroId` como el **máximo `TurNroId` + 1** de la
  sucursal/tipo (o `1` si no hay filas) — paridad con `PPrim_NumTur`.
- **R-TUR-007:** El alta SHALL escribir `TurUsuario` = usuario de la sesión, `TurFchApe` =
  `serverdate`, `TurHraApe` = `servernow`, `TurEstado` = `A`, `TurCPCId` = `Context.Comision`
  (default `0` si vacío).
- **R-TUR-008:** El alta SHALL escribir `TurFchCie` y `TurHraCie` con la sentinela
  `1753-01-01T00:00:00` (columnas `NOT NULL` sin default) y `TurFchPago` en `NULL`.
- **R-TUR-009:** Tras un alta exitosa, el sistema SHALL actualizar `Context.Turno` con el
  `TurNroId` nuevo y persistir el contexto de sesión.
- **R-TUR-010:** Si el tipo es `PUE` y la comisión de `Context.Sucursal`/`Context.Comision` tiene
  `CPCEstado = 'C'`, el sistema SHALL rechazar el alta con el mensaje verbatim
  `La comisión actual esta cerrada , no se puede abrir nuevos turnos`.
- **R-TUR-011:** Para tipo `REC` el alta NO SHALL consultar el estado de la comisión
  (paridad: el legacy no lo hace).

### 3.3 Cierre
- **R-TUR-012:** El cierre, en este orden exacto, SHALL:
  1. si `TurEstado <> 'A'` → mensaje `El turno se encuentra cerrado` y abortar;
  2. si existen formularios abiertos del turno (`PPrim_EstadoDeForm` > 0) → mensaje
     `Existen Formularios Abiertos` y abortar;
  3. si el procedimiento de cierre falla → mensaje `No se puede cerrar el turno`;
  4. si tiene éxito → `TurFchCie` = `serverdate`, `TurHraCie` = `servernow`,
     `TurEstado` = `C`, `Context.Turno` = 0 (persistido) y mensaje
     `El turno seleccionado ha sido cerrado`.
- **R-TUR-013:** `PPrim_EstadoDeForm` SHALL considerar abierto un turno cuando existe un
  `F2276` del turno cuyo lote (`LOTES227` vía `LF276ID`) tiene `LF276Estad = 'C'` y
  `LF276FchHa <= hoy`.
- **R-TUR-014:** Para tipo `REC`, el cierre SHALL generar el SN del volante de pago
  (`TurnoGenerarSN`) y, si la generación falla, SHALL revertir **toda** la operación de cierre
  (el turno permanece abierto) y mostrar `No se puede cerrar el turno`.
- **R-TUR-015:** `TurnoGenerarSN` SHALL totalizar los `F2276` del turno **excluyendo anulados**
  (`F276Estad <> 'A'`), con `TotalIB = TotIB + TotFletes + TotCM` y
  `TotalAd = TotAdIB + TotAdCM + TotAdFletes`; si el total es 0 SHALL NO generar SN y devolver
  éxito.
- **R-TUR-016:** El SN generado SHALL registrarse en `SNSINDEV` con `CodForm = 126`,
  `SNSinCuit = 33999176769`, `SNSinCuo`/`SNSinAnio` del mes/año en curso, `SNSinTotIB` = total,
  `SNSinAdic1` = adicional, `SNSinFPres` = `serverdate`, `SNSinDpcia` = `SucNroId`,
  `SNSinTipLe` = 2, `SNSinIdVol` = 6, y SHALL quedar referenciado en `PPRIM_TU.SNSinNum`.
- **R-TUR-017:** El vencimiento del SN SHALL ser 2 días hábiles para `REC` y
  `serverdate + 15` para el resto.

### 3.4 Navegación e integración
- **R-TUR-018:** El ítem de menú `Turnos` SHALL enlazar a `/turnos` en los tipos `PUE` y `REC`.
- **R-TUR-019:** La pantalla SHALL usar el layout base con header, menú horizontal y footer.

## 4. Decisiones

- **D-TUR-001:** Se replica `PPrim_NumTur` tal cual (máximo + 1), sin corregir al "turno
  abierto + 1" (el legacy no filtra por estado).
- **D-TUR-002:** Se replica la permisividad del legacy: si la comisión no existe o su
  `CPCEstado` es `NULL`, el alta en `PUE` **se permite**.
- **D-TUR-003:** `TurCPCId` se persiste como `0` (no `null`) cuando `Context.Comision` está vacío.
- **D-TUR-004:** Los `char` de longitud fija se normalizan con `trim()` en el servicio.
- **D-TUR-005:** El botón `Resumen` (impresión) y el `Volante de Pago` quedan **fuera de alcance**:
  en el legacy sus links están comentados (inerentes).
- **D-TUR-006:** El WorkWith completo (`WWPPrim_Turnos` con tabs de F2276) queda **fuera de
  alcance**: sus links de Update/Delete están comentados en el legacy.
- **D-TUR-007:** `CPCResNyA` no se mapea (no existe en la tabla física).

## 5. Fuera de alcance
- WorkWith `WWPPrim_Turnos` y sus tabs de F2276/FormDef.
- Impresión de "Resumen" y "Volante de Pago" (PDF).
- `TurnosListadoAbiertos` (selector de turno abierto) — módulo aparte.
- Cierre de comisión (`PPrim_CerrarComision`) — módulo Comisiones.

## 6. Supuestos abiertos

- ASM-TUR-01: vínculo del lote `F2276.LF276ID = LOTES227.LF276ID` (confirmado por el analista).
- ASM-TUR-02: el tipo `COM` no existe en `PPRIM_TU` (sólo `PUE`/`REC`).
- ASM-TUR-03: las fechas `1753-01-01` son la sentinela de "vacío" en columnas `NOT NULL`.
