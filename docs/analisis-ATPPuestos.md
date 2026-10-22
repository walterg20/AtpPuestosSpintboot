# F0 — Análisis e inventario de la KB GeneXus `ATPPuestos`

> Documento de diagnóstico previo a cualquier especificación (SDD).
> La KB legacy se trató en **modo lectura** (abrir/seleccionar/leer). No se compiló, generó ni modificó ningún objeto.

## 1. Entorno y accesos

| Elemento | Valor |
|---|---|
| KB (origen, solo lectura) | `F:\Modelo\Gx18\ATPPuestos` (`ATPPuestos.gxw`) |
| GeneXus | 18.0.13 (U13) — `C:\Program Files (x86)\GeneXus\GeneXus18` |
| Driver MCP | `native-sdk`, resolución `strict`, MCP `genexus-mcp` 3.9.2 |
| Ambiente activo | `Gen18` — generador **Default (Java)** — web path `Data007\web` |
| BD de metadatos KB | `GX_KB_ATPPuestos`
| BD de la aplicación | **sin credenciales almacenadas**) |
| Tamaño KB en disco | 6.738 archivos / ~513 MB |
| Índice MCP | 1.615 entradas (objetos + atributos + clases) |
| Theme CSS | `ATPEvo2.css` (3.754 líneas) + `Unanimo_chameleon` |
| Reachability BD app | TCP **alcanzable**, ping ICMP bloqueado |

Diagnóstico `genexus_doctor` → **DoctorOk** (sin advertencias). KB abierta, seleccionada y con lease propio del worker.

## 2. Inventario por tipo de objeto (1.615 entradas)

| Tipo | Cant. | Tipo | Cant. |
|---|---:|---|---:|
| Attribute | 421 | Transaction | 32 |
| ThemeClass | 274 | Module | 21 |
| Image | 192 | UserControl | 17 |
| Domain | 127 | WebComponent | 16 |
| Index | 88 | Group | 14 |
| Procedure | 77 | MasterPage | 8 |
| SDT | 76 | DesignSystem | 8 |
| WebPanel | 69 | File | 6 |
| ExternalObject | 61 | DataProvider | 6 |
| Folder | 43 | Stencil | 5 |
| Table | 40 | Category | 4 |

Otros: `ThemeForWeb` 2 (`ATPEvo2`, `GeneXusXSD`), `WorkWith` 2, `Pattern Settings` 2, `Document` 1 (`Main`), `Language` 1, `DataStoreCategory` 1, `GeneratorCategory` 1.

> **Lectura clave**: del total, el *footprint propio del negocio* es acotado (40 tablas, 32 transacciones, 77 procedimientos, 69 WebPanels, 6 DataProviders). El resto son artefactos de patrones/base GeneXus: `ThemeClass` (274), `Image` (192), `ExternalObject` (61) y buena parte de los `SDT` (76) provienen del **Design System Unanimo** y de plantillas mobile (`SD*`), no del dominio.

## 3. Modelo de datos (40 tablas / 421 atributos / 127 dominios / 88 índices)

**Hallazgo crítico de paridad**: los nombres **físicos** de tabla son heredados (muchos de 8 caracteres, era GX15) mientras que las transacciones ya usan nombres largos. El mapeo `Tabla física ↔ Transacción` no es 1:1 trivial.

```
Areas   Calles   CAMINOS   Comisio1   Comision   ConAcop1   ConAcopi   ContJur
Contribu   DetProd1   DetProd2   DetProdP   F2276   F22761   F2276Pas   Feriados
Fletes   FormSucu   FormuPag   Ges_SucN   Ges_Sucu   Jurisdic   Localida   Lotes227
Mensajes   NumLog   ORIGRET1   ORIGRETE   Personas   PPRIM_TU   Product1   Producto
ProPri_T   PROVINCI   PuePPrim   RECEPTO1   RECEPTOR   SCRTY   SNSinDev   VERSIONE
```

