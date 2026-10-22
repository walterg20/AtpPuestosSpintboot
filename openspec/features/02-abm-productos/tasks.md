# Tasks — 02 ABM de Productos (Thymeleaf)

> Checklist verificable del módulo `02-abm-productos`. Cada tarea es **atómica** y trazable a un
> requisito de `spec.md` (R-PROD-*) o a un punto del `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` (puerta G-SPEC)** — guardarraíl G2.
> **Ninguna tarea está marcada como completada**: el cierre y la evidencia son del coordinador.
> Tareas **B** = backend (`nucleo/service`, `nucleo/web/rest`, `nucleo/web/dto`, `pom.xml`).
> Tareas **V** = vistas (`nucleo/web/mvc`, `templates/**`, `static/**`).
> Tareas **Q** = verificación y puertas de calidad.

**Leyenda de dependencias**: `→` significa "depende de". `Req:` = requisito de `spec.md`.
`Plan:` = sección de `plan.md`.

---

## A. Extracción y verificación (previa a la implementación)

- [x] **T-001** Releer `Transaction Productos` (`Structure` + `WebForm`), `Table Producto` y `Table Product1` por el MCP `genexus18mcp` (alias `atppuestos`, **sólo lectura**) y confirmar los rótulos verbatim de §3.1/§3.2 y las 4 fórmulas de `ProdIMax1..4`.
  *Sin dependencias.* · **Hecho**: el volcado de `Structure` coincide carácter a carácter con las tablas de `spec.md` §3.1/§3.2, incluidos `Importre C. Mec. Salida` y `Tipo de Producción`.
- [x] **T-002** Confirmar con `INFORMATION_SCHEMA.COLUMNS` / `KEY_COLUMN_USAGE` el esquema de `PRODUCTO` y `PRODUCT1` (8 + 9 columnas, PK, huérfanas, `IS_NULLABLE`, `COLUMN_DEFAULT`, sin `IDENTITY`) y las filas con `ProdImpu3`/`ProdImpu4` en `NULL`.
  *Sin dependencias.* · **Hecho**: el resultado de las consultas coincide con `plan.md` §2.1/§2.2/§3.5 y con la cifra de 293 vigencias nulas. **Sólo `SELECT`**.
- [x] **T-003** Confirmar que `Productos` **no** declara `Rules` ni `Events`, y que no existe una transacción `UMed` en la KB (`genexus_query "UMed"`).
  *Sin dependencias.* · **Hecho**: `Rules` y `Events` devuelven vacío; la búsqueda por `UMed` devuelve sólo los 2 atributos globales. Base de R-PROD-19.
- [x] **T-004** Verificar el estado de la capa de vista en el repositorio (existencia de `templates/layout/`, `static/css/`, `static/js/`, `web/mvc/**` y de la dependencia Thymeleaf en el `pom.xml` del núcleo).
  *Sin dependencias.* · **Hecho**: inventario documentado en `plan.md` §1.3 y confirmado en el commit de la tarea.

---

## B. Backend

- [x] **T-101** Añadir `spring-boot-starter-thymeleaf` al `pom.xml` de `atp-nucleo-compartido` en scope **`compile`** (**sin** `thymeleaf-layout-dialect`, SA-P09 / D-07 de `00-alcance`) y verificar el prefijo de resolución `classpath:/templates/`. **Prohibido** añadir Spring Security al núcleo, ni en `compile` ni en `test` (D-36: rompería la suite verde de `01-nucleo-datos`).
  *→ T-001* · **Hecho**: `./mvnw -pl atp-nucleo-compartido dependency:tree` muestra `spring-boot-starter-thymeleaf`; una plantilla de prueba resuelve.
- [x] **T-102** Reforzar la validación del DTO de escritura: `prodCod` con `@Min(0)`/`@Max(32767)` (rango del `smallint`) y los 4 importes con `@Digits(integer=10, fraction=2)` y límites `±9999999999.99` (`plan.md` D-32).
  *→ T-002* · **Hecho**: `POST /api/v1/productos` con `prodCod = 99999` responde `400 VALIDACION` (antes `500`); un importe con 3 decimales responde `400`. **Req**: R-PROD-07, R-PROD-08. ⚠️ **Cambia el contrato REST ⇒ actualizar Postman (T-404)**.
