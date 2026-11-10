# Tasks — Módulo de Comisiones `13-comisiones`

## Fase A — Dominio y persistencia (reutilización del núcleo)

- [x] T-COM-A1: Verificar que `Comision`, `ComisionIntegrante`, `ComisionId`, `ComisionIntegranteId`
      y `Persona` existen en `atp-nucleo-compartido` y están mapeados a `COMISION`/`COMISIO1`/
      `PERSONAS` (ya existen; sólo verificar, no recrear). **Criterio de aceptación:** compilación
      verde y `ComisionRepositoryIT` existente pasa. **Evidencia:** salida de `./mvnw -pl
      atp-nucleo-compartido test`.
- [x] T-COM-A2: Agregar a `Comision` los campos huérfanos `cpcFeLog`, `cpcUsuLog`, `cpcSnNum`,
      `cpcFePag` con `@Column` read-only (sin setters públicos) (SA-COM-01 RESUELTO: MAPEAR).
      **Criterio de aceptación:** arranque de la app sin errores de mapeo Hibernate. **Evidencia:** log de arranque.
- [x] T-COM-A3: Implementar consulta nativa `findPueDescByPueCod(pueCod)` en `ComisionRepository`
      para resolver `PueDesc` (SA-COM-03 RESUELTO: NO crear entity `Puesto`). **Criterio de
      aceptación:** el servicio puede resolver `PueDesc` por puesto. **Evidencia:** test unitario.
- [x] T-COM-A4: Agregar a `ComisionRepository` (si no existe) un método
      `findAllByPueCodAndCpcResId(pueCod, cpcResId, pageable)` o una query paginada con filtro
      legacy (D-COM-001). Verificar si los métodos existentes (`findComisionConResponsable`) cubren
      el listado; si no, agregar. **Criterio de aceptación:** consulta paginada compila y retorna
      `Page<Comision>`. **Evidencia:** test unitario del repositorio.

## Fase B — Servicios (en `atp-puestos-camineros`)

- [x] T-COM-B1: Crear `ServicioComisiones` en `atp-puestos-camineros` con `listar(pueCod, personaId,
      pageable)` aplicando el filtro legacy (D-COM-001) y orden `CPCId desc`. **Criterio de
      aceptación:** tests unitarios de filtro y orden pasan. **Evidencia:** `ServicioComisionesTest`.
- [x] T-COM-B2: Implementar `obtenerDetalle(pueCod, cpcId)` que devuelve `ComisionGeneralView` o
      `null` (R-COM-009, R-COM-013). **Criterio de aceptación:** test con comisión existente e
      inexistente. **Evidencia:** `ServicioComisionesTest`.
- [x] T-COM-B3: Implementar `listarIntegrantes(pueCod, cpcId)` ordenado por `PerNomyApe` trim
      ascendente (R-COM-014, R-COM-015). **Criterio de aceptación:** test de orden. **Evidencia:**
      `ServicioComisionesTest`.
- [x] T-COM-B4: Centralizar la decodificación de Estado en `DecodificadorEstadoComision` (o método
      estático) con regla R-COM-006/D-COM-003 (`'S'`→"Cerrado", `'N'`→"Abierto", `null`→"Abierto").
      **Criterio de aceptación:** tests unitarios de los 3 casos. **Evidencia:** `ServicioComisionesTest`.
- [x] T-COM-B5: Crear `MensajesComision` con el mensaje "No se encontraron registros" (verbatim del
      legacy). **Criterio de aceptación:** constante usada por el servicio y la vista. **Evidencia:**
      revisión de código.
- [x] T-COM-B6: Crear los view models `ComisionFilaView`, `ComisionGeneralView`, `IntegranteView` en
      `atp-puestos-camineros/.../puestos/web/mvc/comision/`. **Criterio de aceptación:** compilación
      verde. **Evidencia:** `./mvnw -pl atp-puestos-camineros compile`.

## Fase C — Presentación (Thymeleaf)

- [x] T-COM-C1: Crear `templates/puestos/comisiones.html` (listado) con 7 columnas verbatim, paginación
      First/Prev/Next/Last, estado vacío/error, layout base. **Criterio de aceptación:** renderizado
      en test MockMvc sin errores. **Evidencia:** `ComisionMvcControllerTest`.
- [x] T-COM-C2: Crear `templates/puestos/comision-detalle.html` con tabs General/Integrantes y los
      campos verbatim. **Criterio de aceptación:** renderizado en test MockMvc. **Evidencia:**
      `ComisionMvcControllerTest`.
- [x] T-COM-C3: Crear `ComisionMvcController` en `atp-puestos-camineros` con `GET /comisiones` y
      `GET /comisiones/{pueCod}/{cpcId}`. **Criterio de aceptación:** tests MockMvc de rutas y
      parámetros. **Evidencia:** `ComisionMvcControllerTest`.
- [x] T-COM-C4: Manejar el caso de comisión inexistente: mostrar "No se encontraron registros" y
      ocultar tabs (R-COM-013). **Criterio de aceptación:** test MockMvc con id inexistente.
      **Evidencia:** `ComisionMvcControllerTest`.

## Fase D — Integración con el menú

- [x] T-COM-D1: Modificar `ServicioAutorizacion` para que el ítem `Comisiones` enlace a
      `/comisiones` (agregar `RUTA_COMISIONES` y setear link en MENU_PUE y MENU_COM); actualizar
      `AuthMvcHomeTest`. **Criterio de aceptación:** test `AuthMvcHomeTest` pasa con el nuevo link.
      **Evidencia:** `./mvnw -pl atp-nucleo-compartido test`.

## Fase E — Verificación

- [x] T-COM-E1: Tests unitarios de `ServicioComisiones` (filtro legacy, orden, decodificación,
      JOIN responsable e integrantes, paginación). **Criterio de aceptación:** cobertura de los 3
      casos de Estado y de integrantes vacíos. **Evidencia:** reporte de tests.
- [x] T-COM-E2: Tests MockMvc de `ComisionMvcController` (render listado, render detalle, tabs,
      estado vacío, error de carga, paginación). **Criterio de aceptación:** todos verdes.
      **Evidencia:** `ComisionMvcControllerTest`.
- [x] T-COM-E3: Tests de integración contra `Rentas` (`*IT`, gated con `ATP_DB_IT=true`) para
      `ComisionRepository` (listado filtrado, orden, JOIN responsable/integrantes). **Criterio de
      aceptación:** datos reales se leen sin errores. **Evidencia:** salida de `./mvnw test
      "-Dtest=*IT"`.
- [x] T-COM-E4: Verificación QA: paridad de BD (columnas mapeadas), tokens (rótulos verbatim),
      accesibilidad (tabla con caption, th scope), G-POSTMAN no aplica (documentado). **Criterio de
      aceptación:** checklist completo. **Evidencia:** `docs/verificacion/13-evidencia/`.
- [x] T-COM-E5: `mem_save` de hallazgos relevantes (topic_key: spec/comisiones). **Criterio de
      aceptación:** observación guardada en Engram. **Evidencia:** id de observación.

## Supuestos abiertos (resueltos por el coordinador, 2026-11-05)

Ver `spec.md` §7 (SA-COM-01…06). Todos resueltos; las tareas condicionales (T-COM-A2, T-COM-A3)
se ejecutan según las decisiones documentadas.
