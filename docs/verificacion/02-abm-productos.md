# Informe de verificación QA — Módulo 02 «ABM de Productos» (Thymeleaf)

> **Rol**: verificador QA (read-only sobre `src/**` y `openspec/**`; este informe es lo único que escribo).
> **Fecha**: 2026-10-22 · **Repo**: `D:\Desrrollo\AtpPuestosSpintboot` · **Rama/commit**: `7fbd57a`
> **Spec verificada**: `openspec/features/02-abm-productos/{spec.md, plan.md, tasks.md}`
> **Veredicto global**: **FAIL** — 3 de 5 tareas asignadas al QA pasan; **T-304** y **T-213** fallan
> con hallazgos reproducibles. El resto de las puertas (suite, IT, paridad de BD, G-POSTMAN,
> paridad de rótulos) está en verde.

---

## 1. Veredicto por tarea

| Tarea | Veredicto | Evidencia |
|---|---|---|
| **T-303** — Prueba de navegador real (alta, modificación, baja, navegación, alta/baja de filas, persistencia) | **PASS** | §5 |
| **T-304** — `axe-core` in-page + `lighthouse_audit` en 2 estados | **FAIL** | §2.1 · §4-H1 |
| **T-213** — Responsive 375 px / 1440 px | **FAIL** (parcial) | §2.2 · §4-H2 |
| **T-312** — La modificación NO pierde `ProdUsu`/`ProdFeLog`/`ProdImpu5` | **PASS** | §3.3 |
| **T-309** — Cierre de la matriz de paridad de `plan.md` §9 | **PASS** | §3 |
| Suite autoritativa de unitarias | **PASS** | §2.3 |
| IT contra la BD real (`Rentas`) | **PASS** | §2.4 |
| Paridad de BD (nombres físicos, sin DDL, huérfanas) | **PASS** | §3.2 |
| G-POSTMAN (carpeta `Productos` con 9 requests) | **PASS con observación** | §3.2 · §4-H3 |
| Paridad de rótulos / comportamiento con el legacy | **PASS** | §3.1 |
| **Gate 2 general** (despliegue de la pantalla) | **FAIL parcial** | H4: `GET /productos` sin sesión previa devuelve **200 con la página de error** |

---

## 2. Detalle de las verificaciones

### 2.1 T-304 — Accesibilidad (`axe-core` in-page + `lighthouse_audit`)

Herramientas: MCP `chrome-devtools` → `evaluate_script` (inyección de `axe-core` **4.10.2** descargado de
cdnjs, ejecutado in-page) y `lighthouse_audit` (`mode: navigation`, `device: desktop`).
App real: `atp-puestos-camineros` (perfil `dev`, BD `Rentas` real), autenticada con la seguridad por
defecto de Spring Boot (form login).

#### Estado 1 — inicial (`GET /productos`, modo ALTA, grilla vacía)

| Auditoría | Resultado |
|---|---|
| `axe.run(document)` | **`totalViolations: 0`** — sin hallazgos |
| `lighthouse_audit` | **Accessibility: 100** · Best Practices 100 · SEO 90 · Agentic Browsing 100 |
| Audits en rojo | 1, **de SEO**: `meta-description` (fuera del alcance de R-PROD-17) |

Reportes: `…/Temp/opencode/lh-inicial/report.json` · `…/Temp/opencode/lh-inicial/report.html`

#### Estado 2 — edición (`GET /productos?prodCod=1`, 28 vigencias)

| Auditoría | Resultado |
|---|---|
| `axe.run(document)` | **`totalViolations: 1`** — `label`, **impacto `critical`**, **140 nodos** |
| `lighthouse_audit` | **Accessibility: 93** · Best Practices 100 · SEO 90 · Agentic Browsing 50 |
| Audits en rojo | 3: `label` **[accessibility]**, `meta-description` [seo], `agent-accessibility-tree` [agentic-browsing] |

Nodos reportados (Lighthouse, 10 de 140):

```
tbody > tr > td > input#vigencias_0_prodFchVig
tbody > tr > td > input#vigencias_0_prodImpu1
tbody > tr > td > input#vigencias_0_prodImpu2
tbody > tr > td > input#vigencias_0_prodImpu3
tbody > tr > td > input#vigencias_0_prodImpu4
… (idéntico para las filas 1..27)
Fix any of the following: Element does not have an implicit (wrapped) <label>
Element does not have an explicit <label> aria-label attribute does not exist or is empty
aria-labelledby attribute does not exist …
```

> Los **140** nodos = 28 filas × 5 inputs. Los `<input>` de la grilla **no tienen nombre accesible**:
> el `<th scope="col">` da nombre a la columna, pero el control dentro de la celda queda sin
> `label`/`aria-label`/`aria-labelledby`. El formulario raíz (12 campos) **sí** tiene `<label for>`:
> axe no reporta ninguno de ellos.

**⇒ T-304 = FAIL.** El criterio de `spec.md` §8.5 («`axe-core` in-page en dos estados sin hallazgos
críticos») **no** se cumple en el estado de edición. La afirmación de `tasks.md` T-214 («`axe-core` sin
hallazgos críticos en los dos estados») **no se sostiene**: el estado inicial no tiene filas de grilla,
por lo que no hay inputs que auditar; el defecto sólo aparece cuando hay vigencias.

Reportes: `…/Temp/opencode/lh-edicion/report.json` · `…/Temp/opencode/lh-edicion/report.html`

### 2.2 T-213 — Responsive (inspección visual 375 px y 1440 px)

Método: `chrome-devtools` → `emulate { viewport: "375x812x1" | "1440x900x1" }` (viewport exacto en CSS
px; `resize_page` no baja de ~500 px por el mínimo de ventana de Chrome) + `take_screenshot` +
medición de `getComputedStyle`/`getBoundingClientRect`.

| Ancho | Paneles apilados | `overflow-x` de la grilla | `clientWidth` vs `scrollWidth` de `.atp-grilla-scroll` | Desborde horizontal de la **página** |
|---|---|---|---|---|
| 1440 px | **no** (2 columnas: `453.656px 907.344px`) | `auto` | 871 / 871 (no hace falta scroll) | 0 (`scrollWidth` 1425 ≤ 1440) |
| 960 px | sí | `auto` | 885 / 885 | 0 |
| 800 px | sí | `auto` | 725 / 725 | 0 |
| 600 px | sí | `auto` | **720 / 720** | **168 px** |
| **375 px** | **sí** ✔ | `auto` | **720 / 720** | **393 px** |

