# Plan — 09 "Carga Manual de Actas" (Formulario 2276 · alta) — Thymeleaf

> **Estado**: `PROPUESTA` — pendiente de aprobación en la puerta **G-SPEC**.
> Acompaña a `spec.md` (misma puerta). Documento de arquitectura; no contiene tareas (ver `tasks.md`).
> **Paridad de BD**: `ddl-auto=none`, sin DDL, sin migraciones (G5). Las entidades `Acta` y
> `ActaProducto` ya mapean `F2276` y `F22761`; **no se modifican**.
> **G-POSTMAN**: no aplica (sólo controllers MVC; sin cambios en `/api/v1/**`).

---

## 1. Estrategia y ubicación

### 1.1 Qué produce este módulo

La **capa de presentación** del alta del Acta (Formulario 2276 · "Carga manual"), como evolución
visual del WebForm legacy `Form2276`. El backend **no se rediseña**: se reutiliza `ActasController`,
`ActaForm`, `ActaService` y las entidades ya mapeadas.

```text
F2276 / F22761 (BD, sólo lectura) → Acta / ActaProducto (JPA, ya mapeados)
                                            ↓
                                  ActaService / ActasController
                                            ↓
                                  ActaForm (binding) + carga-manual-actas.html
                                            ↓
                                  fragments/acta-productos.html (grilla)
                                  static/css/actas-forms.css   (tokens --stitch-*)
                                  static/js/actas-forms.js     (vanilla)
```

### 1.2 Dónde vive el código

| Artefacto | Ubicación | Razón |
|---|---|---|
| Controller MVC | `atp-puestos-camineros/src/main/java/.../web/mvc/ActasController.java` | Expone `/actas`, `/actas/nueva`, `/actas/guardar`. |
| Form object | `atp-puestos-camineros/src/main/java/.../web/mvc/form/ActaForm.java` | Binding + traducción a `ActaAlta`. |
| Form de producto | `atp-puestos-camineros/src/main/java/.../web/mvc/form/ActaProductoForm.java` | Fila de la grilla. |
| Plantilla principal | `atp-puestos-camineros/src/main/resources/templates/puestos/carga-manual-actas.html` | Vista de alta. |
| Fragmento de grilla | `atp-puestos-camineros/src/main/resources/templates/puestos/fragments/acta-productos.html` | Grilla de productos. |
| CSS del módulo | `atp-puestos-camineros/src/main/resources/static/css/actas-forms.css` | Estilos propios, sólo tokens. |
| JS del módulo | `atp-puestos-camineros/src/main/resources/static/js/actas-forms.js` | JS puntual vanilla. |
| CSS global (no se modifica) | `atp-puestos-camineros/src/main/resources/static/css/actas.css` | Estilos base del módulo. |
| Entidad `Acta` | `atp-puestos-camineros/src/main/java/.../domain/Acta.java` | `@Table(name="F2276")`, sin cambios. |
| Entidad `ActaProducto` | `atp-puestos-camineros/src/main/java/.../domain/ActaProducto.java` | `@Table(name="F22761")`, sin cambios. |

> **El módulo desplegable `atp-puestos-camineros` es el único artefacto que se modifica.** El
> núcleo (`atp-nucleo-compartido`) no se toca: no hay nuevos endpoints ni entidades.

### 1.3 Los cimientos de vista ya existen

A diferencia de `02-abm-productos`, este módulo **no** crea shell de vistas: reutiliza el layout
`base-stitch`, `stitch-tokens.css` y los estilos de `actas.css` ya existentes. **No se modifican**
(R-ACTA-064).

---

## 2. (a) Paridad JPA — `F2276` y `F22761` (sin cambios)

> **No se modifica el esquema ni las entidades.** `Acta` y `ActaProducto` ya mapean `F2276` y
> `F22761` con `ddl-auto=none`. Este módulo **sólo lee/escribe** a través de `ActaService`.