- [x] **T-103** Eliminar el **N+1** del listado de productos: cargar `vigencias` en una sola consulta (`@EntityGraph` / `join fetch`) sin alterar la forma del JSON.
  *→ T-002* · **Hecho**: el log de Hibernate muestra **una** consulta (o un número constante) para `GET /api/v1/productos` con 244 productos; el JSON es idéntico al anterior. **Req**: plan §8.
- [x] **T-104** Crear los form objects en `nucleo/web/mvc/producto/`: `ProductoForm`, `ProductoVigenciaForm`, `ProductoFilaView` (record), `ModoProducto` (ALTA/EDICION) y `FiltroAnulados` (TODOS/NO/SI), con las validaciones de `plan.md` §4.3.
  *→ T-101* · **Hecho**: las clases compilan; `ProductoForm` declara `@NotBlank @Size(max=25)` en `prodDesc`, `@Size(max=3)` en `uMedNroId`, `@Size(max=1)` en `prodAcop`, `@Min(0) @Max(32767)` en `prodCod`, y `ProductoVigenciaForm` declara `@NotNull` + `@Digits(integer=10, fraction=2)` en los importes y `@DateTimeFormat(iso=DATE)` en `prodFchVig`.
- [x] **T-105** Crear el **test de paridad de restricciones** entre `ProductoForm`/`ProductoVigenciaForm` y `ProductoEscrituraDto`/`ProductoVigenciaEscrituraDto` (mismas restricciones, mismos límites).
  *→ T-104, T-102* · **Hecho**: el test compara por reflexión los pares (restricción, atributo) de ambos lados y falla si difieren. **Req**: D-31.
- [x] **T-106** Crear `nucleo/web/mvc/ProductoMvcController` con las rutas de `plan.md` §4.1 (`GET /productos`, `POST /productos`, `POST /productos/{prodCod}`, `POST /productos/{prodCod}/eliminar`), **invocando `ProductoService`** (nunca HTTP a la propia API) y devolviendo la vista `nucleo/productos`.
  *→ T-104* · **Hecho**: `MockMvc` devuelve 200 + nombre de vista `nucleo/productos` en el GET; el controller no contiene ninguna referencia a `RestTemplate`/`WebClient`.
- [x] **T-107** Implementar el **mapeo `form → ProductoEscrituraDto` campo por campo** (`plan.md` D-30): `prodIMax1..4` y `uMedNombre` **quedan fuera**; `prodTipCal`/`tpPriID`/`prodAnula` se envían **ausentes** si están vacíos; los textos se recortan; `prodFchVig` se convierte a medianoche; los importes ausentes se normalizan a `0`; `vigencias` nunca es `null`.
  *→ T-106* · **Hecho**: test que captura el body y verifica (a) ausencia de `prodIMax1..4` y `uMedNombre`; (b) ausencia de `prodTipCal`/`tpPriID`/`prodAnula` con los campos vacíos; (c) `prodDesc` recortado; (d) `prodFchVig` = `T00:00:00`; (e) `prodImpu3 = 0` cuando la carga lo trajo ausente; (f) `vigencias: []` con la grilla vacía. **Req**: R-PROD-03, R-PROD-05, R-PROD-19, R-PROD-20, R-PROD-23, R-PROD-28, R-PROD-29.
- [x] **T-108** Implementar el manejo de errores en el controller MVC: capturar `ConflictException` y `NotFoundException` y **re-renderizar la vista** con `mensajeError` y el formulario conservado (antes de que lleguen al `GlobalExceptionHandler`).
  *→ T-106* · **Hecho**: test con `PK_DUPLICADA` y `VIGENCIA_DUPLICADA` ⇒ 200 con la vista, `mensajeError` presente y los valores enviados intactos. **Req**: R-PROD-11.