Medición cruda a 375 px (estado actual del código, sin ningún hack):

```json
{ "viewport": 375, "docScrollWidth": 768, "bodyScrollWidth": 768,
  "maxRight": 768,
  "peor": "SECTION.atp-panel atp-panel--coleccion right=768 w=756 padre=DIV.atp-paneles",
  "grillaClient": 720, "grillaScroll": 720, "scrollInterno": false }
```

Capturas: `…/Temp/opencode/resp-375.png` (se ve la barra de scroll horizontal inferior y la columna
«Prod Anulado» cortada) y `…/Temp/opencode/resp-1440.png` (correcto).

**Diagnóstico (confirmado, no supuesto)**

* Lo que **sí** funciona: por debajo de 960 px el `@media (max-width: 960px)` de
  `static/css/nucleo.css` apila los paneles (`grid-template-columns: 1fr`) y el formulario de 12
  campos pasa a 1 columna. El `meta name="viewport" content="width=device-width, initial-scale=1"`
  **está** en `templates/layout/base.html` (línea 18) ✔.
* Lo que **no** funciona: `.atp-grilla-scroll { overflow-x: auto }` + `.atp-table { min-width: 720px }`
  **nunca producen scroll interno** a 375/600 px, porque el `min-width: 720px` de la tabla se propaga
  como *min-content* al ítem del grid (`.atp-panel` tiene `min-width: auto` por defecto) ⇒ el track
  del grid mide 756 px ⇒ **el panel se desborda de la página** y el scroll horizontal ocurre a nivel
  de **página**, no de grilla. Medido: `grillaClient === grillaScroll === 720` en 375 y 600 px.
* **Prueba de causa**: inyectando en vivo `.atp-panel { min-width: 0 }` (sin tocar el repo) el
  contenedor pasa a `grillaClient 300 / grillaScroll 720` ⇒ **`scrollInterno: true`**. El mecanismo
  previsto por el diseño existe y funciona; lo que falta es que el ítem del grid pueda encogerse
  (`min-width: 0` en `.atp-panel` o `minmax(0, 1fr)` en el track del `@media`).
  Queda un desborde residual del documento (768 → 681) que hay que atacar además (contención de la
  tabla de la colección, que no tiene contenedor de scroll).

**⇒ T-213 = FAIL parcial.** La primera mitad del requisito («apilar los paneles por debajo de
960 px») se cumple; la segunda («mantener la grilla de vigencias utilizable mediante scroll
horizontal») **no**: a 375 px la grilla no scrollea y la página desborda 393 px.
R-PROD-16 queda incumplido.

### 2.3 Suite autoritativa de unitarias

```powershell
cd D:\Desrrollo\AtpPuestosSpintboot
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\mvnw.cmd -B clean test
```

Salida (resumen de Maven — **fuente de verdad**; no se usaron los `.txt` de surefire ni la suma de XML):

```
[INFO] Tests run: 191, Failures: 0, Errors: 0, Skipped: 0     <- atp-nucleo-compartido
[INFO] Tests run:   5, Failures: 0, Errors: 0, Skipped: 0     <- atp-puestos-camineros
[INFO] BUILD SUCCESS
[INFO] ATPPuestos-NucleoCompartido .... SUCCESS [ 18.134 s]
[INFO] ATPPuestos-PuestosCamineros .... SUCCESS [  9.937 s]
```

**Total: 196 (191 + 5) — 0 fallos, 0 errores, 0 omitidas.** La cifra **191 = 186 núcleo + 5
`atp-puestos-camineros`** del enunciado se confirma como 191 del módulo núcleo; el total del reactor
es 196. Log: `…/Temp/opencode/qa-02-unit.log`.

### 2.4 IT contra la base real `Rentas`

