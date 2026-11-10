# Especificación — Módulo 10: Popup de Selección Manual de Localidades

> **Módulo**: `10-popup-localidades`
> **Estado**: IMPLEMENTADO (2026-11-04) — Gate 2 PASS (unit 177); IT bloqueado por DNS.
> **Norma**: ISO/IEC 25030 · Sintaxis EARS
> **Origen**: WebPanel `ListadoLocalidades` (KB `ATPPuestos`, `Root Module/Transacciones/ListadoLocalidades`)
>           + integración con `PPActaDetalle` (`Root Module/F2776/PPActaDetalle`)
> **Dependencias**: `01-nucleo-datos` (entidades `Localidad`/`Provincia`, repositorios),
>                   `09-formularios-acta` (formulario de alta de Acta)

---

## 1. Propósito y alcance

Agregar un popup de selección manual de localidad (equivalente al `ListadoLocalidades` del legacy)
que se utilice **sólo cuando la derivación automática por CUIT falle** (CUIT inexistente en
`CONTRIBU`). El formulario de alta de Acta (módulo 09) deriva automáticamente la localidad del
CUIT (D-ACTA-17); si el CUIT no existe en `CONTRIBU`, la localidad queda vacía y el usuario no
puede corregirla. El legacy permite seleccionar la localidad manualmente vía `prompt(ListadoLocalidades, ...)`.

Este módulo **no modifica** el esquema de BD ni las entidades ya mapeadas; reutiliza
`Localidad`, `Provincia` y `LocalidadRepository` existentes.

## 2. Objetos legacy de referencia (solo lectura)

| Objeto legacy | Tipo | Path en KB | Uso |
|---|---|---|---|
| `ListadoLocalidades` | WebPanel | `Root Module/Transacciones/ListadoLocalidades` | Popup de selección (fuente de verdad) |
| `PPActaDetalle` | WebPanel | `Root Module/F2776/PPActaDetalle` | Invoca el popup 5 veces (`prompt(...)`) |
| `F2276_BusLocYProv` | Procedure | (ver 01-nucleo-datos) | `parm(in:&LocCod, out:&LocNom, out:&ProvNombre)` |
| `BuscarInfoPorCuitLite` | Procedure | (ver 09) | Deriva RS/Domicilio/LocCod/ProvNombre del CUIT |
| Tabla `LOCALIDA` | Tabla BD | `Rentas.LOCALIDA` | Catálogo de localidades |
| Tabla `PROVINCI` | Tabla BD | `Rentas.PROVINCI` | Catálogo de provincias |

## 3. Evidencia extraída del legacy

### 3.1 WebPanel `ListadoLocalidades`

- **Regla (Rules)**:
  ```
  parm(out:&pProvNombre, out:&pLocNom, out:&pLocCod);
  order(LocCod);
  ```
- **Eventos**:
  - `Start`: `Form.Caption = Format("GXSPC_SelectionList", "Localidades")`.
  - `Load`: `&LinkSelection.FromImage(selectRow)`.
  - `Enter`: asigna `&pLocCod = LocCod`, `&pLocNom = LocNom`, `&pProvNombre = ProvNombre` y `Return` (cierra el popup devolviendo los 3 valores).
- **Conditions (filtros)**:
  - `LocCod >= &cLocCod`
  - `LocNom LIKE &cLocNom`
  - `LocNomProv LIKE &cLocNomProv`
  - `LocCodPost >= &cLocCodPost`
- **Variables**:
  - Filtros: `&cLocCod` (NUMERIC 5), `&cLocNom` (CHAR 20), `&cLocNomProv` (CHAR 15), `&cLocCodPost` (NUMERIC 4).
  - Salida: `&pLocCod` (NUMERIC 5), `&pLocNom` (CHAR 20), `&pProvNombre` (VARCHAR 40).
  - `&LinkSelection` (BITMAP).
- **WebForm**:
  - Fieldset `Filters` con 4 campos de filtro (LocCod, LocNom, LocNomProv, LocCodPost).
  - Fieldset `Lista de Selección` con `Grid1` (MaxRows=5) y 6 columnas:
    1. Selector (`&LinkSelection`, ReturnOnClick=True).
    2. `Cod. Loc.` (`LocCod`).
    3. Nombre (`LocNom`, ReturnOnClick=True, título contextual).
    4. Nombre de provincia (`LocNomProv`, **ColVisible=False** en el legacy).
    5. `Provincia` (`ProvNombre`).
    6. Código postal (`LocCodPost`).
  - Botón `Cancel` (evento `Cancel`).

### 3.2 Invocación desde `PPActaDetalle`