- [x] **T-109** Construir el **view model** de `plan.md` §4.2 (colección ordenada por `prodCod` ascendente, derivados, vecinos de navegación, URLs de acción, filtro activo) a partir de los servicios existentes.
  *→ T-106* · **Hecho**: test que con una colección desordenada verifica el orden ascendente y que `indiceAnterior`/`indiceSiguiente`/`primerCodigo`/`ultimoCodigo` son `null` en los extremos. **Req**: R-PROD-13, R-PROD-25.
- [x] **T-110** Verificar que el servicio no persiste `uMedNombre` ni `ProdIMax1..4` y que los defaults `0` de `ProdTipCal`/`TPPriID`/`ProdAnula` siguen vigente.
  *→ T-002* · **Hecho**: IT de alta que comprueba los valores persistidos en `PRODUCTO` y la ausencia de columnas correspondientes. **Req**: R-PROD-29, plan §2.4.
- [x] **T-111** Normalizar la **escala de los importes leídos** al tipo lógico `NUMERIC(12,2)` en el borde de lectura (entidad → DTO): `setScale(2, RoundingMode.HALF_UP)` sobre `ProdImpu1..4` (`plan.md` D-38). Sin esto, la columna física `money` (escala 4) entrega `12.0000` y el `@Digits(integer=10, fraction=2)` del DTO de escritura **rechaza** el valor ⇒ el ida y vuelta leer → editar → guardar se rompe en la API y en la pantalla. **No** relajar el `@Digits` ni cambiar la forma del JSON (sólo la escala textual del número).
  *→ T-102, T-302* · **Hecho**: test que toma una vigencia real con `ProdImpu1 = 12.0000` (escala 4 en BD), verifica que el DTO de lectura devuelve `12.00` y que ese mismo valor **pasa** la validación del DTO de escritura. **Req**: R-PROD-08, R-PROD-20. **Nota**: las **2 filas** con más de 2 decimales reales se redondean al centavo en el ida y vuelta (mismo efecto que el legacy al asignar a `NUMERIC(12,2)`) — desviación documentada en `plan.md` D-38.
- [x] **T-112** Implementar la **actualización diferencial** de las vigencias en `ProductoService.reemplazarVigencias` (`plan.md` D-39): las vigencias existentes con la misma `ProdFchVig` se **actualizan in situ** (conservando `ProdImpu5`, `ProdFeLog` y `ProdUsu`); las que no llegan en el body se **eliminan**; las nuevas se **insertan**. El conjunto final debe ser exactamente el enviado (R-PROD-21/SA-P08) y **no** puede perder las columnas legacy de auditoría.
  *→ T-111* · **Hecho**: IT que modifica un producto con vigencias reales y comprueba que (a) las filas existentes conservan `ProdUsu`/`ProdFeLog`/`ProdImpu5`, (b) una vigencia quitada se elimina, (c) una vigencia nueva se inserta, (d) el conjunto final es el enviado. **Req**: R-PROD-21, R-036, D-39.

---

## C. Vistas

- [x] **T-201** Crear `static/css/tokens.css` con las *custom properties* de `docs/design.md` §2.2, §3 y §4 (marca, neutros, semánticos, tipografía, espaciado, radio, sombras).
  *→ T-001* · **Hecho**: el archivo contiene los tokens documentados y **ningún** otro archivo de estilo escribe literales de color o tipografía. **Req**: R-PROD-17.
- [x] **T-202** Crear `static/css/nucleo.css` con los componentes del mapeo de `docs/design.md` §5: `.atp-table`, `.atp-btn` (+ `--primary`/`--ghost`), `.atp-alert` (+ variantes `info`/`warn`/`error`/`success`), `.atp-form`, `.atp-grid-2col`, `.atp-field`.
  *→ T-201* · **Hecho**: la hoja no contiene literales de color/tipografía (sólo `var(--atp-*)`).
- [x] **T-203** Crear `templates/layout/base.html` (layout base con `th:replace`/`th:insert`) que enlace `tokens.css`, `nucleo.css` y `js/confirmaciones.js`.
  *→ T-201* · **Hecho**: una página de prueba renderiza con el layout y el `<title>` parametrizable.