```powershell
$env:ATP_DB_IT="true"
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

```
[INFO] Tests run: 51, Failures: 0, Errors: 0, Skipped: 0      <- línea de base 51/51 ✔
[INFO] BUILD SUCCESS
[INFO] ATPPuestos-NucleoCompartido .... SUCCESS [ 45.189 s]
```

Incluye `T-112 - Actualizacion diferencial de las vigencias (D-39)` (2 tests),
`T-311 - Ida y vuelta completo de los importes de vigencia (D-38)` (5),
`T-103 - Listado de Productos sin N+1` (4), `T-302 - Vigencia real con importe NULL` (3),
`T-111` (5), `T-407` (9). Log: `…/Temp/opencode/qa-02-it.log`.

---

## 3. Matriz de paridad de `plan.md` §9 (T-309) — fila por fila

| # | Legacy (KB / BD) | Nuevo (EARS) | Prueba ejecutada | Resultado | Evidencia |
|---|---|---|---|---|---|
| 1 | `Transaction Productos` — 12 atributos raíz | R-PROD-02 | `ProductoMvcControllerTest` («los 12 rótulos del nivel 0 se renderizan verbatim y en orden») **+** navegador: lectura de `.atp-panel--registro label.atp-field__etiqueta` | **PASS** | Navegador devolvió **exactamente**, y en este orden: `Código`, `Producto`, `Tipo de cálculo`, `Tipo de Producción`, `Importe de Entrada`, `Importe C. Mec. Entrada`, `Importe Salida`, `Importre C. Mec. Salida`, `Código`, `Unidad de Medida`, `Acopiador?`, `Prod Anulado` — incluidos el typo `Importre`, la `P` mayúscula y los dos `Código` (`spec.md` §3.1) |
| 2 | `Table Producto` — 8 columnas + `ProdReppa` huérfana | §2.1 | Inspección de `domain/Producto.java` + `ProductoRepositoryIT` (2 tests verdes en la suite IT) | **PASS** | `@Table(name="PRODUCTO")` con `ProdCod`, `ProdDesc`(+`RTRIM`), `ProdTipCal`, `TPPriID`, `UMedNroId`, `ProdAcop`, `ProdReppa`(+`RTRIM`), `ProdAnula` = **8 columnas físicas exactas**; `ProdIMax1..4` y `UMedNombre` son `@Transient` |
| 3 | `Table Product1` — 9 columnas + 3 huérfanas | §2.2 | Inspección de `domain/ProductoVigencia.java` + `ProductoRepositoryIT` | **PASS** | `@Table(name="PRODUCT1")`; `ProdCod` (`@MapsId`+`@JoinColumn`), `ProdFchVig` (`@EmbeddedId`), `ProdImpu1..5`, `ProdFeLog`, `ProdUsu`(+`RTRIM`) = **9 columnas**; huérfanas `ProdImpu5`/`ProdFeLog`/`ProdUsu` **mapeadas** |
| 4 | Fórmulas `ProdIMax1..4` | R-PROD-03 | `ProductoMvcControllerTest` («los 4 derivados vienen de importes-vigentes y son de sólo lectura») **+** navegador | **PASS** | En `/productos?prodCod=1`: `prodIMax1=9.00`, `prodIMax2=10.00`, `prodIMax3=11.00`, `prodIMax4=12.00`; los 5 controles con `readonly:true`; `uMedNombre` presente y vacío con la marca «derivado» |
| 5 | `UMedNombre` sin columna | R-PROD-19 | `ProductoMvcEscrituraTest` («`prodIMax1..4` y `uMedNombre` quedan fuera del cuerpo de escritura») **+** navegador | **PASS** | HTML: `<input id="uMedNombre" readonly>` + leyenda «derivado»; el cuerpo de escritura no lleva la clave (verificado por el test y por el alta real: `PRODUCTO` no tiene la columna y el INSERT no la menciona) |
| 6 | `simplegrid GridProductos_Level1Item` | R-PROD-04 | `ProductoMvcControllerTest` («la grilla del nivel 1 muestra las 5 cabeceras verbatim») **+** navegador **+** prueba de fila agregada/quitada | **PASS** | Cabeceras leídas del DOM: `ProdFchVig`, `Importe Fijo Entrada`, `Imp.Cosech.Mec Entrada`, `Importe Fijo Salida`, `Imp.Cosech.Mec Salida`; sección `#titulo-level1` = **`Level1`** (SA-P02); `ProdFchVig` con `class="atp-th-derivado"` (marca «(derivado)» vía `::after`, D-20/R-PROD-24) |
| 7 | `NavButtons` First/Prev/Next/Last | R-PROD-13 | `ProductoMvcControllerTest` («los vecinos de navegación…», «en el primer y el último… `null`») **+** navegador (clicks reales) | **PASS** | Navegador: `First` → `prodCod=1` (Prev `disabled`), `Next` → `2`, `Prev` → `1`, `Last` → `32000` (Next `disabled`). Ver §5.4 |
| 8 | `FormButtons` Enter/Cancel/Delete | R-PROD-01, R-PROD-14 | `ProductoMvcControllerTest` / `ProductoMvcEscrituraTest` (Alta / Modificación / Baja) **+** navegador | **PASS** | Ver §5.1, §5.2, §5.5 |
| 9 | `errorviewer` | R-PROD-11 | `ProductoMvcEscrituraTest` («un conflicto del backend se muestra inline y conserva el formulario») **+** navegador | **PASS** | Navegador: alta con `prodCod=1` (existente) ⇒ banner `.atp-alert--error` con **«Ya existe un producto con ProdCod 1.»**, `role="alert"`, `aria-live="assertive"`, formulario conservado (`prodDesc="QA DUPLICADO 1"`), **sin escritura** en BD |
| 10 | `DATE(8)` sobre `datetime` (SA-09) | R-PROD-23 | `ProductoMvcEscrituraTest` («la vigencia viaja anidada y con la fecha a medianoche») **+** navegador + consulta SQL | **PASS** | BD: `ProdFchVig = 2026-01-15 00:00:00.000`, `2026-03-15 00:00:00.000` (medianoche); en pantalla `dd/MM/yyyy` (`15/01/2026`) y `<input type="date">` con ISO |
| 11 | `NUMERIC(12,2)` lógico sobre `money` físico | R-PROD-08 | `ProductoValidacionEscrituraTest` (24 tests) + `ProductoImporteEscalaIT` (5) + `T-311 - Ida y vuelta…` (5) | **PASS** | `400 VALIDACION` con `$.code = "VALIDACION"` para 3 decimales y para valores fuera de ±9999999999.99; escala normalizada a 2 decimales en el borde de lectura (D-38) |
| 12 | Sin `IDENTITY` | R-PROD-09 | `ProductoMvcEscrituraTest` («Alta válida…») + navegador | **PASS** | Alta real por la pantalla con `prodCod=32000` ⇒ la fila se creó **con esa PK** (no autogenerada): `SELECT … FROM PRODUCTO WHERE ProdCod=32000` devolvió la fila |
| 13 | `PRODUCT1` sin FK física a `PRODUCTO` | R-PROD-01 (baja en cascada por JPA) | Navegador (baja) + `ProductoMvcEscrituraTest` («Baja: redirige a GET /productos») | **PASS** | Tras la baja: `PRODUCTO WHERE ProdCod=32000` = 0 filas **y** `PRODUCT1 WHERE ProdCod=32000` = 0 filas (cascada JPA `CascadeType.ALL` + `orphanRemoval`) |
| 14 | `docs/design.md` §5/§6/§7/§8 | R-PROD-16, R-PROD-17, R-PROD-27 | `axe-core` + `lighthouse` en 2 estados (T-304) · inspección 375/1440 (T-213) · revisión del JS | **FAIL parcial** | **R-PROD-27 ✔**: `static/js/vigencias.js` no contiene `fetch`/`XMLHttpRequest`/`eval`/validación (la única coincidencia de `fetch` es la frase de la prohibición en el comentario, línea 11). **R-PROD-17 ✘**: 140 nodos `label` críticos en edición. **R-PROD-16 ✘**: la grilla no scrollea a 375/600 px y la página desborda 393/168 px |

### 3.1 Paridad de rótulos con el legacy (`spec.md` §3.1 / §3.2) — **PASS**

| Elemento | Legacy (KB) | Renderizado | Veredicto |
|---|---|---|---|
| Título de pantalla | `textblock Title caption="Productos"` | `<h1>Productos</h1>` | ✔ |
| Título de sección nivel 1 | `textblock TitleLevel1 caption="Level1"` | `<h3 id="titulo-level1">Level1</h3>` | ✔ |
| 12 rótulos nivel 0 | ver `spec.md` §3.1 | ver fila 1 de la matriz | ✔ verbatim y en orden |
| 5 rótulos nivel 1 | ver `spec.md` §3.2 | ver fila 6 | ✔ verbatim |
| Columnas de la colección | `Código`, `Producto`, `Prod Anulado` | `<th scope="col">` iguales | ✔ |
| Botonera | `Guardar` / `Cancelar` / `Eliminar` (SA-P03) | idénticos | ✔ |
| Filtro de anulados | `Todos` / `No` / `Sí` | idénticos | ✔ |
| Navegación | `First` / `Prev` / `Next` / `Last` | idénticos | ✔ |