### 2.1 Tabla `F2276` → `Acta` (cabecera)

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación (estado actual) |
|---|---|---|---|---|---|
| `f276Id` | `F276Id` | `int` | no | `Integer` | `@Id @Column(name="F276Id", nullable=false)` |
| `sucTipo` | `SucTipo` | `char(3)` | no | `String` | `@Id @ColumnTransformer(read="RTRIM(SucTipo)")` |
| `sucNroId` | `SucNroId` | `smallint` | no | `Integer` | `@Id @Column(name="SucNroId", nullable=false)` |
| `pueCod` | `PueCod` | `smallint` | sí (huérfana) | `Integer` | `@Column(name="PueCod")` → default `null` |
| `cpcId` | `CPCId` | `int` | sí (huérfana) | `Integer` | `@Column(name="CPCId")` → default `null` |
| `cpcPerId` | `CPCPerId` | `int` | no | `Integer` | `@Column(name="CPCPerId", nullable=false)` |
| `f276CuitVe` | `F276CuitVe` | `decimal(11,0)` | sí | `BigDecimal` | `@Column(name="F276CuitVe", precision=11, scale=0)` |
| `f276VenRS` | `F276VenRS` | `char(35)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276VenRS)")` |
| `f276VenDom` | `F276VenDom` | `char(50)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276VenDom)")` |
| `f276VenCLo` | `F276VenCLo` | `int` | sí | `Integer` | `@Column(name="F276VenCLo")` |
| `f276TCpbte` | `F276TCpbte` | `smallint` | sí | `Integer` | `@Column(name="F276TCpbte")` |
| `f276NCpbte` | `F276NCpbte` | `char(15)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276NCpbte)")` |
| `f276FchMov` | `F276FchMov` | `datetime` | sí | `LocalDateTime` | `@Column(name="F276FchMov")` |
| `f276CuitDe` | `F276CuitDe` | `decimal(11,0)` | sí | `BigDecimal` | `@Column(name="F276CuitDe", precision=11, scale=0)` |
| `f276DestRS` | `F276DestRS` | `char(35)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276DestRS)")` |
| `f276DestDo` | `F276DestDo` | `char(50)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276DestDo)")` |
| `f276DestCL` | `F276DestCL` | `int` | sí | `Integer` | `@Column(name="F276DestCL")` |
| `f276CuitTr` | `F276CuitTr` | `decimal(11,0)` | sí | `BigDecimal` | `@Column(name="F276CuitTr", precision=11, scale=0)` |
| `f276TranRS` | `F276TranRS` | `char(35)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276TranRS)")` |
| `f276TranDo` | `F276TranDo` | `char(50)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276TranDo)")` |
| `f276TranCL` | `F276TranCL` | `int` | sí | `Integer` | `@Column(name="F276TranCL")` |
| `f276Vehicu` | `F276Vehicu` | `char(20)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Vehicu)")` |
| `f276Chasis` | `F276Chasis` | `char(10)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Chasis)")` |
| `f276Acopla` | `F276Acopla` | `char(10)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Acopla)")` |
| `f276MImpFl` | `F276MImpFl` | `money` | sí | `BigDecimal` | `@Column(name="F276MImpFl", precision=19, scale=4)` |
| `f276KmFle` | `F276KmFle` | `smallmoney` | sí | `BigDecimal` | `@Column(name="F276KmFle", precision=10, scale=4)` |
| `f276TarxKm` | `F276TarxKm` | `money` | sí | `BigDecimal` | `@Column(name="F276TarxKm", precision=19, scale=4)` |
| `f276AlicFl` | `F276AlicFl` | `smallmoney` | sí | `BigDecimal` | `@Column(name="F276AlicFl", precision=10, scale=4)` |
| `f276ImpuFl` | `F276ImpuFl` | `money` | sí | `BigDecimal` | `@Column(name="F276ImpuFl", precision=19, scale=4)` |
| `f276AdicFl` | `F276AdicFl` | `money` | sí | `BigDecimal` | `@Column(name="F276AdicFl", precision=19, scale=4)` |
| `f276AdIBTo` | `F276AdIBTo` | `money` | sí | `BigDecimal` | `@Column(name="F276AdIBTo", precision=19, scale=4)` |
| `f276AdCMTo` | `F276AdCMTo` | `money` | sí | `BigDecimal` | `@Column(name="F276AdCMTo", precision=19, scale=4)` |
| `f276PatVeh` | `F276PatVeh` | `char(10)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276PatVeh)")` |
| `f276Operad` | `F276Operad` | `char(10)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Operad)")` |
| `lf276Id` | `LF276ID` | `int` | sí | `Integer` | `@Column(name="LF276ID")` |
| `f276FchPCC` | `F276FchPCC` | `datetime` | sí | `LocalDateTime` | `@Column(name="F276FchPCC")` |
| `turNroId` | `TurNroId` | `int` | sí | `Integer` | `@Column(name="TurNroId")` |
| `f276Clasif` | `F276Clasif` | `char(1)` | no | `String` | `@Column(name="F276Clasif", nullable=false, length=1)` + RTRIM |
| `f276ForPag` | `F276ForPag` | `char(3)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276ForPag)")` |
| `f276NumPag` | `F276NumPag` | `char(20)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276NumPag)")` |
| `f276Multa` | `F276Multa` | `money` | no | `BigDecimal` | `@Column(name="F276Multa", nullable=false, precision=19, scale=4)` |
| `f276OrCod` | `F276OrCod` | `int` | sí | `Integer` | `@Column(name="F276OrCod")` |
| `f276Origen` | `F276Origen` | `char(50)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Origen)")` |
| `f276DeCod` | `F276DeCod` | `int` | sí | `Integer` | `@Column(name="F276DeCod")` |
| `f276Destin` | `F276Destin` | `char(50)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Destin)")` |
| `f276Estad` | `F276Estad` | `char(1)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Estad)")` |
| `f276GenWeb` | `F276GenWeb` | `char(1)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276GenWeb)")` |
| `f276BanChe` | `F276BanChe` | `char(40)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276BanChe)")` |
| `f276Vers` | `F276Vers` | `char(18)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276Vers)")` |
| `f276FeGra` | `F276FeGra` | `datetime` | sí | `LocalDateTime` | `@Column(name="F276FeGra")` |
| `f276SnSnNu` | `F276SnSnNu` | `decimal(10,0)` | sí | `BigDecimal` | `@Column(name="F276SnSnNu", precision=10, scale=0)` |
| `f276FePag` | `F276FePag` | `datetime` | sí | `LocalDateTime` | `@Column(name="F276FePag")` |
| `f276ActaTi` | `F276ActaTi` | `char(1)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276ActaTi)")` |
| `f276FormTi` | `F276FormTi` | `smallint` | sí | `Integer` | `@Column(name="F276FormTi")` |
| `f276Total` | derivado | — | — | `BigDecimal` | `@Transient` (D-ACTA-07) |
| `f276AdiTot` | derivado | — | — | `BigDecimal` | `@Transient` |
| `f276ImpTotal` | derivado | — | — | `BigDecimal` | `@Transient` |
| `f276ImIBTot` | derivado | — | — | `BigDecimal` | `@Transient` |
| `f276ImCMTot` | derivado | — | — | `BigDecimal` | `@Transient` |
| `f276ProCan` | derivado | — | — | `Integer` | `@Transient` |
| `f276GenMan` | derivado | — | — | `String` | `@Transient` |
| `f276CodBar` | derivado | — | — | `String` | `@Transient` (SA-ACTA-05) |
| `fCalFlete` | derivado | — | — | `Boolean` | `@Transient` |
| `f276VenLoc` | derivado | — | — | `String` | `@Transient` |
| `f276VenProv` | derivado | — | — | `String` | `@Transient` |
| `f276DestLoc` | derivado | — | — | `String` | `@Transient` |
| `f276DestProv` | derivado | — | — | `String` | `@Transient` |
| `f276TranLoc` | derivado | — | — | `String` | `@Transient` |
| `f276TranProv` | derivado | — | — | `String` | `@Transient` |

