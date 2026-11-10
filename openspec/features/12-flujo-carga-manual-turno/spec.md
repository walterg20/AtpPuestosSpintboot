# Spec — 12 Flujo de Carga Manual de Turno (Puestos Camineros)

> **Estado**: `IMPLEMENTADO (2026-11-04)` — Gate 2 PASS (unit 143+161, IT 26+29).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-TURNO-<NNN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13), alias `atppuestos` — **solo lectura**.
>   - `WebPanel TurnosListadoAbiertos` (`Root Module/Turnos/TurnosListadoAbiertos`).
>   - `WebPanel PPActaDetalle` (`Root Module/F2776/PPActaDetalle`) — ya analizado en `09-formularios-acta`.
>   - `Procedure PPrim_TurnoEstado` (`Root Module/.../PPrim_TurnoEstado`).
>   - `DataProvider DPDolphinMenuActas` (`Root Module/Utiles/DolphinStyleMenu/DPDolphinMenuActas`).
>   - `Transaction FormSucursal` (`Root Module/Transacciones/FormSucursal`) — define `ForRecFin` como fórmula.
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` — **G-DATA 2026-11-04, sólo `SELECT`**
>   (`PPRIM_TU` = 12 columnas confirmadas; `FORMSUCU` = 8 columnas confirmadas, sin `ForRecFin`
>   físico; `ForRecFin = ForRecIni + ForRecCant - 1` es atributo fórmula de la transacción).
> **Módulo que consume (no se rediseña)**: `09-formularios-acta` — el formulario de alta es el
>   mismo; este módulo **ajusta el flujo** para que la carga manual exija seleccionar un turno
>   abierto y permita ingresar un número de acta manual.
> **Alcance de escritura de esta spec**: `openspec/features/12-flujo-carga-manual-turno/**`.

---

## 1. Propósito

Verificar (y, confirmado que sí) que el legacy **exige seleccionar un turno abierto** antes de crear
un acta en el flujo de **"Carga Manual"**, y ajustar el replatforming para que:

1. La opción de menú **"Carga Manual"** no abra el formulario de alta directo, sino un **listado
   de turnos abiertos** (paridad con `TurnosListadoAbiertos`).
2. Al seleccionar un turno, se abra el **mismo formulario de alta** de `09-formularios-acta` pero
   en **modo manual** (`Tipo = 'M'`): el `TurNroId` proviene del turno seleccionado (no del
   contexto de sesión) y el usuario ingresa el `F276Id` manual, validado contra los rangos
   registrados en `FORMSUCU`.

El formulario de alta, las validaciones de negocio, la persistencia (cabecera + detalle + pase) y
los totales **no cambian** respecto a `09-formularios-acta`; sólo cambian el **origen del
`TurNroId`** y la **visibilidad/validación del `F276Id`**.

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Enrutamiento de la opción de menú "Carga Manual" al listado de turnos abiertos (R-TURNO-001). |
| 2 | Pantalla de listado de turnos abiertos filtrada por sucursal y tipo del contexto (R-TURNO-002, R-TURNO-005). |
| 3 | Selección de un turno abierto que navega al formulario de alta en modo manual (R-TURNO-003, R-TURNO-012). |
| 4 | Validación de que el turno seleccionado esté abierto (R-TURNO-004, R-TURNO-016). |
| 5 | Estado vacío cuando no hay turnos abiertos (R-TURNO-006, R-TURNO-015). |
| 6 | Formulario de alta en modo manual con `F276Id` editable y validado (R-TURNO-008…R-TURNO-011). |
| 7 | Reutilización de la persistencia y validaciones de `09-formularios-acta` (R-TURNO-007, R-TURNO-014). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Impresión del acta y reportes | Igual que `09-formularios-acta` | Backlog |
| WS `WSActa` (F506 / multa) | Igual que `09-formularios-acta` | Backlog / SA-ACTA-02 |
| Cierre de turno / lotes | Lo define `06-turnos` | `06-turnos` |
| Endpoints REST nuevos | El módulo es sólo MVC (D-ACTA-04) | — |
| E2E automatizado | — | Fuera del módulo |

---

## 3. Modelo legacy verificado

### 3.1 Respuestas a las preguntas de alcance (verificadas en la KB)

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| **A)** ¿El flujo legacy requiere seleccionar un turno abierto antes de crear el acta? | **SÍ** | `DPDolphinMenuActas` define el ítem "Carga Manual" con `Link = TurnosListadoAbiertos.Link()`, **no** con `PPActaDetalle.Link(...)`. |
| **B)** Si SÍ: ¿qué WebPanel se invoca al seleccionar un turno? ¿Pasa `TurNroId`? | **`PPActaDetalle`** con `PPActaDetalle.Link(TurNroId, 'M')` | Evento `GridTurnos.Load` de `TurnosListadoAbiertos`: `&SelectRow.Link = PPActaDetalle.Link(TurNroId,'M') //Manual`. |
| **C)** ¿El turno seleccionado puede ser distinto del turno del contexto de sesión? | **SÍ** | `PPActaDetalle` (modo `'M'`) ejecuta `&F2276.TurNroId = &TurNroId` (el parámetro), **no** `&Context.Turno` (ver `Sub 'BeforeInsert'`: `if &Tipo = 'A' ... else &F2276.TurNroId = &TurNroId`). |
| **D)** ¿Qué validaciones corren al crear el acta desde un turno abierto (vs. desde el contexto)? | En modo `'M'` se agregan: `F276Id = 0` ⇒ "Debe ingresar un Nro. de Formulario Generado mediante el Sistema"; `!NroActaValido` ⇒ "El número de Acta no corresponde a un número generado manualmente"; y `VerificaActa`/`ObtenerTipoActaManual` validan el rango manual en `FORMSUCU`. El resto de validaciones de `PPActaDetalle` son idénticas. | `Sub 'Validar'` y `Sub 'VerificaActa'` / `Sub 'ObtenerTipoActaManual'` de `PPActaDetalle`. |
| **E)** ¿La grilla de turnos abiertos filtra por `SucNroId`/`SucTipo` del contexto? | **SÍ** | `ControlWhere` de `GridTurnos`: `TurEstado = 'A'; SucTipo = &Context.Tipo; SucNroId = &Context.Sucursal`. |

### 3.2 `WebPanel TurnosListadoAbiertos`

**Firma**: sin parámetros.

**Variables**: `&SelectRow BITMAP`, `&Context GX_SDT`, `&Today DATE`, `&Time CHARACTER(8)`,
`&Pgmname CHARACTER(128)`, `&Pgmdesc CHARACTER(256)`.

**Controles**: `Table1`, `Table2`, `GridTurnos` (`gxGrid`, `MaxRows=10`, `title="Seleccione un Turno"`,
`BackColorStyle="Report"`), `TextBlock2`.

**ControlWhere (verbatim)**:
```text
TurEstado = 'A';
SucTipo = &Context.Tipo;
SucNroId = &Context.Sucursal;
```

**ControlOrder (verbatim)**:
```text
SucNroId, SucTipo, (TurNroId)
```

**Columnas del grid** (en orden):
| # | Columna | Origen | Visible | Título |
|---|---|---|---|---|
| 1 | `SucNroId` | att:68 | No | — |
| 2 | `SucTipo` | att:69 | No | — |
| 3 | `TurNroId` | att:67 | Sí | (caption del atributo) |
| 4 | (atributo de apertura, probablemente `TurFchApe`) | att:207 | Sí | (caption del atributo, centrado) |
| 5 | `&SelectRow` | var:5 | Sí | `Seleccionar` (enlace a `PPActaDetalle.Link(TurNroId,'M')`) |

> La columna 4 se identifica como un atributo de apertura por su alineación centrada y su posición
> en el grid; la correspondencia exacta con `TurFchApe`/`TurHraApe` se confirma en la fase de
> implementación (SA-TURNO-02).

**Eventos (verbatim)**:
```text
Event GridTurnos.Load
    &SelectRow.Link = PPActaDetalle.Link(TurNroId,'M')    //Manual
EndEvent

Event Start
    &SelectRow.FromImage(selectRow)
    LoadContext(&Context)
EndEvent
```

**Mensajes verbatim**: ninguno (el WebPanel no emite `msg()`; el estado vacío se deriva del patrón
de `R-ACTA-013`). Ver SA-TURNO-01.

### 3.3 `PPActaDetalle` — diferencias del modo manual (`Tipo = 'M'`)

Reutiliza la firma `parm(in:&TurNroId, in:&Tipo)`. Cuando se invoca desde `TurnosListadoAbiertos`
se pasa `&Tipo = 'M'` (manual). Las diferencias respecto al modo alta (`'A'`, documentado en
`09-formularios-acta` §3.2) son:

| Aspecto | Modo `'A'` (alta web) | Modo `'M'` (carga manual) |
|---|---|---|
| `F276Id` (Nro. Acta) | Oculto (`ctlF276Id.Visible = false`); el sistema lo genera con `GES_SUCN` | **Visible y editable**; el usuario lo ingresa |
| `TurNroId` (BeforeInsert) | `&Context.Turno` | `&TurNroId` (turno seleccionado) |
| `F276Clasif` | Lo elige el usuario (S/E) | Se deriva del rango manual (`ObtenerTipoActaManual`: `ACTI`→`E`, `ACTS`→`S`) |
| Validaciones extra | — | `F276Id = 0` ⇒ "Debe ingresar un Nro. de Formulario Generado mediante el Sistema"; `!NroActaValido` ⇒ "El número de Acta no corresponde a un número generado manualmente" |
| `VerificaActa` | No corre | Corre: valida que `F276Id` caiga en un rango de `FORMSUCU` (`ForRecIni <= F276Id <= ForRecFin`, con `ForRecTipo` acorde a la clasificación) |
| Numeración (`GES_SUCN`) | Se incrementa | **No se incrementa** (el número lo informa el usuario) |

El resto de la validación (`Validar`), la persistencia (`BeforeInsert`, `GridToBC`, `F2276.Save`,
detalle, pase) y los totales son **idénticos** a `09-formularios-acta`.

### 3.4 `PPrim_TurnoEstado`

**Firma**: `parm(in:&SucNroId, in:&SucTipo, in:&TurNroId, out:&TurEstado)`.

**Source verbatim**:
```text
For Each
    where SucNroId  = &SucNroId
    where SucTipo    = &SucTipo
    where TurNroId   = &TurNroId
        &TurEstado = TurEstado
EndFor
```

**Comportamiento observable**: devuelve `TurEstado` del turno (`'A'` = abierto, `'C'` = cerrado). Si
el turno no existe, `&TurEstado` queda vacío.

### 3.5 `FormSucursal` — `ForRecFin` es fórmula

La transacción `FormSucursal` (tabla física `FORMSUCU`) define:

```text
ForRecFin : NUMERIC(8) // "Ultimo formulario del lote",
    [Formula: ForRecIni + ForRecCant - 1]
```

Por lo tanto **`ForRecFin` no es columna física** (confirmado por `INFORMATION_SCHEMA`: `FORMSUCU`
tiene 8 columnas, sin `ForRecFin`). El rango manual inclusivo es
`[ForRecIni, ForRecIni + ForRecCant - 1]`. La detección de un número manual válido es:

```text
ForRecIni <= F276Id <= ForRecIni + ForRecCant - 1
```

con `ForRecTipo = 'ACTS'` (acta de salida) o `'ACTI'` (acta de ingreso), acorde a
`F276Clasif` (`'S'` / `'E'`).

### 3.6 Esquema físico (G-DATA 2026-11-04) — tablas reutilizadas

**`PPRIM_TU`** — 12 columnas. PK: `SucNroId, SucTipo, TurNroId`. Sin `IDENTITY`, sin FK.

| Columna | Tipo SQL | Nul. | Notas |
|---|---|---|---|
| `SucNroId` | `smallint` | NO | PK |
| `SucTipo` | `char(3)` | NO | PK |
| `TurNroId` | `int` | NO | PK |
| `TurUsuario` | `char(10)` | NO | |
| `TurFchApe` | `datetime` | NO | |
| `TurFchCie` | `datetime` | NO | |
| `TurEstado` | `char(1)` | NO | `'A'` abierto / `'C'` cerrado |
| `TurHraApe` | `datetime` | NO | |
| `TurHraCie` | `datetime` | NO | |
| `TurCPCId` | `smallint` | SÍ | |
| `TurFchPago` | `datetime` | SÍ | |
| `SNSinNum` | `decimal(10,0)` | SÍ | |

**`FORMSUCU`** — 8 columnas (ver `09-formularios-acta` §3.10). `ForRecFin` **no** es columna; se
deriva.

**`GES_SUCN`** — 5 columnas (ver `09-formularios-acta` §3.10). **No** se usa en modo manual
(no se genera número).

---

## 4. Requisitos EARS

### 4.1 Flujo de carga manual

#### R-TURNO-001 — Enrutamiento de "Carga Manual"
> **EL SISTEMA DEBE** enrutar la opción de menú "Carga Manual" al listado de turnos abiertos
> (`/actas/carga-manual`), **NO** al formulario de alta directo, reproduciendo
> `DPDolphinMenuActas` (`Link = TurnosListadoAbiertos.Link()`).

* *Criterio de verificación*: test de vista del menú que verifica que el ítem "Carga Manual" apunta
  a `/actas/carga-manual`; revisión del `git diff` que confirma que no se reutiliza `/actas/nueva`
  para el ítem de menú.

#### R-TURNO-002 — Listado de turnos abiertos
> **CUANDO** el usuario acceda a `/actas/carga-manual`, **EL SISTEMA DEBE** mostrar un listado de
> los turnos abiertos (`TurEstado = 'A'`) de la sucursal y tipo de su contexto de sesión, con
> paginación de 10 filas por defecto y ordenado por `SucNroId, SucTipo, TurNroId`, reproduciendo
> `TurnosListadoAbiertos`.

* *Criterio de verificación*: `@WebMvcTest` del controller que comprueba `GET /actas/carga-manual` →
  200 + vista `puestos/turnos-abiertos` + atributos del view model; IT que compara el listado con
  un `SELECT` equivalente (`TurEstado='A' AND SucNroId=? AND SucTipo=?`).

#### R-TURNO-003 — Origen del `TurNroId` en carga manual
> **CUANDO** el usuario seleccione un turno del listado de turnos abiertos, **EL SISTEMA DEBE** tomar
> el `TurNroId` del turno seleccionado (parámetro de la navegación) y **NO** el del contexto de
> sesión, al crear el acta, reproduciendo `PPActaDetalle` en modo `'M'`
> (`&F2276.TurNroId = &TurNroId`).

* *Criterio de verificación*: test MockMvc que solicita `GET /actas/nueva?turNroId=42&manual=true` y
  verifica que el view model lleva `turNroId = 42` y que `ActaForm` se arma con ese turno; test
  unitario del servicio que verifica que `guardar(...)` persiste `TurNroId = 42` aunque el contexto
  tenga otro turno.

#### R-TURNO-004 — Turno seleccionado abierto
> **EL SISTEMA DEBE** validar que el turno seleccionado esté abierto (`TurEstado = 'A'`) antes de
> permitir la creación del acta; si no lo está, **DEBE** rechazar la creación.

* *Criterio de verificación*: test unitario del servicio con un turno `'C'` que verifica que no se
  inserta nada en `F2276`; test MockMvc con `turNroId` de un turno cerrado que verifica el rechazo.

#### R-TURNO-005 — Filtro por sucursal y tipo del contexto
> **EL SISTEMA DEBE** filtrar los turnos abiertos por `SucNroId = Contexto.Sucursal` y
> `SucTipo = Contexto.Tipo`, reproduciendo el `ControlWhere` de `TurnosListadoAbiertos`.

* *Criterio de verificación*: IT que verifica que un turno de otra sucursal o tipo no aparece en el
  listado; test MockMvc con contexto de sucursal `X` que verifica la ausencia de turnos de
  sucursal `Y`.

#### R-TURNO-006 — Estado vacío / sin turnos abiertos
> **SI** no hay turnos abiertos para la sucursal y tipo del contexto, **ENTONCES EL SISTEMA DEBE**
> renderizar el estado vacío (sin grilla) manteniendo el título `Seleccione un Turno`, y **NO DEBE**
> permitir seleccionar un turno.

* *Criterio de verificación*: test MockMvc con el repositorio devolviendo lista vacía que verifica
  la presencia del estado vacío y la ausencia de `<tbody>` con filas.

> Nota: el WebPanel legacy no emite un `msg()` para este caso (ver SA-TURNO-01).

#### R-TURNO-007 — Paridad con `09-formularios-acta`
> **EL SISTEMA DEBE** reutilizar el mismo formulario de alta, las mismas validaciones de negocio, la
> misma persistencia (cabecera + detalle + pase) y los mismos totales de `09-formularios-acta`;
> la única diferencia es el origen del `TurNroId` (R-TURNO-003) y la visibilidad/validación del
> `F276Id` (R-TURNO-008…R-TURNO-011).

* *Criterio de verificación*: revisión de código que confirma que `ActaService.guardar(...)` se
  reutiliza sin duplicar lógica; tests de regresión de `09-formularios-acta` en verde.

### 4.2 Formulario de alta en modo manual

#### R-TURNO-008 — `F276Id` editable en modo manual
> **CUANDO** el acta se crea en modo manual (`manual = true`), **EL SISTEMA DEBE** mostrar el campo
> `Nro. Acta` (`F276Id`) editable (no ocultarlo), reproduciendo `PPActaDetalle` en modo `'M'`
> (donde `ctlF276Id.Visible` **no** se pone en `false`).

* *Criterio de verificación*: test de vista con `manual = true` que verifica la presencia del control
  `F276Id` editable; test de vista con `manual = false` que verifica su ausencia.

#### R-TURNO-009 — Número manual obligatorio
> **SI** en modo manual el `F276Id` es `0`, **ENTONCES EL SISTEMA DEBE** rechazar el alta con el
> mensaje verbatim `Debe ingresar un Nro. de Formulario Generado mediante el Sistema` y **NO DEBE**
> persistir nada.

* *Criterio de verificación*: test MockMvc en modo manual con `F276Id = 0` que verifica el texto
  exacto y la ausencia de escritura.

#### R-TURNO-010 — Número manual válido
> **SI** en modo manual el `F276Id` no cae en un rango manual registrado en `FORMSUCU`
> (`ForRecIni <= F276Id <= ForRecIni + ForRecCant - 1`, con `ForRecTipo` acorde a la clasificación),
> **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> `El número de Acta no corresponde a un número generado manualmente` y **NO DEBE** persistir nada.

* *Criterio de verificación*: test MockMvc en modo manual con un `F276Id` fuera de rango que verifica
  el texto exacto y la ausencia de escritura; test con un `F276Id` dentro del rango que verifica
  que no se rechaza por este motivo.

#### R-TURNO-011 — Derivación de la clasificación en modo manual
> **CUANDO** el usuario ingrese un `F276Id` válido en modo manual, **EL SISTEMA DEBE** derivar
> `F276Clasif` del rango manual correspondiente (`ForRecTipo = 'ACTI'` ⇒ `'E'`,
> `ForRecTipo = 'ACTS'` ⇒ `'S'`), reproduciendo `ObtenerTipoActaManual`, y **NO DEBE** permitir una
> clasificación inconsistente con el rango.

* *Criterio de verificación*: test unitario del servicio con un rango `ACTI` que verifica
  `F276Clasif = 'E'`; test con un rango `ACTS` que verifica `F276Clasif = 'S'`.

#### R-TURNO-012 — `TurNroId` del turno seleccionado en la persistencia
> **CUANDO** se cree el acta en modo manual, **EL SISTEMA DEBE** persistir `TurNroId` con el valor
> del turno seleccionado (R-TURNO-003) en `F2276.TurNroId`, reproduciendo
> `Sub 'BeforeInsert'` (rama `else`).

* *Criterio de verificación*: IT que crea un acta en modo manual y verifica que `F2276.TurNroId`
  coincide con el turno seleccionado y **no** con el turno del contexto.

#### R-TURNO-013 — Sin numeración automática en modo manual
> **CUANDO** se cree el acta en modo manual, **EL SISTEMA DEBE** persistir el `F276Id` informado por
> el usuario y **NO DEBE** generar uno con `GES_SUCN` ni incrementar el contador.

* *Criterio de verificación*: IT que crea un acta en modo manual y verifica que `GES_SUCN.SucUltNum`
  no cambió y que `F2276.F276Id` es el número ingresado.

#### R-TURNO-014 — Reutilización de validaciones y persistencia
> **EL SISTEMA DEBE** reutilizar las validaciones de negocio (R-ACTA-026…R-ACTA-040), la
> persistencia (cabecera + detalle + pase, R-ACTA-045…R-ACTA-049) y los totales (R-ACTA-007) de
> `09-formularios-acta` en modo manual, sin duplicar código.

* *Criterio de verificación*: revisión de código que confirma que no hay una segunda implementación
  de `guardar(...)`; tests de regresión de `09-formularios-acta` en verde con `manual = true`.

#### R-TURNO-015 — Estado vacío del listado de turnos abiertos
> **SI** el listado de turnos abiertos está vacío, **ENTONCES EL SISTEMA DEBE** renderizar el estado
> vacío (sin grilla) y **NO DEBE** renderizar filas seleccionables.

* *Criterio de verificación*: test MockMvc con lista vacía que verifica la ausencia de `<tbody>` con
  filas y la presencia del estado vacío.

#### R-TURNO-016 — Turno cerrado rechazado
> **SI** el turno seleccionado no está abierto (`TurEstado <> 'A'`), **ENTONCES EL SISTEMA DEBE**
> rechazar la creación con el mensaje verbatim `No se puede generar la guia porque el turno
> asociado esta cerrado` y **NO DEBE** persistir nada.

* *Criterio de verificación*: test unitario con turno `'C'` que verifica el mensaje y la ausencia de
  escritura; IT que verifica que no se inserta nada.

---

## 5. Matriz de paridad con el legacy

| Operación legacy | Requisito | Implementación | Prueba |
|---|---|---|---|
| Menú "Carga Manual" → `TurnosListadoAbiertos` | R-TURNO-001 | `CargaManualController` / menú MVC | `MvcMenuCargaManual` |
| Grilla de turnos abiertos (`TurEstado='A'`, filtro sucursal/tipo) | R-TURNO-002, R-TURNO-005 | `TurnoRepositorio` (consulta abiertos) + vista | `TurnosAbiertosIT` |
| Selección de turno → `PPActaDetalle(TurNroId,'M')` | R-TURNO-003, R-TURNO-012 | `ActasController` modo manual | `MvcSeleccionTurno` |
| Turno cerrado rechazado | R-TURNO-004, R-TURNO-016 | `ActaService` + `PPrim_TurnoEstado` | `ActaServiceTest#turnoCerrado` |
| Estado vacío | R-TURNO-006, R-TURNO-015 | vista `turnos-abiertos` | `MvcTurnosAbiertosVacio` |
| `F276Id` editable + validación manual | R-TURNO-008…R-TURNO-011 | `ActaForm` + `ActaService` | `MvcFormularioManual`, `ActaServiceTest#manual` |
| Sin numeración automática | R-TURNO-013 | `ActaService.guardar` (rama manual) | `ActaManualIT` |
| Reutilización de `09` | R-TURNO-007, R-TURNO-014 | `ActaService` (sin duplicar) | suites de `09` en verde |

---

## 6. Supuestos abiertos

| ID | Descripción | Impacto | Bloquea a |
|---|---|---|---|
| **SA-TURNO-01** | El WebPanel `TurnosListadoAbiertos` no emite ningún `msg()` para el caso de lista vacía. Confirmar con el responsable si se quiere un mensaje verbatim específico o basta con el estado vacío estándar. | Bajo | R-TURNO-006 |
| **SA-TURNO-02** | La columna visible `att:207` del grid de `TurnosListadoAbiertos` se asume como `TurFchApe` (fecha de apertura). Confirmar la correspondencia exacta contra la KB (caption del atributo) en la fase de implementación. | Bajo | T-TURNO-051 (vista) |
| **SA-TURNO-03** | El listado de turnos abiertos del legacy no pagina (sólo `MaxRows=10`). Confirmar si el replatforming debe paginar (recomendado, paridad con `06-turnos`) o mostrar sólo los primeros 10. | Medio | T-TURNO-041 |
| **SA-TURNO-04** | Confirmar si el modo manual debe reutilizar la misma plantilla `acta-formulario.html` (con un flag `manual`) o tener una plantilla propia. Recomendación: reutilizar con flag (R-TURNO-007). | Bajo | T-TURNO-054 |
| **SA-TURNO-05** | Confirmar si la opción de menú "Carga Manual" debe convivir con "Formularios ACTAS" (alta web) o reemplazarla. El legacy las tiene como ítems separados. | Bajo | T-TURNO-060 |

---

## 7. Decisiones

| # | Decisión | Efecto |
|---|---|---|
| D-TURNO-01 | El flujo de "Carga Manual" **exige** seleccionar un turno abierto antes de crear el acta (SÍ verificado). | Se agrega la pantalla `/actas/carga-manual`. |
| D-TURNO-02 | Se reutiliza el mismo formulario de alta de `09-formularios-acta` con un flag `manual` (no se duplica la plantilla ni el servicio). | Un solo `ActaForm` con campo opcional `manual` y `turNroId`. |
| D-TURNO-03 | En modo manual, `F276Id` es editable y se valida contra `FORMSUCU` (rango inclusivo `[ForRecIni, ForRecIni + ForRecCant - 1]`); `ForRecFin` se deriva (fórmula). | `ActaService` agrega `validarNumeroManual(...)` y `obtenerTipoActaManual(...)`. |
| D-TURNO-04 | En modo manual **no** se genera número con `GES_SUCN` ni se incrementa el contador. | `ActaService.guardar(...)` omite `ServicioNumeracionActa` cuando `manual = true`. |
| D-TURNO-05 | El `TurNroId` persistido en modo manual proviene del turno seleccionado, no del contexto. | `BeforeInsert` rama `else` (paridad con el legacy). |
| D-TURNO-06 | No se agregan endpoints REST nuevos (D-ACTA-04 se mantiene). | Postman sin cambios; G-POSTMAN sólo verifica que no hay diff. |
| D-TURNO-07 | Se reutiliza `TurnoRepositorio` para listar turnos abiertos; si no existe un método que filtre por `TurEstado = 'A'` sin filtro de comisión, se agrega `listarAbiertosPorSucursal(...)` (lectura, sin DDL). | Cambio acotado al repositorio del núcleo (sólo lectura). |

---

## 8. Alcance excluido

- Impresión del acta y reportes PDF.
- WS `WSActa` (F506 / multa).
- Cierre de turno y lotes (lo define `06-turnos`).
- Endpoints REST nuevos.
- E2E automatizado.