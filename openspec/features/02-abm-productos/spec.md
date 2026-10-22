# Spec — 02 ABM de Productos (Thymeleaf)

> **Estado**: `APROBADA` — puerta **G-SPEC** superada (2026-10-22, coordinación). Verificado: 30 requisitos
> EARS (`R-PROD-01…30`, sin huecos) con criterio de verificación; paridad 1:1 con `PRODUCTO` (8 columnas)
> y `PRODUCT1` (9 columnas) contra la KB y contra `Rentas` (G-DATA 2026-10-22); sin DDL, sin
> migraciones, sin rediseño de esquema; 38 tareas atómicas y verificables en `tasks.md`.
> Trazabilidad: `plan.md` §9 (matriz de paridad) y `tasks.md` (T-301…T-310).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-PROD-<NN>`. Se **reutilizan** los ids `R-PROD-01…18` de la mini-spec aprobada
> `openspec/features/01-nucleo-datos/ui-abm-productos.md` (T-505b) cuando la regla sigue vigente; su
> **arquitectura Angular queda obsoleta** (shell, `mat-table`, `<ng-content>`) y se reformula en
> `plan.md` para Spring MVC + Thymeleaf.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13.186702), alias `atppuestos` — **solo lectura**.
> - `Transaction Productos` (`Root Module/Transacciones/Productos`), `Structure` + `WebForm`.
> - `Table Producto` (nivel 0) y `Table Product1` (nivel 1).
> - Atributos globales `UMedNroId`, `UMedNombre`; índice `IProductos`.
> - **`Productos` no declara `Rules` ni `Events`** (verificado: ambos vacíos) ⇒ todo el comportamiento
>   observable proviene del `GxMultiForm` por defecto (Enter/Cancel/Delete + First/Prev/Next/Last/Select).
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` (G-DATA 2026-10-22, sólo `SELECT`).
> **Backend ya implementado**: `atp-nucleo-compartido` expone `GET/POST/PUT/DELETE /api/v1/productos`
>   y `GET /api/v1/productos/{prodCod}/importes-vigentes`. Este módulo **consume** ese contrato; no lo
>   rediseña ni lo redefine (`openspec/features/01-nucleo-datos/spec.md` R-001…R-037, `plan.md` §3).
> **Alcance de escritura**: `openspec/features/**` (esta spec) y, en la fase de implementación,
>   `atp-nucleo-compartido/**` (núcleo). **Sin DDL, sin migraciones** (G5).

---

## 1. Propósito

Entregar la **pantalla Thymeleaf** del ABM de la transacción `Productos` con su **nivel 1**
(`PRODUCT1`, vigencias), replicando el comportamiento observable del `GxMultiForm` de dos niveles
del legacy:

- un **formulario raíz de 12 campos** con los rótulos **verbatim** del legacy (incluidas sus erratas);
- una **grilla anidada de vigencias** editable, que viaja **anidada** en el body de la escritura.

Es la ABM **más compleja** del núcleo: es la única con dos niveles y con **campos derivados que no
se persisten** (`ProdIMax1..4`) y **atributos sin columna física** (`UMedNombre`).

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Pantalla ABM de `PRODUCTO` en Thymeleaf: alta, modificación, baja y navegación registro-a-registro. |
| 2 | Formulario raíz de **12 campos** con rótulos verbatim (§3.1), 4 de ellos **derivados** y de sólo lectura. |
| 3 | **Grilla anidada de vigencias** (nivel 1) con 5 columnas, editable, con alta/baja de filas. |
| 4 | Envío **anidado** de las vigencias en el body de escritura (P-03 de `01-nucleo-datos`). |
| 5 | Tipo de campo **decimal** para los importes (`money`/`NUMERIC(12,2)`). |
| 6 | Filtro de **anulados** sobre el endpoint ya existente `GET /api/v1/productos?anulados=`. |
| 7 | Cimientos mínimos de vista compartidos (layout, tokens CSS, JS de confirmación) que **hoy no existen** en el repositorio (ver `plan.md` §1.3). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Lookup de **Unidad de Medida** (`UMedNroId` → `UMedNombre`) | **No existe** una transacción `UMed` en la KB (verificado: `genexus_query "UMed"` devuelve sólo los 2 atributos) ni una columna `PRODUCTO.UMedNombre` (verificado en G-DATA). No hay fuente de datos posible. | Fuera del módulo — ver **SA-P01** |
| Persistir `UMedNombre` | Exige **DDL** (prohibido, G5). | Fuera del alcance |
| Rediseño del contrato REST `/api/v1/productos` | Ya está implementado, verificado y en Postman. | `01-nucleo-datos` |
| ABM de las otras 7 maestras | Fuera de este módulo. | Backlog (`01-nucleo-datos`, fase 2) |
| Endpoint nuevo de unidades de medida | Require DDL + endpoint. | Fuera del alcance |
| Autenticación / autorización de la pantalla | La define el módulo `03-login` (Spring Security + CSRF). | `03-login` |
| E2E automatizado | — | Fuera del módulo |