### 2.2 Tabla `F22761` → `ActaProducto` (detalle)

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación (estado actual) |
|---|---|---|---|---|---|
| `f276Id` | `F276Id` | `int` | no | `Integer` | `@Id @Column(name="F276Id", nullable=false)` |
| `sucTipo` | `SucTipo` | `char(3)` | no | `String` | `@Id @ColumnTransformer(read="RTRIM(SucTipo)")` |
| `sucNroId` | `SucNroId` | `smallint` | no | `Integer` | `@Id @Column(name="SucNroId", nullable=false)` |
| `prodCod` | `ProdCod` | `smallint` | no | `Integer` | `@Id @Column(name="ProdCod", nullable=false)` |
| `f276RZona` | `F276RZona` | `char(1)` | no | `String` | `@Id @ColumnTransformer(read="RTRIM(F276RZona)")` |
| `f276RCant` | `F276RCant` | `money` | sí | `BigDecimal` | `@Column(name="F276RCant", precision=19, scale=4)` |
| `f276RMarCM` | `F276RMarCM` | `char(1)` | sí | `String` | `@ColumnTransformer(read="RTRIM(F276RMarCM)")` |
| `f276RMImpo` | `F276RMImpo` | `money` | sí | `BigDecimal` | `@Column(name="F276RMImpo", precision=19, scale=4)` |
| `f276RFijo` | `F276RFijo` | `money` | sí | `BigDecimal` | `@Column(name="F276RFijo", precision=19, scale=4)` |
| `f276RPreCM` | `F276RPreCM` | `money` | sí | `BigDecimal` | `@Column(name="F276RPreCM", precision=19, scale=4)` |
| `f276RAlicu` | `F276RAlicu` | `smallmoney` | sí | `BigDecimal` | `@Column(name="F276RAlicu", precision=10, scale=4)` |
| `f276RImpIB` | `F276RImpIB` | `money` | sí | `BigDecimal` | `@Column(name="F276RImpIB", precision=19, scale=4)` |
| `f276RImpCM` | `F276RImpCM` | `money` | sí | `BigDecimal` | `@Column(name="F276RImpCM", precision=19, scale=4)` |
| `f276Corrig` | `F276Corrig` | `char(1)` | sí (huérfana) | `String` | `@ColumnTransformer(read="RTRIM(F276Corrig)")` → default `null` |
| `f276RSPre` | `F276RSPre` | `money` | sí | `BigDecimal` | `@Column(name="F276RSPre", precision=19, scale=4)` |

