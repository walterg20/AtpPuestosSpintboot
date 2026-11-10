# Verificación QA — Feature `14-cabecera-footer-uniformes`

**Verificador:** verificador-qa (read-only sobre código; escritura solo en `docs/**`)
**Fecha de corrida:** 2026-11-10
**Rama:** `design/stitch-modern-portal`
**Spec:** `openspec/features/14-cabecera-footer-uniformes/` (APPROVED)

## 1. Resumen de gates

| Gate | Comando | Resultado |
|---|---|---|
| Unitarios + @WebMvcTest | `set JAVA_HOME=C:\Program Files\Java\jdk-17` + `.\mvnw.cmd -B clean test` | **PASS** — `Tests run: 408` (núcleo) + `Tests run: 201` (puestos) = 609, 0 fallos, 0 errores. `BUILD SUCCESS`. Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\build-14.log` |
| ITs contra BD real | `set JAVA_HOME=...` + `ATP_DB_IT=true` + vars `.env` + `.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"` | **FAIL parcial** — 78 tests corridos, 2 fallos en `UsuarioRepositoryIT` (conteo esperado vs actual). Resto verde incl. `ServicioAccesoIT` (7), `ProductoVigenciaDiferencialIT`, `ProductoVigenciaImporteNuloIT`, `FeriadoControllerWriteIT`, `ProductoControllerWriteIT`. Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\it-14.log` |
| Paridad BD | inspección git diff + grep | **PASS** — solo templates; sin DDL/schema.sql/data.sql; `ddl-auto=none` (application.yml:69, application-test.yml:16); sin cambios en `domain/` ni `repository/` |
| Gobernanza | grep CDN/http + wc líneas + npm | **PASS** — sin CDN ni `http://`/`https://` en templates migrados (excepto `xmlns`); todos los templates < 500 líneas (máx 336); sin `package.json` ni `npm` en POMs |
| G-POSTMAN | `git diff --stat -- docs/postman/` | **PASS** — sin cambios (no se tocaron endpoints REST) |
| Regla dependencia web→service | grep `Repository` en `web/mvc` | **PASS** — ningún controller MVC importa Repository |

## 2. Detalle de tests de vistas (MockMvc) — todos verdes

Clases `*Mvc*Test` / `*MvcControllerTest` afectadas (render/binding/redirección) y su resultado:

Núcleo (`atp-nucleo-compartido`):
- `AuthMvcCodigosAccesoTest` — 7 ✓
- `AuthMvcHomeTest` — 5 ✓
- `AuthMvcLoginTest` — 19 ✓
- `AuthMvcNotAuthorizedTest` — 3 ✓
- `AuthMvcSecurityTest` — 8 ✓
- `GestorContextoSesionTest` — 3 ✓
- `MenuFragmentoTest` — 3 ✓
- `PantallasHtmlSinSesionTest` — 6 ✓
- `ProductoFormParidadRestriccionesTest` — 5 ✓
- `ProductoMvcControllerTest` — 14 ✓
- `ProductoMvcEscrituraTest` — 12 ✓
- `TurnoMvcControllerTest` — 13 ✓
- `TurnoMvcEscrituraTest` — 8 ✓

Puestos (`atp-puestos-camineros`):
- `ActasMvcControllerTest` — 15 ✓
- `ActasMvcFormularioTest` — 19 ✓
- `ActasMvcManualTest` — 5 ✓
- `ActasMvcPopupTest` — 3 ✓
- `ActasRecursosTest` — 6 ✓
- `ComisionMvcControllerTest` — 10 ✓
- `LocalidadesMvcTest` — 9 ✓
- `TurnosAbiertosMvcTest` — 6 ✓
- `CargaCorreccionMvcTest` — 9 ✓
- `CargaMvcControllerTest` — 19 ✓
- `ProductoMvcCsrfTest` — 4 ✓ (incluye POST con/sin CSRF → 403)

Total MockMvc: ~210 tests, 0 fallos.

## 3. Verificación de fragmentos y migración

- Fragmentos presentes en `base-stitch.html` (líneas): `topbar` (130), `hero` (157), `footer-institucional` (184). R-CH-001 ✓.
- `login.html` consume los 3 fragmentos (líneas 15, 17, 119). R-CH-002 ✓.
- 7 vistas Stitch (`puestos/*`) + `productos.html` + `turnos.html` consumen `topbar + hero + footer-institucional` (3 reemplazos cada una). R-CH-003/R-CH-004 ✓.
- Sin residuos `layout/base ::` en vistas migradas. R-CH-005 ✓.
- Sin cambios en bindings Thymeleaf (`th:object`/`th:field`) — solo chrome. R-CH-007 ✓.

## 4. Hallazgos

### H-01 (medio) — 2 ITs fallando en `UsuarioRepositoryIT` por conteo
- **Síntoma:** `laCantidadDeUsuIdDecodificadosReproduceElDatoDeLaSpec` esperaba 549L, obtuvo 550L; `losUsuIdSeDescifranSinExcepcion` esperaba 552, obtuvo 553.
- **Evidencia:** `C:\Users\walterg20\AppData\Local\Temp\opencode\it-14.log` (sección Results, UsuarioRepositoryIT).
- **Causa raíz probable:** datos de la BD `Rentas` cambiaron (un usuario agregado) o test acoplado a conteo exacto. **No causado por feature 14** (solo templates). Requiere revisión del implementador-backend/analista sobre la expectativa del test.
- **Agente responsable:** implementador-backend (ajustar expectativa o investigar dato).

### H-02 (bajo) — Colores hardcoded en SVG del hero
- **Síntoma:** `stroke="#ec4899"`, `stroke="#38bdf8"` en `base-stitch.html:160-161`.
- **Evidencia:** grep de literales hex en templates migrados.
- **Causa raíz probable:** markup SVG decorativo extraído verbatim del hero original; no es CSS. R-CH-006 se refiere a tokens de estilo; el SVG mantiene colores inline por diseño. No bloqueante, pero documentar.
- **Agente responsable:** implementador-vistas (opcional: mover a tokens CSS o dejar verbatim).

### H-03 (info) — `puestos/fragments/*` no migrados (C8)
- **Síntoma:** tasks.md marca C8 como PENDIENTE, fuera del alcance explícito.
- **Agente responsable:** requiere spec de alcance si se desea migrar.

### H-04 (info) — Smoke manual E3 pendiente
- Requiere ejecución del `desplegador-local` y verificación visual por el usuario.

## 5. Veredicto

- **Gates de compilación/unitarios:** PASS.
- **ITs de lectura contra BD real:** FAIL por 2 tests de `UsuarioRepositoryIT` no relacionados con el feature (datos de BD). Reportar como bloqueo menor para el implementador-backend.
- **Paridad BD / gobernanza / G-POSTMAN / dependencia:** PASS.

## 6. Próximo paso

1. El implementador-backend debe revisar `UsuarioRepositoryIT` (conteos 549/552 vs 550/553) y actualizar la expectativa o investigar el dato nuevo en `Rentas`.
2. Completar smoke manual (E3) con el desplegador-local.
3. Decidir alcance de migración de `puestos/fragments/*` (C8).
4. Re-correr `.\mvnw.cmd -B clean test` tras cualquier ajuste para confirmar 609/609.
