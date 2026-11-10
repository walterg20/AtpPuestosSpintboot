# Plan — 08 Control de Cargas (Puestos Camineros)

> **Estado**: `APROBADA (revisión v2, 2026-11-03)` — puerta **G-SPEC** cumplida.
> Documento de arquitectura. No contiene tareas (ver `tasks.md`).
> **Fuente**: KB `ATPPuestos` (solo lectura) + `Rentas` INFORMATION_SCHEMA (G-DATA 2026-10-30).

---

## 1. Estructura del módulo

```
atp-puestos-camineros/
└── src/main/java/ar/com/atp/puestos/puestos/
    ├── domain/
    │   ├── Contribuyente.java          (@Table CONTRIBU)
    │   ├── DetalleProductoPrimario.java (@Table DETPRODP)
    │   └── PaseGuia.java               (@Table DETPROD2)
    ├── repository/
    │   ├── ContribuyenteRepository.java
    │   ├── DetalleProductoPrimarioRepository.java
    │   └── PaseGuiaRepository.java
    ├── service/
    │   └── CargaService.java
    └── web/
        ├── rest/
        │   └── CargaRestController.java   (si se agregan endpoints)
        └── mvc/
            ├── CargaMvcController.java
            └── form/
                └── CargaForm.java
templates/
└── puestos/
    └── control-cargas.html
    └── fragments/
        └── form506-detalle.html
static/
└── js/
    └── control-cargas.js
```

## 2. Paridad JPA — tablas y columnas