- [x] **T-204** Crear `templates/nucleo/productos.html`: título `Productos`, panel de colección (columnas `Código`, `Producto`, `Prod Anulado`), panel de registro con los **12 campos** y sus rótulos verbatim, sección de vigencias, botonera (`Guardar`/`Cancelar`/`Eliminar`), filtro de anulados, banner de error y estados `empty`/`error`/`sin selección`.
  *→ T-203, T-109* · **Hecho**: la vista renderiza con los 12 rótulos en el orden de `spec.md` §3.1 y el test de rótulos pasa. **Req**: R-PROD-01, R-PROD-02, R-PROD-12, R-PROD-26, R-PROD-29.
- [x] **T-205** Crear `templates/nucleo/fragments/producto-vigencias.html`: `<table class="atp-table">` con las **5 columnas verbatim** del nivel 1, el título `Level1` (SA-P02) con la marca de rótulo derivado, el **`<template id="plantilla-vigencia">`** con los marcadores `__INDEX__` y los botones `+ Fila` / `− Fila`.
  *→ T-204* · **Hecho**: la grilla renderiza las cabeceras `ProdFchVig`, `Importe Fijo Entrada`, `Imp.Cosech.Mec Entrada`, `Importe Fijo Salida`, `Imp.Cosech.Mec Salida`; el `<template>` está presente incluso con la grilla vacía. **Req**: R-PROD-04, R-PROD-24.
- [x] **T-206** Implementar el render de las filas con **nombres indexados**: cada input de la fila `i` emite `name="vigencias[i].<campo>"` e `id="vigencias_i_<campo>"`.
  *→ T-205* · **Hecho**: el HTML renderizado de un producto con 2 vigencias contiene exactamente `vigencias[0].prodFchVig` … `vigencias[1].prodImpu4` y **no** índices duplicados. **Req**: R-PROD-05, R-PROD-27, SA-P10.
- [x] **T-207** Crear `static/js/vigencias.js`: `+ Fila` clona `#plantilla-vigencia`, sustituye `__INDEX__` por el índice nuevo en `name`/`id`, **vacía** los valores de los inputs y la inserta al final; `− Fila` la quita y **renumera** los índices de las filas siguientes.
  *→ T-205* · **Hecho**: el archivo no contiene `fetch`, `XMLHttpRequest`, `eval` ni lógica de validación; prueba de navegador: agregar una fila produce `vigencias[1].prodFchVig` y quitar la fila del medio renumera las siguientes. **Req**: R-PROD-27, G6.
- [x] **T-208** Crear `static/js/confirmaciones.js` con el `confirm()` de la baja (diálogo nativo, sin librería).
  *→ T-203* · **Hecho**: cancelar la confirmación no emite el `POST` de baja. **Req**: R-PROD-14, `docs/design.md` §8.
- [x] **T-209** Marcar los campos derivados `ProdIMax1..4` y `UMedNombre` con el atributo `readonly` y con la leyenda visual de "derivado" en `UMedNombre`.
  *→ T-204* · **Hecho**: el HTML renderizado contiene `readonly` en los 5 controles y el texto derivado. **Req**: R-PROD-03, R-PROD-19, R-PROD-24.
- [x] **T-210** Renderizar los errores de binding junto a cada campo (`th:errors`) y el banner `.atp-alert--error` con `mensajeError`; deshabilitar `Guardar` cuando `tieneErrores` es `true`.
  *→ T-108* · **Hecho**: el test de errores encuentra el mensaje junto al control y `Guardar` con `disabled`. **Req**: R-PROD-10, R-PROD-11.
- [x] **T-211** Implementar los enlaces de navegación (`First`/`Prev`/`Next`/`Last`/`Select`) contra `GET /productos?prodCod=…`, deshabilitados en los extremos, y el filtro de anulados como 3 enlaces que preservan `prodCod`.
  *→ T-109* · **Hecho**: el test verifica la URL de cada enlace y el atributo deshabilitado en los extremos. **Req**: R-PROD-13, R-PROD-25, R-PROD-26.
