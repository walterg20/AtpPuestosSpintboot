# Plan — Reporte "Resumen de Turnos" `07-reportes-turnos`

Acompaña a `spec.md` (misma puerta G-SPEC). Documento de arquitectura; no contiene tareas (ver
`tasks.md`). Paridad de BD: `ddl-auto=none`, sin DDL, sin migraciones.

---

## 1. Contexto y goals

### 1.1 Qué produce este módulo

Un endpoint REST `GET /api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen` que devuelve un PDF
"Resumen de Turnos" (JasperReports), más la integración del botón `Resumen` en la vista
`TurnosListado`. El reporte consolida datos del turno y el detalle de `F2276`.

### 1.2 Dónde vive el código

| Artefacto | Ubicación | Razón |
|---|---|---|
| Entidades JPA (sólo lectura, ya existen) | `atp-nucleo-compartido/.../domain/` (`Turno`, `Formulario2276`, `Sucursal`, `Usuario`) | Paridad de BD; reutiliza el núcleo. |
| Repositorios (sólo SELECT) | `atp-nucleo-compartido/.../repository/` | Idem. |
| Servicio de reporte | `atp-puestos-camineros/.../service/ReporteTurnoService.java` | Dominio de turnos vive en el módulo desplegable. |
| DTO del detalle | `atp-puestos-camineros/.../service/dto/ResumenTurnoFilaDto.java` (record) | Fuente de la `JRBeanCollectionDataSource`. |
| Controller REST | `atp-puestos-camineros/.../web/rest/ReporteTurnoController.java` | Canal `/api/**` (D-04 de `00-alcance`). |
| Plantilla Jasper | `atp-puestos-camineros/src/main/resources/reports/resumen-turno.jrxml` | Plantilla específica del reporte. |
| Componente base compartido | `atp-nucleo-compartido/src/main/resources/reports/common/encabezado-pie.jrxml` (subreport) | Reutilizable por otros reportes (R-REP-TUR-020). |
| Integración de vista | `templates/nucleo/turnos.html` (botón `Resumen`) | Ya existe; se agrega el enlace. |
| JS | `static/js/confirmaciones.js` (sin cambios) | El botón abre en pestaña nueva; sin JS nuevo. |

### 1.3 Goals

| # | Goal |
|---|---|
| G-1 | Generar un PDF con paridad visual/funcional respecto del legacy (encabezado, detalle, totales, pie, firma, paginación). |
| G-2 | Reutilizar la base existente sin DDL. |
| G-3 | Mantener la regla de dependencia `web → service → repository → domain`. |
| G-4 | Componente base de encabezado/pie/firma reutilizable en el núcleo. |

---

## 2. Dependencias

- `03-login` (sesión y `Context`) — implementado.
- `06-turnos` (entidades, repositorios, vista `TurnosListado`) — implementado.
- `05-ui-moderno` (layout base) — implementado.
- Nuevo: dependencia Maven `net.sf.jasperreports:jasperreports` (versión a fijar en el parent POM).
- Regla de paridad: `ddl-auto=none`, sin DDL, sin migraciones.

---

## 3. Tabla de paridad legacy → JPA

No se crean tablas nuevas. El reporte reutiliza las ya mapeadas. Columnas relevantes del detalle:

