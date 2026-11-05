# Verificación Gate 2 — Módulo 13: Comisiones (listado + detalle, sin PDFs)

**Fecha**: 2026-11-05
**Verificador**: QA (read-only sobre código)
**Spec**: `openspec/features/13-comisiones/spec.md` (R-COM-001..024)
**Plan**: `openspec/features/13-comisiones/plan.md`
**Tasks**: `openspec/features/13-comisiones/tasks.md`

## Veredicto

**PASS (unitarios + IT de comisiones) con 1 hallazgo de gobernanza/menú (no bloqueante para el código del módulo).** La suite unitaria está en verde (24 tests en los dos archivos del módulo, 0 fallos) y las verificaciones estáticas (paridad de BD, regla de dependencia, accesibilidad, rótulos verbatim, G-POSTMAN) cumplen. La integración contra la BD real (`ComisionRepositoryIT`) pasó 4/5 tests (1 skipped por asunción de datos, no por error). Se detecta un fallo preexistente no relacionado en `PopupLocalidadesIT` (fuera del alcance de este módulo).

## Comandos ejecutados

1. Compilación:
   ```
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd -pl atp-nucleo-compartido,atp-puestos-camineros compile -B
   ```
   Resultado: `BUILD SUCCESS`. Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\compile-comision.log`.

2. Unitarios del módulo:
   ```
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd -pl atp-puestos-camineros test -B "-Dtest=ServicioComisionesTest,ComisionMvcControllerTest"
   ```
   Resultado: `Tests run: 14` (ServicioComisionesTest) + `Tests run: 10` (ComisionMvcControllerTest) = `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`. Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\unit-comision.log`.

3. Suite completa (sin IT):
   ```
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd -pl atp-nucleo-compartido,atp-puestos-camineros test -B
   ```
   Resultado: `Tests run: 201, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`. Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\unit-run.log`.

4. Integración (gated, BD real):
   ```
   set ATP_DB_IT=true
   set DB_HOST=devmssqlatp.ecom.com.ar; set DB_PORT=1433; set DB_NAME=Rentas
   set DB_USER=rentas; set DB_PASSWORD=dev28rentas
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd test -B "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
   ```
   Resultado: `ComisionRepositoryIT` → `Tests run: 5, Failures: 0, Errors: 0, Skipped: 1` — `BUILD` parcial (1 fallo preexistente en `PopupLocalidadesIT`, no relacionado). Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\it-run.log`.

## Cumplimientos verificados (R-COM)

### Compilación y tests
- **T-COM-A1 / T-COM-E1 / T-COM-E2**: compilación verde y `ServicioComisionesTest` (14) + `ComisionMvcControllerTest` (10) en verde, 0 fallos. ✓
- **T-COM-E3**: `ComisionRepositoryIT` pasó 4/5 (1 skipped por asunción de datos — `laOrdenDeSegBusComiEsLaDelMayorNombreYApellido` requiere una persona con 2+ comisiones vigentes; no es fallo de código). ✓

### Paridad de BD (T-COM-A2, T-COM-A4, T-COM-E4)
- `Comision` (`@Table(name="COMISION")`) mapea **14 columnas**: `PueCod`/`CPCId` (PK compuesta), `CPCFchDes`, `CPCFchHas`, `CPCHorDes`, `CPCHorHas`, `CPCCierre`, `CPCResId`, `CPCEstado`, `CPCFchCie`, `CPCFeLog`, `CPCUsuLog`, `CPCSnNum`, `CPCFePag`. ✓ (spec §2.1)
- `ComisionIntegrante` (`@Table(name="COMISIO1")`) mapea **3 columnas** PK: `PueCod`, `CPCId`, `CPCPerId`. ✓
- `Persona` (`@Table(name="PERSONAS")`) mapea `PerId`, `PerNomyApe`, `PerCodUsu` (3 de 4; `AreaId` no mapeada — decisión previa del núcleo §03-login, documentada; no bloqueante para este módulo, que no usa `AreaId`). ⚠ nota.
- **4 huérfanas** (`CPCFeLog`, `CPCUsuLog`, `CPCSnNum`, `CPCFePag`) mapeadas en `Comision` con getters y **sin setters públicos** (sólo lectura, SA-COM-01). ✓ verificado por inspección de `Comision.java` líneas 198-216.
- `ddl-auto=none` confirmado en `application.yml` (`spring.jpa.hibernate.ddl-auto: none`). ✓
- Sin DDL/migraciones: no hay `schema.sql`/`data.sql` ni Flyway/Liquibase en el árbol (barrido). ✓
- Regla de dependencia: `ComisionMvcController` importa sólo `ServicioComisiones` (servicio), no repositorios. Sin atajo `web→repository`. ✓ (`Select-String` sobre el controller no devolvió coincidencias de "Repository").