### 2.1 Entidad `Contribuyente` (`@Table(name="CONTRIBU")`)

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Nota |
|---|---|---|---|---|---|
| `conCuit` (PK) | `ConCuit` | `BigDecimal` | NO | 11,0 | PK |
| `conNumIns` | `ConNumIns` | `Integer` | SÍ | 10 | |
| `conEstado` | `ConEstado` | `Short` | SÍ | 5 | |
| `conTipDgi` | `ConTipDgi` | `Short` | SÍ | 5 | |
| `conTipDoc` | `ConTipDoc` | `Short` | SÍ | 5 | |
| `conNroDoc` | `ConNroDoc` | `Integer` | SÍ | 10 | |
| `conRazSoc` | `ConRazSoc` | `String` | SÍ | 35 | `char(35)` ⇒ trim |
| `callCodf` | `CallCodf` | `Integer` | SÍ | 10 | |
| `locCodf` | `LocCodf` | `Integer` | SÍ | 10 | |
| `conCProvF` | `ConCProvF` | `String` | SÍ | 1 | |
| `conDptoF` | `ConDptoF` | `String` | SÍ | 3 | |
| `conEPosF` | `ConEPosF` | `String` | SÍ | 6 | |
| `conNMzanaF` | `ConNMzanaF` | `Integer` | SÍ | 10 | |
| `conPisoF` | `ConPisoF` | `String` | SÍ | 2 | |
| `conTBarrF` | `ConTBarrF` | `String` | SÍ | 15 | |
| `conTelf` | `ConTelf` | `String` | SÍ | 15 | |
| `callCodr` | `CallCodr` | `Integer` | SÍ | 10 | |
| `locCodR` | `LocCodR` | `Integer` | SÍ | 10 | |
| `conCProvR` | `ConCProvR` | `String` | SÍ | 15 | |
| `conDptoR` | `ConDptoR` | `String` | SÍ | 3 | |
| `conEPosR` | `ConEPosR` | `String` | SÍ | 6 | |
| `conNMzanaR` | `ConNMzanaR` | `Integer` | SÍ | 10 | |
| `conPisoR` | `ConPisoR` | `String` | SÍ | 2 | |
| `conTBarrR` | `ConTBarrR` | `String` | SÍ | 15 | |
| `conTelfR` | `ConTelfR` | `String` | SÍ | 15 | |
| `conTriIB` | `ConTriIB` | `String` | SÍ | 1 | |
| `conConvMul` | `ConConvMul` | `String` | SÍ | 1 | |
| `conFSP` | `ConFSP` | `String` | SÍ | 1 | |
| `conSello` | `ConSello` | `String` | SÍ | 1 | |
| `conInmRu` | `ConInmRu` | `String` | SÍ | 1 | |
| `conJuego` | `ConJuego` | `String` | SÍ | 1 | |
| `conAgeRet` | `ConAgeRet` | `String` | SÍ | 1 | |
| `conAgePer` | `ConAgePer` | `String` | SÍ | 1 | |
| `conDerAI` | `ConDerAI` | `String` | SÍ | 1 | |
| `conQEmpl` | `ConQEmpl` | `Integer` | SÍ | 10 | |
| `conFecIni` | `ConFecIni` | `LocalDateTime` | SÍ | — | `datetime` |
| `conCodBaja` | `ConCodBaja` | `Short` | SÍ | 5 | |
| `conNroLoc` | `ConNroLoc` | `Integer` | SÍ | 10 | |
| `conFecCS` | `ConFecCS` | `LocalDateTime` | SÍ | — | `datetime` |
| `conDurCS` | `ConDurCS` | `Short` | SÍ | 5 | |
| `conFecCE` | `ConFecCE` | `String` | SÍ | 4 | |
| `jurCod` | `JurCod` | `Short` | SÍ | 5 | |
| `conNroRPC` | `ConNroRPC` | `String` | SÍ | 15 | |
| `conLocRPC` | `ConLocRPC` | `Integer` | SÍ | 10 | |
| `conFecRPC` | `ConFecRPC` | `LocalDateTime` | SÍ | — | `datetime` |
| `conTama` | `ConTama` | `Short` | SÍ | 5 | |
| `conCatIB` | `ConCatIB` | `Short` | SÍ | 5 | |
| `conFormJUR` | `ConFormJUR` | `Short` | SÍ | 5 | |
| `conNomCall` | `ConNomCall` | `String` | SÍ | 50 | huérfana |
| `conNroInmo` | `ConNroInmo` | `Integer` | SÍ | 10 | huérfana (KB: `ConNroInmobR`) |
| `conNroDCat` | `ConNroDCat` | `Integer` | SÍ | 10 | huérfana (KB: `ConNroDCatas`) |
| `conSellTip` | `ConSellTip` | `Short` | SÍ | 5 | huérfana (KB: `ConSellTipo`) |
| `ageUltLin` | `AgeUltLin` | `Short` | SÍ | 5 | huérfana |
| `conPuesCam` | `ConPuesCam` | `String` | SÍ | 1 | huérfana |
| `conNroCoMu` | `ConNroCoMu` | `BigDecimal` | SÍ | 10,0 | huérfana |
| `conCNFdo` | `ConCNFdo` | `String` | SÍ | 1 | huérfana |
| `conCtrlFis` | `ConCtrlFis` | `String` | SÍ | 1 | huérfana |
| `conInsPJur` | `ConInsPJur` | `Integer` | SÍ | 10 | huérfana |
| `conFJRPC` | `ConFJRPC` | `String` | SÍ | 5 | huérfana |
| `conLibRPC` | `ConLibRPC` | `String` | SÍ | 5 | huérfana |
| `conMCMAnt` | `ConMCMAnt` | `String` | SÍ | 1 | huérfana |
| `codPosFis` | `CodPosFis` | `String` | SÍ | 8 | huérfana |
| `codPosReal` | `CodPosReal` | `String` | SÍ | 8 | huérfana |
| `conDDJJAnu` | `ConDDJJAnu` | `String` | SÍ | 1 | huérfana |
| `conNomFan` | `ConNomFan` | `String` | SÍ | 150 | huérfana (`varchar`) |
| `conBenPrCh` | `ConBenPrCh` | `String` | SÍ | 1 | huérfana |
| `conBen7190` | `ConBen7190` | `String` | SÍ | 1 | huérfana |
| `conCatAfipId` | `ConCatAfipId` | `Short` | SÍ | 5 | huérfana |

> **Nota**: la lista de columnas huérfanas puede no ser exhaustiva; el implementador debe
> verificar contra `INFORMATION_SCHEMA.COLUMNS` en el momento de implementar y agregar las que
> falten. Las columnas `ConNomCall`, `CallCodf`, `LocCodf`, `CallCodr`, `LocCodR` existen en la KB
> pero no en la lista de huérfanas (ya están en KB). Los campos de dirección `CallNomF`, `LocNomF`,
> `LocNomProF`, `LocCodPosF` no existen en la BD física (son atributos calculados o no persistidos).

### 2.2 Entidad `DetalleProductoPrimario` (`@Table(name="DETPRODP")`)