Transacciones (32): `Areas, Calles, CAMINOS, Comisiones, Contribuyentes, F2276, F2276_FormDef, F2276_Lotes, Feriados, Flete, FormSucursal, FormuPagos, Jurisdic, Localidades, Mensajes, ORIGRETE, Personas, PPrim_Turnos, PPrim_TurnosNue, Productos, ProPri_T, Provincia, PuePPrim, RECEPTOR, Receptorias, SCRTY, SNSinDev, TConAcopiador, TDetProdPri, TGes_Sucu, TNumLog, VERSIONE`.

## 4. Lógica de negocio (77 procedimientos)

Agrupados por prefijo funcional (el prefijo revela el subsistema):

| Grupo | # | Objetos |
|---|---:|---|
| **Formulario 2276** (`F2276*`) | 19 | Anular, BusDom, BusLoc, BusLocYProv, BusProTarCM, BusProTarifa, BusUlt, CalImpFl, CalTarFl, DevRazSocial, DiarioReceptorias, GenNroForm, RecUltNroForm, RegPasePuestoV2, Reporteimpresion, BuscarSn, GenerarSn, VolanteDePago, VolanteDePagoXTurno |
| **Núcleo / comunes** (`Nuc_*`) | 6 | CacularVencimientoXDias, CalculoImpIB, ImpALetras, ImporteALetras, Usuario, ValidaCuit |
| **Puestos / turnos** (`PPrim_*`, `ProPri_*`) | 11 | BuscarLote, BusTur, CerrarComision, CerrarTurno, EstadoDeForm, NumTur, TurnoEstado, VerificaGenMan, VerIns, VerManual, VerSoloIns |
| **Seguridad** (`Seg_*`) | 8 | BusComi, BusPer, BusPuesto, BusRece, BusResponsable, ControlAcceso, ControlAccesoWS, Encripta |
| **Comisiones / turnos** | 4 | ComisionGenerarSn, ComisionVolanteDePago, TurnoGenerarSN |
| **Búsquedas** | 5 | Bus_DesLoc, BuscarCamino, BuscarInfoPorCuitLite, BuscarOrigenRetencion, BuscarVersionActual |
| **Reportes** | 3 | RPPriTur_TurnosForm, RProPri_TurXCom, RPT_ActaTraslado |
| **Actas** | 2 | ActaDeterminaTipoFormulario, ActaResumenTurno |
| **Códigos de barra** | 3 | CodigoBarra, CodBarToPKF276, PK276ToCodBar |
| **Contexto / offline (patrón mobile)** | 3 | LoadContext, SetContext, OfflineEventReplicator |
| **Seguridad aplicativa** (`IsAuthorized`, `ListPrograms`) | 2 | — |
| **Notificaciones / devices (Unanimo-SD)** | 8 | Add/RemoveDeviceGroups, Add/RemoveDeviceTargetFilter, SendEvent(Targets), SendNotification(Targets) |
| **OAuth / otros** | 3 | RefreshToken, GoogleDirectionsServiceRequest, MarcarGuia |

**Data Providers (6)**: `DPDolphinMenuActas`, `DPDolphinMenuData`, `DPDolphinMenuDataRecepteria`, `DPMenuComisiones`, `SidebarItemsDP`, `SmoothNavMenuDataDP` → alimentan menús/navegación (no lógica de dominio).

## 5. Capa web