- [x] **T-212** Marcar los formularios de escritura con `th:action` (⇒ token CSRF) y verificar su presencia en el HTML renderizado. **Dónde**: el test del token CSRF va en el módulo **desplegable** `atp-puestos-camineros` (`@WebMvcTest` del `ProductoMvcController` del núcleo), porque el núcleo **no** tiene Spring Security (D-36) y allí `th:action` no emite el campo `_csrf`. Verificar además que un POST **sin** token devuelve `403` y **con** token devuelve el `2xx` esperado. En el núcleo basta comprobar que el `action` del formulario apunta a la ruta MVC correcta.
  *→ T-204* · **Hecho**: el HTML renderizado incluye el input `_csrf`; POST sin token → `403`; POST con token → `2xx`. **Req**: R-PROD-22, G-SEC.
- [x] **T-213** Ajustar el layout a **responsive**: apilado de paneles por debajo de 960 px y scroll horizontal en la grilla de vigencias.
  *→ T-204* · **Hecho (fix H2, `implementador-vistas`)**: `.atp-panel { min-width: 0 }`, `@media (max-width: 960px) { .atp-paneles { grid-template-columns: minmax(0, 1fr) } }` y `.atp-grilla-scroll { position: relative }` (contenedor del texto oculto del `<th>` de acciones, que causaba el desborde residual). Medición en app real: **375 px** → `scrollWidth 360 ≤ 375` sin scroll de página y grilla `client 300 / scroll 720` (scroll interno ✔); **600 px** → `585 ≤ 600` y grilla `525 / 720` ✔; **1440 px** → `1425 ≤ 1440` y 2 columnas `453.656 / 907.344` idénticas a la línea de base. **Req**: R-PROD-16.
- [x] **T-214** Aplicar los requisitos de accesibilidad AA: `<label for>` en cada control, `<th scope="col">` en la grilla, foco visible, `aria-live` en el banner de error, contraste ≥ 4,5:1.
  *→ T-201, T-204* · **Hecho, y REABIERTA por el QA (H1) y re-cerrada**: la evidencia original (axe-core sin hallazgos) no se sostenía porque se había medido sólo el estado sin filas. **Fix aplicado por `implementador-vistas`**: cada control de la grilla emite un nombre accesible propio (`aria-label` indexado, p. ej. `"ProdFchVig fila 1"` / `"Importe Fijo Entrada fila 2"`) y `static/js/vigencias.js` reindexa el `aria-label` junto con `name`/`id` al agregar y al quitar filas (marcadores `__INDEX__` 0-based para `name`/`id` y `__FILA__` 1-based para el `aria-label`). Los 12 `<label>` del formulario raíz no se tocan. **Req**: R-PROD-17.

---

## D. Verificación y puertas de calidad

- [x] **T-301** Suite de vistas (`@WebMvcTest` + `MockMvc`): render de la vista, **12 rótulos verbatim** en orden, 5 cabeceras de la grilla, navegación, validaciones (rango de PK, decimales, unicidad de fecha), errores del backend y conservación del formulario. **≥ 25 pruebas nuevas**.
  *→ T-204, T-106* · **Hecho**: `./mvnw clean test` en verde y **sin** regresión sobre la suite existente de `01-nucleo-datos`. **Req**: R-PROD-02, R-PROD-04, R-PROD-06, R-PROD-07, R-PROD-08, R-PROD-10, R-PROD-11, R-PROD-13, R-PROD-18.
- [x] **T-302** Pruebas de integración contra `Rentas` (`ATP_DB_IT=true`, con rollback): alta de un producto **con una vigencia**; modificación **cambiando un importe** y comprobando que **las demás vigencias siguen en `PRODUCT1`**; baja con borrado de las hijas; lectura de una vigencia real con `ProdImpu3` en `NULL`.
  *→ T-301* · **Hecho**: `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` en verde y la BD sin cambios al terminar. **Req**: R-PROD-20, R-PROD-21.
- [x] **T-303** Prueba de navegador real contra el backend: alta de producto con una vigencia, modificación de un importe de la grilla, baja, navegación First/Prev/Next/Last, **alta y baja de filas de la grilla** y verificación de la persistencia en `PRODUCT1`.
  *→ T-301* · **Hecho**: evidencia del flujo completo y de la fila persistida. **Req**: R-PROD-01, R-PROD-04, R-PROD-18.
