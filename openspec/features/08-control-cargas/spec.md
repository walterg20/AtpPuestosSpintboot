# Spec — 08 Control de Cargas (Puestos Camineros)

> **Estado**: `APROBADA (revisión v1, 2026-10-30)` — puerta **G-SPEC** cumplida.
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-CC-<NN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13.186702), alias `atppuestos` — **solo lectura**.
> - `WebPanel ControlDeCargas` (`Root Module/Guias/ControlDeCargas`).
> - `Procedure MarcarGuia` (`Root Module/Guias/MarcarGuia`).
> - `WebPanel Form506Detalle` (`Root Module/Guias/Form506Detalle`).
> - `WebPanel WWTDetProdPri` (`Root Module/Guias/WWTDetProdPri`).
> - `WebPanel WPFormularioPago` (`Root Module/GeneralWeb/Disenos/WPFormularioPago`).
> - `Transaction DetProdP` (2 niveles: `DetProdP`/`DetProdPri` + `DetProd2`/`Level2`).
> - `Transaction Contribu` (`Contribuyentes`).
> - SDT `Context` (13 campos, 10 cargados por login).
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` (G-DATA 2026-10-30, sólo `SELECT`).
> **Backend ya implementado**: `atp-nucleo-compartido` (catálogos, login, contexto de sesión, seguridad).
>   Este módulo **consume** ese contrato; no lo rediseña (`openspec/features/03-login/spec.md`).
> **Alcance de escritura**: `openspec/features/**` (esta spec) y, en la fase de implementación,
>   `atp-puestos-camineros/**` (dominio de cargas). **Sin DDL, sin migraciones** (G5).
> **Alcance de datos**: sólo lectura sobre `CONTRIBU`, `DETPRODP`, `DETPROD2`; la escritura del
>   pase (nivel 2) la realiza el servicio `MarcarGuia` sobre `DETPROD2`.

---

## 1. Propósito

Entregar la **pantalla Thymeleaf** de **Control de Cargas** de Puestos Camineros, replicando el
comportamiento observable del `WebPanel ControlDeCargas` del legacy:

- validación del puesto asignado al usuario al entrar (`Sucursal = 0` ⇒ `Usuario sin Puesto Asignado`);
- búsqueda de un formulario por **código de barras** (extracción de los 10 dígitos del número de
  formulario a partir de la posición 22 del barcode);
- grilla de formularios con acción de **marcar** (registrar el pase del puesto);
- **ver detalle** del formulario (popup `Form506Detalle`);
- **ver tabla** de detalle (`WWTDetProdPri`);
- **pagos** (`WPFormularioPago`).

El módulo es el **único** del replatform que toca las tablas `CONTRIBU` y `DETPRODP`/`DETPROD2`
para la operación de marcado. El **reporte en PDF** queda **fuera de alcance** (OUT) y se
integra más adelante.

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Pantalla `Control de Cargas` en Thymeleaf con validación de puesto al entrar. |
| 2 | Campo de código de barras con extracción de `DPPNroForm` (10 dígitos desde la posición 22). |
| 3 | Grilla de formularios con columna de acción **Marcar** (bitmap `selectRow`). |
| 4 | Acción **Marcar** ⇒ invoca `MarcarGuia` y muestra el resultado (éxito / duplicado / anulada / interna). |
| 5 | Acción **Ver Detalle** ⇒ popup `Form506Detalle` (modo `DSP` / `UPD`). |
| 6 | Acción **Ver Tabla** ⇒ navegación a `WWTDetProdPri`. |
| 7 | Acción **Pagos** ⇒ navegación a `WPFormularioPago`. |
| 8 | JavaScript puntual (vanilla): autofocus del barcode, confirmación de marcado. |
| 9 | Paridad 1:1 con `CONTRIBU`, `DETPRODP` y `DETPROD2` (columnas, PK, FK, huérfanas). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Reporte en PDF de cargas | Se integra más adelante; no forma parte de este módulo | Backlog (módulo futuro) |
| ABM de `Contribu` (contribuyentes) | Fuera del dominio de cargas | `01-nucleo-datos` / backlog |
| ABM de `DetProdP` (cabecera de guía) | Se gestiona por otros módulos; aquí sólo se **lee** y se **marca** el pase | Backlog |
| Autenticación / autorización de la pantalla | La define `03-login` (Spring Security + CSRF) | `03-login` |
| E2E automatizado | — | Fuera del módulo |

## 3. Modelo legacy verificado

### 3.1 WebPanel `ControlDeCargas` (eventos)

Extracción literal de `genexus_analyze({name:"ControlDeCargas", mode:"deep_context"})`:

```text
Event Start
    LoadContext(&Context)
    &Marcar.JSEvent('onclick',"confirm('Esta seguro que desea Marcar el formulario?')")
    if &Context.Sucursal = 0
        &Mensaje = 'Usuario sin Puesto Asignado'
        Mensaje.Call(&Mensaje,3)
    endif
Endevent

Event 'Buscar'
    if not &BarCode.IsEmpty()
        &DPPNroForm = substr(&BarCode,22,10)
    endif
    Grid506.Refresh()
Endevent

Event Grid506.Load
    &Marcar.FromImage(selectRow)
EndEvent

Event 'Marcar'
    &Duplicado = MarcarGuia.Udp(ConCuit,DPPSucu,CalPerFisc,DPPSecu,DPPNroForm,
                                &Context.Usuario,&Context.Tipo,&Context.Sucursal)
EndEvent

Event 'VerDetalle'
    &Duplicado = 0
    Form506Detalle.Popup(DPPNroForm,DPPTipoGui,&Duplicado,'DSP')
EndEVent

Event 'Ver Tabla'
    WWTDetProdPri.Call()
Endevent

Event 'Pagos'
    WpFormularioPago.Call()
Endevent
```

### 3.2 Variables del WebPanel

| Variable | Tipo legacy | Longitud | Uso |
|---|---|---|---|
| `&ConCuit` | `NUMERIC` | 11 | CUIT del contribuyente (clave de la guía) |
| `&ConRazSoc` | `CHARACTER` | 35 | Razón social del contribuyente |
| `&Context` | `GX_SDT` (Context) | 4 | Contexto de sesión (13 campos) |
| `&DPPNroForm` | `CHARACTER` | 10 | Número de formulario extraído del barcode |
| `&DPPTipoGui` | `CHARACTER` | 1 | Tipo de guía (`F` = 509, otro = 506) |
| `&Duplicado` | `NUMERIC` | 3 | Código de resultado del marcado |
| `&Marcar` | `BITMAP` | 4 | Icono de acción (bitmap `selectRow`) |
| `&Mensaje` | `CHARACTER` | 30 | Mensaje para el popup `Mensaje` |
| `&BarCode` | `CHARACTER` | 31 | Código de barras escaneado |

### 3.3 Procedure `MarcarGuia` — lógica de marcado

**Firma**: `parm(in:&ConCuit, in:&DPPSucu, in:&CalPerFisc, in:&DPPSecu, in:&DPPNroForm,
in:&DPPPaseOperador, in:&SucTipo, in:&SucNroId, out:&Duplicado)`.

**Comportamiento observable** (extraído verbatim):

1. Carga el contexto de sesión (`LoadContext`).
2. **Barrido nivel 1** (`For each` sobre `DetProdP` con filtros por `ConCuit`, `DPPSucu`,
   `CalPerFisc`, `DPPSecu`, `DPPNroForm`):
   - Lee `&DPPMarcAnu` y `&DPPClasif`.
   - **Si `DPPMarcAnu = 1`** ⇒ mensaje `'No se puede marcar el pase de la guía porque fue anulada'`,
     `msg`, `return`.
   - **Si `DPPClasif = 'I'`** ⇒ mensaje `'No se puede marcar el pase de una guía interna'`,
     `msg`, `return`.
   - En otro caso:
     - Actualiza la cabecera: `DPPUltPSuN = &SucNroId`, `DPPUltPSuT = &SucTipo`,
       `DPPUltPaOp = &DPPPaseOperador`.
     - Ejecuta el sub `MarcarGuia` (nivel 2) y abre `Form506Detalle.Popup(..., 'UPD')`.
3. **Sub `MarcarGuia`** (nivel 2):
   - `&Indice = 1`, `&Duplicado = 1`.
   - Barrido nivel 2 (`for each` sobre `DetProd2` con los mismos filtros, `defined by DPPPaseFec`):
     - `&Indice += 1`.
     - **Si `DPPPaseEst = 'C'` y `SucTipo = 'PUE'`** ⇒ `&Duplicado += 1`.
   - `New` sobre `DetProd2`:
     - `ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm` (PK del nivel 1).
     - `DPPPaseId = &Indice`.
     - `DPPPaseFec = &Today`.
     - `DPPPaseOpe = &DPPPaseOperador`.
     - `DPPPaseHor = servernow()`.
     - `DPPPaseEst = 'C'`.
     - `SucTipo = &SucTipo`.
     - `SucNroId = &SucNroId`.
     - `DPPPaseCo = &Context.Comision`.

**Regla de negocio clave**: `&Duplicado` cuenta cuántos pases **confirmados** (`'C'`) existen
**previamente** para puestos de tipo `'PUE'`; si `&Duplicado > 1` ⇒ la guía es **duplicada**
(ya pasó por algún puesto). El valor se devuelve al caller.

### 3.4 WebPanel `Form506Detalle` — detalle del formulario

**Firma**: `parm(in:DPPNroForm, in:DPPTipoGui, in:&Duplicado, in:&Mode)`.

**Eventos**:
- `Start`:
  - Si `&Mode = 'UPD'`:
    - `Success.Caption = 'La guia ha sido marcada con exito'`.
    - Si `&Duplicado > 1` ⇒ `Error.Caption = 'Guia Duplicada , ya paso por algun puesto'`.
  - Si `DPPTipoGui = 'F'` (509) ⇒ `TblComprador.Visible = false`, `TblProd.Visible = false`.
  - Si no (506) ⇒ `TblComprador.Visible = true`, `TblProd.Visible = true`.

**Referencia**: tabla `DetProdP` (cabecera, sólo lectura en esta vista).

### 3.5 WebPanel `WWTDetProdPri` — tabla de detalle

**Firma**: sin parámetros (llamada simple).

**Eventos**:
- `Start`: autorización (`IsAuthorized`), `Grid.Rows = 10`, título `'Buscar Guias'`.
- `Refresh`: guarda estado de la grilla.
- `Grid.Load`: enlaces a `TDetProdPri` (display) y `ViewTDetProdPri` (por `ConCuit`, `DPPSucu`, `CalPerFisc`).
- Condiciones: `DPPNroForm = &DPPNroForm` (filtro por número de formulario).

**Referencia**: tabla `DetProdP` (cabecera) + nivel 2 `DetProd2` (pases).

**Autorización**: `IsAuthorized(&PgmName)`; si no ⇒ `NotAuthorized`.

### 3.6 WebPanel `WPFormularioPago` — pagos

**Firma**: sin parámetros (llamada simple).

**Eventos**:
- `Start`: `&Imprimir.FromImage(impresora)`.
- `Grid1.Load`: `&Imprimir.Link = F2276VolanteDePago.Link(SNSinNum)`.

**Referencia**: ninguna tabla directa (sólo impresión de volante de pago).

### 3.7 SDT `Context` (13 campos)

| # | Campo | Tipo legacy | Cargado por login |
|---|---|---|---|
| 1 | `Usuario` | `CHARACTER(10)` | Sí |
| 2 | `UsuarioNombre` | `UsuarioNom` | Sí |
| 3 | `PersonaId` | `NUMERIC(5)` | Sí |
| 4 | `Comision` | `NUMERIC(6)` | Sí |
| 5 | `Turno` | `TurNroId` | Sí |
| 6 | `Sucursal` | `CodNumCor` | Sí |
| 7 | `SucursalNombre` | `CHARACTER(30)` | Sí |
| 8 | `Tipo` | `CHARACTER(3)` | Sí |
| 9 | `Responsable` | `NUMERIC(5)` | Sí |
| 10 | `ResponsableNombre` | `Nombre` | Sí |
| 11 | `Form2276` | `SdtF2276` | No (default) |
| 12 | `F276Id` | `F276Id` | No (default) |
| 13 | `UltimoMenu` | `CHARACTER(30)` | No (default) |

El login carga los **10 primeros**; los 3 restantes quedan en su valor por defecto.

### 3.8 Tablas referenciadas

#### `Contribu` (Contribuyentes) — `CONTRIBU`

- **PK**: `ConCuit` (`decimal(11,0)`, NOT NULL).
- **Uso en este módulo**: lectura de `ConRazSoc` (razón social) para mostrar en la grilla de
  formularios; el CUIT es la clave de la guía (`ConCuit`).
- **Columnas huérfanas** (en la BD, no en la KB): ver §5.2.

#### `DetProdP` (DetProdPri) — `DETPRODP` (nivel 1, cabecera)

- **PK compuesta**: (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm`).
- **Uso en este módulo**: lectura de la cabecera de la guía (campos `DPPMarcAnu`, `DPPClasif`,
  `DPPTipoGui`, `DPPUltPSuN`, `DPPUltPSuT`, `DPPUltPaOp`, etc.) y actualización de los campos
  `DPPUltPSuN`/`DPPUltPSuT`/`DPPUltPaOp` al marcar.
- **FK física**: `DETPRODP.PPTrataId` → `PPTrata.PPTrataId`.
- **Columnas huérfanas**: ver §5.2.

#### `DetProd2` (Level2, pases) — `DETPROD2` (nivel 2)

- **PK compuesta**: (`ConCuit`, `DPPSucu`, `CalPerFisc`, `DPPSecu`, `DPPNroForm`, `DPPPaseId`).
- **Uso en este módulo**: inserción de un nuevo pase (`New` en `MarcarGuia`) con los campos
  `DPPPaseId`, `DPPPaseFec`, `DPPPaseOpe`, `DPPPaseHor`, `DPPPaseEst`, `SucTipo`, `SucNroId`,
  `DPPPaseCo`.
- **Columnas huérfanas**: `DPPPaseCo` es nullable (default de aplicación: `0` o null según el
  contexto).

### 3.9 Mensajes verbatim

| # | Texto (verbatim) | Dónde aparece |
|---|---|---|
| 1 | `Usuario sin Puesto Asignado` | `ControlDeCargas.Start` (cuando `Sucursal = 0`) |
| 2 | `No se puede marcar el pase de la guía porque fue anulada` | `MarcarGuia` (cuando `DPPMarcAnu = 1`) |
| 3 | `No se puede marcar el pase de una guía interna` | `MarcarGuia` (cuando `DPPClasif = 'I'`) |
| 4 | `La guia ha sido marcada con exito` | `Form506Detalle.Start` (modo `UPD`) |
| 5 | `Guia Duplicada , ya paso por algun puesto` | `Form506Detalle.Start` (modo `UPD`, `Duplicado > 1`) |
| 6 | `Esta seguro que desea Marcar el formulario?` | `ControlDeCargas.Start` (confirmación JS del botón Marcar) |

> Los textos se conservan **verbatim**, incluidas las erratas (`guia` sin tilde, `Duplicada ,` con
> espacio antes de la coma). La paridad de identidad manda.

### 3.10 Regla de la grilla (Grid506)

- La grilla se refresca al pulsar **Buscar** (`Grid506.Refresh()`).
- En `Grid506.Load` se asigna el icono `selectRow` a la columna `&Marcar` (bitmap).
- La grilla muestra formularios filtrados por `DPPNroForm` (extraído del barcode).

## 4. Requisitos EARS

### 4.1 Pantalla y validación de puesto

#### R-CC-01 — Pantalla de Control de Cargas
> **EL SISTEMA DEBE** exponer en la ruta de página `/cargas` (o `/control-cargas`) la pantalla de
> Control de Cargas con las acciones Buscar, Marcar, Ver Detalle, Ver Tabla y Pagos, sustentada en
> el servicio `CargaService` (nunca en llamadas HTTP a la propia API).

* *Criterio de verificación*: `@WebMvcTest` del controller MVC que comprueba `GET /cargas` → 200 +
  vista `puestos/control-cargas` + atributos del view model; IT contra `Rentas` con rollback.

#### R-CC-02 — Validación de puesto al entrar
> **CUANDO** el usuario acceda a la pantalla y su contexto de sesión tenga `Sucursal = 0`,
> **EL SISTEMA DEBE** mostrar el mensaje verbatim `Usuario sin Puesto Asignado` mediante el popup
> `Mensaje` (tipo 3) y **NO DEBE** permitir la operación de marcado.

* *Criterio de verificación*: test que simula `Context.Sucursal = 0` y verifica el mensaje renderizado
  y la deshabilitación de la acción Marcar. Test con `Sucursal ≠ 0` que verifica que no se muestra
  el mensaje.

#### R-CC-03 — Confirmación de marcado
> **CUANDO** el usuario active la acción Marcar sobre un formulario, **EL SISTEMA DEBE** solicitar
> confirmación explícita con el texto verbatim `Esta seguro que desea Marcar el formulario?` y
> **NO DEBE** ejecutar el marcado si el usuario cancela.

* *Criterio de verificación*: prueba de navegador que cancela la confirmación y comprueba que no se
  ejecuta `MarcarGuia`. El `confirm()` nativo es JS puntual permitido.

### 4.2 Búsqueda por código de barras

#### R-CC-04 — Extracción del número de formulario desde el barcode
> **CUANDO** el usuario ingrese un código de barras no vacío y active Buscar, **EL SISTEMA DEBE**
> extraer los 10 dígitos del número de formulario a partir de la posición 22 del barcode
> (`substr(barcode, 22, 10)`) y **DEBE** usar ese valor como filtro de la grilla.

* *Criterio de verificación*: test con `barcode = "..."` (31 caracteres) que verifica que
  `DPPNroForm` contiene los 10 dígitos esperados. Test con barcode vacío que verifica que no se
  modifica `DPPNroForm`.

#### R-CC-05 — Refresco de la grilla
> **CUANDO** el usuario active Buscar, **EL SISTEMA DEBE** refrescar la grilla de formularios
> (`Grid506.Refresh()`) aplicando el filtro por `DPPNroForm` extraído.

* *Criterio de verificación*: test que comprueba que tras Buscar la grilla se recarga con el filtro
  aplicado.

#### R-CC-06 — Icono de acción en la grilla
> **EL SISTEMA DEBE** mostrar en cada fila de la grilla el icono de acción Marcar (`selectRow`)
> como bitmap, asociado a la columna `&Marcar`.

* *Criterio de verificación*: test que verifica el render del icono en cada fila.

### 4.3 Operación de marcado

#### R-CC-07 — Marcado de formulario (éxito)
> **CUANDO** el usuario confirme el marcado de un formulario válido (no anulado, no interno),
> **EL SISTEMA DEBE** registrar el pase del puesto (nivel 2 de `DetProdP`) con los campos
> `DPPPaseId`, `DPPPaseFec` (fecha actual), `DPPPaseOpe` (operador), `DPPPaseHor` (hora del
> servidor), `DPPPaseEst = 'C'`, `SucTipo`, `SucNroId` y `DPPPaseCo` (comisión), y actualizar la
> cabecera (`DPPUltPSuN`, `DPPUltPSuT`, `DPPUltPaOp`), y mostrar `La guia ha sido marcada con exito`.

* *Criterio de verificación*: IT contra `Rentas` que verifica la inserción en `DETPROD2` y la
  actualización de `DETPRODP`, con rollback.

#### R-CC-08 — Guía anulada
> **SI** el formulario a marcar tiene `DPPMarcAnu = 1`, **ENTONCES EL SISTEMA DEBE** mostrar el
> mensaje verbatim `No se puede marcar el pase de la guía porque fue anulada` y **NO DEBE**
> registrar el pase.

* *Criterio de verificación*: test que simula `DPPMarcAnu = 1` y verifica el mensaje y la ausencia
  de inserción en `DETPROD2`.

#### R-CC-09 — Guía interna
> **SI** el formulario a marcar tiene `DPPClasif = 'I'`, **ENTONCES EL SISTEMA DEBE** mostrar el
> mensaje verbatim `No se puede marcar el pase de una guía interna` y **NO DEBE** registrar el
> pase.

* *Criterio de verificación*: test que simula `DPPClasif = 'I'` y verifica el mensaje y la ausencia
  de inserción.

#### R-CC-10 — Guía duplicada
> **CUANDO** el marcado se complete y `Duplicado > 1`, **EL SISTEMA DEBE** mostrar adicionalmente
> el mensaje verbatim `Guia Duplicada , ya paso por algun puesto` en el detalle.

* *Criterio de verificación*: IT que verifica que `Duplicado` se calcula correctamente (cuenta
  pases previos con `DPPPaseEst = 'C'` y `SucTipo = 'PUE'`) y que el mensaje se muestra cuando
  `Duplicado > 1`.

#### R-CC-11 — Cálculo de `Duplicado`
> **EL SISTEMA DEBE** calcular `Duplicado` como `1 +` la cantidad de pases previos con
> `DPPPaseEst = 'C'` y `SucTipo = 'PUE'` para la misma guía (misma PK de nivel 1), y **DEBE**
> devolver ese valor al caller.

* *Criterio de verificación*: test unitario con 0, 1 y 2 pases previos que verifica el valor
  devuelto.

### 4.4 Ver detalle

#### R-CC-12 — Ver detalle del formulario (modo DSP)
> **CUANDO** el usuario active Ver Detalle, **EL SISTEMA DEBE** abrir el popup `Form506Detalle`
> con `Mode = 'DSP'`, mostrando la cabecera de la guía y, según `DPPTipoGui`, mostrando u
> ocultando las tablas de comprador y de productos.

* *Criterio de verificación*: test que verifica la apertura del popup con los parámetros correctos y
  la visibilidad condicional (`DPPTipoGui = 'F'` ⇒ ocultas; otro ⇒ visibles).

#### R-CC-13 — Ver detalle tras marcar (modo UPD)
> **CUANDO** el marcado se complete exitosamente, **EL SISTEMA DEBE** abrir `Form506Detalle`
> con `Mode = 'UPD'`, mostrando `La guia ha sido marcada con exito` y, si `Duplicado > 1`,
> adicionalmente `Guia Duplicada , ya paso por algun puesto`.

* *Criterio de verificación*: test que verifica la apertura del popup con `Mode = 'UPD'` y los
  mensajes correspondientes.

### 4.5 Ver tabla y pagos

#### R-CC-14 — Ver tabla de detalle
> **CUANDO** el usuario active Ver Tabla, **EL SISTEMA DEBE** navegar a la pantalla
> `WWTDetProdPri` (tabla de detalle de productos primarios), con autorización verificada.

* *Criterio de verificación*: test que verifica la navegación y la llamada a `IsAuthorized`.

#### R-CC-15 — Pagos
> **CUANDO** el usuario active Pagos, **EL SISTEMA DEBE** navegar a la pantalla
> `WPFormularioPago` (formulario de pago).

* *Criterio de verificación*: test que verifica la navegación.

### 4.6 No funcionales

#### R-CC-16 — No regresión del contrato REST
> **CUANDO** se implemente la pantalla, **EL SISTEMA DEBE** mantener sin cambios los endpoints
> existentes y **NO DEBE** modificar `web/rest/**` (salvo que se agreguen endpoints nuevos de
> cargas, que deben reflejarse en Postman).

* *Criterio de verificación*: `git diff` sin cambios en `nucleo/web/rest/**` salvo endpoints nuevos
  documentados; colección Postman sincronizada (G-POSTMAN).

#### R-CC-17 — Responsive
> **EL SISTEMA DEBE** mantener la grilla de formularios utilizable en 375 px y 1440 px.

* *Criterio de verificación*: inspección visual en los 2 anchos.

#### R-CC-18 — Accesibilidad AA
> **EL SISTEMA DEBE** mantener un contraste ≥ 4,5:1, asociar un `<label>` a cada control y
> garantizar foco visible y navegación por teclado.

* *Criterio de verificación*: `axe-core` in-page sin hallazgos críticos.

#### R-CC-19 — Sincronización de la colección Postman
> **SI** la implementación agrega, modifica o elimina un endpoint del contrato REST,
> **ENTONCES EL SISTEMA DEBE** actualizar `docs/postman/ATPPuestos-API.postman_collection.json`
> en el mismo commit.

* *Criterio de verificación*: revisión del diff del commit.

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
| Icono de acción en grilla | R-CC-06 | `MvcIconoAccion` |
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

## 7. Decisiones

| # | Decisión | Justificación |
|---|---|---|
| D-CC-01 | La pantalla se implementa en `atp-puestos-camineros` (no en el núcleo) | Es dominio de Puestos Camineros, no catálogo compartido |
| D-CC-02 | `MarcarGuia` se implementa como servicio (`CargaService.marcarGuia`) | La lógica de negocio va en `service/**` (AGENTS.md regla 9) |
| D-CC-03 | La grilla se refresca por submit del formulario (no por AJAX) | Thymeleaf renderiza por servidor; JS puntual sólo para barcode y confirmación |
| D-CC-04 | El popup `Form506Detalle` se implementa como fragmento Thymeleaf modal | Evita duplicar la lógica de negocio en un controller separado |
| D-CC-05 | Las columnas huérfanas se mapean con `@Column(name="...")` y default null/0 | Paridad de BD (G-DATA), sin rediseño (G5) |
| D-CC-06 | Los mensajes verbatim se conservan con sus erratas | Paridad de identidad (docs/design.md §1.1) |