- **WebPanels (69)**: 26 son **generados por patrón Work With** (`Gx0010`…`Gx0140`, `Gx00A0`…). El resto es funcional: `ActaCargarDetalle`, `ActaListadoWC`, `ActaPuesto_Chofer`, `Comisiones_ViewComisiones`, `Comisiones_WWComisiones`, `ComisionesListado`, `ControlDeCargas`, `F2276_RegistroPase`, `F2276_ViewF2276`, `F2276CargarDetalle`, `F2276DetalleLite`, `F2276DetallePrompt`, `F2276ListadoControl`, `F2276Listados`, `Form506Detalle`, `Home`, `Inicio`, `ListadoLocalidades`, `Mensaje`, `NotAuthorized`, `PPActaDetalle`, `TurnosListado`, `TurnosListadoAbiertos`, `ViewPPrim_Turnos`, `ViewTDetProdPri`, `WPFormularioPago`, `WWPPrim_Turnos`, `WWTDetProdPri`.
- **WebComponents (16)**: `ActaListadoWC_turno`, `Comisiones_General`, `Comisiones_IntegrantesWC`, `F2276_F22761WC`, `F2276_General`, `F2276ListadosWCPopUp`, `F2276ListadoWC`, `Login`, `PPrim_TurnosF2276_FormDefWC`, `PPrim_TurnosF2276WC`, `PPrim_TurnosGeneral`, `RecentLinks`, `TabbedView`, `TabsMenu`, `UserInfo`, `UserInfoNuevo`.
- **MasterPages (8)**: `AppMasterPage`, `AppMasterSD`, `MasterNuevo`, `MasterPagePuente`, `MasterPrompt`, `MasterUnanimoSidebar`, `PromptMasterPage`, `RwdMasterPage`.
- **UserControls (17)**: pares nativos + Angular (`Alert`/`AlertAngular`, `Button`, `Dropdown`/`DropdownAngular`, `Icon`/`IconAngular`, `Select`/`SelectAngular`, `Sidebar`/`SidebarAngular`, `Slider`, `Step`/`StepAngular`, `Switch`, `Treeview`/`TreeviewAngular`).
- **WorkWith (2)**: `WorkWithWebPPrim_Turnos`, `WorkWithWebTDetProdPri`.

> La existencia de variantes **Angular** de los UserControls indica web UX ya preparada para Angular (`WebUX=SMOOTH`, `DefaultWebFormEditor=1`).

## 6. SDT y External Objects

- **SDT (76)**: mayoría de patrón/plataforma (`Beacon*`, `Analytics*`, `CardInformation`, `Purchase*`, `Store*`, `Media*`, `Map*`, `Geolocation*`, `Synchronization*`, `Notification*`, `OAuth*`). De dominio: `F2276DetalleSDT`, `F2276Totales`, `SDTActaPK`, `SDTDatosChoferActa`, `SDTEntradaLoginWS`, `SDTSalidaLoginWS`, `SdtF2276`, `ProgramNames`, `Context`.
- **ExternalObject (61)**: casi todos son EOs **estándar de GeneXus** para mobile/Unanimo (`Beacons`, `Camera`, `CardScanner`, `Contacts`, `Geolocation`, `Maps`, `Notifications`, `Purchases`, `Store*`, `Synchronization`, `Facebook`, `Twitter`, `WeChat`, `Socket`, `Interop`, `DynamicCall*`…). De dominio: `Server`, `DataBase`, `Log`, `ConfigurationManager`, `GlobalEvents`, `ClientInformation`.

## 7. UI / Design System

- **Theme web**: `ATPEvo2` (exportado como `ATPEvo2.css`, 3.754 líneas) — base para derivar el sistema de diseño moderno.
- **DesignSystems (8)**: `Unanimo`, `UnanimoAngular`, `UnanimoATP`, `UnanimoControls`, `UnanimoControlsATP`, `UnanimoMobile`, `UnanimoWeb`, `UnanimoWebAtp`.
- **Stencils (5)**: `Card`, `CardWithFooter`, `Header`, `OneColumnCards`, `WW_HeaderWithActions`.
- **Imágenes**: 192. **Clases de tema**: 274. **Fuentes**: Source Sans Pro (4 pesos) + `DolphinStyleMenuRender.js`.
- Navegación: menú "Dolphin" (DataProviders dedicados) + `Sidebar`/`SmoothNavMenu`.

## 8. Organización en módulos y carpetas