### Servicio (T-COM-B1..B5)
- Filtro legacy reproducido: `pueCod` y `cpcResId` (personaId 0/null → omitido) en `ServicioComisiones.listar` y en `ComisionRepository.findComisionesConResponsable`. ✓ (tests `listarAplicaFiltroPueCodYPersonaId`, `listarOmitePersonaIdCuandoEsCero`, `listarOmitePersonaIdCuandoEsNull`).
- Orden `CPCId desc` en repositorio (`order by c.id.cpcId desc`) y en `Pageable` del servicio. ✓ (test `listarOrdenaPorCpcIdDesc`).
- Decodificación de Estado: `DecodificadorEstadoComision` → `'S'`→"Cerrado", `'N'`→"Abierto", `null`→"Abierto" (3 casos testeados). ✓
- Integrantes: `listarIntegrantes` ordenado por `trim(PerNomyApe)` asc en el repositorio; vacío manejado (test `listarIntegrantesVacio`). ✓
- `MensajesComision.NO_SE_ENCONTRARON_REGISTROS` usado por el controller en el caso inexistente. ✓
- `TAMANIO_PAGINA = 10` (constante del servicio, paridad con `MaxRows="10"`). ✓
- Formato de hora con cero a la izquierda (`%04d`) y fecha `dd/MM/yy` (SA-COM-005). ✓ (tests `horaFormateadaConCeroALaIzquierda`, `fechaFormateadaDdMmYy`).

### MVC / vistas (T-COM-C1..C4)
- Rutas `GET /comisiones` y `GET /comisiones/{pueCod}/{cpcId}` expuestas por `ComisionMvcController`. ✓ (tests MockMvc).
- Render listado, detalle, tabs, estado vacío, error de carga y paginación cubiertos por `ComisionMvcControllerTest` (10 tests). ✓
- Detalle inexistente muestra "No se encontraron registros" y oculta tabs (test `getDetalleInexistente`). ✓
- Accesibilidad: `<table>` con `<caption>` y `<th scope="col">` en ambas vistas (`comisiones.html`, `comision-detalle.html`). ✓
- Rótulos verbatim verificados en templates:
  - Listado: `Id Comisión`, `Fecha Inicio`, `Hora Inicio`, `Fecha Fin`, `Hora Fin`, `Estado`, `Responsable`. ✓
  - Detalle: `Código del Puesto`, `Puesto Caminero`, `Id Comisión`, `Fecha Inicio`, `Hora Inicio`, `Fecha Fin`, `Hora Fin`, `Estado`, `Id. Responsable`, `Responsable`. ✓
  - Integrantes: `Personal Afectado`, `Ap. y Nombre Persona Comis.`. ✓
- `findPueDescByPueCod` (consulta nativa a `PUEPPRIM`, SA-COM-03) implementado en `ComisionRepository`. ✓

### Integración con menú (T-COM-D1)
- El ítem `Comisiones` existe en `ServicioAutorizacion` (`MENU_PUE` id "7", `MENU_COM` id "1"). ⚠ **La ruta (link) es `null` en ambas entradas**, no `/comisiones` (ver hallazgo H-1). El spec R-COM-019 afirma que "ya existe y enlaza a `/comisiones`"; el código actual no cumple ese enlace. `AuthMvcHomeTest` espera ruta null, por lo que el test pasa, pero el enlace funcional no está.

