# Spec — 04 Modal "Detalle de Carta de Porte / Guía de Tránsito" (Thymeleaf)

> **Estado**: `APROBADO` — aprobado en la puerta **G-SPEC** (2026-11-11).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-MDG-<NN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13.186702), alias `atppuestos` — **solo lectura**.
> - Transacción `Form506` / `DETALLE` (detalle de guía), WebForm `GxMultiForm`.
> - Tabla física `DETPRODP` (ver `00-alcance` y `01-nucleo-datos`).
> **Fuente visual**: Pantalla Stitch "Detalle de Guía - Modal Control de Cargas"
> (proyecto "Modern Spring Boot Portal", id `6019790973415672987`, screen `6f1c283c0c844b20989b16b84935d0f5`).
> Design System "Gestión Caminera Chaco" (`stitch-tokens.css`, tokens `--stitch-*`).
> **Módulo existente**: `control-cargas` (`atp-puestos-camineros`). Fragmento existente:
> `puestos/fragments/form506-detalle.html` (fragment `modal`, id `modalDetalle`).
> **Backend ya implementado**: `CargaMvcController` (`/cargas/detalle`) + `CargaForm`.
> **Alcance de escritura**: `openspec/features/**` (esta spec) y, en la fase de implementación,
> `atp-puestos-camineros/**`. **Sin DDL, sin migraciones** (G5). **G-POSTMAN: no aplica**
> (componente puramente frontend; no agrega ni modifica endpoints REST).

---

## 1. Propósito

Entregar el **componente modal** "Detalle de Carta de Porte / Guía de Tránsito" como un
fragmento Thymeleaf reutilizable, con una presentación moderna alineada al Design System
Stitch (tokens `--stitch-*`), que:

- muestre los datos de la guía (cabecera, fiscalización, flete, totales, trazabilidad);
- ofrezca acciones de **Imprimir** (placeholder) y **Descargar** (placeholder);
- se abra/cierre de forma accesible y no invasiva (JS puntual vanilla);
- se integre en `control-cargas.html` sin modificar estilos globales ni el layout base.

Es la **evolución visual** del fragmento legacy `form506-detalle.html` (`th:fragment="modal"`):
se renombra a `modalDetalleGuia` y se amplía con los campos del modal Stitch, manteniendo
paridad funcional con el legacy (mismos datos, mismos mensajes).

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Fragmento Thymeleaf `modal-detalle-guia.html` con `th:fragment="modalDetalleGuia"`. |
| 2 | Hoja de estilos `static/css/modal-detalle-guia.css` usando **exclusivamente** tokens `--stitch-*`. |
| 3 | Script `static/js/modal-detalle-guia.js` (vanilla, no invasivo). |
| 4 | Ampliación de `CargaForm` con los campos del modal (§3.2) con fallbacks dinámicos. |
| 5 | Integración del fragmento en `control-cargas.html` (reemplazando/extendiendo el include actual). |
| 6 | Accesibilidad: `role="dialog"`, `aria-modal="true"`, foco atrapado, cierre con `Esc`, foco restaurado. |
| 7 | Acciones: **Imprimir** (window.print / placeholder) y **Descargar** (placeholder, sin endpoint). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Endpoint REST nuevo de detalle de guía | El componente es puramente frontend; consume el `CargaForm` ya poblado por `CargaMvcController`. | — |
| Modificación del contrato REST `/api/v1/cargas*` | No hay cambios en el backend REST. | `control-cargas` (sin cambios) |
| Rediseño de `DETPRODP` / nuevas columnas | Paridad BD: `ddl-auto=none`, sin DDL (G5). | — |
| Lógica de negocio en el modal | Las reglas viven en `CargaService`; el modal es adaptador de vista. | — |
| Frameworks JS, bundlers, CDNs | Prohibido por AGENTS.md regla 8. | — |
| Modificación de estilos globales/layout | Regla del módulo: sólo CSS propio del modal. | — |
| Impresión real a PDF / descarga real | Placeholder (sin endpoint ni librería). | Backlog futuro |
| Autenticación/autorización del modal | La define `03-login`. | `03-login` |

---

## 3. Modelo legacy verificado

