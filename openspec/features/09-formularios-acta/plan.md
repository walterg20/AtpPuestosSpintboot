# Plan — 09 Formularios Acta (Puestos Camineros)

> **Estado**: `BORRADOR (v2, 2026-11-04)` — pendiente de aprobación humana (puerta **G-SPEC**).
> **Revisión v2**: G-DATA verificado (54/15/9/5 columnas) y **SA-ACTA-03 cerrado**: la numeración
> se implementa sobre el contador `GES_SUCN.SucUltNum` con incremento **atómico**
> (`plan.md` §2.4.1, §3.4; `spec.md` §3.11, D-ACTA-19/21). `FORMSUCU` se conserva **sólo** para los
> rangos manuales (§2.4.2, D-ACTA-20).
> Documento de arquitectura. No contiene tareas (ver `tasks.md`).
> **Fuente**: KB `ATPPuestos` (solo lectura) + `Rentas` `INFORMATION_SCHEMA` (G-DATA 2026-11-04).
> Requisitos: `spec.md` (`R-ACTA-*`). Decisiones: `spec.md` §7 (`D-ACTA-*`).

---

## 1. Estructura del módulo

```text
atp-puestos-camineros/
└── src/main/java/ar/com/atp/puestos/puestos/
    ├── domain/
    │   ├── Acta.java                  (@Table F2276   — 54 columnas)
    │   ├── ActaId.java                (@IdClass: F276Id + SucTipo + SucNroId)
    │   ├── ActaProducto.java          (@Table F22761  — 15 columnas)
    │   ├── ActaProductoId.java        (@IdClass: F276Id + SucTipo + SucNroId + ProdCod + F276RZona)
    │   ├── ActaPase.java              (@Table F2276PAS — 9 columnas)
    │   ├── ActaPaseId.java            (@IdClass: SucTipo + SucNroId + F276Id + F276PPaseI)
    │   ├── ContadorFormulario.java    (@Table GES_SUCN — 5 columnas · contador de numeración)
    │   └── RangoManualFormulario.java (@Table FORMSUCU — 8 columnas · rangos manuales)
    ├── repository/
    │   ├── ActaRepository.java         (Spring Data JPA; lectura paginada + filtros)
    │   ├── ActaProductoRepository.java (detalle)
    │   ├── ActaPaseRepository.java     (pases)
    │   ├── ContadorFormularioRepositorio.java (GES_SUCN — incremento atómico del contador)
    │   └── RangoManualFormularioRepositorio.java (FORMSUCU — rangos manuales)
    ├── service/
    │   ├── ActaService.java            (reglas de negocio del alta y de la grilla)
    │   ├── ResultadoAltaActa.java      (éxito / error + mensaje verbatim)
    │   └── ServicioNumeracionActa.java (generación de F276Id sobre GES_SUCN — D-ACTA-19)
    └── web/
        └── mvc/
            ├── ActasController.java     (GET /actas, GET /actas/nueva, POST /actas/guardar)
            └── form/
                ├── ActaForm.java        (@ModelAttribute del alta; traduce a ActaAlta)
                ├── ActaProductoForm.java (fila de la grilla de productos; traduce a ActaAltaProducto)
                └── FiltroActasForm.java  (filtros; traduce a ActaFiltro)
    └── service/
        └── model/                        (contrato del servicio — D-ACTA-22)
            ├── ActaFiltro.java
            ├── ActaFila.java
            ├── ActaAlta.java
            ├── ActaAltaProducto.java
            ├── ActaProductoDisponible.java
            ├── ActaPreparada.java
            └── ResultadoAltaActa.java
atp-puestos-camineros/
└── src/main/resources/
    ├── templates/puestos/
    │   ├── actas-lista.html
    │   ├── acta-formulario.html
    │   └── fragments/
    │       └── acta-productos.html     (grilla de productos reutilizable)
    └── static/
        ├── css/actas.css                (sólo var(--atp-*))
        └── js/actas.js                  (vanilla: alta/baja de filas, confirm, autofocus)
atp-nucleo-compartido/
└── src/main/resources/static/css/tokens.css   (se AGREGAN tokens --atp-actas-*)
```

> **No se toca** `atp-nucleo-compartido/.../domain/Formulario2276.java` ni
> `.../repository/Formulario2276Repositorio.java` (proyección de lectura de `06-turnos`).

### 1.1 Paquetes y dependencias

| Capa | Paquete | Puede depender de |
|---|---|---|
| Dominio | `ar.com.atp.puestos.puestos.domain` | nada (G7) |
| Repositorio | `ar.com.atp.puestos.puestos.repository` | `domain` |
| Servicio | `ar.com.atp.puestos.puestos.service` | `domain`, `repository`, `nucleo.service` |
| Web MVC | `ar.com.atp.puestos.puestos.web.mvc` | `service`, `nucleo.web.mvc` |

**Prohibido**: `web → repository`, `domain → web`, lógica de negocio en el controller (G7).

---

## 2. Paridad JPA — tablas y columnas

### 2.1 Entidad `Acta` (`@Table(name = "F2276")`)

PK compuesta: (`F276Id`, `SucTipo`, `SucNroId`) — constraint `PK__F2276__66F200B7`.
**54 columnas físicas** (G-DATA 2026-11-04). Los `char(n)` se leen con `RTRIM` y se escriben
recortados (R-ACTA-052).

