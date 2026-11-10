# Tareas — Módulo 10: Popup de Selección Manual de Localidades

> **Módulo**: `10-popup-localidades`
> **Spec**: `openspec/features/10-popup-localidades/spec.md`
> **Plan**: `openspec/features/10-popup-localidades/plan.md`
> **Estado**: Pendiente (ninguna tarea marcada como completa).

---

## Fase 0 — Verificación y preparación

- [x] **T-001** Verificar que `LocalidadRepository` expone los métodos necesarios (`findAllConProvincia`, `findByProvCodConProvincia`, `findByIdConProvincia`) y que no requieren modificación.
  - *Hecho cuando*: revisión del código confirma que los métodos existen y son suficientes para los filtros del legacy.
  - *Dependencias*: ninguna.

- [x] **T-002** Verificar la estructura física de `LOCALIDA` y `PROVINCI` contra `INFORMATION_SCHEMA` (ya realizado en spec §3.3). Documentar en el plan.
  - *Hecho cuando*: el plan refleja las 6 columnas de `LOCALIDA` y las 3 de `PROVINCI` con sus tipos.
  - *Dependencias*: ninguna.

## Fase 1 — Controlador MVC

- [x] **T-003** Crear `LocalidadesController` en `puestos.web.mvc` con `@GetMapping("/localidades")` que:
  - Verifica sesión autenticada (redirige a login si no).
  - Lee los query params de filtro (locCod, locNom, provNombre, locCodPost) y paginación (page, size default 5).
  - Aplica los filtros del legacy (LocCod >=, LocNom LIKE, ProvNombre/LocNomProv LIKE, LocCodPost >=).
  - Pagina con `Pageable` (sort LocCod asc).
  - Renderiza `puestos/localidades-seleccion`.
  - *Hecho cuando*: `GET /localidades` devuelve 200 con la plantilla y los atributos del modelo (`localidades`, `totalPages`, `number`, `totalElements`).
  - *Dependencias*: T-001.

- [x] **T-004** Agregar validación de binding de los filtros (numéricos para locCod y locCodPost) con manejo de error (re-render con mensajes por campo).
  - *Hecho cuando*: un filtro con formato inválido re-renderiza la página con error de binding y no lanza 500.
  - *Dependencias*: T-003.

## Fase 2 — Vista Thymeleaf del popup

- [x] **T-005** Crear `templates/puestos/localidades-seleccion.html` que:
  - Extiende `layout/base` (fragmentos `cabecera`, `sesion`, `navegacion`, `pie`).
  - Renderiza Fieldset `Filters` con 4 campos de filtro.
  - Renderiza Fieldset `Lista de Selección` con `Grid1` (6 columnas, paridad con legacy).
  - Renderiza paginación (totalPages, number, totalElements) con enlaces que preservan filtros.
  - Renderiza botón `Cancelar`.
  - *Hecho cuando*: la plantilla renderiza sin errores y pasa `@WebMvcTest` de columnas y paginación.
  - *Dependencias*: T-003.

- [x] **T-006** Implementar las columnas de la grilla con paridad exacta:
  - Columna 1: selector (ícono `selectRow`, ReturnOnClick).
  - Columna 2: `Cod. Loc.` (LocCod).
  - Columna 3: Nombre (LocNom, ReturnOnClick, título contextual).
  - Columna 4: `LocNomProv` (oculta, `ColVisible=False`).
  - Columna 5: `Provincia` (ProvNombre).
  - Columna 6: `Cod. Postal` (LocCodPost).
  - *Hecho cuando*: test de vista verifica los 6 encabezados y la columna 4 oculta.
  - *Dependencias*: T-005.

- [x] **T-007** Implementar los enlaces de selección (ReturnOnClick) que llaman a `window.opener.setLocalidad(locCod, locNom, provNombre)` y cierran la ventana.
  - *Hecho cuando*: la plantilla incluye el atributo `onclick` (o data-attribute) con la llamada JS.
  - *Dependencias*: T-005, T-010.

## Fase 3 — Integración con el formulario de alta (09)

- [x] **T-008** Modificar `ActaFormularioController` para exponer en el modelo un flag por parte (`locCodVenDerivado`, `locCodDesDerivado`, `locCodTranDerivado`) indicando si la localidad fue derivada (LocCod != null).
  - *Hecho cuando*: el modelo incluye los 3 flags y `@WebMvcTest` los verifica.
  - *Dependencias*: ninguna (depende de 09 ya implementado).

- [x] **T-009** Modificar `templates/puestos/acta-formulario.html` para agregar el ícono ⇧ junto al campo `Localidad` de cada fieldset de parte (Vendedor, Destinatario, Transportista), con:
  - `data-popup-enabled` seteado server-side según el flag.
  - Clase `localidad-popup-trigger` + `is-enabled`/`is-disabled`.
  - `aria-disabled` y `disabled` cuando no habilitado.
  - *Hecho cuando*: test de vista verifica la presencia del ícono en los 3 fieldsets y su estado según el flag.
  - *Dependencias*: T-008.

