# Plan — 02 ABM de Productos (Thymeleaf)

> **Estado**: `PROPUESTA` — pendiente de aprobación en la puerta **G-SPEC**.
> Acompaña a `spec.md` (misma puerta). Documento de arquitectura; no contiene tareas (ver `tasks.md`).
> Contrato REST **vigente**: `openspec/features/01-nucleo-datos/spec.md` R-020…R-037 y `plan.md` §3.
> Paridad de BD: `openspec/features/00-alcance/plan.md` §3 y `docs/analisis-ATPPuestos.md` §13.

---

## 1. Estrategia y ubicación

### 1.1 Qué produce este módulo

La **capa de presentación** del ABM de `PRODUCTO` + `PRODUCT1`. El backend ya existe y **no se
rediseña**: este módulo lo **consume**.

```text
KB (legacy, sólo lectura)  →  spec EARS  →  [backend YA EXISTE]  →  web/mvc + templates + static
                                                   ↑
                                   ProductoService / ProductoRepository (núcleo)
```

### 1.2 Dónde vive el código

| Artefacto | Ubicación | Razón |
|---|---|---|
| Controller MVC | `atp-nucleo-compartido/src/main/java/ar/com/atp/puestos/nucleo/web/mvc/ProductoMvcController.java` | La pantalla es un **catálogo del núcleo** ⇒ `templates/nucleo/` (`00-alcance/plan.md` §5). |
| Form objects | `.../nucleo/web/mvc/producto/` (`ProductoForm`, `ProductoVigenciaForm`) | Los DTOs de formulario **no** van en `web/dto/**` (`00-alcance/plan.md` §2, D-05). |
| Plantilla | `atp-nucleo-compartido/src/main/resources/templates/nucleo/productos.html` | Prefijo de módulo (D-03): evita la colisión silenciosa del resolver de Thymeleaf. |
| Fragmento de grilla | `.../templates/nucleo/fragments/producto-vigencias.html` | Permite reutilizar la grilla y su fila-plantilla. |
| Layout | `.../templates/layout/base.html` | Layout base y fragmentos reutilizables (núcleo). |
| CSS | `.../static/css/tokens.css`, `.../static/css/nucleo.css` | Tokens + componentes (`docs/design.md` §2.2–§5). |
| JS | `.../static/js/vigencias.js`, `.../static/js/confirmaciones.js` | Inventario permitido por `docs/design.md` §8. |

> **El módulo desplegable `atp-puestos-camineros` no se toca**: hereda el núcleo en el fat JAR.

### 1.3 Los cimientos de vista **no existen** todavía

Verificado en el repositorio: `atp-nucleo-compartido/src/main/resources` contiene **sólo**
`application.yml` y `templates/error.html`. **No hay** `templates/layout/`, **no hay** `static/css/`,
**no hay** `static/js/`, **no hay** `web/mvc/**`, y el `pom.xml` del núcleo **no** declara
`spring-boot-starter-thymeleaf`.

⇒ Este módulo **debe** crear el mínimo shell de vistas para poder renderizar la pantalla
(D-27). `03-login` lo reutiliza después; no se duplica.

---

## 2. (a) Paridad JPA — `PRODUCTO` y `PRODUCT1`

> **No se modifica el esquema.** Estructura existente reutilizada (`ddl-auto=none`, G5).
> Fuente de autoridad: `INFORMATION_SCHEMA` de `Rentas` (G-DATA 2026-10-22) contrastada con la
> `Structure` de la KB. Nombres físicos **exactos**.

### 2.1 `PRODUCTO` (tabla física `PRODUCTO`, PK `ProdCod`)

| Atributo lógico | Columna física | Tipo SQL | Nula | Tipo Java | Anotación (estado actual) | Default de aplicación |
|---|---|---|---|---|---|---|
| `ProdCod*` | `ProdCod` | `smallint` | NO | `Integer` | `@Id @Column(name="ProdCod", nullable=false)` | **provisto por el cliente** (no hay `IDENTITY`) |
| `ProdDesc` | `ProdDesc` | `char(25)` | NO | `String` | `@Column(name="ProdDesc", nullable=false, length=25)` + `@ColumnTransformer(read="RTRIM(ProdDesc)")` | — (obligatorio; `@NotBlank`) |
| `ProdTipCal` | `ProdTipCal` | `smallint` | NO | `Integer` | `@Column(name="ProdTipCal", nullable=false)` | **`0`** (`aplicarCampos`) |
| `TPPriID` | `TPPriID` | `smallint` | NO | `Integer` | `@Column(name="TPPriID", nullable=false)` | **`0`** (`aplicarCampos`) |
| `UMedNroId` | `UMedNroId` | `char(3)` | sí | `String` | `@Column(name="UMedNroId", length=3)` | `null` |
| `ProdAcop` | `ProdAcop` | `char(1)` | sí | `String` | `@Column(name="ProdAcop", length=1)` | `null` |
| *(legacy)* `ProdReppa` | `ProdReppa` | `char(1)` | sí | `String` | `@Column(name="ProdReppa", length=1)` + `RTRIM` | `null` — **no expuesta** |
| `ProdAnula` | `ProdAnula` | `smallint` | NO | `Integer` | `@Column(name="ProdAnula", nullable=false)` | **`0`** (`aplicarCampos`) |
| *(derivado)* `ProdIMax1..4` | — | — | — | `BigDecimal` | `@Transient` | calculado por `ProductoService.importesVigentes` |
| *(derivado)* `UMedNombre` | — | — | — | `String` | `@Transient` | **siempre `null`** (no hay columna ni origen) |