| Atributo Java | Columna física | Tipo SQL | Tipo Java | Nul. | Long./prec. | Notas |
|---|---|---|---|---|---|---|
| `f276Id` | `F276Id` | `int` | `Integer` | NO | 10,0 | **PK** |
| `sucTipo` | `SucTipo` | `char(3)` | `String` | NO | 3 | **PK** · `RTRIM` |
| `sucNroId` | `SucNroId` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `pueCod` | `PueCod` | `smallint` | `Integer` | SÍ | 5,0 | **huérfana** (no en KB) · default `null` |
| `cpcId` | `CPCId` | `int` | `Integer` | SÍ | 10,0 | **huérfana** (no en KB) · default `null` |
| `cpcPerId` | `CPCPerId` | `int` | `Integer` | NO | 10,0 | `NOT NULL` ⇒ default de aplicación: `ContextoSesion.personaId()` |
| `f276CuitVe` | `F276CuitVe` | `decimal(11,0)` | `BigDecimal` | SÍ | 11,0 | CUIT vendedor |
| `f276VenRS` | `F276VenRS` | `char(35)` | `String` | SÍ | 35 | razón social vendedor · `RTRIM` |
| `f276VenDom` | `F276VenDom` | `char(50)` | `String` | SÍ | 50 | domicilio vendedor · `RTRIM` |
| `f276VenCLo` | `F276VenCLo` | `int` | `Integer` | SÍ | 10,0 | localidad vendedor |
| `f276TCpbte` | `F276TCpbte` | `smallint` | `Integer` | SÍ | 5,0 | tipo de comprobante (1,2,3,5,6,7) |
| `f276NCpbte` | `F276NCpbte` | `char(15)` | `String` | SÍ | 15 | nro. de comprobante · `RTRIM` |
| `f276FchMov` | `F276FchMov` | `datetime` | `LocalDateTime` | SÍ | — | fecha de movimiento |
| `f276CuitDe` | `F276CuitDe` | `decimal(11,0)` | `BigDecimal` | SÍ | 11,0 | CUIT destinatario |
| `f276DestRS` | `F276DestRS` | `char(35)` | `String` | SÍ | 35 | `RTRIM` |
| `f276DestDo` | `F276DestDo` | `char(50)` | `String` | SÍ | 50 | `RTRIM` |
| `f276DestCL` | `F276DestCL` | `int` | `Integer` | SÍ | 10,0 | localidad destinatario |
| `f276CuitTr` | `F276CuitTr` | `decimal(11,0)` | `BigDecimal` | SÍ | 11,0 | CUIT transportista |
| `f276TranRS` | `F276TranRS` | `char(35)` | `String` | SÍ | 35 | `RTRIM` |
| `f276TranDo` | `F276TranDo` | `char(50)` | `String` | SÍ | 50 | `RTRIM` |
| `f276TranCL` | `F276TranCL` | `int` | `Integer` | SÍ | 10,0 | localidad transportista |
| `f276Vehicu` | `F276Vehicu` | `char(20)` | `String` | SÍ | 20 | `RTRIM` |
| `f276Chasis` | `F276Chasis` | `char(10)` | `String` | SÍ | 10 | patente de chasis · `RTRIM` |
| `f276Acopla` | `F276Acopla` | `char(10)` | `String` | SÍ | 10 | patente de acoplado · `RTRIM` |
| `f276MImpFl` | `F276MImpFl` | `money` | `BigDecimal` | SÍ | 19,4 | monto imposing de flete |
| `f276KmFle` | `F276KmFle` | `smallmoney` | `BigDecimal` | SÍ | 10,4 | kilómetros de flete |
| `f276TarxKm` | `F276TarxKm` | `money` | `BigDecimal` | SÍ | 19,4 | tarifa por km (fórmula en KB, **columna física**) |
| `f276AlicFl` | `F276AlicFl` | `smallmoney` | `BigDecimal` | SÍ | 10,4 | alícuota de flete |
| `f276ImpuFl` | `F276ImpuFl` | `money` | `BigDecimal` | SÍ | 19,4 | impuesto de flete ⇒ `0` con `F276GenWeb='A'` |
| `f276AdicFl` | `F276AdicFl` | `money` | `BigDecimal` | SÍ | 19,4 | adicional de flete |
| `f276AdIBTo` | `F276AdIBTo` | `money` | `BigDecimal` | SÍ | 19,4 | adicional de ingresos brutos |
| `f276AdCMTo` | `F276AdCMTo` | `money` | `BigDecimal` | SÍ | 19,4 | adicional de cosecha mecánica |
| `f276PatVeh` | `F276PatVeh` | `char(10)` | `String` | SÍ | 10 | `RTRIM` |
| `f276Operad` | `F276Operad` | `char(10)` | `String` | SÍ | 10 | usuario generador (uppercase) · `RTRIM` |
| `lf276Id` | `LF276ID` | `int` | `Integer` | SÍ | 10,0 | vínculo con `LOTES227.LF276ID` |
| `f276FchPCC` | `F276FchPCC` | `datetime` | `LocalDateTime` | SÍ | — | fecha de primer uso |
| `turNroId` | `TurNroId` | `int` | `Integer` | SÍ | 10,0 | turno de la sesión |
| `f276Clasif` | `F276Clasif` | `char(1)` | `String` | NO | 1 | **NOT NULL** ⇒ `E`/`S` (nunca `N`) · `RTRIM` |
| `f276ForPag` | `F276ForPag` | `char(3)` | `String` | SÍ | 3 | forma de pago ⇒ `'EFE'` · `RTRIM` |
| `f276NumPag` | `F276NumPag` | `char(20)` | `String` | SÍ | 20 | `RTRIM` |
| `f276Multa` | `F276Multa` | `money` | `BigDecimal` | NO | 19,4 | **NOT NULL** ⇒ default `0` |
| `f276OrCod` | `F276OrCod` | `int` | `Integer` | SÍ | 10,0 | código de localidad de origen |
| `f276Origen` | `F276Origen` | `char(50)` | `String` | SÍ | 50 | descripción de origen · `RTRIM` |
| `f276DeCod` | `F276DeCod` | `int` | `Integer` | SÍ | 10,0 | código de localidad de destino |
| `f276Destin` | `F276Destin` | `char(50)` | `String` | SÍ | 50 | descripción de destino · `RTRIM` |
| `f276Estad` | `F276Estad` | `char(1)` | `String` | SÍ | 1 | `'C'` confirmada, `'A'` anulada · `RTRIM` |
| `f276GenWeb` | `F276GenWeb` | `char(1)` | `String` | SÍ | 1 | `'A'` generada por la web · `RTRIM` |
| `f276BanChe` | `F276BanChe` | `char(40)` | `String` | SÍ | 40 | `RTRIM` |
| `f276Vers` | `F276Vers` | `char(18)` | `String` | SÍ | 18 | versión del sistema (KB: `BuscarVersionActual`) · `RTRIM` |
| `f276FeGra` | `F276FeGra` | `datetime` | `LocalDateTime` | SÍ | — | fecha de grabación ⇒ `ServerNow()` |
| `f276SnSnNu` | `F276SnSnNu` | `decimal(10,0)` | `BigDecimal` | SÍ | 10,0 | nro. de SN (sólo casa central) |
| `f276FePag` | `F276FePag` | `datetime` | `LocalDateTime` | SÍ | — | fecha de pago |
| `f276ActaTi` | `F276ActaTi` | `char(1)` | `String` | SÍ | 1 | motivo del acta (`F`/`P`/`T`/`N`) · `RTRIM` |
| `f276FormTi` | `F276FormTi` | `smallint` | `Integer` | SÍ | 5,0 | tipo de formulario (1/2/3) |

**Atributos de la KB que NO son columnas** (no se mapean; se derivan en `ActaService` — D-ACTA-07):
`F276VenLoc`, `F276VenProv`, `F276DestLoc`, `F276DestProv`, `F276TranLoc`, `F276TranProv`,
`F276ImIBTot`, `F276ImCMTot`, `F276AdiTot`, `F276ImpTotal`, `F276Total`, `F276ProCan`,
`F276GenMan`, `F276CodBar` (SA-ACTA-05), `FCalFlete`.

> **Total del acta (R-ACTA-007)**: `F276Total = F276AdiTot + F276ImpTotal`, con
> `F276AdiTot = F276AdicFl + F276AdIBTo + F276AdCMTo` y
> `F276ImpTotal = F276ImpuFl + F276ImIBTot + F276ImCMTot`.
> `F276ImIBTot` y `F276ImCMTot` se obtienen sumando el detalle `F22761.F276RImpIB` /
> `F22761.F276RImpCM` (misma técnica que `Formulario2276Repositorio#totalizarTurno`).
> Con `F276GenWeb = 'A'` el legacy fuerza `F276ImpuFl = F276ImIBTot = F276ImCMTot = 0`.

### 2.2 Entidad `ActaProducto` (`@Table(name = "F22761")`)

PK compuesta: (`F276Id`, `SucTipo`, `SucNroId`, `ProdCod`, `F276RZona`) — `PK__F22761__67E624F0`.
**15 columnas físicas**.