PK compuesta: (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm`).

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Nota |
|---|---|---|---|---|---|
| `conCuit` (PK) | `ConCuit` | `BigDecimal` | NO | 11,0 | PK |
| `dppSucu` (PK) | `DPPSucu` | `Short` | NO | 5 | PK |
| `calPerFisc` (PK) | `CalPerFisc` | `Integer` | NO | 10 | PK |
| `dppSecu` (PK) | `DPPSecu` | `Short` | NO | 5 | PK |
| `dppNroForm` (PK) | `DPPNroForm` | `String` | NO | 10 | PK, `char(10)` ⇒ trim |
| `dppTCpbte` | `DPPTCpbte` | `Short` | SÍ | 5 | |
| `dppNCpbte` | `DPPNCpbte` | `String` | SÍ | 15 | `char(15)` |
| `dppFchMov` | `DPPFchMov` | `LocalDateTime` | SÍ | — | `datetime` |
| `dppCuitDes` | `DPPCuitDes` | `BigDecimal` | SÍ | 11,0 | |
| `dppDestRS` | `DPPDestRS` | `String` | SÍ | 35 | `char(35)` |
| `dppDestDom` | `DPPDestDom` | `String` | SÍ | 50 | `char(50)` |
| `dppCuitTra` | `DPPCuitTra` | `BigDecimal` | SÍ | 11,0 | |
| `dppTranRS` | `DPPTranRS` | `String` | SÍ | 35 | `char(35)` |
| `dppTranDom` | `DPPTranDom` | `String` | SÍ | 50 | `char(50)` |
| `dppVehicu` | `DPPVehicu` | `String` | SÍ | 10 | `char(10)` |
| `dppChasis` | `DPPChasis` | `String` | SÍ | 10 | `char(10)` |
| `dppAcopla` | `DPPAcopla` | `String` | SÍ | 10 | `char(10)` |
| `dppMImpFle` | `DPPMImpFle` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppKmFle` | `DPPKmFle` | `BigDecimal` | SÍ | 10,4 | `smallmoney` |
| `dppAlicFle` | `DPPAlicFle` | `BigDecimal` | SÍ | 10,4 | `smallmoney` |
| `dppImpuFle` | `DPPImpuFle` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppAdicFle` | `DPPAdicFle` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppImIBTot` | `DPPImIBTot` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppAdIBTot` | `DPPAdIBTot` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppImCMTot` | `DPPImCMTot` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppAdCMTot` | `DPPAdCMTot` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppMarcAnu` | `DPPMarcAnu` | `Short` | SÍ | 5 | |
| `dppBarrMar` | `DPPBarrMar` | `Short` | SÍ | 5 | |
| `dppTipoPr` | `DPPTipoPr` | `Short` | SÍ | 5 | |
| `dppTarxKm` | `DPPTarxKm` | `BigDecimal` | SÍ | 19,4 | `money` |
| `dppMEIBrut` | `DPPMEIBrut` | `Short` | SÍ | 5 | huérfana (KB: `DPPMEIBrutos`) |
| `dppMECMeca` | `DPPMECMeca` | `Short` | SÍ | 5 | |
| `dppMEFlete` | `DPPMEFlete` | `Short` | SÍ | 5 | |
| `dppFchGrab` | `DPPFchGrab` | `LocalDateTime` | SÍ | — | `datetime` |
| `dppMarcInt` | `DPPMarcInt` | `Short` | SÍ | 5 | |
| `dppTipoGui` | `DPPTipoGui` | `String` | SÍ | 1 | `char(1)` |
| `dppMarImp` | `DPPMarImp` | `Short` | SÍ | 5 | |
| `dppClasif` | `DPPClasif` | `String` | SÍ | 1 | `char(1)` |
| `dppTipoTra` | `DPPTipoTra` | `String` | NO | 1 | `char(1)` |
| `dppLocOr` | `DPPLocOr` | `Integer` | SÍ | 10 | huérfana |
| `dppLocDe` | `DPPLocDe` | `Integer` | SÍ | 10 | huérfana |
| `dppIncluid` | `DPPIncluid` | `String` | SÍ | 1 | huérfana |
| `dppCorGre` | `DPPCorGre` | `BigDecimal` | SÍ | 19,4 | huérfana |
| `dppUltPaOp` | `DPPUltPaOp` | `String` | SÍ | 10 | `char(10)` |
| `dppUltPSuT` | `DPPUltPSuT` | `String` | SÍ | 3 | `char(3)` |
| `dppUltPSuN` | `DPPUltPSuN` | `Short` | SÍ | 5 | |
| `dppAnuObs` | `DPPAnuObs` | `String` | SÍ | 200 | huérfana (`varchar`) |
| `dppActaNro` | `DPPActaNro` | `Integer` | SÍ | 10 | huérfana |
| `dppActaPue` | `DPPActaPue` | `Short` | SÍ | 5 | huérfana |
| `ppTrataId` | `PPTrataId` | `Short` | SÍ | 5 | FK → `PPTrata` |

> **Total General (`DPPTotGral`) NO es columna física**: es un atributo fórmula
> (`DPPAdCMTot + DPPAdIBTot + DPPAdicFle + DPPImCMTot + DPPImIBTot + DPPImpuFle`). Se deriva
> en `CargaService` (R-CC-21) y se expone en el view model como `totalGeneral` por fila; no se
> mapea con `@Column`. Si se desea persistirlo en la entidad, usar `@Transient`.
> **Nota**: las columnas `DPPNCpbte`, `DPPDestRS`, `DPPDestDom`, `DPPTranRS`, `DPPTranDom`,
> `DPPVehicu`, `DPPChasis`, `DPPAcopla` son `char(n)` de ancho fijo ⇒ se leen con `RTRIM` y se
> escriben con `trim` (paridad con el legacy).

### 2.3 Entidad `PaseGuia` (`@Table(name="DETPROD2")`)

PK compuesta: (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm`, `DPPPaseId`).

| Atributo Java | Columna física | Tipo Java | Nulable | Longitud | Nota |
|---|---|---|---|---|---|
| `conCuit` (PK) | `ConCuit` | `BigDecimal` | NO | 11,0 | PK |
| `dppSucu` (PK) | `DPPSucu` | `Short` | NO | 5 | PK |
| `calPerFisc` (PK) | `CalPerFisc` | `Integer` | NO | 10 | PK |
| `dppSecu` (PK) | `DPPSecu` | `Short` | NO | 5 | PK |
| `dppNroForm` (PK) | `DPPNroForm` | `String` | NO | 10 | PK, `char(10)` |
| `dppPaseId` (PK) | `DPPPaseId` | `Short` | NO | 5 | PK |
| `dppPaseFec` | `DPPPaseFec` | `LocalDateTime` | NO | — | `datetime` |
| `dppPaseOpe` | `DPPPaseOpe` | `String` | NO | 10 | `char(10)` |
| `dppPaseHor` | `DPPPaseHor` | `LocalDateTime` | NO | — | `datetime` |
| `dppPaseEst` | `DPPPaseEst` | `String` | NO | 1 | `char(1)` |
| `sucTipo` | `SucTipo` | `String` | NO | 3 | `char(3)` |
| `sucNroId` | `SucNroId` | `Short` | NO | 5 | |
| `dppPaseCo` | `DPPPaseCo` | `Integer` | SÍ | 10 | nullable |

## 3. Contratos REST

### 3.1 Endpoints existentes (consumidos, no modificados)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/health` | Health check |
| `GET` | `/api/v1/contribuyentes/{conCuit}` | Obtener contribuyente (si se expone) |

> **Nota**: si el módulo expone endpoints nuevos de cargas, deben documentarse aquí y en
> Postman (G-POSTMAN).

### 3.2 Endpoints nuevos propuestos (si aplica)

| Método | Ruta | Request | Response | Códigos |
|---|---|---|---|---|
| `GET` | `/api/v1/cargas/formularios?dppNroForm=` | query param | `List<DetalleProductoPrimarioDto>` | 200, 400 |
| `POST` | `/api/v1/cargas/marcar` | `MarcarGuiaRequest` | `MarcarGuiaResponse` | 200, 400, 404, 409 |

**DTOs**:

```json
// MarcarGuiaRequest
{
  "conCuit": 20123456789,
  "dppSucu": 1,
  "calPerFisc": 202610,
  "dppSecu": 1,
  "dppNroForm": "1234567890",
  "dppPaseOperador": "OPERADOR1",
  "sucTipo": "PUE",
  "sucNroId": 5
}

// MarcarGuiaResponse
{
  "duplicado": 1,
  "mensaje": "La guia ha sido marcada con exito"
}
```

> **Decisión**: los endpoints REST son opcionales; el módulo puede implementarse sólo con MVC
> (servicio directo). Si se agregan, deben sincronizarse con Postman (G-POSTMAN).
>
> **Corrección de pantalla (2026-11-03)**: no se agregan endpoints nuevos; se reutilizan
> `/cargas/buscar`, `/cargas/marcar` y `/cargas/detalle`. Por lo tanto la colección Postman **no
> requiere cambios** en esta corrección (R-CC-25 se considera satisfecha sin diff de colección).

## 4. Contrato de vistas (Thymeleaf)

### 4.1 Ruta de página y plantilla

| Ruta | Plantilla | Controlador |
|---|---|---|
| `/cargas` | `puestos/control-cargas.html` | `CargaMvcController` |
| `/cargas/detalle` (modal) | `puestos/fragments/form506-detalle.html` | `CargaMvcController` |
| `/cargas/tabla` | redirección a `WWTDetProdPri` | `CargaMvcController` |
| `/cargas/pagos` | redirección a `WPFormularioPago` | `CargaMvcController` |

### 4.2 View model

```java
public class CargaForm {
    private String barCode;          // &BarCode (31)
    private String dppNroForm;      // &DPPNroForm (10)
    private Long conCuit;           // &ConCuit
    private String conRazSoc;       // &ConRazSoc
    private ContextoSesion context; // &Context
    private String mensaje;         // &Mensaje
    private Integer duplicado;      // &Duplicado
    private String dpptipoGui;      // &DPPTipoGui
    private Boolean mostrarModal;   // flag dedicado (R-CC-23): NO inferir de dppNroForm != null
    private List<DetalleProductoPrimario> formularios; // grilla (7 columnas, R-CC-20)
}
```

### 4.3 Reglas de validación visibles

- `barCode`: no vacío para buscar (opcional, paridad con legacy).
- `dppNroForm`: 10 caracteres (extraído del barcode).
- La acción Marcar requiere confirmación JS (`confirm`).
- El botón Marcar se deshabilita si `Context.Sucursal = 0`.
- La grilla renderiza 7 columnas en el orden/anchos del Figma (R-CC-20); el header usa
  `--atp-button` y los valores `--atp-cargas-text-secondary`.
- Tokens de cargas en `tokens.css` (R-CC-24): `--atp-cargas-fieldset-border`,
  `--atp-cargas-input-border`, `--atp-cargas-text-secondary`, `--atp-cargas-button-border`.
- El modal sólo se renderiza si `mostrarModal = true` (R-CC-23).

### 4.4 Comportamiento ante error

- Si `MarcarGuia` devuelve error (anulada, interna), se muestra el mensaje verbatim en un banner.
- Si `Duplicado > 1`, se muestra `Guia Duplicada , ya paso por algun puesto` en el detalle.
- Si la carga falla, se muestra el `message` del servicio en un mensaje inline.

## 5. Decisiones de diseño

| # | Decisión | Justificación |
|---|---|---|
| D-CC-01 | La pantalla se implementa en `atp-puestos-camineros` | Dominio de Puestos Camineros |
| D-CC-02 | `MarcarGuia` se implementa como servicio | Lógica de negocio en `service/**` |
| D-CC-03 | La grilla se refresca por submit del formulario | Thymeleaf renderiza por servidor |
| D-CC-04 | El popup `Form506Detalle` se implementa como fragmento modal | Evita duplicar lógica |
| D-CC-05 | Las columnas huérfanas se mapean con `@Column` y default null/0 | Paridad de BD, sin rediseño |
| D-CC-06 | Los mensajes verbatim se conservan con sus erratas | Paridad de identidad |
| D-CC-07 | Se reutiliza `ContextoSesion` de `03-login` | No duplicar el contexto |
| D-CC-08 | Se reutiliza `GestorContextoSesion` para recuperar el contexto | Paridad con `LoadContext` |
| D-CC-09 | Se reutiliza la seguridad de `03-login` (CSRF, sesión) | No rediseñar seguridad |
| D-CC-10 | El modal se abre desde 2 columnas ("Nro Guia" canónico + "Cuit" por pedido del usuario) | Satisfacer pedido del usuario sin perder paridad Figma/legacy |
| D-CC-11 | Buscar sólo refresca la grilla; el modo detalle se controla con flag dedicado `mostrarModal` | Evitar inferencia desde `dppNroForm != null` |
| D-CC-12 | El modal Thymeleaf incorpora botón de cierre explícito (JS puntual) | El legacy lo heredaba del popup de GeneXus |

## 6. Supuestos abiertos

Ver `spec.md` §6. Resumen:
- SA-CC-01: ruta `/cargas` vs `/control-cargas`.
- SA-CC-02: popup modal vs página separada.
- SA-CC-03: `WWTDetProdPri`/`WPFormularioPago` sólo navegación.
- SA-CC-04: validación de longitud de barcode.
- SA-CC-05: `DPPPaseCo` null vs 0.
- SA-CC-06: mensajes en popup vs vista separada.
- SA-CC-07: el lienzo Figma `2024:371` cubre sólo la pantalla de consulta; el modal se deriva del
  WebPanel legacy `Form506Detalle` (popup). El modal se mantiene como fragmento Thymeleaf (D-CC-04).

## 7. Alcance excluido

- Reporte en PDF de cargas.
- ABM de `Contribu`.
- ABM de `DetProdP` (cabecera).
- Autenticación/autorización (definida por `03-login`).
- E2E automatizado.
