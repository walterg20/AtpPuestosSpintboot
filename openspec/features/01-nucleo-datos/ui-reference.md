# UI Reference — Maestras geográficas y catálogo (T-502)

> **PORTADO (2026-10-22)** desde el repositorio `atppuestos2` (commit `974c3d6`). Inventario visual
> del legacy: sigue siendo la referencia de **comportamiento** de las 8 maestras y de
> `ListadoLocalidades`, con independencia del framework de presentación.

> **Alcance**: Inventory visual de las 8 transacciones maestras y `ListadoLocalidades` (único WebPanel de esta familia).  
> **Restricción**: Solo lectura sobre KB GeneXus 18. Solo escritura dentro de `openspec/features/**`.  
> **Destino**: `openspec/features/01-nucleo-datos/ui-reference.md` (desvío con respecto a `docs/` mencionado en T-502; ver §8).  
> **Fuente**: `ATPPuestos` (GeneXus 18.0.13.186702) vía MCP `genexus18mcp` (solo lectura).

## 1. Método y cadena de evidencia

**Herramientas MCP usadas (solo lectura):**
- `genexus_list_objects` (typeFilter, nameFilter, limit): inventario de objetos. Resultado en `.results` (no `.items`). WebPanels filtrados por `typeFilter="WebPanel"` (69 totales). `ListadoLocalidades` localizado por `nameFilter="ListadoLocalidades"` con `type="WebPanel"`.
- `genexus_read({name, type, part, projection:"standard"})`: lectura de fuente. Para transacciones: `part` in `webForm|structure|rules|events|variables|conditions`. Para WebPanel `ListadoLocalidades`: `part` in `webForm|structure|rules|events|variables|conditions`. La fuente vive en `.source`. `xmlKind` reportado. `kb` explicitado cuando requerido.
- `genexus_structure` (inspección de índices/metadata): usado para confirmar nombres físicos/truncamientos (§4.3).

**Reglas de evidencia:** Cada afirmación sobre UI (botones, grid, campos, etiquetas, eventos) debe poder trazarse a un objeto GeneXus + parte leída + `.source` offset. No se inventa HTML/CSS/runtime. Las etiquetas son: (a) `title/textblock CaptionExpression` (Tokens Attribute/Constant), (b) `labelCaption="=Att.ContextualTitle"`, (c) descripción del atributo en `structure` (prefijo `// "..."`).

## 2. (a) Inventario legacy→maestra