### 3.1 Tabla física `DETPRODP` (referencia, **no se modifica**)

La entidad `DetalleProductoPrimario` (`@Table(name="DETPRODP")`) ya mapea las columnas físicas
relevantes. El modal **sólo lee** de esta entidad a través de `CargaForm`; no agrega columnas
ni altera el esquema.

Columnas que el modal consume (ya mapeadas, con `@ColumnTransformer(read="RTRIM(...)")`):

| Campo lógico | Columna física | Tipo SQL | Uso en el modal |
|---|---|---|---|
| `dppChasis` | `DPPChasis` | `char(10)` | Chasis del vehículo |
| `dppAcopla` | `DPPAcopla` | `char(10)` | Acoplado |
| `dppKmFle` | `DPPKmFle` | `numeric(10,4)` | Km de flete |
| `dppTarxKm` | `DPPTarxKm` | `numeric(19,4)` | Tarifa por km |
| `dppMImpFle` | `DPPMImpFle` | `numeric(19,4)` | Monto de flete |
| `dppAlicFle` | `DPPAlicFle` | `numeric(10,4)` | Alícuota |
| `dppImpuFle` | `DPPImpuFle` | `numeric(19,4)` | Impuesto de flete |
| `dppAdicFle` | `DPPAdicFle` | `numeric(19,4)` | Adicional 10% |
| `dppImIBTot` | `DPPImIBTot` | `numeric(19,4)` | Subtotal flete |
| `dppAdIBTot` | `DPPAdIBTot` | `numeric(19,4)` | Subtotal traslado |
| `totalGeneral` | `@Transient` | — | Total general (calculado en servicio/vista) |
| `dppVehicu` | `DPPVehicu` | `char(10)` | Vehículo (fiscalizador / trazabilidad) |
| `dppFchMov` | `DPPFchMov` | `datetime` | Fecha de control / movimiento |
| `dppNCpbte` | `DPPNCpbte` | `char(15)` | Nro. comprobante |
| `dppTipoGui` | `DPPTipoGui` | `char(1)` | Tipo de comprobante / guía |
| `dppActaNro` | `DPPActaNro` | `int` | Expediente digital |
| `dppActaPue` | `DPPActaPue` | `smallint` | Puesto de ingreso |
| `dppAnuObs` | `DPPAnuObs` | `varchar(200)` | Observaciones (domicilio fiscal / notas) |
| `dppUltPaOp` | `DPPUltPaOp` | `char(10)` | Último operador (fiscalizador) |
| `conCuit` / `conRazSoc` | PK / `@Transient` | — | Contribuyente |
| `dppNroForm` | PK | — | Nro. de formulario |

> **Huérfanas**: las columnas de `DETPRODP` que existen en la BD y no en la KB se **mapean**
> (no se rediseñan). El modal no introduce nuevas columnas ni las expone si no están en la
> entidad. Fuente de autoridad: `INFORMATION_SCHEMA` de `Rentas` (G-DATA, sólo `SELECT`).

### 3.2 Campos del modal en `CargaForm` (ampliación)

`CargaForm` se amplía con los siguientes campos, **todos opcionales** (`String`/`BigDecimal`/
`LocalDateTime`/`Integer`/`Short`) y con **fallbacks dinámicos** (§4.4). Los nombres de los
getters/setters siguen la convención camelCase del formulario (`getChasis()`, `setKmFlete(...)`,
etc.), **independiente** del nombre de la columna física.