| Atributo Java | Columna física | Tipo SQL | Tipo Java | Nul. | Long./prec. | Notas |
|---|---|---|---|---|---|---|
| `f276Id` | `F276Id` | `int` | `Integer` | NO | 10,0 | **PK** |
| `sucTipo` | `SucTipo` | `char(3)` | `String` | NO | 3 | **PK** · `RTRIM` |
| `sucNroId` | `SucNroId` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `prodCod` | `ProdCod` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `f276RZona` | `F276RZona` | `char(1)` | `String` | NO | 1 | **PK** · `RTRIM` · ver SA-ACTA-06 |
| `f276RCant` | `F276RCant` | `money` | `BigDecimal` | SÍ | 19,4 | cantidad |
| `f276RMarCM` | `F276RMarCM` | `char(1)` | `String` | SÍ | 1 | marca de cosecha mecánica ⇒ `'N'` · `RTRIM` |
| `f276RMImpo` | `F276RMImpo` | `money` | `BigDecimal` | SÍ | 19,4 | importe del manufacturer's |
| `f276RFijo` | `F276RFijo` | `money` | `BigDecimal` | SÍ | 19,4 | precio fijo (fórmula en KB → **se persiste**) |
| `f276RPreCM` | `F276RPreCM` | `money` | `BigDecimal` | SÍ | 19,4 | precio cosecha mecánica (fórmula en KB → **se persiste**) |
| `f276RAlicu` | `F276RAlicu` | `smallmoney` | `BigDecimal` | SÍ | 10,4 | alícuota ⇒ `1` |
| `f276RImpIB` | `F276RImpIB` | `money` | `BigDecimal` | SÍ | 19,4 | impuesto IB (fórmula en KB → **se persiste**) |
| `f276RImpCM` | `F276RImpCM` | `money` | `BigDecimal` | SÍ | 19,4 | impuesto CM (fórmula en KB → **se persiste**) |
| `f276Corrig` | `F276Corrig` | `char(1)` | `String` | SÍ | 1 | **huérfana** (no en KB) · default `null` · `RTRIM` |
| `f276RSPre` | `F276RSPre` | `money` | `BigDecimal` | SÍ | 19,4 | precio informado (≥ `F276RFijo`) |

**No físicos** (se derivan de `PRODUCTO`): `ProdDesc`, `ProdTipCal`.

### 2.3 Entidad `ActaPase` (`@Table(name = "F2276PAS")`)

PK compuesta: (`SucTipo`, `SucNroId`, `F276Id`, `F276PPaseI`) — `PK__F2276PAS__F86DC8B00A111FC2`.
**9 columnas físicas**.

| Atributo Java | Columna física | Tipo SQL | Tipo Java | Nul. | Long./prec. | Notas |
|---|---|---|---|---|---|---|
| `sucTipo` | `SucTipo` | `char(3)` | `String` | NO | 3 | **PK** · `RTRIM` |
| `sucNroId` | `SucNroId` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `f276Id` | `F276Id` | `int` | `Integer` | NO | 10,0 | **PK** |
| `f276PPaseI` | `F276PPaseI` | `smallint` | `Integer` | NO | 5,0 | **PK** · índice correlativo del pase |
| `f276PFecha` | `F276PFecha` | `datetime` | `LocalDateTime` | NO | — | **NOT NULL** ⇒ `ServerNow()` |
| `f276PUsuar` | `F276PUsuar` | `char(10)` | `String` | NO | 10 | **NOT NULL** ⇒ usuario de la sesión · `RTRIM` |
| `f276PaseST` | `F276PaseST` | `char(3)` | `String` | SÍ | 3 | tipo del pase (`'PUE'`) · `RTRIM` |
| `f276PaseSN` | `F276PaseSN` | `smallint` | `Integer` | SÍ | 5,0 | nro. de sucursal del pase |
| `f276PaseCo` | `F276PaseCo` | `int` | `Integer` | SÍ | 10,0 | comisión del pase |

> `F276PaseSD` existe en la KB pero **no** en la BD — **verificado** el 2026-11-04
> (`SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='F2276PAS' AND
> COLUMN_NAME='F276PaseSD'` ⇒ `0`). No se mapea.

### 2.4 Entidades de apoyo propias — `GES_SUCN` (contador) y `FORMSUCU` (rangos manuales)

Dos tablas con **propósitos distintos** (D-ACTA-20). La numeración usa `GES_SUCN`; la detección de
números manuales usa `FORMSUCU`.

#### 2.4.1 `ContadorFormulario` (`@Table(name = "GES_SUCN")`) — **5 columnas**

PK: (`SucNroId`, `SucTipo`, `SucNuForm`) — constraint `PK__GES_SUCN__…` (verificado). Sin `IDENTITY`,
sin FK declaradas.

| Atributo Java | Columna física | Tipo SQL | Tipo Java | Nul. | Long./prec. | Notas |
|---|---|---|---|---|---|---|
| `sucNroId` | `SucNroId` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `sucTipo` | `SucTipo` | `char(3)` | `String` | NO | 3 | **PK** · `RTRIM` |
| `sucNuForm` | `sucNuForm` | `smallint` | `Integer` | NO | 5,0 | **PK** · `2276` para actas |
| `sucNuId` | `SucNuId` | `char(3)` | `String` | NO | 3 | **NOT NULL** · default `'MAN'` (D-ACTA-19, SA-ACTA-14) · `RTRIM` |
| `sucUltNum` | `SucUltNum` | `int` | `Integer` | NO | 10,0 | **NOT NULL** · contador del último número emitido |

> **Uso**: fuente autoritativa de la numeración (§3.11 de `spec.md`). El incremento debe ser
> **atómico** (D-ACTA-19).

#### 2.4.2 `RangoManualFormulario` (`@Table(name = "FORMSUCU")`) — **8 columnas**

PK lógica: (`SucNroId`, `SucTipo`, `SucNuForm`, `ForRecIni`).

| Atributo Java | Columna física | Tipo SQL | Tipo Java | Nul. | Long./prec. | Notas |
|---|---|---|---|---|---|---|
| `sucNroId` | `SucNroId` | `smallint` | `Integer` | NO | 5,0 | **PK** |
| `sucTipo` | `SucTipo` | `char(3)` | `String` | NO | 3 | **PK** · `RTRIM` |
| `sucNuForm` | `SucNuForm` | `smallint` | `Integer` | NO | 5,0 | **PK** · `2276` |
| `forRecIni` | `ForRecIni` | `int` | `Integer` | NO | 10,0 | **PK** · inicio del rango manual |
| `forRecCant` | `ForRecCant` | `int` | `Integer` | NO | 10,0 | cantidad del rango |
| `forRecFech` | `ForRecFech` | `datetime` | `LocalDateTime` | NO | — | fecha de recepción |
| `forRecUsu` | `ForRecUsu` | `char(10)` | `String` | NO | 10 | usuario que recibió · `RTRIM` |
| `forRecTipo` | `ForRecTipo` | `char(5)` | `String` | SÍ | 5 | tipo del rango (`'ACTS'` / `'ACTI'`) · `RTRIM` |

> **NO existen** `SucUltNum` ni `ForRecFin` en esta tabla (verificado). El rango manual es
> `[ForRecIni, ForRecIni + ForRecCant)` (D-ACTA-20, SA-ACTA-15/16).