### 2.2 `PRODUCT1` (tabla física `PRODUCT1`, PK compuesta `ProdCod` + `ProdFchVig`)

| Atributo lógico | Columna física | Tipo SQL | Nula | Tipo Java | Anotación (estado actual) | Default de aplicación |
|---|---|---|---|---|---|---|
| `ProdCod*` | `ProdCod` | `smallint` | NO | `Integer` | `@MapsId("prodCod")` + `@JoinColumn(name="ProdCod", nullable=false)` (FK lógica) | derivado del padre |
| `ProdFchVig*` | `ProdFchVig` | `datetime` | NO | `LocalDateTime` | dentro de `@EmbeddedId ProductoVigenciaId` | — (obligatorio) |
| `ProdImpu1` | `ProdImpu1` | `money` | NO | `BigDecimal` | `@Column(name="ProdImpu1", nullable=false)` | `0` (`nvl`) |
| `ProdImpu2` | `ProdImpu2` | `money` | NO | `BigDecimal` | `@Column(name="ProdImpu2", nullable=false)` | `0` (`nvl`) |
| `ProdImpu3` | `ProdImpu3` | `money` | sí | `BigDecimal` | `@Column(name="ProdImpu3")` | `0` (`nvl`) |
| `ProdImpu4` | `ProdImpu4` | `money` | sí | `BigDecimal` | `@Column(name="ProdImpu4")` | `0` (`nvl`) |
| *(legacy)* `ProdImpu5` | `ProdImpu5` | `money` | sí | `BigDecimal` | `@Column(name="ProdImpu5")` | `null` — **no expuesta** |
| *(legacy)* `ProdFeLog` | `ProdFeLog` | `datetime` | sí | `LocalDateTime` | `@Column(name="ProdFeLog")` | `null` — **no expuesta** |
| *(legacy)* `ProdUsu` | `ProdUsu` | `char(10)` | sí | `String` | `@Column(name="ProdUsu", length=10)` + `RTRIM` | `null` — **no expuesta** |

### 2.3 Divergencias KB ↔ BD (ya resueltas en `01-nucleo-datos`; se respetan)

| # | Divergencia | Resolución aplicada |
|---|---|---|
| 1 | `DATE(8)` lógico sobre `datetime` físico | `LocalDateTime`; la UI envía **medianoche** (SA-09, R-PROD-23). |
| 2 | `CHARACTER(n)` → `char(n)` fijo | Lectura con `RTRIM`; la UI **recorta** antes de enviar (R-PROD-28). |
| 3 | `NUMERIC(12,2)` lógico sobre `money` físico (escala 4) | Tipo Java `BigDecimal`; la **validación** sigue el tipo lógico (2 decimales, `±9999999999.99`). |
| 4 | `UMedNombre` sin columna | `@Transient`, siempre `null`; no se envía (R-PROD-19, SA-P01). |
| 5 | `ProdIMax1..4` sin columna | `@Transient`; se calculan al vuelo (R-PROD-03). |
| 6 | Sin `IDENTITY` ni `COLUMN_DEFAULT` | La PK viaja en el body del `POST` (R-PROD-09). |
| 7 | `ProdImpu3`/`ProdImpu4` `NULL` en 293 filas reales | La UI normaliza ausente → `0` (R-PROD-20). |

### 2.4 Defaults de aplicación que **exige** cada `NOT NULL` sin contraparte en el DTO

| Columna | `NOT NULL` | Sin contraparte | Default aplicado | Dónde |
|---|---|---|---|---|
| `PRODUCTO.ProdDesc` | sí | — (`@NotBlank` en el DTO) | — | — |
| `PRODUCTO.ProdTipCal` | sí | sí (el DTO lo admite `null`) | **`0`** | `ProductoService.aplicarCampos` |
| `PRODUCTO.TPPriID` | sí | sí | **`0`** | `ProductoService.aplicarCampos` |
| `PRODUCTO.ProdAnula` | sí | sí | **`0`** | `ProductoService.aplicarCampos` |
| `PRODUCTO.ProdReppa` | no | — | `null` | — |
| `PRODUCT1.ProdImpu1/2` | sí | — (`@NotNull`) | `0` | `ProductoService.nvl` |
| `PRODUCT1.ProdImpu3/4` | no | — (`@NotNull` en el DTO) | `0` | `ProductoService.nvl` |
| `PRODUCT1.ProdImpu5/ProdFeLog/ProdUsu` | no | — | `null` | — |

> En `PRODUCTO` **no** hay ninguna columna `NOT NULL` huérfana (a diferencia de `PROVINCI`/`Localida`/
> `Areas`). Los defaults `0` de `ProdTipCal`/`TPPriID`/`ProdAnula` son la salvaguarda existente.

---

## 3. (b) Contrato REST vigente (no se rediseña)

**Base**: `/api/v1`. `ProductoController` (`nucleo/web/rest`). Errores uniformes `{code, message}`
(`ApiError`). `jackson.default-property-inclusion: non_null` ⇒ **las claves `null` se omiten del JSON**.

### 3.1 Endpoints que consume esta pantalla

