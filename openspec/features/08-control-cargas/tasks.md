# Tasks — 08 Control de Cargas (Puestos Camineros)

> **Estado de la puerta G-SPEC**: **APROBADA (revisión v1, 2026-10-30)** — el módulo puede pasar a
> implementación.
> Checklist verificable del módulo `08-control-cargas`. Cada tarea es **atómica** y trazable a un
> requisito de `spec.md` (R-CC-*) o a un punto de `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` (puerta G-SPEC)** — guardarraíl G2.
> **Ninguna tarea está marcada como completada por el analista**: el cierre y la evidencia son del
> coordinador.
> Tareas **B** = backend (`puestos/domain`, `puestos/repository`, `puestos/service`, `puestos/web`).
> Tareas **V** = vistas (`puestos/web/mvc`, `templates/**`, `static/**`).
> Tareas **Q** = verificación y puertas de calidad.
> Tareas **P** = Postman / documentación.

**Leyenda de dependencias**: `→` significa "depende de". `Req:` = requisito de `spec.md`.
`Plan:` = sección de `plan.md`.

---

## A. Extracción y verificación (previa a la implementación)

- [ ] **T-001** Releer por el MCP `genexus18mcp` (alias `atppuestos`, **sólo lectura**) el WebPanel
  `ControlDeCargas`, el Procedure `MarcarGuia`, los WebPanels `Form506Detalle`, `WWTDetProdPri` y
  `WPFormularioPago`, y las tablas `Contribu` y `DetProdP` (2 niveles); **confirmar** los 6 mensajes
  verbatim (§3.9), la extracción `substr(&BarCode,22,10)`, la validación `Sucursal = 0`, y los 13
  campos del SDT `Context`.
  *Sin dependencias.* · **Criterio de aceptación**: el volcado coincide carácter a carácter con
  `spec.md` §3; cualquier diferencia se corrige en `spec.md` **antes** de T-101.

- [ ] **T-002** **G-DATA — puerta de T-1xx.** Con `INFORMATION_SCHEMA.COLUMNS` + `KEY_COLUMN_USAGE` +
  `REFERENTIAL_CONSTRAINTS` de `Rentas` (**sólo `SELECT`**), determinar el nombre físico y el esquema
  exactos de: `CONTRIBU`, `DETPRODP`, `DETPROD2`; confirmar tipo, nulabilidad y PK de cada columna
  usada, y enumerar las columnas huérfanas.
  *Sin dependencias.* · **Criterio de aceptación**: queda escrito el nombre físico real de las 3
  tablas, la PK real de cada una, la FK `DETPRODP.PPTrataId → PPTrata.PPTrataId`, y la lista de
  columnas huérfanas. **Si la BD no responde**, T-002 queda bloqueado y **ninguna entidad puede
  escribirse**.

- [ ] **T-003** Verificar la conectividad con `devmssqlatp.ecom.com.ar:1433` desde el entorno de
  build/CI y dejar constancia del resultado (DNS, TCP y login).
  *Sin dependencias.* · **Criterio de aceptación**: T-002 puede ejecutarse, o queda documentado el
  bloqueo con su plan de contingencia.

- [ ] **T-004** Inventariar el estado real del módulo de puestos: presencia de `spring-boot-starter-thymeleaf`,
  existencia de `puestos/web/mvc/**`, `templates/puestos/**`, `static/js/**`, y el estado de la
  seguridad (CSRF, sesión) heredada de `03-login`.
  *Sin dependencias.* · **Criterio de aceptación**: el inventario queda en `plan.md` §1 y §4, y las
  tareas que dependen de un archivo existente se marcan como **dependencia de `03-login`**.

- [ ] **T-005** Confirmar que la seguridad de `03-login` (CSRF, sesión, `ContextoSesion`) está
  disponible para su reutilización en este módulo.
  *Sin dependencias.* · **Criterio de aceptación**: la verificación queda en `plan.md` §5 (D-CC-07/08/09).

---

## B. Backend