### 2.5 Lecturas de apoyo (se reutilizan entidades del núcleo)

| Origen | Entidad existente | Uso en este módulo |
|---|---|---|
| `PRODUCTO` | `nucleo.domain.Producto` (`ProdCod`, `ProdDesc`, `ProdTipCal`, `ProdAnula`) | R-ACTA-020 (filtro `ProdAnula = 0`), R-ACTA-021 (`ProdTipCal = 3`) |
| `PRODUCT1` | `nucleo.domain.ProductoVigencia` (`ProdFchVig`, `ProdImpu1..5`) | R-ACTA-022 (`F2276_BusProTarifa` / `F2276_BusProTarCM`) |
| `CONTRIBU` | `puestos.domain.Contribuyente` (`ConCuit`, `ConRazSoc`, `ConEstado`) | R-ACTA-018 (derivación por CUIT), R-ACTA-046 (`ConEstado` 12/14) |
| `CALLES` | `nucleo.domain.Calle` (`CallCod`, `CallNom`) | R-ACTA-018 (domicilio: `CallNom + ' ' + manzana`, `F2276_BusDom`) |
| `Localida` | `nucleo.domain.Localidad` (`LocCod`, `LocNom`, `LocNomProv`) | R-ACTA-018 (localidad y provincia) |
| `PROVINCI` | `nucleo.domain.Provincia` | R-ACTA-018 (nombre de provincia) |
| `PPRIM_TU` | `nucleo.domain.Turno` (`TurNroId`, `TurEstado`) | R-ACTA-035 / R-ACTA-042 (`PPrim_TurnoEstado`) |
| `VERSIONE` | **nueva** `VersionSistema` (`puestos.domain`) — `VerApliId`, `VerDescr`, `VerVersion`, `VerRevisio`, `VerBuild` | R-ACTA-045 (`F276Vers`, `BuscarVersionActual`) |
| `LOTES227` | `nucleo.domain.Lote227` | R-ACTA-041 (vía `ServicioEstadoDeForm`) |

### 2.6 Default de aplicación por columna `NOT NULL`

| Columna | Default de aplicación | Origen |
|---|---|---|
| `F2276.CPCPerId` | `ContextoSesion.personaId()` | `Default(CPCPerId, &Context.PersonaId)` |
| `F2276.F276Clasif` | `'E'` o `'S'` (nunca vacío; la vista no ofrece otra opción) | `F276Clasif` es `NOT NULL` |
| `F2276.F276Multa` | `0` | `default(F276Multa, 0)` |
| `F2276.F276Id` | número generado por el servicio desde `GES_SUCN.SucUltNum` | `F2276_GenNroForm` (D-ACTA-19) |
| `F2276.SucTipo` / `SucNroId` | del contexto de sesión | `BeforeInsert` del legacy |
| `F22761.F276RZona` | `' '`/vacío (ver SA-ACTA-06) | `GridToBC` legacy no lo informa |
| `F2276PAS.F276PFecha` | `ServerNow()` | `F2276_RegPasePuestoV2` |
| `F2276PAS.F276PUsuar` | `ContextoSesion.usuario()` | ídem |
| `GES_SUCN.SucNuId` | `'MAN'` | `when none` del legacy no lo informa (D-ACTA-19, SA-ACTA-14) |
| `GES_SUCN.SucUltNum` | `1` en la inserción; `+1` en el incremento | `F2276_GenNroForm` |

---

## 3. Contratos

### 3.1 REST — sin endpoints nuevos

| Método | Ruta | Estado |
|---|---|---|
| — | — | El módulo **no** expone API (D-ACTA-04). No hay cambios en `docs/postman/ATPPuestos-API.postman_collection.json`. |

> Si en el futuro se agrega `/api/v1/actas/**`, G-POSTMAN obliga a actualizar la colección en el
> mismo commit (R-ACTA-058).

### 3.2 MVC — rutas, métodos y atributos de modelo

| Método | Ruta | Plantilla / destino | Atributos del `Model` | Notas |
|---|---|---|---|---|
| `GET` | `/actas` | `puestos/actas-lista` | `actas` (`Page<ActaFila>`), `filtroActas`, `contexto`, `programas`, `rutaActual`, `mensaje` | `Pageable` (R-ACTA-010). Acepta `page`, `size`, `sort` y los filtros. |
| `GET` | `/actas/nueva` | `puestos/acta-formulario` | `actaForm`, `contexto`, `programas`, `rutaActual`, `productosDisponibles`, `bloqueado`, `mensajeBloqueo` | Valida lotes/turno **antes** de renderizar (R-ACTA-041, R-ACTA-042). |
| `POST` | `/actas/guardar` | `puestos/acta-formulario` (200) ó `redirect:/actas` (302) | `actaForm`, `contexto`, `programas`, `rutaActual`, `productosDisponibles` | `@Valid @ModelAttribute("actaForm") ActaForm` + `BindingResult`. Errores de binding ⇒ re-render con `th:errors`. Error de negocio ⇒ re-render con el mensaje verbatim. Éxito ⇒ `redirect:/actas` + flash `mensajeExito`. |

**Flash attributes** (sólo en el `redirect` de éxito):

| Atributo | Contenido |
|---|---|
| `mensajeExito` | `El acta se ha creado correctamente` (verbatim, legacy) |

**Parámetros de consulta de `GET /actas`**

| Parámetro | Tipo | Semántica |
|---|---|---|
| `page` | `int` (0-based) | `Pageable` |
| `size` | `int` (default **10**) | `Pageable` |
| `sort` | `String` | por defecto `F276Id,asc` |
| `f276Id` | `Integer` | nº de Acta (exacto) |
| `cuit` | `Long` | CUIT de cualquiera de los tres roles |
| `razonSocial` | `String` | coincidencia parcial, case-insensitive |
| `desde` / `hasta` | `LocalDate` | rango de `F276FchMov` |

### 3.3 View models

> **D-ACTA-22 — los view models de la capa `web` NO son el contrato del servicio.**
> La regla de dependencia del proyecto es `web → service → repository → domain`
> (`openspec/constitution/stack.md`, G7). Si `ActaService` devolviera o consumiera tipos de
> `web/mvc/form/**`, la dependencia quedaría invertida (`service → web`) y el servicio dejaría
> de ser reutilizable por otro adaptador (REST, jobs).
> **Corrección**: los **DTO de contrato del servicio** viven en `service/model/**` (o en el
> paquete del servicio), y la capa `web/mvc/form/**` contiene **objetos de binding**
> (Bean Validation + `th:field`) que el controller **traduce** a los DTO del servicio y
> viceversa. La traducción vive en el controller (adaptador), nunca en el servicio.