- [x] **T-304** Accesibilidad: `axe-core` in-page y `lighthouse_audit` en **dos** estados (inicial y edición).
  *→ T-214* · **Hecho (fix H1)**: FAIL original del QA (1 violación `label` `critical` sobre 140 nodos en el estado de edición). Tras el fix, `axe-core` 4.10.2 reporta **0 violaciones** en los dos estados (inicial y edición con 28 filas / 140 inputs) y el árbol de accesibilidad muestra los nombres correctos. La re-verificación formal por `verificador-qa` queda **diferida por decisión del usuario**; la evidencia es del `implementador-vistas` sobre la app real. **Req**: R-PROD-17.
- [x] **T-305** Verificar el **contrato REST sin cambios** salvo el refuerzo de validación de T-102: `git diff` sin cambios en `nucleo/web/rest/**` fuera de las anotaciones de validación.
  *→ T-102* · **Hecho**: el diff corresponde **únicamente** a `ProductoEscrituraDto`/`ProductoVigenciaEscrituraDto`. **Req**: R-PROD-15.
- [x] **T-306** **G-POSTMAN**: actualizar `docs/postman/ATPPuestos-API.postman_collection.json` si T-102 cambió el contrato (nuevos `400` por rango/decimales), **en el mismo commit**. Si no cambió, dejar constancia de que los 7 requests de `Productos` están intactos.
  *→ T-102, T-305* · **Hecho**: la colección refleja el contrato vigente y el commit contiene ambos cambios. **Req**: R-PROD-30.
- [x] **T-307** **G-SEC**: escaneo de secretos en el diff, CSRF activo en los formularios, sin entradas inline con lógica.
  *→ T-212* · **Hecho**: escaneo sin hallazgos; el token CSRF presente en el HTML renderizado.
- [x] **T-308** **G5**: verificar que el diff **no** contiene DDL, migraciones, `schema.sql`/`data.sql`, Flyway/Liquibase ni cambios de `ddl-auto`.
  *→ T-305* · **Hecho**: el diff no toca `application.yml` en la sección `spring.jpa.hibernate.ddl-auto`.
- [x] **T-309** Cerrar la **matriz de paridad** de `plan.md` §9 con la evidencia de cada prueba y registrar el resultado en Engram (`mem_save`, `topic_key: spec/02-abm-productos`).
  *→ T-301…T-308* · **Hecho**: cada fila de la matriz referencia su prueba y su resultado; el resumen queda guardado en Engram.
- [ ] **T-310** Revisar y resolver los **supuestos abiertos SA-P01…SA-P10** con el responsable y actualizar `spec.md` §7 y `plan.md` §6 con la resolución.
  *→ T-001…T-109* · **Hecho**: cada supuesto tiene una resolución escrita o una decisión de postergar con motivo.
- [x] **T-311** Verificar el **ida y vuelta completo** tras T-111: `GET` de un producto con vigencia real -> el DTO devuelve el importe con escala 2 -> la pantalla lo renderiza -> el `POST` de modificación con ese mismo valor **no** es rechazado con `400 VALIDACION` -> la vigencia queda persistida con el importe correcto (contra `Rentas`, con rollback). Incluir el caso de una de las **2 filas** con más de 2 decimales reales, verificando el redondeo documentado en `plan.md` D-38.
  *→ T-111, T-302* · **Hecho**: el ciclo completo pasa en verde y la BD queda sin cambios. **Req**: R-PROD-08, R-PROD-20, R-PROD-21.
- [x] **T-312** Verificar que una **modificación de producto NO pierde** las columnas legacy de auditoría (`ProdUsu`, `ProdFeLog`, `ProdImpu5`) de las vigencias existentes, contra `Rentas`, con rollback y con el conteo de filas antes/después (`PRODUCT1` = 1553).
  *→ T-112* · **Hecho**: el IT pasa en verde y la BD queda sin cambios. **Req**: R-036, D-39.

---

## E. Evidencia de cierre — Gate 2 (coordinación, 2026-10-22)

### Build central (evidencia propia, no reports de subagentes)