| Método | Ruta | Uso en la pantalla | Respuesta |
|---|---|---|---|
| GET | `/api/v1/productos[?anulados=]` | Colección del panel izquierdo (filtro de anulados) | `ProductoDto[]` |
| GET | `/api/v1/productos/{prodCod}` | **No se consume** (D-23: la colección ya trae las vigencias) | `ProductoDto` |
| GET | `/api/v1/productos/{prodCod}/importes-vigentes` | Los 4 derivados, 2ª llamada (D-22) | `ImportesVigentesDto` |
| POST | `/api/v1/productos` | — (la UI **no** escribe por HTTP) | `201` + `Location` |
| PUT | `/api/v1/productos/{prodCod}` | — (idem) | `200` |
| DELETE | `/api/v1/productos/{prodCod}` | — (idem) | `204` |

> **La UI no hace HTTP a la API.** El controller MVC invoca **el mismo `ProductoService`** que usa
> `web/rest` (G7: `web → service → repository → domain`; nada de lógica en el controller).
> La colección Postman **no cambia** por este módulo (G-POSTMAN: sólo si se altera un endpoint).

### 3.2 Forma de la lectura (`GET /api/v1/productos`)

```json
[
  {
    "prodCod": 1,
    "prodDesc": "Algodón",
    "prodTipCal": 1,
    "tpPriID": 3,
    "uMedNroId": "TON",
    "prodAcop": "N",
    "prodAnula": 0,
    "vigencias": [
      { "prodFchVig": "2005-09-09T00:00:00", "prodImpu1": 12.00, "prodImpu2": 3.80 },
      { "prodFchVig": "2008-03-31T00:00:00", "prodImpu1": 6.00,  "prodImpu2": 3.80 }
    ]
  }
]
```

Claves **omitidas** cuando su valor es `null` (`non_null`):

| Clave omitida | Causa | Tratamiento en la UI |
|---|---|---|
| `uMedNombre` | siempre `null` (`@Transient` sin origen) | Rótulo `Unidad de Medida` vacío y de sólo lectura (R-PROD-19) |
| `prodImpu3`, `prodImpu4` | `NULL` en `PRODUCT1` (293 filas reales) | Normalizar a `0` para mostrar **y** para enviar (R-PROD-20) |
| `prodAcop` | `NULL` | Mostrar vacío |
| `uMedNroId` | `NULL` | Mostrar vacío |

### 3.3 Forma de la escritura (`POST`/`PUT`)

```json
{
  "prodCod": 999,
  "prodDesc": "Producto de prueba",
  "prodTipCal": 1,
  "tpPriID": 2,
  "uMedNroId": "KG",
  "prodAcop": "N",
  "prodAnula": 0,
  "vigencias": [
    { "prodFchVig": "2026-01-01T00:00:00", "prodImpu1": 10.50, "prodImpu2": 0, "prodImpu3": 0, "prodImpu4": 0 }
  ]
}
```

Reglas del contrato de escritura:

| Regla | Detalle |
|---|---|
| `prodCod` **siempre** en el body | No hay `IDENTITY` (R-037 de `01-nucleo-datos`). |
| `vigencias` **siempre** presente | `@Valid @NotNull`; puede ser `[]` (array vacío ⇒ el producto queda sin vigencias). |
| `vigencias` = **reemplazo total** | El `PUT` borra las filas que no vienen (R-PROD-21, SA-P08). |
| `prodFchVig` a medianoche | `LocalDateTime` `T00:00:00` (SA-09). |
| `prodIMax1..4` **nunca** viajan | No están en el DTO de escritura (R-PROD-03). |
| `uMedNombre` **nunca** viaja | No tiene columna (R-PROD-19). |
| Nombre de la clave | `tpPriID` (**t** minúscula) para el atributo legacy `TPPriID`. |

### 3.4 Códigos de error que la UI debe distinguir

| HTTP | `code` | Origen | Comportamiento de la UI |
|---|---|---|---|
| 400 | `VALIDACION` | `@Valid` del DTO | Mensaje inline; conservar el formulario (R-PROD-11). |
| 400 | `CUERPO_INVALIDO` | Body ilegible | Mensaje inline. |
| 404 | `NO_ENCONTRADO` | Producto inexistente | Mensaje inline + volver a la colección. |
| 409 | `PK_DUPLICADA` | `prodCod` repetido | Mensaje inline; **conservar** el formulario para corregir la PK. |
| 409 | `VIGENCIA_DUPLICADA` | `ProdFchVig` repetida | Mensaje inline; la UI lo anticipa (R-PROD-06). |
| 500 | `ERROR_INTERNO` | Fallo no controlado | Mensaje inline **sin** traza ni nombre de excepción. |

> Los mensajes que la UI muestra son los de `ApiError.message` (o el texto del `BindingResult` en el
> caso de validaciones de formulario), **no** una traducción propia: la paridad exige mostrar el
> `message` del backend (R-PROD-11).

---

## 4. (c) Contrato de vistas Thymeleaf

### 4.1 Rutas y plantillas