---

## 3. Modelo legacy verificado

### 3.1 Nivel 0 — `Transaction Productos` / `Table Producto` (rótulos **verbatim**)

Extracción literal de `genexus_read({name:"Productos", type:"Transaction", part:"Structure"})`
(idéntica en `Table Producto`):

```text
ProdCod* : NUMERIC(3) // "Código"
ProdDesc : CHARACTER(25) // "Producto"
ProdTipCal : NUMERIC(1) // "Tipo de cálculo"
TPPriID : NUMERIC(1) // "Tipo de Producción"
ProdIMax1 : NUMERIC(12,2) // "Importe de Entrada", [Formula: max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu1);]
ProdIMax2 : NUMERIC(12,2) // "Importe C. Mec. Entrada", [Formula: max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu2);]
ProdIMax3 : NUMERIC(12,2) // "Importe Salida", [Formula: max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu3);]
ProdIMax4 : NUMERIC(12,2) // "Importre C. Mec. Salida", [Formula: max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu4);]
UMedNroId : CHARACTER(3) // "Código"
UMedNombre : VARCHAR(40) // "Unidad de Medida"
ProdAcop : CHARACTER(1) // "Acopiador?", [Nullable]
ProdAnula : NUMERIC(1) // "Prod Anulado"
```

Orden de presentación en el `WebForm` (`xmlKind=GxMultiForm`, `layout 17.11.0`): los 12 `<data>` van
en filas consecutivas dentro de `FormContainer`, en el orden de la tabla; `Toolbar` (NavButtons) va
**antes** del primer campo y `FormButtons` (Enter/Cancel/Delete) **después** del último.

| # | Atributo | Rótulo legacy (verbatim) | Tipo lógico | Columna física | Nula | Editable |
|---|---|---|---|---|---|---|
| 1 | `ProdCod*` | `Código` | `NUMERIC(3)` | `PRODUCTO.ProdCod` `smallint` (PK) | NO | alta sí / edición no |
| 2 | `ProdDesc` | `Producto` | `CHARACTER(25)` | `PRODUCTO.ProdDesc` `char(25)` | NO | sí |
| 3 | `ProdTipCal` | `Tipo de cálculo` | `NUMERIC(1)` | `PRODUCTO.ProdTipCal` `smallint` | NO | sí |
| 4 | `TPPriID` | `Tipo de Producción` | `NUMERIC(1)` | `PRODUCTO.TPPriID` `smallint` | NO | sí |
| 5 | `ProdIMax1` | `Importe de Entrada` | `NUMERIC(12,2)` | — (fórmula, **no persistido**) | — | **no** |
| 6 | `ProdIMax2` | `Importe C. Mec. Entrada` | `NUMERIC(12,2)` | — (fórmula, **no persistido**) | — | **no** |
| 7 | `ProdIMax3` | `Importe Salida` | `NUMERIC(12,2)` | — (fórmula, **no persistido**) | — | **no** |
| 8 | `ProdIMax4` | `Importre C. Mec. Salida` | `NUMERIC(12,2)` | — (fórmula, **no persistido**) | — | **no** |
| 9 | `UMedNroId` | `Código` | `CHARACTER(3)` | `PRODUCTO.UMedNroId` `char(3)` | sí | sí |
| 10 | `UMedNombre` | `Unidad de Medida` | `VARCHAR(40)` | — (**no persistido**, ver R-PROD-19) | — | **no** |
| 11 | `ProdAcop` | `Acopiador?` | `CHARACTER(1)` | `PRODUCTO.ProdAcop` `char(1)` | sí | sí |
| 12 | `ProdAnula` | `Prod Anulado` | `NUMERIC(1)` | `PRODUCTO.ProdAnula` `smallint` | NO | sí |

> - El **typo legacy** `Importre C. Mec. Salida` (por `Importe`) se conserva **verbatim**: la paridad
>   de identidad manda (`docs/design.md` §1.1 y `conventions.md` §6).
> - `Código` aparece **dos veces** (campos 1 y 9). Es lo que hace el legacy: se conserva. La
>   distinción es semántica (producto vs. unidad de medida), no visual.
> - `TPPriID` se rotula `Tipo de Producción` con **P mayúscula** (verbatim de la KB).
> - `UMedNombre` **no tiene columna** en `PRODUCTO` (G-DATA 2026-10-22) ⇒ el backend lo expone
>   siempre como ausente y lo descarta en la escritura. La mini-spec T-505b declaraba aquí una
>   columna `varchar(40)`: **es incorrecto** y se corrige (ver R-PROD-19 y SA-P01).

### 3.2 Nivel 1 — `Table Product1` (rótulos **verbatim**)

```text
ProdCod* : NUMERIC(3) // "Código"
ProdFchVig* : DATE(8)
ProdImpu1 : NUMERIC(12,2) // "Importe Fijo Entrada"
ProdImpu2 : NUMERIC(12,2) // "Imp.Cosech.Mec Entrada"
ProdImpu3 : NUMERIC(12,2) // "Importe Fijo Salida"
ProdImpu4 : NUMERIC(12,2) // "Imp.Cosech.Mec Salida"
```