- **Modules (21)**: `Root Module`, `Client`, `Server`, `Common`, `Configuration`, `General`, `UI`, `UI`(dup), `v2`, `Media`, `Notifications`(dup), `OAuth`, `Offline`, `Social`, `Store`, `Synchronization`, `SD`, `GeneXus`, `GeneXusUnanimo`, `iOS`.
- **Folders (43)**: `Nucleo`, `Transacciones`, `SDTs`(dup), `Procedimientos`, `Seguridad`, `Security`, `Librerias`, `Utiles`, `Varios`, `Disenos`, `Stencils`, `DesignSystems`, `Icon`, `Controls`, `Login`, `Purchases`, `Turnos`, `Comisiones`, `ComisionesWW`, `F2276WW`, `F2776`, `Feriados`, `Formulas`, `GeneralWeb`, `GoogleDirections`, `Guias`, `Deprecated`, `ContextManagement`, `Alert`, `Dropdown`, `Select`, `Sidebar`, `Step`, `TabsMenu`, `Treeview`, `SmoothNavMenu`, `DolphinStyleMenu`, `DirectionsServices`, `SmartDevicesApi`, `SubtypeGroups`, `Common`, `GeneXus`.

> Nombres duplicados (`UI`, `Notifications`, `SDTs`, `Common`, `GeneXus`) corresponden a nodos con distinto padre — no son duplicados lógicos.

## 9. Bases de datos

| Rol | Motor | Servidor | Base | Auth | Notas |
|---|---|---|---|---|---|
| Metadatos KB | SQL Server |  `GX_KB_ATPPuestos` | Integrated | `.mdf/.ldf` dentro de la carpeta KB |
| Aplicación (runtime) | SQL Server | `devmssqlatp.ecom.com.ar` (10.5.2.180) | **`Rentas`** | Driver (sin credenciales en KB/model.ini) | TCP 1433 alcanzable; no hay BD local `Rentas` |
| Ambiente alterno | Oracle | (referenciado, sin `CS_*` en model.ini) | — | — | Modelo `Design`/`006`, aparentemente inactivo |

En `model.ini`: `[MODEL 007] DataStore=Default (SQL Server)`,`CS_USETOCONNECT=Driver` (pide credenciales al conectar; **no** hay usuario/clave persistidos → nada que filtrar al repo).

## 10. Observaciones y riesgos

1. **Acceso a la BD real resuelto (G-DATA 2026-10-16)**: `Rentas` es remota (`devmssqlatp.ecom.com.ar`); con credenciales provistas en `.env` se validó el esquema físico (ver §13). El esquema de paridad ya **no** se deriva solo de la metadata de la KB: está contrastado contra `INFORMATION_SCHEMA`.
2. **Nombres físicos legacy** (8 chars) vs. nombres lógicos actuales → riesgo confirmado y acotado (ver §13): tablas truncadas a **8** y columnas a **10** caracteres (`FerDescripcion → FerDescrip`); requiere tabla de correspondencia explícita.
3. **Contaminación de metadatos**: EO/SDT/Tema/Imágenes de Unanimo y plantillas mobile inflan el conteo; el alcance real de negocio es sensiblemente menor → conviene **definir el alcance migrable** antes de especificar.
4. **Generador objetivo ya es Java** (`Default (Java)`) en el ambiente `Gen18` — alineado con el target Spring Boot.
5. **Doble KB con el mismo nombre** (`GX_KB_ATPPuestos` en la instancia de GX18 y en la de GX15) → usar siempre para GX18.
6. **Seguridad**: la KB referencia `SCRTY`, `Seg_ControlAccesoWS`, login WS (SDTs de entrada/salida) y GAM (`Security`). Hay que caracterizar el modelo de autenticación/autorización antes de reimplementar (F2).
7. **Regla de oro respetada**: lectura exclusiva; abrir/seleccionar y consultar no altera la KB.

## 11. Alcance tentativo del replatform (a validar)

> **Actualizado 2026-10-22** (F1): el destino de la capa de presentación cambió de Angular a
> **Thymeleaf** (decisión del equipo). El resto del alcance no cambia. El alcance definitivo,
> con lo que queda **fuera** de este repositorio y su mapeo, está en
> `openspec/features/00-alcance/spec.md`.

Candidato a migrar (backend Java + vistas Thymeleaf):
- 40 tablas / 32 transacciones (dominio: personas, contribuyentes, formularios, comisiones, turnos, receptorías, feriados, jurisdicciones, localidades).
- ~50 procedimientos de negocio (excluyendo los 19 de notificaciones/device y los de patrón mobile).
- Servicios web de login/consulta (`SDTEntrada/SalidaLoginWS`, `Seg_ControlAccesoWS`).
- WebPanels funcionales + WebComponents de Form2276 / Comisiones / Actas / Turnos.
- Derivar sistema de diseño desde `ATPEvo2.css` + DesignSystems Unanimo.

