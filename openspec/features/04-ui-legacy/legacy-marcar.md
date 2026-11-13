# Hallazgos legacy — Control de Cargas / acción "Marcar" (marcar guía)

**Fuente:** KB GeneXus 18 `F:\Modelo\Gx18\ATPPuestos` (solo lectura).  
**Fecha de análisis:** 2026-11-13.  
**Objetivo:** comportamiento observable exacto de la acción "Marcar" en el legacy, para
validar el modal Stitch "Modal Marcar Guía con Éxito - Control de Cargas"
(proyecto `6019790973415672987`, screen `594b77f59613474495d650825b5487da`).

## 1. Objetos GeneXus inventariados

| Objeto | Tipo | Ruta en KB | Rol |
|---|---|---|---|
| `ControlDeCargas` | WebPanel | `Root Module/Guias/ControlDeCargas` | Pantalla de control de cargas (listado + búsqueda + marcar). Descripción: "Listado p/Control De Cargas". |
| `MarcarGuia` | Procedure | `Root Module/Guias/MarcarGuia` | Lógica de marcar (parm in/out). |
| `Form506Detalle` | WebPanel | `Root Module/Guias/Form506Detalle` | Popup (modal) de detalle de guía, reutilizado en modo `UPD` para el éxito de marcar. |
| `Mensaje` | WebPanel | `Root Module/GeneralWeb/Mensaje` | Popup modal genérico de mensajes (usado por `ControlDeCargas` para "Usuario sin Puesto Asignado"). |

## 2. Variables relevantes (ControlDeCargas)

- `&Context` (GX_SDT) — contexto de sesión (Sucursal, Usuario, Tipo, Comision).
- `&DPPNroForm` (CHAR, based on `DPPNroForm`) — nro. de guía.
- `&DPPTipoGui` (CHAR, based on `DPPTipoGui`) — tipo de guía (`F` = 509, otro = 506).
- `&Duplicado` (NUMERIC) — contador de pases previos con estado `C` en puesto `PUE`.
- `&BarCode` (CHAR) — código de barras (se extrae `substr(&BarCode,22,10)` → `&DPPNroForm`).
- `&Marcar` (BITMAP) — imagen de acción en la grilla.
- `&Mensaje` (CHAR) — texto para el popup `Mensaje`.

Variables de `Form506Detalle`: `&Duplicado` (NUMERIC), `&Mode` (CHAR), más atributos de la guía.

## 3. Flujo legacy exacto

### 3.1 Pantalla ControlDeCargas (WebPanel)

Eventos (verbatim):

```
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
    &Duplicado = MarcarGuia.Udp(ConCuit,DPPSucu,CalPerFisc,DPPSecu,DPPNroForm,&Context.Usuario,&Context.Tipo,&Context.Sucursal)
EndEvent

Event 'VerDetalle'
    &Duplicado = 0
    Form506Detalle.Popup(DPPNroForm,DPPTipoGui,&Duplicado,'DSP')
EndEvent

Event 'Ver Tabla'
    WWTDetProdPri.Call()
Endevent

Event 'Pagos'
    WpFormularioPago.Call()
Endevent
```

Notas del UI:
- Grilla `Grid506` (MaxRows=10) con columna de acción `&Marcar` (var:10) ligada al evento `'Marcar'`.
- Antes de marcar, se muestra confirmación JS: `confirm('Esta seguro que desea Marcar el formulario?')`.
- Si `&Context.Sucursal = 0` (sin puesto asignado), se muestra el popup `Mensaje` con tipo 3 (rojo) con texto `'Usuario sin Puesto Asignado'`. No se permite marcar (no se llega al evento Marcar).

### 3.2 Procedure MarcarGuia

Regla (parm): `parm(in:&ConCuit,in:&DPPSucu,in:&CalPerFisc,in:&DPPSecu,in:&DPPNroForm,in:&DPPPaseOperador,in:&SucTipo,in:&SucNroId,out:&Duplicado);`

Fuente (verbatim):

