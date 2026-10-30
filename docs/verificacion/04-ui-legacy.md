# Verificación — Módulo `04-ui-legacy` (re-verificación tras fix H-1)

**Veredicto:** `PASS`

Todas las verificaciones locales/estáticas, la suite unitaria y la integración contra la base real
pasan. El hallazgo H-1 (`/images` → `/img`) quedó resuelto y las rutas públicas/exclusiones del
materializador CSRF coinciden con `static/img`. La base `devmssqlatp.ecom.com.ar:1433` responde en
esta corrida (el bloqueo H-5 del reporte anterior no se reproduce).

**Fecha:** 2026-10-23
**Agente:** verificador-qa
**Comandos exactos y logs:**
- Unitarios: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-test-04recheck.log`
- IT: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-it-04recheck.log`

---

## 1. Unitarios

**Comando exacto:**

```powershell
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B clean test
```

**Resultado:** `BUILD SUCCESS`.

Resumen autoritativo por módulo:

- `atp-nucleo-compartido`: `Tests run: 308, Failures: 0, Errors: 0, Skipped: 0`
- `atp-puestos-camineros`: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
- Total: **313 tests, 0 fallos, 0 errores, 0 omitidos**.

Log completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-test-04recheck.log`.

## 2. Integración contra la BD real (IT)

**Comando exacto:**

```powershell
# Variables cargadas desde .env (DB_HOST=devmssqlatp.ecom.com.ar, DB_PORT=1433, DB_NAME=Rentas, ...)
set ATP_DB_IT=true
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Resultado:** `BUILD SUCCESS`. `Tests run: 69, Failures: 0, Errors: 0, Skipped: 1`.

La conexión TCP a `devmssqlatp.ecom.com.ar:1433` responde en esta corrida (el bloqueo H-5 del
reporte anterior no se reproduce). Entre los IT relevantes para seguridad/login:

- `T-304/T-305 - Login contra la base Rentas`: 7 tests, 0 fallos (credenciales correctas,
  incorrectas, usuario inexistente, usuario sin clave legacy, usuario con `UsuIngreso=1`).
- `T-304 - SCRTY (Usuario)`: lectura de solo acceso, 6 tests.
- `T-133 - COMISION y COMISIO1`: 5 tests, 1 skipped (comportamiento documentado).

Log completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-it-04recheck.log`.

## 3. Confirmación del fix H-1 (`/images` → `/img`)

**Estado: RESUELTO.**

- `SeguridadConfig.RUTAS_PUBLICAS` declara `"/img/**"` (singular) — `SeguridadConfig.java:63`.
- `MaterializadorTokenCsrf.RECURSOS_ESTATICOS` declara `"/img/"` (singular) —
  `MaterializadorTokenCsrf.java:36`.
- Ambas listas coinciden con el directorio real `static/img/` (`header.png`, `login-bg.png`,
  `logo-atp.png`, `logo-ecom.png`) y con las referencias de las plantillas (`@{/img/...}` en
  `layout/base.html` y `nucleo/login.html`/`nucleo/home.html`).
- No quedan referencias a `/images` (plural) en código ni plantillas.

Consecuencias verificadas:
1. Un usuario no autenticado que carga `/login` puede obtener las imágenes (ruta pública).
2. El materializador CSRF no crea sesión para peticiones bajo `/img/`.

## 4. Vistas Thymeleaf

Pantallas auditadas: `/login`, `/home`, `/inicio` (más `/`, `/not-authorized`).

### Render / binding / redirección / error

- `GET /login` → vista `nucleo/login`, view model completo (`loginForm`, `tipos`,
  `tieneErrores`, `mensajeError=null`). Tests: `AuthMvcLoginTest` (19),
  `AuthMvcCodigosAccesoTest` (7 parametrizados + 3), `PantallasHtmlSinSesionTest` (5).
- `POST /login` exitoso → 302 a `/home`, cookie `SUCURSAL` escrita (HttpOnly, SameSite=Lax,
  Path=/, sin Max-Age). Test: `AuthMvcLoginTest.elLoginExitosoEscribeLaCookieSucursalYRedirigeAHome`.
- `POST /login` con error de negocio (códigos 2-6) → 200, mensaje verbatim junto a la contraseña
  (`atp-alert--error`). Tests: `AuthMvcCodigosAccesoTest`.
- `POST /login` con binding inválido → 200, error junto al campo y botón `Ingresar` deshabilitado.
  Test: `AuthMvcLoginTest.losErroresDeBindingSeMuestranJuntoAlCampoYDeshabilitanElBoton`.
- `GET /home` sin sesión → 302 a `/login`; con sesión → `nucleo/home` con los 10 campos del
  contexto, texto de inicio verbatim y menú de programas. Tests: `AuthMvcHomeTest` (5),
  `AuthMvcSecurityTest`.
- `GET /inicio` → `nucleo/inicio` con texto descriptivo y menú activo (`AuthMvcController.inicio`).
  **Sin test dedicado** (ver hallazgo H-3, baja).
- `GET /` → 302 a `/home`. Test: `AuthMvcSecurityTest`.
- `GET /not-authorized` → vista con mensaje verbatim. Tests: `AuthMvcNotAuthorizedTest` (3).

### Inspección de formularios (CSRF y tokens)

- Todos los formularios de escritura usan `th:action`:
  - `nucleo/login.html:37` `th:action="@{/login}"`
  - `nucleo/home.html:103` `th:action="@{/logout}"`
  - `fragments/user-info.html:22` `th:action="@{/logout}"`
  - `nucleo/productos.html:103/216` `th:action=...`
- CSRF activo y verificado: `SeguridadCsrfTest` (5), `AuthMvcSecurityTest` (POST sin token → 403).
- Errores de validación junto al campo: `th:errors="*{...}"` en login y productos.

### Literales de color/tipografía en plantillas

Búsqueda de `#[0-9a-fA-F]{3,6}`, `\d+(px|rem|em)` y `(color|background|font-size|font-family):`
en `templates/`: **sin coincidencias**. Sólo se usa `th:style` para `background-image` (imagen de
fondo del login), no para color ni tipografía. Cumple R-UI-008.