| Método | Ruta | Vista | Uso |
|---|---|---|---|
| `GET` | `/productos` | `nucleo/productos` | Colección + formulario en modo **ALTA** |
| `GET` | `/productos?prodCod={cod}` | `nucleo/productos` | Colección + producto `{cod}` en modo **EDICIÓN** |
| `GET` | `/productos?anulados={false\|true}` | `nucleo/productos` | Colección filtrada (combinable con `prodCod`) |
| `POST` | `/productos` | `nucleo/productos` | **Alta**; redirige a `GET /productos?prodCod={cod}` |
| `POST` | `/productos/{prodCod}` | `nucleo/productos` | **Modificación**; redirige a `GET /productos?prodCod={prodCod}` |
| `POST` | `/productos/{prodCod}/eliminar` | `nucleo/productos` | **Baja**; redirige a `GET /productos` |

> **Principio de diseño**: la pantalla es **100 % servidor-rendered** (R-ALC-006). El estado
> (selección, filtro, modo) viaja en la **URL**; no hay estado en el navegador. Por eso la
> navegación, la selección de fila, `Cancelar` y la confirmación son **links y formularios**, no JS.

| Plantilla | Rol |
|---|---|
| `templates/layout/base.html` | Layout base (`th:replace` / `th:insert`); tokens CSS; `js/confirmaciones.js`. |
| `templates/nucleo/productos.html` | La pantalla completa. |
| `templates/nucleo/fragments/producto-vigencias.html` | Grilla del nivel 1 + `<template>` de la fila vacía. |

### 4.2 View model (atributos del modelo)

| Atributo | Tipo | Contenido | Notas |
|---|---|---|---|
| `producto` | `ProductoForm` | Form-backing object (`th:object`) | Viene del `@ModelAttribute`; en error de binding conserva lo enviado |
| `coleccion` | `List<ProductoFilaView>` | Filas del panel de colección | Sólo `prodCod`, `prodDesc`, `prodAnula` |
| `modo` | `enum ModoProducto {ALTA, EDICION}` | Modo del formulario | |
| `accionFormulario` | `String` | URL del `POST` (`/productos` o `/productos/{id}`) | Calculada en el servidor ⇒ la plantilla no decide |
| `accionEliminar` | `String` | URL del `POST` de baja; `null` en modo ALTA | El botón no se renderiza en ALTA |
| `filtroAnulados` | `enum FiltroAnulados {TODOS, NO, SI}` | Filtro activo | |
| `productoId` | `Integer` | Código del producto seleccionado; `null` en ALTA | Origen del path de modificación/baja |
| `mensajeError` | `String` | Mensaje inline (backend o binding) | `null` si no hay error |
| `tieneErrores` | `boolean` | Hay errores de binding ⇒ `Guardar` deshabilitado | |
| `indiceAnterior` / `indiceSiguiente` | `Integer` | `prodCod` de los vecinos en la colección ordenada | `null` en los extremos ⇒ botón deshabilitado |
| `primerCodigo` / `ultimoCodigo` | `Integer` | Extremos de la colección | `null` si la colección está vacía |

### 4.3 Form objects (`web/mvc/producto/`)

#### `ProductoForm`

| Campo | Tipo Java | Control | `th:field` | Validación de formulario | En el body de escritura |
|---|---|---|---|---|---|
| `prodCod` | `Integer` | `number`, `min=0`, `max=32767` | `prodCod` | `@NotNull` (alta), `@Min(0) @Max(32767)` | **sí** |
| `prodDesc` | `String` | `text`, `maxlength=25` | `prodDesc` | `@NotBlank @Size(max=25)` | **sí** (recortado) |
| `prodTipCal` | `Integer` | `number`, `step=1` | `prodTipCal` | opcional | **sí** (o ausente) |
| `tpPriID` | `Integer` | `number`, `step=1` | `tpPriID` | opcional | **sí** (o ausente) |
| `prodIMax1` | `BigDecimal` | `number`, `step=0.01`, **`readonly`** | `prodIMax1` | — (sólo lectura) | **no** |
| `prodIMax2` | `BigDecimal` | `number`, `step=0.01`, **`readonly`** | `prodIMax2` | — | **no** |
| `prodIMax3` | `BigDecimal` | `number`, `step=0.01`, **`readonly`** | `prodIMax3` | — | **no** |
| `prodIMax4` | `BigDecimal` | `number`, `step=0.01`, **`readonly`** | `prodIMax4` | — | **no** |
| `uMedNroId` | `String` | `text`, `maxlength=3` | `uMedNroId` | `@Size(max=3)` | **sí** (recortado) |
| `uMedNombre` | `String` | `text`, `maxlength=40`, **`readonly`** | `uMedNombre` | — (sólo lectura) | **no** |
| `prodAcop` | `String` | `text`, `maxlength=1` | `prodAcop` | `@Size(max=1)` | **sí** (recortado) |
| `prodAnula` | `Integer` | `number`, `step=1` | `prodAnula` | opcional | **sí** (o ausente) |
| `vigencias` | `List<ProductoVigenciaForm>` | grilla | `vigencias[i].*` | `@Valid`, no nula | **sí** (anidado) |

> Los campos derivados **se renderizan** (`th:field`) pero **no se envía**: el controller construye el
> `ProductoEscrituraDto` **campo por campo**, de modo que `prodIMax1..4` y `uMedNombre` quedan fuera
> por construcción (R-PROD-03, R-PROD-19). Un test verifica la ausencia de esas claves en el body.

#### `ProductoVigenciaForm`

