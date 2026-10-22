# Tasks — 00 Alcance migrable

> **Estado**: `APPROVED` — puerta **G-SPEC**.
> Casillas `- [ ]`. El **coordinador** marca lo completado, sólo con evidencia.

## F1 — Andamiaje del monorepo

- [ ] **T-001** — Crear el monorepo: `pom.xml` parent (packaging=pom) con los módulos
  `atp-nucleo-compartido` y `atp-puestos-camineros`, y el Maven Wrapper en la raíz.
  *Criterio*: `mvn validate` resuelve los dos módulos. (R-ALC-001)
- [ ] **T-002** — Portar el código de `atppuestos2/backend` al módulo `atp-nucleo-compartido`
  (`nucleo.**` + `shared.**`) y la clase ejecutable a `atp-puestos-camineros`.
  *Criterio*: los 105 tests preexistentes siguen verdes. (R-ALC-001)
- [ ] **T-003** — Renombrar `nucleo.web.controller` → `nucleo.web.rest` y actualizar imports.
  *Criterio*: `mvn clean test` verde. (D-04)
- [ ] **T-004** — Mover los dobles de test REST al árbol `web/rest` y añadir los imports
  correspondientes en `GlobalExceptionHandlerTest`.
  *Criterio*: `mvn clean test` verde. (D-04)
- [ ] **T-005** — Convertir el `GlobalExceptionHandler` en advice **global** con negociación
  JSON/HTML por prefijo de ruta, y crear `templates/error.html`.
  *Criterio*: los 12 tests de `GlobalExceptionHandlerTest` verdes y un test nuevo que verifique
  que una ruta **no** `/api/` devuelve HTML. (D-02)
- [ ] **T-006** — Dejar `application.yml` **sólo** en el núcleo y `application-test.yml` en los
  `src/test/resources` de cada módulo.
  *Criterio*: `mvn clean test` verde en los dos módulos; el perfil `test` no toca SQL Server.
  (D-01)
- [ ] **T-007** — Añadir `spring-boot-starter-thymeleaf` y `spring-boot-starter-security` al
  módulo desplegable, y `spring-security-test` en test.
  *Criterio*: `mvn clean package` genera el fat jar. (R-ALC-006)
- [ ] **T-008** — Reescribir el arnés: `agents.md`, los 5 subagentes (con
  `implementador-vistas` en lugar de `implementador-frontend`) y los 3 comandos.
  *Criterio*: la allow-list de subagentes del `coordinador` coincide **exactamente** con los
  `id` de los archivos de `.opencode/agents/`. (R-ALC-001)
- [ ] **T-009** — Escribir `openspec/config.yaml` y `openspec/constitution/{stack,conventions,sdd-workflow}.md`.
  *Criterio*: las secciones obligatorias de la constitución están presentes. (R-ALC-001)
- [ ] **T-010** — Actualizar `README.md`, `docs/design.md`, `.env.example` y
  `docs/postman/README.md` al nuevo stack y a la nueva estructura.
  *Criterio*: los documentos **vigentes** (README, agents.md, docs/design.md, .env.example,
  docs/postman/README.md, openspec/**) no mencionan Angular ni `frontend/`; las specs portadas de
  `01-nucleo-datos/` sí lo hacen, y está bien: documentan la implementación Angular histórica.
  (R-ALC-006)
- [ ] **T-011** — Agregar `docs/analisis-ATPPuestos.md` §14 con el mapa del portfolio (KB →
  destino → base de datos) y §13 con el esquema físico de seguridad.
  *Criterio*: el mapa coincide con lo verificado. (R-ALC-008)
- [ ] **T-012** — Actualizar `.gitignore` a la estructura del monorepo (sin `backend/`, sin
  `frontend/`) e inicializar el repositorio Git.
  *Criterio*: `git status` no lista artefactos de build. (R-ALC-001)

## F2 — Módulos derivados

- [ ] **T-020** — Escribir `openspec/features/02-abm-productos/{spec,plan,tasks}.md`.
  *Criterio*: G-SPEC aprobado; reutiliza la mini-spec `ui-abm-productos.md` del proyecto
  anterior. (R-ALC-001)
- [ ] **T-021** — Escribir `openspec/features/03-login/{spec,plan,tasks}.md`.
  *Criterio*: G-SPEC aprobado; incluye la matriz de códigos 0-6 y la transcripción literal del
  cifrado legacy. (R-ALC-001)
- [ ] **T-022** — Registrar los supuestos abiertos ASM-01..04 de `00-alcance/spec.md` en las
  specs de los módulos que los toquen.
  *Criterio*: cada supuesto tiene dueño y criterio de resolución. (R-ALC-010)

## F3 — Verificación del andamiaje

- [ ] **T-030** — Correr la suite completa: `.\mvnw.cmd -B clean test` con JDK 17.
  *Criterio*: BUILD SUCCESS, sin fallos ni errores.
- [ ] **T-031** — Correr la integración contra la BD real:
  `.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"` con `ATP_DB_IT=true`.
  *Criterio*: BUILD SUCCESS contra `Rentas`; sin escrituras no autorizadas.
- [ ] **T-032** — Verificar G-POSTMAN: la colección coincide con los endpoints expuestos.
  *Criterio*: `docs/postman/README.md` describe la cobertura actual y coincide con el código.
- [ ] **T-033** — Verificar gobernanza: sin secretos, `ddl-auto=none`, sin DDL ni migraciones,
  archivos de configuración < 500 líneas.
  *Criterio*: informe del verificador con `PASS`.
