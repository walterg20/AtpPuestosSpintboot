# Anexo - 08 Control de Cargas (trazabilidad, supuestos, decisiones)

> Companion de spec.md.

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB) | Requisito nuevo | Prueba |
|---|---|---|
| `WebPanel ControlDeCargas` (`Events`: Start, 'Buscar', Grid506.Load, 'Marcar', 'VerDetalle', 'Ver Tabla', 'Pagos') | R-CC-01…R-CC-15 | Tests de vistas + IT |
| `substr(&BarCode,22,10)` | R-CC-04 | Test de extracción |
| `&Context.Sucursal = 0` ⇒ `Usuario sin Puesto Asignado` | R-CC-02 | Test de validación de puesto |
| `confirm('Esta seguro que desea Marcar el formulario?')` | R-CC-03 | Prueba de navegador |
| `MarcarGuia.Udp(...)` | R-CC-07…R-CC-11 | IT + tests unitarios |
| `Form506Detalle.Popup(..., 'DSP'/'UPD')` | R-CC-12, R-CC-13 | Test de popup |
| `WWTDetProdPri.Call()` | R-CC-14 | Test de navegación |
| `WpFormularioPago.Call()` | R-CC-15 | Test de navegación |
| `Grid506.Load` ⇒ `&Marcar.FromImage(selectRow)` | R-CC-06 | Test de render |
| Grid506 (7 columnas visibles) | R-CC-20 | Test de vista (7 columnas) |
| `Total General` = `DPPTotGral` (fórmula) | R-CC-21 | Test unitario de suma |
| PK compuesta de `DETPRODP` (5 partes) enviada en Marcar | R-CC-22 | Test de vista (5 hidden inputs) |
| Flag dedicado `mostrarModal` en `CargaForm` | R-CC-23 | Test Buscar vs /detalle |
| Tokens de cargas en `tokens.css` | R-CC-24 | `grep` sin literales |
| Modal con control de cierre | R-CC-19 | Test de cierre |
| SDT `Context` (13 campos, 10 cargados) | R-CC-02 | Test de contexto |
| `03-login` (ContextoSesion, ServicioAcceso, GestorContextoSesion) | R-CC-01, R-CC-02 | Tests de integración |
| `00-alcance` R-ALC-002 (sin DDL), R-ALC-006 (render por servidor) | R-CC-16, R-CC-19 | Revisión de diff |

### 5.2 Columnas huérfanas (BD vs KB)

#### `CONTRIBU` (columnas en BD no en KB)

`AgeUltLin`, `ConBen7190`, `ConBenPrCh`, `ConCatAfipId`, `ConCNFdo`, `ConCtrlFis`,
`ConDDJJAnu`, `ConInsPJur`, `ConLibRPC`, `ConMCMAnt`, `ConNomFan`, `ConNroCoMu`,
`CodPosFis`, `CodPosReal`, `ConFJRPC`, `ConNroInmo` (KB: `ConNroInmobR`), `ConNroDCat`
(KB: `ConNroDCatas`), `ConSellTip` (KB: `ConSellTipo`), `ConNomCall`.

#### `DETPRODP` (columnas en BD no en KB)

`DPPLocOr`, `DPPLocDe`, `DPPIncluid`, `DPPCorGre`, `DPPAnuObs`, `DPPActaNro`,
`DPPActaPue`, `PPTrataId`, `DPPMEIBrut` (KB: `DPPMEIBrutos`).

#### `DETPROD2`

Sin columnas huérfanas adicionales (la BD coincide con la KB).

> Las columnas huérfanas se **mapean** con `@Column(name="...")` y se dejan con su default de
> aplicación (típicamente `null` o el valor que exija la columna NOT NULL). No se rediseñan.

### 5.3 Matriz de paridad funcional