```java
// ===== service/model/ActaFiltro.java  (contrato del servicio) =====
public record ActaFiltro(
    Integer f276Id,          // nº de Acta (exacto)
    Long cuit,               // CUIT de cualquiera de los tres roles
    String razonSocial,      // coincidencia parcial, case-insensitive
    LocalDate desde,         // rango sobre F276FchMov
    LocalDate hasta
) {}

// ===== service/model/ActaFila.java  (fila de la grilla de listado) =====
public record ActaFila(
    Integer f276Id,          // F276Id
    Long conCuit,            // contraparte (vendedor > destinatario > transportista)
    String conRazSoc,        // razón social de la contraparte
    String genManual,        // "Sí" | "No"
    String f276Operad,       // usuario generador (trim)
    BigDecimal f276Total,    // total derivado
    boolean imprimible       // true si NO es manual (columna "Imprimir" habilitada)
) {}

// ===== service/model/ActaAlta.java  (comando de alta) =====
public record ActaAlta(
    String f276ActaTi, String f276Clasif, LocalDate f276FchMov,
    Long f276CuitVe, String f276VenRS, String f276VenDom, Integer f276VenCLo,
    Long f276CuitDe, String f276DestRS, String f276DestDo, Integer f276DestCL,
    Long f276CuitTr, String f276TranRS, String f276TranDo, Integer f276TranCL,
    Integer f276TCpbte, String f276NCpbte, String f276Chasis, String f276Acopla,
    BigDecimal f276KmFle,
    List<ActaAltaProducto> productos) {}

// ===== service/model/ActaAltaProducto.java =====
public record ActaAltaProducto(
    Integer prodCod, BigDecimal f276RCant, BigDecimal f276RSPre) {}

// ===== service/model/ActaProductoDisponible.java  (producto elegible en la grilla) =====
public record ActaProductoDisponible(
    Integer prodCod,        // ProdCod
    String prodDesc,        // ProdDesc (trim)
    Integer prodTipCal,     // ProdTipCal (3 ⇒ cantidad fija 1)
    String prodAcop,        // ProdAcop
    BigDecimal precioFijo,  // F2276_BusProTarifa (F276RFijo)
    BigDecimal precioCM     // F2276_BusProTarCM (F276RPreCM)
) {}

// ===== service/model/ActaPreparada.java  (estado del formulario antes de renderizar) =====
public record ActaPreparada(
    boolean bloqueado,              // true ⇒ hay lote/turno que bloquea el alta
    String mensajeBloqueo,          // motivo verbatim del bloqueo (null si no hay bloqueo)
    String turnoEstado,             // estado del turno del contexto
    Integer f276IdSugerido,         // próximo número (informativo; el real lo asigna el servicio)
    LocalDate fechaMovimiento        // fecha por defecto de la alta
) {}

// ===== service/model/ResultadoAltaActa.java =====
public record ResultadoAltaActa(boolean ok, String mensaje, Integer f276Id) {}
```

```java
// ===== web/mvc/form/FiltroActasForm.java  (binding + traducción) =====
public class FiltroActasForm {
    private Integer f276Id;
    private Long cuit;
    private String razonSocial;
    private LocalDate desde;
    private LocalDate hasta;

    /** Traducción al contrato del servicio (adaptador). */
    public ActaFiltro aFiltro() { return new ActaFiltro(f276Id, cuit, razonSocial, desde, hasta); }
}

// ===== web/mvc/form/ActaForm.java  (@ModelAttribute("actaForm")) =====
public class ActaForm {
    // Datos del Acta
    @NotBlank private String f276ActaTi;   // motivo: N|F|P|T
    @NotBlank private String f276Clasif;   // tipo de movimiento: N|S|E
    @NotNull  @PastOrPresent private LocalDate f276FchMov;
    // Vendedor
    @NotNull private Long f276CuitVe;
    private String f276VenRS; private String f276VenDom;
    private Integer f276VenCLo; private String f276VenLoc; private String f276VenProv;
    // Destinatario
    @NotNull private Long f276CuitDe;
    private String f276DestRS; private String f276DestDo;
    private Integer f276DestCL; private String f276DestLoc; private String f276DestProv;
    // Transportista
    @NotNull private Long f276CuitTr;
    private String f276TranRS; private String f276TranDo;
    private Integer f276TranCL; private String f276TranLoc; private String f276TranProv;
    // Datos del Transporte
    @NotNull private Integer f276TCpbte;   // 1|2|3|5|6|7
    @NotBlank private String f276NCpbte;
    @NotBlank private String f276Chasis;
    @NotBlank private String f276Acopla;
    @NotNull  private BigDecimal f276KmFle;
    // Detalle
    @Valid @NotEmpty private List<ActaProductoForm> productos;
    // Contexto (sólo lectura, no se bindea)
    private ContextoSesion contexto;

    /** Traducción al comando del servicio (adaptador). */
    public ActaAlta aAlta() { /* ... mapea campo a campo ... */ }
}

// ===== web/mvc/form/ActaProductoForm.java  (fila de la grilla de productos del alta) =====
public class ActaProductoForm {
    private Integer prodCod;
    private String  prodDesc;     // sólo lectura (descripción)
    private Integer prodTipCal;   // sólo lectura
    private BigDecimal f276RCant; // cantidad
    private BigDecimal f276RFijo; // sólo lectura (precio fijo calculado)
    private BigDecimal f276RSPre; // precio informado
    private String  f276RZona;    // sólo lectura
    private String  prodAcop;     // sólo lectura

    /** Traducción al contrato del servicio (adaptador). */
    public ActaAltaProducto aProducto() { return new ActaAltaProducto(prodCod, f276RCant, f276RSPre); }
}
```

> **Los `NOT NULL` del formulario** son la primera línea de defensa; las reglas del legacy
> (orden de evaluación, CUIT válido, razón social, transporte completo, máx. 6 productos) se
> validan **en el servicio**, que devuelve el **primer** mensaje verbatim (R-ACTA-040).
> El servicio **nunca** conoce `ActaForm`: recibe `ActaAlta` y devuelve `ResultadoAltaActa`.

### 3.4 Contrato del servicio `ActaService`

```java
// service/ActaService.java
@Transactional(readOnly = true)
Page<ActaFila> listar(ActaFiltro filtro, Pageable pageable, ContextoSesion ctx);

@Transactional(readOnly = true)
List<ActaProductoDisponible> productosDisponibles();   // Producto con ProdAnula = 0

@Transactional(readOnly = true)
ActaPreparada prepararFormulario(ContextoSesion ctx);   // defaults + estado de bloqueo

@Transactional
ResultadoAltaActa guardar(ActaAlta alta, ContextoSesion ctx);   // reglas + cabecera + detalle + pase

// --- derivaciones (procedures verificados, spec.md §3.12) ---
@Transactional(readOnly = true)
DatosParte derivarPorCuit(Long cuit);     // F2276_DevRazSocial + F2276_BusDom + F2276_BusLoc

@Transactional(readOnly = true)
BigDecimal precioProducto(Integer prodCod, String clasif, LocalDate fecha, boolean marCM);
                                                       // F2276_BusProTarifa / F2276_BusProTarCM
@Transactional(readOnly = true)
BigDecimal impuestoIb(Integer prodTipCal, BigDecimal cant, BigDecimal mImpo,
        BigDecimal fijo, BigDecimal alicu);             // Nuc_CalculoImpIB

@Transactional(readOnly = true)
String versionSistema();                                 // BuscarVersionActual (VERSIONE)

// service/ServicioNumeracionActa.java  (D-ACTA-19 / D-ACTA-21)
@Transactional(propagation = Propagation.MANDATORY)
int siguienteNumeroActa(Integer sucNroId, String sucTipo);
    // 1) UPDATE GES_SUCN SET SucUltNum = SucUltNum + 1 WHERE SucNroId=? AND SucTipo=? AND SucNuForm=2276
    // 2) si 0 filas afectadas ⇒ INSERT (SucNroId, SucTipo, SucNuForm, 'MAN', 1) ⇒ devuelve 1
    // 3) devuelve el valor incrementado (en la misma transacción del alta)
```