### 3.2 Paridad de BD, G5 y G-POSTMAN — **PASS**

* **Nombres físicos exactos**: `PRODUCTO` (8) y `PRODUCT1` (9) mapeados con `@Column(name=…)` =
  nombre físico, sin `@Table` inventado. Sin DDL: `spring.jpa.hibernate.ddl-auto: none`
  (`application.yml:34`), sin `schema.sql`/`data.sql`/Flyway/Liquibase/migraciones en el diff
  (`git diff 6d85b3c..HEAD --name-only` ⇒ ninguna coincidencia).
* **Columnas huérfanas cubiertas**: `PRODUCTO.ProdReppa` ✔, `PRODUCT1.ProdImpu5` / `ProdFeLog` /
  `ProdUsu` ✔ (las tres mapeadas y **preservadas** por la modificación — ver §3.3).
* **Sin DDL en el código de tests**: `ProductoListadoN1H2Test` usa `ddl-auto=create-drop` contra un
  **H2 en memoria** acotado a esa clase; no toca `Rentas` ni el esquema del proyecto.
* **G-POSTMAN**: `docs/postman/ATPPuestos-API.postman_collection.json` es JSON válido; la carpeta
  `Productos` tiene **9 requests** = los **7 felices** intactos
  (`GET /api/v1/productos`, `GET ?anulados=false`, `GET /{prodCod}`, `GET /{prodCod}/importes-vigentes`,
  `POST`, `PUT /{prodCod}`, `DELETE /{prodCod}`) **+ 2 de `400 VALIDACION`**
  (`prodCod` fuera de rango → 99999; importe con 3 decimales → 100.001). Las URLs coinciden 1:1 con
  los `@*Mapping` de `web/rest/ProductoController`. `docs/postman/README.md` también se actualizó.
* **G-SEC**: 0 secretos en el diff; `.env` **no** está versionado (`git ls-files --error-unmatch .env`
  → *did not match any file(s) known to git*).
* **Contrato REST sin cambios de forma**: `git diff --stat 6d85b3c..HEAD -- '*/web/rest/*'` devuelve
  **sólo** `ProductoValidacionEscrituraTest.java` (+204, un **test**). El código productivo de
  `web/rest/**` no cambió; el refuerzo de validación de T-102 está en `web/dto/`
  (`ProductoEscrituraDto` +9, `ProductoVigenciaEscrituraDto` +19) ⇒ la **forma** del payload es la
  misma ⇒ G-POSTMAN coherente.

### 3.3 T-312 — La modificación NO pierde `ProdUsu`/`ProdFeLog`/`ProdImpu5` — **PASS**

Corrida aislada del IT (evidencia textual del propio test):

```powershell
.\mvnw.cmd -B -pl atp-nucleo-compartido test "-Dtest=ProductoVigenciaDiferencialIT" …
```