### Assets (Fase A)

Presentes en `static/img/`: `header.png`, `login-bg.png`, `logo-atp.png`, `logo-ecom.png`;
`static/favicon.ico`. Cumple R-UI-007.

### Menú server-side (Fase D)

`ServicioAutorizacion.programasPorTipo`: PUE=5, RECEP/REC=4, COM=1, estructura recursiva
`SmoothNavMenuData`. Test: `ServicioAutorizacionMenuTest` (4). Cumple R-UI-003.

## 5. Paridad de BD

- Sin DDL ni migraciones: no existe `schema.sql`/`data.sql`, ni Flyway/Liquibase en los POM.
- `ddl-auto: none` en `application.yml:69`.
- Entidades no modificadas por este módulo (feature 04 es solo UI). Mapeo de nombres físicos
  heredado de `01-nucleo-datos` sin regresión en compilación/tests.
- IT de repositorios y servicios contra la BD real pasan (ver §2).

## 6. Seguridad

- Sin secretos en texto plano: `.env` está en `.gitignore`; `.env.example` no contiene valores
  reales.
- `ddl-auto=none` (ver §5).
- CSRF activo en rutas MVC con sesión; descartado explícitamente en `/api/**` (D-LOG-17(a)).
  Verificado por `SeguridadCsrfTest` y `AuthMvcSecurityTest`.
- Cookie de sesión `SameSite=Lax`, `HttpOnly`.
- Verificación de credenciales legacy contra la BD real: 7 tests pasan (ver §2, T-304/T-305).

## 7. Gobernanza

- Archivos de configuración por debajo de 500 líneas: verificado (pom.xml ≤ 87 líneas,
  `application.yml` = 98 líneas, configs de test ≤ 16 líneas).
- G-POSTMAN: la feature 04 no agregó ni modificó endpoints REST (`/api/**`); no hay cambios en
  controladores REST desde el reporte anterior. La colección
  `docs/postman/ATPPuestos-API.postman_collection.json` (46 requests) sigue sincronizada con los
  42 endpoints expuestos por los controladores REST. Sin cambios requeridos.
- Requisitos ISO/IEC 25030 de la spec `04-ui-legacy` (R-UI-001..009): verificados en §4 y §5.
  Cobertura por requisito:
  - R-UI-001 layout base: `layout/base.html` (header + nav + footer; main en cada página). ✓
  - R-UI-002 bloque de usuario condicional a sesión: `fragments/user-info.html`
    (`th:if="${contexto != null}"`). ✓
  - R-UI-003 menú server-side por Tipo: `ServicioAutorizacion` + `fragments/menu.html`. ✓
  - R-UI-004 login replicando WebComponent: `nucleo/login.html`. ✓
  - R-UI-005 Home con Login si sin sesión / Inicio si con sesión: `AuthMvcController.home`. ✓
  - R-UI-006 `/inicio` con texto descriptivo: `AuthMvcController.inicio` + `nucleo/inicio.html`. ✓
  - R-UI-007 assets en `static/img/` (y favicon). ✓
  - R-UI-008 solo tokens en plantillas (sin literales). ✓
  - R-UI-009 formularios con `th:action` y errores junto al campo. ✓

## 8. Hallazgos

Ordenados por severidad.

### H-1 (Media) — RESUELTO

Recursos estáticos bajo `/img/` no estaban en la lista de rutas públicas ni en la exclusión del
materializador CSRF. **Fix aplicado**: `SeguridadConfig` usa `/img/**` y `MaterializadorTokenCsrf`
usa `/img/`. Verificado en §3. Sin regresión (313 unitarios + 69 IT verdes).

### H-2 (Baja) — T-UI-A6 (fuentes SourceSansPro) pendiente

Tarea unchecked en `tasks.md` (Fase A). Decisión D-UI pendiente. No bloquea Fases A-D
funcionalmente.

### H-3 (Baja) — Sin tests Thymeleaf para `/inicio`

`AuthMvcController.inicio` y la vista `nucleo/inicio.html` existen y renderizan, pero no hay test
dedicado (T-UI-F1 pendiente). Cobertura actual: render indirecto vía `/home`. Recomendado agregar
prueba de render/binding para `/inicio`.

### H-4 (Baja) — T-UI-E3 y T-UI-F1..F5 pendientes

Tests de user-info y verificación formal (contraste AA, navegación por teclado, inspección de
tokens) no implementados. Las verificaciones estáticas de tokens se realizaron manualmente (§4).

### H-5 (Entorno) — RESUELTO

El bloqueo de red del reporte anterior no se reproduce: la BD responde y los 69 IT pasan
(ver §2).