- [ ] **T-101** Crear la entidad `puestos.domain.Contribuyente` (`@Table(name="CONTRIBU")`) con las
  columnas de §2.1 de `plan.md`, incluyendo las columnas huérfanas, con `@Column(name="...")`
  exacto y `@ColumnTransformer(read="RTRIM(...)")` en los `char(n)`.
  *→ T-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con
  `INFORMATION_SCHEMA.COLUMNS`; los `char(n)` se leen con `RTRIM`. **Req**: R-CC-01, R-CC-16.
  **Plan**: §2.1.

- [ ] **T-102** Crear la entidad `puestos.domain.DetalleProductoPrimario`
  (`@Table(name="DETPRODP")`) con PK compuesta (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`,
  `DPPNroForm`) y las columnas de §2.2 de `plan.md`, incluyendo las huérfanas y la FK
  `PPTrataId`.
  *→ T-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con
  `INFORMATION_SCHEMA.COLUMNS`. **Req**: R-CC-01, R-CC-16. **Plan**: §2.2.

- [ ] **T-103** Crear la entidad `puestos.domain.PaseGuia` (`@Table(name="DETPROD2")`) con PK
  compuesta (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm`, `DPPPaseId`) y las
  columnas de §2.3 de `plan.md`.
  *→ T-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con
  `INFORMATION_SCHEMA.COLUMNS`. **Req**: R-CC-07. **Plan**: §2.3.

- [ ] **T-104** Crear `puestos.repository.ContribuyenteRepository` con `findByConCuit` y
  `puestos.repository.DetalleProductoPrimarioRepository` con `findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm`
  y consultas de filtrado por `DPPNroForm`.
  *→ T-101, T-102* · **Criterio de aceptación**: los repositorios compilan y las consultas se
  generan correctamente. **Req**: R-CC-04, R-CC-05. **Plan**: §2.

- [ ] **T-105** Crear `puestos.repository.PaseGuiaRepository` con `findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm`
  y `countBy...AndDppPaseEstAndSucTipo` (para el cálculo de `Duplicado`).
  *→ T-103* · **Criterio de aceptación**: el repositorio compila y la consulta de conteo se
  genera correctamente. **Req**: R-CC-11. **Plan**: §2.3.

- [ ] **T-106** Crear `puestos.service.CargaService` con el método `marcarGuia(...)` que implementa
  la lógica de §3.3 de `spec.md`: validación de guía anulada (`DPPMarcAnu = 1`), guía interna
  (`DPPClasif = 'I'`), actualización de cabecera, cálculo de `Duplicado`, inserción del pase
  (nivel 2), y apertura de `Form506Detalle` (modo `UPD`).
  *→ T-104, T-105* · **Criterio de aceptación**: el servicio implementa todas las ramas y devuelve
  el código de resultado correcto. **Req**: R-CC-07…R-CC-11. **Plan**: D-CC-02.

- [ ] **T-107** Implementar en `CargaService` el método de búsqueda de formularios por `DPPNroForm`
  (filtrado por barcode extraído).
  *→ T-104* · **Criterio de aceptación**: el método devuelve la lista filtrada. **Req**: R-CC-04, R-CC-05.

- [ ] **T-108** Crear `puestos.web.mvc.CargaMvcController` con `GET /cargas`, `POST /cargas/buscar`,
  `POST /cargas/marcar`, y navegación a `/cargas/detalle`, `/cargas/tabla`, `/cargas/pagos`.
  *→ T-106, T-107* · **Criterio de aceptación**: el controller compila y las rutas responden.
  **Req**: R-CC-01, R-CC-04, R-CC-07, R-CC-12, R-CC-14, R-CC-15. **Plan**: §4.1.

- [ ] **T-109** Crear `puestos.web.rest.CargaRestController` (opcional) con endpoints
  `GET /api/v1/cargas/formularios` y `POST /api/v1/cargas/marcar` si se decide exponer la API.
  *→ T-106* · **Criterio de aceptación**: los endpoints responden con el contrato definido en
  `plan.md` §3.2. **Req**: R-CC-16. **Plan**: §3.2.