| Corrida | Comando | Resultado |
|---|---|---|
| Unitarias | `.\mvnw.cmd -B clean test` | **BUILD SUCCESS** — **179 pruebas** (174 núcleo + 5 `atp-puestos-camineros`), 0 fallos, 0 errores, 0 omitidas |
| Integración | `ATP_DB_IT=true .\mvnw.cmd -B -pl atp-nucleo-compartido test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"` | **BUILD SUCCESS** — **39/39 IT verdes** contra la BD real `Rentas` (`devmssqlatp.ecom.com.ar`), 0 fallos |

Pruebas nuevas que aportan la evidencia de las tareas marcadas: 30 de vistas
(`ProductoMvcControllerTest` 13, `ProductoMvcEscrituraTest` 12,
`ProductoFormParidadRestriccionesTest` 5), 24 de validación del DTO (`ProductoValidacionEscrituraTest`),
6 de servicio (`ProductoEscrituraServicioTest`), 5 de Thymeleaf (`ThymeleafNucleoTest`), 3 de N+1 en H2
(`ProductoListadoN1H2Test`), 2 estructurales (`ProductoRepositoryConsultaTest`), 4 de CSRF en el módulo
desplegable (`ProductoMvcCsrfTest`, T-212) y 7 de integración contra `Rentas`
(`ProductoListadoN1IT` 4 + `ProductoVigenciaImporteNuloIT` 3, T-302/T-103). Sin regresión sobre la
suite de `01-nucleo-datos`.

> **Corrección de un conteo propio**: la primera medición de la coordinación dio **139** pruebas; era
> un error del método de conteo (los `@Nested` se reportan por clase anidada y el XML del contenedor
> sólo cuenta sus pruebas directas). La cifra autoritativa es la del resumen de Maven: **174 + 5 = 179**.

### Puertas verificadas por la coordinación

| Puerta | Resultado |
|---|---|
| **T-305** | `git diff --stat -- '*/web/rest/*'` **vacío**: el contrato REST no cambió. |
| **T-306** | `ATPPuestos-API.postman_collection.json` **JSON válido**; carpeta `Productos` de 7 → 9 requests (los 7 felices **intactos**, +2 para los `400 VALIDACION`). Coherente con T-102. |
| **T-308** | Sin DDL, sin migraciones, sin `schema.sql`/`data.sql`, sin Flyway/Liquibase y sin cambios de `ddl-auto` en `src/main`. **Única excepción, a dictaminar por el QA**: `ProductoListadoN1H2Test` usa `ddl-auto=create-drop` contra un **H2 en memoria**, acotado a esa clase de test; no toca `Rentas` ni el esquema del proyecto. |
| **T-307** | Escaneo de secretos en el diff: **0 hallazgos**. Sin entradas inline con lógica. Token CSRF **verificado** en el módulo desplegable (T-212). |

### Corrección de un reporte erróneo

Dos subagentes reportaron *"la BD `Rentas` es inalcanzable: el nombre DNS no existe"*. **No se sostiene**:
`Resolve-DnsName devmssqlatp.ecom.com.ar` → `10.5.2.180`, y los 39 IT corren verdes contra la base real.
Hubo **un** timeout transitorio de conexión en la primera corrida de la suite IT (los `@SpringBootTest` con
perfil `dev`); el reintento pasó sin cambios en el código ⇒ no es una regresión.

### Tareas que quedan ABIERTAS (1) y por qué

| Tarea | Motivo |
|---|---|

| **T-310** | Resolución de los supuestos SA-P01…SA-P10 **con el responsable**; requiere intervención humana. |

### Tareas cerradas en esta pasada (evidencia QA / navegador real)

| Tarea | Evidencia |
|---|---|
| **T-303** | `docs/verificacion/02-abm-productos.md` §5 — PASS (flujo completo). |
| **T-309** | `docs/verificacion/02-abm-productos.md` §3 — PASS (matriz de paridad completa, 13/14 filas verdes). |
| **T-311** | `docs/verificacion/02-abm-productos.md` §2.4 — PASS (5 IT de ida y vuelta). |
| **T-312** | `docs/verificacion/02-abm-productos.md` §3.3 — PASS (auditoría de auditoría preservada). |