`ResultadoAltaActa` es un `record` inmutable: `(boolean ok, String mensaje, Integer f276Id)`.

**Transaccionalidad del alta**: `ActaService.guardar` es `@Transactional` y **delega** el
incremento del contador a `ServicioNumeracionActa` con `Propagation.MANDATORY`, de modo que el
número y la cabecera se confirmen o reviertan **juntos** (R-ACTA-049).

---

## 4. Contrato de vistas (Thymeleaf)

### 4.1 Rutas y plantillas

| Ruta | Plantilla | Fragmentos incluidos |
|---|---|---|
| `/actas` | `puestos/actas-lista` | `layout/base :: head/cabecera/sesion/navegacion/pie`, `fragments/menu :: menu` |
| `/actas/nueva` | `puestos/acta-formulario` | ídem + `puestos/fragments/acta-productos :: grilla` |

### 4.2 `actas-lista.html` — estructura

```text
<main class="atp-pagina atp-actas__page">
  <header class="atp-actas__cabecera">
      <h1 class="atp-pagina__titulo">Actas</h1>
      <a class="atp-btn atp-btn--primary atp-actas__crear" th:href="@{/actas/nueva}">Crear Acta</a>
  </header>

  <form class="atp-form atp-actas__filtros" method="get" th:action="@{/actas}"
        th:object="${filtroActas}">
      <!-- f276Id, cuit, razonSocial, desde, hasta -->
      <button type="submit" class="atp-btn">Buscar</button>
  </form>

  <table class="atp-table atp-actas__tabla" th:if="${not #lists.isEmpty(actas.content)}">
    <caption class="atp-table__caption">Actas del turno</caption>
    <thead><tr>
      <th scope="col">Acto Nro.</th>
      <th scope="col">CUIT</th>
      <th scope="col">Razón Social</th>
      <th scope="col">Gen. Manual?</th>
      <th scope="col">Usuario Gen.</th>
      <th scope="col">Imprimir</th>
    </tr></thead>
    <tbody>
      <tr th:each="a : ${actas.content}">
        <td th:text="${a.f276Id}">0</td>
        <td th:text="${a.conCuit}">0</td>
        <td th:text="${a.conRazSoc}">Razón social</td>
        <td th:text="${a.genManual}">No</td>
        <td th:text="${a.f276Operad}">USUARIO</td>
        <td>
          <!-- th:if: sólo si NO es manual; el enlace de impresión es OUT (R-ACTA-015) -->
          <span class="atp-actas__no-imprime" th:if="${a.imprimible}" aria-hidden="true">🖨</span>
          <span class="atp-actas__no-imprime" th:unless="${a.imprimible}"
                title="No Imprime">🖨</span>
        </td>
      </tr>
    </tbody>
  </table>

  <p class="atp-vacio" th:if="${#lists.isEmpty(actas.content)}">Sin actas para mostrar.</p>

  <!-- Paginación: links dinámicos que preservan los filtros -->
  <nav class="atp-paginacion" th:if="${actas.totalPages > 1}">
    <a th:each="p : ${#numbers.sequence(0, actas.totalPages - 1)}"
       th:href="@{/actas(page=${p},size=${actas.size},sort=${actas.sort},f276Id=${filtroActas.f276Id},cuit=${filtroActas.cuit},razonSocial=${filtroActas.razonSocial},desde=${filtroActas.desde},hasta=${filtroActas.hasta})}"
       th:text="${p + 1}">1</a>
  </nav>
</main>
```

**Reglas de la vista**

| # | Regla | Requisito |
|---|---|---|
| V-1 | El botón "Crear Acta" es un `<a>` con `th:href="@{/actas/nueva}"`, alineado a la derecha con una clase de token (sin `style` inline). | R-ACTA-009 |
| V-2 | Las filas se renderizan con `th:each="a : ${actas.content}"`. | R-ACTA-005 |
| V-3 | La celda "Gen. Manual?" muestra `Sí`/`No` según `a.genManual`. | R-ACTA-008 |
| V-4 | La columna "Imprimir" refleja el estado: `imprimible = true` ⇒ icono de impresora; `false` ⇒ título `No Imprime`. El enlace de impresión **no** se implementa (OUT). | R-ACTA-005 |
| V-5 | El estado vacío se muestra con `th:if` cuando `actas.content` está vacío. | R-ACTA-013 |
| V-6 | La paginación usa `th:each` sobre `#numbers.sequence` y conserva los filtros en la URL. | R-ACTA-010, R-ACTA-012 |
| V-7 | El turno inactivo (`contexto.turno() <= 0`) muestra el banner `Debe iniciar un turno...` y deshabilita "Crear Acta". | R-ACTA-002 |

### 4.3 `acta-formulario.html` — estructura

```text
<main class="atp-pagina atp-actas__page">
  <h1 class="atp-pagina__titulo">Carga manual · Acta</h1>

  <form class="atp-form atp-actas__form" method="post" th:action="@{/actas/guardar}"
        th:object="${actaForm}">

    <!-- Fieldset 1 -->
    <fieldset class="atp-actas__fieldset">
      <legend class="atp-actas__legend">Datos del Acta</legend>
      <div class="atp-field">
        <label for="f276ActaTi" class="atp-field__etiqueta">Motivo de Acta</label>
        <select id="f276ActaTi" class="atp-field__control" th:field="*{f276ActaTi}"
                th:errorclass="atp-field__control--error">
          <option value="N">Seleccionar...</option>
          <option value="F">Falta Documentación de Fletes</option>
          <option value="P">Falta Documentación de Prod. Prim.</option>
          <option value="T">Sin Documentación</option>
        </select>
        <p class="atp-field__error" th:if="${#fields.hasErrors('f276ActaTi')}"
           th:errors="*{f276ActaTi}" role="alert"></p>
      </div>
      <!-- f276Clasif (N|S|E) y f276FchMov (readonly, prefilled) -->
    </fieldset>

    <!-- Fieldsets 2..4: Vendedor / Destinatario / Transportista -->
    <div class="atp-actas__partes">
      <fieldset class="atp-actas__fieldset atp-actas__fieldset--parte">…</fieldset>
      <fieldset class="atp-actas__fieldset atp-actas__fieldset--parte">…</fieldset>
      <fieldset class="atp-actas__fieldset atp-actas__fieldset--parte">…</fieldset>
    </div>

    <!-- Fieldset 5 -->
    <fieldset class="atp-actas__fieldset">
      <legend class="atp-actas__legend">Datos del Transporte</legend>
      <!-- f276Chasis, f276Acopla, f276TCpbte (combo), f276NCpbte, f276KmFle -->
    </fieldset>

    <!-- Fieldset 6 -->
    <fieldset class="atp-actas__fieldset">
      <legend class="atp-actas__legend">Productos</legend>
      <table class="atp-table" id="acta-productos">
        <thead><tr><th>Código</th><th>Descripción</th><th>Tn.</th><th>Eliminar</th></tr></thead>
        <tbody id="acta-productos-body">
          <tr th:each="p, iter : *{productos}">
            <td><select th:field="*{productos[__${iter.index}__].prodCod}">…</select></td>
            <td th:text="${p.prodDesc}">Descripción</td>
            <td><input type="number" step="0.0001"
                       th:field="*{productos[__${iter.index}__].f276RCant}"></td>
            <td><button type="button" class="atp-actas__quitar"
                        th:attr="data-indice=${iter.index}">−</button></td>
          </tr>
        </tbody>
      </table>
      <button type="button" class="atp-actas__agregar" id="acta-agregar-producto"
              aria-label="Agregar producto">+</button>
    </fieldset>

    <div class="atp-actas__acciones">
      <button type="submit" class="atp-btn atp-btn--primary"
              onclick="return confirm('¿Confirma que desea enviar el Formulario de Acta?')">Confirmar</button>
      <a class="atp-btn atp-btn--ghost" th:href="@{/actas}">Cancelar</a>
    </div>
  </form>
</main>
```