| Campo | Tipo Java | Control | `th:field` | Validación |
|---|---|---|---|---|
| `prodFchVig` | `LocalDate` | `date` | `prodFchVig` | `@NotNull`, `@DateTimeFormat(iso = ISO.DATE)` |
| `prodImpu1` | `BigDecimal` | `number`, `step=0.01` | `prodImpu1` | `@NotNull`, `@Digits(integer=10, fraction=2)` |
| `prodImpu2` | `BigDecimal` | `number`, `step=0.01` | `prodImpu2` | `@NotNull`, `@Digits(integer=10, fraction=2)` |
| `prodImpu3` | `BigDecimal` | `number`, `step=0.01` | `prodImpu3` | `@NotNull`, `@Digits(integer=10, fraction=2)` |
| `prodImpu4` | `BigDecimal` | `number`, `step=0.01` | `prodImpu4` | `@NotNull`, `@Digits(integer=10, fraction=2)` |

### 4.4 Parámetros enviados por el formulario

```text
prodCod, prodDesc, prodTipCal, tpPriID, uMedNroId, uMedNombre, prodAcop, prodAnula,
vigencias[0].prodFchVig, vigencias[0].prodImpu1, vigencias[0].prodImpu2,
vigencias[0].prodImpu3, vigencias[0].prodImpu4,
vigencias[1].prodFchVig, …
```

Si la grilla está vacía **no** se envía ningún parámetro `vigencias[i].*` ⇒ el binder deja la lista
vacía ⇒ `vigencias: []` en el body (nunca `null`).

### 4.5 Grilla anidada de vigencias: render y patrón de alta/baja

**Render (servidor).** Por cada fila `i` de `producto.vigencias`:

```html
<tr th:each="v, it : ${producto.vigencias}">
  <td><input type="date" th:field="*{vigencias[__${it.index}__].prodFchVig}"/></td>
  <td><input type="number" step="0.01" th:field="*{vigencias[__${it.index}__].prodImpu1}"/></td>
  <!-- … prodImpu2, prodImpu3, prodImpu4 … -->
  <td><button type="button" class="atp-btn atp-btn--ghost js-quitar-vigencia">Quitar</button></td>
</tr>
```

> El `name` generado debe ser **exactamente** `vigencias[0].prodFchVig`, `vigencias[1].prodFchVig`, …
> Ese es el contrato observable que un test debe verificar (el índice es lo que permite al binder de
> Spring asignar cada fila a su objeto de la lista).

**Plantilla de fila (`<template>`)** — siempre presente en el DOM, incluso con la grilla vacía:

```html
<template id="plantilla-vigencia">
  <tr>
    <td><input type="date"  name="vigencias[__INDEX__].prodFchVig" id="vigencias___INDEX___prodFchVig"/></td>
    <td><input type="number" step="0.01" name="vigencias[__INDEX__].prodImpu1" id="vigencias___INDEX___prodImpu1"/></td>
    <!-- … -->
    <td><button type="button" class="atp-btn atp-btn--ghost js-quitar-vigencia">Quitar</button></td>
  </tr>
</template>
```

**`static/js/vigencias.js`** — alcance **estrictamente mecánico** (R-PROD-27, G6):

| Operación | Qué hace | Qué **no** hace |
|---|---|---|
| `+ Fila` | Clona `#plantilla-vigencia`, sustituye `__INDEX__` por el índice de la nueva fila en los atributos `name`/`id`, **vacía** los valores de los inputs, la inserta al final del `<tbody>` y renumera los índices de las filas siguientes | No valida fechas, no valida importes, no calcula nada, no llama al servidor |
| `− Fila` | Quita la fila del DOM y renumera los `name`/`id` de las filas posteriores para que los índices sigan siendo contiguos | No pregunta al servidor ni borra nada en la BD |

**Por qué renumerar**: Spring necesita índices contiguos `0..n-1`; si una fila del medio se quita y la
última conserva el índice `3` mientras hay 3 filas, el binder deja un hueco y la fila se pierde.

**Estado en el navegador**: ninguno. La grilla se re-renderiza en el servidor tras cada escritura.

### 4.6 Reglas de validación **visibles**

| Regla | Dónde se muestra | Mensaje |
|---|---|---|
| `prodCod` obligatorio (alta) | junto al campo | `El código del producto es obligatorio.` |
| `prodCod` fuera de `0…32767` | junto al campo | `El código debe estar entre 0 y 32767.` |
| `prodDesc` vacío o > 25 | junto al campo | `El producto es obligatorio.` / `Máximo 25 caracteres.` |
| `uMedNroId` > 3 | junto al campo | `Máximo 3 caracteres.` |
| `prodAcop` > 1 | junto al campo | `Máximo 1 carácter.` |
| `prodFchVig` vacía | junto a la celda | `La fecha de vigencia es obligatoria.` |
| Importe con > 2 decimales o fuera de rango | junto a la celda | `Máximo 2 decimales.` / `Fuera de rango.` |
| `ProdFchVig` repetida | banner sobre la grilla | `Hay dos filas con la misma fecha de vigencia.` |

Todos los mensajes se renderizan con `th:errors="*{...}"` (binding) o con el atributo de modelo
`mensajeError` (backend). El botón `Guardar` se renderiza **deshabilitado** cuando
`tieneErrores` es `true`.

### 4.7 Comportamiento ante error