```
prompt(ListadoLocalidades, &ProvVenNom,  &LocNomVen,  &F2276.F276VenCLo);
prompt(ListadoLocalidades, &ProvDesNom,  &LocNomDes,  &F2276.F276DestCL);
prompt(ListadoLocalidades, &ProvTranNom, &LocNomTra,  &F2276.F276TranCL);
prompt(ListadoLocalidades, &ProvOrigenNom, &LocNomOrigen, &F2276.F276OrCod);
prompt(ListadoLocalidades, &ProvDestinoNom, &LocNomDestino, &F2276.F276DeCod);
```

Los parámetros se asignan por posición: `&pProvNombre`, `&pLocNom`, `&pLocCod`. Es decir, el
popup devuelve el **nombre de provincia**, el **nombre de localidad** y el **código de localidad**.

Además, `PPActaDetalle` define los eventos `ctlF276VenCLo.IsValid`, `ctlF276DestCL.IsValid` y
`ctlF276TranCL.IsValid`, que llaman a `F2276_BusLocYProv` para derivar `LocNom` y `ProvNombre`
a partir del `LocCod` (paridad con la edición manual del código).

### 3.3 Estructura física (INFORMATION_SCHEMA)

**LOCALIDA** (PK `LocCod`):

| Columna | Tipo SQL | Nullable | Mapeo JPA |
|---|---|---|---|
| `LocCod` | int | NO | `@Id @Column(name="LocCod")` Integer |
| `LocNom` | char(50) | YES | `@ColumnTransformer(read="RTRIM(LocNom)")` |
| `LocNomProv` | char(19) | YES | `@ColumnTransformer(read="RTRIM(LocNomProv)")` |
| `LocCodPost` | smallint | YES | `@Column(name="LocCodPost")` Integer |
| `ProvCod` | smallint | YES | `@JoinColumn(name="ProvCod")` (FK lógica) |
| `LocCodAfip` | int | NO | `@Column(name="LocCodAfip")` Integer (huérfana, default 0) |

**PROVINCI** (PK `ProvCod`):

| Columna | Tipo SQL | Nullable | Mapeo JPA |
|---|---|---|---|
| `ProvCod` | smallint | NO | `@Id @Column(name="ProvCod")` Integer |
| `ProvNombre` | varchar(40) | NO | `@Column(name="ProvNombre", length=40)` |
| `ProvCodAfip` | int | NO | `@Column(name="ProvCodAfip")` (huérfana, default 0) |

Sin cambios en BD (G5).

### 3.4 Entidades existentes (sin modificación)

- `nucleo.domain.Localidad` → `@Table("Localida")`.
- `nucleo.domain.Provincia` → `@Table("PROVINCI")`.
- `nucleo.repository.LocalidadRepository` → `findAllConProvincia()`, `findByProvCodConProvincia(provCod)`,
  `findByIdConProvincia(locCod)`.

## 4. Requisitos (EARS)

### 4.1 Habilitación del popup en el formulario de alta

#### R-LOC-001 — Habilitar popup cuando el CUIT no exista
> **CUANDO** el CUIT del Vendedor, del Destinatario o del Transportista no exista en `CONTRIBU`,
> **EL SISTEMA DEBE** habilitar el popup de selección manual de localidad (ícono ⇧) junto al campo
> `Localidad` del fieldset correspondiente, y **NO DEBE** derivar la localidad automáticamente.

* *Criterio de verificación*: test `@WebMvcTest` que simula `BuscarInfoPorCuitLite` devolviendo vacío
  y verifica que el ícono ⇧ queda habilitado (atributo `data-popup-enabled="true"` o clase CSS
  `is-enabled`) y que los campos `LocNom`/`ProvNombre` permanecen vacíos.

#### R-LOC-002 — Derivar automáticamente cuando el CUIT exista
> **CUANDO** el CUIT exista en `CONTRIBU`, **EL SISTEMA DEBE** derivar automáticamente la Razón
> Social, el Domicilio, el código de Localidad y el nombre de la Provincia, y **DEBE** deshabilitar
> el popup (ícono ⇧ no clickable / oculto).

* *Criterio de verificación*: test `@WebMvcTest` con un contribuyente conocido que verifica que los
  campos derivados se llenan y que el ícono ⇧ queda deshabilitado (`disabled` o `aria-disabled="true"`).

### 4.2 Comportamiento del popup