| Maestra (nombre legacy) | Tipo GX | Ruta en KB | ABM propio (form del objeto) | WebPanel asociado con nombre de la maestra | `xmlKind` webForm | Source len (webForm) | MCP tool + invocación (evidencia) |
|---|---|---|---|---|---|---|---|
| `Provincia` | Transaction | `Root Module/Transacciones/Provincia` | Sí (Transaction.webForm) | No | `GxMultiForm` | 5641 | `genexus_read({name:"Provincia", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Localidades` | Transaction | `Root Module/Transacciones/Localidades` | Sí (Transaction.webForm) | No | `GxMultiForm` | 6693 | `genexus_read({name:"Localidades", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Calles` | Transaction | `Root Module/Transacciones/Calles` | Sí (Transaction.webForm) | No | `GxMultiForm` | 5635 | `genexus_read({name:"Calles", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `CAMINOS` | Transaction | `Root Module/Varios/CAMINOS` | Sí (Transaction.webForm) | No | `GxMultiForm` | 5634 | `genexus_read({name:"CAMINOS", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Areas` | Transaction | `Root Module/Transacciones/Areas` | Sí (Transaction.webForm) | No | `GxMultiForm` | 5630 | `genexus_read({name:"Areas", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Jurisdic` | Transaction | `Root Module/Transacciones/Formulas/Jurisdic` | Sí (Transaction.webForm) | No | `GxMultiForm` | 5635 | `genexus_read({name:"Jurisdic", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Feriados` | Transaction | `Root Module/Feriados/Feriados` (Folder/Table/Index colisionan con nombre) | Sí (Transaction.webForm) — raw HTML | No | `GxMultiForm` (mismo `xmlKind` en lectura) | 4363 | `genexus_read({name:"Feriados", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `Productos` | Transaction (2 niveles) | `Root Module/Transacciones/Productos` | Sí (Transaction.webForm) — con grilla de nivel 1 | No | `GxMultiForm` | 11091 | `genexus_read({name:"Productos", type:"Transaction", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |
| `ListadoLocalidades` | WebPanel | `Root Module/Transacciones/ListadoLocalidades` (guid fea40714-7e3c-4888-a325-5c7895912f48) | N/A (selector/listado) | Sí (único relacionado a maestras) | `GxMultiForm` | 6239 | `genexus_read({name:"ListadoLocalidades", type:"WebPanel", part:"webForm", projection:"standard", kb:"ATPPuestos"})` |

**Hallazgo clave:** No existe un WebPanel nombrado con ninguna maestra. El ABM visual de las 8 maestras es el **form propio de la Transaction** (generado por GeneXus). `ListadoLocalidades` es el único WebPanel in-scope (maestra-related).

## 3. (b1) Patrón visual general: GxMultiForm (predeterminado)

**Forma general (7 de 8):** `Provincia, Localidades, Calles, CAMINOS, Areas, Jurisdic, Productos` → `xmlKind=GxMainWebForm`/`GxMultiForm` generado. Elementos comunes observados:
- Título: textblock con `caption="NombreObjeto"` (ej. `caption="Provincia"`, `caption="Localidades"`, `caption="Calles"`, `caption="CAMINOS"`, `caption="Areas"`, `caption="Jurisdic"`; `Productos` usa `textblock` con `caption="Productos"`). `ListadoLocalidades` no tiene textblock título en el body (caption se setea por `Form.Caption` en `Event Start`: `Format("GXSPC_SelectionList","Localidades")`).
- Error: `errorViewer` / `gxErrorViewer` (`ctlError` en `Feriados` y presente en GxMultiForm).
- Navegación: `actionGroup controlName="NavButtons"` con acciones `btn_first`, `btn_previous`, `btn_next`, `btn_last`, `btn_select` (`onClickEvent=First/Previous/Next/Last/Select`).
- Botonera de formulario: `actionGroup controlName="FormButtons"` con `btn_enter`, `btn_cancel`, `btn_delete` (presentes en GxMultiForm). `Productos` muestra también `Toolbar` + `NavButtons` + `FormButtons` (estructura conforme a layout 17.11.0).
- Campos: filas con `textblock` (etiqueta) + `data attribute="att:N"` con `labelCaption="=Att.ContextualTitle"` o con `CaptionExpression` basado en Tokens Attribute/Constant. Los atributos del formulario corresponden a la estructura de la transacción.
- `Productos` incluye grilla anidada (nivel 1): `simplegrid controlName="GridProductos_Level1Item"` con items `att:171,44,173,175,177` (ProdFchVig, ProdImpu1..4) y `titleExp="=Att.ContextualTitle"`. Estructura dos niveles confirmada (§4.1.1).

**Excepción (raw HTML):** `Feriados` → formulario HTML con `<BODY>`, `<TABLE id="Table1/Table2">`, botones `btn_first/previous/next/last/select` en fila superior, `gxErrorViewer ctlError`, 5 filas de etiqueta+atributo (`att:394,392,393,396,395`), botón `btn_get` junto a `Fecha del Feriado` (`Event="Get"`), y botonera inferior `btn_enter/btn_check/btn_cancel/btn_delete/btn_help`. **Sin grilla**. (Verificado en `.source` completo.)

## 4. (b2) Referencia visual por pantalla

### 4.1 `Provincia` (Transaction.webForm, GxMultiForm)
- Título: `caption="Provincia"`.
- Error: `errorViewer`.
- NavButtons: First/Prev/Next/Last/Select.
- FormButtons: Enter/Cancel/Delete.
- Campos (estructura):
  - `ProvCod* NUMERIC(4)` — etiqueta "Prov Cod" (ContextualTitle/description). att id no explicitado aquí; corresponde a campo PK.
  - `ProvNombre VARCHAR(40)` — etiqueta "Prov Nombre".
- Source: 5641 bytes, `xmlKind=GxMultiForm`. (Evidencia: `genexus_read` Provincia webForm.)

### 4.2 `Localidades` (Transaction.webForm, GxMultiForm)
- Título: `caption="Localidades"`.
- Error: `errorViewer`.
- NavButtons + FormButtons.
- Campos:
  - `LocCod* NUMERIC(5)` — "Código de Localidad"
  - `LocNom CHARACTER(20)` — "Nombre de la Localidad"
  - `LocNomProv CHARACTER(15)` — "Provincia"
  - `LocCodPost NUMERIC(4)` — "Cod. Postal de la Localidad"
  - `ProvCod NUMERIC(4)` — "Prov Cod" (FK lógica)
- Source: 6693 bytes.

### 4.3 `Calles` (Transaction.webForm, GxMultiForm)
- Título: `caption="Calles"`.
- Campos: `CallCod* NUMERIC(4)` "Código de Calle", `CallNom CHARACTER(22)` "Descripción de la Calle".
- Botonera navegación+acción estándar.
- Source: 5635 bytes.

### 4.4 `CAMINOS` (Transaction.webForm, GxMultiForm)
- Título: `caption="CAMINOS"`.
- Campos: `CamID* CHARACTER(3)` "Cam ID", `CanRuta CHARACTER(100)` (sin descripción en structure → etiqueta visible deriva de nombre lógico/ContextualTitle).
- Source: 5634 bytes.

### 4.5 `Areas` (Transaction.webForm, GxMultiForm)
- Título: `caption="Areas"`.
- Campos: `AreaId* NUMERIC(10)` "Identificación del Area", `AreaDesc CHARACTER(50)` "Descripción".
- Source: 5630 bytes.

### 4.6 `Jurisdic` (Transaction.webForm, GxMultiForm)
- Título: `caption="Jurisdic"`.
- Campos: `JurCod* NUMERIC(3)` "Código de la Jurisdiccion", `JurNom CHARACTER(20)` "Nombre de la  Jurisdicción" (doble espacio en descripción original).
- Source: 5635 bytes.

### 4.7 `Feriados` (Transaction.webForm, raw HTML)
- Formulario HTML `<BODY>`, dos tablas (`Table1`, `Table2`). **Sin grilla**.
- Navegación superior: `btn_first`, `btn_previous`, `btn_next`, `btn_last`, `btn_select` (`Event=First/Previous/Next/Last/Select`).
- Error: `gxErrorViewer ControlName="ctlError"`.
- Campos (filas con etiqueta constante + atributo att:):
  1. `Fecha del Feriado` — `att:394`, junto a `btn_get` (`Event="Get"`) (lookup/acción por fecha)
  2. `Descripción del Feriado` — `att:392`
  3. `Fecha del Registro` — `att:393` (auditoría)
  4. `Usuario del Registro` — `att:396` (auditoría)
  5. `Tipo de Feriado ?` — `att:395`
- Botonera inferior: `btn_enter` (Enter), `btn_check` (Check), `btn_cancel` (Cancel), `btn_delete` (Delete), `btn_help` (Help). **Distinto** al set estándar (incluye `Check` y `Help`; no incluye `Select` aquí).
- Source: 4363 bytes. (Evidencia: lectura webForm con `kb:"ATPPuestos"`.)

### 4.8 `Productos` (Transaction.webForm, GxMultiForm, 2 niveles)
- Título: `caption="Productos"`, `errorViewer`.
- Estructura layout responsive (17.11.0). `Toolbar` con `NavButtons` (First/Prev/Next/Last/Select). `FormButtons` (Enter/Cancel/Delete).
- Nivel 0 (raíz) — campos en filas, `labelCaption="=Att.ContextualTitle"`:
  - `att:43` = `ProdCod* NUMERIC(3)` "Código"
  - `att:166` = `ProdDesc CHARACTER(25)` "Producto"
  - `att:34`  = `ProdTipCal NUMERIC(1)` "Tipo de cálculo"
  - `att:168` = `TPPriID NUMERIC(1)` "Tipo de Producción"
  - `att:170` = `ProdIMax1 NUMERIC(12,2)` "Importe de Entrada" (**fórmula derivada**, no editable)
  - `att:172` = `ProdIMax2 NUMERIC(12,2)` "Importe C. Mec. Entrada" (**derivado**)
  - `att:174` = `ProdIMax3 NUMERIC(12,2)` "Importe Salida" (**derivado**)
  - `att:176` = `ProdIMax4 NUMERIC(12,2)` "Importre C. Mec. Salida" (**derivado**; typo legacy)
  - `att:169` = `UMedNroId CHARACTER(3)` "Código"
  - `att:178` = `UMedNombre VARCHAR(40)` "Unidad de Medida"
  - `att:167` = `ProdAcop CHARACTER(1)` "Acopiador?"
  - `att:411` = `ProdAnula NUMERIC(1)` "Prod Anulado"
- Nivel 1 — grilla `simplegrid controlName="GridProductos_Level1Item"`:
  - `att:171` = `ProdFchVig* DATE(8)` (sin descripción; `titleExp="=ProdFchVig.ContextualTitle"`)
  - `att:44`  = `ProdImpu1 NUMERIC(12,2)` "Importe Fijo Entrada"
  - `att:173` = `ProdImpu2 NUMERIC(12,2)` "Imp.Cosech.Mec Entrada"
  - `att:175` = `ProdImpu3 NUMERIC(12,2)` "Importe Fijo Salida"
  - `att:177` = `ProdImpu4 NUMERIC(12,2)` "Imp.Cosech.Mec Salida"
- Fórmulas (semántica): `ProdIMax1..4 = max(ProdFchVig, ProdFchVig <= serverdate(), 0, ProdImpu1..4)` → toma el valor de la vigencia más reciente con `ProdFchVig <= hoy`; 0 si ninguna. Estos campos son **derivados** (no se persisten) — ver R-005/R-035.
- Source: 11091 bytes. (Evidencia: webForm + structure.)

### 4.9 `ListadoLocalidades` (WebPanel.webForm)
- WebPanel (type=WebPanel), no ABM de transacción. Caption runtime: `Event Start` → `Form.Caption = Format("GXSPC_SelectionList","Localidades")`.
- Error: `gxErrorViewer ControlName="ctlError"`.
- Filtros (`FIELDSET Caption "Filters"`, LEGEND "Filters"):
  - `&cLocCod` (Attribute:LocCod) — etiqueta `Attribute 57 Title` (`TextBlockLocCod`)
  - `&cLocNom` (Attribute:LocNom) — `Attribute 14 Title` (`TextBlockLocNom`)
  - `&cLocNomProv` (Attribute:LocNomProv) — `Attribute 16 Title` (`TextBlockLocNomProv`)
  - `&cLocCodPost` (Attribute:LocCodPost) — `Attribute 160 Title` (`TextBlockLocCodPost`)
- Selección (`FIELDSET Caption "GXM_sltlst"`, LEGEND "Lista de Selección"):
  - `gxGrid ControlName="Grid1" MaxRows="5" BackColorStyle="Header" cellPadding="1" cellSpacing="0"`
    - Columna 0: `var:9` (BITMAP/selection), `ReturnOnClick="True"`, `title="GX_BtnSelect"`, `ColTitleExpression` vacío
    - Columna 1: `att:57` (LocCod), `ColTitleExpression` constante `"Cod. Loc."` (custom)
    - Columna 2: `att:14` (LocNom), `ReturnOnClick="True"`, `ColTitleExpression = Attribute 14 ContextualTitle`, `title="GX_BtnSelect"`
    - Columna 3: `att:16` (LocNomProv), `ColVisible="False"`, `ColTitleExpression = Attribute 16 ContextualTitle`
    - Columna 4: `att:244` (atributo id 244), `ColTitleExpression` constante `"Provincia"` (custom title)
    - Columna 5: `att:160` (LocCodPost), `ColTitleExpression = Attribute 160 ContextualTitle`
- Botonera: `gxButton ControlName="btn_cancel" Event="Cancel" CaptionExpression constant "GX_BtnCancel"`.
- Events:
  - `Start`: `Form.Caption = Format("GXSPC_SelectionList","Localidades")`
  - `Load`: `&LinkSelection.FromImage(selectRow)` (BITMAP(4))
  - `Enter`: `&pLocCod = LocCod; &pLocNom = LocNom; &pProvNombre = ProvNombre; Return`
- Rules/conditions/parm: `parm(out:&pProvNombre,out:&pLocNom,out:&pLocCod); order(LocCod);` conditions `LocCod>=&cLocCod; LocNom LIKE &cLocNom; LocNomProv LIKE &cLocNomProv; LocCodPost>=&cLocCodPost;` (LIKE may be case-insensitive por configuración). Variables: `&cLocCod/&cLocCodPost/&cLocNom/&cLocNomProv (Attribute:…)`, `&LinkSelection BITMAP(4)`, `&pLocCod/&pLocNom/&pProvNombre (Attribute:…)`, `&Today DATE(8)`, `&Time/&Pgmname/&Pgmdesc`.
- Source: 6239 bytes. (Evidencia completa: webForm/rules/events/variables/conditions.)

**Nota `att:244` — RESUELTA (T-503):** En el grid aparece `att:244` con título constante `"Provincia"`. Dado que `ProvNombre` pertenece a `Provincia` (no a `Localidades`), el título "Provincia" indica que la columna muestra el **nombre de la provincia asociada** (valor de la relación `Localidades → Provincia` sobre `ProvCod`), y no el atributo denormalizado `LocNomProv` (que, además, la columna 3 declara `ColVisible="False"`).

**Resolución adoptada (T-503):** la columna "Provincia" se alimenta de `provNombre` del DTO backend `LocalidadDto(locCod, locNom, locCodPost, provCod, provNombre)`, que ya resuelve la asociación `R-003` en el servidor. No se usa `locNomProv` para esta columna. El filtro legacy por `LocNomProv` (`&cLocNomProv`) queda **sin equivalente en T-503** porque el backend `GET /api/v1/localidades` sólo admite filtro por `provCod`; se registra como gap de paridad en **T-507**.

> **Estado de supuestos**: `SA-08` (mapping `att:244`) → **CERRADO** por resolución en T-503. `SA-09` (formato DATE/DATETIME runtime) permanece **ABIERTO** y afecta a T-505 (`Feriados`/`Productos`).

## 5. (c) Mapeo Angular 22 (tokens + componentes)

> **SUPERADO para la capa de presentación (2026-10-22).** Este proyecto migró de Angular a
> **Thymeleaf**. El mapeo de abajo a **Angular Material** queda como registro histórico de la
> implementación anterior; el vigente es el de tokens CSS + HTML semántico de `docs/design.md` §5.
> El *inventario* de la sección (campos, acciones, rótulos, comportamientos) sigue siendo válido
> como fuente de las specs Thymeleaf.

**Tokens (alineados a `docs/design.md` §2.2/§3/§4):**
- Marca: `$atp-primary: #C60B44`, `$atp-primary-dark: #9D1F1F`, `$atp-primary-contrast: #FFFFFF`
- Neutrales: `$atp-surface: #FFFFFF`, `$atp-surface-alt: #F0F0F0`, `$atp-border: #D3D4D3`, `$atp-text: #333333`, `$atp-text-muted: #747474`, `$atp-disabled: #A9A9A9`
- Semánticos: `$atp-info: #1D6893`, `$atp-warn: #FF8000`, `$atp-error: #C0392B` (desvío intencional por contraste AA desde `#FF0000`), `$atp-success: #2E7D32`
- Tipografía: `$atp-font: "Segoe UI","Trebuchet MS",Verdana,Arial,sans-serif`; `$atp-font-mono: "Cascadia Mono",Consolas,"Courier New",monospace`. Escala: display 24/600, title 18/600, body 14/400, caption 12/400.
- Espaciado/forma: escala 4px (4,8,12,16,24,32), radio 4px, elevación `mat-elevation-z1/z2`.

**Mapa legacy→Material (Angular 22, Standalone):**
- Grid/FreeStyleGrid → `MatTable` + `MatPaginator` + `MatSort` + estados `loading/empty/error`. Columnas: mostrar etiquetas visibles (incluye títulos personalizados observados). Para `ListadoLocalidades`, replicar columnas: Selección (acción), Cod. Loc., Localidad (LocNom), Provincia (att:244), Cod. Postal. `LocNomProv` permanece oculto (no visible).
- Formularios (GxMultiForm) → `MatFormField` + `ReactiveFormsModule` (Standalone). Campos PK editables solo en alta según regla del backend (R-037: PK provista en body; sin IDENTITY). Campos derivados (`ProdIMax1..4`) **readonly**.
- Botones (`BaseRBtn`, acción) → `MatButton` (primary/stroked/icon) según acción (Enter/Cancel/Delete/Check/Help/First/Prev/Next/Last/Select).
- ErrorViewer/mensajes → `MatSnackBar` (errores operativos) + mensajes inline. Errores API uniformes `{code,message}` (T-309).
- Date → `MatDatepicker` (alineado a `DATE(8)`; `DATETIME(8,5)` debe mapearse a control datetime apropiado o `mat-input + pattern` respetando semántica; `Feriados.FerFecha`/`ProdFchVig` → `LocalDate`/`LocalDateTime` en dominio).
- Popup/dialogo → `MatDialog` (según design.md §5).
- `ListadoLocalidades` → listado con filtros (4 campos) + grilla paginada/scroll con `MaxRows=5` (replicar comportamiento; considerar `pageSize=5` o `MatPaginator` según implementación) + acción Cancel/selección por fila.

**Patrones UX (replicar intención legacy):**
- Navegación registro-a-registro (First/Prev/Next/Last/Select) presente en ABMs GxMultiForm.
- `Feriados` incluye `btn_get` junto a Fecha del Feriado (lookup) y `btn_check`/`btn_help` → mantener acciones equivalentes en UI Angular.
- `Productos` grilla de vigencias anidada editable/gestiona N filas (alineado a R-034: persistencia atómica raíz+hijas, unicidad `ProdFchVig`).
- `ListadoLocalidades` selección por click en fila (ReturnOnClick) + botón Select visual — replicar flujo de retorno a llamador.

## 6. (d) Trazabilidad a requisitos/tasks

**Requisitos (spec.md 01-nucleo-datos):**
- R-020 (Ubicuo): exponer maestras bajo `/api/v1` con CRUD → mapea a formularios ABM replicando origen (Feriados raw form + GxMultiForm pattern). Trazado a pantallas T-504/T-505.
- R-021 (Ubicuo): filtrar `localidades` por `ProvCod` y `feriados` por rango de fechas → `ListadoLocalidades` filtros ya incluyen 4 campos (incluye `&cLocCod` por LocCod; filtro por ProvCod puede aplicarse vía selector de Provincia o por `LocNomProv` según caso de uso) + `Feriados` filtrado por rango fechas (R-021). UI debe soportar ambos.
- R-030 (Ubicuo): create/modify/delete → ABMs completos (P-01). Formularios replican patrones: 7 GxMultiForm + Feriados raw.
- R-031 (Comportamiento no deseado): 409 duplicado / 404 inexistente → UI muestra mensajes uniformes.
- R-032 (Ubicuo): 400 validación → validación en línea con MatFormField.
- R-033 (Dirigido por contexto): `Feriados` auditoría (`FerFchReg`, `FerUsuario`) → UI no permite editar estos campos (audit-only), backend los completa.
- R-034 (Dirigido por contexto): `Productos` atómico raíz+vigencias + unicidad `ProdFchVig` → UI grilla de vigencias con validación.
- R-035 (Ubicuo): recalculo `ProdIMax1..4` derivados → UI muestra readonly derivados.
- R-036 (Ubicuo): mapear/ persistir columnas legacy (`ProvCodAfip`, `LocCodAfip`, `AreaEmails`, etc.) → UI no requiere exponerlas (negocio interno) salvo que sean visibles en origen (no observadas en webForm). Mantener compatibilidad backend.
- R-037 (Ubicuo): PK provista en body (sin IDENTITY) → UI exige PK en alta; en edición PK readonly.
- P-01 (resuelto): ABM completo + fase 2 pantallas replicando CSS/JS/HTML origen → patrón GxMultiForm + Feriados raw HTML son la referencia visual.

**Tareas (tasks.md §I fase 2):**
- T-502 (esta tarea): documentar referencia visual en `openspec/features/01-nucleo-datos/ui-reference.md` (destino actual). **Path deviation:** destino propuesto era `docs/` (ver tarea T-502 en tasks.md: “documentar la referencia visual en `docs/`”); se usa `openspec/features/01-nucleo-datos/ui-reference.md` por restricción de escritura (`openspec/features/**`) y ubicación coherente con spec/plan/tasks. Ver §8.
- T-503: `localidad-list` (standalone, Reactive Forms) con filtro por provincia → deriva de `ListadoLocalidades` (filtros 4 campos + grilla).
- T-504: ABM maestras simples (Provincia, Calles, Caminos, Áreas, Jurisdicciones) replicando diseño origen → patrón GxMultiForm.
- T-505: ABM `Feriados` y `Productos` (con grilla vigencias anidadas) → Feriados raw form + Productos grid nivel 1.
- T-506: E2E `ListadoLocalidades` + un ABM contra backend.

## 7. (d,e) Supuestos abiertos (SA) + preguntas exactas

| ID | Supuesto / ambigüedad | Evidencia disponible | Pregunta exacta para responder | Impacto en T-503..T-506 |
|---|---|---|---|---|
| SA-01 | ¿Hay patrón WorkWithPlus/Unanimo oculto? | `wwpMetadata` no presente en `genexus_read` (sin part) ni en lecturas parciales; patrones observados son GxMultiForm + raw HTML. | “¿Alguna de las 8 transacciones o `ListadoLocalidades` es WorkWithPlus (tiene wwpMetadata/UX object asociado)? Si sí, ¿qué objeto UX y qué diferencias con el form actual?” | Alto (podría requerir replicar UX WP+ distinto). |
| SA-02 | CSS/JS runtime (tema GeneXus) vs design.md | WebForm XML solo layout. Clases `d4876646-…`, estilos inline/classref. No se extrajo CSS del tema. | “¿Debemos replicar el CSS exacto del tema GeneXus usado en runtime (lookup en KB/temas) o aplicar `docs/design.md` (tokens Material)? ¿Existen overrides CSS críticos?” | Alto (look&feel). Decisión recomendada: usar design.md + ajustar mínimos. |
| SA-03 | Ancho/visibilidad columnas Productos nivel 1 | Solo `simplegrid` con 5 items; sin anchos explícitos en snippet. | “¿Qué anchos/etiquetas visuales tiene la grilla de vigencias en runtime (column widths, header)? ¿ProdFchVig muestra formato DATE?” | Medio (T-505). |
| SA-04 | Etiqueta `CanRuta` (sin descripción) | Structure: `CanRuta CHARACTER(100)` sin `// "..."`. | “¿La etiqueta visible de `CanRuta` en el form de `CAMINOS` es el nombre lógico (`CanRuta`) u otra? ¿ContextualTitle definido?” | Bajo. |
| SA-05 | `ProvCod` en Localidades form (selector) | FK lógica. XML no revela combo/prompt; campo atributo. | “¿`ProvCod` en form `Localidades` es prompt/lookup a `Provincia` (selector) o entrada libre? ¿Existe prompt asociado?” | Medio (T-504). |
| SA-06 | Semántica `btn_get` en Feriados | Presente junto a `FerFecha` (`Event="Get"`). | “¿Qué hace `btn_get` (Feriados)? ¿Lookup por fecha / carga registro / validación?” | Medio (T-505). |
| SA-07 | `ListadoLocalidades` selection asset | `&LinkSelection BITMAP(4)`, `FromImage(selectRow)`. | “¿Qué imagen/ícono usa `selectRow` (BITMAP(4))? ¿Necesario replicarlo o usar icono Material (`check`/`open_in_new`)?” | Bajo. |
| SA-08 | `att:244` mapping en grid ListadoLocalidades | Grid columna 4: `att:244`, título constante `"Provincia"`. Variables/att visibles: no incluye `ProvNombre` global explícito. | “¿Qué atributo lógico corresponde a `att:244` en `ListadoLocalidades.webForm` (¿`ProvNombre` de `Provincia` vía asociación, o `LocNomProv`)? ¿Por qué título constante ‘Provincia’?” | **CERRADO (T-503)** — corresponde a `ProvNombre` de la provincia asociada; la columna se alimenta de `LocalidadDto.provNombre` (no de `LocNomProv`). Ver §4.9. |
| SA-09 | Formato DATE/DATETIME runtime | `DATE(8)` y `DATETIME(8,5)`. | “¿Formato de fecha mostrado (dd/mm/yyyy)? ¿`FerFchReg`/`ProdFchVig` mostrados como fecha u hora?” | Medio (validación UI). |
| SA-10 | `Feriados` `btn_check`/`btn_help` comportamiento | Presentes en raw form. | “¿Acciones reales de `btn_check` y `btn_help` en `Feriados` (eventos/llamadas)? ¿Requeridas funcionalmente?” | Bajo (replicar intención). |
| SA-11 | `wwpMetadata.isWorkWithPlusAware` claim previo | No encontrado en lecturas actuales. | “¿Existe evidencia de WorkWithPlus en estos objetos (metadata UX)? Si no existe, confirmar que la UI generada es la única referencia.” | Bajo. |

## 8. (e) Limitaciones de extracción + desviación de path

**Limitaciones:**
- Solo lectura KB GeneXus 18 via MCP. No se compiló/regeneró KB.
- WebForm XML contiene layout/controles; **CSS/JS tema GeneXus no extraído** (ver SA-02).
- Assets (imágenes BITMAP) no leídos vía `asset_read` a menos que necesario; `&LinkSelection BITMAP(4)` identificado pero contenido no obtenido (SA-07).
- Algunos detalles de runtime (mensajes GXSPC_*, formatos, prompts asociados) requieren lectura adicional o inspección en runtime (no disponible).
- Intentos puntuales de leer `ListadoLocalidades.structure` devolvieron error interno; inferido desde webForm/variables/conditions. El `att:244` quedó resuelto en T-503 adoptando `provNombre` del DTO backend (**SA-08 cerrado**); ver §4.9.
- No se accedió a BD `Rentas` para contrastar labels mostrados vs datos; referencia basada en KB.

**Desvío de path (obligatorio reportar):**
- T-502 original indica: “documentar la referencia visual en `docs/`”.  
- **Destino real:** `openspec/features/01-nucleo-datos/ui-reference.md`  
- **Motivo:** (1) restricción de escritura “Write permission limited to `openspec/features/**`” (establecida en instrucciones del sistema/arnés); (2) coherencia con ubicación de `spec.md`, `plan.md`, `tasks.md` del módulo; (3) mantiene trazabilidad en mismo feature. **No** se modificó `docs/` (solo lectura). Este archivo es el entregable de T-502.

## 9. Notas de gaps (ABMs no existentes + derivación)

**ABMs no existentes como WebPanel:** No hay WebPanels con nombres `Provincia`, `Localidades`, `Calles`, `CAMINOS`, `Areas`, `Jurisdic`, `Feriados`, `Productos`. Los ABMs son las **transacciones mismas** (Transaction.webForm) con patrón GxMultiForm (7) + Feriados raw HTML (1). Por tanto:
- Para T-504/T-505: **replicar patrón visual del Transaction.webForm** (layout, botones, navegación, etiquetas). No existe un WebPanel hand-crafted a copiar; el origen es el form generado.
- `ListadoLocalidades` **sí** existe como WebPanel → T-503 replica ese WebPanel.webForm exactamente (filtros+grid+acciones).

**Derivación fuente para Angular 22:**
- **Simple maestras (GxMultiForm):** tomar estructura (structure) + webForm + títulos/etiquetas (ContextualTitle) → generar formularios Reactivos con MatFormField; mantener NavButtons/FormButtons equivalentes.
- **Feriados (raw HTML):** replicar layout Table1/Table2, botón `btn_get` junto a fecha, botonera inferior (incluir `btn_check`, `btn_help`) — acciones a mapear según SA-06/SA-10.
- **Productos (2 niveles):** generar form nivel 0 + `MatTable` editable para `GridProductos_Level1Item` (columnas: ProdFchVig, ProdImpu1..4). Mostrar `ProdIMax1..4` readonly (derivados). Backend ya calcula (R-005/R-035).
- **ListadoLocalidades:** generar componente standalone con Reactive Forms para filtros + `MatTable` paginado (pageSize configurable; respetar intención `MaxRows=5`), selección por fila, retorno a llamador. Columnas: Selección, Cod. Loc., Nombre, Provincia (att:244), Cod. Postal; ocultar LocNomProv.

**Riesgos:** SA-01–SA-11 deben cerrarse antes de implementar UI intensiva (especialmente SA-02 CSS tema vs design.md). `SA-08` ya está cerrado (T-503, ver §4.9). No se asume WorkWithPlus. Se prioriza fidelidad funcional (botoneras, navegación, campos, grid) sobre pixel-perfect CSS hasta resolver SA-02.