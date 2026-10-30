# Spec — Reporte "Resumen de Turnos" `07-reportes-turnos`

**Estado:** DRAFT (para aprobación — G-SPEC)  
**Módulo:** `07-reportes-turnos` — Puestos Camineros / Receptorías  
**Fuente legacy (solo lectura):** KB `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13), carpeta
`Root Module/Turnos/` y `Root Module/F2776/Procedimientos/`.  
**Análisis de origen:** Engram `topic_key: spec/turnos` (obs #163) y el presente análisis
(`topic_key: spec/reportes-turnos`).  
**Módulo previo:** `06-turnos` (APPROVED 2026-10-23) — dejó explícitamente fuera de alcance
la impresión de "Resumen" y "Volante de Pago" (D-TUR-005, §5). Este spec incorpora únicamente el
reporte **"Resumen de Turnos"**.

**Alcance:** generación de un PDF "Resumen de Turnos" para un turno seleccionado
(`SucNroId`, `SucTipo`, `TurNroId`), invocable desde la columna "Resumen" de `TurnosListado` y
desde la API REST.

---

## 1. Objetivo

Reimplementar la acción **"imprimir resumen"** del legacy (`TurnosListado`, columna `Resumen`) que
en la KB figura **comentada** (`&Imprimir.Link = ActaResumenTurno.Link(...)`), como un reporte PDF
nuevo, sobre las tablas existentes de SQL Server, sin modificar la KB ni el esquema
(`ddl-auto=none`, sin DDL, sin migraciones). El reporte consolida los datos del turno y el detalle
de los formularios `F2276` del mismo.

## 2. Modelo de datos (paridad de BD — solo lectura)

### 2.1 `PPRIM_TU` — PK `(SucNroId, SucTipo, TurNroId)` (ya mapeada en `06-turnos`)

Campos usados por el reporte: `SucNroId`, `SucTipo`, `TurNroId`, `TurUsuario`, `TurFchApe`,
`TurFchCie`, `TurEstado`, `TurHraApe`, `TurHraCie`, `TurCPCId`, `SNSinNum`.

### 2.2 `F2276` — PK `F276Id` (detalle del reporte)

| Atributo | Físico | Tipo físico | Notas |
|---|---|---|---|
| `F276Id` | `F276Id` | `int` NOT NULL | N° de acta (identificador del formulario) |
| `SucNroId` | `SucNroId` | `smallint` NOT NULL | FK lógica al turno |
| `SucTipo` | `SucTipo` | `char(3)` NOT NULL | FK lógica al turno |
| `TurNroId` | `TurNroId` | `int` NULL | FK lógica al turno |
| `F276NCpbte` | `F276NCpbte` | `char(15)` NULL | N° de comprobante |
| `LF276ID` | `LF276ID` | `int` NULL | FK a `LOTES227` |
| `F276Estad` | `F276Estad` | `char(1)` NULL | `A` anulado; se excluyen los anulados |
| `F276GenWeb` | `F276GenWeb` | `char(1)` NULL | `A` = generado por web; filtro del legacy |
| `F276FchMov` | `F276FchMov` | `datetime` NULL | fecha de movimiento (orden del detalle) |
| `F276VenRS` | `F276VenRS` | `char(35)` NULL | razón social del vendedor |
| `F276CuitVe` | `F276CuitVe` | `decimal` NULL | CUIT del vendedor |
| `F276TranRS` | `F276TranRS` | `char(35)` NULL | razón social del transportista |
| `F276CuitTr` | `F276CuitTr` | `decimal` NULL | CUIT del transportista |
| `F276MImpFl` | `F276MImpFl` | `money` NULL | monto IB del formulario |
| `F276ImpuFl` | `F276ImpuFl` | `money` NULL | impuesto de fletes |
| `F276AdicFl` | `F276AdicFl` | `money` NULL | adicional de fletes |
| `F276AdIBTo` | `F276AdIBTo` | `money` NULL | adicional IB total |
| `F276AdCMTo` | `F276AdCMTo` | `money` NULL | adicional CM total |
| `F276ActaTi` | `F276ActaTi` | `char(1)` NULL | tipo de acta (`F`/`P`/`T`/NULL) |

### 2.3 Tablas de apoyo (solo lectura)

- `GES_SUCU` (PK `SucNroId`,`SucTipo`): `SucDescr`.
- `PUEPPRIM` (PK `PueCod`): `PueDesc`, `PueIPRed`.
- `COMISION` (PK `PueCod`,`CPCId`): `CPCEstado`, `CPCCierre`, `CPCFchCie`,
  `CPCFchDes`, `CPCFchHas`, `CPCResId`, `CPCResNyA` (columna física inexistente → JOIN a
  `PERSONAS`).
- `LOTES227` (PK `LF276ID`): `LF276Estad`, `LF276FchHa`.
- `PERSONAS` (PK `PerId`): `PerNomyApe`, `PerCodUsu`.
- `SCRTY` (PK `UsuId`): `UsuNomYApe`, `UsuId` (para resolver el nombre del operador).
- `SNSINDEV` (PK `SNSinNum`): datos del SN del volante de pago (referencia, no detalle del
  resumen).

> **Nota de paridad:** los nombres físicos de `F2276` difieren de los nombres de atributo del
> legacy (`F276ImIBTot`/`F276ImCMTot` no existen en la tabla física). El mapeo exacto de los
> totales por columna se registra como **supuesto abierto SA-REP-01** (§6).

## 3. Requisitos (EARS)

### 3.1 Generación del reporte

- **R-REP-TUR-001:** El sistema SHALL generar un documento PDF "Resumen de Turnos" para el turno
  identificado por `(SucNroId, SucTipo, TurNroId)` cuando se invoque el endpoint REST o el botón
  de la vista.
- **R-REP-TUR-002:** El PDF SHALL incluir un **encabezado institucional** con el logo del ATP, el
  título `Resumen de Turnos`, la fecha y hora de generación, la versión del sistema
  (`BuscarVersionActual`) y los datos del turno: sucursal (`SucNroId` + `SucDescr`), tipo
  (`PUE`/`REC`), comisión (`TurCPCId`), fechas de apertura y cierre, estado (`A`→`Abierto`,
  `C`→`Cerrado`) y operador (`TurUsuario` + `UsuNomYApe`).
- **R-REP-TUR-003:** El PDF SHALL incluir un **detalle** de los formularios `F2276` del turno,
  ordenados por `F276FchMov` ascendente, con las columnas: N° Acta (`F276Id`), N° Comprobante
  (`F276NCpbte`), Vendedor (`F276VenRS`), CUIT Vendedor (`F276CuitVe`), Transportista
  (`F276TranRS`), CUIT Transportista (`F276CuitTr`), Tipo de Acta (decodificado:
  `F`→`Falta Doc. Flete`, `P`→`Falta Doc. Prod. Prim`, `T`→`Sin Documentación`), Fecha de
  Movimiento (`F276FchMov`), IB, CM, Adicional IB, Adicional CM, Adicional Fletes e Impuesto
  Fletes.
- **R-REP-TUR-004:** El PDF SHALL incluir una **fila de totales** con la suma de IB, CM,
  Adicional IB, Adicional CM, Adicional Fletes e Impuesto Fletes del detalle.
- **R-REP-TUR-005:** El detalle SHALL incluir únicamente los formularios del turno con
  `F276Estad <> 'A'` y `F276GenWeb = 'A'` (paridad con `ActaResumenTurno`), ordenados por
  `F276FchMov` (paridad con `defined by F276FchMov`).
- **R-REP-TUR-006:** El PDF SHALL incluir un **pie de página** con firma (línea + rótulo
  `Firma del Operador`), la fecha y hora de generación, la versión del sistema y la **numeración
  de página** `Pág. X de Y`.
- **R-REP-TUR-007:** Si el turno no tiene formularios que cumplan el filtro, el PDF SHALL
  generarse igualmente, mostrando el encabezado, el bloque de totales en cero y el mensaje
  `Sin formularios para el turno seleccionado` en el detalle.
- **R-REP-TUR-008:** El PDF SHALL usar el tamaño de página `A4` y orientación **horizontal**
  (landscape), paridad con los reportes legacy (`PL 76` / ancho 830 pt).

### 3.2 API REST

- **R-REP-TUR-009:** El sistema SHALL exponer `GET /api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen`
  que devuelva `200 OK` con `Content-Type: application/pdf` y el cuerpo binario del PDF.
- **R-REP-TUR-010:** La respuesta SHALL incluir `Content-Disposition: inline; filename="ResumenTurno_{sucNroId}_{sucTipo}_{turNroId}.pdf"`.
- **R-REP-TUR-011:** Si el turno no existe, el endpoint SHALL responder `404 Not Found` con el
  cuerpo JSON uniforme `{ "code": "TURNO_NO_ENCONTRADO", "message": "..." }`.
- **R-REP-TUR-012:** Si ocurre un error de generación o de BD, el endpoint SHALL responder
  `500 Internal Server Error` con el cuerpo JSON uniforme `{ "code": "ERROR_GENERACION_REPORTE",
  "message": "..." }` y **no** deberá exponer trazas al cliente.
- **R-REP-TUR-013:** El endpoint SHALL respetar la política de seguridad vigente del módulo de
  login: **sin CSRF** (descarte explícito para todo `/api/**`, D-LOG-17) y `permitAll` en este
  módulo (D-LOG-19), con la deuda documentada (SA-LOG-14).

### 3.3 Vista Thymeleaf

- **R-REP-TUR-014:** La vista `TurnosListado` SHALL mostrar en cada fila un botón/ícono
  `Resumen` (tooltip verbatim `Imprimir Resumen`, ícono `impresora`) que abra el PDF en una pestaña
  nueva (`target="_blank"`) enlazando al endpoint REST (R-REP-TUR-009).
- **R-REP-TUR-015:** El botón SHALL estar disponible para **todo** turno listado (abierto o
  cerrado), sin condición de estado (paridad con el legacy, donde `&Imprimir` se inicializa en el
  evento `Start`).
- **R-REP-TUR-016:** Si el endpoint responde con error, la vista SHALL mostrar un mensaje de error
  legible (`.atp-alert--error`) sin exponer trazas.

### 3.4 Calidad (ISO/IEC 25030)

- **R-REP-TUR-017 (Adecuación funcional — completitud):** el PDF SHALL contener todos los campos
  listados en R-REP-TUR-002 y R-REP-TUR-003 para cualquier turno válido.
- **R-REP-TUR-018 (Adecuación funcional — corrección):** los totales de R-REP-TUR-004 SHALL
  coincidir exactamente con la suma de las filas del detalle (sin redondeo intermedio).
- **R-REP-TUR-019 (Usabilidad — operabilidad):** la generación SHALL completarse en menos de 5
  segundos para turnos con hasta 500 formularios.
- **R-REP-TUR-020 (Mantenibilidad — modularidad):** el encabezado, el pie y la firma SHALL
  reutilizar un componente base compartido del núcleo (`atp-nucleo-compartido`), de modo que un
  cambio de logo o texto institucional se propague a todos los reportes.

## 4. Decisiones

- **D-REP-001:** Se utiliza **JasperReports** para la generación del PDF (decisión previa del
  proyecto). Plantillas `.jrxml` versionadas en
  `src/main/resources/reports/` del módulo que las usa; el componente base de encabezado/pie/firma
  vive en `atp-nucleo-compartido/src/main/resources/reports/common/`.
- **D-REP-002:** El reporte se sirve como `application/pdf` desde un endpoint REST (no se escribe
  a disco). El nombre de archivo sigue el patrón `ResumenTurno_{sucNroId}_{sucTipo}_{turNroId}.pdf`.
- **D-REP-003:** Los datos se obtienen por el **servicio** (no por SQL embebido en el `.jrxml`),
  pasando una `JRBeanCollectionDataSource` con DTOs tipados. El `.jrxml` no contiene lógica de
  negocio.
- **D-REP-004:** Los `char` de longitud fija se normalizan con `trim()` en el servicio antes de
  poblar el reporte (paridad con D-TUR-004).
- **D-REP-005:** La numeración de página (`Pág. X de Y`) se resuelve con las variables nativas de
  JasperReports (`PAGE_NUMBER`, `REPORT_COUNT`).
- **D-REP-006:** El reporte se genera en orientación landscape A4, paridad con los reportes
  legacy.
- **D-REP-007:** El "Volante de Pago" (PDF) **permanece fuera de alcance** (D-TUR-005). Este spec
  cubre únicamente el "Resumen de Turnos".

## 5. Fuera de alcance

- Impresión del "Volante de Pago" (PDF) — sigue fuera de alcance.
- Reportes de actas (`ActaResumenTurno`, `RPT_ActaTraslado`) — módulo aparte.
- Reporte "Turnos por Comisiones" (`RProPri_TurXCom`) — módulo aparte.
- Exportación a otros formatos (Excel, HTML) — sólo PDF.
- Firma digital o timestamping del PDF.
- Almacenamiento persistente del PDF generado (no se guarda en disco ni en BD).

## 6. Supuestos abiertos

- **SA-REP-01:** El mapeo exacto de los totales por columna de `F2276` (IB, CM, adicional,
  fletes) no está documentado en la KB para un reporte de "Resumen de Turnos" porque **no existe
  un reporte legacy con ese nombre**: en `TurnosListado` el link `&Imprimir.Link =
  ActaResumenTurno.Link(...)` está **comentado**. Se asume el mapeo
  `IB = F276MImpFl`, `CM = (no existe columna directa → 0 / pendiente)`,
  `AdicIB = F276AdIBTo`, `AdicCM = F276AdCMTo`, `AdicFletes = F276AdicFl`,
  `ImpuFletes = F276ImpuFl`. **Pregunta exacta a resolver:** ¿cuál es la fuente del total de CM
  por formulario (columna `F276ImCMTot` no existe en `F2276`)? ¿Se calcula desde `LOTES227.LF276ICM`
  o desde otra tabla?
- **SA-REP-02:** El título exacto del reporte (`Resumen de Turnos` vs `Planilla Resumen Diario de
  Turnos`) no está evidenciado. Se asume `Resumen de Turnos`. **Pregunta:** ¿cuál es el título
  impreso en el encabezado del reporte legacy que se quiere replicar?
- **SA-REP-03:** El logo institucional a incluir en el encabezado (imagen `gxReportImage1` del
  legacy) no está disponible como recurso versionado. **Pregunta:** ¿se cuenta con el archivo de
  imagen del encabezado (PNG/SVG) para versionarlo en `static/img/`?
- **SA-REP-04:** El texto de la firma (`Firma del Operador` vs otro rótulo) y la presencia de una
  segunda firma (supervisor) no están evidenciados. **Pregunta:** ¿cuántas firmas lleva el reporte
  y con qué rótulo?
- **SA-REP-05:** La habilitación del botón `Resumen` (siempre vs sólo turnos cerrados) no está
  evidenciada porque el link está comentado en el legacy. Se asume **siempre habilitado**
  (R-REP-TUR-015). **Pregunta:** ¿el botón debe estar deshabilitado para turnos abiertos?
- **SA-REP-06:** La política de seguridad del endpoint (permitAll vs autenticado) sigue la
  decisión vigente D-LOG-19 (`permitAll` en `/api/**`). **Pregunta:** ¿el reporte debe exigir
  sesión autenticada antes de la implementación del endurecimiento T-135?
- **SA-REP-07:** La fuente de la "versión del sistema" (`BuscarVersionActual`) no está portada al
  backend nuevo. Se asume un valor estático configurable (`app.version`) hasta que se porte el
  procedimiento. **Pregunta:** ¿existe un equivalente en el backend nuevo o se mantiene el valor
  del legacy?
- **SA-REP-08:** La relación `F2276.TurNroId` → `PPRIM_TU.TurNroId` no tiene FK física; se
  filtra por `(SucNroId, SucTipo, TurNroId)`. Confirmado por el analista de datos.