### 2.3 Defaults de aplicación

- Las columnas `NOT NULL` (`F276Id`, `SucTipo`, `SucNroId`, `CPCPerId`, `F276Clasif`, `F276Multa` en
  `F2276`; `F276Id`, `SucTipo`, `SucNroId`, `ProdCod`, `F276RZona` en `F22761`) se resuelven en
  `ActaService` al persistir (numeración, sucursal del contexto, etc.). **No se agregan defaults de
  BD.**
- Las columnas huérfanas (`PueCod`, `CPCId`, `F276Corrig`) default `null`.
- Los atributos derivados (`@Transient`) default `null`/`0` según el servicio.

### 2.4 Regla de paridad

- **Sin DDL**: no se agregan columnas, no se alteran tipos, no se crean FK.
- **Sin cambios en las entidades**: `Acta` y `ActaProducto` no se modifican por este módulo.
- **Sin cambios en repositorios**: `ActaRepository`, `ActaProductoRepository` no se modifican.

---

## 3. (b) Contrato REST

> **No hay cambios en el contrato REST.** El módulo es puramente MVC y consume `ActaForm`/`ActaService`.
> No se agregan, modifican ni eliminan endpoints REST (`/api/v1/**`).

### 3.1 Endpoints MVC existentes (sin cambios)

| Método | Ruta | Uso en la pantalla | Respuesta |
|---|---|---|---|
| GET | `/actas` | Listado de Actas (paginado + filtros) | vista `puestos/actas-lista` |
| GET | `/actas/nueva` (params `turNroId`, `manual`) | Formulario de alta | vista `puestos/carga-manual-actas` |
| POST | `/actas/guardar` | Alta del Acta | 200 (re-render con errores) o 302 → `/actas` |

### 3.2 G-POSTMAN

**No aplica**: no se agregan, modifican ni eliminan endpoints REST. La colección
`docs/postman/ATPPuestos-API.postman_collection.json` no cambia por este módulo.

---

## 4. (c) Contrato de vistas Thymeleaf

### 4.1 Rutas y plantillas

| Método | Ruta | Vista | Uso |
|---|---|---|---|
| GET | `/actas` | `puestos/actas-lista` | Listado |
| GET | `/actas/nueva` | `puestos/carga-manual-actas` | Alta |
| POST | `/actas/guardar` | `puestos/carga-manual-actas` | Re-render con errores |

