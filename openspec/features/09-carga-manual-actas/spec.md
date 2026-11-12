# Spec — 09 "Carga Manual de Actas" (Formulario 2276 · alta) — Thymeleaf

> **Estado**: `APROBADO` — aprobado en la puerta **G-SPEC** (2026-11-11).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-ACTA-<NN>` (requisitos funcionales) y `V-<NN>` (reglas de vista verificables
> en el HTML). Requisitos no funcionales con prefijo `R-ACTA-<NN>` continuando la numeración.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13.186702), alias `atppuestos` — **solo lectura**.
> - Transacción `F2276` (Actas) y WebForm `Form2276` (alta/carga manual).
> - Tablas físicas `F2276` (cabecera) y `F22761` (detalle de productos).
> **Fuente visual**: Pantalla Stitch "Carga Manual de Actas" (proyecto "Modern Spring Boot Portal",
> id `6019790973415672987`, screen `2bd494a3af6544b49f0af8d6c5f727ab`). Design System
> "Gestión Caminera Chaco" (`stitch-tokens.css`, tokens `--stitch-*`).
> **Módulo existente**: `09 Formularios Acta` (referencia en `AGENTS.md`). Vista implementada:
> `puestos/carga-manual-actas.html`. Backend implementado: `ActasController` (`/actas`,
> `/actas/nueva`, `/actas/guardar`), `ActaForm`, `ActaService`, `ActaRepository`,
> `ActaProductoRepository`.
> **Alcance de escritura**: `openspec/features/**` (esta spec) y, en la fase de implementación,
> `atp-puestos-camineros/**`. **Sin DDL, sin migraciones** (G5). **G-POSTMAN: no aplica**
> (sólo controllers MVC; sin cambios en `/api/v1/**`).

---

## 1. Propósito

Entregar la **pantalla de alta** del Acta (Formulario 2276 · "Carga manual") como vista Thymeleaf
moderna alineada al Design System Stitch (tokens `--stitch-*`), que:

- permita cargar manualmente un Acta cuando no proviene de un turno cerrado (modo manual);
- valide los datos con los mensajes verbatim del legacy (Bean Validation + servicio);
- presente 6 fieldsets (Datos del Acta, Vendedor, Destinatario, Transportista, Datos del
  Transporte, Productos) y una barra de acciones con Confirmar/Cancelar;
- bloquee el formulario cuando el turno esté cerrado o haya lotes/formularios abiertos;
- mantenga paridad funcional con el legacy (mismos datos, mismos mensajes, misma numeración).

Es la **evolución visual** del WebForm legacy `Form2276` (alta): se renombra la plantilla a
`carga-manual-actas.html` y se mantiene el contrato de campos, validaciones y mensajes.

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Plantilla Thymeleaf `templates/puestos/carga-manual-actas.html` (alta). |
| 2 | Hoja de estilos `static/css/actas-forms.css` (tokens `--stitch-*`, sin hex/rgb). |
| 3 | Script `static/js/actas-forms.js` (vanilla: alta/baja de filas en la grilla de productos, confirmación, totales, búsqueda de CUIT placeholder). |
| 4 | Fragmento de grilla de productos `templates/puestos/fragments/acta-productos.html` (`grilla`). |
| 5 | Controller MVC `ActasController` (`/actas`, `/actas/nueva`, `/actas/guardar`). |
| 6 | Form object `ActaForm` (binding + traducción a `ActaAlta`). |
| 7 | Validación por campo con `th:errorclass`/`th:errors` (V-9). |
| 8 | Bloqueo por lote/turno con motivo (V-17). |
| 9 | Accesibilidad AA, responsive, sin CDN, sin DDL. |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Listado de Actas (`actas-lista.html`) | Módulo aparte dentro del mismo controlador; cubierto por R-ACTA-001…R-ACTA-013. | `09-carga-manual-actas` (sub-vista, fuera de esta spec de alta) |
| Endpoint REST nuevo | El módulo es puramente MVC; sin cambios en `/api/v1/**`. | — |
| Rediseño de `F2276`/`F22761` | Paridad BD: `ddl-auto=none`, sin DDL (G5). | — |
| Lógica de negocio en la vista o en el JS | Las reglas viven en `ActaService`; la vista es adaptador. | `ActaService` |
| Frameworks JS, bundlers, CDNs | Prohibido por AGENTS.md regla 8. | — |
| Modificación de estilos globales/layout | Sólo CSS propio del módulo. | — |
| Autenticación/autorización | La define `03-login`. | `03-login` |
| Modal "Detalle de Guía" | Spec separada. | `04-modal-detalle-guia` |

---

## 3. Modelo legacy verificado

### 3.1 Tablas físicas (referencia, **no se modifican**)

| Tabla | Entidad JPA | PK compuesta | Uso en la pantalla |
|---|---|---|---|
| `F2276` | `Acta` (`@Table(name="F2276")`) | `(F276Id, SucTipo, SucNroId)` | Cabecera del Acta (alta). |
| `F22761` | `ActaProducto` (`@Table(name="F22761")`) | `(F276Id, SucTipo, SucNroId, ProdCod, F276RZona)` | Detalle de productos. |
| `LOTE227` / contadores | `ActaNumeracion` (servicio) | — | Numeración del Acta (R-ACTA-036). |

### 3.2 Columnas huérfanas (mapeadas, no expuestas por la vista)

- `F2276`: `PueCod`, `CPCId` (existen en la BD, no en la KB) → default `null` (R-ACTA-051).
- `F22761`: `F276Corrig` (existe en la BD, no en la KB) → default `null` (R-ACTA-051).

### 3.3 Atributos derivados (no son columnas)

`F276Total`, `F276AdiTot`, `F276ImpTotal`, `F276ImIBTot`, `F276ImCMTot`, `F276ProCan`,
`F276GenMan`, `F276CodBar`, `FCalFlete` y las localidades/provincias de las tres partes se
declaran `@Transient` y los calcula el servicio (D-ACTA-07). La vista no inventa valores.

### 3.4 Mensajes verbatim (paridad observable)

| Situación | Texto legacy (verbatim) | Dónde se muestra |
|---|---|---|
| Alta exitosa | `El acta se ha creado correctamente` | Flash `mensajeExito` tras redirect a `/actas`. |
| Sin turno | `Debe iniciar un turno...` | Banner de error (listado). |
| Motivo sin informar | `Seleccionar el motivo de Acta` | Banner de error (alta). |
| Tipo de movimiento sin informar | `Seleccionar el tipo de movimiento` | Banner de error (alta). |
| CUIT inválido | `El CUIT del vendedor/destinatario/transportista es incorrecto` | Banner de error (alta). |
| Razón social vacía | `La razon social del vendedor/destinatario/transportista no puede quedar vacia` | Banner de error (alta). |
| Faltan datos del transporte | `Debe ingresar los datos del transporte` | Banner de error (alta). |
| Grilla vacía | `Debe seleccionar algun producto` | Banner de error (alta). |
| Más de 6 productos | `No se pueden ingresar mas de 6 productos` | Banner de error (alta). |
| Producto duplicado | `El producto ... ya fue ingresado` | Banner de error (alta). |
| Fila sin código | `Debe seleccionar el codigo del producto en la fila N` | Banner de error (alta). |
| Entrada con motivo no permitido | `Para una Entrada el motivo de Acta debe ser P` | Banner de error (alta). |
| Sin tipo de comprobante | `Debe seleccionar el tipo de comprobante` | Banner de error (alta). |
| Sin número de comprobante | `Debe ingresar el numero de comprobante` | Banner de error (alta). |
| Fecha futura | `La fecha no puede ser mayor a la fecha actual` | Banner de error (alta). |
| Turno cerrado | `El turno esta cerrado` | Banner de error (alta). |
| Precio informado inferior | `El precio informado no puede ser inferior al precio registrado` | Banner de error (alta). |
| Bloqueo por lote/turno | motivo verbatim del servicio | Banner de error (alta, V-17). |
| Confirmación | `¿Confirma que desea enviar el Formulario de Acta?` | `window.confirm` (JS). |
| Búsqueda CUIT no implementada | `Búsqueda de CUIT no implementada` | `window.alert` (JS). |

---

## 4. Requisitos EARS

### 4.1 Estructura de la página (breadcrumb, título, layout)

#### R-ACTA-057 — Extensión del layout base
> **EL SISTEMA DEBE** extender `layout/base-stitch` (cabecera, topbar, hero, sesión, navegación,
> pie) y **NO DEBE** duplicar cabecera ni pie en la vista.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaNoDuplicaCabeceraNiPie` (1 `<header>`,
  1 `<footer>`).

#### R-ACTA-058 — Título de la página
> **EL SISTEMA DEBE** mostrar el título "Carga manual · Acta" en el `<title>` y en el encabezado
> principal de la página.

* *Criterio de verificación*: inspección del HTML renderizado (`<title>` y `<h1>`).

#### R-ACTA-002 — Banner de turno inactivo
> **CUANDO** el turno de la sesión no sea mayor que cero, **EL SISTEMA DEBE** mostrar el banner de
> turno inactivo y **NO DEBE** permitir crear un Acta.

* *Criterio de verificación*: `ActasMvcControllerTest` (banner presente).

### 4.2 Datos del Acta (fieldset 1)

#### R-ACTA-015 — Fieldset "Datos del Acta"
> **EL SISTEMA DEBE** renderizar el fieldset "Datos del Acta" con los controles de motivo de Acta,
> tipo de movimiento y fecha del Acta.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaRenderizaLosFieldsets`.

#### R-ACTA-014 — Número de Acta sólo en modo manual (V-11)
> **EN MODO MANUAL** (`manual == true`), **EL SISTEMA DEBE** renderizar el campo "Nro. Acta"
> (`f276Id`) editable; **EN MODO TURNO**, **NO DEBE** renderizarlo.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaNoRenderizaElNumeroDeActa` (modo turno
  por defecto no contiene `Nro. Acta` ni `name="f276Id"`).

#### V-10 — Fecha del Acta en sólo lectura
> **EL SISTEMA DEBE** renderizar la fecha del Acta (`f276FchMov`) en un input `type="date"` con
> atributo `readonly`, precargada por el servidor (fecha del turno o hoy).

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaRenderizaLaFechaEnSoloLectura`.

#### R-ACTA-026 — Motivo de Acta obligatorio
> **CUANDO** el motivo de Acta no se informe, **EL SISTEMA DEBE** rechazar el alta con el mensaje
> verbatim `Seleccionar el motivo de Acta`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-026).

#### R-ACTA-027 — Tipo de movimiento obligatorio
> **CUANDO** el tipo de movimiento no se informe, **EL SISTEMA DEBE** rechazar el alta con el
> mensaje verbatim `Seleccionar el tipo de movimiento`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-027).

#### R-ACTA-034 — Fecha no futura
> **CUANDO** la fecha del Acta sea mayor a la fecha actual, **EL SISTEMA DEBE** rechazar el alta.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-034).

### 4.3 Tríptico de actores (Vendedor / Destinatario / Transportista)

#### R-ACTA-016 — Fieldsets del tríptico
> **EL SISTEMA DEBE** renderizar tres fieldsets ("Vendedor", "Destinatario", "Transportista") con
> los 15 controles (CUIT, Razón Social, Domicilio, Localidad, Provincia) por actor.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaRenderizaLosControlesDeLasPartes`.

#### V-12 — Localidad y Provincia en sólo lectura
> **EL SISTEMA DEBE** renderizar los campos de Localidad y Provincia de los tres actores en modo
> `readonly`; el código de localidad se deriva del CUIT y el botón de popup se deshabilita cuando
> el código está derivado.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaLocalidadYProvinciaEnSoloLectura`.

#### R-ACTA-028 — CUIT válido
> **CUANDO** el CUIT de cualquiera de los tres actores sea inválido, **EL SISTEMA DEBE** rechazar
> el alta con el mensaje verbatim correspondiente.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-028).

#### R-ACTA-029 — Razón social obligatoria
> **CUANDO** la razón social de cualquiera de los tres actores esté vacía, **EL SISTEMA DEBE**
> rechazar el alta con el mensaje verbatim correspondiente.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-029).

#### R-ACTA-018 — Búsqueda de contribuyente por CUIT
> **CUANDO** se informe el CUIT de un actor, **EL SISTEMA DEBE** consultar el contribuyente y
> completar razón social, domicilio y localidad; si no existe, dejar los datos vacíos.

* *Criterio de verificación*: `ActaServiceTest` (R-ACTA-018).

#### V-9 — Errores por campo
> **CUANDO** un campo tenga error de binding o de validación, **EL SISTEMA DEBE** aplicar la clase
> de error al control (`th:errorclass`) y mostrar el mensaje en un párrafo con `role="alert"`
> (`th:errors`).

* *Criterio de verificación*: inspección del HTML + `ActasMvcFormularioTest` (campos con `error`).

### 4.4 Datos del Transporte (fieldset 5)

#### R-ACTA-017 — Fieldset "Datos del Transporte"
> **EL SISTEMA DEBE** renderizar el fieldset "Datos del Transporte" con patente chasis, patente
> acoplado, tipo de comprobante, número de comprobante y kilómetros de flete.

* *Criterio de verificación*: inspección del HTML renderizado.

#### R-ACTA-030 — Datos del transporte obligatorios
> **CUANDO** falte cualquiera de los datos del transporte (chasis, acoplado, tipo de comprobante,
> número de comprobante o kilómetros de flete), **EL SISTEMA DEBE** rechazar el alta con el mensaje
> verbatim `Ingrese todos los datos del Transporte`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-030).

> **Nota (validado contra KB PPActaDetalle, Sub 'Validar')**: el legacy valida con un único bloque
> `OR` sobre los 5 campos y muestra un solo mensaje genérico. No hay mensajes específicos por campo.

#### R-ACTA-032 — Entrada sólo admite motivo "P"
> **CUANDO** el tipo de movimiento sea "Entrada" y el motivo de Acta no sea "P", **EL SISTEMA
> DEBE** rechazar el alta con el mensaje verbatim `Para una Entrada el motivo de Acta debe ser P`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-032).

### 4.5 Declaración de productos (fieldset 6)

#### R-ACTA-020 — Fieldset "Productos" con grilla
> **EL SISTEMA DEBE** renderizar el fieldset "Productos" con la grilla de productos (columnas
> Código, Descripción, Tn.) y el botón "+" para agregar filas.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaRenderizaLaGrillaDeProductos`.

#### R-ACTA-031 — Grilla no vacía
> **CUANDO** la grilla de productos esté vacía, **EL SISTEMA DEBE** rechazar el alta con el
> mensaje verbatim `Debe seleccionar algun producto`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-031).

#### R-ACTA-023 — Máximo 6 productos
> **CUANDO** se ingresen más de 6 productos, **EL SISTEMA DEBE** rechazar el alta con el mensaje
> verbatim `No se pueden ingresar mas de 6 productos`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-023).

#### R-ACTA-038 — Producto no duplicado
> **CUANDO** se ingrese el mismo producto dos veces, **EL SISTEMA DEBE** rechazar el alta.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-038).

#### R-ACTA-039 — Fila con código seleccionado
> **CUANDO** una fila de producto no tenga código seleccionado, **EL SISTEMA DEBE** rechazar el
> alta con el mensaje verbatim `Debe seleccionar el codigo del producto en la fila N`.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-039).

#### R-ACTA-021 — Cantidad forzada a 1 para TipCal=3
> **CUANDO** el producto seleccionado tenga `ProdTipCal = 3`, **EL SISTEMA DEBE** forzar la
> cantidad a 1 y dejarla en sólo lectura.

* *Criterio de verificación*: `actas-forms.js` (`sincronizarFila`) + test de JS.

#### R-ACTA-022 — Precio de la vigencia vigente
> **EL SISTEMA DEBE** obtener el precio del producto de la vigencia vigente (`PRODUCT1`) según la
> clasificación (Entrada/Salida) y la fecha del Acta.

* *Criterio de verificación*: `ActaServiceTest` (R-ACTA-022).

#### R-ACTA-037 — Precio informado no inferior al registrado
> **CUANDO** el precio informado sea inferior al precio registrado, **EL SISTEMA DEBE** rechazar
> el alta; un precio informado en cero toma el precio registrado.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-037).

#### R-ACTA-047 — Cálculo de impuesto de ingresos brutos
> **EL SISTEMA DEBE** calcular el impuesto de ingresos brutos por línea según `Nuc_CalculoImpIB`
> (TipCal 1/2/3) con la fórmula documentada.

* *Criterio de verificación*: `ActaServiceTest` (R-ACTA-047).

#### R-ACTA-055 — Accesibilidad de la grilla
> **EL SISTEMA DEBE** asociar un `<label for>` a cada control de la grilla (oculto visualmente) y
> actualizar los `for`/`id` al renumerar filas.

* *Criterio de verificación*: `ActasMvcFormularioTest.postGuardarReRenderizaLaGrillaDeProductos`
  (`for="productos_0_prodCod"`).

### 4.6 Barra de acciones y firma

#### R-ACTA-059 — Botones Confirmar y Cancelar
> **EL SISTEMA DEBE** mostrar la barra de acciones con el botón "Confirmar" (submit) y el enlace
> "Cancelar" (a `/actas`), y mostrar el mensaje de confirmación verbatim antes de enviar.

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaMuestraLaConfirmacionYCancelar`.

#### R-ACTA-010 — Tamaño de página del listado
> **EL SISTEMA DEBE** usar tamaño de página 10 en el listado (paridad con `Grid276.MaxRows`).

* *Criterio de verificación*: `ActasController` (`TAMANIO_PAGINA = 10`).

### 4.7 Bloqueo por lote/turno (V-17)

#### R-ACTA-041 — Sin turno bloquea el formulario
> **CUANDO** no haya turno o haya formularios/lotes abiertos, **EL SISTEMA DEBE** bloquear el
> formulario y mostrar el motivo verbatim (sin renderizar los controles).

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaBloqueadaNoRenderizaElFormulario` +
  `ActaServiceTest` (R-ACTA-041).

#### R-ACTA-042 — Turno cerrado bloquea el formulario
> **CUANDO** el turno esté cerrado, **EL SISTEMA DEBE** bloquear el formulario.

* *Criterio de verificación*: `ActaServiceTest` (R-ACTA-042) + `ActaAltaTest` (R-ACTA-035).

#### R-ACTA-040 — Validación del servicio (primer mensaje)
> **CUANDO** el alta tenga varias infracciones, **EL SISTEMA DEBE** devolver sólo el primer
> mensaje verbatim.

* *Criterio de verificación*: `ActaAltaTest` (R-ACTA-040).

#### R-ACTA-044 — Contexto de sesión de sólo lectura
> **EL SISTEMA DEBE** inyectar el contexto de sesión como sólo lectura (no bindea ni persiste) y
> tomar turno/sucursal del contexto, no del formulario.

* *Criterio de verificación*: `ActasMvcFormularioTest.postGuardarUsaElContextoDeSesion` + `ActaForm`
  (`contexto`).

### 4.8 No funcionales

#### R-ACTA-053 — Tokens exclusivos en CSS
> **EL SISTEMA DEBE** usar exclusivamente tokens `--stitch-*` en `actas-forms.css` y
> **NO DEBE** contener literales hexadecimales (`#xxx`), `rgb(...)`, `rgba(...)` ni nombres de
> color CSS (salvo `color-mix` con tokens).

* *Criterio de verificación*: `ActasRecursosTest` + grep (0 matches de `#[0-9a-fA-F]{3,8}` y
  `rgba?\s*\(`).

#### R-ACTA-060 — Sin CDN ni recursos externos
> **EL SISTEMA DEBE** no referenciar CDNs, fuentes externas ni scripts de terceros en la vista ni
> en su CSS/JS.

* *Criterio de verificación*: grep sin `https?://`/`cdn`/`unpkg`/`jsdelivr` en `actas-forms.js`.

#### R-ACTA-061 — JS puntual vanilla
> **EL SISTEMA DEBE** implementar el comportamiento de la grilla (alta/baja de filas, renumeración,
> totales, sincronización por producto, confirmación) en JavaScript vanilla (sin frameworks, sin
> bundlers, sin `npm`), ubicado en `static/js/actas-forms.js`.

* *Criterio de verificación*: revisión del archivo (sin `import`/`require`, sin `fetch`).

#### R-ACTA-062 — Accesibilidad AA
> **EL SISTEMA DEBE** mantener contraste ≥ 4,5:1, asociar `<label>` a cada control, garantizar
> foco visible y navegación por teclado, y anunciar los mensajes con `aria-live`.

* *Criterio de verificación*: `axe-core` in-page sin hallazgos críticos.

#### R-ACTA-063 — Responsive
> **EL SISTEMA DEBE** adaptar la vista a viewports pequeños (≤ 640 px): grilla apilada, botones
> a ancho completo.

* *Criterio de verificación*: inspección visual en 375 px y 1440 px.

#### R-ACTA-064 — Sin modificación de estilos globales
> **EL SISTEMA DEBE** no modificar `stitch-tokens.css`, el layout base ni `actas.css` (salvo el
> propio `actas-forms.css`).

* *Criterio de verificación*: `git diff` sin cambios en los archivos globales.

#### R-ACTA-065 — Sin DDL ni regresión de paridad BD
> **CUANDO** se implemente la pantalla, **EL SISTEMA DEBE** mantener `ddl-auto=none` y **NO
> DEBE** generar DDL, migraciones ni cambios de esquema ni de entidades JPA.

* *Criterio de verificación*: revisión del diff (sin `schema.sql`/`data.sql`, sin Flyway/Liquibase,
  sin cambios en `domain/`).

#### R-ACTA-056 — Sin duplicar cabecera ni pie
> **EL SISTEMA DEBE** renderizar una única cabecera y un único pie (paridad con el layout base).

* *Criterio de verificación*: `ActasMvcFormularioTest.getNuevaNoDuplicaCabeceraNiPie`.

---

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB / BD / Stitch / código) | Requisito nuevo | Prueba |
|---|---|---|
| WebForm `Form2276` (alta) | R-ACTA-014…R-ACTA-020 | `ActasMvcFormularioTest` |
| `F2276` (54 columnas físicas) | R-ACTA-014…R-ACTA-034 | `ActaRepositoryIT`, `ActaAltaTest` |
| `F22761` (15 columnas físicas) | R-ACTA-020…R-ACTA-047 | `ActaRepositoryIT` |
| `PRODUCTO` / `PRODUCT1` (vigencias) | R-ACTA-022, R-ACTA-037 | `ActaServiceTest` |
| Stitch "Carga Manual de Actas" (screen `2bd494a3…`) | R-ACTA-015…R-ACTA-020, R-ACTA-058, R-ACTA-063 | Inspección visual |
| Design System "Gestión Caminera Chaco" (`--stitch-*`) | R-ACTA-053, R-ACTA-064 | `ActasRecursosTest` |
| `ActasController` (rutas) | R-ACTA-010, R-ACTA-040, R-ACTA-041, R-ACTA-042, R-ACTA-044 | `ActasMvcFormularioTest`, `ActasMvcControllerTest` |
| `ActaForm` (binding) | R-ACTA-014…R-ACTA-025, V-9, V-10, V-11, V-12 | `ActasMvcFormularioTest` |
| `actas-forms.js` (JS puntual) | R-ACTA-021, R-ACTA-061 | Revisión del JS |
| AGENTS.md regla 8 (JS puntual) | R-ACTA-061 | Revisión |
| G5 (sin DDL) | R-ACTA-065 | Revisión del diff |
| R-ACTA-056 (sin duplicar cabecera/pie) | R-ACTA-057 | `ActasMvcFormularioTest` |

### 5.2 Matriz de paridad funcional (pantalla de alta)

| Operación legacy / Stitch | Requisito | Prueba prevista |
|---|---|---|
| Render del formulario (6 fieldsets) | R-ACTA-015, R-ACTA-016, R-ACTA-017, R-ACTA-020 | `ActasMvcFormularioTest.getNuevaRenderizaLosFieldsets` |
| Nro. Acta sólo en modo manual | R-ACTA-014 (V-11) | `ActasMvcFormularioTest.getNuevaNoRenderizaElNumeroDeActa` |
| Fecha en sólo lectura | V-10 | `ActasMvcFormularioTest.getNuevaRenderizaLaFechaEnSoloLectura` |
| Localidad/Provincia en sólo lectura | V-12 | `ActasMvcFormularioTest.getNuevaLocalidadYProvinciaEnSoloLectura` |
| Grilla de productos + botón + | R-ACTA-020 | `ActasMvcFormularioTest.getNuevaRenderizaLaGrillaDeProductos` |
| Confirmación verbatim + Cancelar | R-ACTA-059 | `ActasMvcFormularioTest.getNuevaMuestraLaConfirmacionYCancelar` |
| Bloqueo por lote/turno | R-ACTA-041, R-ACTA-042 (V-17) | `ActasMvcFormularioTest.getNuevaBloqueadaNoRenderizaElFormulario` |
| Errores por campo | V-9 | Inspección del HTML |
| Alta exitosa → redirect | R-ACTA-040 | `ActasMvcFormularioTest.postGuardarExitoso` |
| Error de negocio → banner | R-ACTA-040 | `ActasMvcFormularioTest.postGuardarConErrorDeNegocio` |
| CSRF obligatorio | R-ACTA-040 | `ActasMvcFormularioTest.postGuardarSinCsrf` |
| Turno/sucursal del contexto | R-ACTA-044 | `ActasMvcFormularioTest.postGuardarUsaElContextoDeSesion` |
| Tokens CSS | R-ACTA-053 | `ActasRecursosTest` + grep |
| Sin CDN | R-ACTA-060 | grep |
| JS vanilla | R-ACTA-061 | Revisión |
| Accesibilidad AA | R-ACTA-062 | axe-core |
| Responsive | R-ACTA-063 | Inspección |
| Sin duplicar cabecera/pie | R-ACTA-056 | `ActasMvcFormularioTest.getNuevaNoDuplicaCabeceraNiPie` |
| Sin DDL | R-ACTA-065 | Revisión del diff |

---

## 6. Desviaciones y decisiones

| # | Desviación | Justificación | Riesgo |
|---|---|---|---|
| **D-ACTA-01** | El formulario se renderiza por servidor (`GET /actas/nueva`), no por AJAX | Paridad legacy + R-ALC-006 (render por servidor). | Bajo |
| **D-ACTA-02** | La validación se hace en dos niveles: Bean Validation (binding) + `ActaService` (negocio) | Mensajes verbatim en ambos niveles (R-ACTA-040). | Bajo |
| **D-ACTA-03** | El número de Acta en modo manual es editable (`f276Id`); en modo turno lo genera el servicio | Paridad legacy (R-ACTA-036). | Bajo |
| **D-ACTA-04** | El servicio está mockeado en los tests MVC | La pantalla no habla con la BD (R-ACTA-001). | Bajo |
| **D-ACTA-05** | La búsqueda de CUIT por botón es placeholder (`window.alert`) | Sin endpoint de búsqueda implementado; fuera de alcance. | Bajo |
| **D-ACTA-06** | La cantidad se fuerza a 1 en el cliente (JS) para TipCal=3, además de validarse en el servicio | UX: evita enviar valores inválidos. | Bajo |
| **D-ACTA-07** | Los atributos derivados (totales, localidades) son `@Transient` y los calcula el servicio | No son columnas físicas (verificado contra `INFORMATION_SCHEMA`). | Bajo |
| **D-ACTA-08** | El listado y el alta comparten el mismo controlador (`ActasController`) | Cohesión del módulo 09; rutas bajo `/actas`. | Bajo |

---

## 7. Supuestos abiertos

| ID | Pregunta | Decisión provisional | Riesgo si se resuelve distinto |
|---|---|---|---|
| **SA-ACTA-01** | ¿La búsqueda de CUIT debe implementarse con endpoint propio o queda placeholder? | Placeholder (D-ACTA-05). | Medio |
| **SA-ACTA-02** | ¿El modo manual debe permitir editar el número de Acta o siempre lo genera el servicio? | Editable en modo manual (D-ACTA-03). | Bajo |
| **SA-ACTA-03** | ¿La fecha del Acta debe ser editable por el usuario o siempre precargada? | Precargada y de sólo lectura (V-10). | Bajo |
| **SA-ACTA-04** | ¿La validación de CUIT debe ser en el cliente o sólo en el servicio? | Sólo en el servicio (R-ACTA-028). | Bajo |
| **SA-ACTA-05** | ¿El código de barras (`F276CodBar`) debe generarse en el alta o en el listado? | Derivado, no es columna (D-ACTA-07). | Bajo |

---

## 8. Criterios de aceptación (puerta G-TEST)

1. `./mvnw clean test` en verde con **≥ 19 pruebas** del formulario (ActasMvcFormularioTest) y **sin
   regresión** sobre `ActasMvcControllerTest`, `ActasMvcManualTest`, `ActasMvcPopupTest`.
2. Inspección visual de la pantalla en 375 px y 1440 px sin desbordes.
3. `axe-core` in-page sin hallazgos críticos.
4. `git diff` sin cambios en `stitch-tokens.css`, layout base ni entidades JPA.
5. Sin secretos en el diff (G-SEC); ningún archivo de configuración por encima de 500 líneas (G4).
6. `ddl-auto=none` mantenido; sin DDL ni migraciones (G5).
7. G-POSTMAN: no aplica (sin endpoints REST nuevos/modificados).