### G-POSTMAN
- No aplica: el módulo no agrega/modifica/elimina endpoints `/api/**` (sólo MVC, R-COM-017). La colección `docs/postman/ATPPuestos-API.postman_collection.json` no contiene "comisiones" ni fue modificada (no aparece en `git status`). Documentado. ✓

### Gobernanza / ISO 25030
- Archivos de configuración: ninguno ≥ 500 líneas (barrido). ✓
- Sin secretos en texto plano; credenciales por `.env`. ✓
- R-COM-021 (adecuación funcional): paridad en columnas, rótulos, orden, paginación (10), decodificación de Estado y resolución de Responsable por JOIN. ✓
- R-COM-022 (usabilidad): tabs, estado vacío/error, paginación, formato legible. ✓
- R-COM-023 (mantenibilidad): reglas en `service/**`; `web/mvc` es adaptador. ✓
- R-COM-024 (seguridad): acceso con sesión autenticada (paridad `IsAuthorized`); el menú filtra por programa. ✓ (no se tocó seguridad).

## Hallazgos

### H-1 (severidad media) — Menú `Comisiones` no enlaza a `/comisiones`
- **Síntoma**: `ServicioAutorizacion` define el ítem "Comisiones" con `link = null` (MENU_PUE línea 31, MENU_COM línea 40), por lo que el fragmento `fragments/menu.html` lo renderiza como `href="#"`. El spec R-COM-019 y el task T-COM-D1 afirman que "ya existe y enlaza a `/comisiones`".
- **Evidencia**: `atp-nucleo-compartido/src/main/java/.../ServicioAutorizacion.java:31` y `:40`; `grep` de `/comisiones` en el archivo no devolvió resultados.
- **Causa raíz probable**: el coordinador asumió (SA-COM-05/D) que el ítem ya enlazaba; en el código el enlace nunca se agregó (patrón SA-LOG-10: los WebPanels del legacy no tenían equivalente). Otros items (Turnos) sí tienen `RUTA_TURNOS = "/turnos"`.
- **Responsable del arreglo**: coordinador / implementador-backend (agregar `RUTA_COMISIONES = "/comisiones"` y usarlo en las dos entradas del menú). Nota: el spec dice "no se modifica el menú en esta iteración", por lo que requiere decisión del coordinador (posible enmienda SA-COM). Mientras tanto, el usuario puede navegar tipeando `/comisiones` directamente.
- **Impacto en tasks**: T-COM-D1 no puede marcarse como completado hasta resolver H-1.

### H-2 (severidad baja, preexistente, fuera de alcance) — Fallo en `PopupLocalidadesIT`
- **Síntoma**: `PopupLocalidadesIT.renderizaFilasHuerfanasConProvinciaVacia` falla (línea 67). No relacionado con comisiones.
- **Evidencia**: `it-run.log` línea 17396; `PopupLocalidadesIT.java:67`.
- **Responsable**: módulo 10 (popup-localidades), no este módulo.

### H-3 (informativo) — `Persona` no mapea `AreaId`
- El spec de este módulo lista `AreaId` como columna 4 de `PERSONAS`, pero la entity `Persona` (decisión del núcleo, spec 03-login) no la mapea. No bloquea este módulo (no se usa). Documentado para trazabilidad.

### H-4 (informativo) — 1 test IT skipped por asunción de datos
- `ComisionRepositoryIT.laOrdenDeSegBusComiEsLaDelMayorNombreYApellido` se saltó (`assumeTrue(personaConVarias != null)`). No es fallo de código; depende de datos reales de `Rentas`.

## Items de tasks.md marcables como completos