| Plantilla | Rol |
|---|---|
| `templates/puestos/carga-manual-actas.html` | Pantalla de alta (6 fieldsets). |
| `templates/puestos/fragments/acta-productos.html` | Fragmento `grilla` de productos. |
| `templates/layout/base-stitch.html` | Layout base (no se modifica). |
| `templates/puestos/actas-lista.html` | Listado (fuera de esta spec de alta). |

### 4.2 View model (atributos del modelo)

| Atributo | Tipo | Origen |
|---|---|---|
| `actaForm` | `ActaForm` | Controller (formulario inicial o re-render). |
| `bloqueado` | `boolean` | `ActaPreparada.bloqueado()`. |
| `mensajeBloqueo` | `String` | `ActaPreparada.mensajeBloqueo()`. |
| `turnoEstado` | `String` | `ActaPreparada.turnoEstado()`. |
| `turnoActivo` | `boolean` | Contexto de sesión. |
| `manual` | `boolean` | Parámetro de la URL. |
| `turNroId` | `Integer` | Parámetro de la URL. |
| `productosDisponibles` | `List<?>` | `ActaService.productosDisponibles()`. |
| `locCodVenDerivado` | `boolean` | `actaForm.f276VenCLo != null`. |
| `locCodDesDerivado` | `boolean` | `actaForm.f276DestCL != null`. |
| `locCodTranDerivado` | `boolean` | `actaForm.f276TranCL != null`. |
| `mensajeError` | `String` | Resultado del servicio (alta fallida). |

### 4.3 Form object — `ActaForm`

Campos (binding con el legacy, ver `ActaForm.java`):

- Datos del Acta: `f276Id` (sólo manual), `f276ActaTi`, `f276Clasif`, `f276FchMov`.
- Vendedor: `f276CuitVe`, `f276VenRS`, `f276VenDom`, `f276VenCLo`, `f276VenLoc`, `f276VenProv`.
- Destinatario: `f276CuitDe`, `f276DestRS`, `f276DestDo`, `f276DestCL`, `f276DestLoc`, `f276DestProv`.
- Transportista: `f276CuitTr`, `f276TranRS`, `f276TranDo`, `f276TranCL`, `f276TranLoc`, `f276TranProv`.
- Transporte: `f276TCpbte`, `f276NCpbte`, `f276Chasis`, `f276Acopla`, `f276KmFle`.
- Detalle: `productos` (`List<ActaProductoForm>`).
- Modo: `manual`, `turNroId`, `contexto`.

Traducción a `ActaAlta` mediante `aAlta()`.

### 4.4 Fragmento de grilla — `acta-productos.html`

Columnas: Código, Descripción, Tn. + acción Eliminar. Cada control tiene su `<label for>` (oculto
visualmente) con `data-rotulo` para renumeración (R-ACTA-055). El botón "+" agrega filas desde
una plantilla `<template id="acta-producto-plantilla">`.

### 4.5 CSS — `actas-forms.css`

- Sólo tokens `--stitch-*`. Sin literales hex/rgb/rgba (salvo `color-mix` con tokens).
- Prefijo de clase `atp-actas__*` / `atp-field__*` (consistente con el resto del módulo).
- Responsive: `@media (max-width: 640px)`.

### 4.6 JS — `actas-forms.js`

Alcance **estrictamente mecánico** (R-ACTA-061, regla 8):

| Operación | Qué hace | Qué **no** hace |
|---|---|---|
| Alta de fila | Clona la plantilla, renumera, agrega al cuerpo. | No llama al servidor. |
| Baja de fila | Elimina la fila, renumera, recalcula totales. | No valida en servidor. |
| Renumeración | Actualiza `name`/`id`/`for`/`aria-label` de cada control. | — |
| Sincronización por producto | Completa la descripción y fuerza cantidad a 1 si `TipCal=3`. | — |
| Totales | Suma las cantidades y muestra en `#acta-total-toneladas`. | — |
| Búsqueda CUIT | Muestra `window.alert('Búsqueda de CUIT no implementada')`. | No busca en servidor. |
| Confirmación | `window.confirm(MENSAJE_CONFIRMACION)` antes de enviar. | — |
| Impresión | `window.print()`. | No genera PDF. |

Sin `import`, sin `require`, sin `fetch`, sin CDNs.

---

## 5. (d) Decisiones