| # | Atributo | Rótulo legacy (verbatim) | Tipo lógico | Columna física | Nula |
|---|---|---|---|---|---|
| 1 | `ProdFchVig*` | `ProdFchVig` *(derivado del nombre del atributo — D-20)* | `DATE(8)` | `PRODUCT1.ProdFchVig` `datetime` | NO (PK del nivel) |
| 2 | `ProdImpu1` | `Importe Fijo Entrada` | `NUMERIC(12,2)` | `PRODUCT1.ProdImpu1` `money` | NO |
| 3 | `ProdImpu2` | `Imp.Cosech.Mec Entrada` | `NUMERIC(12,2)` | `PRODUCT1.ProdImpu2` `money` | NO |
| 4 | `ProdImpu3` | `Importe Fijo Salida` | `NUMERIC(12,2)` | `PRODUCT1.ProdImpu3` `money` | sí |
| 5 | `ProdImpu4` | `Imp.Cosech.Mec Salida` | `NUMERIC(12,2)` | `PRODUCT1.ProdImpu4` `money` | sí |

> `ProdFchVig` **no tiene descripción declarada** en la KB ⇒ su rótulo en el legacy es el
> `ContextualTitle` del atributo. Mismo patrón que SA-04 en `CAMINOS`: rótulo derivado del nombre
> del atributo (**`ProdFchVig`**), marcado en la UI como derivación (D-20).
>
> El título de la sección del nivel 1 en el legacy es el `textblock` `TitleLevel1` con
> `caption="Level1"` ⇒ rótulo **verbatim `Level1`** (ver SA-P02).

### 3.3 Fórmulas de los derivados

Semántica GeneXus `max(atributo, condición, default, atributo)` = valor del 4.º argumento en la fila
cuyo 1.º argumento es máximo bajo la condición; `default` si no hay filas:

```text
ProdIMax1 = max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu1)
ProdIMax2 = max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu2)
ProdIMax3 = max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu3)
ProdIMax4 = max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu4)
```

Se calculan **al vuelo** contra la fecha del servidor y **no se persisten** (R-035 de
`01-nucleo-datos`). El backend los expone en `GET /api/v1/productos/{prodCod}/importes-vigentes`
→ `ImportesVigentesDto(prodCod, prodIMax1..4)`.

### 3.4 SA-09 (formato `DATE`/`DATETIME`) — cerrado

`ProdFchVig` es `DATE(8)` lógico sobre una columna `datetime` ⇒ **se envía a medianoche** y se
muestra en `dd/MM/yyyy`. Confirmado en los datos reales: las 1553 filas de `PRODUCT1` tienen
`ProdFchVig` con hora `00:00:00.000`. Misma resolución que `FerFecha` en el módulo de feriados.

### 3.5 Datos reales de `Rentas` (G-DATA 2026-10-22) — hechos que condicionan la UI

| Hecho | Valor | Consecuencia en la UI |
|---|---|---|
| Filas `PRODUCTO` | 244 (`ProdCod` 1…483) | Colección de tamaño medio. |
| `ProdAnula` | 183 con `0`, 61 con `1` | El filtro de anulados es útil (R-PROD-26). |
| Filas `PRODUCT1` | 1553, sobre 228 productos | Grilla con hasta **30** filas por producto. |
| Productos **sin** vigencias | 16 | `importes-vigentes` ⇒ los 4 derivados en `0`. |
| `ProdImpu3`/`ProdImpu4` en `NULL` | **293 de 1553 filas (18,9 %)** | La ausencia de la clave en el JSON **debe** normalizarse a `0` antes de enviar (R-PROD-20). |
| `PRODUCTO.ProdReppa` (columna legacy) | `'N'` en las filas muestreadas | Mapeada pero **no expuesta** ni editable. |
| `PRODUCT1.ProdImpu5/ProdFeLog/ProdUsu` (legacy) | `NULL` en las filas muestreadas | Mapeadas, **no expuestas**. |
| FK física `PRODUCT1 → PRODUCTO` | **no existe** | La baja del producto borra las vigencias por cascada JPA, no por FK. |
| `IDENTITY` / `COLUMN_DEFAULT` | ninguno | La PK viaja en el body del `POST` (R-PROD-09). |

---

## 4. Requisitos EARS

### 4.1 Pantalla, navegación y estados

#### R-PROD-01 — Pantalla ABM de Productos
> **EL SISTEMA DEBE** exponer en la ruta de página `/productos` un ABM de `PRODUCTO` con alta,
> modificación, baja y navegación registro-a-registro, sustentado en el servicio existente
> `ProductoService` (nunca en llamadas HTTP a la propia API).

* *Criterio de verificación*: `@WebMvcTest` del controller MVC que comprueba `GET /productos` → 200 +
  vista `nucleo/productos` + atributos del view model; `POST /productos` (alta) → persiste en
  `PRODUCTO`/`PRODUCT1`; `POST /productos/{prodCod}` (modificación) → actualiza; `POST
  /productos/{prodCod}/eliminar` (baja) → borra raíz + hijas. IT contra `Rentas` con rollback.