- **T-COM-A1** ✓ (compilación verde + IT de comisión pasa).
- **T-COM-A2** ✓ (4 huérfanos mapeados, sin setters).
- **T-COM-A3** ✓ (`findPueDescByPueCod` implementado).
- **T-COM-A4** ✓ (`findComisionesConResponsable` paginado).
- **T-COM-B1..B6** ✓ (servicio, decodificador, mensajes, view models).
- **T-COM-C1..C4** ✓ (templates + controller + inexistente).
- **T-COM-D1** ✗ (bloqueado por H-1; verificar enlace a `/comisiones`).
- **T-COM-E1** ✓ (14 tests unitarios de servicio).
- **T-COM-E2** ✓ (10 tests MockMvc).
- **T-COM-E3** ✓ (IT pasa 4/5, 1 skip por datos).
- **T-COM-E4** ✓ parcial (paridad BD, tokens, accesibilidad OK; menú pendiente por H-1; G-POSTMAN N/A documentado).
- **T-COM-E5** pendiente (mem_save a ejecutar por el coordinador).

## Conclusión

El módulo 13-comisiones cumple los requisitos R-COM-001..024 verificables en el entorno local (unitarios + estático + IT de comisiones). El único bloqueo funcional es el enlace del menú (H-1), que requiere decisión del coordinador por la contradicción con "no se modifica el menú en esta iteración". El fallo de `PopupLocalidadesIT` (H-2) es preexistente y no pertenece a este módulo.
## Re-verificación post-fix (ORDER BY duplicado en ComisionRepository)

**Fecha**: 2026-11-05
**Verificador**: QA (read-only sobre código)

### Comandos ejecutados

1. Instalar núcleo:
   `powershell
   C:\Program Files\Java\jdk1.8.0_321 = "C:\Program Files\Java\jdk-17"
   .\mvnw.cmd -B -pl atp-nucleo-compartido install -DskipTests
   `
   Resultado: BUILD SUCCESS. Log: C:\Users\walterg20\AppData\Local\Temp\opencode\step1.log.

2. Tests de tp-puestos-camineros:
   `powershell
   C:\Program Files\Java\jdk1.8.0_321 = "C:\Program Files\Java\jdk-17"
   .\mvnw.cmd -B -pl atp-puestos-camineros test
   `
   Resultado: Tests run: 201, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS. Log: C:\Users\walterg20\AppData\Local\Temp\opencode\step2.log.

3. IT de comisiones contra BD real:
   `powershell
   C:\Program Files\Java\jdk1.8.0_321 = "C:\Program Files\Java\jdk-17"
    = "true"
   # DB_* cargadas desde .env
   .\mvnw.cmd -B -pl atp-nucleo-compartido test "-Dtest=ComisionRepositoryIT" "-Dsurefire.failIfNoSpecifiedTests=false"
   `
   Resultado: Tests run: 6, Failures: 0, Errors: 0, Skipped: 1 - BUILD SUCCESS. Log: C:\Users\walterg20\AppData\Local\Temp\opencode\step3.log.

4. Suite completa unitaria (núcleo):
   `powershell
   C:\Program Files\Java\jdk1.8.0_321 = "C:\Program Files\Java\jdk-17"
   .\mvnw.cmd -B -pl atp-nucleo-compartido test
   `
   Resultado: Tests run: 408, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS. Log: C:\Users\walterg20\AppData\Local\Temp\opencode\step4.log.

### Confirmación de los 2 registros (findComisionesConResponsableDevuelveDos)

Salida del test (COMISIONES size=2):
- COMISION id=12 pueCod=9 cpcResId=91 responsable=Sebastian Montiel
- COMISION id=7 pueCod=9 cpcResId=91 responsable=Sebastian Montiel

pagina.getTotalElements() == 2 -> aserción PASS.

### Skipped

- laOrdenDeSegBusComiEsLaDelMayorNombreYApellido: skipped por Assumptions.assumeTrue (ninguna persona con 2+ comisiones vigentes en la BD actual). No es fallo de código; es dependencia de datos.

### Veredicto re-verificación

**PASS** - compilación OK, suite unitaria OK (408 núcleo + 201 puestos = 609 tests, 0 fallos), IT de comisiones OK (5 ejecutados + 1 skipped por asunción de datos), y el test indComisionesConResponsableDevuelveDos confirma los 2 registros esperados (ids 12 y 7).