| Campo del modal | Tipo Java | Origen / fallback | Notas |
|---|---|---|---|
| `chasis` | `String` | `DetalleProductoPrimario.dppChasis` → `RTRIM` → `""` | Vehículo |
| `acoplado` | `String` | `dppAcopla` → `RTRIM` → `""` | Vehículo |
| `kmFlete` | `BigDecimal` | `dppKmFle` → `0` | Km de flete |
| `tarifaKm` | `BigDecimal` | `dppTarxKm` → `0` | Tarifa por km |
| `montoFlete` | `BigDecimal` | `dppMImpFle` → `0` | Monto de flete |
| `alicuota` | `BigDecimal` | `dppAlicFle` → `0` | Alícuota |
| `impuestoFlete` | `BigDecimal` | `dppImpuFle` → `0` | Impuesto de flete |
| `adicional10` | `BigDecimal` | `dppAdicFle` → `0` | Adicional 10% |
| `subtotalFlete` | `BigDecimal` | `dppImIBTot` → `0` | Subtotal flete |
| `subtotalTraslado` | `BigDecimal` | `dppAdIBTot` → `0` | Subtotal traslado |
| `totalGeneral` | `BigDecimal` | `DetalleProductoPrimario.totalGeneral` → `0` | Total general |
| `fiscalizador` | `String` | `dppUltPaOp` → `RTRIM` → contexto de sesión (usuario) → `""` | Último operador |
| `hashControl` | `String` | `dppNroForm` + puesto + fecha → placeholder determinista → `""` | Hash de control |
| `expedienteDigital` | `Integer` | `dppActaNro` → `null` → `""` | Expediente digital |
| `domicilioFiscal` | `String` | `dppAnuObs` → `RTRIM` → `""` | Domicilio fiscal / observaciones |
| `puestoIngreso` | `String` | `dppActaPue` → contexto de sesión (puesto) → `""` | Puesto de ingreso |
| `fechaControl` | `LocalDateTime` | `dppFchMov` → `LocalDateTime.now()` → `null` | Fecha de control |
| `tipoComprobante` | `String` | `dppTipoGui` → `RTRIM` → `dppTipoPr` → `"F"` | Tipo de comprobante |

> **Variables dinámicas con fallbacks**: cada campo del modal se resuelve en el controller
> `CargaMvcController` a partir del `DetalleProductoPrimario` disponible (si lo hay) y del
> `ContextoSesion`. Si un valor es `null`, se aplica el fallback documentado. La vista **no**
> inventa valores: sólo renderiza lo que el form trae.

### 3.3 Mensajes legacy verbatim (paridad observable)

| Situación | Texto legacy (verbatim) | Dónde se muestra |
|---|---|---|
| Guía marcada con éxito | `La guia ha sido marcada con exito` | Banner de éxito del modal (ya existente) |
| Guía duplicada | `Guia Duplicada , ya paso por algun puesto` | Banner de advertencia del modal (ya existente) |
| Usuario sin puesto asignado | `Usuario sin Puesto Asignado` | Banner de error (control-cargas) |

---

## 4. Requisitos EARS

### 4.1 Apertura y cierre del modal

#### R-MDG-01 — Apertura por servidor
> **CUANDO** el usuario active la acción "Ver detalle" de una guía (o `GET /cargas/detalle` con
> `mostrarModal=true`), **EL SISTEMA DEBE** renderizar el modal `modalDetalleGuia` con los datos
> de la guía seleccionada y **NO DEBE** requerir JavaScript para su aparición.

* *Criterio de verificación*: `@WebMvcTest` que comprueba `GET /cargas/detalle?dppNroForm=…` → 200 +
  vista `puestos/control-cargas` + presencia del fragmento `modalDetalleGuia` y del id `modalDetalle`.

#### R-MDG-02 — Cierre con botón Cerrar
> **CUANDO** el usuario active el botón "Cerrar" del modal, **EL SISTEMA DEBE** ocultar el modal
> y restaurar el foco al elemento que lo abrió.

* *Criterio de verificación*: prueba de navegador que hace click en "Cerrar" y comprueba que el modal
  deja de ser visible y que el foco vuelve al disparador.

#### R-MDG-03 — Cierre con tecla Escape
> **CUANDO** el modal esté abierto y el usuario presione `Esc`, **EL SISTEMA DEBE** cerrarlo
> (equivalente a "Cerrar").

* *Criterio de verificación*: prueba de navegador que abre el modal, envía `Escape` y comprueba el
  cierre.

#### R-MDG-04 — Cierre por click en backdrop
> **CUANDO** el usuario haga click fuera del contenido del modal (sobre el backdrop), **EL SISTEMA
> DEBE** cerrarlo.

* *Criterio de verificación*: prueba de navegador que hace click sobre el backdrop y comprueba el cierre.

