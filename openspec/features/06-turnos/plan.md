# Plan — Módulo de Turnos `06-turnos`

## 1. Fases

### Fase A — Dominio y persistencia (backend)
- `Turno` (entidad JPA) → `@Table(name = "PPRIM_TU")` con PK compuesta (`@EmbeddedId`).
- `TurnoId` (embeddable) con `SucNroId`, `SucTipo`, `TurNroId`.
- `TurnoRepositorio` (Spring Data): listar por sucursal+tipo (orden por PK), con paginación;
  `maxTurnoNroId(sucNroId, sucTipo)`; lectura por PK.
- `Comision` (lectura) → `@Table(name = "COMISION")` con PK compuesta; `estadoComision(pueCod, cpcId)`.
- Mapper DTO ↔ entidad, con `trim()` de los `char` fijos.

### Fase B — Servicios de negocio
- `ServicioTurnos`:
  - `listar(contexto)` → grilla del listado.
  - `nuevoTurno(contexto)` → valida comisión (sólo PUE), calcula `TurNroId` (max+1), inserta con
    sentinelas, actualiza `Context.Turno`.
  - `cerrarTurno(contexto, turno)` → orden de comprobaciones R-TUR-012, con
    `ServicioCierreTurno` (escritura) y `ServicioEstadoDeForm` (detección de formularios
    abiertos).
  - `ServicioGeneradorSn` (sólo REC) → totaliza F2276, inserta `SNSINDEV`, actualiza
    `PPRIM_TU.SNSinNum`; transacción con rollback.
- Los mensajes verbatim se centralizan en una clase de mensajes (no literales en las vistas).

### Fase C — Presentación (Thymeleaf)
- `templates/nucleo/turnos.html`: grilla con rótulos verbatim, acciones `Nuevo Turno` y
  `Cerrar Turno` (POST con token CSRF), estado vacío y de error.
- `TurnoMvcController`: `GET /turnos`, `POST /turnos/nuevo`, `POST /turnos/{...}/cerrar`.
- Layout base (header + nav + footer) y confirmación JS puntual (`confirmaciones.js`).

### Fase D — Integración con el menú
- `ServicioAutorizacion`: el ítem `Turnos` ya enlaza a `/turnos` (PUE y REC).

### Fase E — Verificación
- Tests unitarios de servicio (alta, cierre, bloqueos, mensajes).
- Tests MVC (render, binding, redirección, error, CSRF).
- Tests de integración contra `Rentas` (`*IT`, gated con `ATP_DB_IT=true`).
- QA: paridad de BD, tokens, accesibilidad.

## 2. Dependencias
- `03-login` (sesión y `Context`) — implementado.
- `05-ui-moderno` (layout base y menú) — implementado.
- Regla de paridad: `ddl-auto=none`, sin DDL, sin migraciones.

## 3. Riesgos
- `char` de longitud fija: comparación y `LIKE` con padding (mitigación: `trim()` en el servicio).
- Sentinela `1753-01-01` en columnas `NOT NULL` (mitigación: constante `FECHA_VACIA`).
- Rollback del cierre en REC cuando falla el SN (mitigación: `@Transactional` + excepción).
- Volumen de la tabla (54 768 filas): el listado filtra por sucursal y tipo.