```
[T-112] conteo inicial (fuera de la transaccion): PRODUCTO=244, PRODUCT1=1553
[T-112] producto real elegido por consulta: ProdCod=347
[T-112] vigencias antes (5): [2011-03-12, 2017-12-15, 2017-12-27, 2018-04-24, 2018-05-17]
[T-112] vigencia quitada: 2017-12-15T00:00
[T-112] vigencia nueva:   2017-12-26T00:00
[T-112] vigencia 2011-03-12T00:00 -> antes: ProdUsu=SEBAM, ProdFeLog=2011-03-11T11:15:57, ProdImpu5=null
                              | despues: ProdUsu=SEBAM, ProdFeLog=2011-03-11T11:15:57, ProdImpu5=null
[T-112] vigencia 2017-12-27T00:00 -> antes: ProdUsu=SEBAM, ProdFeLog=2017-12-26T10:01:31, ProdImpu5=0.0000
                              | despues: ProdUsu=SEBAM, ProdFeLog=2017-12-26T10:01:31, ProdImpu5=0.0000
[T-112] vigencia 2018-04-24T00:00 -> … ProdUsu=SEBAM, ProdFeLog=2018-04-24T08:12:27, ProdImpu5=0.0000
[T-112] vigencia 2018-05-17T00:00 -> … ProdUsu=SEBAM, ProdFeLog=2018-05-16T09:54:14, ProdImpu5=0.0000
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Además, en el mismo IT: la vigencia **quitada** (2017-12-15) se elimina, la **nueva**
(2017-12-26) se inserta con `ProdUsu`/`ProdFeLog`/`ProdImpu5` en `NULL` (SA-P11, pendiente de
`03-login`) y el conjunto final es exactamente el enviado (R-PROD-21 / SA-P08). Conteo final
`PRODUCTO=244, PRODUCT1=1553` ⇒ **rollback confirmado**.

---

## 4. Hallazgos (ordenados por severidad)

### H1 — ALTA · Accesibilidad: los 140 controles de la grilla de vigencias no tienen nombre accesible

* **Síntoma**: en el estado de edición, `axe-core` reporta **1 violación `label` de impacto `critical`
  sobre 140 nodos** (28 filas × 5 inputs) y Lighthouse baja la accesibilidad a **93/100**.
  Afecta a `vigencias[i].prodFchVig`, `prodImpu1..4` de **todas** las filas, incluidas las que
  agrega el JS.
* **Evidencia**: `…/Temp/opencode/lh-edicion/report.html` (audit `label`, 140 nodos);
  `…/Temp/opencode/lh-inicial/report.html` (mismo audit, **0** nodos ⇒ el defecto sólo se ve con
  filas). Ejemplo de nodo:
  `<input class="atp-field__control" type="date" value="1753-01-01" name="vigencias[0].prodFchVig" id="vigencias_0_prodFchVig">`
* **Causa raíz probable**: en `templates/nucleo/fragments/producto-vigencias.html` los `<input>` de
  la fila (líneas 32–72) y los del `<template id="plantilla-vigencia">` (líneas 88–107) no llevan
  `label for`, `aria-label` ni `aria-labelledby`. El `<th scope="col">` nombra la **columna**, pero
  no el **control**. La T-214 se cerró midiendo sólo el estado sin filas.
* **Viola**: R-PROD-17 («asociar un `<label>` a cada campo del formulario **y a cada columna de la
  grilla**»), `spec.md` §8.5, `docs/design.md` §7.
* **A quién corresponde**: **implementador-vistas**. El arreglo debe incluir la fila-plantilla y el
  renumerado del `static/js/vigencias.js` (que ya reindexa `name`/`id`), p. ej. un `aria-label` con
  el índice de fila reindexado por el JS, o un `<label>` oculto por celda.

### H2 — MEDIA · Responsive: a 375/600 px la grilla de vigencias no scrollea y la página desborda

* **Síntoma**: a 375 px el documento desborda **393 px** horizontalmente (`scrollWidth` 768 vs
  `innerWidth` 375) y `.atp-grilla-scroll` **nunca** scrollea (`clientWidth == scrollWidth == 720`).
  A 600 px: desborde de 168 px, mismo síntoma. El scroll ocurre a nivel de página, no de grilla.
* **Evidencia**: `…/Temp/opencode/resp-375.png` (barra de scroll horizontal; la columna «Prod Anulado»
  queda cortada) y mediciones de §2.2.
* **Causa raíz probable**: `static/css/nucleo.css` — `.atp-table { min-width: 720px }` dentro de
  `.atp-grilla-scroll { overflow-x: auto }`, sobre ítems de grid con `min-width: auto` por defecto:
  el `min-content` de la tabla (720 px) fija el track del grid en 756 px ⇒ el panel excede el
  viewport. **Prueba de causa**: inyectando `.atp-panel { min-width: 0 }` en vivo, la grilla pasa a
  `clientWidth 300 / scrollWidth 720` ⇒ `scrollInterno: true` (el mecanismo existe; falta poder
  encogerse). Queda un desborde residual (768 → 681) por la tabla de la colección, que no tiene
  contenedor de scroll.
* **Viola**: R-PROD-16 («mantener la grilla de vigencias utilizable mediante scroll horizontal»).
* **A quién corresponde**: **implementador-vistas**. Fix mínimo: `min-width: 0` en `.atp-panel` (o
  `grid-template-columns: minmax(0, 1fr)` en el `@media (max-width: 960px)`) + contención de la tabla
  de la colección.

### H3 — MEDIA · La API REST no es invocable por HTTP con la seguridad por defecto; la colección Postman no define autenticación

* **Síntoma**: `POST /api/v1/productos` con Basic auth y **sin** token CSRF ⇒ **HTTP 401**
  (`WWW-Authenticate: Basic realm="Realm"`), no 403. Con **sesión** (navegador) el mismo POST sin
  token ⇒ **403** ✔ (comportamiento correcto, ver §5.6). En consecuencia, las peticiones de
  escritura de la colección Postman **no son ejecutables** contra la app tal como está.
* **Evidencia**: `curl -u user:*** -X POST http://localhost:8080/api/v1/productos -d '{…}'` ⇒
  `HTTP=401`; en el navegador, `fetch('/productos', {method:'POST', body:'…'})` sin `_csrf` ⇒
  **403**. `docs/postman/ATPPuestos-API.postman_collection.json`: `auth` de colección = **vacío**,
  `auth` de request = **vacío**, variables = sólo `baseUrl` y códigos.
* **Causa raíz probable**: la seguridad por defecto de Spring Boot (D-37) deja CSRF activo para
  **todas** las rutas, incluido `/api/**`; y como el `CsrfFilter` corre **antes** que el
  `BasicAuthenticationFilter`, un POST con autenticación Basic todavía es anónimo cuando el CSRF
  corta ⇒ `AccessDeniedHandlerImpl` delega en el `AuthenticationEntryPoint` ⇒ **401** en lugar de 403.
* **Viola / impacto**: no es un defecto del módulo 02 (la seguridad es de `03-login`), pero afecta la
  **ejecutabilidad de la colección Postman** (G-POSTMAN) y el criterio de aceptación 7 de
  `spec.md` §8.
* **A quién corresponde**: **coordinator** (encargar a `03-login`): el `SecurityFilterChain` debe
  autenticar la API y **excluir `/api/**` de CSRF** (o documentar el flujo), y la colección Postman
  debe declarar la autenticación que se use.

### H4 — MEDIA-ALTA · `GET /productos` sin sesión previa devuelve **200 con la página de error** (sin `<form>`)

* **Síntoma**: un `GET /productos` autenticado por **HTTP Basic** y **sin cookie de sesión** devuelve
  **HTTP 200** pero el cuerpo es la **página de error** (`ERROR_INTERNO`, «Ocurrió un error interno no
  inesperado», path `/productos`): **no hay ningún `<form>` ni el campo `_csrf`** en el HTML servido.
  Reproducible **4/4** veces. En el log, cada petición deja **3 trazas ERROR**.
* **Evidencia**:
  ```
  curl -u user:*** http://localhost:8080/productos
    HTTP=200 SIZE=56449  cierraHtml=True  <form>=0  name="_csrf"=False
    (el cuerpo termina en: <h1 class="atp-error__code">ERROR_INTERNO</h1> … <p class="atp-error__path">/productos</p>)

  C:\Users\walterg20\AppData\Local\Temp\opencode\qa-02-app2.log
  ERROR … org.thymeleaf.TemplateEngine : [THYMELEAF][http-nio-8080-exec-6] Exception processing template
         "nucleo/productos": Error during execution of processor
         'org.thymeleaf.spring6.processor.SpringActionTagProcessor' (template: "nucleo/productos" - line 103, col 56)
  Caused by: java.lang.IllegalStateException: Cannot create a session after the response has been committed
         at org.apache.catalina.connector.Request.doGetSession(Request.java:2822)
         at org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.saveToken(...)
  ERROR … GlobalExceptionHandler : Error interno no controlado
  ERROR … HttpMessageNotWritableException: No converter for [class java.util.LinkedHashMap]
         with preset Content-Type 'text/html;charset=UTF-8'
  ```
  La línea 103 col 56 de `templates/nucleo/productos.html` es
  `<form class="atp-form" th:object="${producto}" th:action="${accionFormulario}" method="post">`.