**Fuera de alcance probable** (regenerar/descartar, no reimplementar): EOs estándar de plataforma, SDTs de patrón mobile, carpetas `Deprecated`, WebPanels generados `Gx00xx`, `iOS`/`Social`/`Store`/`Purchases`/`Media`.

**Fuera de este repositorio** (pertenece a otra aplicación del parque, ver §14): Portal Gestión
ATP, Gestión de Agentes, Planes de Pago y Portal Contribuyente.

## 12. Próximos pasos

- **F1**: monorepo Maven de 2 módulos (`atp-nucleo-compartido` + `atp-puestos-camineros`),
  `openspec/{config.yaml,constitution/}`, `docs/`, `.env.example`, `.gitignore`, `git init`,
  arnés de subagentes reescrito para Thymeleaf. ✅ (2026-10-22)
- **F1.1**: alcance migrable in/out con el mapeo del parque de aplicaciones. ✅
  `openspec/features/00-alcance/`
- **F2**: ciclo SDD — spec `02-abm-productos` (ABM de productos en Thymeleaf).
- **F3**: ciclo SDD — spec `03-login` (login completo de Puestos Camineros).

### Cómo regenerar estos datos (determinista)

- Inventario: `genexus_list_objects` (MCP) paginado con `projection=minimal`, `sort=name`.
- Esquema por transacción: `genexus_analyze mode=context target=<Transacción>`.
- Fuente: `genexus_read`; búsqueda cruzada: `genexus_search_source`.
- Impacto/dependencias: `genexus_analyze mode=impact|dependencies target=<objeto>`.

## 13. G-DATA — Esquema físico validado contra `Rentas` (2026-10-16)

> **Puerta G-DATA** del módulo piloto `01-nucleo-datos`. Se conectó a `Rentas` con las credenciales de
> `.env` (`sqlcmd`) y se contrastó la **metadata lógica de la KB** contra el **esquema físico real**
> (`INFORMATION_SCHEMA.COLUMNS` / `KEY_COLUMN_USAGE` / `REFERENTIAL_CONSTRAINTS`). Fuente de autoridad
> para el mapeo JPA: `spec.md` §4.5 (esquema físico), `plan.md` §2.2/§2.3.

### 13.1 Supuestos (ASM-01…05) — veredicto

| ASM | Supuesto original | Veredicto | Detalle |
|---|---|---|---|
| ASM-01 | El nombre físico de columna = nombre lógico del atributo. | **Matizado** | Se cumple salvo **truncado a 10 caracteres**: `FerDescripcion → FerDescrip`. El resto coincide. |
| ASM-02 | `NUMERIC(n,0)` mapea a `smallint`/`int`/`bigint`. | **Parcialmente falso** | `Areas.AreaId` es `decimal(10,0)` → `BigDecimal`. El resto sí son `smallint`/`int`. |
| ASM-03 | `DATE(8)` mapea a `date`. | **Falso** | `Feriados.FerFecha` y `Product1.ProdFchVig` son **`datetime`** → `LocalDateTime`. |
| ASM-04 | `CHARACTER(n)` mapea a `varchar(n)`. | **Falso** | Es **`char(n)` fijo** (rellena con espacios → `trim` al leer). Excepciones `varchar`: `ProvNombre(40)`, `AreaEmails(255)`, `AreaRespons(100)`. |
| ASM-05 | `DATETIME(8,5)` mapea a `datetime2`/`datetime`. | **Confirmado** | `Feriados.FerFchReg` es `datetime`. |

### 13.2 Nombres físicos de tabla (truncado a 8 chars)

`Provincia → PROVINCI` · `Localidades → Localida` · `Calles → CALLES` · `CAMINOS → CAMINOS` ·
`Areas → Areas` · `Jurisdic → Jurisdic` · `Feriados → FERIADOS` · `Productos(0) → PRODUCTO` ·
`Productos(1) → PRODUCT1`.