#### R-MDG-05 — Atrapamiento de foco
> **MIENTRAS** el modal esté abierto, **EL SISTEMA DEBE** mantener el foco dentro del modal
> (tabulación cíclica) y **NO DEBE** permitir que el foco escape al contenido de fondo.

* *Criterio de verificación*: prueba de navegador que tabula y comprueba que el foco permanece dentro
  del modal; `axe-core` sin hallazgos de foco.

#### R-MDG-06 — Restauración de foco
> **CUANDO** el modal se cierre, **EL SISTEMA DEBE** devolver el foco al elemento que lo abrió
> (o, si no existe, al `body`).

* *Criterio de verificación*: prueba de navegador que abre y cierra el modal y comprueba el foco final.

### 4.2 Presentación de datos de la guía

#### R-MDG-07 — Cabecera de la guía
> **EL SISTEMA DEBE** mostrar en el modal la cabecera de la guía con: Nro. de formulario, CUIT,
> Razón social, Tipo de guía, Tipo de comprobante, Fecha de control y Puesto de ingreso.

* *Criterio de verificación*: test que renderiza el modal con un `CargaForm` poblado y comprueba la
  presencia de los 7 valores en el HTML.

#### R-MDG-08 — Sección de fiscalización
> **EL SISTEMA DEBE** mostrar la sección de fiscalización con: Fiscalizador, Hash de control,
> Expediente digital y Domicilio fiscal.

* *Criterio de verificación*: test que comprueba la presencia de los 4 campos y sus valores (con
  fallbacks cuando son `null`).

#### R-MDG-09 — Sección de flete
> **EL SISTEMA DEBE** mostrar la sección de flete con: Chasis, Acoplado, Km de flete, Tarifa por km,
> Monto de flete, Alícuota, Impuesto de flete y Adicional 10%.

* *Criterio de verificación*: test que comprueba la presencia de los 8 campos y sus valores.

#### R-MDG-10 — Sección de totales
> **EL SISTEMA DEBE** mostrar la sección de totales con: Subtotal flete, Subtotal traslado y
> Total general.

* *Criterio de verificación*: test que comprueba la presencia de los 3 campos y sus valores.

#### R-MDG-11 — Fallbacks dinámicos
> **CUANDO** un campo del modal sea `null` o vacío en `CargaForm`, **EL SISTEMA DEBE** mostrar el
> fallback documentado en §3.2 (cadena vacía para texto, `0` para importes, fecha actual para
> `fechaControl`) y **NO DEBE** mostrar `null` ni `undefined` en la vista.

* *Criterio de verificación*: test que renderiza el modal con un `CargaForm` con todos los campos del
  modal en `null` y comprueba que no aparece `null`/`undefined` y que los importes muestran `0`.

#### R-MDG-12 — Formato de importes
> **EL SISTEMA DEBE** formatear los importes (`kmFlete`, `tarifaKm`, `montoFlete`, `alicuota`,
> `impuestoFlete`, `adicional10`, `subtotalFlete`, `subtotalTraslado`, `totalGeneral`) con 2
> decimales y separador de miles.

* *Criterio de verificación*: test que comprueba el texto renderizado de un importe (ej. `1.234,56`).

#### R-MDG-13 — Formato de fecha de control
> **EL SISTEMA DEBE** mostrar `fechaControl` en formato `dd/MM/yyyy HH:mm` (o `dd/MM/yyyy` si no hay
> hora) y **NO DEBE** mostrar la representación ISO cruda.

* *Criterio de verificación*: test que comprueba el texto renderizado de la fecha.

### 4.3 Acciones del modal

#### R-MDG-14 — Acción Imprimir (placeholder)
> **CUANDO** el usuario active "Imprimir", **EL SISTEMA DEBE** invocar `window.print()` (o, en su
> defecto, abrir el diálogo de impresión del navegador) y **NO DEBE** realizar una llamada al
> servidor ni generar un PDF.

* *Criterio de verificación*: prueba de navegador que hace click en "Imprimir" y comprueba que se
  invoca `window.print` (mock) y que no hay request HTTP.

