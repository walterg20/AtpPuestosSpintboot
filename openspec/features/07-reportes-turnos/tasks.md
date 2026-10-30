# Tasks — Reporte "Resumen de Turnos" `07-reportes-turnos`

Reglas: tareas atómicas, verificables, con dependencia y criterio de "hecho" observable. No marcar
nada como completo; el coordinador lo hace con evidencia.

- [ ] **T-001** Agregar dependencia `net.sf.jasperreports:jasperreports` al parent POM (versión
  compatible con Java 17 / Spring Boot 3.5), en `<dependencyManagement>` y en el módulo
  `atp-puestos-camineros`. *Depende de:* ninguna. *Hecho cuando:* `./mvnw -q -o compile` resuelve
  la dependencia sin error.

- [ ] **T-002** Crear el componente base compartido en el núcleo:
  `atp-nucleo-compartido/src/main/resources/reports/common/encabezado-pie.jrxml` con parámetros
  `P_LOGO`, `P_FECHA_GENERACION`, `P_VERSION`, `P_TITULO`. *Depende de:* T-001. *Hecho cuando:* el
  subreport compila con `JasperCompileManager` en un test unitario.

- [ ] **T-003** Crear `ResumenTurnoFilaDto` (record) en
  `atp-puestos-camineros/.../service/dto/` con los campos del detalle (R-REP-TUR-003). *Depende
  de:* ninguna. *Hecho cuando:* compila y tiene test de construcción.

- [ ] **T-004** Extender `Formulario2276Repository` (si no existe) con método
  `findBySucNroIdAndSucTipoAndTurNroIdOrderByF276FchMovAsc(...)` que filtre `F276Estad <> 'A'` y
  `F276GenWeb = 'A'`. *Depende de:* entidades existentes (06-turnos). *Hecho cuando:* test de
  integración (gated `ATP_DB_IT=true`) devuelve filas ordenadas.

- [ ] **T-005** Implementar `ReporteTurnoService`:
  - `generarPdf(short sucNroId, String sucTipo, int turNroId): byte[]`
  - carga del turno (404 si no existe), carga de sucursal, usuario, detalle, totales.
  - normalización `trim()` de `char` fijos (D-REP-004).
  - llenado del reporte con `JRBeanCollectionDataSource` y exportación a `ByteArrayOutputStream`.
  *Depende de:* T-002, T-003, T-004. *Hecho cuando:* test unitario con datos mock devuelve un PDF
  no vacío (header `%PDF`).

- [ ] **T-006** Crear la plantilla `atp-puestos-camineros/src/main/resources/reports/resumen-turno.jrxml`
  con las bandas de §5.1 del plan y los parámetros de §5.2. *Depende de:* T-002. *Hecho cuando:*
  `JasperCompileManager.compileReport` no lanza excepción en test.

- [ ] **T-007** Implementar `ReporteTurnoController` (`GET /api/v1/turnos/{sucNroId}/{sucTipo}/{turNroId}/resumen`)
  con los códigos de estado de R-REP-TUR-009/011/012. *Depende de:* T-005. *Hecho cuando:* test
  MockMvc devuelve `200` con `Content-Type: application/pdf` para un turno válido y `404` para uno
  inexistente.

- [ ] **T-008** Modificar `templates/nucleo/turnos.html` para agregar el botón `Resumen` por fila
  (R-REP-TUR-014/015). *Depende de:* T-007. *Hecho cuando:* render de la vista contiene el enlace
  con `target="_blank"` y el tooltip `Imprimir Resumen`.

- [ ] **T-009** Actualizar la colección Postman `docs/postman/ATPPuestos-API.postman_collection.json`
  con el nuevo endpoint (G-POSTMAN). *Depende de:* T-007. *Hecho cuando:* la colección incluye el
  request y es ejecutable.

- [ ] **T-010** Tests unitarios de `ReporteTurnoService` (totales, filtro de anulados, mensaje
  vacío). *Depende de:* T-005. *Hecho cuando:* suite verde.

- [ ] **T-011** Tests MockMvc del controller (200/404/500, Content-Type, Content-Disposition).
  *Depende de:* T-007. *Hecho cuando:* suite verde.

- [ ] **T-012** Tests MVC de la vista (botón presente, enlace correcto). *Depende de:* T-008.
  *Hecho cuando:* suite verde.

- [ ] **T-013** Test de integración contra `Rentas` (gated `ATP_DB_IT=true`) que genera un PDF real
  para un turno existente. *Depende de:* T-007. *Hecho cuando:* test verde con `ATP_DB_IT=true`.

- [ ] **T-014** Verificación de paridad de BD y gobernanza (QA): nombres de tabla/columna exactos,
  sin DDL, sin secretos, archivos < 500 líneas. *Depende de:* T-001..T-013. *Hecho cuando:* informe
  de verificación sin hallazgos bloqueantes.

- [ ] **T-015** (Supuesto) Resolver SA-REP-01 (mapeo de CM por formulario) antes de cerrar el
  detalle: confirmar con el responsable si `CM` se toma de `LOTES227.LF276ICM` o de otra fuente.
  *Depende de:* decisión humana. *Hecho cuando:* supuesto cerrado y documentado en el spec.