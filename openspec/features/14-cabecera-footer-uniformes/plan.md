# Plan — Cabecera y footer institucionales uniformes

## Fase A — Extraer fragmentos (enablement)
1. En `base-stitch.html`, agregar `th:fragment="topbar"` con el markup del `TopBarInfo` de login.
2. Agregar `th:fragment="hero"` con el markup del `MainHeroBanner` (camión + título + distintivo).
3. Agregar `th:fragment="footer-institucional"` con el markup del `InstitutionalFooter`.
4. Mantener los fragmentos existentes (`cabecera`, `sesion`, `navegacion`, `pie`) sin cambios.

## Fase B — Refactorizar login.html
5. Reemplazar los bloques inline por `th:replace="~{layout/base-stitch :: topbar}"`, `:: hero`, `:: footer-institucional"`.
6. Verificar render idéntico (diff visual).

## Fase C — Migrar vistas Stitch (puestos/*)
7. Para cada vista en `puestos/` (turnos-abiertos, actas-lista, acta-formulario, comisiones, comision-detalle, control-cargas, localidades-seleccion):
   - head → `base-stitch :: head(...)`
   - body `stitch-body` + skip link
   - topbar, hero, sesion (si auth), navegacion (si auth)
   - main `stitch-main stitch-bg-grid` con contenido existente
   - footer-institucional
8. Adaptar clases de contenedor al sistema stitch; mantener `th:object`/`th:field`/bindings.

## Fase D — Migrar vistas del layout viejo (núcleo)
9. `productos.html`: migrar de `layout/base` a `base-stitch` con chrome institucional; convertir clases `atp-*` → `stitch-*` para el chrome (contenido puede mantener `atp-*` si hay CSS puente).
10. `turnos.html`: idem.

## Fase E — Limpieza y verificación
11. Verificar que no queden referencias a `layout/base :: ...` en vistas migradas.
12. Correr `./mvnw clean test` (unitarios + @WebMvcTest).
13. Smoke manual de todas las vistas.

## Rutas afectadas
- `atp-nucleo-compartido/src/main/resources/templates/layout/base-stitch.html`
- `atp-nucleo-compartido/src/main/resources/templates/nucleo/login.html`
- `atp-nucleo-compartido/src/main/resources/templates/nucleo/productos.html`
- `atp-nucleo-compartido/src/main/resources/templates/nucleo/turnos.html` (si existe)
- `atp-puestos-camineros/src/main/resources/templates/puestos/*.html`

## Dependencias entre fases
A → B → C → D → E (secuencial; cada fase depende de la anterior para el chrome).