| Operación legacy | Requisito | Prueba prevista |
|---|---|---|
| Validación de puesto al entrar | R-CC-02 | `MvcValidacionPuesto` |
| Búsqueda por barcode | R-CC-04, R-CC-05 | `MvcBusquedaBarcode` |
| Refresco de grilla | R-CC-05 | `MvcRefrescoGrilla` |
| Icono de acción en grilla (check verde 21x21) | R-CC-06 | `MvcIconoAccion` |
| Paridad de grilla Figma (7 columnas) | R-CC-20 | `MvcParidadGrilla` |
| Total General calculado en service | R-CC-21 | `CargaServiceTest#totalGeneral` |
| PK completa en Marcar | R-CC-22 | `MvcMarcarPkCompleta` |
| Modal con flag dedicado | R-CC-23 | `MvcModalFlag` |
| Tokens sin literales | R-CC-24 | `CssTokensCargas` |
| Modal con control de cierre | R-CC-19 | `MvcModalCierre` |
| Confirmación de marcado | R-CC-03 | Prueba de navegador |
| Marcado exitoso | R-CC-07 | IT `Rentas` + `MvcMarcado` |
| Guía anulada | R-CC-08 | `MvcGuiaAnulada` |
| Guía interna | R-CC-09 | `MvcGuiaInterna` |
| Guía duplicada | R-CC-10, R-CC-11 | IT + test unitario |
| Ver detalle (DSP) | R-CC-12 | `MvcVerDetalle` |
| Ver detalle tras marcar (UPD) | R-CC-13 | `MvcVerDetalleUPD` |
| Ver tabla | R-CC-14 | `MvcVerTabla` |
| Pagos | R-CC-15 | `MvcPagos` |

## 6. Supuestos abiertos

| ID | Pregunta | Decisión provisional | Riesgo si se resuelve distinto |
|---|---|---|---|
| **SA-CC-01** | ¿La ruta de página debe ser `/cargas` o `/control-cargas`? La convención del repositorio es `/<recurso>` (sin prefijo). | `/cargas` | Bajo |
| **SA-CC-02** | ¿El popup `Form506Detalle` debe implementarse como modal Thymeleaf o como página separada con parámetros en la URL? | Modal Thymeleaf (fragmento) | Medio |
| **SA-CC-03** | ¿`WWTDetProdPri` y `WPFormularioPago` deben reimplementarse en este módulo o sólo referenciarse como navegación? | Sólo navegación (las pantallas se implementan en sus propios módulos) | Medio |
| **SA-CC-04** | ¿El campo `&BarCode` (31 caracteres) debe validarse en longitud antes de extraer? | No validar (paridad con legacy) | Bajo |
| **SA-CC-05** | ¿La columna `DPPPaseCo` (comisión) debe enviarse como `null` cuando no hay comisión o como `0`? | Enviar `&Context.Comision` (que puede ser 0) | Bajo |
| **SA-CC-06** | ¿Los mensajes de `Form506Detalle` deben mostrarse en el mismo popup o en una vista separada? | En el mismo popup (modal) | Bajo |
| **SA-CC-07** | El lienzo Figma `2024:371 "Consulta de cargas"` cubre sólo la pantalla de consulta; el modal `Form506Detalle` se deriva del WebPanel legacy `Form506Detalle` (popup). | Modal Thymeleaf (fragmento) (D-CC-04) | Medio |

## 7. Decisiones

| # | Decisión | Justificación |
|---|---|---|
| D-CC-01 | La pantalla se implementa en `atp-puestos-camineros` (no en el núcleo) | Es dominio de Puestos Camineros, no catálogo compartido |
| D-CC-02 | `MarcarGuia` se implementa como servicio (`CargaService.marcarGuia`) | La lógica de negocio va en `service/**` (AGENTS.md regla 9) |
| D-CC-03 | La grilla se refresca por submit del formulario (no por AJAX) | Thymeleaf renderiza por servidor; JS puntual sólo para barcode y confirmación |
| D-CC-04 | El popup `Form506Detalle` se implementa como fragmento Thymeleaf modal | Evita duplicar la lógica de negocio en un controller separado |
| D-CC-05 | Las columnas huérfanas se mapean con `@Column(name="...")` y default null/0 | Paridad de BD (G-DATA), sin rediseño (G5) |
| D-CC-06 | Los mensajes verbatim se conservan con sus erratas | Paridad de identidad (docs/design.md §1.1) |
| D-CC-10 | El modal se abre desde 2 columnas ("Nro Guia" canónico + "Cuit" por pedido del usuario) | Satisfacer pedido del usuario sin perder paridad Figma/legacy (Engram #198) |
| D-CC-11 | Buscar sólo refresca la grilla; el modo detalle se controla con flag dedicado `mostrarModal` | Evitar inferencia desde `dppNroForm != null` (Engram #197) |
| D-CC-12 | El modal Thymeleaf incorpora botón de cierre explícito (JS puntual) | El legacy lo heredaba del popup de GeneXus (Engram #198) |