* **Causa raíz probable**: `HttpSessionCsrfTokenRepository` **difiere** el guardado del token CSRF hasta
  el primer acceso; ese acceso ocurre en el `th:action`. Para entonces la respuesta **ya está
  confirmada** (committed): la página pesa ~56 KB y el panel de colección (244 filas) se renderiza
  **antes** del formulario ⇒ se supera el buffer de respuesta de Tomcat (8 KB) ⇒ Tomcat hace flush y
  confirma la respuesta ⇒ `request.getSession()` ya no puede crear la sesión ⇒ `IllegalStateException`.
  Al no poder fijarse el estado, el cliente recibe **200** en lugar de **500**.
* **Alcance**: **no** afecta al flujo previsto (navegador + login por formulario): en el navegador la
  sesión ya existe ⇒ `request.getSession()` la devuelve ⇒ el `th:action` renderiza el `_csrf`
  correctamente (verificado: `csrfPresente: true` en todas las pruebas de navegador). **Sí** afecta a
  cualquier cliente **sin sesión previa** (HTTP Basic, o la primera petición antes de que exista
  cookie) y deja ruido ERROR en el log.
* **Viola / impacto**: R-PROD-22 («formularios `POST` con `th:action` (token CSRF activo)») se cumple
  en el flujo de navegador, pero la pantalla **no es servible** sin sesión; y el código de estado
  incorrecto (200 para un error) es un defecto del camino de render de error
  (`shared/error/GlobalExceptionHandler`, módulo 01) que no puede corregir el estado ya confirmado.
* **A quién corresponde**: **coordinator** (decidir el arreglo, que es casi todo de `03-login`):
  definir un `SecurityFilterChain` con `CookieCsrfTokenRepository` (sin sesión) o materializar el
  token CSRF **antes** de que la respuesta se confirme (p. ej. `CsrfTokenRepository` + acceso
  anticipado, o renderizar el input `_csrf` al principio de la plantilla).
  **implementador-vistas** puede mitigarlo desde la plantilla (p. ej. forzar la materialización del
  token antes del panel de colección), lo que además eliminaría el error en el log.

### H5 — BAJA · La colección Postman supera las 500 líneas (regla G4)

* **Síntoma**: `docs/postman/ATPPuestos-API.postman_collection.json` tiene **1990** líneas (> 500).
* **Evidencia**: `(Get-Content … | Measure-Object -Line).Lines` = 1990; antes del módulo 02
  (`git show 6d85b3c:…`) = **1898** ⇒ el módulo 02 añadió **92** líneas (2 requests + README).
  Ningún otro archivo de configuración de `src/main` supera 500 líneas.
* **Causa raíz probable**: la colección monolítica de todos los catálogos.
* **A quién corresponde**: **coordinator** (decidir si G4 aplica a artefactos de `docs/` y, en su
  caso,_split_ por módulo o moves a `docs/postman/`).

### H6 — BAJA · Observación de seguridad (no bloqueante para este módulo)

* **Síntoma**: un `POST` **con** HTTP Basic y **sin** token CSRF devuelve **401** (no 403); con
  sesión autenticada devuelve **403** (correcto). Comportamiento esperado de Spring Security 6,
  no del código del módulo.
* **Evidencia**: ver H3 y H4.
* **A quién corresponde**: **coordinator** → `03-login` debe contemplarlo al redactar el
  `SecurityFilterChain` (si la API se autentica con Basic, conviene un `SecurityFilterChain` propio
  para `/api/**` con CSRF desactivado).

### H7 — INFO · `ProductoEscrituraDto` conserva `uMedNombre` y los ejemplos de Postman lo envían

* **Síntoma**: `web/dto/ProductoEscrituraDto` declara `@Size(max = 40) String uMedNombre`
  (documentado como «informativo/derivado») y los bodies de ejemplo de Postman lo incluyen
  (`"uMedNombre": "Unidad"`).
* **Veredicto**: **no es una violación**. R-PROD-19 aplica a la **UI**, y el controller MVC excluye el
  campo **por construcción** (verificado por `ProductoMvcEscrituraTest` y por el alta real en BD).
* **A quién corresponde**: **coordinator** (alinear los ejemplos de Postman con el contrato real para
  no inducir a error) — **no bloquea**.

### H8 — INFO · `git diff --stat -- '*/web/rest/*'` no está vacío (sólo por un archivo de test)

* `ProductoValidacionEscrituraTest.java` (+204) está bajo `web/rest/`. El **código productivo** de
  `web/rest/**` no cambió ⇒ T-305 se sostiene. Se deja constancia para que el filtro del gate no
  produzca un falso positivo.

### H9 — INFO · Hibernate detecta SQL Server 10.0 y avisa que el mínimo es 11.0

* **Síntoma**: en cada arranque, `HHH000511: The 10.0.0 version for
  [org.hibernate.dialect.SQLServerDialect] is no longer supported … The minimum supported version is
  11.0.0`. El servidor real es **SQL Server 2019 (15.0.2170.1)**; el driver `mssql-jdbc 12.10` con
  `encrypt=true;trustServerCertificate=true` reporta la versión 10.0.
* **Impacto**: hoy **ninguno** observable (los 51 IT verdes y la prueba de navegador funcionan), pero
  es un riesgo latente de compatibilidad de Hibernate 6.6 con este driver/configuración.
* **Evidencia**: `…\opencode\qa-02-app2.log`, línea de arranque `2026-10-22T16:25:41.052 … WARN …
  o.h.engine.jdbc.spi.SqlExceptionHelper` / `HHH000511`.
* **A quién corresponde**: **coordinator** (evaluar en `03-login` o en la configuración de datasource
  si conviene fijar `hibernate.dialect` explícito o cambiar los parámetros de conexión).

---

## 5. Evidencia de la prueba de navegador (T-303) — **PASS**

App: `atp-puestos-camineros` empaquetado (`package -DskipTests`), perfil `dev`, BD real `Rentas`.
Autenticación: seguridad por defecto de Spring Boot (form login con la clave autogenerada del log).
Herramienta: MCP `chrome-devtools` (`navigate_page`, `fill`, `click`, `evaluate_script`,
`handle_dialog`). **Zona reservada usada: `prodCod` 32000** (32000–32010), verificada vacía antes de
empezar y **borrada al final**.

### 5.1 Alta de producto con una vigencia (y alta de filas de la grilla)