| Situación | Comportamiento |
|---|---|
| Violación de Bean Validation del **formulario** | `BindingResult` con errores por campo ⇒ se re-renderiza la misma vista con `tieneErrores=true`, los valores enviados intactos y el mensaje junto al campo. **No** se llama al servicio. |
| `ConflictException` (`PK_DUPLICADA`, `VIGENCIA_DUPLICADA`) | El controller MVC la **captura** y re-renderiza la vista con `mensajeError = ex.getMessage()` y el formulario conservado. |
| `NotFoundException` | Igual, con `mensajeError`; la grilla queda como estaba. |
| Error de la BD | `GlobalExceptionHandler` responde la vista `error` (canal HTML). Sin traza ni nombre de excepción (R-206). |
| Fallo al cargar la colección | `mensajeError` + estado `error`; **no** se renderiza la grilla de datos. |

> El `GlobalExceptionHandler` es global y, para rutas que no son `/api/**`, devuelve la vista `error`.
> Para **conservar el formulario** el controller MVC debe capturar las excepciones de dominio
> (`ConflictException`, `NotFoundException`) **antes** de que lleguen al advice.

### 4.8 Estados y navegación

| Elemento | Implementación |
|---|---|
| Colección | `<table class="atp-table">` con las columnas `Código`, `Producto`, `Prod Anulado` (rótulos verbatim del nivel 0). Cada fila es un link a `GET /productos?prodCod={cod}`. |
| `First` / `Prev` / `Next` / `Last` | Links a `GET /productos?prodCod={cod}`; deshabilitados en los extremos (`indiceAnterior`/`indiceSiguiente`/`primerCodigo`/`ultimoCodigo` en `null`). El orden de la colección es por `prodCod` ascendente (R-PROD-25). |
| `Seleccionar` | Equivale al click en la fila. |
| `Guardar` | `POST` del formulario (`th:action` ⇒ CSRF). |
| `Cancelar` | Link a `GET /productos` (vuelve a ALTA con el formulario y la grilla vacíos). |
| `Eliminar` | Sólo en EDICION. `POST` a `accionEliminar` con `confirm()` (`js/confirmaciones.js`). |
| Filtro de anulados | 3 links (`TODOS`/`NO`/`SI`) a `GET /productos?anulados=…`, preservando `prodCod`. |
| Estados | `loading` (no aplica: render de servidor; el estado inicial es "sin selección"), `empty` ("Sin registros" + link `Recargar`), `error` (mensaje inline), `sin selección` (formulario vacío en ALTA). |

### 4.9 CSS y JS

| Archivo | Contenido | Regla |
|---|---|---|
| `static/css/tokens.css` | `:root` con las *custom properties* de `docs/design.md` §2.2, §3 y §4 | Ningún literal de color o tipografía fuera de este archivo |
| `static/css/nucleo.css` | `.atp-table`, `.atp-btn`, `.atp-btn--primary`, `.atp-btn--ghost`, `.atp-alert` (+ variantes), `.atp-form`, `.atp-grid-2col` | Mapeo de `docs/design.md` §5 |
| `static/js/vigencias.js` | Alta/baja de filas de la grilla (§4.5) | Sin `fetch`/`XMLHttpRequest`/validación |
| `static/js/confirmaciones.js` | `confirm()` de la baja | Reutiliza el diálogo nativo |

### 4.10 Trazabilidad de la vista con el legacy

| Legacy (`WebForm`) | Thymeleaf |
|---|---|
| `textblock Title caption="Productos"` | Título de la página: `Productos` |
| `errorviewer ErrorViewer` | Banner `.atp-alert--error` con `mensajeError` |
| `actionGroup Toolbar / NavButtons` | Links de navegación en la cabecera del panel de colección |
| 12 × `<data labelCaption="=Att.ContextualTitle">` | 12 `<label>` + `<input th:field>` con los rótulos verbatim (§3.1) |
| `textblock TitleLevel1 caption="Level1"` | Título de la sección de vigencias: `Level1` (SA-P02) |
| `simplegrid GridProductos_Level1Item` | `<table class="atp-table">` + `<template>` de fila (§4.5) |
| `actionGroup FormButtons` (`btn_enter/cancel/delete`) | `Guardar` / `Cancelar` / `Eliminar` (SA-P03) |

---

## 5. (d) Decisiones

