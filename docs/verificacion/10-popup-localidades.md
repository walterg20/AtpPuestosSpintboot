# Verificación Gate 2 — Módulo 10: Popup de Selección Manual de Localidades

**Fecha**: 2026-11-04
**Verificador**: QA (read-only sobre código)
**Spec**: `openspec/features/10-popup-localidades/spec.md` (R-LOC-001..015)
**Plan**: `openspec/features/10-popup-localidades/plan.md`

## Veredicto

**PASS (unitarios) / BLOQUEADO (IT por BD)** — la suite unitaria está en verde (177 tests, 0 fallos) y las verificaciones estáticas (paridad, seguridad, gobernanza, JS vanilla) cumplen. Las pruebas de integración contra la BD real (`PopupLocalidadesIT`) no pudieron ejecutarse por indisponibilidad de red a `devmssqlatp.ecom.com.ar:1433` (DNS/firewall), como en verificaciones previas del módulo 08.

## Comandos ejecutados

1. Unitarios:
   ```
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd -B clean test
   ```
   Resultado: `Tests run: 177, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` (37.2 s).
   Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-mvn-test.log`

2. Integración (gated):
   ```
   set ATP_DB_IT=true
   set DB_HOST=devmssqlatp.ecom.com.ar; set DB_PORT=1433; set DB_NAME=Rentas
   set DB_USER=rentas; set DB_PASSWORD=dev28rentas
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   .\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
   ```
   Resultado: `Tests run: 72, Failures: 0, Errors: 72, Skipped: 0` — `BUILD FAILURE` (1:34 min).
   Error textual (extracto de `atp-nucleo-compartido/target/surefire-reports/...AreaRepositoryIT.txt`):
   > `org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC Connection [The TCP/IP connection to the host devmssqlatp.ecom.com.ar, port 1433 has failed. Error: "devmssqlatp.ecom.com.ar. Verify the connection properties... Make sure that TCP connections to the port are not blocked by a firewall."]`
   Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-mvn-it.log`

## Hallazgos

### Cumplimientos verificados

- **R-LOC-001 / R-LOC-002**: `ActasController` expone los 3 flags `locCodVenDerivado`, `locCodDesDerivado`, `locCodTranDerivado` (líneas 163-167 y 191-193 de `ActasController.java`). `ActasMvcPopupTest` verifica render con `is-enabled`/`data-popup-enabled="true"` (CUIT inexistente) y `is-disabled`/`data-popup-enabled="false"` (CUIT existente). Tests en verde.
- **R-LOC-003**: `localidades-seleccion.html` renderiza 6 `<th>` (selector, `Cod. Loc.`, `Nombre`, `LocNomProv` oculta, `Provincia`, `Cod. Postal`). `LocalidadesMvcTest.getLocalidadesRenderizaColumnas` pasa.
- **R-LOC-004**: provincia como `<input readonly>` en acta-formulario (f276VenProv/Des/Tran) y columna de solo lectura en popup (`ProvNombre`). Sin `<input>` editable para provincia. OK.
- **R-LOC-005 (JS puntual)**: `localidades-popup.js` vanilla, usa `window.open('/localidades', ...)` y `window.opener.setLocalidad(...)`. Grep sobre `*.js`/`*.html` (excluyendo `target` y `docs/verificacion`) no encontró `iframe`, `bootstrap`, `tailwind`, `cdn.`, `unpkg`, `jsdelivr`, `node_modules`, `fetch(`, `XMLHttpRequest`, `eval(`. Las únicas coincidencias son comentarios documentales.
- **R-LOC-006**: selección vía `window.opener.setLocalidad(cod, nom, prov)` + `window.close()` en filas (botón y enlace). `window.setLocalidad` global definida en el JS. OK.
- **R-LOC-007**: plantilla sigue siendo `puestos/acta-formulario`; fieldsets `Vendedor`, `Destinatario`, `Transportista` presentes con sus campos; ícono ⇧ (U+21E7) en 3 fieldsets. `ActasMvcPopupTest` pasa.
- **R-LOC-008**: filtros `filtroLocCod` (>=), `filtroLocNom` (LIKE/contains), `filtroProvNombre` (LIKE sobre `ProvNombre`/`LocNomProv`), `filtroLocCodPost` (>=) en `LocalidadPopupService`. Test parametrizado pasa.
- **R-LOC-009**: paginación default 5 (`TAMANIO_PAGINA = 5`), expone `totalPages`, `number`, `totalElements` (test `getLocalidadesExponePaginacion` pasa).
- **R-LOC-010**: sin sesión → 302 a `/login` (`getLocalidadesSinSesionDevuelve302` pasa).
- **R-LOC-011**: `POST /localidades` sin CSRF → 403 (`postLocalidadesSinCsrfDevuelve403` pasa).
- **R-LOC-012**: cancelar cierra sin modificar (`window.close()` en botón Cancelar; no se tocan campos del padre). OK por inspección.
- **R-LOC-013**: `LocalidadPopupService` reutiliza `LocalidadRepository.findAllConProvincia()` (left join fetch). IT bloqueado por BD; paridad por código.
- **R-LOC-014 (no regresión)**: `./mvnw clean test` en verde (177 tests). `git status` no muestra cambios en `Localidad.java`, `Provincia.java` ni `LocalidadRepository.java`. OK.
- **R-LOC-015 (paridad mapeo)**: `ddl-auto=none` confirmado en `application.yml` (línea 69) y en `application-test.yml`. Sin DDL/migraciones en el diff. OK.

### Gobernanza

- Archivos de configuración: ninguno ≥ 500 líneas (verificado por barrido).
- Nuevos archivos del módulo: `LocalidadesController.java` (93 L), `LocalidadPopupService.java` (121 L), `localidades-popup.js` (92 L), `localidades-seleccion.html` (117 L). Todos < 500.
- G-POSTMAN: la colección `docs/postman/ATPPuestos-API.postman_collection.json` no aparece en `git status` (sin cambios); el módulo es MVC-only (D-LOC-05), sin endpoints REST nuevos. OK.
- G-SEC: sin secretos en texto plano; credenciales por `.env`. OK.
- G-DATA: sin DDL, sin `schema.sql`/`data.sql`, sin Flyway/Liquibase. OK.

### Pendiente (bloqueado por entorno)

- `PopupLocalidadesIT` (2 tests: 30 filas FK huérfana con provincia vacía; conteo total vs `SELECT COUNT(*) FROM LOCALIDA`) no pudo ejecutarse por indisponibilidad de `devmssqlatp.ecom.com.ar:1433`. Se debe re-ejecutar con VPN/red disponible para cerrar R-LOC-013 y la paridad de conteo.

## Conclusión

El módulo 10 cumple los requisitos R-LOC-001..015 verificables en el entorno local (unitarios + estático). Queda pendiente la validación IT contra la BD real por bloqueo de red (no atribuible al código). Se recomienda re-correr `*IT` con conectividad a `Rentas` para cerrar R-LOC-013.