#### R-LOC-003 — Grilla de localidades
> **EL SISTEMA DEBE** renderizar el popup en la ruta `/localidades` con una grilla de columnas
> `Cod. Loc.` (LocCod), Nombre (LocNom, clickable), `Provincia` (ProvNombre) y `Cod. Postal`
> (LocCodPost), reproduciendo las 6 columnas del legacy `Grid1` (selector, código, nombre, provincia
> oculta `LocNomProv`, provincia visible `ProvNombre`, código postal), con paginación de
> **5 filas por página** (paridad con `Grid1.MaxRows = 5`).

* *Criterio de verificación*: test MockMvc `GET /localidades` que verifica los encabezados visibles y
  la paginación por defecto de 5; IT que compara el conteo con `SELECT COUNT(*) FROM LOCALIDA`.

#### R-LOC-004 — Provincia como label (sólo lectura)
> **EL SISTEMA DEBE** mostrar la provincia en el formulario de alta como **label de sólo lectura**
> (paridad con R-ACTA-017/R-ACTA-018) y en el popup como columna de sólo lectura (`ProvNombre`),
> sin permitir su edición en ninguno de los dos contextos.

* *Criterio de verificación*: test de vista que verifica la ausencia de `<input>` editable para la
  provincia en ambos contextos y la presencia del texto renderizado.

#### R-LOC-005 — JS puntual (ventana nueva)
> **EL SISTEMA DEBE** abrir el popup mediante JavaScript vanilla (`window.open('/localidades', ...)`)
> en una **ventana nueva**, **NO DEBE** usar iframe, modal inline, ni ningún framework JS, y
> **NO DEBE** referenciar recursos por CDN.

* *Criterio de verificación*: `grep` sobre `static/js/**` y `templates/puestos/**` sin apariciones de
  `iframe`, `bootstrap`, `tailwind`, `cdn.`, `https://`, `unpkg`, `jsdelivr`, `node_modules`; revisión
  del JS que verifica el uso de `window.open`.

#### R-LOC-006 — Actualización de campos al seleccionar
> **CUANDO** el usuario seleccione una fila de la grilla del popup, **EL SISTEMA DEBE** asignar al
> formulario padre el código de localidad, el nombre de localidad y el nombre de provincia
> seleccionados, y **DEBE** cerrar la ventana popup.

* *Criterio de verificación*: prueba de navegador que selecciona una fila y verifica que los 3 campos
  del formulario padre se actualizan y que la ventana popup se cierra; test unitario del controlador
  del popup que verifica el `Return` con los 3 valores.

#### R-LOC-007 — Paridad con 09-formularios-acta
> **EL SISTEMA DEBE** mantener el formulario de alta de Acta (ruta `/actas/nueva`, plantilla
> `puestos/acta-formulario`) sin cambios estructurales; **sólo** se agrega el ícono ⇧ junto al
> campo `Localidad` de cada fieldset de parte (Vendedor, Destinatario, Transportista).

* *Criterio de verificación*: test MockMvc `GET /actas/nueva` que verifica que la plantilla sigue
  siendo `puestos/acta-formulario` y que los fieldsets `Vendedor`, `Destinatario` y `Transportista`
  siguen presentes con sus 5 campos.

### 4.3 Filtros y paginación del popup

#### R-LOC-008 — Filtros del legacy
> **EL SISTEMA DEBE** ofrecer en el popup los filtros del legacy `ListadoLocalidades`:
> código de localidad (>=, `LocCod`), nombre (LIKE, `LocNom`), provincia (LIKE por nombre,
> `LocNomProv`/`ProvNombre`) y código postal (>=, `LocCodPost`), **además** del filtro base (todas
> las localidades).

* *Criterio de verificación*: test MockMvc parametrizado (un caso por filtro) que verifica que el
  modelo devuelve menos filas con filtro que sin él; IT que compara el conteo filtrado con un
  `SELECT` equivalente.

#### R-LOC-009 — Paginación
> **CUANDO** el usuario navegue por la grilla del popup, **EL SISTEMA DEBE** paginar con tamaño de
> página por defecto **5** (paridad con `Grid1.MaxRows = 5`) y **DEBE** volcar a la vista el total
> de páginas, el número de página actual y el total de elementos.

* *Criterio de verificación*: test MockMvc `GET /localidades?page=0&size=5` que verifica los atributos
  `totalPages`, `number` y `totalElements`.

### 4.4 Seguridad y sesión

#### R-LOC-010 — Sesión autenticada
> **EL SISTEMA DEBE** exigir sesión autenticada para acceder a `/localidades` y **NO DEBE** devolver
> el popup a un usuario sin sesión.

* *Criterio de verificación*: test MockMvc sin sesión que comprueba `302` al login.