- [ ] **T-110** Verificar que todos los repositorios del módulo son de sólo lectura
  (`@Transactional(readOnly=true)`) y que el `diff` **no** toca `spring.jpa.hibernate.ddl-auto`.
  *→ T-101, T-102, T-103* · **Criterio de aceptación**: el `git diff` no contiene `CREATE TABLE`/
  `ALTER TABLE` ni referencias a migraciones. **Plan**: G5, §5.5.

- [x] **T-111** Implementar en `CargaService` el cálculo de `Total General` (`DPPTotGral`) como
  `DPPAdCMTot + DPPAdIBTot + DPPAdicFle + DPPImCMTot + DPPImIBTot + DPPImpuFle`, exponiéndolo
  en el view model por fila (R-CC-21). No es columna física (no `@Column`).
  *→ T-102* · **Criterio de aceptación**: método unit-testeable con valores conocidos. **Req**: R-CC-21.
  **Plan**: §2.2.

- [x] **T-112** Agregar el campo `mostrarModal` (Boolean) a `CargaForm` y cablearlo en
  `CargaMvcController`: Buscar NO lo setea (false); `/detalle` y `/marcar` SÍ (true). Eliminar la
  inferencia `dppNroForm != null` (R-CC-23).
  *→ T-108* · **Criterio de aceptación**: tras Buscar el HTML no contiene el fragmento modal; tras
  `/detalle` y `/marcar`, sí. **Req**: R-CC-23, R-CC-05. **Plan**: §4.2.

---

## C. Vistas

- [x] **T-201** Crear `templates/puestos/control-cargas.html` con: campo de barcode, botón Buscar,
  grilla de formularios con columna Marcar (icono `selectRow`), botones Ver Detalle, Ver Tabla,
  Pagos, y validación de puesto (`Sucursal = 0` ⇒ mensaje + deshabilitación de Marcar).
  *→ T-108* · **Criterio de aceptación**: la vista renderiza con los elementos esperados.
  **Req**: R-CC-01, R-CC-02, R-CC-06. **Plan**: §4.1, §4.2.

- [x] **T-202** Crear `templates/puestos/fragments/form506-detalle.html` (modal) con: cabecera de la
  guía, visibilidad condicional de `TblComprador` y `TblProd` según `DPPTipoGui`, y mensajes
  `La guia ha sido marcada con exito` / `Guia Duplicada , ya paso por algun puesto`.
  *→ T-108* · **Criterio de aceptación**: el fragmento renderiza correctamente en ambos modos
  (`DSP`/`UPD`). **Req**: R-CC-12, R-CC-13. **Plan**: §4.1.

- [x] **T-203** Crear `static/js/control-cargas.js` con: autofocus del barcode, confirmación de
  marcado (`confirm('Esta seguro que desea Marcar el formulario?')`), y extracción de `DPPNroForm`
  del barcode (sólo visual, la lógica real está en el servicio).
  *→ T-201* · **Criterio de aceptación**: el JS **no** contiene `fetch`/`XMLHttpRequest`/`eval`
  ni lógica de negocio. **Req**: R-CC-03, R-CC-04. **Plan**: D-CC-03.

- [x] **T-204** Verificar el CSS: reutilizar `static/css/tokens.css` y `static/css/nucleo.css`
  (creados por `02-abm-productos`); agregar sólo las clases nuevas necesarias.
  *→ T-004, T-201* · **Criterio de aceptación**: `grep` sobre `static/css/**` no encuentra
  literales hexadecimales ni nombres de fuente fuera de `tokens.css`.

- [ ] **T-205** Verificar el comportamiento responsive de la vista a 375 px y 1440 px.
  *→ T-201* · **Criterio de aceptación**: inspección visual en los 2 anchos sin desbordes.

- [ ] **T-206** Aplicar los requisitos de accesibilidad AA: `<label for>` en cada control, foco
  visible, contraste ≥ 4,5:1, navegación por teclado.
  *→ T-201* · **Criterio de aceptación**: `axe-core` sin hallazgos críticos.