| # | Decisión | Justificación |
|---|---|---|
| **D-26** | La pantalla vive en el **núcleo** (`templates/nucleo/`, `nucleo/web/mvc/`), no en `atp-puestos-camineros` | Es un catálogo del núcleo (`00-alcance/plan.md` §5); el núcleo ya expone el servicio. |
| **D-27** | Este módulo crea el **shell mínimo de vistas** (layout, tokens CSS, JS de confirmación, dependencia Thymeleaf) | Hoy **no existe** nada de eso (§1.3) y la pantalla no se puede renderizar sin él. `03-login` lo reutiliza. |
| **D-28** | La pantalla es **100 % servidor-rendered**; el estado viaja en la URL | R-ALC-006; elimina el JS de aplicación y hace la navegación, la selección y `Cancelar` links planos. |
| **D-29** | El controller MVC invoca **`ProductoService`**; no hay llamada HTTP a la propia API | G7 (`web → service → repository → domain`); evita latencia y doble serialización. |
| **D-30** | El mapeo `form → ProductoEscrituraDto` es **campo por campo** | Garantiza por construcción que `prodIMax1..4` y `uMedNombre` no viajen (R-PROD-03, R-PROD-19). |
| **D-31** | Las reglas se declaran en el DTO **y** en el form object, con un **test de paridad de restricciones** | El DTO es la fuente de verdad del servidor; el form object replica para el feedback inmediato. El test impide que se desincronicen (AGENTS.md regla 9). |
| **D-32** | El **rango de `prodCod`** y los **2 decimales** se validan **también en el backend** (DTO) | El backend hoy responde `500` ante un `prodCod` fuera del rango del `smallint` (hallazgo **N-4**). ⇒ **Cambio de contrato REST** ⇒ la colección Postman se actualiza (G-POSTMAN). |
| **D-33** | La colección se ordena por `prodCod` **en la capa de vista**, no en el endpoint | `GET /api/v1/productos` no declara `ORDER BY`; ordenar en el endpoint cambiaría el orden observable del contrato. La vista es la capa correcta para una decisión de presentación. |
| **D-34** | El filtro de anulados se implementa con el parámetro **ya existente** | No agrega ni modifica endpoints ⇒ G-POSTMAN N/A. |
| **D-35** | La grilla se implementa con una **fila-plantilla `<template>`** y `name` indexados | Es el único patrón que permite alta/baja de filas con render de servidor **y** binding correcto por índice. |
| **D-36** | El núcleo incorpora `spring-boot-starter-thymeleaf` en scope **`compile`** y **NO** incorpora Spring Security (ni en `compile` ni en `test`) | Las vistas viven en el núcleo (D-26) ⇒ sus `templates/` deben renderizarse desde el jar desplegable. Spring Security **no** se mete en el núcleo ni siquiera en `test`: su autoconfiguración se activaría en los `@WebMvcTest` del núcleo y rompería la suite verde de `01-nucleo-datos` (105 pruebas). Consecuencia directa: en el núcleo `th:action` **no** renderiza el campo `_csrf`, por lo que la verificación del token CSRF (R-PROD-22, T-212) se ejecuta en el módulo desplegable `atp-puestos-camineros`, que ya tiene `spring-boot-starter-security` + `spring-security-test`. |
| **D-37** | La seguridad de la pantalla **no** se implementa en este módulo | Hasta que `03-login` defina el `SecurityFilterChain`, la app desplegada aplica la seguridad por defecto de Spring Boot (HTTP Basic con contraseña autogenerada en el log) ⇒ `/productos` queda **protegida**, no expuesta. Esto es coherente con SA-P07 y con la regla 9 (la seguridad es de `03-login`). El módulo 02 **no** añade ni modifica ninguna configuración de seguridad. |
| **D-38** | El importe leído de `PRODUCT1.ProdImpu1..5` se **normaliza al tipo lógico `NUMERIC(12,2)`** en el borde de lectura (entidad → DTO), con `setScale(2, RoundingMode.HALF_UP)` | La columna física es `money` (**escala 4** en SQL Server) mientras el tipo lógico de la KB es `NUMERIC(12,2)`. Sin normalizar, el driver entrega `12.0000` y el `@Digits(integer=10, fraction=2)` del DTO de escritura (D-31/D-32) **rechaza** el valor ⇒ el ida y vuelta leer → editar → guardar se rompe en la propia API y en la pantalla. Se normaliza en el borde de lectura para que el JSON Devuelto sea ya el valor lógico, sin tocar la forma del contrato ni relajar la validación (R-PROD-08 sigue exigiendo ≤ 2 decimales). **Implementado** en `nucleo/service/ImporteNormalizador.java` (helper único, null-preservante: el `null → 0` de R-005 sigue en el servicio). **Consecuencia documentada**: las **2 filas** de `PRODUCT1` con más de 2 decimales reales se redondean al centavo en el ida y vuelta; el legacy las asignaba a un `NUMERIC(12,2)`, con el mismo efecto. |
| **D-39** | La modificación de un producto hace **actualización diferencial** de `PRODUCT1`: las vigencias existentes con la misma `ProdFchVig` se **actualizan in situ** (conservando `ProdImpu5`, `ProdFeLog` y `ProdUsu`), se eliminan las que no llegan y se insertan las nuevas | El `PUT` anterior hacía `clear()` + reinserción (orphan removal) ⇒ **borraba y recreaba** las filas hijas, con lo que las columnas legacy de auditoría quedaban en `NULL`. Impacto real en `Rentas`: **1116** filas con `ProdUsu`, **1116** con `ProdFeLog` y **764** con `ProdImpu5` ⇒ una modificación de producto **perdía datos de auditoría que hoy existen**. Además, el borrado + reinserción se aleja del legacy: GeneXus actualiza in situ las filas hijas cuyos valores cambian. La actualización diferencial **preserva el contrato observable** (el conjunto final de vigencias es exactamente el enviado — R-PROD-21/SA-P08) y **elimina la pérdida de datos**. Las columnas legacy de las filas **nuevas** siguen en `NULL` hasta que `03-login` defina el usuario de sesión (**SA-P11**). |

## 6. Supuestos explícitos