#### R-PROD-13 — Navegación de registros
> **CUANDO** el usuario active First, Prev, Next o Last, **EL SISTEMA DEBE** mostrar en el formulario
> el registro correspondiente de la colección **ordenada por `prodCod` ascendente**, y **DEBE**
> deshabilitar el control correspondiente en los extremos.

* *Criterio de verificación*: test que recorre la colección y verifica el registro mostrado en cada
  paso y el estado deshabilitado en el primer y el último elemento.

#### R-PROD-25 — Orden determinista de la colección
> **EL SISTEMA DEBE** presentar la colección de productos **ordenada por `prodCod` ascendente**, con
> independencia del orden en que la base devuelva las filas.

* *Criterio de verificación*: test con una colección de `prodCod` desordenada en el origen ⇒ la vista
  los renderiza ascendentemente. Base: `GET /api/v1/productos` **no** declara `ORDER BY`
  (`ProductoRepository.findAll()`), por lo que el orden **no** puede delegarse al endpoint.

#### R-PROD-26 — Filtro de anulados
> **DONDE** el usuario elija el filtro de anulación, **EL SISTEMA DEBE** listar la colección según ese
> filtro (**Todos** / **No** / **Sí**) usando el parámetro `anulados` del endpoint existente, y **DEBE**
> mantener el filtro al navegar y al volver de una escritura.

* *Criterio de verificación*: test con los 3 valores del filtro que comprueba el parámetro enviado al
  servicio y la colección resultante; el filtro **no** agrega ni modifica endpoints.

#### R-PROD-12 — Estados de la vista
> **EL SISTEMA DEBE** distinguir los estados `loading`, `empty` (colección sin registros), `error`
> (fallo al cargar) y `sin selección` en la vista de colección, y **NO DEBE** renderizar la grilla de
> datos cuando la colección está vacía o la carga falló.

* *Criterio de verificación*: tests que simulan colección vacía, fallo de carga y carga diferida, y
  comprueban el marcador de estado renderizado y la ausencia de la grilla.

#### R-PROD-18 — Paridad funcional del CRUD
> **EL SISTEMA DEBE** ofrecer, en el ABM de `Productos`, las mismas operaciones que el `GxMultiForm` de
> 2 niveles del legacy: alta, modificación, baja, navegación registro-a-registro y gestión del nivel 1.

* *Criterio de verificación*: matriz de paridad (§5.2) con una prueba por operación.

### 4.2 Formulario raíz

#### R-PROD-02 — Formulario raíz de 12 campos verbatim
> **EL SISTEMA DEBE** presentar los 12 campos del legacy con sus rótulos **verbatim** (§3.1) y en ese
> orden, incluidos el typo `Importre C. Mec. Salida`, la **P mayúscula** de `Tipo de Producción` y los
> dos campos rotulados `Código`.

* *Criterio de verificación*: test que compara los 12 rótulos renderizados con la tabla de §3.1, en
> orden, carácter a carácter.

#### R-PROD-07 — Rango de la PK
> **SI** el usuario ingresa una `prodCod` fuera del rango `0…32767` de la columna `smallint`,
> **ENTONCES EL SISTEMA DEBE** marcar el campo como inválido e **NO DEBE** ejecutar la escritura.

* *Criterio de verificación*: test con `prodCod = 99999` y con `prodCod = -1` que comprueba el marcado
  de inválido y que la escritura no se ejecuta.

#### R-PROD-09 — PK en el body y de sólo lectura en edición
> **EL SISTEMA DEBE** incluir la `prodCod` en el cuerpo de la escritura en **alta y modificación**
> (no hay columnas `IDENTITY` en `Rentas`), y **MIENTRAS** el modo de edición esté activo,
> `prodCod` **DEBE** ser no editable.

* *Criterio de verificación*: test que comprueba el cuerpo enviado en alta y en modificación, y el
  atributo `readonly` del campo en modo edición.

#### R-PROD-10 — Validación en línea
> **SI** un campo del formulario raíz o una fila de la grilla de vigencias es inválido,
> **ENTONCES EL SISTEMA DEBE** marcarlo como inválido con un mensaje visible y **NO DEBE** ejecutar la
> escritura.

* *Criterio de verificación*: tests que comprueban el marcado y el mensaje junto al control, y que la
  escritura no se ejecuta.

#### R-PROD-19 — `UMedNombre` sin persistencia
> **MIENTRAS** se presenta el campo `Unidad de Medida`, **EL SISTEMA DEBE** mostrarlo como **no
> editable** y **NO DEBE** incluirlo en el cuerpo de la escritura, dado que `PRODUCTO` **no** posee la
> columna `UMedNombre` y no existe una transacción `UMed` en la KB que lo provea.