1. `GET /productos` ⇒ modo ALTA, `accionFormulario=/productos`, 0 filas de grilla, `prodCod`
   editable, `_csrf` presente.
2. `+ Fila` ×2 ⇒ el DOM queda con `vigencias[0].prodFchVig … vigencias[0].prodImpu4` y
   `vigencias[1].prodFchVig … vigencias[1].prodImpu4` — **índice correcto, sin duplicados**
   (R-PROD-27 / SA-P10).
3. Relleno **con eventos de entrada reales**: `prodCod=32000`, `prodDesc="QA Producto 32000"`,
   `prodTipCal=1`, `tpPriID=2`, `uMedNroId=KG`, `prodAcop=N`, `prodAnula` **vacío**;
   fila 0 = `2026-01-15` / `10.50` / `3.80` / `0` / `0`; fila 1 = `2026-02-15` / `20.25` / `4.40` / `0` / `0`.
4. `Guardar` ⇒ redirección a `GET /productos?prodCod=32000`, modo EDICIÓN, `prodCod` `readonly=true`,
   botón `Eliminar` presente (`/productos/32000/eliminar`).
5. **BD** (`SELECT … WHERE ProdCod=32000`):

   ```
   PRODUCTO : 32000 | QA Producto 32000 | ProdTipCal=1 | TPPriID=2 | UMedNroId=KG | ProdAcop=N | ProdAnula=0 | ProdReppa=NULL
   PRODUCT1 : 2026-01-15 00:00:00.000 | 10.5000 | 3.8000 | .0000 | .0000 | ProdImpu5=NULL | ProdFeLog=NULL | ProdUsu=NULL
              2026-02-15 00:00:00.000 | 20.2500 | 4.4000 | .0000 | .0000 | NULL | NULL | NULL
   ```

   ⇒ PK enviada por el cliente ✔ (sin `IDENTITY`), `ProdAnula` ausente en el body y defaulted a `0`
   en el servidor ✔ (R-PROD-29), fechas a medianoche ✔ (R-PROD-23), columnas legacy de las filas
   nuevas en `NULL` ✔ (SA-P11).

### 5.2 Modificación de un importe de la grilla + baja de una fila de la grilla

1. Se cambió el importe de la fila 0 de `10.50` a **`99.99`** (input real).
2. `+ Fila` ⇒ tercera fila `vigencias[2].*` con valores vacíos; se completó con `2026-03-15` /
   `30.00` / `5.55`.
3. `- Fila` sobre la fila **del medio** (`2026-02-15`) ⇒ la fila `2026-03-15` **se renumeró de índice 2
   a índice 1** (`vigencias[1].prodFchVig = 2026-03-15`), sin huecos ⇒ R-PROD-27 / `plan.md` §4.5 ✔.
4. `Guardar` ⇒ `GET /productos?prodCod=32000` con 2 filas; **BD**:

   ```
   2026-01-15 00:00:00.000 | 99.9900 | 3.8000 | .0000 | .0000
   2026-03-15 00:00:00.000 | 30.0000 | 5.5500 | .0000 | .0000
   ```

   ⇒ el importe modificado persistió, la fila quitada se eliminó y la nueva se insertó.

### 5.3 Verificación de la persistencia en `PRODUCT1` — incluida arriba (consulta SQL directa)

### 5.4 Navegación First / Prev / Next / Last (clicks reales)

| Acción | URL resultante | `prodCod` mostrado | Vecinos |
|---|---|---|---|
| (estado) | `/productos?prodCod=32000` | 32000 | First→1, Prev→483, **Next `disabled`**, Last→32000 |
| `First` | `/productos?prodCod=1&anulados=` | 1 (Algodón, 28 vigencias) | **Prev `disabled`**, Next→2 |
| `Next` | `/productos?prodCod=2&anulados=` | 2 (Caña De Azúcar, 12 vigencias) | Prev→1, Next→3 |
| `Prev` | `/productos?prodCod=1&anulados=` | 1 | Prev `disabled` |
| `Last` | `/productos?prodCod=32000&anulados=` | 32000 | **Next `disabled`** |

⇒ R-PROD-13 (extremos deshabilitados) y R-PROD-25 (orden ascendente por `prodCod`) ✔.
El filtro de anulados se verificó aparte: `anulados=` → **244** filas, `anulados=false` → **183**,
`anulados=true` → **61** (idéntico a `GET /api/v1/productos?anulados=true` ⇒ 61) ✔ R-PROD-26.
> Los conteos 244/183/61 se tomaron del panel de colección, que se renderiza **antes** del `<form>`
> ⇒ son válidos incluso en la respuesta truncada de H4 (verificado: 244 enlaces `?prodCod=` presentes
> en esa respuesta).

### 5.5 Baja con confirmación

* `Eliminar` con `confirm()` **cancelado** ⇒ sin POST ⇒ `PRODUCTO` 32000 = 1 fila y `PRODUCT1` = 2
  filas (no se borró nada) ⇒ R-PROD-14 ✔.
* `Eliminar` con `confirm()` **aceptado** ⇒ redirección a `/productos`, formulario vacío, botón
  `Eliminar` ausente ⇒ `PRODUCTO` 32000 = **0**, `PRODUCT1` 32000 = **0** (cascada JPA) ✔.

### 5.6 CSRF y errores inline (complemento de T-303)

* Con sesión autenticada: `POST /productos` sin `_csrf` ⇒ **403**; `POST /productos/32000/eliminar`
  sin `_csrf` ⇒ **403** ✔ (R-PROD-22).
* Alta con `prodCod=1` (ya existente) ⇒ banner inline **«Ya existe un producto con ProdCod 1.»**,
  `role="alert"`, `aria-live="assertive"`, formulario conservado, **sin escritura** ✔ (R-PROD-11).
* Alta con dos vigencias de la **misma** `ProdFchVig` ⇒ banner inline **«La fecha de vigencia
  2026-05-01T00:00 está repetida.»**, **sin escritura** (`PRODUCTO` 32000 = 0 filas) ✔ (R-PROD-06).

---

## 6. Estado de la BD al terminar

Consultas propias con `sqlcmd` (sólo `SELECT` salvo las filas de la zona reservada creadas y borradas
por la propia prueba de navegador):