| ID | Supuesto | Se resuelve en |
|---|---|---|
| SA-P01 | `UMedNombre` no se persiste (no hay columna ni origen) | `spec.md` §7 |
| SA-P02 | Título de la sección del nivel 1 = `Level1` (verbatim) | `spec.md` §7 |
| SA-P03 | Textos de los botones: `Guardar` / `Cancelar` / `Eliminar` / `Seleccionar` | `spec.md` §7 |
| SA-P04 | Rango de `prodCod` = `0…32767` | `spec.md` §7 |
| SA-P05 | El filtro de anulados se incluye | `spec.md` §7 |
| SA-P06 | Ruta de página = `/productos` | `spec.md` §7 |
| SA-P07 | La pantalla queda sin autenticación hasta `03-login` | `spec.md` §7 |
| SA-P08 | La modificación **reemplaza** el conjunto de vigencias | `spec.md` §7 |
| **SA-P09** *(nuevo)* | `spring-boot-starter-thymeleaf` **sin** `thymeleaf-layout-dialect` (D-07 de `00-alcance/plan.md`): los fragmentos `th:replace` alcanzan | `tasks.md` T-001 |
| **SA-P10** *(nuevo)* | La numeración de la grilla se resuelve con `name="vigencias[i].campo"` explícito (índice literal), sin depender de la auto-expansión de listas del binder | `tasks.md` T-301 |
| **SA-P11** *(nuevo)* | Las columnas legacy de auditoría `ProdUsu`, `ProdFeLog` y `ProdImpu5` de las vigencias **nuevas** se dejan en `NULL` (no hay usuario de sesión todavía). Con **D-39** las vigencias **existentes** ya no las pierden. ¿Debe `03-login` escribirlas (usuario de la sesión + fecha/hora) al dar de alta una vigencia? | `03-login` (la escritura de auditoría pertenece al módulo que define la sesión) |

## 7. Alcance excluido

- Modificación del contrato REST (sólo el **refuerzo** de validación de D-32, que sí obliga a
  actualizar Postman).
- Cualquier DDL, migración, `schema.sql`/`data.sql`, Flyway o Liquibase (G5).
- Persistencia de `UMedNombre` (exigiría DDL).
- Lookup de unidades de medida (no hay datos ni endpoint).
- Autenticación, autorización y gestión de sesión (módulo `03-login`).
- ABM de las otras 7 maestras, `ListadoLocalidades`, portales fuera del monorepo.
- E2E automatizado.

## 8. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| **`PUT` reemplaza el conjunto de vigencias** y la UI envía sólo las filas editadas ⇒ pérdida de datos | Alto | R-PROD-21 + IT que verifica que las filas no editadas sobreviven |
| **`ProdImpu3`/`ProdImpu4` en `NULL`** (293 filas reales) + `@NotNull` en el DTO ⇒ `400` al guardar | Alto | R-PROD-20 (normalizar a `0` antes de enviar) + IT con una vigencia real nula |
| **N+1** al listar: `findAll()` + `@OneToMany(LAZY)` ⇒ 1 consulta por producto (244 → 245 consultas) | Medio | Tarea de backend: `@EntityGraph`/`join fetch` en el listado; **no** cambia el contrato |
| **`prodCod` fuera de rango** ⇒ el backend responde `500` (desborde de `smallint`) | Medio | R-PROD-07 (UI) + D-32 (validación en el DTO) |
| **Orden no determinista** de `GET /api/v1/productos` ⇒ la navegación First/Last sería no reproducible | Medio | R-PROD-25 + D-33 (orden en la vista) |
| **Índice 404/405** de la colección o de los defaults de la BD | Bajo | La UI muestra el `message` del backend; los defaults están centralizados en `ProductoService` |
| La pantalla queda **expuesta** hasta que llegue `03-login` | Medio | SA-P07: se acepta explícitamente; `03-login` cierra |
| Conflicto de nombres de plantilla entre módulos | Bajo | D-03: prefijo `templates/nucleo/` |

## 9. Matriz de paridad (R-202)

| Legacy (KB / BD) | Nuevo (EARS) | Prueba |
|---|---|---|
| `Transaction Productos` — 12 atributos raíz | R-PROD-02 | Test de rótulos renderizados |
| `Table Producto` — 8 columnas + `ProdReppa` huérfana | §2.1 | `ProductoRepositoryIT` (existente) |
| `Table Product1` — 9 columnas + 3 huérfanas | §2.2 | `ProductoRepositoryIT` (existente) |
| Fórmulas `ProdIMax1..4` | R-PROD-03 | `MvcImportesDerivadosTest` |
| `UMedNombre` sin columna | R-PROD-19 | `MvcAtributoNoPersistidoTest` |
| `simplegrid GridProductos_Level1Item` | R-PROD-04 | `MvcGrillaVigenciasTest` + prueba de navegador |
| `NavButtons` First/Prev/Next/Last | R-PROD-13 | `MvcNavegacionTest` |
| `FormButtons` Enter/Cancel/Delete | R-PROD-01, R-PROD-14 | `MvcAltaTest`, `MvcModificacionTest`, `MvcBajaTest` |
| `errorviewer` | R-PROD-11 | `MvcErroresTest` |
| `DATE(8)` sobre `datetime` (SA-09) | R-PROD-23 | `MvcFechaVigenciaTest` |
| `NUMERIC(12,2)` lógico sobre `money` físico | R-PROD-08 | `MvcDecimalesTest` |
| Sin `IDENTITY` | R-PROD-09 | `MvcAltaTest` (PK en el body) |
| `PRODUCT1` sin FK física a `PRODUCTO` | R-PROD-01 (baja en cascada por JPA) | IT de baja |
| `docs/design.md` §5/§6/§7/§8 | R-PROD-16, R-PROD-17, R-PROD-27 | Inspección + `axe-core` + revisión del JS |