* *Criterio de verificación*: test que comprueba el atributo `readonly` del campo y que el cuerpo
  generado **no** contiene la clave `uMedNombre`. Justificación de la no-columna: G-DATA
  (`INFORMATION_SCHEMA.COLUMNS`) y `genexus_query "UMed"` (sólo 2 atributos, sin transacción).

#### R-PROD-28 — Recorte de los campos de texto
> **EL SISTEMA DEBE** enviar los campos de texto (`Producto`, `Unidad de Medida`, `Acopiador?`) sin
> espacios sobrantes al inicio ni al final, porque las columnas físicas son `char(n)` de ancho fijo y
> el espacio de relleno es indistinguible del dato.

* *Criterio de verificación*: test con `prodDesc = "  Algodón  "` que comprueba que el cuerpo enviado
  contiene `"Algodón"`. Base: `PRODUCTO.ProdDesc` es `char(25)` (G-DATA) y el backend ya lee con
  `RTRIM`.

#### R-PROD-29 — Enteros opcionales enviados como `null`
> **SI** los campos `Tipo de cálculo`, `Tipo de Producción` o `Prod Anulado` se dejan vacíos,
> **ENTONCES EL SISTEMA DEBE** enviarlos como ausentes (no como `0`), y **NO DEBE** inventar un valor.

* *Criterio de verificación*: test con los tres campos vacíos que comprueba que el cuerpo enviado los
  omite. Base: el backend aplica el default `0` a las columnas `NOT NULL`
  (`ProductoService.aplicarCampos`); la UI **no** debe anticiparlo.

### 4.3 Derivados `ProdIMax1..4`

#### R-PROD-03 — Los 4 derivados en sólo lectura
> **EL SISTEMA DEBE** mostrar `ProdIMax1..4` (`Importe de Entrada`, `Importe C. Mec. Entrada`,
> `Importe Salida`, `Importre C. Mec. Salida`) como **no editables**, con el valor que devuelve
> `GET /api/v1/productos/{prodCod}/importes-vigentes` al seleccionar un registro — y con `0` mientras no
> haya producto seleccionado — y **NO DEBE** incluirlos en el cuerpo de la escritura.

* *Criterio de verificación*: test que comprueba la llamada a `importes-vigentes`, el valor renderizado,
> el atributo `readonly` y la ausencia de las claves `prodIMax1..4` en el cuerpo de escritura.

### 4.4 Grilla de vigencias (nivel 1)

#### R-PROD-04 — Grilla anidada de vigencias
> **EL SISTEMA DEBE** mostrar, dentro del panel del registro, una grilla con las 5 columnas del nivel 1
> (§3.2) y sus rótulos verbatim, que permita **agregar**, **editar** y **quitar** filas.

* *Criterio de verificación*: test que renderiza la grilla con las 5 cabeceras verbatim; prueba de
  navegador que agrega una fila, edita un importe y quita una fila, y comprueba el número de filas.

#### R-PROD-05 — Envío anidado de las vigencias
> **CUANDO** el usuario confirme el guardado, **EL SISTEMA DEBE** enviar las vigencias **anidadas** en
> el cuerpo, con `prodFchVig` a medianoche y los 4 importes como números decimales.

* *Criterio de verificación*: test que captura el cuerpo del alta y de la modificación y verifica la
> estructura `vigencias[]` con los valores exactos, incluido `prodFchVig` = `T00:00:00`.

#### R-PROD-06 — Unicidad de `ProdFchVig`
> **SI** el usuario agrega dos filas con la misma `ProdFchVig`, **ENTONCES EL SISTEMA DEBE** avisar
> antes de escribir y **NO DEBE** ejecutar la escritura.

* *Criterio de verificación*: test con dos filas de la misma fecha que comprueba el mensaje visible y que
  la escritura no se ejecuta.

#### R-PROD-08 — Decimales de los importes
> **EL SISTEMA DEBE** aceptar importes con **hasta 2 decimales** dentro del rango
> `±9999999999.99` (`NUMERIC(12,2)` lógico) y **NO DEBE** aceptar más de 2 decimales ni valores fuera
> de rango.

* *Criterio de verificación*: test por regla violada (3 decimales, valor fuera de rango) que comprueba
  el marcado y que la escritura no se ejecuta.

#### R-PROD-20 — Normalización de importes ausentes
> **CUANDO** una vigencia cargada tenga `ProdImpu3` o `ProdImpu4` ausente del JSON (columna `NULL`),
> **EL SISTEMA DEBE** mostrarla como `0` y **DEBE** enviarla como `0`, y **NO DEBE** enviarla ausente
> (el DTO de escritura la exige).

* *Criterio de verificación*: test con una vigencia de `PRODUCT1` que tenga `ProdImpu3 IS NULL` (293
  filas reales cumplen esto) que comprueba el valor mostrado, el valor enviado y que la escritura no
  es rechazada con `400`. Base: `jackson.default-property-inclusion: non_null` **omite** la clave del
  JSON; `ProductoVigenciaEscrituraDto` la declara `@NotNull`.