```
PRODUCTO                       244   (= línea de base)
PRODUCT1                      1553   (= línea de base)
PRODUCT1 con ProdUsu NOT NULL  1116   (= línea de base)
PRODUCT1 con ProdFeLog NOT NULL 1116 (= línea de base)
PRODUCT1 con ProdImpu5 NOT NULL 764  (= línea de base)
PRODUCTO  con ProdCod 32000..32010   0
PRODUCT1  con ProdCod 32000..32010   0
```

**No queda ninguna fila de la zona reservada 32000–32010.** Ningún producto real fue modificado
(comprobado: `PRODUCTO WHERE ProdCod=1 AND ProdDesc='QA DUPLICADO 1'` = 0 filas).
Los IT que corrí usan `@Transactional` ⇒ rollback (verificado por el propio IT: conteo final
`PRODUCTO=244, PRODUCT1=1553`).

---

## 7. Trazabilidad a los requisitos de `spec.md` §4

| Requisito | Veredicto | Prueba |
|---|---|---|
| R-PROD-01 Pantalla ABM | PASS | navegador §5 |
| R-PROD-02 12 rótulos verbatim | PASS | navegador + `ProductoMvcControllerTest` |
| R-PROD-03 derivados sólo lectura | PASS | navegador + test |
| R-PROD-04 grilla anidada | PASS | navegador §5.1–5.2 |
| R-PROD-05 envío anidado | PASS | navegador + BD §5.1 |
| R-PROD-06 unicidad `ProdFchVig` | PASS | navegador §5.6 |
| R-PROD-07 rango de la PK | PASS | `ProductoMvcEscrituraTest` + `ProductoValidacionEscrituraTest` (400) |
| R-PROD-08 decimales | PASS | `ProductoValidacionEscrituraTest` (24) |
| R-PROD-09 PK en el body / sólo lectura | PASS | navegador §5.1 |
| R-PROD-10 validación en línea | PASS | `ProductoMvcEscrituraTest` |
| R-PROD-11 mapeo de errores | PASS | navegador §5.6 + test |
| R-PROD-12 estados de la vista | PASS | `ProductoMvcControllerTest` (colección vacía) |
| R-PROD-13 navegación | PASS | navegador §5.4 |
| R-PROD-14 confirmación de baja | PASS | navegador §5.5 |
| R-PROD-15 contrato REST sin cambios | PASS | §3.2 (diff) |
| **R-PROD-16 responsive** | **FAIL** | §2.2 / H2 |
| **R-PROD-17 accesibilidad AA** | **FAIL** | §2.1 / H1 |
| R-PROD-18 paridad del CRUD | PASS | §5 |
| R-PROD-19 `UMedNombre` sin persistencia | PASS | §3.2 + test |
| R-PROD-20 normalización de importes | PASS | navegador (`0` donde la BD es `NULL`) + `ProductoVigenciaImporteNuloIT` |
| R-PROD-21 reemplazo total | PASS | navegador §5.2 + `ProductoVigenciaDiferencialIT` |
| R-PROD-22 CSRF | PASS (con salvedad H4) | navegador §5.6 + `ProductoMvcCsrfTest` |
| R-PROD-23 fecha a medianoche | PASS | navegador + BD |
| R-PROD-24 rótulo derivado | PASS | `Level1` + `ProdFchVig (derivado)` |
| R-PROD-25 orden determinista | PASS | navegador §5.4 |
| R-PROD-26 filtro de anulados | PASS | navegador (244/183/61) |
| R-PROD-27 alcance del JS | PASS | revisión del JS (sin `fetch`/XHR/validación) + navegador §5.1/5.2 |
| R-PROD-28 recorte de textos | PASS | `ProductoForm`/`ProductoMapper` + test |
| R-PROD-29 enteros opcionales ausentes | PASS | navegador §5.1 (`prodAnula` vacío → `0` en BD) |
| R-PROD-30 sincronización Postman | PASS | §3.2 |

---

## 8. Artefactos de la verificación

| Artefacto | Ruta |
|---|---|
| Log de unitarias | `C:\Users\walterg20\AppData\Local\Temp\opencode\qa-02-unit.log` |
| Log de IT | `C:\Users\walterg20\AppData\Local\Temp\opencode\qa-02-it.log` |
| Log del IT T-112 (T-312) | `C:\Users\walterg20\AppData\Local\Temp\opencode\qa-02-t312.log` |
| Log de arranque de la app | `C:\Users\walterg20\AppData\Local\Temp\opencode\qa-02-app.log` |
| Lighthouse estado inicial | `…\opencode\lh-inicial\report.{json,html}` |
| Lighthouse estado edición | `…\opencode\lh-edicion\report.{json,html}` |
| Capturas responsive | `…\opencode\resp-375.png`, `…\opencode\resp-1440.png` |
| `axe-core` 4.10.2 | `…\opencode\axe.min.js` (cdnjs) |
| HTML de la pantalla | `…\opencode\productos.html` |

## 9. Recomendación al coordinador

1. **Reabrir T-214** (la evidencia de cierre no se sostiene) y asignar **H1** a
   `implementador-vistas`; exigir la medición `axe-core` **con filas de grilla** (estado de edición)
   antes de volver a cerrar.
2. Asignar **H2** a `implementador-vistas` (responsive).
3. Cargar **H3** en el backlog de `03-login` (CSRF vs `/api/**` + autenticación de la colección
   Postman) y decidir **H5** (G4 vs `docs/`).
4. **H4** es el hallazgo que **no** estaba en el radar y conviene decidirlo con `03-login`: la
   pantalla no es servible por HTTP Basic (ni sin cookie de sesión) porque el token CSRF se materializa
   tarde, con la respuesta ya confirmada. Si `03-login` va a usar sesiones + formulario (lo
   previsible), el flujo queda cubierto; si va a usar tokens/stateless, **hay que** cambiar el
   `CsrfTokenRepository` o materializar el token antes del render.
5. **T-309 puede cerrarse**: la matriz de `plan.md` §9 está completa en §3 de este informe, con
   evidencia por fila. **13 de 14 filas en verde**; la fila 14 (`docs/design.md` §5/§6/§7/§8) queda
   en rojo parcial por H1 (R-PROD-17) y H2 (R-PROD-16) — R-PROD-27 sí está verde.
6. `tasks.md` §E dice «30/38 tareas cerradas»; con T-303, T-309 y T-312 verdes y T-213/T-304 en rojo,
   el estado real del módulo es **32 cerradas / 2 fallidas / 4 abiertas** (T-310 requiere decisión
   humana).