```
LoadContext(&Context)

For each
    Where ConCuit = &ConCuit
    Where DPPSucu = &DPPSucu
    Where CalPerFisc = &CalPerFisc
    Where DPPSecu = &DPPSecu
    Where DPPNroForm = &DPPNroForm
        &DPPMarcAnu = DPPMarcAnu
        &DPPClasif = DPPClasif

        Do Case
            Case &DPPMarcAnu = 1
                &Mensaje = 'No se puede marcar el pase de la guía porque fue anulada'
                msg(&Mensaje)
                return
            Case &DPPClasif = 'I'
                &Mensaje = 'No se puede marcar el pase de una guía interna'
                msg(&Mensaje)
                return
            Otherwise
                // Registra la última marca de pase a nivel cabecera
                DPPUltPSuN = &SucNroId
                DPPUltPSuT = &SucTipo
                DPPUltPaOp = &DPPPaseOperador

                Do 'MarcarGuia'
                Form506Detalle.Popup(DPPNroForm,DPPTipoGui,&Duplicado,'UPD')
        Endcase
Endfor
```

Sub `MarcarGuia` (verbatim):

```
&Indice = 1
&Duplicado = 1
for each
    where ConCuit = &ConCuit
    where DPPSucu = &DPPSucu
    where CalPerFisc = &CalPerFisc
    where DPPSecu = &DPPSecu
    where DPPNroForm = &DPPNroForm
    defined by DPPPaseFec
        &Indice += 1
        if DPPPaseEst = 'C' and SucTipo = 'PUE'
            &Duplicado += 1
        endif
endfor

New
    ConCuit = &ConCuit
    DPPSucu = &DPPSucu
    CalPerFisc = &CalPerFisc
    DPPSecu = &DPPSecu
    DPPNroForm = &DPPNroForm
    DPPPaseId = &Indice
    DPPPaseFec = &Today
    DPPPaseOpe = &DPPPaseOperador
    DPPPaseHor = servernow()
    DPPPaseEst = 'C'
    SucTipo = &SucTipo
    SucNroId = &SucNroId
    DPPPaseCo = &Context.Comision
EndNew
```

### 3.3 WebPanel Form506Detalle (popup) en modo `UPD`

Regla: `parm(in:DPPNroForm , in:DPPTipoGui , in:&Duplicado , in:&Mode);`

Event Start (verbatim):

```
if &Mode = 'UPD'
    Success.Caption = 'La guia ha sido marcada con exito'
    if &Duplicado > 1
        Error.Caption = 'Guia Duplicada , ya paso por algun puesto'
    endif
endif
if DPPTipoGui = 'F' // 509
    TblComprador.Visible = false
    TblProd.Visible = false
else // 506
    TblComprador.Visible = true
    TblProd.Visible = true
endif
```

UI de Form506Detalle:
- `Success` (gxTextBlock, ForeColor=Green) — mensaje de éxito.
- `Error` (gxTextBlock, ForeColor=Red, Font bold) — mensaje de duplicado.
- Muestra datos de la guía: Comprobante (combo: Carta de Porte/Factura/Remito/Doc.Equivalente), Nº, Fecha; Datos Comprador (Comprador, C.U.I.T., Domicilio); Datos Transportista (Transportista, C.U.I.T., Domicilio, Vehículo, Chasis, Acoplado); Tipo Producción (Agrícola y/o Forestal/Ganadero/Flete); Total Flete-Pago (KM Flete, Tarifa x KM, etc.); trazabilidad (att:332).
- Si `DPPTipoGui = 'F'` (509): oculta `TblComprador` y `TblProd`.

## 4. Comportamiento observable (EARS-friendly)

- **R-CC-MARCAR-001 (éxito):** CUANDO el usuario confirma el marcar de una guía válida
  (no anulada, no interna) y el puesto está asignado, EL SISTEMA DEBE actualizar la cabecera
  (`DPPUltPSuN`, `DPPUltPSuT`, `DPPUltPaOp`), insertar un nuevo registro de pase en
  `DETPRODP` con `DPPPaseEst = 'C'`, `DPPPaseFec = today`, `DPPPaseHor = servernow()`,
  `DPPPaseOpe = operador`, `DPPPaseCo = comisión`, y ABRIR el popup (modal)
  `Form506Detalle` en modo `UPD`.

- **R-CC-MARCAR-002 (modal de éxito):** EN EL MODAL de éxito (`Form506Detalle`, modo `UPD`),
  EL SISTEMA DEBE mostrar el texto (verbatim, sin acentos, como en la KB)
  `La guia ha sido marcada con exito` en color verde, y ADEMÁS, SI `&Duplicado > 1`,
  mostrar `Guia Duplicada , ya paso por algun puesto` en rojo (bold).