#### R-PROD-21 — Reemplazo total del conjunto de vigencias
> **CUANDO** el usuario modifique un producto, **EL SISTEMA DEBE** enviar **todas** las vigencias del
> producto — incluidas las no modificadas — porque la modificación del backend **reemplaza** el
> conjunto completo de filas hijas.

* *Criterio de verificación*: test que carga un producto con 3 vigencias, modifica sólo una y comprueba
  que el cuerpo enviado contiene las 3; IT contra `Rentas` que comprueba que tras la modificación
  siguen existiendo las 3 filas en `PRODUCT1`.

#### R-PROD-23 — Fecha de vigencia a medianoche
> **EL SISTEMA DEBE** mostrar `ProdFchVig` en formato `dd/MM/yyyy` y **DEBE** enviarla con hora
> `00:00:00`.

* *Criterio de verificación*: test que comprueba el texto renderizado de la celda y el valor del
  parámetro enviado. Base: SA-09 cerrado (§3.4).

#### R-PROD-24 — Rótulo derivado de `ProdFchVig`
> **EL SISTEMA DEBE** rotular la columna de fecha de vigencia como `ProdFchVig` —derivación del nombre
> del atributo, sin descripción en la KB— y **DEBE** señalarla como rótulo derivado en la propia vista.

* *Criterio de verificación*: test que compara el texto de la cabecera con `ProdFchVig` y que verifica
  la marca de derivación. Base: D-20, mismo patrón que SA-04 en `CAMINOS`.

#### R-PROD-27 — Alcance del JavaScript de la grilla
> **EL SISTEMA DEBE** resolver el alta y la baja de filas de la grilla **exclusivamente** clonando y
> quitando una fila de plantilla en el navegador, **DEBE** reindexar los `name`/`id` de la fila clonada
> al índice que le corresponde y **NO DEBE** ejecutar en el navegador ninguna validación, cálculo ni
> regla de negocio.

* *Criterio de verificación*: revisión del archivo `static/js/vigencias.js` (sin `fetch`, sin
  `XMLHttpRequest`, sin validación) y prueba de navegador que comprueba que la fila agregada envía
  `vigencias[1].prodFchVig` (y no `vigencias[0].…`).

### 4.5 Errores, confirmación y escritura

#### R-PROD-11 — Mapeo de errores del backend
> **SI** la escritura o la carga falla, **ENTONCES EL SISTEMA DEBE** mostrar el `message` devuelto por el
> servicio en un mensaje **inline** y **DEBE** conservar el contenido del formulario y de la grilla.

* *Criterio de verificación*: tests que fuerzan `409 PK_DUPLICADA`, `409 VIGENCIA_DUPLICADA`,
> `404 NO_ENCONTRADO` y `400 VALIDACION`, y comprueban el mensaje visible y la conservación del
  formulario.

#### R-PROD-14 — Confirmación de baja
> **EL SISTEMA DEBE** solicitar confirmación explícita antes de ejecutar una baja y **NO DEBE** ejecutar
> el borrado si el usuario cancela.

* *Criterio de verificación*: prueba de navegador que cancela la confirmación y comprueba que no se
  ejecuta el borrado. El `confirm()` nativo es JS puntual permitido (`docs/design.md` §8).

#### R-PROD-22 — Escrituras por formulario con CSRF
> **EL SISTEMA DEBE** ejecutar las escrituras mediante formularios `POST` con `th:action` (token CSRF
> activo) y **NO DEBE** invocar la API REST `/api/v1/productos` por HTTP desde el servidor ni desde el
> navegador.

* *Criterio de verificación*: test que comprueba que el formulario renderizado incluye el campo CSRF
  oculto y que el controller MVC invoca el **servicio**, no el cliente HTTP.

### 4.6 No funcionales

#### R-PROD-15 — No regresión del contrato REST
> **CUANDO** se implemente la pantalla, **EL SISTEMA DEBE** mantener sin cambios los 7 endpoints
> `/api/v1/productos*` y su forma de payload, y **NO DEBE** modificar `web/rest/**`.

* *Criterio de verificación*: `git diff` sin cambios en `nucleo/web/rest/**`; la colección Postman sigue
  sincronizada (G-POSTMAN).

#### R-PROD-16 — Responsive
> **EL SISTEMA DEBE** apilar los paneles por debajo de 960 px de ancho y **DEBE** mantener la grilla de
> vigencias utilizable mediante scroll horizontal.

* *Criterio de verificación*: inspección visual en 375 px y en 1440 px.

#### R-PROD-17 — Accesibilidad AA
> **EL SISTEMA DEBE** mantener un contraste ≥ 4,5:1, asociar un `<label>` a cada campo del formulario y
> a cada columna de la grilla, garantizar foco visible y navegación por teclado, y anunciar los errores.

* *Criterio de verificación*: `lighthouse_audit` (accesibilidad) y `axe-core` in-page en **dos**
  estados (inicial y edición), según `docs/design.md` §7.

#### R-PROD-30 — Sincronización de la colección Postman
> **SI** la implementación agrega, modifica o elimina un endpoint del contrato REST,
> **ENTONCES EL SISTEMA DEBE** actualizar `docs/postman/ATPPuestos-API.postman_collection.json` en el
> mismo commit.