#### R-MDG-15 — Acción Descargar (placeholder)
> **CUANDO** el usuario active "Descargar", **EL SISTEMA DEBE** mostrar un mensaje informativo
> (toast/alert) indicando que la descarga estará disponible próximamente y **NO DEBE** realizar una
> llamada al servidor ni descargar un archivo.

* *Criterio de verificación*: prueba de navegador que hace click en "Descargar" y comprueba el mensaje
  visible y la ausencia de request HTTP.

#### R-MDG-16 — Botones con iconos y rótulos accesibles
> **EL SISTEMA DEBE** presentar los botones de acción (Cerrar, Imprimir, Descargar) con un icono
> (Material Symbols) **y** un rótulo accesible (`aria-label` o texto visible), y **NO DEBE** usar
> iconos sin texto alternativo.

* *Criterio de verificación*: inspección del HTML + `axe-core` sin hallazgos de botones sin nombre.

### 4.4 Integración en `control-cargas.html`

#### R-MDG-17 — Include del fragmento
> **EL SISTEMA DEBE** incluir el fragmento `modalDetalleGuia` en `control-cargas.html` en lugar del
> include legacy `form506-detalle :: modal`, manteniendo la condición de render
> (`mostrarModal == true`).

* *Criterio de verificación*: test que comprueba que `GET /cargas/detalle` renderiza el nuevo
  fragmento (`modal-detalle-guia` / `modalDetalleGuia`) y que el include legacy ya no se usa.

#### R-MDG-18 — Sin regresión de la vista existente
> **CUANDO** se integre el nuevo modal, **EL SISTEMA DEBE** mantener sin cambios el resto de
> `control-cargas.html` (tabla de formularios, filtros, métricas, paginación) y **NO DEBE**
> eliminar funcionalidad existente.

* *Criterio de verificación*: la suite `CargaMvcControllerTest` y `CargaCorreccionMvcTest` siguen en
  verde sin modificaciones (salvo las que actualicen el nombre del fragmento).

#### R-MDG-19 — Carga de CSS y JS del modal
> **EL SISTEMA DEBE** cargar `modal-detalle-guia.css` y `modal-detalle-guia.js` en
> `control-cargas.html` (vía `th:href`/`th:src`), **sólo** cuando el modal esté presente o de forma
> incondicional sin bloquear el render.

* *Criterio de verificación*: test que comprueba la presencia de los `<link>`/`<script>` en el HTML
  renderizado.

### 4.5 No funcionales

#### R-MDG-20 — Tokens exclusivos
> **EL SISTEMA DEBE** usar **exclusivamente** tokens `--stitch-*` en `modal-detalle-guia.css` y
> **NO DEBE** contener literales hexadecimales (`#xxx`), `rgb(...)`, `rgba(...)` ni nombres de
> color CSS.

* *Criterio de verificación*: test con regex que busca literales de color en el CSS del modal y falla
  si encuentra alguno (patrón `CargaCorreccionMvcTest.cssSinHex`).

#### R-MDG-21 — Sin CDN ni recursos externos
> **EL SISTEMA DEBE** no referenciar CDNs, fuentes externas ni scripts de terceros en el modal ni en
> su CSS/JS; los iconos deben ser Material Symbols ya disponibles en el layout base.

* *Criterio de verificación*: revisión del HTML/CSS/JS del modal en busca de `http://`/`https://`
  externos; `grep` sin resultados.

#### R-MDG-22 — JS puntual vanilla
> **EL SISTEMA DEBE** implementar el comportamiento del modal (abrir/cerrar, foco, escape,
> backdrop, imprimir, descargar) en JavaScript **vanilla** (sin frameworks, sin bundlers, sin
> `npm`), ubicado en `static/js/modal-detalle-guia.js`.

* *Criterio de verificación*: revisión del archivo: sin `import` de módulos externos, sin `require`,
  sin llamadas `fetch`/`XMLHttpRequest`.

#### R-MDG-23 — Accesibilidad AA
> **EL SISTEMA DEBE** mantener contraste ≥ 4,5:1, asociar `<label>` a cada dato, garantizar foco
> visible y navegación por teclado, y anunciar los cambios (mensajes) con `aria-live`.