- [x] **T-207** Agregar control de cierre al modal `form506-detalle.html`: botón "Cerrar" y/o tecla
  Escape, implementado como JS puntual (vanilla) o navegación server-side a `/cargas` (R-CC-19).
  *→ T-202* · **Criterio de aceptación**: prueba que el modal desaparece al activar el cierre.
  **Req**: R-CC-19. **Plan**: §4.1.

- [x] **T-208** Agregar a `tokens.css` las custom properties `--atp-cargas-fieldset-border: #acacac`,
  `--atp-cargas-input-border: #d0d0d0`, `--atp-cargas-text-secondary: #868686`,
  `--atp-cargas-button-border: #262626`, y reutilizarlas en `control-cargas.css` sin literales
  (R-CC-24).
  *→ T-204* · **Criterio de aceptación**: `grep` sobre `control-cargas.css` no encuentra literales
  hexadecimales. **Req**: R-CC-24. **Plan**: §4.3.

- [x] **T-209** Refactorizar `control-cargas.html` a la paridad de grilla Figma: 7 columnas en el
  orden/anchos de R-CC-20, header fondo `--atp-button`, check verde 21×21 en Marcar, botones
  "Ver Tabla" y "Pagos" a nivel de BODY, CUIT con underline y clickeable (abre modal), Nro Guia
  clickeable (abre modal), Total General alineado a la derecha (R-CC-06, R-CC-12, R-CC-20).
  *→ T-201, T-111* · **Criterio de aceptación**: test de vista con 7 columnas en el orden indicado.
  **Req**: R-CC-06, R-CC-12, R-CC-20. **Plan**: §4.1, §4.2.

- [x] **T-210** Corregir el form de Marcar para enviar los 5 componentes de la PK compuesta
  (`conCuit`, `dppSucu`, `calPerFisc`, `dppSecu`, `dppNroForm`) como hidden inputs por fila
  (R-CC-22).
  *→ T-209* · **Criterio de aceptación**: test de vista que verifica los 5 hidden inputs por fila.
  **Req**: R-CC-22. **Plan**: §4.1.

---

## D. Verificación y puertas de calidad

- [ ] **T-301** Suite de vistas (`@WebMvcTest` + `MockMvc`): render de `/cargas` con los elementos
  esperados, validación de puesto, búsqueda por barcode, marcado (éxito/anulada/interna/duplicada),
  ver detalle, ver tabla, pagos. **≥ 15 pruebas nuevas**.
  *→ T-201, T-202, T-108* · **Criterio de aceptación**: `./mvnw clean test` en verde y **sin
  regresión** sobre las suites de `01-nucleo-datos`, `02-abm-productos` y `03-login`.
  **Req**: R-CC-01…R-CC-15.

- [ ] **T-302** Pruebas de los caminos de error: guía anulada, guía interna, guía duplicada, puesto
  no asignado; mensajes verbatim.
  *→ T-106, T-201* · **Criterio de aceptación**: el test compara el texto renderizado contra las
  cadenas literales de `spec.md` §3.9. **Req**: R-CC-02, R-CC-08, R-CC-09, R-CC-10.

- [ ] **T-303** Test unitario del cálculo de `Duplicado` (0, 1, 2 pases previos).
  *→ T-105* · **Criterio de aceptación**: el test verifica el valor devuelto. **Req**: R-CC-11.

- [ ] **T-304** Pruebas de integración contra `Rentas` (`ATP_DB_IT=true`, con rollback): marcado
  exitoso (verificación de inserción en `DETPROD2` y actualización de `DETPRODP`), guía anulada,
  guía interna, guía duplicada.
  *→ T-106, T-101, T-102, T-103* · **Criterio de aceptación**: `ATP_DB_IT=true ./mvnw test
  "-Dtest=*IT"` en verde y la BD **sin cambios** al terminar. **Req**: R-CC-07…R-CC-11.

- [ ] **T-305** Verificación de BD y de auditoría: confirmar que el marcado **escribe** (inserción en
  `DETPROD2`, actualización de `DETPRODP`) y que `ddl-auto` sigue en `none`.
  *→ T-110* · **Criterio de aceptación**: los conteos son correctos antes y después; la BD queda
  sin cambios al terminar (rollback). **Plan**: §5.5.