### 13.3 Hallazgos no anticipados

- **Columnas huérfanas** (existen en `Rentas`, **no** en la KB): `Areas.AreaBaja/AreaEmails/AreaAdjun/AreaRespons`;
  `CALLES.CallAlt1/2/3`; `Localida.LocCodAfip`; `PROVINCI.ProvCodAfip`; `PRODUCTO.ProdReppa`;
  `PRODUCT1.ProdImpu5/ProdFeLog/ProdUsu`. → El mapeo debe cubrirlas (R-036).
- **`NOT NULL` huérfanas** (obligatorias en el alta, sin contraparte en el DTO): `AreaEmails`, `LocCodAfip`,
  `ProvCodAfip` → default de aplicación `""`/`0`/`0`.
- **`NOT NULL` adicionales**: `PRODUCT1.ProdImpu1/ProdImpu2`; `PRODUCTO.ProdDesc/ProdTipCal/TPPriID/ProdAnula`;
  `FERIADOS.FerDescrip/FerFchReg/FerUsuario`.
- **Sin `COLUMN_DEFAULT`** en ninguna columna y **sin `IDENTITY`** en las 9 tablas → la **PK viaja en el body**
  del `POST` (R-037); duplicado → `409`.
- **Sin FK física** `Localida → PROVINCI` (asociación solo lógica). FKs físicas hacia el piloto: únicamente
  `Rastrillo → CALLES` y `VElectronica → AREAS`.
- **No existe tabla de unidad de medida**: `UMedNombre` y `ProdIMax1..4` son **derivados/no persistidos**.

### 13.4 Reglas nuevas derivadas

- **R-036**: persistir todas las columnas legacy/huérfanas aplicando defaults `0`/`0`/`""` en el alta.
- **R-037**: la PK se recibe en el `POST` (no autoincremental).

### Cómo regenerar G-DATA (determinista)

- Columnas + tipos: `SELECT ... FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN (...)`.
- PK/FK/índices: `INFORMATION_SCHEMA.KEY_COLUMN_USAGE` + `REFERENTIAL_CONSTRAINTS`.
- `NOT NULL`/defaults: `INFORMATION_SCHEMA.COLUMNS` (`IS_NULLABLE`, `COLUMN_DEFAULT`).
- Credenciales: cargar `.env` (`DB_*`) → `sqlcmd -S "$DB_HOST,$DB_PORT" -d $DB_NAME -U $DB_USER -b -W -s '|'`.

## 14. Mapa del parque de aplicaciones (2026-10-22)

> Extensión del diagnóstico: el relevamiento del entorno mostró que **`ATPPuestos` es una de
> varias aplicaciones**, no "la" aplicación. Esta sección es la fuente de verdad del mapa
> legacy → destino previsto, y el insumo de `openspec/features/00-alcance/`.

### 14.1 KB del entorno

| Ruta | KB | Contenido | Datastores declarados |
|---|---|---|---|
| `F:\Modelo\Gx18\ATPPuestos` | ATPPuestos | **Puestos Camineros** — 1.615 objetos | `Default` |
| `F:\Modelo\Gx18\ATPWebGX18` | ATPWebGX18 | **Gestión ATP (web)** — 10.867 objetos | `Default`, `Domiciliario`, `MarcaYSenial` |
| `F:\Modelo\Gx18\ATPGestion` | ATPGestion | **Gestión ATP (escritorio)** — 4.500 objetos | `Default`, `RentasTDA`, `ATPDomiciliarioWeb`, `RENTASTestigo`, `RENTASImportacion`, `DataStore2` |
| `F:\Modelo\Gx18\ATPWebGX18` (carpetas) | — | `ABM_8_Agentes` · `ABM_11_13_14_Domicilio` · `Contribuyentes` · `Contrib` · `Contribuyente_Transitorio` · `BloquesMenu` · `ABMWeb` | — |
| `F:\Modelo\Gx15\*` | 6 KB | `ATPDomiciliarios`, `ATPGestion`, `ATPPuestos`, `ATPWeb`, `ATPWebIA`, `ATPWebModulos` | — |