* *Criterio de verificación*: `axe-core` in-page en dos estados (modal cerrado y modal abierto) sin
  hallazgos críticos; `lighthouse_audit` sin audits de accesibilidad en rojo.

#### R-MDG-24 — Responsive
> **EL SISTEMA DEBE** adaptar el modal a viewports pequeños (≤ 640 px): ancho completo con margen,
> scroll vertical interno y botones apilados.

* *Criterio de verificación*: inspección visual en 375 px y 1440 px.

#### R-MDG-25 — Sin modificación de estilos globales
> **EL SISTEMA DEBE** no modificar `stitch-tokens.css`, `control-cargas.css` ni el layout base; los
> estilos del modal deben vivir exclusivamente en `modal-detalle-guia.css`.

* *Criterio de verificación*: `git diff` sin cambios en los archivos globales (salvo el include en
  `control-cargas.html`).

#### R-MDG-26 — Sin regresión de paridad BD
> **CUANDO** se implemente el modal, **EL SISTEMA DEBE** mantener `ddl-auto=none` y **NO DEBE**
> generar DDL, migraciones ni cambios de esquema.

* *Criterio de verificación*: revisión del diff: sin `schema.sql`/`data.sql`, sin Flyway/Liquibase,
  sin `@GeneratedValue` nuevo, sin cambios en entidades JPA.

---

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB / BD / Stitch) | Requisito nuevo | Prueba |
|---|---|---|
| `form506-detalle.html` (fragment `modal`, id `modalDetalle`) | R-MDG-01, R-MDG-17 | `CargaCorreccionMvcTest.detalleRenderizaModal` |
| `DETPRODP` (columnas físicas) | R-MDG-07…R-MDG-10 | Test de render del modal |
| `CargaForm` (campos existentes) | R-MDG-07, R-MDG-11 | Test de render |
| Stitch "Detalle de Guía - Modal Control de Cargas" | R-MDG-07…R-MDG-16 | Inspección visual + axe |
| Design System "Gestión Caminera Chaco" (`--stitch-*`) | R-MDG-20, R-MDG-25 | Test de tokens |
| `control-cargas.html` (include actual) | R-MDG-17, R-MDG-18 | `CargaMvcControllerTest` |
| AGENTS.md regla 8 (JS puntual) | R-MDG-22 | Revisión del JS |
| AGENTS.md regla 5 (sin secretos) | R-MDG-21 | Revisión |
| G5 (sin DDL) | R-MDG-26 | Revisión del diff |

### 5.2 Matriz de paridad funcional

| Operación legacy / Stitch | Requisito | Prueba prevista |
|---|---|---|
| Abrir modal (servidor) | R-MDG-01 | `MvcModalAbre` |
| Cerrar con botón | R-MDG-02 | Prueba de navegador |
| Cerrar con Escape | R-MDG-03 | Prueba de navegador |
| Cerrar por backdrop | R-MDG-04 | Prueba de navegador |
| Foco atrapado | R-MDG-05 | Prueba de navegador + axe |
| Restaurar foco | R-MDG-06 | Prueba de navegador |
| Cabecera de guía | R-MDG-07 | `MvcModalCabecera` |
| Fiscalización | R-MDG-08 | `MvcModalFiscalizacion` |
| Flete | R-MDG-09 | `MvcModalFlete` |
| Totales | R-MDG-10 | `MvcModalTotales` |
| Fallbacks | R-MDG-11 | `MvcModalFallbacks` |
| Formato importes | R-MDG-12 | `MvcModalFormatoImportes` |
| Formato fecha | R-MDG-13 | `MvcModalFormatoFecha` |
| Imprimir | R-MDG-14 | Prueba de navegador |
| Descargar | R-MDG-15 | Prueba de navegador |
| Iconos accesibles | R-MDG-16 | axe |
| Include | R-MDG-17 | `CargaCorreccionMvcTest` actualizado |
| Sin regresión | R-MDG-18 | Suite existente verde |
| Carga CSS/JS | R-MDG-19 | `MvcModalAssets` |
| Tokens | R-MDG-20 | `MvcModalCssSinHex` |
| Sin CDN | R-MDG-21 | Revisión |
| JS vanilla | R-MDG-22 | Revisión |
| Accesibilidad | R-MDG-23 | axe + lighthouse |
| Responsive | R-MDG-24 | Inspección |
| Sin cambio global | R-MDG-25 | `git diff` |
| Sin DDL | R-MDG-26 | Revisión del diff |