- **R-CC-MARCAR-003 (modal vs banner):** EL ÉXITO DE MARCAR SE MUESTRA COMO UN MODAL (popup
  `Form506Detalle`), NO como un banner/mensaje. El modal Stitch "Modal Marcar Guía con Éxito -
  Control de Cargas" corresponde a este popup y replica los textos
  "La guía ha sido marcada con éxito" y "Duplicada, ya pasó por algún puesto".

- **R-CC-MARCAR-004 (guía anulada):** CUANDO la guía tiene `DPPMarcAnu = 1`,
  EL SISTEMA DEBE mostrar el mensaje (no modal, `msg()`)
  `No se puede marcar el pase de la guía porque fue anulada` y NO abrir `Form506Detalle`,
  NO insertar pase, NO actualizar cabecera.

- **R-CC-MARCAR-005 (guía interna):** CUANDO la guía tiene `DPPClasif = 'I'`,
  EL SISTEMA DEBE mostrar el mensaje (no modal, `msg()`)
  `No se puede marcar el pase de una guía interna` y NO abrir `Form506Detalle`,
  NO insertar pase, NO actualizar cabecera.

- **R-CC-MARCAR-006 (sin puesto):** CUANDO `&Context.Sucursal = 0`,
  EL SISTEMA DEBE mostrar el popup `Mensaje` (tipo 3, rojo) con
  `Usuario sin Puesto Asignado` y NO permitir marcar.

- **R-CC-MARCAR-007 (confirmación):** ANTES de ejecutar el marcar, EL SISTEMA DEBE
  mostrar la confirmación JS `confirm('Esta seguro que desea Marcar el formulario?')`.

- **R-CC-MARCAR-008 (duplicado):** `&Duplicado` se calcula contando los pases previos
  (`DPPPaseEst = 'C'` y `SucTipo = 'PUE'`) + 1. Si > 1, la guía se considera duplicada
  y se muestra la leyenda correspondiente.

## 5. Confirmación Stitch (modal de éxito)

Screen Stitch: "Modal Marcar Guía con Éxito - Control de Cargas"
(proyecto `6019790973415672987`, screen `594b77f59613474495d650825b5487da`).
Textos extraídos del HTML Stitch:
- "La guía ha sido marcada con éxito"
- "Duplicada, ya pasó por algún puesto"

Coinciden (con normalización ortográfica) con los textos legacy de `Form506Detalle`
(modo `UPD`). Conclusión: el legacy muestra un MODAL de éxito (popup `Form506Detalle`),
no un banner. El modal Stitch es la evolución visual de ese popup.

## 6. Supuestos abiertos

1. `msg()` en GeneXus web se renderiza como un mensaje estándar (banner/alert) y NO como
   un popup WebPanel. A confirmar contra el runtime real del legacy (si es alert JS o
   mensaje en `gxErrorViewer`). La evidencia indirecta: `ControlDeCargas` usa `Mensaje.Call`
   (popup modal) para "Usuario sin Puesto Asignado" y `msg()` (no popup) para anulada/interna.
2. `&Context.Usuario` se mapea a `DPPPaseOperador` (operador) y `&Context.Tipo`/`&Context.Sucursal`
   a `SucTipo`/`SucNroId`. Confirmar tipos exactos del SDT `Context`.
3. La tabla física donde se hace `New` es `DETPRODP` (pases). Confirmar contra
   `INFORMATION_SCHEMA` (columnas `DPPPaseId`, `DPPPaseFec`, `DPPPaseOpe`, `DPPPaseHor`,
   `DPPPaseEst`, `SucTipo`, `SucNroId`, `DPPPaseCo`, `DPPUltPSuN`, `DPPUltPSuT`, `DPPUltPaOp`,
   `DPPMarcAnu`, `DPPClasif`).
4. El popup `Form506Detalle` se abre como modal (`.Popup()`). En Thymeleaf se debe replicar
   como un modal/fragmento con los mismos datos y mensajes. Ver módulo `04-modal-detalle-guia`
   para la evolución visual.

## 7. Riesgos

- Textos legacy sin acentos vs. Stitch con acentos: el contrato observable debe fijar
  una variante. Recomendación: usar la variante con acentos (Stitch/moderno) pero mantener
  paridad con legacy si se requiere exactitud byte-a-byte.
- El `confirm()` JS es contrato observable: debe mantenerse o justificarse su eliminación.
- `msg()` vs modal: si se migra a Thymeleaf, el caso de error debe ser un banner/mensaje
  inline, no un modal, para preservar la distinción legacy.