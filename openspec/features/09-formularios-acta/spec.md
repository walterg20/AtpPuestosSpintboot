# Spec — 09 Formularios Acta (Puestos Camineros)

> **Estado**: `BORRADOR (v2, 2026-11-04)` — pendiente de aprobación humana (puerta **G-SPEC**).
> **Revisión v2**: G-DATA verificado contra `Rentas` (54/15/9/5 columnas confirmadas) y
> **SA-ACTA-03 cerrado** (numeración sobre `GES_SUCN`, ver §3.11); 15 procedures verificados
> verbatim en §3.12; 3 decisiones nuevas (D-ACTA-19/20/21) y 3 supuestos nuevos
> (SA-ACTA-14/15/16).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-ACTA-<NNN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13), alias `atppuestos` — **solo lectura**.
> - `WebPanel ActaListadoWC` (`Root Module/F2776/ActaListadoWC`).
> - `WebPanel PPActaDetalle` (`Root Module/F2776/PPActaDetalle`) — formulario de alta.
> - `WebPanel ActaCargarDetalle` (`Root Module/F2776/ActaCargarDetalle`) — alta de detalle.
> - `WebComponent ActaListadoWC_turno` (`Root Module/F2776/ActaListadoWC_turno`).
> - `Transaction F2276` (`Root Module/F2776/F2276`) — "Formulario 2276 desde Receptorias" (2 niveles + pase).
> - `Procedure ActaDeterminaTipoFormulario`, `F2276_GenNroForm`, `F2276_RecUltNroForm`,
>   `ProPri_VerManual`, `PPrim_VerificaGenMan`, `F2276_BusProTarifa`, `F2276_BusProTarCM`,
>   `F2276_RegPasePuestoV2`, `Nuc_CalculoImpIB`, `PPrim_TurnoEstado`, `BuscarVersionActual`.
> - `DataProvider DPDolphinMenuActas` (menú: Control de Cargas / Formularios ACTAS / Carga Manual / Turnos / Comisiones).
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` — **G-DATA 2026-11-04, sólo `SELECT`**
>   (`INFORMATION_SCHEMA.COLUMNS`, `KEY_COLUMN_USAGE`, `sys.tables`, `sys.foreign_keys`).
> **Backend ya implementado que este módulo consume (no se rediseña)**:
>   `atp-nucleo-compartido` → `Formulario2276` (proyección de lectura de 11 columnas), `Formulario2276Repositorio`,
>   `ServicioEstadoDeForm`, `ServicioTurnos`, `ServicioGeneradorSn`, `ServicioCierreTurno`,
>   `ContextoSesion` / `GestorContextoSesion`, `ProveedorNavegacion`, `Producto` / `ProductoVigencia`;
>   `atp-puestos-camineros` → `Contribuyente` (CONTRIBU), `CargaService` (patrón de servicio).
> **Alcance de escritura de esta spec**: `openspec/features/09-formularios-acta/**`.
>   En la fase de implementación: `atp-puestos-camineros/**` (dominio, repositorio, servicio, web MVC,
>   plantillas, CSS/JS). **Sin DDL, sin migraciones** (G5).
> **Datos**: escritura sobre `F2276`, `F22761`, `F2276PAS`; lectura sobre `F2276`, `F22761`, `F2276PAS`,
>   `FORMSUCU`, `LOTES227`, `PRODUCTO`, `PRODUCT1`, `CONTRIBU`, `LOCALIDA`, `PROVINCI`.

---

## 1. Propósito

Entregar **dos pantallas Thymeleaf** del módulo de Formularios Acta de Puestos Camineros,
replicando el comportamiento observable del legacy:

1. **Pantalla principal** (`actas-lista.html`): grilla de las Actas del turno con paginación,
   filtros de búsqueda y botón **"Crear Acta"** destacado arriba a la derecha que navega al
   formulario de alta.
2. **Formulario de alta** (`acta-formulario.html`): carga manual de un Acta (Formulario 2276),
   con los fieldsets del Figma `ztMkGHkjqvQL2TOjHP1DSN` nodo `2030:2` ("Carga manual · Acta").

El módulo **escribe** la cabecera (`F2276`), el detalle de productos (`F22761`) y el pase del
puesto caminero (`F2276PAS`), y **deriva** en el servicio los importes que en la KB son atributos
fórmula (`F276ImIBTot`, `F276ImCMTot`, `F276AdiTot`, `F276ImpTotal`, `F276Total`, `F276ProCan`,
`F276GenMan`), porque **no son columnas físicas** (verificado contra `INFORMATION_SCHEMA`).

El módulo es el **único** del replatform que escribe sobre `F2276`/`F22761`/`F2276PAS`.
La impresión (`ActaPuesto_Chofer`), el PDF `ActaResumenTurno`, el WS `WSActa` y el resto de la
transacción `F2276` (`F2276_FormDef`, `F2276_Lotes`, WorkWiths) quedan **fuera de alcance** (OUT).

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Pantalla de listado de Actas en `/actas` con grilla paginada (tamaño por defecto 10), filtros de búsqueda y botón "Crear Acta" (R-ACTA-001…R-ACTA-013). |
| 2 | Grilla con las **7 columnas** del legacy `Grid276` y el filtro base del turno (`F276Estad <> 'A'`, `F276GenWeb = 'A'`), ordenadas por `TurNroId, SucNroId, SucTipo, F276Id` (R-ACTA-003, R-ACTA-004, R-ACTA-005). |
| 3 | Cálculo de la contraparte visible (CUIT + Razón social) según el orden vendedor → destinatario → transportista (R-ACTA-006). |
| 4 | Indicador **"Gen. Manual?"** derivado del rango manual de `FORMSUCU` (no es columna) y total del acta derivados en el servicio (R-ACTA-007, R-ACTA-008). |
| 5 | Formulario de alta en `/actas/nueva` con los 4 fieldsets del Figma: "Datos del Acta", "Vendedor"/"Destinatario"/"Transportista", "Datos del Transporte", "Productos" (R-ACTA-014…R-ACTA-025). |
| 6 | Grilla de productos con alta/baja de filas (JS puntual vanilla), máximo 6 filas, cantidad forzada a 1 si `ProdTipCal = 3` (R-ACTA-020, R-ACTA-021, R-ACTA-023). |
| 7 | Todas las validaciones de negocio del legacy transcritas **verbatim** (R-ACTA-026…R-ACTA-040). |
| 8 | Control de estado/lotes: validación de formularios abiertos del turno antes de mostrar el alta y bloqueo por turno cerrado (R-ACTA-041, R-ACTA-042, R-ACTA-044). |
| 9 | Persistencia de cabecera + detalle + pase con los valores por defecto del legacy (`F276GenWeb='A'`, `F276Operad`, `CPCPerId`, `F276Estad='C'`, `F276Multa=0`, `F276ForPag='EFE'`, `F276RMarCM='N'`) (R-ACTA-045). |
| 10 | Numeración de `F276Id` y cálculo de `F276FormTi` (R-ACTA-046, R-ACTA-047). |
| 11 | Paridad 1:1 con `F2276` (54 columnas), `F22761` (15) y `F2276PAS` (9), incluidas las **columnas huérfanas** (R-ACTA-050, R-ACTA-051). |
| 12 | CSS propio basado en tokens custom properties, sin frameworks externos, sin CDN, sin `npm` (R-ACTA-053). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Impresión del acta (`ActaPuesto_Chofer`) | Requisito fuera del encargo; requiere `F2276VolanteDePago` y rendering PDF | Backlog |
| `ActaResumenTurno` (reporte PDF por turno) | Reporte legacy; no migrar ahora | Backlog |
| `WSActa` (WS `WsActaControlador` que genera el F506 y la multa) | No hay especificación del WS en el encargo; su respuesta es texto libre del WS | Backlog / SA-ACTA-02 |
| `F2276_FormDef`, `F2276_Lotes` (transacciones completas) | Fuera del encargo | Backlog |
| WorkWiths `F2276ListadoWC`, `F2276_F22761WC`, `PPrim_TurnosF2276WC`, etc. | Fuera del encargo | Backlog |
| Anulación de actas (`F276Estad = 'A'`) | No hay objeto legacy de anulación en el alcance | Backlog |
| Cálculo de impuestos de flete (`F276AlicFl`, `F276ImpuFl`, `F276MImpFl`) | Requiere `BuscarOrigenRetencion`, `F2276_CalImpFl` y `F2276_CalTarFl` (los 3 **OUT**, ver §3.12); en el alta `F276GenWeb='A'` ⇒ el legacy fuerza `F276ImpuFl = 0` (R-ACTA-045) | Backlog |
| `F2276GenerarSn` / `F2276BuscarSn` (SN de casa central) | Sólo aplican a `SucTipo='REC' and SucNroId=0`; este módulo es de Puestos Camineros | `06-turnos` (ya implementado) |
| Autenticación / autorización | La define `03-login` (Spring Security + CSRF) | `03-login` |
| E2E automatizado | — | Fuera del módulo |

---

## 3. Modelo legacy verificado

### 3.1 `WebPanel ActaListadoWC` — pantalla principal

**Firma**: sin parámetros.

**Variables**: `&F276Total NUMERIC(12,2)`, `&SucNroId NUMERIC(4)`, `&SucTipo CHAR(3)`,
`&TurNroId NUMERIC(8)`, `&ConCuit NUMERIC(11)`, `&ConRazSoc CHAR(35)`, `&Context`, `&Delete BITMAP`,
`&F276GenMan CHAR(1)`, `&Imprimir BITMAP`, `&Mensaje CHAR(30)`, `&ImprimirVolante BITMAP`,
`&TurEstado CHAR(1)`, `&F276Id NUMERIC(9)`, `&F276Operad CHAR(10)`, `&Mode CHAR(3)`.

**Controles**: `ErrViewer1`, `Grid276` (`gxGrid`, `MaxRows=10`, `AutoResize`), `BtnNuevo` (`gxButton`,
caption `Nuevo Acta`, evento `'Nuevo'`).

**Regla de la grilla** (verbatim):

```text
ControlWhere: TurNroId = &TurNroId; SucNroId = &SucNroId; SucTipo = &SucTipo;
              F276Estad <> 'A'; F276GenWeb = 'A';
ControlOrder: TurNroId, SucNroId, SucTipo, (F276Id)
```

**Columnas del grid** (en orden):

| # | Columna | Origen | Tipo | Visible | Título |
|---|---|---|---|---|---|
| 1 | `F276Id` | att:5 | NUMERIC(9) | Sí | `Acto Nro.` |
| 2 | `&ConCuit` | var:9 | NUMERIC(11) | Sí | (sin caption) |
| 3 | `&ConRazSoc` | var:10 | CHAR(35) | Sí | (sin caption) |
| 4 | `&F276GenMan` | var:13 | CHAR(1) | Sí | `Gen. Manual?` |
| 5 | `F276GenMan` | att:79 | CHAR(1) | No | Combo Box `No:N` / `Sí:S` |
| 6 | `&F276Operad` | att:53 | CHAR(10) | Sí | `Usuario Gen.` |
| 7 | `&Imprimir` | var:14 | BITMAP | Sí | `Imprimir` (evento `'Imprimir'`) |
| 8 | `TurNroId` | att:69 | NUMERIC(8) | No | — |
| 9 | `SucNroId` | att:67 | NUMERIC(4) | No | — |
| 10 | `SucTipo` | att:68 | CHAR(3) | No | — |

**Eventos (verbatim)**:

```text
Event Start
    LoadContext(&Context)
    &SucNroId = &Context.Sucursal
    &SucTipo  = &Context.Tipo
    &TurNroId = &Context.Turno
    &Imprimir.FromImage(impresora)
    If &TurNroId <= 0
        &Mensaje = 'Debe iniciar un turno...'
        Mensaje.Call(&Mensaje,2)
    EndIf
EndEvent

Event Grid276.Load
    &ConCuit.SetEmpty()
    &ConRazSoc.SetEmpty()
    &F276Total.SetEmpty()
    Do Case
        Case not F276CuitVe.IsEmpty()
            &ConCuit = F276CuitVe
            &ConRazSoc = F276VenRS
        Case not F276CuitDe.IsEmpty()
            &ConCuit = F276CuitDe
            &ConRazSoc = F276DestRS
        Case not F276CuitTr.IsEmpty()
            &ConCuit = F276CuitTr
            &ConRazSoc = F276TranRS
    EndCase
    &F276Total = F276Total
    if F276GenMan = 'N'
        &Imprimir.FromImage(impresora)
        &Imprimir.Tooltiptext = 'Imprimir'
        &F276GenMan = 'No'
    else
        &Imprimir.FromImage(ActionDisabled)
        &Imprimir.Tooltiptext = 'No Imprime'
        &Imprimir.Link = Link('')
        &F276GenMan = 'Sí'
    endif
EndEvent

Event 'Nuevo'
    PPActaDetalle.Call(&Context.Turno,'A')
EndEvent

Event 'Imprimir'
    if F276GenMan = 'N'
        ActaPuesto_Chofer.Popup(SucTipo,SucNroId,F276Id)
    Endif
EndEvent
```

**Mensajes / tooltips verbatim**: `Debe iniciar un turno...` (tipo 2), `Imprimir`, `No Imprime`.

> **`WebComponent ActaListadoWC_turno`**: misma grilla, sin botón "Nuevo Acta" y con la sección de
> impresión comentada. Es la misma grilla sin acción de alta ⇒ **no** se replica como pantalla
> separada (R-ACTA-009).

### 3.2 `WebPanel PPActaDetalle` — formulario de alta (verificado en esta revisión)

**Firma**: `parm(in:&TurNroId NUMERIC(8), in:&Tipo CHARACTER(1))`.
En el alta se invoca `PPActaDetalle.Call(&Context.Turno,'A')` ⇒ `&Tipo = 'A'` (ALTA).

**Reglas (verbatim)**:

```text
parm(in:&TurNroId,in:&Tipo);

prompt(ListadoLocalidades,&ProvVenNom,&LocNomVen,&F2276.F276VenCLo);
prompt(ListadoLocalidades,&ProvDesNom,&LocNomDes,&F2276.F276DestCL);
prompt(ListadoLocalidades,&ProvTranNom,&LocNomTra,&F2276.F276TranCL);
prompt(ListadoLocalidades,&ProvOrigenNom,&LocNomOrigen,&F2276.F276OrCod);
prompt(ListadoLocalidades,&ProvDestinoNom,&LocNomDestino,&F2276.F276DeCod);
```

**Valores por defecto (verbatim)**:

```text
&F2276.F276ForPag = "EFE"
&F276FchMov = &Today
if &Tipo = 'A' //ALTA
    ctlF276Id.Visible = false
    TextBlockFormId.Visible = false
    &F276FchMov.Enabled = false
endif
```

**Estructura visual (Figma `ztMkGHkjqvQL2TOjHP1DSN` nodo `2030:2` + `WebForm` del legacy)**:

| Fieldset | Controles | Leyenda |
|---|---|---|
| `Group4` | `F276ActaTi` (Combo), `F276Clasif` (Combo), `F276FchMov` | `Datos del Acta` |
| `Group1` | `F276CuitVe`, `F276VenRS`, `F276VenDom`, `F276VenCLo` (+ `LocNomVen`, `ProvVenNom` read-only) | `Vendedor` |
| `Group2` | `F276CuitDe`, `F276DestRS`, `F276DestDo`, `F276DestCL` (+ `LocNomDes`, `ProvDesNom` read-only) | `Destinatario` |
| `GroupTransportista` | `F276CuitTr`, `F276TranRS`, `F276TranDo`, `F276TranCL` (+ `LocNomTra`, `ProvTranNom` read-only) | `Transportista` |
| `Group3` | `F276Chasis`, `F276Acopla`, `F276TCpbte` (Combo), `F276NCpbte`, `F276KmFle` | `Datos del Transporte` |
| `Group6` | `Grid1` (SDT `F2276DetalleSDT`, item 43) | `Productos` |

**Combos (verbatim, `ControlValues` del `WebForm`)**:

| Control | Atributo | Valores |
|---|---|---|
| `Motivo de Acta` | `F276ActaTi` | `Seleccionar...:N`, `Falta Documentación de Fletes:F`, `Falta Documentación de Prod. Prim.:P`, `Sin Documentación:T` |
| `Tipo Movimiento` | `F276Clasif` | `Seleccionar...:N`, `Salida:S`, `Entrada:E` |
| `T. de Comp.` | `F276TCpbte` | `Carta Porte:1`, `Factura:2`, `Remito:3`, `DT-e:5`, `DT-v:6`, `Guia Forestal:7` (+ `AddEmptyItem=True`, `EmptyItemText="Seleccionar..."`) |

**Rótulos verbatim** del `WebForm`: `Datos del Acta`, `Nro. Acta`, `Motivo de Acta`,
`Tipo Movimiento`, `Fecha Acta`, `Vendedor`, `Destinatario`, `Transportista`,
`Datos del Transporte`, `Patente Chasis`, `Patente Acoplado`, `Localidad`, `Flete (Km)`, `Productos`,
`Cancelar`.

**Eventos (verbatim, relevantes)**:

```text
Event 'Confirmar'
    Do 'Validar'
    if &Error = 'N'
        Do 'LimpiaGridBC'
        Do 'BeforeInsert'
        Do 'GridToBC'
        &F2276.Save()
        if &F2276.Fail()
            &Messages = &F2276.GetMessages()
            For &Message in &Messages
                msg(&Message.Description)
            Endfor
            rollback
        else
            BtnConfirmar.Visible = 0
            BtnCancel.Caption = 'Cerrar'
            commit
            Do 'WSActa' //GENERO EL F506
            if StrSearch(&responsejson, "OK") <> 0
                ActaListadoWC.Call()
                msg("El acta se ha creado correctamente")
            else
                msg("El acta se ha creado correctamente , pero hubo problemas con la generacion de la multa.("+&responsejson+")")
            endif
        endif
    endif
EndEvent

Sub 'BeforeInsert'
    if &Tipo = 'A'
        &F2276.TurNroId = &Context.Turno
    else
        &F2276.TurNroId = &TurNroId
    endif
    &F2276.F276FchMov = &F276FchMov
    &F2276.SucTipo = &Context.Tipo
    &F2276.SucNroId = &Context.Sucursal
EndSub

Sub 'GridToBC'
    for &ItemGrid in &Productos
        &F2276Item = new()
        &F2276Item.F276RCant = &ItemGrid.F276RCant
        &F2276Item.ProdCod  = &ItemGrid.ProdCod
        &F2276.F22761.Add(&F2276Item)
    endfor
EndSub
```

> `BtnConfirmar` dispara `confirm('¿Confirma que desea enviar el Formulario de Acta?')` (JS puntual).
> El `WSActa` queda **OUT** (SA-ACTA-02): sin WS no hay respuesta que evaluar, por lo que el alta
> **se confirma con el mensaje de éxito del legacy** y el pase del puesto queda registrado.

### 3.3 `Transaction F2276` — reglas (verbatim, confirmadas en esta revisión)

```text
default(F276GenWeb, 'A');
default(F276Operad, &Context.Usuario.ToUpper());
Default(F276RMarCM ,'N' );
default(F276Multa, 0);
Default(F276Estad, 'C');
Default(CPCPerId, &Context.PersonaId);

&ban = PPrim_VerificaGenMan.Udp(SucTipo, SucNroId, 2276, F276Id) if  F276Id <> 0;
error("Debe ingresar un Nro. de Formulario Generado mediante el Sistema") if (&ban = 0 and F276Id <> 0);

F276Id = F2276_GenNroForm.Udp(SucNroId, 2276, SucTipo) if  F276Id = 0  and insert on beforeinsert;
error("No se puede generar la guia porque el turno asociado esta cerrado") if PPrim_TurnoEstado(SucNroId,SucTipo,TurNroId) = 'C' on beforeinsert;

F276Vers = BuscarVersionActual.Udp();
F276Origen = Bus_DesLoc.Udp(F276OrCod);
F276Destin = Bus_DesLoc.Udp(F276DeCod);

Error('El precio ingresado no puede ser inferior al Precio registrado del Producto') if (F276RSPre <> 0) and (F276RSPre < F276RFijo);
F276RSPre = F276RFijo if F276RSPre = 0;

F276AlicFl = BuscarOrigenRetencion.Udp(14,F276FchMov) if insert;
F276ImpuFl = F2276_CalImpFl.Udp(F276TarxKm, F276MImpFl, F276AlicFl) if FCalFlete;
F276ImpuFl = 0 if not FCalFlete;

F276VenRS = F2276_DevRazSocial.Udp(F276CuitVe) if F276VenRS.IsEmpty();
F276VenDom = F2276_BusDom.Udp(F276CuitVe) IF F276VenDom.IsEmpty();
F276VenCLo = F2276_BusLoc.Udp(F276CuitVe) if F276VenCLo.IsEmpty();
F276DestRS = F2276_DevRazSocial.Udp(F276CuitDe) if F276DestRS.IsEmpty();
F276DestDo = F2276_BusDom.Udp(F276CuitDe) IF F276DestDo.IsEmpty();
F276DestCL = F2276_BusLoc.Udp(F276CuitDe) if F276DestCL.IsEmpty();
F276TranRS = F2276_DevRazSocial.Udp(F276CuitTr) if F276TranRS.IsEmpty();
F276TranDo = F2276_BusDom.Udp(F276CuitTr) IF F276TranDo.IsEmpty();
F276TranCL = F2276_BusLoc.Udp(F276CuitTr) if F276TranCL.IsEmpty();

F276RAlicu = 1;
F276ImpuFl = 0 if F276GenWeb = 'A';
F276ImIBTot = 0 if F276GenWeb = 'A';
F276ImCMTot = 0 if F276GenWeb = 'A';
F276AdicFl = F276ImpuFl *0.1;
F276AdIBTo = F276ImIBTot*0.1;
F276AdCMTo = F276ImCMTot*0.1;
F276FeGra = ServerNow();
F276FormTi = ActaDeterminaTipoFormulario.Udp(F276CuitVe,F276Clasif) if insert;

F2276_RegPasePuestoV2.Call(SucTipo, SucNroId, F276Id,&Duplicado) on BeforeComplete;
F2276GenerarSn.call(F276CuitVe,F276FeGra,F276ImpTotal,F276AdiTot,F276SnSnNu) if (SucTipo = 'REC' and SucNroId = 0) on afterlevel;
F276SnSnNu = F2276BuscarSn.Udp(F276CuitVe,F276FeGra,127,16,F276ImpTotal) if (SucTipo = 'REC' and SucNroId = 0) on afterlevel;

error('Debe seleccionar un tipo de comprobante') if F276TCpbte.IsEmpty();
error('Debe ingresar un nro. de comprobante') if F276NCpbte.IsEmpty();
error('La fecha no puede ser superior a la fecha actual') if F276FchMov > serverdate();
```

> Nota: `&SoloIns = ProPri_VerSoloIns.Udp(F276CuitVe)` y `&Ins = ProPRi_VerIns.Udp(F276CuitVe, ProdCod)`
> se calculan pero **no disparan error** (las dos validaciones están comentadas en el legacy) ⇒ no
> se replican.

> **La línea `F276Id = F2276_GenNroForm.Udp(SucNroId, 2276, SucTipo)`** se implementa contra el
> **contador persistente `GES_SUCN.SucUltNum`** (ver §3.11), **no** contra `MAX(F276Id) + 1` ni
> contra `FORMSUCU`. La línea
> `&ban = PPrim_VerificaGenMan.Udp(SucTipo, SucNroId, 2276, F276Id)` sí lee `FORMSUCU` (rangos
> manuales, ver §3.12).

### 3.4 `ActaDeterminaTipoFormulario` (verbatim)

```text
parm(in:&ConCuit, in:&F276Clasif , out:&F276FormTi);

Do 'VerContribuEstado'
Do Case
    Case &F276Clasif = 'E' and &esInfraccion = false
        &F276FormTi = 1
    Case &F276Clasif = 'E' and &esInfraccion = true
        &F276FormTi = 2
    Case &F276Clasif = 'S'
        &F276FormTi = 3
Endcase

Sub 'VerContribuEstado'
    &esInfraccion = false
    For each where ConCuit = &ConCuit
        if ConEstado = 12 or ConEstado = 14
            &esInfraccion = false
        else
            &esInfraccion = true
        endif
    EndFor
EndSub
```

### 3.5 `ProPri_VerManual` / `PPrim_VerificaGenMan` (verbatim) — origen del indicador "Gen. Manual?"

```text
ProPri_VerManual:  parm(in:&sucnroid, in: &suctipo, in:&F276Id, out:&Man);
    &tot = 0 ; &Man = 'N'
    for each where SucNroId = &sucnroid where SucTipo = &suctipo where SucNuForm = 2276
            where ForRecIni <= &F276Id
        &Tot = ForRecIni + ForRecCant
        if &Tot > &F276Id
            &Man = 'S'
            exit
        endif
    endfor

PPrim_VerificaGenMan:  parm(in:&SucTipo, in:&SucNroId, in:&SucNuForm, in:&F276Id, out:&ban);
    &ban = 0
    for each where SucNroId = &SucNroId where SucTipo = &SucTipo where SucNuForm = &SucNuForm
            where ForRecIni <= &F276Id where ForRecIni + ForRecCant > &F276Id
        &ban = 1 //Es manual
        exit
    endfor
```

> `F276GenMan` **no es columna** de `F2276`: es un atributo fórmula
> (`ProPri_VerManual.udp(...)`). El indicador "Gen. Manual?" se **deriva** consultando `FORMSUCU`
> (rango manual `[ForRecIni, ForRecIni + ForRecCant)`).

### 3.6 `F2276_RegPasePuestoV2` (verbatim) — pase del puesto

```text
LoadContext(&Context)
&Indice = 1
For Each Order SucTipo SucNroId F276Id where ... defined by F276PaseST
    &Indice += 1
    if F276PaseST = 'PUE' then &cantPases += 1
EndFor
New
    SucNroId = &SucNroId ; SucTipo = &SucTipo ; F276Id = &F276Id
    F276PPaseI = &Indice ; F276PFecha = ServerNow() ; F276PUsuar = &Context.Usuario
    F276PaseST = &Context.Tipo ; F276PaseSN = &Context.Sucursal ; F276PaseCo = &Context.Comision
EndNew
```

### 3.7 `F2276_BusProTarifa` / `F2276_BusProTarCM` (verbatim) — precio del producto

```text
F2276_BusProTarifa:  Parm(in:&ProdCod, in:&F276Clasif, in: &F276FchMov, out:&ProdImpu);
    For each Order ProdCod, (ProdFchVig) where ProdCod = &ProdCod where ProdFchVig <= &F276FchMov
        If &F276Clasif = 'S' then &ProdImpu = ProdImpu1 else &ProdImpu = ProdImpu3
        Exit
    when none  &ProdImpu = 0

F2276_BusProTarCM:   Parm(in:&ProdCod, in:&F276Clasif, in:&F276RMarCM, in:&F276FchMov, out:&ProdImpu);
    &ProdImpu = 0
    if &F276RMarCM = 'S'
        For each Order ProdCod, (ProdFchVig) where ProdCod = &ProdCod where ProdFchVig <= &F276FchMov
            If &F276Clasif = 'S' then &ProdImpu = ProdImpu2 else &ProdImpu = ProdImpu4
            Exit
        Endfor
    endif
```

### 3.8 `WebPanel ActaCargarDetalle` — alta de detalle (verificado)

**Firma**: `parm(in:&F276Clasif CHAR(1), in:&F276FchMov DATE(8), in:&F276CuitVe NUMERIC(11))`.

**SDT `F2276DetalleSDT`** (item `&Item`): `ProdCod`, `F276RZona`, `ProdAcop`, `ProdDesc`, `ProdTipCal`,
`F276RCant`, `F276RMImpo`, `F276RFijo`, `F276RPreCM`, `F276RAlicu`, `F276RImpIB`, `F276RImpCM`,
`F276RMarCM`, `F276RSPre`, `Indice NUMERIC(4)`, `Modificado Boolean`.

**Eventos verbatim**:

```text
Event Start
    ctlProdTipCal.Visible = false
    &ProdAcop.Visible = false
    &Item.F276RAlicu = 1
EndEvent

Event ctlProdCod.IsValid
    if &Item.ProdTipCal = 3
        ctlF276RCant.Enabled = 0
        &Item.F276RCant = 1
    else
        ctlF276RCant.Enabled = 1
    endif
    &Item.F276RSPre = &Item.F276RFijo
    &ProdCod2 = &Item.ProdCod
EndEvent

Event Enter
    Do 'Validaciones'
    if &Valid
        &Item.ProdDesc = ctlProdCod.Text
        &ItemJson = &Item.ToJson()
        &Session.Set("PROD",&ItemJson)
        return
    endif
EndEvent

Sub 'Validaciones'
    &Valid = true
    if &Item.ProdCod.IsEmpty()
        Msg('Debe seleccionar algun producto')
        &Valid = false
    endif
EndSub
```

**Combo de productos**: `ProdCod` con descripción `ProdDesc`, filtro `ProdAnula = 0`, `AddEmptyItem=True`.

### 3.9 Mensajes verbatim (contrato observable)

| # | Texto | Origen |
|---|---|---|
| 1 | `Debe iniciar un turno...` | `ActaListadoWC.Start` (turno ≤ 0) |
| 2 | `Imprimir` | tooltip de `&Imprimir` (acta no manual) |
| 3 | `No Imprime` | tooltip de `&Imprimir` (acta manual) |
| 4 | `Seleccionar el motivo de Acta` | `PPActaDetalle.Validar` |
| 5 | `Debe seleccionar Tipo de Movimiento` | `PPActaDetalle.Validar` |
| 6 | `Debe ingresar CUIT del Vendedor` | `PPActaDetalle.Validar` |
| 7 | `Debe ingresar CUIT del Destinatario` | `PPActaDetalle.Validar` |
| 8 | `Debe ingresar CUIT del Transportista` | `PPActaDetalle.Validar` |
| 9 | `Debe ingresar Razón Social del Vendedor` | `PPActaDetalle.Validar` |
| 10 | `Debe ingresar Razón Social del Destinatario` | `PPActaDetalle.Validar` |
| 11 | `Debe ingresar Razón Social del Transportista` | `PPActaDetalle.Validar` |
| 12 | `Ingrese todos los datos del Transporte` | `PPActaDetalle.Validar` |
| 13 | `Debe ingresar el detalle de los productos` | `PPActaDetalle.Validar` |
| 14 | `La cantidad maxima de productos que se pueden agregar es de seis` | `PPActaDetalle.Validar` |
| 15 | `El movimiento de Entrada solo puede generar un Acta por falta de Documentación de Prod. Primaria` | `PPActaDetalle.Validar` |
| 16 | `Debe ingresar un Nro. de Formulario Generado mediante el Sistema` | `F2276` (rules) y `PPActaDetalle.Validar` |
| 17 | `El número de Acta no corresponde a un número generado manualmente` | `PPActaDetalle.Validar` (modo manual) |
| 18 | `Debe seleccionar algun producto` | `ActaCargarDetalle.Validaciones` |
| 19 | `El producto ya ha sido ingresado` | `PPActaDetalle.Cargar` (producto duplicado) |
| 20 | `Debe seleccionar un tipo de comprobante` | `F2276` (rules) |
| 21 | `Debe ingresar un nro. de comprobante` | `F2276` (rules) |
| 22 | `La fecha no puede ser superior a la fecha actual` | `F2276` (rules) |
| 23 | `No se puede generar la guia porque el turno asociado esta cerrado` | `F2276` (rules, `on beforeinsert`) |
| 24 | `El precio ingresado no puede ser inferior al Precio registrado del Producto` | `F2276` (rules) |
| 25 | `¿Confirma que desea enviar el Formulario de Acta?` | `PPActaDetalle.Start` (JS `confirm`) |
| 26 | `El acta se ha creado correctamente` | `PPActaDetalle.'Confirmar'` |
| 27 | `El acta se ha creado correctamente , pero hubo problemas con la generacion de la multa.(` + respuesta WS + `)` | `PPActaDetalle.'Confirmar'` (sólo con WS; ver SA-ACTA-02) |

> Los textos se conservan **verbatim**, incluidas las erratas (`maxima` sin tilde,
> `guia` sin tilde, `prod.` abreviado, la coma antes del paréntesis en el mensaje 27).
> La paridad de identidad manda (`openspec/constitution/conventions.md` §6).

### 3.10 Esquema físico verificado (G-DATA 2026-11-04, `Rentas`)

**`F2276`** — 54 columnas. PK: `F276Id` (int), `SucTipo` (char(3)), `SucNroId` (smallint).
`NOT NULL`: `F276Id`, `SucTipo`, `SucNroId`, `CPCPerId`, `F276Clasif`, `F276Multa`.
Sin columnas `IDENTITY`. Sin claves foráneas declaradas.

**`F22761`** — 15 columnas. PK: `F276Id`, `SucTipo`, `SucNroId`, `ProdCod`, `F276RZona`.

**`F2276PAS`** — 9 columnas. PK: `SucTipo`, `SucNroId`, `F276Id`, `F276PPaseI`.
**`F276PaseSD` no existe en la BD** — verificado por `INFORMATION_SCHEMA.COLUMNS`
(`SELECT COUNT(*) … WHERE TABLE_NAME='F2276PAS' AND COLUMN_NAME='F276PaseSD'` ⇒ `0`).
El atributo existe en la KB pero **no se mapea** (ya no es un supuesto).

**`GES_SUCN`** — **5 columnas** (contador persistente de numeración, ver §3.11):
`SucNroId` (smallint NOT NULL), `SucTipo` (char(3) NOT NULL), `SucNuForm` (smallint NOT NULL),
`SucNuId` (char(3) NOT NULL), `SucUltNum` (int NOT NULL).
PK: (`SucNroId`, `SucTipo`, `SucNuForm`). Sin `IDENTITY`. Sin claves foráneas declaradas.

**`FORMSUCU`** — 8 columnas: `SucNroId`, `SucTipo`, `SucNuForm`, `ForRecIni`, `ForRecCant`,
`ForRecFech`, `ForRecUsu`, `ForRecTipo`. **No existen `SucUltNum` ni `ForRecFin`** (verificado).
**`FORMSUCU` NO participa de la numeración** (§3.11): es el registro de *formularios recibidos*
y sólo interviene en la detección de **números manuales** (§3.12).

**Atributos de la KB que NO son columnas físicas de `F2276`** (verificado):
`F276VenLoc`, `F276VenProv`, `F276DestLoc`, `F276DestProv`, `F276TranLoc`, `F276TranProv`,
`F276ImIBTot`, `F276ImCMTot`, `F276AdiTot`, `F276ImpTotal`, `F276Total`, `F276ProCan`,
`F276GenMan`, `F276CodBar`, `FCalFlete`.

**Columnas huérfanas** (existen en la BD, no en la KB):
`F2276.PueCod` (smallint NULL), `F2276.CPCId` (int NULL), `F22761.F276Corrig` (char(1) NULL).

**Valores observados en producción** (`SELECT` agregados sobre `F2276`):
`F276Clasif` ∈ {`E`, `S`, vacío}; `F276ActaTi` ∈ {`F`, `P`, `T`, vacío};
`F276GenWeb` ∈ {`A`, `N`, `S`, vacío}; `F276Estad` ∈ {`C`, `A`, vacío};
`F276FormTi` ∈ {1, 2, 3, 0, NULL}; `LOTES227.LF276Estad` ∈ {`P`, `C`}.

### 3.11 `F2276_GenNroForm` y `GES_SUCN` — numeración del Acta (**resuelve SA-ACTA-03**)

**Firma** (`Root Module/F2776/Procedimientos/F2276_GenNroForm`):

```text
Parm(in:&ReceId, in:&RecNuForm, in:&Tipo, out:&RecNuUlt);
```

**Source verbatim**:

```text
For Each
	where SucNroId = &ReceId
	where SucTipo = &Tipo
	where SucNuForm = &RecNuForm
	defined by SucUltNum
	&RecNuUlt = SucUltNum + 1
	SucUltNum = &RecNuUlt
when none
	New
		SucNroId	= &ReceId
		SucTipo 	= &Tipo
		SucNuForm 	= &RecNuForm
		SucUltNum	= 1
	EndNew
	&RecNuUlt = 1
EndFor
```

**Business Component referenciado**: `Ges_SucN` (Level1) → **tabla física `GES_SUCN`**.

**Comportamiento observable**: la numeración es un **contador persistente por
(sucursal, tipo de sucursal, nro. de formulario)**. El procedimiento lee la fila de
`GES_SUCN` para `(SucNroId = <sucursal>, SucTipo = <tipo>, SucNuForm = 2276)`, devuelve
`SucUltNum + 1` y **escribe** el valor devuelto en `SucUltNum`. Si la fila **no existe**
(`when none`), la crea con `SucUltNum = 1` y devuelve `1`.

> **Corrección de SA-ACTA-03**: la numeración **NO** es `MAX(F276Id) + 1` ni usa `FORMSUCU`.
> `FORMSUCU` (`SucNroId, SucTipo, SucNuForm, ForRecIni, ForRecCant, ForRecFech, ForRecUsu,
> ForRecTipo`) es el registro de *formularios recibidos* y sólo interviene en la detección de
> números manuales (§3.12).

**Verificación empírica en `Rentas` (2026-11-04, sólo `SELECT`)** — `GES_SUCN` con
`SucNuForm = 2276` y `SucTipo = 'PUE'`:

| `SucNroId` | `GES_SUCN.SucUltNum` | `MAX(F2276.F276Id)` (PUE) | `F2276` filas | ¿Coincide? |
|---|---|---|---|---|
| 0 | 51 | 7 | 1 | no |
| 1 | 15663 | 15663 | 9890 | sí |
| 2 | 10784 | 10784 | 7899 | sí |
| 3 | 6149 | **300002** | 5378 | **no** |
| 7 | 6009 | **74824** | 3271 | **no** |
| 8 | 135 | 134 | 111 | contador = MAX+1 |
| 9 | 1426 | 1426 | 25 | sí |
| 10 | 9 | 8 | 6 | contador = MAX+1 |
| 11 | 9 | 9 | 1 | sí |
| 12 | 5 | — | — | — |
| 13 | 104 | 104 | 44 | sí |
| 14 | 342 | 342 | 278 | sí |
| 15 | 320 | 313 | 262 | contador = MAX+1 |
| 17 | 4 | 3 | 3 | contador = MAX+1 |
| 74 | 5 | — | — | — |

**Conclusión**: `GES_SUCN.SucUltNum` es la **fuente autoritativa** de la numeración;
`MAX(F276Id)` es sólo un indicio (en las sucursales 3 y 7 hay actas cargadas por vías ajenas
al contador, con `F276Id` muy superior al contador: usar `MAX+1` produciría una numeración
**distinta** de la del legacy).

**Distribución de `SucNuId`** en `GES_SUCN`: 36 filas con `'MAN'` y 11 en blanco (3 espacios).
El legacy **no informa** `SucNuId` en el `New` (sólo `SucNroId`, `SucTipo`, `SucNuForm`,
`SucUltNum`), por lo que el valor por defecto de aplicación se documenta como supuesto
(SA-ACTA-14) y se fija en `'MAN'` (D-ACTA-19).

**Desvío consciente del legacy (D-ACTA-19)**: el legacy **no bloquea** la fila de `GES_SUCN`
al incrementarla ⇒ bajo concurrencia dos altas simultáneas pueden obtener el mismo `F276Id`
y violar la PK `(F276Id, SucTipo, SucNroId)`. La implementación Java debe hacer el incremento
**atómico** dentro de la misma transacción del alta.

### 3.12 Procedures verificados (fase A — T-ACTA-001)

Todos leídos por MCP `genexus18mcp` (alias `atppuestos`, **solo lectura**) el 2026-11-04.
Columna **Alcance**: `IN` = se replica en este módulo; `OUT` = no se replica (ver §2.2).

| # | Procedure | Firma (verbatim) | Comportamiento observable | Tabla(s) | Alcance |
|---|---|---|---|---|---|
| 1 | `ProPri_VerManual` | `parm(in:&sucnroid, in: &suctipo, in:&F276Id, out:&Man);` | `&Man = 'N'` por defecto; recorre los rangos de la sucursal y **si `ForRecIni <= F276Id < ForRecIni + ForRecCant`** ⇒ `&Man = 'S'` y sale. Es el origen de `F276GenMan` ("Gen. Manual?"). | `FORMSUCU` (`FormSucu`) | **IN** |
| 2 | `PPrim_VerificaGenMan` | `parm(in:&SucTipo, in:&SucNroId, in:&SucNuForm, in:&F276Id, out:&ban);` | `&ban = 0`; si `ForRecIni <= F276Id` **y** `ForRecIni + ForRecCant > F276Id` ⇒ `&ban = 1` (es manual) y sale. Es la regla que dispara `error("Debe ingresar un Nro. de Formulario Generado mediante el Sistema")` cuando `ban = 0` y `F276Id <> 0`. | `FORMSUCU` | **IN** |
| 3 | `F2276_RegPasePuestoV2` | (sin `parm`; usa `&Context`) | `&Indice = 1`; recorre los pases de la Acta **ordenados por `SucTipo, SucNroId, F276Id`** y definidos por `F276PaseST`, incrementando `&Indice` por pase y contando los de tipo `'PUE'`; inserta un pase nuevo con `F276PPaseI = &Indice`, `F276PFecha = ServerNow()`, `F276PUsuar = &Context.Usuario`, `F276PaseST = &Context.Tipo`, `F276PaseSN = &Context.Sucursal`, `F276PaseCo = &Context.Comision`. | `F2276PAS` | **IN** |
| 4 | `F2276_BusProTarifa` | `Parm(in:&ProdCod, in:&F276Clasif, in: &F276FchMov, out:&ProdImpu);` | Recorre las vigencias del producto **ordenadas por `ProdCod, ProdFchVig`** con `ProdFchVig <= &F276FchMov` y toma la **última** (hace `Exit`); si `F276Clasif = 'S'` ⇒ `ProdImpu1`, si no ⇒ `ProdImpu3`; sin coincidencias ⇒ `0`. Es el precio fijo `F276RFijo` del detalle. | `PRODUCT1` | **IN** |
| 5 | `F2276_BusProTarCM` | `Parm(in:&ProdCod, in:&F276Clasif, in:&F276RMarCM, in:&F276FchMov, out:&ProdImpu);` | `&ProdImpu = 0`; **sólo** si `F276RMarCM = 'S'` recorre las vigencias (`ProdFchVig <= fecha`) y toma `ProdImpu2` si `F276Clasif = 'S'`, `ProdImpu4` en caso contrario; sin coincidencias ⇒ `0`. Es el precio de cosecha mecánica `F276RPreCM`. | `PRODUCT1` | **IN** |
| 6 | `PPrim_TurnoEstado` | `Parm(in:&SucNroId, in:&SucTipo, in:&TurNroId, out:&TurEstado);` | Recorre el turno de la sucursal/tipo y devuelve su `TurEstado` (`'C'` = cerrado, `'A'` = abierto). Es la regla que dispara `error("No se puede generar la guia porque el turno asociado esta cerrado")`. | `PPRIM_TU` | **IN** |
| 7 | `F2276_DevRazSocial` | `Parm(in:ConCuit, out:&ConRazSoc);` | Devuelve la razón social del contribuyente; **sin coincidencia ⇒ cadena vacía** (`when none ⇒ &ConRazSoc = ''`). | `CONTRIBU` | **IN** |
| 8 | `F2276_BusDom` | `PARM(IN:&ConCuit, OUT:&CallNom);` | Toma el código de calle y la manzana del contribuyente (`CallCodr`, `ConNMzanaR`), y arma el domicilio como **`CallNom + ' ' + &Domicilio`** (calle + espacio + manzana). | `CONTRIBU` (+ catálogo de calles `CALLES`) | **IN** |
| 9 | `F2276_BusLoc` | `PARM(IN:&ConCuit, OUT:&LocCod);` | Devuelve `LocCodR` (código de localidad del contribuyente). | `CONTRIBU` | **IN** |
| 10 | `Nuc_CalculoImpIB` | `parm(in:&PGTipCal, in:&CompDCant, in:&CompDMonto, in:&CompDFijo, in:&CompExIB, in:&CompDAlic, out:&CompDImpIB);` | Calcula el impuesto a los ingresos brutos del detalle: `TipCal = 1` ("PorTonelada") y `TipCal = 2` ("PorUnidad") ⇒ `Calculo1 = Round(CompDCant * CompDFijo, 2)` y `Calculo2 = Round(CompDMonto * CompDAlic / 100, 2)`; `TipCal = 3` ("PorMontoFijo") ⇒ `Calculo1 = CompDFijo` y el mismo `Calculo2`; **`CompDImpIB = MAX(Calculo1, Calculo2)`**. `CompExIB` se recibe pero **no se usa**; si `TipCal` ∉ {1,2,3} ⇒ `0`. | ninguna (cálculo puro) | **IN** |
| 11 | `BuscarVersionActual` | `Parm(out:&Version);` | Toma la fila de `VERSIONE` con `VerApliId = 'SWP'` (ordenada por `VerDescr`) y arma `Version = VerVersion + "." + VerRevisio + "." + VerBuild`. Es el origen de `F276Vers`. | `VERSIONE` | **IN** |
| 12 | `F2276_CalImpFl` | `parm(In:&F276TarxKm, in:&F276ImpuFl, in:&F276AliFle, out:&Importe);` | `Calculo = Round(F276ImpuFl * F276AliFle / 100, 2)`; el importe es **`MAX(Calculo, F276TarxKm)`**. | ninguna | **OUT** (§2.2: el flete no se calcula en este módulo) |
| 13 | `F2276_CalTarFl` | `parm(In:&F276KmFle, In:&F276FchMov, out: &F276TarXKm);` | Si `F276KmFle > 0`, busca la primera fila con `FleKmHas >= F276KmFle` **y** `FleFchHas >= F276FchMov` y devuelve `FleTarKm`; si no hay, toma el `FleTarKm` de la primera fila. Origen de `F276TarxKm`. | `FLETES` | **OUT** (§2.2) |
| 14 | `BuscarOrigenRetencion` | `parm(in:&ORECod, in:&F276FchMov, out:&OREAlic);` | Calcula `PerFis = Año * 100 + Mes` de la fecha y toma la alícuota `OREAlic` de la fila de `ORIGRET1` con `ORECod = &ORECod` y `OREPFis <= PerFis` (ordenada por `OREPFis`, `Exit` en la primera). Origen de `F276AlicFl`. | `ORIGRET1` | **OUT** (§2.2) |
| 15 | `F2276GenerarSn` / `F2276BuscarSn` | — | Se invocan **sólo** si `SucTipo = 'REC' and SucNroId = 0` (casa central). | `SNSINDEV` | **OUT** — fuera de Puestos Camineros (ya implementado en `06-turnos`) |

**Llamada de referencia del detalle** (`WebPanel F2276CargarDetalle`, `Sub 'Formulas'`), que
documenta las fórmulas exactas de las columnas del detalle:

```text
&Item.F276RFijo  = F2276_BusProTarifa.udp(&Item.ProdCod, &F276Clasif, &F276FchMov)
&Item.F276RPreCM = F2276_BusProTarCM.udp(&Item.ProdCod, &F276Clasif, &Item.F276RMarCM, &F276FchMov)
&Item.F276RImpIB = Nuc_CalculoImpIB.udp(&Item.ProdTipCal, &Item.F276RCant, &Item.F276RMImpo, &Item.F276RFijo, 0, &Item.F276RAlicu)
&Item.F276RImpCM = &Item.F276RCant * &Item.F276RPreCM
```

> **Variante de mensajes**: `F2276CargarDetalle` (flujo de guías) usa
> `El precio de facturacion no puede ser menor al precio unitario` y
> `Debe seleccionar alguna producto` (con "alguna"); el flujo de **Actas**
> (`ActaCargarDetalle`, el que usa `PPActaDetalle`) usa
> `El precio ingresado no puede ser inferior al Precio registrado del Producto` (mensaje 24, de
> las rules de `F2276`) y `Debe seleccionar algun producto` (mensaje 18). Cada panel conserva
> **su propio** texto; este módulo replica los del flujo de Actas.

**Derivable sin conocer `ProPri`**: la columna "Gen. Manual?" **sí** es resoluble: tanto
`ProPri_VerManual` como `PPrim_VerificaGenMan` leen `FORMSUCU`, que es una tabla física
verificada con las 8 columnas necesarias. La limitación residual es que `FORMSUCU` registra
los rangos **recibidos** (no necesariamente los *asignados*), por lo que el indicador es
"informa que el número cae en un rango manual registrado", no "el número fue asignado
manualmente" (ver SA-ACTA-15).

---

## 4. Requisitos EARS

### 4.1 Pantalla principal — listado de Actas

#### R-ACTA-001 — Pantalla de Actas
> **EL SISTEMA DEBE** exponer en la ruta de página `/actas` la pantalla de listado de Actas de
> Puestos Camineros, sustentada en el servicio `ActaService` (nunca en llamadas HTTP a la propia API
> ni en acceso directo al repositorio).

* *Criterio de verificación*: `@WebMvcTest` del controller MVC que comprueba `GET /actas` → 200 +
  vista `puestos/actas-lista` + atributos del view model; revisión del `git diff` que confirma que
  el controller no importa repositorios.

#### R-ACTA-002 — Turno activo obligatorio
> **CUANDO** el usuario acceda a la pantalla de Actas y el turno de su contexto de sesión sea
> `<= 0`, **EL SISTEMA DEBE** mostrar el mensaje verbatim `Debe iniciar un turno...` y **NO DEBE**
> permitir la creación de un Acta.

* *Criterio de verificación*: test MockMvc con `ContextoSesion.turno() == 0` que comprueba el texto
  renderizado y que el botón "Crear Acta" queda deshabilitado; test con turno > 0 que comprueba que
  el mensaje no aparece.

#### R-ACTA-003 — Filtro base de la grilla
> **EL SISTEMA DEBE** mostrar en la grilla **exclusivamente** las Actas del turno, sucursal y tipo
> de la sesión (`TurNroId = &Context.Turno`, `SucNroId = &Context.Sucursal`,
> `SucTipo = &Context.Tipo`), **NO anuladas** (`F276Estad <> 'A'`) y **generadas por la web**
> (`F276GenWeb = 'A'`).

* *Criterio de verificación*: IT contra `Rentas` que cuenta las filas que satisfacen el filtro
  legacy y compara con el total devuelto por el repositorio; test que verifica que un registro con
  `F276Estad = 'A'` o `F276GenWeb <> 'A'` no aparece.

#### R-ACTA-004 — Orden de la grilla
> **EL SISTEMA DEBE** presentar las Actas ordenadas por `TurNroId`, `SucNroId`, `SucTipo` y
> `F276Id`, en ese orden, reproduciendo el `ControlOrder` del legacy.

* *Criterio de verificación*: IT que lee el listado paginado y verifica que la secuencia de `F276Id`
  es no decreciente dentro del mismo turno; test de vista que verifica el `sort` enviado.

#### R-ACTA-005 — Columnas de la grilla
> **EL SISTEMA DEBE** renderizar la grilla con las **6 columnas visibles** del legacy `Grid276` (el legacy define 10 columnas, de las cuales 4 son ocultas: el selector `F276GenMan` y las claves `TurNroId`, `SucNroId`, `SucTipo`) y en este orden:
> `Acto Nro.` (F276Id), CUIT (sin título), Razón Social (sin título), `Gen. Manual?`,
> `Usuario Gen.` (F276Operad) e `Imprimir`, **ocultando** el selector de `F276GenMan` (`No:N` / `Sí:S`)
> y las claves `TurNroId`, `SucNroId` y `SucTipo`.

* *Criterio de verificación*: test de vista que verifica los encabezados visibles en orden y la
  ausencia de las columnas ocultas.

#### R-ACTA-006 — Contraparte de la fila (CUIT y Razón Social)
> **EL SISTEMA DEBE** calcular, para cada fila, el CUIT y la Razón Social visibles aplicando el
> orden del legacy: si `F276CuitVe` no está vacío ⇒ (`F276CuitVe`, `F276VenRS`); si no,
> si `F276CuitDe` no está vacío ⇒ (`F276CuitDe`, `F276DestRS`); si no, si `F276CuitTr` no está vacío
> ⇒ (`F276CuitTr`, `F276TranRS`); si todos están vacíos ⇒ celdas vacías.

* *Criterio de verificación*: test unitario del método de servicio con las 4 combinaciones
  (vendedor / destinatario / transportista / ninguno) verificando CUIT y razón social resultantes.

#### R-ACTA-007 — Total del Acta
> **EL SISTEMA DEBE** exponer el total del Acta (`F276Total`) calculado en `ActaService` como
> `F276AdiTot + F276ImpTotal`, con `F276AdiTot = F276AdicFl + F276AdIBTo + F276AdCMTo` y
> `F276ImpTotal = F276ImpuFl + F276ImIBTot + F276ImCMTot`, porque `F276Total`, `F276AdiTot` e
> `F276ImpTotal` **no son columnas** de `F2276`.

* *Criterio de verificación*: test unitario con valores conocidos que verifica la suma; la entidad
> **no** declara `@Column` para esos tres atributos.

#### R-ACTA-008 — Indicador "Gen. Manual?"
> **EL SISTEMA DEBE** mostrar en la columna `Gen. Manual?` el texto `Sí` cuando el `F276Id` de la
> fila cae dentro de un rango manual registrado en `FORMSUCU`
> (`ForRecIni <= F276Id < ForRecIni + ForRecCant`, con `SucNuForm = 2276` y la misma sucursal y
> tipo) y `No` en caso contrario, reproduciendo `ProPri_VerManual`, y **NO DEBE** leer dicho valor
> de una columna porque `F276GenMan` no existe en la tabla física.

* *Criterio de verificación*: test unitario del servicio con un rango de `FORMSUCU` conocido
  (`ForRecIni=100, ForRecCant=50`) que verifica `Sí` para `F276Id=100` y `149` y `No` para `99` y `150`.

#### R-ACTA-009 — Botón "Crear Acta"
> **EL SISTEMA DEBE** mostrar un enlace **"Crear Acta"** destacado, situado arriba a la derecha de la
> pantalla de listado, cuyo `href` apunte al formulario de alta (`/actas/nueva`).

* *Criterio de verificación*: test de vista que verifica la existencia del enlace, su texto
  `Crear Acta`, su `href` y su alineación a la derecha (clase de token CSS, no atributo inline).

#### R-ACTA-010 — Paginación
> **CUANDO** el usuario navegue por la grilla, **EL SISTEMA DEBE** paginar el resultado con el
> soporte de `Pageable` de Spring Data, con **tamaño de página por defecto 10** (paridad con
> `MaxRows=10` del legacy) y **DEBE** volcar a la vista el total de páginas, el número de página
> actual y el total de elementos.

* *Criterio de verificación*: test MockMvc que solicita `GET /actas?page=0&size=10` y verifica los
  atributos `totalPages`, `number` y `totalElements`; IT que comprueba que una segunda página
  devuelve filas distintas.

#### R-ACTA-011 — Filtros de búsqueda
> **CUANDO** el usuario aplique uno o más filtros de búsqueda, **EL SISTEMA DEBE** filtrar la grilla
> por: número de Acta (`F276Id`, coincidencia exacta), CUIT de cualquiera de los tres roles
> (vendedor, destinatario o transportista), Razón Social (coincidencia parcial, sin distinguir
> mayúsculas) y rango de fechas de movimiento (`F276FchMov` entre `desde` y `hasta`), **además** del
> filtro base del turno (R-ACTA-003).

* *Criterio de verificación*: test MockMvc parametrizado (un caso por filtro) que verifica que el
  modelo devuelve menos filas con filtro que sin él; IT que compara el conteo filtrado con un
  `SELECT` equivalente.

#### R-ACTA-012 — Links de paginación dinámicos
> **EL SISTEMA DEBE** renderizar los enlaces de navegación de página preservando los filtros y el
> orden activos, de modo que cambiar de página no pierda el criterio de búsqueda.

* *Criterio de verificación*: test de vista que verifica que el enlace de la página 2 contiene los
  query params del filtro aplicado.

#### R-ACTA-013 — Estado vacío
> **SI** la consulta no devuelve Actas, **ENTONCES EL SISTEMA DEBE** renderizar un mensaje de estado
> vacío en lugar de una tabla vacía, **DEBE** mantener visible el botón "Crear Acta" y **NO DEBE**
> renderizar la grilla.

* *Criterio de verificación*: test MockMvc con el servicio devolviendo `Page.empty()` que verifica
  la presencia del mensaje y la ausencia de `<tbody>` con filas.

### 4.2 Formulario de alta

#### R-ACTA-014 — Ruta y plantilla del alta
> **EL SISTEMA DEBE** exponer en la ruta de página `/actas/nueva` el formulario de alta, renderizado
> por la plantilla `puestos/acta-formulario`, que **extienda** el layout base del proyecto
> (`layout/base`) **sin duplicar** cabecera institucional, barra de sesión, menú ni pie.

* *Criterio de verificación*: test MockMvc que comprueba `GET /actas/nueva` → 200 + vista
  `puestos/acta-formulario`; revisión del HTML renderizado que verifica la presencia de los
  fragmentos `cabecera`, `sesion`, `navegacion` y `pie` y la **ausencia** de una segunda
  declaración de `<header>`/`<footer>` propios.

#### R-ACTA-015 — Fieldset "Datos del Acta"
> **EL SISTEMA DEBE** renderizar el fieldset `Datos del Acta` con los campos `Motivo de Acta`
> (selector), `Tipo Movimiento` (selector) y `Fecha Acta` (valor precargado con la fecha del servidor
> y no editable en el alta), y **NO DEBE** mostrar el campo `Nro. Acta` en el modo alta.

* *Criterio de verificación*: test de vista que verifica los 3 rótulos y la ausencia de `Nro. Acta`.

#### R-ACTA-016 — Opciones de los selectores (verbatim)
> **EL SISTEMA DEBE** ofrecer en `Motivo de Acta` exactamente las opciones
> `Seleccionar...` (`N`), `Falta Documentación de Fletes` (`F`),
> `Falta Documentación de Prod. Prim.` (`P`) y `Sin Documentación` (`T`); y en `Tipo Movimiento`
> exactamente `Seleccionar...` (`N`), `Salida` (`S`) y `Entrada` (`E`), con los valores persistidos
> tal cual.

* *Criterio de verificación*: test de vista que compara el conjunto de `(valor, texto)` de cada
  `<select>` contra las tablas de §3.2.

#### R-ACTA-017 — Fieldsets de las partes
> **EL SISTEMA DEBE** renderizar tres fieldsets lado a lado —`Vendedor`, `Destinatario` y
> `Transportista`— con los campos CUIT, Razón Social, Domicilio, Localidad y Provincia, donde
> Localidad y Provincia se muestran en modo lectura y son los valores derivados del CUIT.

* *Criterio de verificación*: test de vista que verifica las 3 leyendas y los 15 controles
  (5 por fieldset) con su `label` asociado.

#### R-ACTA-018 — Derivación por CUIT
> **CUANDO** el usuarioIngresse un CUIT válido en cualquiera de los tres roles, **EL SISTEMA DEBE**
> derivar y mostrar la Razón Social, el Domicilio, el código de Localidad y el nombre de la Provincia
> del contribuyente, **NO DEBE** persistir la Provincia (no es columna) y **NO DEBE** exigir que el
> usuario los escriba a mano.

* *Criterio de verificación*: test unitario del servicio de derivación con un contribuyente conocido;
  test de vista que verifica que los campos de Localidad/Provincia son de sólo lectura.

#### R-ACTA-019 — Fieldset "Datos del Transporte"
> **EL SISTEMA DEBE** renderizar el fieldset `Datos del Transporte` con los campos `Patente Chasis`,
> `Patente Acoplado`, `T. de Comp.` (selector con `Carta Porte`/`Factura`/`Remito`/`DT-e`/`DT-v`/
> `Guia Forestal` y la opción vacía `Seleccionar...`), `N° Comp.` y `Flete (Km)`.

* *Criterio de verificación*: test de vista que verifica los 5 rótulos y las 6 opciones del selector
  de tipo de comprobante.

#### R-ACTA-020 — Grilla de productos
> **EL SISTEMA DEBE** renderizar el fieldset `Productos` con una grilla de columnas `Código`,
> `Descripción` y `Tn.` y una acción `Eliminar` por fila, y un botón `+` que agrega una fila de
> producto vacía; el selector de producto debe listar únicamente los productos con
> `ProdAnula = 0`.

* *Criterio de verificación*: test de vista que verifica los encabezados y la presencia del botón
  `+`; test del endpoint/catálogo de productos que verifica el filtro `ProdAnula = 0`.

#### R-ACTA-021 — Cantidad según el tipo de cálculo del producto
> **CUANDO** el producto seleccionado tenga `ProdTipCal = 3`, **EL SISTEMA DEBE** deshabilitar el
> campo de cantidad y forzar su valor a `1`; en caso contrario **DEBE** habilitarlo y dejar que el
> usuario lo informe.

* *Criterio de verificación*: test de vista con un producto `ProdTipCal = 3` que verifica el
  atributo `disabled` y el valor `1` forzado; test con `ProdTipCal <> 3` que verifica que el campo
  queda editable.

#### R-ACTA-022 — Precio del producto
> **EL SISTEMA DEBE** calcular el precio fijo del producto (`F276RFijo`) con la vigencia de
> `PRODUCT1` cuyo `ProdFchVig <= F276FchMov` (la más reciente), tomando `ProdImpu1` si
> `F276Clasif = 'S'` y `ProdImpu3` en caso contrario, y **DEBE** inicializar el precio informado
> (`F276RSPre`) con ese valor.

* *Criterio de verificación*: test unitario con dos vigencias del mismo producto que verifica que
  se toma la vigente a la fecha del acta y la columna correcta según la clasificación.

#### R-ACTA-023 — Máximo de productos
> **SI** la grilla de productos tiene más de **6** filas, **ENTONCES EL SISTEMA DEBE** rechazar el
> alta con el mensaje verbatim `La cantidad maxima de productos que se pueden agregar es de seis`.

* *Criterio de verificación*: test MockMvc con 7 filas que verifica el mensaje renderizado y que no
  se insertó ninguna fila en `F22761`.

#### R-ACTA-024 — Confirmación antes de guardar
> **CUANDO** el usuario active `Confirmar`, **EL SISTEMA DEBE** solicitar confirmación explícita con
> el texto verbatim `¿Confirma que desea enviar el Formulario de Acta?` y **NO DEBE** persistir nada
> si el usuario cancela.

* *Criterio de verificación*: prueba de navegador que cancela la confirmación y comprueba que no se
  ejecutó el alta; el `confirm()` nativo es JS puntual permitido (G6).

#### R-ACTA-025 — Acciones del formulario
> **EL SISTEMA DEBE** ofrecer las acciones `Confirmar` y `Cancelar`, y **CUANDO** se cancele,
> **DEBE** volver a la pantalla de listado (`/actas`) sin persistir cambios.

* *Criterio de verificación*: test MockMvc que verifica la redirección a `/actas` tras cancelar y
  que la tabla `F2276` no registra filas nuevas.

### 4.3 Validaciones de negocio (verbatim)

#### R-ACTA-026 — Motivo de Acta obligatorio
> **SI** el `Motivo de Acta` está en `Seleccionar...` (`N`), **ENTONCES EL SISTEMA DEBE** rechazar
> el alta con el mensaje verbatim `Seleccionar el motivo de Acta` y **NO DEBE** persistir nada.

* *Criterio de verificación*: test MockMvc que verifica el texto exacto y la ausencia de escritura.

#### R-ACTA-027 — Tipo de Movimiento obligatorio
> **SI** el `Tipo Movimiento` está en `Seleccionar...` (`N`), **ENTONCES EL SISTEMA DEBE** rechazar el
> alta con el mensaje verbatim `Debe seleccionar Tipo de Movimiento` y **NO DEBE** persistir nada.

* *Criterio de verificación*: test MockMvc que verifica el texto exacto y la ausencia de escritura.

#### R-ACTA-028 — CUIT de las tres partes
> **SI** el CUIT del Vendedor, del Destinatario o del Transportista no es un CUIT válido
> (validación `Nuc_ValidaCuit`), **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje
> verbatim correspondiente (`Debe ingresar CUIT del Vendedor`,
> `Debe ingresar CUIT del Destinatario`, `Debe ingresar CUIT del Transportista`), evaluando los tres
> roles **en ese orden**.

* *Criterio de verificación*: test MockMvc parametrizado (3 casos) que compara el texto renderizado
  contra las cadenas literales y verifica que el primer CUIT inválido corta la evaluación.

#### R-ACTA-029 — Razón Social obligatoria
> **SI** el CUIT de una parte está informado y su Razón Social está vacía, **ENTONCES EL SISTEMA DEBE**
> rechazar el alta con el mensaje verbatim `Debe ingresar Razón Social del Vendedor` /
> `Debe ingresar Razón Social del Destinatario` / `Debe ingresar Razón Social del Transportista`.

* *Criterio de verificación*: test MockMvc parametrizado (3 casos) con CUIT informado y razón social
  vacía.

#### R-ACTA-030 — Datos del transporte completos
> **SI** falta el tipo de comprobante, el número de comprobante, la patente de chasis, la patente de
> acoplado o el flete, **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> `Ingrese todos los datos del Transporte`.

* *Criterio de verificación*: test MockMvc parametrizado (5 casos, una omisión por vez).
* *Nota (D-ACTA-23, Gate 2)*: según la KB (`PPActaDetalle`, Sub 'Validar'), el check del WebPanel cubre los 5 campos del transporte (incluido `F276TCpbte` y `F276NCpbte`) y se evalúa **antes** de `&F2276.Save()`. Por eso el mensaje `Ingrese todos los datos del Transporte` "gana" cuando falta cualquiera de esos 5. Las rules de la transacción `F2276` (`Debe seleccionar un tipo de comprobante` / `Debe ingresar un nro. de comprobante` / `La fecha no puede ser superior a la fecha actual`) sólo se evalúan si el check del WebPanel pasa y son, en la práctica, redundantes para tipo/nro de comprobante. R-ACTA-033 se mantiene como contrato de la transacción (alcanzable por otros puntos de entrada), no como segundo mensaje en el flujo de alta manual.

#### R-ACTA-031 — Detalle de productos obligatorio
> **SI** la grilla de productos está vacía, **ENTONCES EL SISTEMA DEBE** rechazar el alta con el
> mensaje verbatim `Debe ingresar el detalle de los productos`.

* *Criterio de verificación*: test MockMvc sin filas de producto.

#### R-ACTA-032 — Restricción de Entrada
> **SI** la clasificación es `Entrada` (`E`) y el `Motivo de Acta` no es `P`
> (`Falta Documentación de Prod. Prim.`), **ENTONCES EL SISTEMA DEBE** rechazar el alta con el
> mensaje verbatim `El movimiento de Entrada solo puede generar un Acta por falta de Documentación de
> Prod. Primaria`.

* *Criterio de verificación*: test MockMvc con `F276Clasif='E'` y `F276ActaTi='F'`.

#### R-ACTA-033 — Comprobante obligatorio
> **SI** el tipo de comprobante o el número de comprobante están vacíos, **ENTONCES EL SISTEMA DEBE**
> rechazar el alta con el mensaje verbatim `Debe seleccionar un tipo de comprobante` o
> `Debe ingresar un nro. de comprobante`, respectivamente.

* *Criterio de verificación*: test MockMvc parametrizado (2 casos).

#### R-ACTA-034 — Fecha no futura
> **SI** la fecha de movimiento del Acta es posterior a la fecha del servidor, **ENTONCES EL SISTEMA
> DEBE** rechazar el alta con el mensaje verbatim `La fecha no puede ser superior a la fecha actual`.

* *Criterio de verificación*: test unitario del servicio con una fecha futura.

#### R-ACTA-035 — Turno cerrado
> **CUANDO** se intente guardar un Acta y el turno asociado esté cerrado
> (`PPrim_TurnoEstado(...) = 'C'`), **EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> `No se puede generar la guia porque el turno asociado esta cerrado` y **NO DEBE** persistir nada.

* *Criterio de verificación*: test unitario del servicio con el turno en estado `C`; IT que verifica
  que `F2276` no recibió filas.

#### R-ACTA-036 — Número de formulario generado
> **CUANDO** el alta se ejecute sin número de Acta (`F276Id = 0`), **EL SISTEMA DEBE** generar el
> número **atómicamente** a partir del contador persistente `GES_SUCN` de la sucursal
> (`SucNroId`, `SucTipo`, `SucNuForm = 2276`, `SucNuId = 'MAN'`): debe **incrementar**
> `SucUltNum` y **devolver** el valor incremented; si la fila **no existe**, **DEBE** insertar
> `(SucNroId, SucTipo, SucNuForm, 'MAN', 1)` y **devolver 1**, y persistir el número obtained como
> `F276Id`; **SI** en cambio se informa un `F276Id` que **no** cae en un rango manual de
> `FORMSUCU`, **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> `Debe ingresar un Nro. de Formulario Generado mediante el Sistema`.

* *Criterio de verificación*: test unitario con un `F276Id` dentro y fuera de un rango manual de
  `FORMSUCU`; IT que verifica que el `F276Id` persistido es el siguiente del contador `GES_SUCN`
  de esa sucursal y que **no** colisiona con la PK `(F276Id, SucTipo, SucNroId)`.

#### R-ACTA-037 — Precio inferior al registrado
> **SI** el precio informado de un producto es distinto de cero y es **menor** que el precio fijo
> registrado (`F276RFijo`), **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> `El precio ingresado no puede ser inferior al Precio registrado del Producto`; **SI** el precio
> informado es cero, **DEBE** asumir el precio fijo registrado.

* *Criterio de verificación*: test unitario con precio menor / igual / cero.

#### R-ACTA-038 — Producto duplicado
> **SI** la grilla de productos contiene dos filas con el mismo `ProdCod`, **ENTONCES EL SISTEMA DEBE**
> rechazar el alta con el mensaje verbatim `El producto ya ha sido ingresado`.

* *Criterio de verificación*: test MockMvc con dos filas del mismo producto.

#### R-ACTA-039 — Producto no seleccionado
> **SI** se intenta agregar una fila de producto sin `ProdCod`, **ENTONCES EL SISTEMA DEBE** impedir
> la alta de esa fila con el mensaje verbatim `Debe seleccionar algun producto`.

* *Criterio de verificación*: test de vista o de endpoint de alta de detalle que verifica el mensaje
  al confirmar una fila sin producto.

#### R-ACTA-040 — Orden de evaluación de las validaciones
> **EL SISTEMA DEBE** evaluar las validaciones del alta en el mismo orden del legacy
> (`motivo` → `nro. de acta` → `tipo de movimiento` → `CUIT vendedor` → `CUIT destinatario` →
> `CUIT transportista` → `razón social` → `datos de transporte` → `detalle de productos` →
> `máximo de productos`), de modo que ante varias infracciones sólo se muestre el **primer** mensaje
> del legacy.

* *Criterio de verificación*: test MockMvc con un formulario que infringe 4 reglas a la vez que
  verifica que se renderiza sólo el mensaje de la primera.

### 4.4 Control de estado, lotes y turno

#### R-ACTA-041 — Validación de lotes antes del alta
> **CUANDO** el usuario solicite el formulario de alta (`GET /actas/nueva`), **EL SISTEMA DEBE**
> verificar, mediante el servicio de validación de lotes del núcleo
> (`ServicioEstadoDeForm.hayFormularioAbierto(sucursal, tipo, turno)`), si el turno tiene formularios
> abiertos; **SI** los hay, **DEBE** bloquear la creación e informar el estado, y **NO DEBE** renderizar
> el formulario de alta.

* *Criterio de verificación*: `@WebMvcTest` con `ServicioEstadoDeForm` simulado que devuelve `true` y
  `false`; verificación de que el template del formulario sólo se renderiza cuando el bloqueo no
  está activo.

#### R-ACTA-042 — Bloqueo por turno cerrado
> **SI** el turno de la sesión está cerrado, **ENTONCES EL SISTEMA DEBE** impedir el alta y **NO DEBE**
> insertar filas en `F2276`.

* *Criterio de verificación*: test unitario del servicio + IT que verifica el conteo de filas antes y
  después.

#### R-ACTA-043 — Cierre de lotes sin Actas pendientes
> **CUANDO** se cierre el turno/lote, **EL SISTEMA DEBE** validar que no queden Actas en estado
> pendiente; una Acta se considera **pendiente** cuando su `F276Estad` es distinto de `C`
> (confirmada) y de `A` (anulada) —incluido el valor nulo— y **NO DEBE** completar el cierre si
> existen Actas pendientes del turno.

* *Criterio de verificación*: test unitario con un turno que tiene un Acta con `F276Estad` vacío
  (debe bloquear) y otro con todas `C`/`A` (debe permitir); revisión de la integración con
  `ServicioCierreTurno` (`06-turnos`).

#### R-ACTA-044 — Origen de sucursal, tipo y turno
> **EL SISTEMA DEBE** tomar `SucTipo`, `SucNroId` y `TurNroId` del **contexto de sesión** (nunca del
> formulario enviado por el cliente) y **NO DEBE** aceptar valores de sucursal, tipo o turno
> arbitrarios en el `POST` de alta.

* *Criterio de verificación*: test MockMvc que envía `sucNroId`/`turNroId` distintos en el body y
  verifica que la fila persistida usa los del contexto.

### 4.5 Persistencia y paridad de base de datos

#### R-ACTA-045 — Valores por defecto del alta
> **CUANDO** se inserte un Acta, **EL SISTEMA DEBE** persistir los valores por defecto del legacy:
> `F276GenWeb = 'A'`, `F276Operad = &Context.Usuario.ToUpper()`, `CPCPerId = &Context.PersonaId`,
> `F276Estad = 'C'`, `F276Multa = 0`, `F276RMarCM = 'N'` (por fila de detalle), `F276ForPag = 'EFE'`,
> `F276FchMov` = fecha del acta, `F276FeGra` = `ServerNow()`, `TurNroId` = turno de la sesión y
> `F276Vers` = `BuscarVersionActual` (fila de `VERSIONE` con `VerApliId = 'SWP'`, armando
> `VerVersion + "." + VerRevisio + "." + VerBuild`).

* *Criterio de verificación*: IT que inserta un Acta y compara los 11 valores con la fila leída de
  `F2276`.

#### R-ACTA-046 — Tipo de formulario (`F276FormTi`)
> **EL SISTEMA DEBE** calcular `F276FormTi` al insertar, reproduciendo `ActaDeterminaTipoFormulario`:
> `1` si `F276Clasif = 'E'` y el contribuyente **no** está en infracción, `2` si `F276Clasif = 'E'` y
> está en infracción, `3` si `F276Clasif = 'S'`; la condición de infracción se cumple cuando el
> contribuyente existe y su `ConEstado` **no** es `12` ni `14` (transitorio / transitorio especial).

* *Criterio de verificación*: test unitario parametrizado con (clasificación, estado del
  contribuyente) → valor esperado, incluyendo el caso "contribuyente inexistente" ⇒ `1`.

#### R-ACTA-047 — Detalle de productos
> **EL SISTEMA DEBE** persistir una fila en `F22761` por cada producto de la grilla, con
> `F276Id`, `SucTipo`, `SucNroId` (de la cabecera), `ProdCod`, `F276RCant`, `F276RFijo`,
> `F276RSPre`, `F276RPreCM`, `F276RAlicu = 1`, `F276RMarCM = 'N'`, `F276RImpIB` y `F276RImpCM`
> **calculados con las fórmulas verificadas del legacy**, y `F276RZona` con el valor por defecto
> del dominio (`' '`/vacío, ver SA-ACTA-06):
> - `F276RFijo` = `F2276_BusProTarifa` (última vigencia de `PRODUCT1` con `ProdFchVig <= F276FchMov`;
>   `ProdImpu1` si `F276Clasif = 'S'`, `ProdImpu3` en caso contrario; `0` si no hay vigencia);
> - `F276RPreCM` = `F2276_BusProTarCM` (`0` salvo `F276RMarCM = 'S'`; entonces `ProdImpu2` si
>   `F276Clasif = 'S'`, `ProdImpu4` en caso contrario);
> - `F276RImpIB` = `Nuc_CalculoImpIB(ProdTipCal, F276RCant, F276RMImpo, F276RFijo, 0, F276RAlicu)`
>   ⇒ `MAX(Round(RCcant * RFijo, 2), Round(RMImpo * RAlicu / 100, 2))`, con
>   `Calculo1 = RFijo` cuando `ProdTipCal = 3`, y `0` si `ProdTipCal` ∉ {1, 2, 3};
> - `F276RImpCM` = `F276RCant * F276RPreCM`.

* *Criterio de verificación*: IT que inserta un Acta con 2 productos y verifica las 2 filas de
  `F22761` con sus importes; test unitario parametrizado de `Nuc_CalculoImpIB` con los 3
  `ProdTipCal` y con `ProdTipCal` fuera de {1,2,3}.

#### R-ACTA-048 — Pase del puesto caminero
> **CUANDO** el alta se complete, **EL SISTEMA DEBE** registrar el pase del puesto en `F2276PAS`
> con `F276PPaseI` = siguiente índice del pase de esa Acta, `F276PFecha` = `ServerNow()`,
> `F276PUsuar` = usuario de la sesión, `F276PaseST` = tipo de la sesión (`'PUE'`),
> `F276PaseSN` = sucursal de la sesión y `F276PaseCo` = comisión de la sesión.

* *Criterio de verificación*: IT que verifica la fila insertada en `F2276PAS` y que el índice es
  correlativo.

#### R-ACTA-049 — Atomicidad
> **SI** cualquiera de las validaciones o la persistencia falla, **ENTONCES EL SISTEMA DEBE**
> revertir **toda** la operación (cabecera, detalle y pase) y **NO DEBE** dejar Actas parciales.

* *Criterio de verificación*: IT que fuerza un error en la segunda fila de detalle y verifica que
  `F2276`, `F22761` y `F2276PAS` quedan sin cambios.

#### R-ACTA-050 — Paridad de mapeo de columnas
> **EL SISTEMA DEBE** mapear las **54 columnas físicas** de `F2276`, las **15** de `F22761` y las
> **9** de `F2276PAS` con `@Table(name = "...")` y `@Column(name = "...")` de nombre físico exacto,
> **NO DEBE** generar DDL ni migraciones y **DEBE** mantener `ddl-auto=none`.

* *Criterio de verificación*: test que compara el mapeo JPA contra `INFORMATION_SCHEMA.COLUMNS`;
  revisión del diff que confirma que no hay `CREATE TABLE`, `ALTER TABLE`, `schema.sql`, `data.sql`,
  Flyway ni Liquibase, y que `spring.jpa.hibernate.ddl-auto` sigue en `none`.

#### R-ACTA-051 — Columnas huérfanas
> **EL SISTEMA DEBE** mapear también las columnas huérfanas `F2276.PueCod`, `F2276.CPCId` y
> `F22761.F276Corrig`, dejándolas con el valor por defecto de aplicación (`null`) y **NO DEBE**
> redesignearlas ni eliminarlas.

* *Criterio de verificación*: test que verifica la presencia de los tres atributos en la entidad y que
  su valor por defecto es `null` tras un alta.

#### R-ACTA-052 — Caracteres fijos y `trim`
> **EL SISTEMA DEBE** leer los atributos `char(n)` de `F2276`, `F22761` y `F2276PAS` con `RTRIM`
> (para no exponer los espacios de relleno) y **DEBE** escribir los valores de texto **recortados**.

* *Criterio de verificación*: IT que lee un `F276Operad` real (`'SEBAM     '`) y verifica que el
  valor expuesto es `SEBAM`.

### 4.6 Requisitos no funcionales

#### R-ACTA-053 — CSS propio basado en tokens
> **EL SISTEMA DEBE** implementar el estilo de ambas pantallas con **CSS propio basado en custom
> properties (tokens)**, **NO DEBE** usar Bootstrap, Tailwind ni ningún otro framework CSS, **NO DEBE**
> referenciar recursos por CDN ni fuentes remotas en runtime, y **NO DEBE** introducir `npm`, bundlers
> ni paso de build de assets.

* *Criterio de verificación*: `grep` sobre `static/css/**` y `templates/puestos/**` sin apariciones de
  `bootstrap`, `tailwind`, `cdn.`, `https://`, `unpkg`, `jsdelivr` ni `node_modules`; todos los
  colores de las hojas nuevas provienen de `var(--atp-*)`.

#### R-ACTA-054 — Responsive
> **EL SISTEMA DEBE** mantener ambas pantallas utilizables y sin desbordes horizontales a **375 px**
> y a **1440 px** de ancho.

* *Criterio de verificación*: inspección visual en los 2 anchos (captura de evidencia).

#### R-ACTA-055 — Accesibilidad AA
> **EL SISTEMA DEBE** mantener contraste ≥ 4,5:1, asociar un `<label for>` a **cada** control
> (incluidos los de la grilla de productos), garantizar foco visible y navegación por teclado, y
> marcar los errores de validación con `aria-describedby` y `role="alert"`.

* *Criterio de verificación*: `axe-core` in-page sin hallazgos críticos; test de vista que verifica
  la presencia de `th:errorclass` y `th:errors` por campo.

#### R-ACTA-056 — Layout base
> **EL SISTEMA DEBE** extender el layout base del proyecto y **NO DEBE** duplicar en las plantillas
> la cabecera institucional, la barra de sesión, el menú principal ni el pie.

* *Criterio de verificación*: revisión del HTML renderizado: 1 `<header>` y 1 `<footer>` por página,
  provenientes de `layout/base`.

#### R-ACTA-057 — Sesión y CSRF
> **EL SISTEMA DEBE** exigir sesión autenticada y token CSRF válido en el `POST` de alta, y **NO DEBE**
> devolver el formulario a un usuario sin sesión.

* *Criterio de verificación*: test MockMvc que comprueba `302` al login sin sesión y `403` con CSRF
  inválido.

#### R-ACTA-058 — Sincronización de la colección Postman
> **SI** la implementación agrega, modifica o elimina un endpoint del contrato REST,
> **ENTONCES EL SISTEMA DEBE** actualizar
> `docs/postman/ATPPuestos-API.postman_collection.json` en el mismo commit.

* *Criterio de verificación*: revisión del diff del commit. (Decisión provisional D-ACTA-04: el
  módulo **no** agrega endpoints REST ⇒ la colección no requiere cambios.)

#### R-ACTA-059 — No regresión
> **CUANDO** se implemente el módulo, **EL SISTEMA DEBE** mantener en verde las suites existentes de
> `01-nucleo-datos`, `02-abm-productos`, `03-login`, `06-turnos` y `08-control-cargas`, y **NO DEBE**
> modificar `Formulario2276` ni `Formulario2276Repositorio` (proyección de lectura usada por
> `06-turnos`).

* *Criterio de verificación*: `./mvnw clean test` en verde; `git diff` sin cambios en
  `nucleo/domain/Formulario2276.java` ni en `nucleo/repository/Formulario2276Repositorio.java`.

#### R-ACTA-060 — Higiene de configuración
> **EL SISTEMA DEBE** mantener los archivos de configuración por debajo de **500 líneas**, **NO DEBE**
> versionar secretos ni credenciales y **DEBE** usar sólo las variables de entorno ya definidas.

* *Criterio de verificación*: escaneo de secretos sobre el diff; conteo de líneas de los archivos
  tocados.

---

## 5. Trazabilidad

### 5.1 Requisito legacy → requisito nuevo

| Origen legacy (evidencia KB) | Requisito nuevo | Prueba prevista |
|---|---|---|
| `ActaListadoWC.Start` (`LoadContext`, `&TurNroId <= 0` ⇒ `Debe iniciar un turno...`) | R-ACTA-002 | `MvcActasTurnoInactivo` |
| `Grid276.ControlWhere` (`TurNroId`, `SucNroId`, `SucTipo`, `F276Estad <> 'A'`, `F276GenWeb = 'A'`) | R-ACTA-003 | `ActaRepositoryIT` |
| `Grid276.ControlOrder` (`TurNroId, SucNroId, SucTipo, (F276Id)`) | R-ACTA-004 | `ActaListadoIT` |
| Columnas de `Grid276` (7 visibles) | R-ACTA-005 | `MvcGrillaColumnas` |
| `Grid276.Load` ⇒ `Do Case` (vendedor → destinatario → transportista) | R-ACTA-006 | `ActaServiceTest#contraparte` |
| `&F276Total = F276Total` (fórmula) | R-ACTA-007 | `ActaServiceTest#total` |
| `ProPri_VerManual` (`ForRecIni <= F276Id < ForRecIni + ForRecCant`) | R-ACTA-008 | `ActaServiceTest#genManual` |
| `F2276_GenNroForm` → contador `GES_SUCN.SucUltNum` (§3.11) | R-ACTA-036 | `ActaServiceTest#numeracion`, `ActaNumeracionIT` |
| `PPrim_VerificaGenMan` (rangos manuales de `FORMSUCU`) | R-ACTA-036 | `ActaServiceTest#numeroManual` |
| `BtnNuevo` (caption `Nuevo Acta`) → `PPActaDetalle.Call(&Context.Turno,'A')` | R-ACTA-009 | `MvcBotonCrear` |
| `Grid276.MaxRows = 10` | R-ACTA-010 | `MvcPaginacion` |
| (extensión de UX solicitada) | R-ACTA-011, R-ACTA-012 | `MvcFiltros` |
| `BtnNuevo` / `BtnCancel` (`Cancelar`) | R-ACTA-025 | `MvcCancelar` |
| `PPActaDetalle` fieldset `Group4` (`Datos del Acta`) | R-ACTA-015, R-ACTA-016 | `MvcFormularioAlta` |
| `PPActaDetalle` fieldsets `Group1`/`Group2`/`GroupTransportista` | R-ACTA-017, R-ACTA-018 | `MvcFormularioAlta` |
| `PPActaDetalle` fieldset `Group3` (`Datos del Transporte`) | R-ACTA-019 | `MvcFormularioAlta` |
| `PPActaDetalle` fieldset `Group6` (`Productos`) + botón `+` (Figma) | R-ACTA-020 | `MvcGrillaProductos` |
| `ActaCargarDetalle.Start` (`ProdTipCal = 3` ⇒ cantidad 1 y deshabilitada) | R-ACTA-021 | `MvcCantidadProducto` |
| `F2276_BusProTarifa` / `F2276_BusProTarCM` | R-ACTA-022 | `ActaServiceTest#precio` |
| `Nuc_CalculoImpIB` (fórmula verificada de `F276RImpIB`) | R-ACTA-047 | `ActaServiceTest#impuestoIb` |
| `BuscarVersionActual` → `VERSIONE` (origen de `F276Vers`) | R-ACTA-045 | `ActaServiceTest#version` |
| `PPActaDetalle.Validar` (`&Productos.Count > 6`) | R-ACTA-023 | `MvcMaxProductos` |
| `BtnConfirmar.JSEvent('onclick', "confirm('¿Confirma que desea enviar el Formulario de Acta?')")` | R-ACTA-024 | Prueba de navegador |
| `PPActaDetalle.Validar` — 14 `msg(...)` | R-ACTA-026…R-ACTA-032, R-ACTA-038, R-ACTA-039 | `MvcValidaciones` |
| `F2276` rules (`error(...)` × 3) | R-ACTA-033, R-ACTA-034, R-ACTA-035 | `ActaServiceTest#reglas` |
| `F2276` rules (`PPrim_VerificaGenMan` + `F2276_GenNroForm`) | R-ACTA-036 | `ActaServiceTest#numeracion` |
| `F2276` rules (`Error('El precio ingresado...')`) | R-ACTA-037 | `ActaServiceTest#precio` |
| `F2276` rules (`default(...)` × 6, `F276FeGra = ServerNow()`) | R-ACTA-045 | `ActaAltaIT` |
| `ActaDeterminaTipoFormulario` | R-ACTA-046 | `ActaServiceTest#formTi` |
| `PPActaDetalle.GridToBC` | R-ACTA-047 | `ActaAltaIT` |
| `F2276_RegPasePuestoV2` | R-ACTA-048 | `ActaAltaIT` |
| `PPActaDetalle.'Confirmar'` (`commit`/`rollback`) | R-ACTA-049 | `ActaAltaIT` |
| G-DATA `INFORMATION_SCHEMA` (54/15/9 columnas) | R-ACTA-050, R-ACTA-051 | `ActaMapeoIT` |
| `char(n)` con relleno (`F276Operad = 'SEBAM     '`) | R-ACTA-052 | `ActaMapeoIT` |
| `openspec/constitution/stack.md` §2 | R-ACTA-053 | `CssTokensActas` |
| `openspec/constitution/stack.md` §2 (AA) | R-ACTA-055 | `MvcAccesibilidad` |
| `openspec/constitution/conventions.md` §2 (layout) | R-ACTA-056 | `MvcLayoutUnico` |
| `03-login` (Spring Security + CSRF) | R-ACTA-057 | `MvcCsrf` |
| G-POSTMAN | R-ACTA-058 | Revisión de diff |
| `openspec/constitution/conventions.md` §1 (G5, G2) | R-ACTA-059, R-ACTA-060 | `mvn clean test` |

### 5.2 Matriz de paridad funcional

| Operación legacy | Requisito | Prueba prevista |
|---|---|---|
| Listar actas del turno | R-ACTA-001, R-ACTA-003 | `ActaListadoIT` |
| Orden de la grilla | R-ACTA-004 | `ActaListadoIT` |
| Columnas de la grilla | R-ACTA-005 | `MvcGrillaColumnas` |
| Ver contraparte (CUIT / razón social) | R-ACTA-006 | `ActaServiceTest#contraparte` |
| Ver total del acta | R-ACTA-007 | `ActaServiceTest#total` |
| Ver "Gen. Manual?" | R-ACTA-008 | `ActaServiceTest#genManual` |
| Botón "Nuevo Acta" | R-ACTA-009 | `MvcBotonCrear` |
| Alta manual del acta | R-ACTA-014…R-ACTA-025 | `MvcFormularioAlta`, `ActaAltaIT` |
| Validaciones del alta (14 mensajes) | R-ACTA-026…R-ACTA-040 | `MvcValidaciones` |
| Pase del puesto | R-ACTA-048 | `ActaAltaIT` |
| Numeración del acta | R-ACTA-036 | `ActaServiceTest#numeracion` |
| Tipo de formulario | R-ACTA-046 | `ActaServiceTest#formTi` |
| Impresión del acta | — (OUT) | — |

---

## 6. Supuestos abiertos

| ID | Pregunta | Decisión provisional | Riesgo si se resuelve distinto | Acción para resolver |
|---|---|---|---|---|
| **SA-ACTA-01** | ¿La ruta de página debe ser `/actas` o `/formularios-actas`? | `/actas` (convención `/<recurso>`) | Bajo | Confirmar con el responsable funcional; ajustar `ActasController` y los `th:href`. |
| **SA-ACTA-02** | El legacy, tras guardar, invoca el WS `WsActaControlador` (`WSActa`) para generar el F506 y la multa, y muestra `El acta se ha creado correctamente` o el mensaje de error del WS. **¿El módulo debe invocar ese WS?** | **NO** (OUT): sin especificación del WS no se puede evaluar su respuesta. Se persiste el acta + pase y se muestra `El acta se ha creado correctamente`. | Medio | Pedir el contrato del WS (`WsActaControlador`: URL, payload `SDTActaPK`, respuesta) y decidir su inclusión en un módulo posterior. |
| **SA-ACTA-03** | ~~¿Cómo se obtiene en producción el siguiente número de Acta?~~ | **CERRADO (2026-11-04)**: la fuente es el **contador persistente `GES_SUCN.SucUltNum`** (por `SucNroId`, `SucTipo`, `SucNuForm = 2276`), que se incrementa **atómicamente**. `FORMSUCU` **no** participa de la numeración (es el registro de *formularios recibidos*). `MAX(F276Id)` es sólo un indicio y **no** debe usarse como fuente (en las sucursales 3 y 7 difiere del contador). | — (resuelto) | Evidencia: Source verbatim de `F2276_GenNroForm` y tabla `GES_SUCN` (§3.11); contraste empírico `GES_SUCN` vs `MAX(F2276.F276Id)` del 2026-11-04. Decisiones: D-ACTA-19 (incremento atómico) y D-ACTA-21 (fuente autoritativa). |
| **SA-ACTA-04** | ¿Cuál es el estado "Pendiente" de un Acta? En `F2276` no existe el valor `'P'` (sólo `C`, `A` y vacío); el concepto de pendiente vive en `LOTES227.LF276Estad` (`'P'`). | "Pendiente" = `F276Estad` distinto de `C` y de `A` (incluido `NULL`). El cierre de lote usa además `ServicioEstadoDeForm` (lote `C` con `LF276FchHa <= hoy`). | Medio | Confirmar con negocio qué estado debe bloquear el cierre y si el bloqueo aplica al acta o al lote. |
| **SA-ACTA-05** | `F276CodBar` es un atributo de la KB que **no** tiene columna física (verificado). ¿Debe calcularse y mostrarse? | No se persiste ni se muestra (no hay columna). | Bajo | Confirmar si el código de barras del acta se genera en otro proceso (WS `WSActa` / `F2276_CodBar`). |
| **SA-ACTA-06** | `F22761.F276RZona` es parte de la PK (`char(1)`) y la KB no le asigna valor en `GridToBC`. ¿Qué valor se persiste? | `null`/vacío (el motor lo convierte a `' '` en `char(1)`), replicando el `GridToBC` legacy que no lo informa. | Bajo | Verificar en `Rentas` los valores reales: `SELECT DISTINCT F276RZona FROM F22761;` y adoptar el valor observado. |
| **SA-ACTA-07** | ¿El módulo debe exponer la pantalla `ActaListadoWC_turno` (misma grilla sin botón "Nuevo Acta")? | No: se reusa la misma pantalla con el botón "Crear Acta" (OUT de la variante sin alta). | Bajo | Confirmar si algún rol (p. ej. receptoría) necesita la variante sin alta. |
| **SA-ACTA-08** | ¿La numeración manual (`&Tipo = 'M'`, con `F276Id` informado y rangos `ACTS`/`ACTI`) entra en alcance? | No: el módulo es **carga manual** (`&Tipo = 'A'`), que genera el número. La validación del número informado (R-ACTA-036) queda implementada por paridad. | Bajo | Confirmar si el rol de receptoría necesita el alta manual con número preasignado. |
| **SA-ACTA-09** | Los tipos de comprobante (`F276TCpbte`) están **hardcodeados** en el control del legacy (`Carta Porte:1, Factura:2, Remito:3, DT-e:5, DT-v:6, Guia Forestal:7`). ¿Vienen de una tabla de dominio? | Se replican como literales verbatim en la vista (paridad exacta). | Bajo | Buscar en la KB un catálogo de tipos de comprobante (`SELECT` en `INFORMATION_SCHEMA.TABLES` con nombre tipo `COMPROB`/`TCOMP`) y, si existe, modelarlo. |
| **SA-ACTA-10** | ¿La grilla de productos se implementa en línea (Figma, botón `+`) o como popup (legacy `ActaCargarDetalle`)? | **En línea**, replicando el Figma (el Figma es la fuente de verdad del diseño). El popup legacy se descarta. | Medio | Confirmar con diseño que la interacción en línea es la deseada. |
| **SA-ACTA-11** | ¿Los filtros de búsqueda son una extensión de UX (el legacy no tiene filtros) o debenCeñirse a algún catálogo? | Extensión de UX pedida por el usuario, sobre el filtro base del turno. | Bajo | Confirmar los campos de filtro con el responsable funcional. |
| **SA-ACTA-12** | ¿La pantalla de listado debe permitir cambiar de turno/sucursal? | No: el turno es el de la sesión (paridad `ActaListadoWC`). | Bajo | Confirmar con negocio. |
| **SA-ACTA-13** | ¿El cálculo de `F276Total` debe mostrarse en la grilla? El legacy lo calcula en la grilla pero **no** lo muestra (no hay columna visible para `&F276Total`). | Se calcula y se expone en el view model, pero **no** se renderiza una columna de total (paridad de columnas). | Bajo | Confirmar si se quiere una columna "Total" en la grilla. |
| **SA-ACTA-14** | `GES_SUCN.SucNuId` es `char(3) NOT NULL` y el legacy **no lo informa** al insertar la fila (`when none` del `F2276_GenNroForm`: sólo `SucNroId`, `SucTipo`, `SucNuForm`, `SucUltNum`). En producción hay 36 filas con `'MAN'` y 11 en blanco. ¿Cuál es el valor por defecto correcto? | `'MAN'` (D-ACTA-19): es el valor observado en 36 de 47 filas y el que la coordinación indiquer; el legacy dejaría la cadena vacía. | Bajo | Confirmar con el DBA qué significa `SucNuId` ('MAN' = manual?) y si existe otro valor válido (`SELECT DISTINCT SucNuId FROM GES_SUCN;` ⇒ `'MAN'` y vacío). |
| **SA-ACTA-15** | El contador `GES_SUCN.SucUltNum` puede estar **desfasado** respecto de las actas realmente cargadas (suc. 3: contador 6149 vs `MAX(F276Id)` 300002; suc. 7: 6009 vs 74824; otras: contador = `MAX+1`). ¿Se acepta el contador como fuente autoritativa (paridad estricta con el legacy) o debe tomarse `MAX(Contador, MAX(F276Id)) + 1`? | **Contador** (paridad estricta, D-ACTA-21). Tomar el máximo cambiaría la numeración respecto del legacy en las sucursales con desfase. | Medio | Confirmar con negocio/DBA si las actas con `F276Id` superior al contador (suc. 3 y 7) provienen de una carga masiva y si deben respetarse. |
| **SA-ACTA-16** | `FORMSUCU` registra los rangos de formularios **recibidos** (`ForRecFech`, `ForRecUsu`), no necesariamente **asignados**. ¿El indicador "Gen. Manual?" debe significar "el número cae en un rango registrado" (paridad con `ProPri_VerManual`) o "el número fue asignado manualmente"? | Paridad estricta: "el número cae en un rango registrado en `FORMSUCU`" (R-ACTA-008). | Bajo | Confirmar con negocio el significado funcional de la columna. |

---

## 7. Decisiones

| # | Decisión | Alternativas descartadas | Justificación |
|---|---|---|---|
| **D-ACTA-01** | Crear una **entidad nueva** `Acta` (`@Table("F2276")`) en `puestos.domain` en lugar de extender `Formulario2276`. | (a) Extender `Formulario2276` con las 43 columnas restantes. | `Formulario2276` es una **proyección de lectura de 11 columnas** usada por `06-turnos` y su PK declarada (`@Id F276Id`) es parcial: tocarla arriesga la regresión de `06-turnos` (R-ACTA-059) y mezcla dos proyecciones de la misma tabla en el núcleo. Dos entidades sobre la misma tabla son legales en Hibernate con `ddl-auto=none`; la escritura sólo la hace `Acta`. |
| **D-ACTA-02** | El módulo vive en `atp-puestos-camineros` (no en el núcleo). | Ubicarlo en `atp-nucleo-compartido`. | El menú "Formularios ACTAS" pertenece a la aplicación de Puestos Camineros y comparte `Contribuyente` (ya en `puestos.domain`). |
| **D-ACTA-03** | Toda la lógica de negocio vive en `ActaService` (`puestos.service`); el controller MVC sólo mapea, valida (`@Valid`) y delega. | (a) Lógica en el controller; (b) lógica en el repositorio. | Regla de dependencia `web → service → repository → domain` (G7) y AGENTS.md regla 9. |
| **D-ACTA-04** | El módulo **no expone endpoints REST**: sólo MVC (`/actas`, `/actas/nueva`, `POST /actas/guardar`). | Crear `/api/v1/actas` + `/api/v1/actas/nueva`. | El encargo pide un controller MVC con `Pageable`; no hay consumidor API. Evita el compromiso de G-POSTMAN (la colección no cambia). Si más adelante se agrega API, G-POSTMAN obliga a actualizarla (R-ACTA-058). |
| **D-ACTA-05** | La validación de lotes reutiliza `ServicioEstadoDeForm` del núcleo; no se reimplementa. | (a) Consultar `LOTES227` desde `ActaRepository`; (b) nuevo servicio de lotes. | El servicio ya está implementado y probado en `06-turnos` (`R-TUR-013`); duplicarlo violaría DRY y la paridad de la regla. |
| **D-ACTA-06** | Se reutilizan `ContextoSesion`, `GestorContextoSesion` y `ProveedorNavegacion` del núcleo (`03-login`). | Crear un contexto propio del módulo. | Paridad con `LoadContext`/`SetContext`; evita duplicar el SDT `Context`. |
| **D-ACTA-07** | `F276Total`, `F276AdiTot`, `F276ImpTotal`, `F276ImIBTot`, `F276ImCMTot`, `F276ProCan` y `F276GenMan` **se derivan en el servicio** y no se mapean como columnas. | (a) Mapearlos con `@Column` (falla: no existen); (b) persistirlos (prohibido: sin DDL). | Verificado contra `INFORMATION_SCHEMA`: `F2276` tiene 54 columnas y ninguna de ellas es un total. Es el mismo criterio ya aplicado en `06-turnos` (`Formulario2276Repositorio#totalizarTurno`). |
| **D-ACTA-08** | Los mensajes al usuario se transcriben **verbatim**, erratas incluidas. | Traducir/corregir la ortografía. | `openspec/constitution/conventions.md` §6: los textos visibles son parte del contrato. |
| **D-ACTA-09** | Las plantillas se crean en `templates/puestos/` (`actas-lista.html`, `acta-formulario.html`) y extienden `layout/base`. | (a) Crear un layout propio; (b) duplicar el header/menú/pie en la plantilla. | (a) duplicaría el layout; (b) viola R-ACTA-056 y la convención de no duplicar el cromo institucional. |
| **D-ACTA-10** | El estilo va en `static/css/actas.css` y los colores nuevos se agregan como tokens `--atp-actas-*` en `tokens.css`. | (a) Estilos inline; (b) CSS de terceros; (c) literales hexadecimales en la hoja. | (a) rompe la separación; (b) viola G6 y R-ACTA-053; (c) rompe la regla de "único archivo con literales" (R-ACTA-053). |
| **D-ACTA-11** | La grilla de productos se **agrega en línea** con JS puntual vanilla (botón `+` / `−`), replicando el Figma. | (a) Popup Thymeleaf como el legacy; (b) grilla editable con framework JS. | (a) contradice el Figma; (b) viola G6 (prohibido cualquier framework JS). El JS sólo manipula el DOM de la grilla; la validación real vive en el servicio. |
| **D-ACTA-12** | El `POST` de alta usa `POST /actas/guardar` con `@Valid @ModelAttribute` + `BindingResult`; si hay errores de binding se **ree-renderiza** el formulario (código HTTP 200) con los errores por campo; si el servicio devuelve un error de negocio, se re-renderiza con el mensaje verbatim. | (a) Redirección al listado en cualquier error; (b) `ResponseStatusException`. | (a) perdería los errores; (b) no permite mostrar los mensajes verbatim por campo. |
| **D-ACTA-13** | La numeración de `F276Id` se delega a un método del servicio (`generarNumeroActa`) y su fuente exacta queda abierta en SA-ACTA-03. | (a) Usar `FORMSUCU.SucUltNum` (no existe en la BD); (b) generar el número en el cliente. | (a) imposible con el esquema físico; (b) inseguro (carrera) y rompe la paridad del generador del servidor. |
| **D-ACTA-14** | La confirmación `¿Confirma que desea enviar el Formulario de Acta?` se implementa con `confirm()` nativo (JS puntual). | Confirmación en servidor con pantalla intermedia. | Paridad literal con el legacy (`BtnConfirmar.JSEvent('onclick', "confirm(...)")`) y uso permitido de JS puntual (G6). |
| **D-ACTA-15** | El módulo **no** implementa la impresión (`ActaPuesto_Chofer`) ni el PDF `ActaResumenTurno`. | Implementarlos en este módulo. | Fuera del encargo y de la G-DATA; se registran como OUT (§2.2). |
| **D-ACTA-16** | El tamaño de página por defecto es **10** y el orden por defecto es `F276Id` ascendente dentro del filtro del turno. | (a) 20; (b) sin paginación. | Paridad con `Grid276.MaxRows = 10`. |
| **D-ACTA-17** | Los campos derivados (Razón Social, Domicilio, Localidad, Provincia) se **calculan en el servidor** al recibir el CUIT, y la provincia **no** se persiste (no es columna). | (a) Enviar la provincia al backend y guardarla; (b) derivar sólo en el cliente. | (a) imposible (no hay columna); (b) el cliente no puede resolver CUIT → domicilio sin un endpoint nuevo (D-ACTA-04). |
| **D-ACTA-18** | `F276GenWeb = 'A'` se persiste siempre en el alta (paridad con `default(F276GenWeb,'A')` y con el filtro de la grilla). | Permitir elegir el origen. | El módulo es el generador de actas web; `F276GenWeb='A'` es la marca que las identifica (y fuerza `F276ImpuFl/ImIBTot/ImCMTot = 0` en el legacy). |
| **D-ACTA-19** | La numeración del Acta se implementa sobre el contador **`GES_SUCN`** con **incremento atómico** dentro de la misma transacción del alta: `UPDATE GES_SUCN SET SucUltNum = SucUltNum + 1 WHERE SucNroId=? AND SucTipo=? AND SucNuForm=2276` (o `@Lock(PESSIMISTIC_WRITE)` sobre la fila); si la fila **no existe**, se inserta `(SucNroId, SucTipo, SucNuForm, 'MAN', 1)` y se devuelve `1` (paridad exacta con el `when none` del legacy). El `SucNuId` por defecto es `'MAN'`. | (a) Reproducir el `For Each … defined by SucUltNum` literal del legacy (leer, sumar, escribir) — **descartado**: el legacy **no bloquea** la fila ⇒ dos altas simultáneas pueden obtener el mismo `F276Id` y **violar la PK** `(F276Id, SucTipo, SucNroId)`; (b) `MAX(F276Id) + 1` — **descartado**: no es la fuente del legacy y difiere del contador en las sucursales 3 y 7 (§3.11). | El legacy tiene una condición de carrera; el monolith Spring Boot con BD relacional debe corregirla **sin cambiar el resultado observable** (el número asignado). El incremento atómico es transparente para el usuario y garantiza la unicidad de la PK. `SucNuId = 'MAN'` es el valor observado en producción (SA-ACTA-14). |
| **D-ACTA-20** | Se **mantiene** `FORMSUCU` en el modelo, pero **sólo** para la detección de **números manuales** (`ProPri_VerManual` → columna "Gen. Manual?" y `PPrim_VerificaGenMan` → validación del número informado). Se implementa con una entidad propia `RangoManualFormulario` (`@Table("FORMSUCU")`, 8 columnas). | (a) Eliminar `FORMSUCU` del modelo y mostrar siempre `No` en "Gen. Manual?" — **descartado**: `ProPri_VerManual` y `PPrim_VerificaGenMan` **sí** leen `FORMSUCU` (verificado en §3.12); eliminarla rompería R-ACTA-008 y R-ACTA-036; (b) reutilizar `GES_SUCN` para ambas cosas — **descartado**: `GES_SUCN` no tiene rangos (`ForRecIni`/`ForRecCant`), sólo un contador. | La instrucción de la coordinación era "reemplazar `FORMSUCU`/`RangoManual` por `GES_SUCN`/`ContadorFormulario`" en el contexto de la **numeración**: en ese sentido se cumple (la numeración ya no usa `FORMSUCU`). Se conserva `FORMSUCU` **únicamente** para los rangos manuales, porque la KB demuestra que es su único uso real en el módulo. Son dos tablas con dos propósitos distintos: `GES_SUCN` = contador; `FORMSUCU` = rangos recibidos. |
| **D-ACTA-21** | `GES_SUCN.SucUltNum` es la **fuente autoritativa** de la numeración (paridad estricta con el legacy), aunque esté desfasada respecto de `MAX(F2276.F276Id)` en algunas sucursales. | (a) `MAX(Contador, MAX(F276Id)) + 1` — **descartado por ahora**: cambiaría la numeración respecto del legacy (SA-ACTA-15, riesgo Medio). | Es la única fuente que reproduce el legacy. El desfase se documenta en SA-ACTA-15 y se revisa con el DBA antes del cierre del módulo. |