## Fase 4 — JavaScript puntual

- [x] **T-010** Crear `static/js/puestos/localidades-popup.js` (vanilla) que:
  - Define `window.setLocalidad(locCod, locNom, provNombre)` que actualiza los campos del formulario padre (`#locCodVen`, `#locNomVen`, `#provVenNom`, etc.) y cierra la ventana.
  - Agrega listener en `.localidad-popup-trigger[data-popup-enabled="true"]` que abre `window.open('/localidades', 'localidadesPopup', 'width=800,height=600')`.
  - *Hecho cuando*: el archivo existe, no usa frameworks ni CDN, y pasa la revisión de G6 (`grep` sin `bootstrap`, `tailwind`, `cdn.`, `https://`, `unpkg`, `jsdelivr`, `node_modules`).
  - *Dependencias*: ninguna.

- [x] **T-011** Incluir el script en `acta-formulario.html` y en `localidades-seleccion.html` mediante `<script src="/js/puestos/localidades-popup.js" defer>`.
  - *Hecho cuando*: ambas plantillas referencian el script y el test de vista verifica su presencia.
  - *Dependencias*: T-010.

## Fase 5 — Seguridad

- [x] **T-012** Verificar que `/localidades` requiere sesión autenticada (Spring Security config existente).
  - *Hecho cuando*: test MockMvc sin sesión comprueba `302` al login.
  - *Dependencias*: T-003.

- [x] **T-013** Verificar CSRF en cualquier POST del popup (si aplica).
  - *Hecho cuando*: test MockMvc con CSRF inválido comprueba `403`.
  - *Dependencias*: T-003.

## Fase 6 — Tests

- [x] **T-014** Test `@WebMvcTest` para `LocalidadesController`:
  - `GET /localidades` → 200 + vista `puestos/localidades-seleccion`.
  - Verificación de columnas de la grilla.
  - Verificación de paginación (totalPages, number, totalElements).
  - *Hecho cuando*: el test pasa en `./mvnw test`.
  - *Dependencias*: T-003, T-005.

- [x] **T-015** Test `@WebMvcTest` para los filtros del popup (parametrizado, un caso por filtro).
  - *Hecho cuando*: el test pasa en `./mvnw test`.
  - *Dependencias*: T-004.

- [x] **T-016** Test `@WebMvcTest` para el formulario de alta con popup:
  - Verificación de la presencia del ícono ⇧ en los 3 fieldsets.
  - Verificación del estado habilitado/deshabilitado según el flag.
  - *Hecho cuando*: el test pasa en `./mvnw test`.
  - *Dependencias*: T-008, T-009.

- [x] **T-017** IT `PopupLocalidadesIT` contra `Rentas`:
  - Verifica que `/localidades` renderiza las 30 filas con FK huérfana (`ProvCod = 0`) con provincia vacía.
  - Verifica que el conteo total coincide con `SELECT COUNT(*) FROM LOCALIDA`.
  - *Hecho cuando*: el test pasa con `ATP_DB_IT=true`.
  - *Dependencias*: T-003, T-005.

- [x] **T-018** Prueba de navegador (manual) para el flujo completo:
  - Ingresar CUIT inexistente → ícono ⇧ habilitado → click → popup se abre → seleccionar fila → campos se actualizan → popup se cierra.
  - Ingresar CUIT existente → ícono ⇧ deshabilitado.
  - Cerrar popup sin seleccionar → campos sin cambios.
  - *Hecho cuando*: evidencia (captura) guardada en `docs/` o reportada.
  - *Dependencias*: T-003, T-009, T-010.

## Fase 7 — Verificación final

- [x] **T-019** Ejecutar `./mvnw clean test` y verificar que todas las suites pasan (no regresión).
  - *Hecho cuando*: build en verde.
  - *Dependencias*: todas las anteriores.

- [x] **T-020** Verificar que no se modificaron `Localidad`, `Provincia`, `LocalidadRepository` ni la colección Postman.
  - *Hecho cuando*: `git diff` confirma ausencia de cambios en esos archivos y en `docs/postman/`.
  - *Dependencias*: T-019.

---

## Resumen

- **Total de tareas**: 20 (T-001 a T-020).
- **Tareas de implementación**: T-003 a T-011.
- **Tareas de test**: T-014 a T-018.
- **Tareas de verificación**: T-001, T-002, T-012, T-013, T-019, T-020.
- **Dependencia crítica**: T-003 (controlador) bloquea T-005 (vista) y T-014 (test).