> **Gx15 no es analizable con la herramienta vigente**: el servidor MCP `genexus18mcp` soporta
> `major` 16/17/18. Para el alcance en Gx15 hace falta otro canal (exportación `.xpz` / `.xml`).

### 14.2 Bases de datos en `devmssqlatp.ecom.com.ar`

`RENTAS` · `RentasTDA` · `RENTASTestigo` · `RENTASImportacion` · `ATPDomiciliarioWeb` ·
`ATPPadronWeb` · `ATPDevelopment` · `ATPWiki` · `RENTASWiki`

> Las 3 KB de Gx18 **comparten la misma instancia de SQL Server**: un destino de 2 portales
> desplegados sobre este servidor no requiere infraestructura nueva.

### 14.3 Correspondencia con el destino previsto

| Aplicación / KB | KB real | Base | Destino | Decisión |
|---|---|---|---|---|
| Puestos Camineros | `Gx18\ATPPuestos` | `RENTAS` | Módulo del Portal | **IN** en este repo (spec `01`, `02`) |
| Gestión de Agentes | `ATPWebGX18/ABM_8_Agentes` | `Default` | Módulo del Portal | OUT |
| Gestión ATP | `Gx18\ATPWebGX18` + `Gx18\ATPGestion` | varias | Portal Gestión ATP + módulos internos | OUT |
| Planes de Pago | `ATPWebGX18/ABM_11_13_14_Domicilio` | `ATPDomiciliarioWeb` | Módulo de negocio para uno o ambos portales | OUT (descartado 2026-10-22) |
| Contribuyente | `ATPWebGX18/Contribuyentes` | `ATPPadronWeb` | Portal Contribuyente | OUT |

### 14.4 Hallazgos estructurales

1. **`ATPPadronWeb` ya es un proxy thin**: 18 tablas, *todas* de proceso (`Proc*`,
   `AuditProcesos`, `Progress`, `ParamtrosGenerales`, `TiposArchivos`, `TiposCese`), sin datos de
   negocio. El Portal Contribuyente es naturalmente un portal aparte, no un módulo.
2. **`ATPDomicILIARIO` es un módulo de planes de pago con base propia**: `PlanActuacion`,
   9 tablas `PlanParam*`, `Solicitud`/`SolicitudEstado`/`SolicitudExcepcion`,
   `OrigenDeuda`/`OrigenDeudaPlan*`, `ClientesProveedores`.
3. **Los catálogos están DUPLICADOS entre bases**: `Provincias` existe en `RENTAS` **y** en
   `ATPDomicILIARIO`. El legacy resolvió el "núcleo compartido" **duplicando datos**, no
   compartiendo código. Con la regla "sin rediseño inicial de BD", el núcleo compartido de este
   replatform es **código** (biblioteca Maven), no datos.
4. **Consecuencia de riesgo**: si "Planes de Pago" se implementara como módulo compilado en dos
   portales, habría **dos deployables escribiendo las mismas tablas** de `ATPDomicILIARIO`, sin
   transacción cruzada ni caché compartida. Queda fuera de alcance; cuando se retome, es la
   decisión a tomar (módulo compartido vs. servicio).
5. **GeneXus 15 tiene 6 KB** cuyo contenido no pudo contrastarse contra Gx18.

## 15. G-DATA — Esquema físico del módulo de seguridad (2026-10-22)

> Insumo de la puerta **G-DATA** del módulo `03-login`. Validado contra `INFORMATION_SCHEMA`
> de la BD `Rentas`.

### 15.1 Tablas