| Tabla | Columna física | Tipo físico | Mapeo JPA (entidad existente) | Notas |
|---|---|---|---|---|
| `PPRIM_TU` | `SucNroId` | `smallint` NOT NULL | `Turno.id.sucNroId` (Short) | PK 1 |
| `PPRIM_TU` | `SucTipo` | `char(3)` NOT NULL | `Turno.id.sucTipo` (String, trim) | PK 2 |
| `PPRIM_TU` | `TurNroId` | `int` NOT NULL | `Turno.id.turNroId` (Integer) | PK 3 |
| `PPRIM_TU` | `TurUsuario` | `char(10)` NOT NULL | `Turno.turUsuario` (String, trim) | operador |
| `PPRIM_TU` | `TurFchApe` | `datetime` NOT NULL | `Turno.turFchApe` (LocalDateTime) | |
| `PPRIM_TU` | `TurFchCie` | `datetime` NOT NULL | `Turno.turFchCie` (LocalDateTime) | sentinela `1753-01-01` |
| `PPRIM_TU` | `TurEstado` | `char(1)` NOT NULL | `Turno.turEstado` (String, trim) | `A`/`C` |
| `PPRIM_TU` | `TurCPCId` | `smallint` NULL | `Turno.turCpcId` (Short) | comisión |
| `F2276` | `F276Id` | `int` NOT NULL | `Formulario2276.id` (Integer) | N° acta |
| `F2276` | `F276NCpbte` | `char(15)` NULL | `Formulario2276.f276NCpbte` (String, trim) | |
| `F2276` | `F276VenRS` | `char(35)` NULL | `Formulario2276.f276VenRS` (String, trim) | |
| `F2276` | `F276CuitVe` | `decimal` NULL | `Formulario2276.f276CuitVe` (BigDecimal) | |
| `F2276` | `F276TranRS` | `char(35)` NULL | `Formulario2276.f276TranRS` (String, trim) | |
| `F2276` | `F276CuitTr` | `decimal` NULL | `Formulario2276.f276CuitTr` (BigDecimal) | |
| `F2276` | `F276MImpFl` | `money` NULL | `Formulario2276.f276MImpFl` (BigDecimal) | IB |
| `F2276` | `F276ImpuFl` | `money` NULL | `Formulario2276.f276ImpuFl` (BigDecimal) | impuesto fletes |
| `F2276` | `F276AdicFl` | `money` NULL | `Formulario2276.f276AdicFl` (BigDecimal) | adicional fletes |
| `F2276` | `F276AdIBTo` | `money` NULL | `Formulario2276.f276AdIBTo` (BigDecimal) | adicional IB |
| `F2276` | `F276AdCMTo` | `money` NULL | `Formulario2276.f276AdCMTo` (BigDecimal) | adicional CM |
| `F2276` | `F276ActaTi` | `char(1)` NULL | `Formulario2276.f276ActaTi` (String, trim) | tipo de acta |
| `F2276` | `F276FchMov` | `datetime` NULL | `Formulario2276.f276FchMov` (LocalDateTime) | orden |
| `F2276` | `F276Estad` | `char(1)` NULL | `Formulario2276.f276Estad` (String, trim) | filtro `<> 'A'` |
| `F2276` | `F276GenWeb` | `char(1)` NULL | `Formulario2276.f276GenWeb` (String, trim) | filtro `= 'A'` |
| `GES_SUCU` | `SucDescr` | `varchar(40)` NOT NULL | `Sucursal.sucDescr` (String, trim) | |
| `SCRTY` | `UsuNomYApe` | `char(35)` NULL | `Usuario.usuNomYApe` (String, trim) | nombre del operador |

> **Columnas huérfanas:** no se agregan columnas nuevas. Las columnas huérfanas de `F2276` que el
> reporte no usa se **mapean** (ya existen en las entidades del módulo 06) y no se rediseñan.

---

## 4. Contratos

### 4.1 REST — `GET /api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen`

| Aspecto | Detalle |
|---|---|
| Método | `GET` |
| Ruta | `/api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen` |
| Parámetros de ruta | `sucNroId` (short), `sucTipo` (String, 3), `turNroId` (int) |
| Respuesta exitosa | `200 OK`, `Content-Type: application/pdf`, cuerpo binario |
| Headers | `Content-Disposition: inline; filename="ResumenTurno_{sucNroId}_{sucTipo}_{turNroId}.pdf"` |
| 404 | `{ "code": "TURNO_NO_ENCONTRADO", "message": "..." }` |
| 500 | `{ "code": "ERROR_GENERACION_REPORTE", "message": "..." }` |
| Seguridad | Sin CSRF (D-LOG-17), `permitAll` (D-LOG-19) en este módulo |

> **G-POSTMAN:** el endpoint nuevo obliga a agregar una entrada en
> `docs/postman/ATPPuestos-API.postman_collection.json` (T-012).

### 4.2 Thymeleaf — vista `TurnosListado` (modificación mínima)

| Elemento | Cambio |
|---|---|
| Botón `Resumen` por fila | Enlace a `/api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen`, `target="_blank"`, ícono `impresora`, tooltip `Imprimir Resumen`. |
| Estado de error | Si el endpoint responde con error, mostrar `.atp-alert--error` (R-REP-TUR-016). |

No se agrega ninguna ruta MVC nueva; el botón consume el endpoint REST directamente.

---

## 5. Plantilla JasperReports

### 5.1 Estructura de bandas (`resumen-turno.jrxml`)