| # | Decisión | Justificación |
|---|---|---|
| **D-ACTA-01** | El formulario se renderiza por servidor (`GET /actas/nueva`) | Paridad legacy + R-ALC-006. |
| **D-ACTA-02** | Validación en dos niveles: Bean Validation + `ActaService` | Mensajes verbatim en ambos (R-ACTA-040). |
| **D-ACTA-03** | Nro. Acta editable sólo en modo manual | Paridad legacy (R-ACTA-036). |
| **D-ACTA-04** | Servicio mockeado en tests MVC | La pantalla no habla con la BD (R-ACTA-001). |
| **D-ACTA-05** | Búsqueda de CUIT placeholder | Sin endpoint implementado. |
| **D-ACTA-06** | Cantidad forzada a 1 en cliente para TipCal=3 | UX. |
| **D-ACTA-07** | Atributos derivados `@Transient` | No son columnas físicas. |
| **D-ACTA-08** | Listado y alta comparten controlador | Cohesión del módulo 09. |

---

## 6. Supuestos explícitos

| ID | Supuesto | Se resuelve en |
|---|---|---|
| SA-ACTA-01 | La búsqueda de CUIT queda placeholder | `spec.md` §7 |
| SA-ACTA-02 | El modo manual permite editar el Nro. Acta | `spec.md` §7 |
| SA-ACTA-03 | La fecha del Acta es precargada y de sólo lectura | `spec.md` §7 |
| SA-ACTA-04 | La validación de CUIT es sólo en el servicio | `spec.md` §7 |
| SA-ACTA-05 | El código de barras es derivado | `spec.md` §7 |

---

## 7. Alcance excluido

- Modificación del contrato REST (no hay endpoints nuevos ni cambios).
- Cualquier DDL, migración, `schema.sql`/`data.sql`, Flyway o Liquibase (G5).
- Modificación de las entidades `Acta`/`ActaProducto` o de los repositorios.
- Lógica de negocio en la vista o en el JS.
- Frameworks JS, bundlers, CDNs.
- Modificación de estilos globales/layout (`stitch-tokens.css`, `actas.css`, `base-stitch`).
- Autenticación/autorización (módulo `03-login`).
- Modal "Detalle de Guía" (spec `04-modal-detalle-guia`).

---

## 8. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| La validación de CUIT sólo en servicio puede dar UX tardía | Bajo | Mensajes verbatim claros (R-ACTA-028). |
| La búsqueda de CUIT placeholder confunde al usuario | Bajo | `window.alert` informativo (D-ACTA-05). |
| La renumeración de filas puede desincronizar labels | Medio | R-ACTA-055 + test de binding indexado. |
| `ActaService` supera 500 líneas (H-2) | Bajo | Preexistente, no bloquea. |
| La carga de CSS puede romper el test de tokens | Bajo | Test propio en `ActasRecursosTest`. |

---

## 9. Matriz de paridad (R-202)

| Legacy / BD / Stitch | Nuevo (EARS) | Prueba |
|---|---|---|
| WebForm `Form2276` (alta) | R-ACTA-014…R-ACTA-020 | `ActasMvcFormularioTest` |
| `F2276` (54 columnas) | R-ACTA-014…R-ACTA-034 | `ActaRepositoryIT`, `ActaAltaTest` |
| `F22761` (15 columnas) | R-ACTA-020…R-ACTA-047 | `ActaRepositoryIT` |
| `PRODUCTO` / `PRODUCT1` | R-ACTA-022, R-ACTA-037 | `ActaServiceTest` |
| Stitch "Carga Manual de Actas" | R-ACTA-015…R-ACTA-020, R-ACTA-058, R-ACTA-063 | Inspección visual |
| Design System "Gestión Caminera Chaco" | R-ACTA-053, R-ACTA-064 | `ActasRecursosTest` |
| `ActasController` (rutas) | R-ACTA-010, R-ACTA-040…R-ACTA-044 | `ActasMvcFormularioTest` |
| `ActaForm` (binding) | R-ACTA-014…R-ACTA-025, V-9…V-12 | `ActasMvcFormularioTest` |
| `actas-forms.js` (JS puntual) | R-ACTA-021, R-ACTA-061 | Revisión del JS |
| AGENTS.md regla 8 | R-ACTA-061 | Revisión |
| G5 (sin DDL) | R-ACTA-065 | Revisión del diff |