**Reglas de la vista**

| # | Regla | Requisito |
|---|---|---|
| V-8 | El form usa `th:object="${actaForm}"` y **cada** control usa `th:field`. | R-ACTA-014 |
| V-9 | Cada campo con error usa `th:errorclass` en el control y `th:errors` en un `<p role="alert">`. | R-ACTA-055 |
| V-10 | `f276FchMov` se renderiza **read-only** con la fecha del servidor (paridad `&F276FchMov.Enabled = false` en alta). | R-ACTA-015 |
| V-11 | El campo `Nro. Acta` **no** se renderiza en alta. | R-ACTA-015 |
| V-12 | Localidad y Provincia son de sólo lectura (se derivan del CUIT). | R-ACTA-017, R-ACTA-018 |
| V-13 | La grilla de productos se agrega en línea con un botón `+` (JS vanilla) y cada fila tiene su `−` para eliminar. | R-ACTA-020, D-ACTA-11 |
| V-14 | El mensaje de error de negocio se muestra en un banner `atp-alert--error` **por encima** del formulario. | R-ACTA-040 |
| V-15 | El botón `Confirmar` dispara el `confirm()` con el texto verbatim. | R-ACTA-024 |
| V-16 | `Cancelar` es un enlace a `/actas` (no persiste). | R-ACTA-025 |
| V-17 | Si el bloque por lotes/turno está activo, el formulario **no** se renderiza: se muestra el motivo. | R-ACTA-041, R-ACTA-042 |

### 4.4 Tokens CSS nuevos (`tokens.css`)

| Token | Valor | Uso |
|---|---|---|
| `--atp-actas-fieldset-border` | `#b1b1b1` | borde de los fieldsets (Figma) |
| `--atp-actas-legend` | `#121212` | color de las leyendas |
| `--atp-actas-label` | `#868686` | color de los rótulos |
| `--atp-actas-input-border` | `#c8c8c8` | borde de los controles (Figma) |
| `--atp-actas-input-radius` | `5px` | radio de los controles |
| `--atp-actas-grid-header-bg` | `#206e92` | fondo del header de la grilla (reutilizar `--atp-button` es preferible) |
| `--atp-actas-agregar` | `#77b41a` | color del botón `+` |
| `--atp-actas-error` | `#c0392b` | reusa `--atp-error` (preferible) |

> **Regla**: `actas.css` **no** contiene literales hexadecimales; todo color sale de
> `var(--atp-*)` (R-ACTA-053). Preferir los tokens existentes (`--atp-button`, `--atp-error`,
> `--atp-surface-alt`, `--atp-border`) antes de crear nuevos.

### 4.5 Comportamiento ante error

| Situación | Comportamiento | Código HTTP |
|---|---|---|
| Sin sesión | Redirección al login (seguridad de `03-login`). | 302 |
| CSRF inválido | Rechazo del `POST`. | 403 |
| Error de binding (`@Valid`) | Re-render del formulario con `th:errors` por campo. | 200 |
| Regla de negocio violada | Re-render del formulario con el **banner** del mensaje verbatim (primer error, en el orden del legacy). | 200 |
| Turno/lote bloqueado en `GET /actas/nueva` | Se muestra el motivo; el formulario no se renderiza. | 200 |
| Éxito | `redirect:/actas` con flash `mensajeExito = El acta se ha creado correctamente`. | 302 |

---

## 5. Matriz de paridad con el legacy

| Operación legacy | Requisito | Implementación | Prueba |
|---|---|---|---|
| Listar actas del turno | R-ACTA-001/03/04 | `ActaRepository.buscar(...)` (filtro base + `Pageable`) | `ActaListadoIT` |
| Columnas de la grilla | R-ACTA-005 | `ActaFila` + `actas-lista.html` | `MvcGrillaColumnas` |
| Contraparte (CUIT / Razón Social) | R-ACTA-006 | `ActaService.contraparte(...)` | `ActaServiceTest` |
| Total del acta | R-ACTA-007 | `ActaService.calcularTotal(...)` | `ActaServiceTest` |
| "Gen. Manual?" | R-ACTA-008 | `RangoManualFormularioRepositorio` + `ActaService.esManual(...)` | `ActaServiceTest` |
| Botón "Nuevo Acta" | R-ACTA-009 | `th:href="@{/actas/nueva}"` | `MvcBotonCrear` |
| Paginación (MaxRows 10) | R-ACTA-010 | `Pageable` (size por defecto 10) | `MvcPaginacion` |
| Filtros | R-ACTA-011/12 | `FiltroActasForm` + query params | `MvcFiltros` |
| Alta del acta | R-ACTA-014…R-ACTA-025 | `ActaForm` + `acta-formulario.html` + `ActaService.guardar` | `MvcFormularioAlta`, `ActaAltaIT` |
| 14 validaciones del `Validar` legacy | R-ACTA-026…R-ACTA-032, R-ACTA-038/39 | `ActaService.validar(...)` en el orden del legacy | `ActaServiceTest#validaciones` |
| 3 `error(...)` de las rules | R-ACTA-033…R-ACTA-035 | `ActaService.validar(...)` | `ActaServiceTest#rules` |
| Numeración del acta | R-ACTA-036 | `ServicioNumeracionActa.siguienteNumeroActa(...)` sobre `GES_SUCN` con incremento atómico | `ActaServiceTest#numeracion`, `ActaNumeracionIT` |
| Número manual informado | R-ACTA-036 | `RangoManualFormularioRepositorio` (`PPrim_VerificaGenMan`) | `ActaServiceTest#numeroManual` |
| `default(...)` (6 valores) + `F276Vers` | R-ACTA-045 | `ActaService.guardar(...)` + `ActaService.versionSistema()` (`VERSIONE`) | `ActaAltaIT` |
| `F276FormTi` | R-ACTA-046 | `ActaService.tipoFormulario(...)` | `ActaServiceTest#formTi` |
| Detalle (`GridToBC`) con las fórmulas verificadas | R-ACTA-047 | `ActaService.guardarDetalle(...)` + `Nuc_CalculoImpIB` | `ActaAltaIT`, `ActaServiceTest#impuestoIb` |
| Pase (`F2276_RegPasePuestoV2`) | R-ACTA-048 | `ActaService.registrarPase(...)` | `ActaAltaIT` |
| `commit` / `rollback` | R-ACTA-049 | `@Transactional` en el servicio (contador + cabecera + detalle + pase) | `ActaAltaIT` |
| Paridad de columnas (54/15/9/5) | R-ACTA-050/51/52 | entidades + `@ColumnTransformer(read="RTRIM(...)")` | `ActaMapeoIT` |