---

## 6. Desviaciones y decisiones

| # | Desviación | Justificación | Riesgo |
|---|---|---|---|
| **D-MDG-01** | El modal se renderiza **siempre** en el DOM cuando `mostrarModal=true` (no se carga por AJAX) | Paridad con el legacy y con R-MDG-01 (render por servidor, R-ALC-006). | Bajo |
| **D-MDG-02** | Los campos del modal se añaden a `CargaForm` (form-backing) en lugar de crear un DTO nuevo | Reutiliza el form existente y el flujo `/cargas/detalle`; evita duplicar el modelo. | Bajo |
| **D-MDG-03** | `hashControl` es **derivado determinista**, no un hash criptográfico real | No hay endpoint ni columna dedicada; es un placeholder informativo. | Bajo |
| **D-MDG-04** | "Descargar" es **placeholder** (sin endpoint) | Fuera de alcance (§2.2). | Bajo |
| **D-MDG-05** | "Imprimir" usa `window.print()` del navegador | Sin librería de PDF; placeholder. | Bajo |
| **D-MDG-06** | El fragmento legacy `form506-detalle.html` se **mantiene** (no se borra) pero deja de usarse en `control-cargas.html` | Evita romper tests que lo referencian; se marca como deprecado. | Bajo |
| **D-MDG-07** | Los fallbacks se resuelven en el **controller** (`CargaMvcController`), no en la vista ni en el servicio | La vista sólo renderiza; el servicio no cambia; el controller ya pobla `CargaForm`. | Bajo |

---

## 7. Supuestos abiertos

| ID | Pregunta | Decisión provisional | Riesgo si se resuelve distinto |
|---|---|---|---|
| **SA-MDG-01** | ¿El modal debe abrirse automáticamente al cargar `/cargas/detalle` (como hoy) o sólo bajo demanda por JS? | Automáticamente (paridad legacy). | Bajo |
| **SA-MDG-02** | ¿Los importes del modal deben formatearse con `DecimalFormat` del servidor o con JS? | Servidor (Thymeleaf `#numbers`), para paridad y accesibilidad. | Bajo |
| **SA-MDG-03** | ¿El `hashControl` debe ser un UUID real o un derivado determinista? | Derivado determinista (placeholder). | Bajo |
| **SA-MDG-04** | ¿El fragmento legacy `form506-detalle.html` debe eliminarse o mantenerse deprecado? | Mantenerse deprecado (D-MDG-06). | Bajo |
| **SA-MDG-05** | ¿La prueba de navegador (Playwright/Selenium) está disponible en CI? | Sí, se asume `webapp-testing`. | Medio |
| **SA-MDG-06** | ¿Los campos `expedienteDigital`/`puestoIngreso` deben mapearse a columnas existentes o a datos de sesión? | Columnas existentes con fallback a sesión (§3.2). | Medio |

---

## 8. Criterios de aceptación (puerta G-TEST)

1. `./mvnw clean test` en verde con **≥ 12 pruebas nuevas** del modal (render, fallbacks, formato,
   assets, tokens) y **sin regresión** sobre `CargaMvcControllerTest` / `CargaCorreccionMvcTest`.
2. Prueba de navegador real: abrir modal, cerrar (botón, Escape, backdrop), tabular dentro del modal,
   imprimir (mock `window.print`), descargar (mensaje placeholder).
3. `axe-core` in-page en dos estados (modal cerrado y abierto) sin hallazgos críticos.
4. `git diff` sin cambios en `stitch-tokens.css`, `control-cargas.css` ni el layout base (salvo el
   include en `control-cargas.html`).
5. Sin secretos en el diff (G-SEC); ningún archivo de configuración por encima de 500 líneas (G4).
6. `ddl-auto=none` mantenido; sin DDL ni migraciones (G5).
7. G-POSTMAN: no aplica (no hay endpoints nuevos/modificados).