### Cierre de H1 y H2 (fix de `implementador-vistas`, 2026-10-22)

**H1 — accesibilidad de la grilla.** Cada control de las 5 columnas de `PRODUCT1` emite un nombre
accesible propio (`aria-label` indexado: `"<rótulo verbatim> fila <n>"`, 1-based). El
`<template id="plantilla-vigencia">` usa el marcador `__FILA__`; `static/js/vigencias.js` reindexa
el `aria-label` **junto con** `name`/`id` al agregar (`__FILA__` → índice+1) y al quitar filas
(`/fila \d+/` → índice+1), de modo que el nombre accesible siempre describe la posición real.
Vía elegida: `aria-label` y no `<label for>` por celda, porque `ProductoMvcControllerTest` exige
**exactamente 12** elementos `<label>` en el HTML renderizado (el formulario raíz) y agregar uno por
celda rompía ese test. **Ningún test fue modificado.** `axe-core` 4.10.2: **0 violaciones** en
inicial y en edición (28 filas / 140 inputs), antes 1 `label` `critical` × 140.

**H2 — responsive.** Tres reglas en `static/css/nucleo.css`: (1) `.atp-panel { min-width: 0 }`
—permite que el ítem del grid se encoja por debajo del `min-content` de la tabla de 720 px—;
(2) `@media (max-width: 960px) { .atp-paneles { grid-template-columns: minmax(0, 1fr) } }` —`1fr`
equivale a `minmax(auto, 1fr)` y su mínimo automático es el `min-content`—; (3)
`.atp-grilla-scroll { position: relative }` —el `<span class="atp-oculto">` del `<th>` de acciones es
`position: absolute` y, sin ancestro posicionado, se medía desde el borde de la tabla (679 px) y
alargaba el `scrollWidth` del documento a 681 px—. Medición en app real: 375 px → `360 ≤ 375`
(sin scroll de página) con grilla `client 300 / scroll 720`; 600 px → `585 ≤ 600` con grilla
`525 / 720`; 1440 px → `1425 ≤ 1440` y 2 columnas `453.656 / 907.344` (idénticas a la línea de base
del QA). Ningún literal de color/tipografía nuevo (sólo `var(--atp-*)`).

**Alcance del diff (verificado por la coordinación).** `git status --short` ⇒ sólo 3 archivos de
vistas (`templates/nucleo/fragments/producto-vigencias.html`, `static/js/vigencias.js`,
`static/css/nucleo.css`) + `tasks.md` (coordinación). Sin cambios en `web/rest/**`, `service/**`,
`domain/**`, `repository/**`, `pom.xml` ni `docs/`.

**Build de la coordinación (evidencia propia, posterior al fix).**

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\mvnw.cmd -B clean test     # EXIT=0
[INFO] Tests run: 191, Failures: 0, Errors: 0, Skipped: 0   <- atp-nucleo-compartido
[INFO] Tests run:   5, Failures: 0, Errors: 0, Skipped: 0   <- atp-puestos-camineros
[INFO] BUILD SUCCESS
```

**Total: 196 (191 + 5), 0 fallos, 0 errores, 0 omitidas.** Log: `…\Temp\opencode\coord-02-test.log`.

**Re-verificación formal diferida.** Por decisión del usuario **no** se lanza `verificador-qa` en
esta pasada: la evidencia de T-213/T-304 es la del `implementador-vistas` sobre la app real. Queda
pendiente una pasada de QA (o una prueba automatizada de regresión de a11y/responsive) en el backlog.

**Estado del módulo**: `VERIFICADO CON DEUDA REGISTRADA` — 41/42 tareas cerradas; **1 pendiente de
decisión humana (T-310)**. El usuario valida manualmente el flujo del ABM (T-303 PASS). Hallazgos
adicionales H3–H9 (CSRF vs `/api/**`, H4 pantalla no servible sin sesión previa, H5 Postman > 500
líneas, H6, H7, H8, H9) quedan registrados en `docs/verificacion/02-abm-productos.md` para el
backlog de `03-login`.