* *Criterio de verificación*: revisión del diff del commit: si hay cambios en `web/rest/**`, el
  Postman cambia en el mismo commit. Base: G-POSTMAN (`AGENTS.md`, regla 6).

---

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB) | Requisito nuevo | Prueba |
|---|---|---|
| `Transaction Productos` (`Structure`: 12 atributos raíz + `Level1`) | R-PROD-02, R-PROD-24 | Test de rótulos |
| `WebForm` `GxMultiForm`, `<data>` × 12 con `labelCaption="=Att.ContextualTitle"` | R-PROD-02 | Test de rótulos y de orden |
| `WebForm` `simplegrid controlName="GridProductos_Level1Item"` (items `att:171,44,173,175,177`) | R-PROD-04, R-PROD-05 | Test de grilla + body anidado |
| `WebForm` `textblock TitleLevel1 caption="Level1"` | R-PROD-04, R-PROD-24 (SA-P02) | Test de cabecera de la grilla |
| `WebForm` `actionGroup NavButtons` (`btn_first/previous/next/last/select`) | R-PROD-13 | Test de navegación |
| `WebForm` `actionGroup FormButtons` (`btn_enter/btn_cancel/btn_delete`) | R-PROD-01, R-PROD-14 | Tests de escritura + confirmación |
| `WebForm` `errorviewer controlName="ErrorViewer"` | R-PROD-11 | Test de mapeo de error |
| Fórmulas `ProdIMax1..4` (`max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpuN)`) | R-PROD-03 | Test de derivados |
| `ProdIMax1..4` sin columna física | R-PROD-03 | Test: ausentes del body de escritura |
| `UMedNombre` sin columna y sin transacción `UMed` | R-PROD-19 | Test: `readonly` + ausente del body |
| `ProdFchVig : DATE(8)` sobre `datetime` | R-PROD-23 | Test de formato y medianoche |
| `Table Producto` / `Table Product1` (`Structure`) | R-PROD-07, R-PROD-09, R-PROD-20, R-PROD-21, R-PROD-28 | Tests de cuerpo y de paridad de BD |
| `01-nucleo-datos` R-004 (2 niveles), R-005/R-035 (derivados), R-034 (atomicidad), R-037 (PK en el body) | R-PROD-03, R-PROD-05, R-PROD-09, R-PROD-21 | Tests de contrato + IT |
| `00-alcance` R-ALC-002 (sin DDL), R-ALC-006 (render por servidor) | R-PROD-15, R-PROD-22 | Revisión de diff |
| `docs/design.md` §5, §6, §7, §8 | R-PROD-16, R-PROD-17, R-PROD-27 | Inspección + tests |
| `conventions.md` §3 (rutas de página sin prefijo), §6 (textos verbatim) | R-PROD-01, R-PROD-02, R-PROD-24 | Test de ruta + test de rótulos |

### 5.2 Matriz de paridad funcional

| Operación legacy | Requisito | Prueba prevista |
|---|---|---|
| `Enter` (alta) | R-PROD-01, R-PROD-05, R-PROD-09 | `MvcAlta` + IT `Rentas` |
| `Enter` (modificación) | R-PROD-01, R-PROD-21 | `MvcModificacion` + IT `Rentas` |
| `Cancel` | R-PROD-01 (regreso a modo alta, formulario y grilla limpios) | `MvcCancelar` |
| `Delete` | R-PROD-14 | `MvcBaja` (con y sin confirmación) |
| `First` / `Previous` / `Next` / `Last` / `Select` | R-PROD-13, R-PROD-25 | `MvcNavegacion` |
| Alta de fila de vigencia | R-PROD-04, R-PROD-27 | Test de navegador + test de binding |
| Baja de fila de vigencia | R-PROD-04, R-PROD-27 | Test de navegador + test de binding |
| Edición de importe de vigencia | R-PROD-05, R-PROD-08 | Test de binding |
| Cálculo de `ProdIMax1..4` | R-PROD-03 | Test de `importes-vigentes` |

---

## 6. Desviaciones y decisiones heredadas