- [ ] **T-306** Prueba de navegador real: búsqueda por barcode, marcado con confirmación, cancelación
  de confirmación, ver detalle, ver tabla, pagos; captura de evidencia.
  *→ T-301* · **Criterio de aceptación**: evidencia del flujo completo.

- [ ] **T-307** **G-SEC**: escaneo de secretos en el diff; CSRF activo en los formularios; sin
  scripts o estilos inline con lógica; sin `fetch`/`XMLHttpRequest` en `control-cargas.js`.
  *→ T-108, T-203* · **Criterio de aceptación**: escaneo sin hallazgos.

- [ ] **T-308** **G5**: verificar que el diff **no** contiene DDL, migraciones, `schema.sql`/
  `data.sql`, Flyway/Liquibase ni cambios en `spring.jpa.hibernate.ddl-auto`.
  *→ T-110* · **Criterio de aceptación**: el diff no toca la sección `ddl-auto`.

- [ ] **T-309** Cerrar la matriz de paridad de `plan.md` §5.3 con la evidencia de cada prueba y
  registrar el resultado en Engram (`mem_save`, `topic_key: spec/08-control-cargas`).
  *→ T-301…T-308* · **Criterio de aceptación**: cada fila de la matriz referencia su prueba y su
  resultado.

- [ ] **T-310** Revisar y resolver los supuestos abiertos SA-CC-01…SA-CC-07 con los responsables y
  actualizar `spec.md` §6 y `plan.md` §6 con la resolución (o la decisión de postergar con motivo).
  *→ T-001…T-109* · **Criterio de aceptación**: cada supuesto tiene una resolución escrita o una
  decisión de postergar con motivo y fecha.

- [x] **T-311** Test MockMvc: Buscar NO renderiza el modal (mostrarModal=false) y `/detalle` y
  `/marcar` SÍ lo renderizan (R-CC-23, R-CC-05).
  *→ T-209, T-112* · **Criterio de aceptación**: assertions sobre presencia/ausencia del fragmento
  modal. **Req**: R-CC-05, R-CC-23.

- [x] **T-312** Test MockMvc: click en "Nro Guia" y en "Cuit" renderizan el modal con los 4
  parámetros (`dppNroForm`, `dpptipoGui`, `duplicado`, `conCuit`) (R-CC-12).
  *→ T-209* · **Criterio de aceptación**: assertions sobre los parámetros del modal. **Req**: R-CC-12.

- [x] **T-313** Test MockMvc: la grilla renderiza 7 columnas en el orden indicado (R-CC-20).
  *→ T-209* · **Criterio de aceptación**: assertions sobre headers/anchos. **Req**: R-CC-20.

- [x] **T-314** Test MockMvc: el form de Marcar incluye los 5 hidden inputs de PK por fila
  (R-CC-22).
  *→ T-210* · **Criterio de aceptación**: assertions sobre hidden inputs. **Req**: R-CC-22.

- [x] **T-315** Test unitario de `CargaService`: Total General = suma de los 6 componentes con
  valores conocidos (R-CC-21).
  *→ T-111* · **Criterio de aceptación**: test verde. **Req**: R-CC-21.

- [x] **T-316** Test de CSS: `control-cargas.css` no contiene literales hexadecimales (R-CC-24).
  *→ T-208* · **Criterio de aceptación**: `grep` sin hallazgos. **Req**: R-CC-24.

- [x] **T-317** Test MockMvc: el modal incluye control de cierre y desaparece al activarlo (R-CC-19).
  *→ T-207* · **Criterio de aceptación**: assertions sobre el botón "Cerrar"/Escape. **Req**: R-CC-19.

- [x] **T-318** Memoria Engram: al cerrar la corrección, ejecutar `mem_save` con
  `topic_key: spec/08-control-cargas` registrando objetos inventariados, decisiones tomadas y
  supuestos abiertos (R-CC-19..R-CC-24).
  *→ T-311…T-317* · **Criterio de aceptación**: observación guardada en Engram.

---

## E. G-POSTMAN y documentación

