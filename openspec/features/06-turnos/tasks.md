# Tasks — Módulo de Turnos `06-turnos`

## Fase A — Dominio y persistencia (completada 2026-10-23)
- [x] T-TUR-A1: Entidad `Turno` (`PPRIM_TU`) + `TurnoId` (PK compuesta).
- [x] T-TUR-A2: `TurnoRepositorio` (listado por sucursal+tipo, paginado, orden por PK; max `TurNroId`).
- [x] T-TUR-A3: Entidad de lectura `Comision` (`COMISION`) + `estadoComision`.
- [x] T-TUR-A4: Mapper DTO ↔ entidad con `trim()` de `char` fijos.
- [x] T-TUR-A5: Constante `FECHA_VACIA` = `1753-01-01T00:00:00` (`shared/time/FechaVacia`).

## Fase B — Servicios (completada 2026-10-23)
- [x] T-TUR-B1: `ServicioTurnos.listar(contexto)` (R-TUR-001..003).
- [x] T-TUR-B2: `ServicioTurnos.nuevoTurno(contexto)` (R-TUR-006..011).
- [x] T-TUR-B3: `ServicioEstadoDeForm` (R-TUR-013) — `F2276` + `LOTES227`.
- [x] T-TUR-B4: `ServicioCierreTurno` (R-TUR-012) con orden de comprobaciones.
- [x] T-TUR-B5: `ServicioGeneradorSn` (R-TUR-014..017) — `F2276` + `SNSINDEV`, con rollback.
- [x] T-TUR-B6: Mensajes verbatim centralizados (`MensajesTurno`).

## Fase C — Presentación (completada 2026-10-23)
- [x] T-TUR-C1: `templates/nucleo/turnos.html` (grilla, rótulos verbatim, estados vacío/error).
- [x] T-TUR-C2: `TurnoMvcController` (`GET /turnos`, `POST /turnos/nuevo`, `POST /turnos/{nro}/cerrar`).
- [x] T-TUR-C3: Layout base + confirmación `¿Confirma que desea cerrar el Turno?` (`data-confirmar`).
- [x] T-TUR-C4: CSRF en las acciones de escritura.

## Fase D — Menú (completada 2026-10-23)
- [x] T-TUR-D1: Ítem `Turnos` enlaza a `/turnos` (PUE y REC).

## Fase E — Verificación
- [x] T-TUR-E1: Tests unitarios de servicio (alta, cierre, bloqueos, mensajes verbatim).
- [x] T-TUR-E2: Tests MVC (render, binding, redirección, error, CSRF).
- [x] T-TUR-E3: IT de lectura contra `Rentas` (77 tests verdes). IT de **escritura** del SN: pendiente
      (requiere entorno descartable; el `INSERT` en `SNSINDEV` no se probó contra la BD real).
- [ ] T-TUR-E4: Verificación QA (paridad de BD, tokens, accesibilidad, G-POSTMAN).
- [x] T-TUR-E5: `mem_save` de hallazgos relevantes.

## Hallazgos que corrigen la spec (2026-10-23)
1. `F276ImIBTot` / `F276ImCMTot` **no son columnas** de `F2276`: son atributos calculados
   (`sum(F276RImpIB)` / `sum(F276RImpCM)` sobre `F22761`, sólo si `F276GenWeb <> 'A'`).
2. `PPrim_NumTur` usa `Order (TurNroId)` ascendente ⇒ el legacy haría `min+1` (colisionaría con la
   PK). Se implementó **max+1** (D-TUR-001) — queda como supuesto abierto.
3. `TurFchApe`/`TurFchCie` usan `serverdate()` (medianoche) y `TurHraApe`/`TurHraCie` `servernow()`
   (con hora), según las Rules del legacy.
4. "Sin monto" = `TotalIB` **y** `TotalAd` en cero (ambos), no sólo `TotalIB`.