| Banda | Contenido |
|---|---|
| `Title` | Título `Resumen de Turnos` (18 pt, bold). |
| `Page Header` | Subreport `encabezado-pie.jrxml`: logo, fecha/hora de generación, versión. |
| `Column Header` | Encabezados de las columnas del detalle (IB, CM, etc.). |
| `Detail` | Una fila por `F2276` (ver R-REP-TUR-003). |
| `Summary` | Fila de totales (R-REP-TUR-004) + firma (línea + rótulo). |
| `Page Footer` | `Pág. X de Y`, versión, fecha de generación. |

### 5.2 Parámetros del reporte

| Parámetro | Tipo Java | Origen |
|---|---|---|
| `P_TITULO` | `String` | constante `Resumen de Turnos` |
| `P_FECHA_GENERACION` | `LocalDateTime` | `LocalDateTime.now()` |
| `P_VERSION` | `String` | `app.version` (hasta portar `BuscarVersionActual`) |
| `P_SUC_NRO_ID` | `Short` | ruta |
| `P_SUC_TIPO` | `String` | ruta |
| `P_TUR_NRO_ID` | `Integer` | ruta |
| `P_SUC_DESCR` | `String` | `GES_SUCU.SucDescr` |
| `P_TUR_USUARIO` | `String` | `PPRIM_TU.TurUsuario` (trim) |
| `P_TUR_USUARIO_NOMBRE` | `String` | `SCRTY.UsuNomYApe` (trim) |
| `P_TUR_FCH_APE` | `LocalDateTime` | `PPRIM_TU.TurFchApe` |
| `P_TUR_FCH_CIE` | `LocalDateTime` | `PPRIM_TU.TurFchCie` |
| `P_TUR_ESTADO` | `String` | decodificado |
| `P_TUR_CPC_ID` | `Short` | `PPRIM_TU.TurCPCId` |
| `P_MENSAJE_VACIO` | `String` | `Sin formularios para el turno seleccionado` |

### 5.3 Fuente de datos

`JRBeanCollectionDataSource` sobre `List<ResumenTurnoFilaDto>` producida por
`ReporteTurnoService.cargarDetalle(...)`. El servicio consulta `Formulario2276Repository` por
`(sucNroId, sucTipo, turNroId)` con el filtro de R-REP-TUR-005 y orden por `f276FchMov`.

### 5.4 Componente base compartido (`atp-nucleo-compartido`)

Subreport `encabezado-pie.jrxml` con parámetros `P_LOGO`, `P_FECHA_GENERACION`, `P_VERSION`,
`P_TITULO`. Reutilizable por futuros reportes. El logo se carga desde
`classpath:static/img/logo-atp.png` (SA-REP-03).

---

## 6. Riesgos

| Riesgo | Mitigación |
|---|---|
| Mapeo de totales de CM no evidenciado (SA-REP-01) | Implementar con `F276MImpFl` para IB y dejar CM en 0 con TODO, documentado. |
| Reporte inexistente en legacy ⇒ sin referencia visual | Basar en `ActaResumenTurno` (cabecera) y `RProPri_TurXCom` (detalle + totales). |
| Rendimiento en turnos con muchos formularios | Cargar el detalle en una sola consulta con `@EntityGraph`/fetch join; medir (R-REP-TUR-019). |
| Tamaño del PDF en memoria | Usar `JasperFillManager.fillReport` con `JRBeanCollectionDataSource` y exportar a `ByteArrayOutputStream`. |
| Dependencia nueva (JasperReports) | Fijar versión en el parent POM; verificar compatibilidad con Java 17 / Spring Boot 3.5. |
| Seguridad del endpoint (permitAll) | Seguir D-LOG-19; endurecer en T-135 del módulo 03. |

---

## 7. Decisiones de arquitectura adicionales

- **D-REP-008:** El servicio de reporte vive en `atp-puestos-camineros` (no en el núcleo) porque
  el dominio de turnos y el reporte son específicos del módulo desplegable. El núcleo sólo aporta
  el componente base de encabezado/pie.
- **D-REP-009:** No se crea una ruta MVC nueva; el botón consume el endpoint REST directamente
  (`target="_blank"`), paridad con el legacy (`LinkTarget = '_blank'`).
- **D-REP-010:** El PDF no se persiste ni se escribe a disco; se devuelve en memoria.