- [ ] **T-401** **G-POSTMAN**: si se agregan endpoints nuevos (`/api/v1/cargas/**`), agregar la
  carpeta `Cargas` a `docs/postman/ATPPuestos-API.postman_collection.json` con los requests
  correspondientes y su test de status + presencia de campos; actualizar `docs/postman/README.md`
  (§"Cobertura actual" y *Changelog*) **en el mismo commit** que el backend.
  *→ T-109* · **Criterio de aceptación**: la colección contiene la carpeta `Cargas` (si aplica);
  el commit incluye backend **y** colección. **Req**: R-CC-25. **Plan**: §3.2.
  > **Corrección 2026-11-03**: no se agregan endpoints nuevos; la colección **no** requiere
  > cambios en esta corrección.

- [ ] **T-402** Verificar que la colección **no** altera los requests preexistentes y que ningún
  endpoint del módulo cambió de contrato sin actualizar Postman.
  *→ T-401* · **Criterio de aceptación**: `git diff` de la colección muestra **únicamente** la
  carpeta `Cargas` (si aplica).

- [ ] **T-403** Actualizar `docs/analisis-ATPPuestos.md` con el resultado del G-DATA re-ejecutado
  (T-002): nombres físicos reales de `CONTRIBU`, `DETPRODP`, `DETPROD2`, columnas huérfanas.
  *→ T-002* · **Criterio de aceptación**: el documento refleja la evidencia de `INFORMATION_SCHEMA`.

---

## F. Orden de ejecución y paralelismo

### 6.1 Secuencia

```text
T-001, T-002, T-003, T-004, T-005   (paralelos entre sí; T-002/T-003 bloquean T-101..T-103)
        │
        ▼
T-101 ──┬── T-102 ──┐
        │            ├── T-104 ── T-106 ── T-108 ── T-201 ── T-202
        ├── T-103 ───┘         │            │
        │                      T-105 ──────┘
        │                      T-107 ──────┘
        │
        └── T-109 (opcional, si se exponen endpoints REST)
        │
        ▼
T-110  →  T-203 … T-206  →  T-301 … T-310  →  T-401 … T-403
```

### 6.2 Qué puede hacer `implementador-backend` y `implementador-vistas` en paralelo

| Implementador | Escribe **sólo** en | Puede empezar cuando |
|---|---|---|
| `implementador-backend` | `puestos/domain/**`, `puestos/repository/**`, `puestos/service/**`, `puestos/web/**`, `pom.xml` | T-002/T-004 cerradas (T-101..T-110) |
| `implementador-vistas` | `puestos/web/mvc/**` (sólo form objects y controller si los coordina), `templates/puestos/**`, `static/**` | T-004 cerrada y T-108 con el contrato de la vista definido (T-201…T-206) |

**Colisiones a evitar**:

| Archivo | Conflicto | Regla |
|---|---|---|
| `puestos/web/mvc/CargaMvcController.java` | Lo escriben los dos | **Backend** lo crea (T-108) con el contrato de vista completo; **vistas** sólo consume el nombre de vista y los nombres de atributo del modelo. |
| `puestos/web/mvc/form/CargaForm.java` | Lo escriben los dos | **Backend** lo crea (T-108) con los nombres de campo; **vistas** sólo usa `th:field`. |
| `templates/puestos/**` | Sólo vistas | El backend no crea plantillas. |
| `static/css/tokens.css`, `static/css/nucleo.css` | Los crea `02-abm-productos` | **Vistas** sólo **agrega** clases; si el archivo no existe, se crea en T-204 y se avisa al coordinador. |
| `docs/postman/ATPPuestos-API.postman_collection.json` | Sólo backend (T-401) | Las vistas **no** tocan la colección (G-POSTMAN). |
| `application.yml` (núcleo) | Sólo backend | **No** se toca `spring.jpa.hibernate.ddl-auto` (G5). |

### 6.3 Reglas de coordinación