---

## 6. Decisiones

Ver `spec.md` §7 (`D-ACTA-01` … `D-ACTA-21`). Resumen operativo:

| # | Decisión | Efecto en la implementación |
|---|---|---|
| D-ACTA-01 | Entidad nueva `Acta` (no se toca `Formulario2276`). | `puestos/domain/Acta.java` + `ActaId`. |
| D-ACTA-02 | Módulo en `atp-puestos-camineros`. | Rutas bajo el despliegue de Puestos. |
| D-ACTA-03 | Lógica en `ActaService`. | El controller sólo mapea y delega. |
| D-ACTA-04 | Sólo MVC. | Sin `web/rest`; Postman sin cambios. |
| D-ACTA-05 | Reutilizar `ServicioEstadoDeForm`. | Sin servicio de lotes nuevo. |
| D-ACTA-06 | Reutilizar `ContextoSesion` / `GestorContextoSesion` / `ProveedorNavegacion`. | Sin contexto propio. |
| D-ACTA-07 | Los totales y `Gen. Manual?` se derivan. | Sin `@Column` para atributos fórmula. |
| D-ACTA-08 | Mensajes verbatim. | Constantes en el servicio (o `MensajesActa`). |
| D-ACTA-09 | Plantillas en `templates/puestos/` sobre `layout/base`. | Sin layout nuevo. |
| D-ACTA-10 | `actas.css` + tokens `--atp-actas-*`. | Sin frameworks ni CDN. |
| D-ACTA-11 | Grilla de productos en línea con JS vanilla. | `static/js/actas.js`. |
| D-ACTA-12 | `POST /actas/guardar` con `@Valid` + `BindingResult`. | Errores por campo + banner de negocio. |
| D-ACTA-13 | Numeración en el servicio. | `ServicioNumeracionActa` (SA-ACTA-03 **cerrado**). |
| D-ACTA-14 | `confirm()` nativo. | JS puntual. |
| D-ACTA-15 | Impresión y PDF fuera. | Columna "Imprimir" sólo decorativa. |
| D-ACTA-16 | Página de 10. | `Pageable` con `size` por defecto 10. |
| D-ACTA-17 | Derivación por CUIT en el servidor; provincia no persistida. | Sin columna de provincia. |
| D-ACTA-18 | `F276GenWeb = 'A'` siempre. | Marca de acta web. |
| **D-ACTA-19** | **Numeración con incremento atómico sobre `GES_SUCN`**; si la fila no existe, insertar `(SucNroId, SucTipo, SucNuForm, 'MAN', 1)` y devolver `1`. | `ContadorFormulario` + `ContadorFormularioRepositorio.incrementarAtomico(...)`; `Propagation.MANDATORY` desde `ActaService.guardar`. |
| **D-ACTA-20** | `FORMSUCU` **se conserva** pero **sólo** para los rangos manuales (indicador "Gen. Manual?" y validación del número informado). | `RangoManualFormulario` + `RangoManualFormularioRepositorio`. |
| **D-ACTA-21** | `GES_SUCN.SucUltNum` es la fuente **autoritativa** de la numeración (aunque esté desfasada respecto de `MAX(F2276.F276Id)`). | No usar `MAX(F276Id)+1` (SA-ACTA-15). |
| **D-ACTA-22** | Los view models de la capa `web` NO son el contrato del servicio (G7: `web → service → repository → domain`). Los DTO de contrato viven en `service/model/**`; `web/mvc/form/**` contiene objetos de binding que el controller traduce. | `ActaFila`, `ActaFiltro`, `ActaAlta`, `ActaAltaProducto`, `ActaProductoDisponible`, `ActaPreparada`, `ResultadoAltaActa`, `DatosParte` en `service/model/**`; `ActaForm`/`ActaProductoForm`/`FiltroActasForm` en `web/mvc/form/**`. |
| **D-ACTA-23** | El mensaje `Ingrese todos los datos del Transporte` del WebPanel cubre los **5** campos del transporte (incluido `F276TCpbte` y `F276NCpbte`) y se evalúa **antes** de `&F2276.Save()`. Las rules de la transacción `F2276` (`Debe seleccionar un tipo de comprobante` / `Debe ingresar un nro. de comprobante` / `La fecha no puede ser superior a la fecha actual`) s�lo se evalúan si el check del WebPanel pasa. | `R-ACTA-030` (check combinado, 5 campos) gana; `R-ACTA-033` (rules) se mantiene como contrato de la transacción, alcanzable por otros puntos de entrada, pero inalcanzable en el flujo de alta manual. |

---

## 7. Supuestos abiertos

Ver `spec.md` §6 (`SA-ACTA-01` … `SA-ACTA-16`). Estado y bloqueos:

| ID | Impacto | Estado | Bloquea a |
|---|---|---|---|
| **SA-ACTA-03** | ~~Alto~~ — numeración del `F276Id` | **CERRADO (2026-11-04)**: `GES_SUCN.SucUltNum`, incremento atómico (D-ACTA-19/21). | — |
| SA-ACTA-02 | Medio — el WS `WSActa` decide el mensaje final del alta. | Abierto | T-ACTA-160 (mensaje de éxito) |
| SA-ACTA-04 | Medio — define qué es un acta "pendiente" en el cierre. | Abierto | T-ACTA-131 (cierre de lotes) |
| SA-ACTA-10 | Medio — decide la interacción de la grilla de productos. | Abierto | T-ACTA-157 (grilla de productos) |
| SA-ACTA-15 | Medio — desfase entre el contador `GES_SUCN` y `MAX(F2276.F276Id)`. | Abierto (decisión provisional: contador) | Revisión con DBA antes del cierre (T-ACTA-184) |
| SA-ACTA-06 | Bajo — valor de `F276RZona`. | Abierto | T-ACTA-124 (detalle) |
| SA-ACTA-14 | Bajo — default de `GES_SUCN.SucNuId`. | Abierto (provisional `'MAN'`) | T-ACTA-104 / T-ACTA-133 |
| SA-ACTA-16 | Bajo — significado funcional de "manual". | Abierto | — |

---

## 8. Alcance excluido

- Impresión del acta (`ActaPuesto_Chofer`) y su popup.
- Reporte PDF `ActaResumenTurno`.
- WS `WSActa` (`WsActaControlador`) y la generación del F506 / la multa.
- Transacciones `F2276_FormDef` y `F2276_Lotes` completas.
- WorkWiths (`F2276ListadoWC`, `F2276_F22761WC`, `PPrim_TurnosF2276WC`, `PPrim_TurnosF2276_FormDefWC`).
- Anulación de actas.
- Cálculo de impuestos de flete (`F2276_CalImpFl`, `F2276_CalTarFl`, `BuscarOrigenRetencion`) —
  los 3 **OUT** (ver `spec.md` §3.12).
- SN de casa central (`F2276GenerarSn`, `F2276BuscarSn`) — implementado en `06-turnos`.
- `ProPri_VerSoloIns` / `ProPRi_VerIns` (sus errores están comentados en el legacy).
- Endpoints REST del módulo.
- E2E automatizado.
