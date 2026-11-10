# Tasks — Cabecera y footer institucionales uniformes

## Fase A — Extraer fragmentos
- [x] A1: Agregar `th:fragment="topbar"` a `base-stitch.html` (markup TopBarInfo de login). — Evidencia: base-stitch.html línea 130.
- [x] A2: Agregar `th:fragment="hero"` a `base-stitch.html` (markup MainHeroBanner). — Evidencia: base-stitch.html línea 157.
- [x] A3: Agregar `th:fragment="footer-institucional"` a `base-stitch.html` (markup InstitutionalFooter). — Evidencia: base-stitch.html línea 184.

## Fase B — Refactorizar login.html
- [x] B1: Reemplazar bloques inline por `th:replace` de los 3 fragmentos. — Evidencia: login.html refactorizado.
- [x] B2: Verificar render idéntico (sin regresión visual). — Evidencia: AuthMvcLoginTest + PantallasHtmlSinSesionTest verdes.

## Fase C — Migrar vistas Stitch (puestos/*)
- [x] C1: `turnos-abiertos.html` — migrada (topbar+hero+sesion+nav+footer-institucional).
- [x] C2: `actas-lista.html` — migrada.
- [x] C3: `acta-formulario.html` — migrada.
- [x] C4: `comisiones.html` — migrada.
- [x] C5: `comision-detalle.html` — migrada.
- [x] C6: `control-cargas.html` — migrada.
- [x] C7: `localidades-seleccion.html` — migrada.
- [ ] C8: fragmentos `puestos/fragments/*` — PENDIENTE: no migrados (fuera del alcance explícito; quedan con layout base como fallback). Motivo: implementador-vistas los dejó fuera; requiere spec de alcance si se desea migrar.

## Fase D — Migrar vistas del layout viejo (núcleo)
- [x] D1: `productos.html` — migrada de layout/base a base-stitch con chrome institucional.
- [x] D2: `turnos.html` (núcleo) — migrada.

## Fase E — Limpieza y verificación
- [x] E1: Buscar referencias residuales a `layout/base ::` en vistas migradas. — Evidencia: grep sin resultados.
- [x] E2: `./mvnw clean test` verde (unitarios + @WebMvcTest). — Evidencia: BUILD SUCCESS, 609 tests (408 núcleo + 201 puestos), 0 fallos. Log: C:\Users\walterg20\AppData\Local\Temp\opencode\build.log.
- [ ] E3: Smoke manual de todas las vistas. — PENDIENTE: requiere ejecución manual del desplegador-local y verificación visual por el usuario.