| Regla | Detalle |
|---|---|
| 1 | Ninguna tarea de código arranca antes de la **aprobación de `spec.md` + `plan.md`** (puerta G-SPEC). |
| 2 | **T-002 es una puerta**: si el nombre físico de las tablas no queda confirmado, T-101..T-103 quedan bloqueados. |
| 3 | La lógica de negocio va **siempre** en `service/**`; ni el controller REST ni el MVC implementan reglas (AGENTS.md regla 9). |
| 4 | Los **mensajes verbatim** no se tocan, traducen ni capitalizan. |
| 5 | La KB es de **solo lectura**: T-001/T-002/T-003 son de lectura; nunca compilar, guardar, regenerar ni borrar objetos. |
| 6 | `tasks.md` se actualiza (con evidencia) **por el coordinador**, no por los implementadores. |
| 7 | **No regresión**: T-301 debe verificar que las suites de `01-nucleo-datos`, `02-abm-productos` y `03-login` siguen verdes. |
| 8 | **Sin DDL**: T-110 verifica que no hay cambios en `spring.jpa.hibernate.ddl-auto`. |

---

## H. Corrección de pantalla (registrada 2026-11-03)

Decisiones del usuario (Engram #198): modal desde "Nro Guia" + "Cuit", paridad de grilla Figma
(7 columnas), Total General derivado, PK completa en Marcar, flag dedicado `mostrarModal`, tokens
nuevos y botón de cierre del modal.

### H.1 Secuencia
```text
T-111, T-112  (backend: Total General, flag mostrarModal)
   │
   ▼
T-207, T-208, T-209, T-210  (vistas: cierre modal, tokens, paridad grilla, PK completa)
   │
   ▼
T-311 … T-317  (tests MockMvc/unit/CSS)
   │
   ▼
T-318  (mem_save Engram)
```

### H.2 Qué puede hacer cada implementador en paralelo
| Implementador | Escribe sólo en | Puede empezar cuando |
|---|---|---|
| `implementador-backend` | `puestos/service/CargaService.java`, `puestos/web/mvc/form/CargaForm.java`, `puestos/web/mvc/CargaMvcController.java` | T-111, T-112 (T-002/T-004 ya cerradas) |
| `implementador-vistas` | `templates/puestos/control-cargas.html`, `templates/puestos/fragments/form506-detalle.html`, `static/css/tokens.css`, `static/css/control-cargas.css`, `static/js/control-cargas.js` | T-209 con el contrato de vista de T-112 |

### H.3 Reglas de coordinación adicionales
| Regla | Detalle |
|---|---|
| 1 | Ningún literal hexadecimal en `control-cargas.css` (R-CC-24); usar tokens. |
| 2 | Buscar NO abre el modal ni setea `mostrarModal` (R-CC-05, R-CC-23). |
| 3 | El modal sólo se renderiza si `mostrarModal = true` (R-CC-23). |
| 4 | El form de Marcar envía los 5 componentes de la PK (R-CC-22). |
| 5 | `Total General` se calcula en `CargaService`, no es columna física (R-CC-21). |
| 6 | El botón "Cerrar" del modal es JS puntual vanilla (R-CC-19). |
| 7 | G-POSTMAN: no se agregan endpoints nuevos en esta corrección (R-CC-25 satisfecha sin diff). |

---

## G. Cierre del modulo (registrado 2026-10-30)

- **Build**: `./mvnw clean test` -> **BUILD SUCCESS** (nucleo 367 + puestos 33 = 400 tests, 0 fallos).
- **IT contra Rentas**: BLOQUEADO por entorno (IP privada 10.5.2.180, requiere VPN). Codigo compila y los 4 IT de CargaMarcadoIT estan implementados con rollback.
- **H-CC-01**: resuelto (mensajes con tilde en CargaService y tests).
- **H-CC-02**: resuelto (CargaServiceTest 9 casos + CargaMarcadoIT 4 casos).
- **G-POSTMAN**: carpeta Cargas sincronizada (2 requests).
- **ddl-auto**: none, sin DDL. Sin secretos. Configs < 500 lineas.
- **Commits**: feat(puestos) + docs(spec) + docs(verificacion).
- **Estado**: modulo **commiteado y estable** (pendiente validacion IT en entorno con VPN). Listo para continuar con otra funcionalidad.