#### R-LOC-011 — CSRF en selección
> **EL SISTEMA DEBE** proteger con token CSRF válido cualquier `POST` que el popup realice (si
> aplica) y **NO DEBE** aceptar selecciones sin CSRF válido.

* *Criterio de verificación*: test MockMvc con CSRF inválido que comprueba `403`.

### 4.5 Robustez

#### R-LOC-012 — Cierre sin selección
> **SI** el usuario cierra el popup sin seleccionar una localidad, **ENTONCES EL SISTEMA DEBE**
> dejar los campos del formulario padre (código, nombre, provincia) sin cambios.

* *Criterio de verificación*: prueba de navegador que cierra el popup y verifica que los campos del
  formulario padre no cambiaron.

#### R-LOC-013 — FK huérfana en el popup
> **EL SISTEMA DEBE** renderizar correctamente las 30 localidades con `ProvCod = 0` (FK lógica
> huérfana) en el popup, mostrando la provincia como vacía/null sin lanzar error, replicando el
> comportamiento del `left join fetch` de `LocalidadRepository`.

* *Criterio de verificación*: IT que solicita `/localidades` y verifica que las 30 filas con
  `ProvCod = 0` se renderizan con provincia vacía y sin `500`.

### 4.6 No regresión y paridad

#### R-LOC-014 — No regresión
> **CUANDO** se implemente el módulo, **EL SISTEMA DEBE** mantener en verde las suites existentes de
> `01-nucleo-datos`, `02-abm-productos`, `03-login`, `06-turnos`, `08-control-cargas` y
> `09-formularios-acta`, y **NO DEBE** modificar `Localidad`, `Provincia` ni `LocalidadRepository`.

* *Criterio de verificación*: `./mvnw clean test` en verde; `git diff` sin cambios en las entidades
  ni el repositorio del núcleo.

#### R-LOC-015 — Paridad de mapeo
> **EL SISTEMA DEBE** reutilizar el mapeo JPA existente de `LOCALIDA` y `PROVINCI` (sin DDL, sin
> migraciones, `ddl-auto=none`), y **NO DEBE** generar cambios en el esquema.

* *Criterio de verificación*: revisión del diff que confirma ausencia de `CREATE TABLE`, `ALTER TABLE`,
  `schema.sql`, `data.sql`, Flyway, Liquibase; `spring.jpa.hibernate.ddl-auto` sigue en `none`.

---

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB) | Requisito nuevo | Prueba prevista |
|---|---|---|
| `ListadoLocalidades.Rules` (`parm(out:&pProvNombre,out:&pLocNom,out:&pLocCod); order(LocCod)`) | R-LOC-003, R-LOC-006 | `MvcPopupLocalidades` |
| `ListadoLocalidades.Events.Start` (`Form.Caption = Format(...)`) | R-LOC-003 | `MvcPopupTitulo` |
| `ListadoLocalidades.Events.Enter` (asignación de 3 valores + `Return`) | R-LOC-006 | `MvcPopupSeleccion` |
| `ListadoLocalidades.Conditions` (4 filtros) | R-LOC-008 | `MvcPopupFiltros` |
| `ListadoLocalidades.WebForm` (`Grid1.MaxRows=5`, 6 columnas) | R-LOC-003, R-LOC-009 | `MvcPopupGrilla` |
| `PPActaDetalle.Rules` (`prompt(ListadoLocalidades, ...) ×5`) | R-LOC-001, R-LOC-002, R-LOC-006, R-LOC-007 | `MvcFormularioAltaPopup` |
| `PPActaDetalle.Events` (`ctlF276VenCLo.IsValid` → `F2276_BusLocYProv`) | R-LOC-002 | `MvcDerivacionCuit` |
| `LocalidadRepository` (`left join fetch`, FK huérfana) | R-LOC-013 | `PopupLocalidadesIT` |
| `openspec/constitution/stack.md` §2 (JS puntual) | R-LOC-005 | `JsPopupVanilla` |

### 5.2 Matriz de paridad funcional

| Operación legacy | Requisito | Prueba prevista |
|---|---|---|
| Abrir popup de localidades | R-LOC-001, R-LOC-005 | `MvcPopupAbrir` |
| Derivar localidad por CUIT | R-LOC-002 | `MvcDerivacionCuit` |
| Seleccionar localidad en popup | R-LOC-006 | `MvcPopupSeleccion` |
| Filtrar localidades | R-LOC-008 | `MvcPopupFiltros` |
| Paginar localidades | R-LOC-009 | `MvcPopupPaginacion` |
| Cancelar popup | R-LOC-012 | `MvcPopupCancelar` |
| Renderizar FK huérfana | R-LOC-013 | `PopupLocalidadesIT` |