| # | Desviación | Justificación | Riesgo |
|---|---|---|---|
| **D-19** *(corregida)* | `UMedNombre` es **de sólo lectura** y **no** se envía | `PRODUCTO` **no** tiene la columna y **no** existe transacción `UMed` en la KB ⇒ no hay destino posible. La mini-spec T-505b lo declaraba `varchar(40)`: **incorrecto** (G-DATA lo desmiente). | Bajo |
| **D-20** | El rótulo de `ProdFchVig` es la derivación `ProdFchVig` | La KB no declara descripción para ese atributo (mismo patrón que SA-04 en `CAMINOS`) | Bajo |
| **D-21** | La navegación opera sobre la colección descargada | `GET /api/v1/productos` no pagina | Bajo |
| **D-22** | Los 4 derivados requieren una **segunda llamada** | `ProductoDto` no los incluye; el backend los expone en `importes-vigentes` | Bajo |
| **D-23** | El flujo **no** consume `GET /api/v1/productos/{prodCod}` | La colección ya trae las `vigencias[]` anidadas ⇒ la selección usa ese dato y se ahorra una llamada. El endpoint sigue en el contrato para otros consumidores. | Bajo |
| **D-24** | Se **corrige** la fila de la mini-spec que declaraba `UMedNombre` como `varchar(40)` persistido | Verificado en `INFORMATION_SCHEMA.COLUMNS`: `PRODUCTO` tiene 8 columnas y ninguna es `UMedNombre` | — |
| **D-25** *(nueva)* | La **ruta de página** es `/productos` (no `/maestras/productos`) | `conventions.md` §3: "Rutas de página: `/<recurso>` (sin prefijo)". El hub `/maestras` pertenecía a la app Angular de origen, que no existe en este monorepo. | Bajo |

---

## 7. Supuestos abiertos

> Cada supuesto lleva la **pregunta exacta** que hay que responder. Ninguno bloquea la
> implementación: la decisión provisional es la que se declara en «Decisión provisional».

| ID | Pregunta | Decisión provisional | Riesgo si se resuelve distinto |
|---|---|---|---|
| **SA-P01** | ¿Debe `UMedNombre` ("Unidad de Medida") persistirse? `PRODUCTO` no tiene la columna, no hay transacción `UMed` en la KB y el backend descarta el valor. ¿Se acepta que la pantalla lo muestre de sólo lectura y vacío, o se autoriza (fuera de alcance, G5) una columna nueva? | Sólo lectura, fuera del body | Medio (campo inerte en pantalla) |
| **SA-P02** | ¿El título de la sección del nivel 1 debe ser el literal del legacy `Level1` (paridad de identidad) o un rótulo descriptivo `Vigencias`? | **`Level1`** (verbatim) | Bajo |
| **SA-P03** | ¿Qué textos literales corresponden a los botones del legacy `GX_BtnEnter`, `GX_BtnCancel`, `GX_BtnDelete`, `GX_BtnSelect` en la localización de `ATPPuestos`? | `Guardar` / `Cancelar` / `Eliminar` / `Seleccionar` (según la mini-spec T-505b) | Bajo |
| **SA-P04** | ¿La restriction de `prodCod` a `0…32767` es la correcta? El tipo **lógico** KB es `NUMERIC(3)` (`0…999`) y la columna **física** `smallint` admitiría negativos; la mini-spec fija `0…32767`. | `0…32767` | Bajo |
| **SA-P05** | ¿El filtro de anulados debe existir en esta pantalla? El legacy no lo expone y el endpoint ya lo soporta. | Sí (usa el parámetro existente, no altera el contrato) | Bajo |
| **SA-P06** | ¿Se confirma la ruta `/productos` (convención del repositorio) frente a `/maestras/productos` (mini-spec Angular)? | `/productos` | Bajo |
| **SA-P07** | ¿Esta pantalla debe incorporate ya con un marcador de autorización, o queda expuesta hasta que `03-login` incorpore Spring Security? | Queda expuesta hasta `03-login`; esta spec **no** introduce autenticación | Medio (exposición temporal) |
| **SA-P08** | ¿Se acepta la semántica de **reemplazo total** de vigencias en la modificación (la que tiene el backend hoy) o debe el backend pasar a un modelo diferencial (borrado explícito por clave)? | Reemplazo total; la UI envía siempre todas las filas | Bajo |

---

## 8. Criterios de aceptación (puerta G-TEST)

1. `./mvnw clean test` en verde con **≥ 25 pruebas nuevas** de vistas (render, binding, navegación,
   validaciones, errores) y **sin regresión** sobre la suite existente de `01-nucleo-datos`.
2. Pruebas de integración (`*IT`) contra `Rentas` en verde (`ATP_DB_IT=true`): alta, modificación con
   cambio de un importe de la grilla, y baja — comprobando en la BD que las filas de `PRODUCT1`
   sobreviven a una modificación parcial (R-PROD-21).
3. `./mvnw test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"` en verde, con **rollback** (la
   BD de producción no debe quedar modificada).
4. Prueba de navegador real contra el backend: alta de un producto **con una vigencia**, modificación
   **cambiando un importe de la grilla**, baja, navegación First/Prev/Next/Last, alta y baja de filas
   de la grilla, y verificación de que la fila y sus importes quedan persistidos en `PRODUCT1`.
5. `axe-core` in-page en **dos** estados (inicial y edición) sin hallazgos críticos; `lighthouse_audit`
   sin audits de accesibilidad en rojo.
6. `git diff` **sin** cambios en `nucleo/web/rest/**` y **sin** DDL ni migraciones (G5).
7. Colección Postman sincronizada (G-POSTMAN): los 7 requests de `Productos` intactos salvo
   actualización documentada si algún endpoint cambia.
8. Sin secretos en el diff (G-SEC); ningún archivo de configuración por encima de 500 líneas (G4).