| Legacy | Tabla física | PK | Columnas relevantes |
|---|---|---|---|
| Usuarios (`SCRTY`) | `SCRTY` | `UsuId` `char(10)` NOT NULL | `UsuNiv` `smallint`, `UsuClave` `char(10)`, `UsuGrupo` `char(5)`, `UsuNomYApe` `char(35)`, `UsuTipDoc` `smallint`, `UsuNroDoc` `int`, `UsuNotiAre` `char(1)`, `UsuArea` `decimal(10,0)`, `UsuEMail` `char(40)`, `UsuIngreso` `smallint` |
| Personas | `PERSONAS` | `PerId` `int` NOT NULL | `PerNomyApe` `char(40)`, `AreaId` `decimal(10,0)`, `PerCodUsu` `char(10)` |
| Puestos | `PUEPPRIM` | `PueCod` `smallint` | `PueDesc` `char(20)`, `PueIPRed` `varchar(40)` |
| Receptoría | `RECEPTOR` | `ReceId` `smallint` | `ReceDsc` `char(30)`, `AreaId` `decimal(10,0)`, `ReceNLiq` `int` |
| Comisión | `COMISION` | (`PueCod`, `CPCId`) | `CPCCierre` `char(1)`, `CPCResId` `int`, `CPCEstado` `char(1)`, `CPCUsuLog` `char(10)` |
| Turnos PPrim | `PPRIM_TU` | (`SucNroId`, `TurNroId`) | — |

> **Truncados**: `UsuNotiArea` (KB) → `UsuNotiAre` (físico, 10 chars).
> **Puestos no es `PUESTOS`**: el maestro es `PUEPPRIM` (`PUESTOSP` es otra tabla: `Anio`/`Mes`/
> `PueCod`/totales).

### 15.2 Columnas huérfanas de `SCRTY`

En la BD, ausentes de la KB: `UsuLegajo` `int`, `UsuFoto` `varbinary(max)`,
`UsuIdComarb` `varchar(15)`, `UsuAppFchAdhesion` `datetime`, `UsuHash` `varchar(256)`.

### 15.3 Cobertura de credenciales (552 filas)

| Métrica | Valor |
|---|---:|
| Total usuarios | 552 |
| Con `UsuClave` no vacía | 550 |
| Con `UsuHash` no vacío | 26 |
| **Sólo con `UsuHash`** (sin `UsuClave`) | **2** |
| Sin ninguno de los dos | 0 |
| Con `UsuIngreso = 1` (deben cambiar la clave) | 10 |

`UsuHash` usa el formato `Pbkdf2-SHA256$   10000$<salt base64 44>$<hash base64 44>`
(**3 espacios literales** en el separador — artefacto del formato del sistema integrado).
`UsuId` **está cifrado** con el mismo algoritmo: por eso `Seg_ControlAcceso` compara
`UsuId.ToUpper() = Seg_Encripta(&Usuario)`.

### 15.4 Grupos de usuario presentes en `SCRTY.UsuGrupo`

`CONSU` · `PUECA` · `RECEP` · `ADMIN` · `REGES` · `AGRET` · `COMUN` · `MORAT` · `DESPA` ·
`RECA1` · `RECA2` · `ECOM` · `FISE` · `INTI` · `INMOB` · `ABOGA` · `CONCF` · `MULTA` ·
`GRABA` · `IMPUT` · `CONLD` · `SELLO` · `CLONL` · (vacío)
Niveles (`UsuNiv`): 0, 2, 5, 6, 7, 8, 9, 10, 11.

### 15.5 Algoritmo de verificación de credenciales (`Seg_Encripta`)

Sustitución monoalfabética **reversible** sobre 10 caracteres, con alfabeto de 66 y mapeo espejo
(`destino = 67 - origen`), normalizando la entrada a mayúsculas. **No es cifrado**: equivale a
texto plano. Los 2 usuarios con `UsuClave` vacía quedan sin acceso por este mecanismo; ambos
tienen `UsuIngreso = 1`, con lo que el legacy ya los deriva al mensaje *"Debe cambiar su clave
desde el Sistema Integrado (win)"*. El desvío es, por lo tanto, coherente con el comportamiento
legacy (error 5).

---

_Generado en F0 — 2026-10-15. Fuente: KB GeneXus 18 `ATPPuestos` en modo lectura._
_Ampliado en Fase D (G-DATA) — 2026-10-16: esquema físico validado contra la BD `Rentas` (ver §13)._
_Ampliado en F1 — 2026-10-22: §14 mapa del parque de aplicaciones y §15 esquema de seguridad._