---

## 6. Supuestos abiertos

| ID | Pregunta | Decisión provisional | Riesgo | Acción para resolver |
|---|---|---|---|---|
| **SA-LOC-01** | ¿El popup debe filtrar por provincia seleccionada con un combo, o mantener el filtro por texto libre del legacy (`LocNomProv LIKE`)? | Texto libre (paridad estricta con legacy). | Bajo | Confirmar con negocio; si se quiere combo, agregar `ProvinciaRepository.findAll()` y un `<select>`. |
| **SA-LOC-02** | ¿La ventana popup debe ser modal (bloqueante) como el `prompt()` del legacy, o no modal? | No modal (`window.open`), porque `showModalDialog` está deprecado y el stack prohíbe frameworks. | Medio | Confirmar con UX; si se requiere bloqueo, implementar overlay modal con JS vanilla. |
| **SA-LOC-03** | ¿Qué pasa si el usuario cierra el popup sin seleccionar? | Los campos del formulario padre quedan sin cambios. | Bajo | Confirmar con negocio. |
| **SA-LOC-04** | ¿El ícono ⇧ debe ser un `<button type="button">` o un `<a>`? | `<button type="button">` con JS puntual. | Bajo | Confirmar con diseño. |
| **SA-LOC-05** | ¿La página `/localidades` debe requerir sesión autenticada? | Sí (paridad con seguridad). | Bajo | Confirmar con el responsable de seguridad. |
| **SA-LOC-06** | ¿El popup debe paginar o mostrar todas las localidades? | Paginar con size=5 (paridad con `Grid1.MaxRows=5`). | Bajo | Confirmar con UX. |
| **SA-LOC-07** | ¿El popup debe mostrar la columna `LocNomProv` (oculta en el legacy)? | Oculta (paridad con `ColVisible=False`); se muestra `ProvNombre` en su lugar. | Bajo | Confirmar con diseño. |
| **SA-LOC-08** | ¿La habilitación del popup se basa en un flag explícito o en la ausencia de localidad derivada? | En la ausencia de localidad derivada (LocCod vacío) tras `BuscarInfoPorCuitLite`. | Medio | Confirmar con el implementador de 09; si 09 introduce un flag `localidadDerivada`, usarlo. |

---

## 7. Decisiones

| # | Decisión | Alternativas descartadas | Justificación |
|---|---|---|---|
| **D-LOC-01** | El popup es una **página Thymeleaf** `/localidades` (server-side), abierta en **ventana nueva** vía JS vanilla (`window.open`). | (a) Modal inline; (b) iframe; (c) endpoint REST + render JS. | (a)/(b) contradicen el stack (Thymeleaf + JS puntual); (c) duplicaría la lógica de filtrado en el cliente. La página server-side mantiene la lógica en el servidor (G7). |
| **D-LOC-02** | Se reutilizan `Localidad`, `Provincia` y `LocalidadRepository` sin modificación. | Crear entidades nuevas en `puestos.domain`. | Ya mapeadas y probadas en `01-nucleo-datos`; duplicarlas violaría DRY. |
| **D-LOC-03** | La selección asigna los 3 valores vía `window.opener` y cierra la ventana popup. | (a) POST al padre; (b) cookies/session. | (a) requiere recarga; (b) es frágil. `window.opener` es el patrón estándar para popups. |
| **D-LOC-04** | El ícono ⇧ se habilita/deshabilita según exista localidad derivada (LocCod vacío ⇒ habilitado). | Flag explícito en el DTO. | Evita cambios en el DTO de 09; se deriva del estado existente. |
| **D-LOC-05** | El módulo **no expone endpoints REST**; sólo MVC (`/localidades`, `/localidades?...`). | Crear `/api/v1/localidades`. | Paridad con D-ACTA-04: no hay consumidor API. Evita el compromiso de G-POSTMAN (la colección no cambia). |
| **D-LOC-06** | La provincia en el formulario de alta sigue siendo label (paridad con 09); en el popup es columna de sólo lectura. | Combo de provincia en el popup. | Paridad con legacy (no hay combo en `ListadoLocalidades`). |
| **D-LOC-07** | La paginación por defecto es **5** (paridad con `Grid1.MaxRows = 5`). | 10 (paridad con listado de actas). | `ListadoLocalidades` usa 5; el listado de actas usa 10. Son contextos distintos. |
| **D-LOC-08** | El módulo **no modifica** `Localidad`, `Provincia` ni `LocalidadRepository`. | Extender el repositorio con métodos nuevos. | Ya existen los métodos necesarios (`findAllConProvincia`, `findByProvCodConProvincia`). |