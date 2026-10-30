# Spec — 03 Login de Puestos Camineros

> **Estado**: `APROBADA (revisión v2)` — puerta **G-SPEC** superada. Las **3 decisiones de seguridad**
> (**D-LOG-17** política CSRF y autenticación de `/api/**`, **D-LOG-18** materialización anticipada
> del token CSRF, **D-LOG-19** nivel de autenticación de `/api/v1/**`) fueron **aprobadas el
> 2026-10-22 por el responsable del proyecto** (alternativa **(a)** en las tres) ⇒ **no queda nada
> pendiente de aprobación humana** y el módulo está listo para implementación.
> Verificado: **46** requisitos EARS `R-LOG-001…046` (sin huecos), todos con verbo normativo y
> criterio de verificación; **64** tareas atómicas en `tasks.md`; **21** decisiones `D-LOG-xx` (2 de
> ellas **sustituidas** por las nuevas) y **18** supuestos `SA-LOG-xx` en `plan.md` (**SA-LOG-08**,
> **SA-LOG-09**, **SA-LOG-16** y **SA-LOG-17** **cerrados**; **SA-LOG-14**, **SA-LOG-15** y
> **SA-LOG-18** **abiertos y asumidos**, cada uno con su tarea); 8 tablas legacy mapeadas con
> `ddl-auto=none`, sin DDL ni migraciones.
> **Qué cambió en la revisión v2 (2026-10-22)**:
> 1. **SA-LOG-08 cerrado** con evidencia de `INFORMATION_SCHEMA` de `Rentas`: la tabla raíz de
>    comisiones es **`COMISION`** (no `COMISIO`), `COMISIO1` **existe** con **3** columnas, y la
>    PK real de `PPRIM_TU` es (`SucNroId`,`SucTipo`,`TurNroId`). Nueva §3.1.1 con la evidencia.
> 2. **Corrección de la nota de §3.9**: `CPCPerNyA` y `CPCResNyA` **NO son columnas físicas**
>    (atributos calculados del legacy); `PerNomyApe` **sí** lo es. ⇒ el nombre del integrante y
>    del responsable se resuelven por **JOIN a `PERSONAS`** (nuevos **R-LOG-035**, **R-LOG-036**).
> 3. **14 requisitos nuevos** `R-LOG-033…046`: 6 de paridad de BD verificada y 8 de los hallazgos
>    del QA del módulo 02 (`docs/verificacion/02-abm-productos.md` §4, H3–H9).
> 4. **5 decisiones nuevas** `D-LOG-17…021` y **4 supuestos nuevos** `SA-LOG-15…018`.
> 5. **17 tareas nuevas** `T-007`, `T-008`, `T-126…T-129`, `T-132`, `T-133`, `T-135`, `T-136`,
>    `T-311…T-314`, `T-404…T-406`.
> 6. **Aprobación de las 3 decisiones de seguridad** (2026-10-22, responsable del proyecto):
>    **D-LOG-17(a)** `/api/**` *stateless* y **sin CSRF** (CSRF sólo en las rutas MVC con sesión) ·
>    **D-LOG-18(a)** materialización anticipada del token conservando
>    `HttpSessionCsrfTokenRepository` · **D-LOG-19(a)** `permitAll` en `/api/v1/**` con la deuda
>    **SA-LOG-14** asumida y la tarea **T-135** de seguimiento. ⇒ **D-LOG-11** y **D-LOG-12**
>    quedan **sustituidas** (se conservan como histórico) y los requisitos **R-LOG-028**,
>    **R-LOG-031**, **R-LOG-039**, **R-LOG-041**, **R-LOG-042** y **R-LOG-043** quedan redactados
>    contra la política aprobada.
> **Nota de estilo**: los encabezados de requisito usan raya larga (U+2014), no guion ASCII —
> las búsquedas por `R-LOG-xxx -` con guion simple no los encuentran.
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación** y
> **trazabilidad** al objeto GeneXus que lo origina.
> **IDs**: prefijo `R-LOG-<NN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13.186702), alias `atppuestos` — **solo lectura**
> (G1). Objetos leídos: `Login` (WebComponent), `Seg_ControlAcceso`, `Seg_ControlAccesoWS`,
> `Seg_Encripta`, `Seg_BusPer`, `Seg_BusComi`, `Seg_BusRece`, `Seg_BusResponsable`, `PPrim_BusTur`,
> `Seg_BusPuesto` (procedimiento **sin callers**: variante huérfana de `Seg_BusComi`, no usada por
> el login), `SetContext`, `LoadContext`, `ListPrograms`, `IsAuthorized`, `NotAuthorized`
> (WebPanel), `Home` (WebPanel), `Inicio` (WebPanel), SDT `Context`, SDT `SDTEntradaLoginWS`, SDT
> `SDTSalidaLoginWS`, transacciones `SCRTY`, `PERSONAS`, `PUEPPRIM`, `RECEPTOR`, `Comisiones`
> (transacción `Root Module/Comisiones/Comisiones`), tabla `Comisio1`, `PPRIM_TU`.
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` (G-DATA 2026-10-22, **sólo `SELECT`**;
> **re-ejecutada el 2026-10-22** en la revisión v2 ⇒ §3.1.1).
> **Alcance de escritura**: `openspec/features/**` (esta spec) y, en fase de implementación,
> `atp-puestos-camineros/**` y `atp-nucleo-compartido/**`. **Sin DDL, sin migraciones** (G5).

---

## 1. Propósito

Especificar el **login completo de Puestos Camineros**: la verificación de credenciales contra
`SCRTY`, la resolución del contexto operativo (persona → comisión/receptoría → puesto → turno), el
código de resultado 0-6 y su mensaje **verbatim**, la persistencia del tipo de acceso, la
autorización por programa y los dos canales de entrada (formulario Thymeleaf y API REST
stateless).

El módulo **no** reimplementa la seguridad: **replica** el mecanismo observable del legacy,
incluidas sus decisiones de diseño (cifrado reversible, no cifrado real) y sus límites.

---

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Verificación de credenciales contra `SCRTY` con el algoritmo `Seg_Encripta` **reproducido tal cual**. |
| 2 | Matriz de **7 códigos de resultado** (0 a 6) con los mensajes **verbatim** del legacy. |
| 3 | Contexto de sesión con los **13 campos** del SDT `Context`. |
| 4 | Cookie `SUCURSAL` (tipo de acceso) persistida entre sesiones. |
| 5 | Chequeo de IP del cliente contra el prefijo de red del puesto (`PUEPPRIM.PueIPRed`). |
| 6 | Autorización por programa (`ListPrograms` + `IsAuthorized`) y página `NotAuthorized`. |
| 7 | Formulario de login Thymeleaf (`/login`), página `Home` (`/home`) y `NotAuthorized` (`/not-authorized`). |
| 8 | Canal REST stateless `POST /api/v1/auth/login` con la forma de `SDTSalidaLoginWS`. |
| 9 | Protección de rutas (Spring Security, sesión HTTP + CSRF), **política CSRF explícita por ruta**, materialización anticipada del token CSRF y **ejecutabilidad de la colección Postman** (revisión v2, hallazgos H3–H9 del QA del módulo 02). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Alta / modificación de usuarios (`SCRTY`) | El legacy no expone un ABM de usuarios en esta KB; la clave se gestiona en el *Sistema Integrado (win)* (mensaje del código 5). | Fuera del módulo |
| Gestión de `UsuHash` (PBKDF2) | Sólo 26 de 552 usuarios lo tienen y **ningún objeto de la KB lo consulta** para autenticar. El legacy no lo usa en el login. | Fuera del módulo — ver **SA-L04** |
| Turnos (`PPrim_Turnos`), comisiones, Catálogos | Módulos de negocio posteriores; el login sólo **resuelve** turno y comisión. | Módulos futuros |
| Portal Contribuyente / Portal Gestión ATP | Fuera del monorepo (`00-alcance` §14). | Fuera del monorepo |
| Módulos compartidos (`Planes de Pago`) | Fuera del monorepo. | Fuera del monorepo |
| Recuperación / restablecimiento de contraseña | No existe en el legacy. | Fuera del módulo |
| Autenticación multifactor, SSO, tokens OAuth | No existen en el legacy. | Fuera del módulo |

---

## 3. Modelo legacy verificado

### 3.1 Inventario de objetos y su aporte

| Objeto GeneXus | Tipo | Qué aporta |
|---|---|---|
| `Login` | WebComponent | Formulario: `Usuario` (var:6, rótulo *Usuario*), `UsuClave` (var:7, rótulo *Contraseña*, `IsPassword=True`), `Tipo` (var:5, rótulo *Acceso*), `TextBlockPass` (`gxTextBlock`, `ForeColor="Red"`, caption inicial vacía), botón `BtnIngresar` (evento `Enter`, rótulo *Ingresar*). `Event Start` lee la cookie `SUCURSAL`; `Event Enter` llama a `Seg_ControlAcceso` y mapea el código a `TextBlockPass.Caption`. **Verificado el 2026-10-22 sobre el `WebForm`**: `gxAttribute` var:5/6/7 con `invitemsg` `Acceso` / `Usuario` / `Contraseña`, `IsPassword="True"` en var:7, `gxButton ControlName="BtnIngresar" Event="Enter"` con caption constante `Ingresar`, y `gxTextBlock id="TextBlockPass" ForeColor="Red"` con `CaptionExpression` vacío. |
| `Seg_ControlAcceso` | Procedure | Algoritmo completo del acceso con sesión. `parm(in:&Usuario, in:&Pass, in:&Tipo, out:&error)`. |
| `Seg_Encripta` | Procedure | Codificación del usuario y de la clave. `parm(in:&Desencrip, out:&Encrip)`. |
| `Seg_ControlAccesoWS` | Procedure | Variante JSON stateless: sin contexto, sin chequeo de IP. |
| `Seg_BusPer` | Procedure | Resuelve la Persona desde `PERSONAS.PerCodUsu`; devuelve `-1` si no hay match. |
| `Seg_BusComi` | Procedure | Comisión activa como integrante (nivel `COMISIO1`) y como responsable. |
| `Seg_BusResponsable` | Procedure | `CPCResId` / `CPCResNyA` de la comisión del puesto. |
| `Seg_BusRece` | Procedure | Receptoría del persona (nivel `RECEPTO1`). |
| `PPrim_BusTur` | Procedure | Turno activo del puesto/receptoría (`PPRIM_TU`). |
| `SetContext` / `LoadContext` | Procedure | `&Session.Set(!"context", &Context.ToXml())` / `&Context.FromXml(&Session.Get(!"context"))`. |
| `Context` | SDT | 13 campos del contexto de sesión. |
| `ListPrograms` / `IsAuthorized` | Procedure | Tabla de autorización por programa. |
| `NotAuthorized` | WebPanel | `Msg('You are not authorized to do this action')`. |
| `Home` | WebPanel | `Event Start`: `LoadContext`; si `Context.Usuario` está vacío muestra el `Login`, si no llama a `Inicio`. |
| `Inicio` | WebPanel | `Event Start` asigna a `&Texto` el texto de bienvenida; lo renderiza en `TextBlock1` (`GxFormat="HTML"`). |
| `SDTEntradaLoginWS` | SDT | `{Usuario(10), Pass(10), Tipo(3)}`. |
| `SDTSalidaLoginWS` | SDT | `{error(NUMERIC 1), sucursal(NUMERIC 4), comision(NUMERIC 6)}`. |

### 3.1.1 Nombres físicos verificados — G-DATA 2026-10-22 (cierra **SA-LOG-08**)

> **Método**: `SELECT` sobre `INFORMATION_SCHEMA.TABLES` / `COLUMNS` / `KEY_COLUMN_USAGE` /
> `TABLE_CONSTRAINTS` de `Rentas` @ `devmssqlatp.ecom.com.ar` (usuario `rentas`, **esquema por
> defecto `Rentas`**), SQL Server 2019 (15.0.2170.1). **Sólo lectura** (G-DATA). Sin DDL.
> Evidencia extendida (columnas, tipos, nulabilidad, PK) en `plan.md` §5.3.

**Veredicto de SA-LOG-08 — la divergencia `COMISIO` vs `COMISION` queda resuelta: es `COMISION`.**

| Objeto KB | Tabla física | Esquema | PK física (verificada) | Columnas | Filas |
|---|---|---|---|---:|---:|
| `SCRTY` | `SCRTY` | `Rentas` | `UsuId` | 16 | 552 |
| `PERSONAS` | `PERSONAS` | `Rentas` | `PerId` | 4 | 183 |
| `PUEPPRIM` | `PUEPPRIM` | `Rentas` | `PueCod` | 3 | 14 |
| `RECEPTOR` | `RECEPTOR` | `Rentas` | `ReceId` | 4 | 24 |
| `RECEPTO1` | `RECEPTO1` | `Rentas` | (`ReceId`, `PerId`) | 3 | 69 |
| `Comisiones` (transacción) | **`COMISION`** | `Rentas` | (`PueCod`, `CPCId`) | 14 | 3 120 |
| `Comisio1` (tabla) | **`COMISIO1`** | `Rentas` | (`PueCod`, `CPCId`, `CPCPerId`) | 3 | 7 238 |
| `PPRIM_TU` | `PPRIM_TU` | `Rentas` | (`SucNroId`, `SucTipo`, `TurNroId`) | 12 | 54 768 |

Puntos que la revisión v2 deja **fijos**:

1. La tabla raíz de comisiones es **`COMISION`** (8 caracteres **con** la `N`). El truncado a 8
   caracteres de GeneXus **no** aplica al nombre `Comision` ⇒ la hipótesis de `plan.md` §5.3
   (`COMISIO`) queda **descartada** y la de `docs/analisis-ATPPuestos.md` §15.1 queda
   **confirmada**.
2. **`COMISIO1` existe** y tiene **3** columnas: `PueCod`, `CPCId`, `CPCPerId`.
3. `CPCFchDes` (`datetime`, nullable) y `CPCFchHas` (`datetime`, nullable) **sí existen**, al igual
   que `CPCHorDes` / `CPCHorHas` (**`int`**, nullable: la hora es un **número**, no una fecha-hora).
4. La PK real de `PPRIM_TU` es la de la KB: (`SucNroId`, `SucTipo`, `TurNroId`) — **no** la
   (`SucNroId`, `TurNroId`) de `docs/analisis-ATPPuestos.md` §15.1.
5. **No hay ninguna clave foránea física** entre estas 8 tablas (`KEY_COLUMN_USAGE` con
   `FOREIGN KEY` devuelve 0 filas): las relaciones son **lógicas** y las reproduce el repositorio.
6. Existe además `dbo.AU_COMISION` (17 columnas: `id_evento`, `t_evento`, `fecha`, `usuario`,
   `terminal`, `aplicacion` + una instantánea de las columnas de `COMISION`). Es una **tabla de
   auditoría de eventos de otra aplicación**, **no** la tabla de comisiones del legacy: vive en el
   esquema `dbo` y **no** se mapea.
7. El esquema por defecto del usuario de aplicación es `Rentas` ⇒ `@Table(name = "COMISION")`
   resuelve a `Rentas.COMISION` sin ambigüedad (mismo mecanismo que ya usan `PRODUCTO`/`PRODUCT1`
   en el módulo 02).

### 3.2 `Seg_Encripta` — traducción exacta

**Fuente**: `genexus_read({name:"Seg_Encripta"})` → `Source` (leído en modo lectura).

Algoritmo (`Desencrip` → `Encrip`):

1. `&DesencripMay = &Desencrip.ToUpper()` — **normalización a mayúsculas**.
2. Se carga el **alfabeto de 66 caracteres** `&Ch(1..66)`:

```text
índice:  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20
ch:      ' ' '!' '"' '#' '$' '%' '&' 'a' '(' ')' '*' '+' ',' '-' '.' '/' '0' '1' '2'
índice: 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40
ch:      '4' '5' '6' '7' '8' '9' ':' ';' '<' '=' '>' '?' '@' 'A' 'B' 'C' 'D' 'E' 'F'
índice: 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60
ch:      'G' 'H' 'I' 'J' 'K' 'L' 'M' 'N' 'O' 'P' 'Q' 'R' 'S' 'T' 'U' 'V' 'W' 'X'
índice: 61 62 63 64 65 66
ch:      '\' ']' '^' '_' '`' 'b'
```

   Observaciones verificables:
   - El alfabeto es **ASCII 32-96** (`' '`..`` ` ``) con **dos sustituciones**: `'` (ASCII 39)
     queda fuera y en su lugar aparece `'a'` (índice 8); y `'b'` (ASCII 98) ocupa el índice 66.
   - El índice 8 (`'a'`) y el índice 66 (`'b'`) son los únicos caracteres en minúscula y, por
     tanto, **inalcanzables como entrada** (la entrada se normaliza a mayúsculas).

3. Doble bucle: `&Co` de **1 a 10** (`Do While &Co < 11`), y dentro `&Cont` de **1 a 65**
   (`Do While &Cont < 66`).
   - Se toma `&Letra = substr(&DesencripMay, &Co, 1)` (posición `&Co` del texto normalizado).
   - Si `&Letra = &Ch(&Cont)`, entonces `&Cual = 67 - &Cont` y se **agrega** `&Ch(&Cual)` al resultado.
   - El índice 66 (`'b'`) **nunca se compara** (el bucle interno termina en 65) ⇒ `'b'` sólo puede
     aparecer en la **salida**.
4. Resultado: un `CHARACTER(10)` con la imagen especular.

**Regla espejo**: `destino = 67 − origen`. Es una **sustitución monoalfabética** sobre 65
caracteres alcanzables, y por lo tanto **reversible**: la misma función, aplicada sin el
`ToUpper()`, deshace el cifrado (`origen = 67 − destino`).

**Vectores de verificación** (calculados con la traducción anterior y validados contra la BD real,
§3.2.1):

| Entrada | Salida `Seg_Encripta` | Longitud |
|---|---|---:|
| `ABC123` | `@?>PON` | 6 |
| `1234567890` | `PONMLKJIHQ` | 10 |
| `YOLOFFG` | `(252;;:` | 7 |
| `YOLOFFG␠␠␠` | `(252;;:bbb` | 10 |
| `SCARRASCO␠` | `.>@//@.>2b` | 10 |
| `Z` | `a` | 1 |
| `␠` (espacio) | `b` | 1 |

**Corolario crítico (caracteres fuera del alfabeto)**: si un carácter de la entrada **no pertenece**
al alfabeto (por ejemplo `Ñ`, `Á`, `É`), **no hay coincidencia** y ese carácter **se descarta**
(sin emitir nada), de modo que la salida queda **más corta** y las posiciones siguientes se
desplazan. Esto está confirmado en la BD: 3 de los 552 `SCRTY.UsuId` decodifican a 9 caracteres
en lugar de 10 (`PASICHG␠␠`, `GMAANEZ␠␠`, `GNUEZ␠␠␠␠`) porque el `Ñ` original se perdió.

**No es cifrado**: la transformación es un **cifrado por sustitución monoalfabética reversible**,
equivalente a texto plano con una transformación fija y pública. La constante de la KB es pública.
Esta decisión se documenta en `plan.md` §2 (**D-LOG-03**) y en `plan.md` §10 (R-1).

#### 3.2.1 Contraste con la BD real (`Rentas`, 2026-10-22)

| Métrica | Valor | Lectura |
|---|---:|---|
| Filas en `SCRTY` | 552 | — |
| `UsuId` con `DATALENGTH = 10` | 552 | Todos cifrados de 10 posiciones. |
| `UsuId` que decodifican a 10 caracteres | 549 | Padding con espacios a 10. |
| `UsuId` que decodifican a 9 caracteres | 3 | Un carácter `Ñ` descartado por el codificador. |
| `UsuId` que contienen `b` minúscula | 504 | El cifrado produce `b` (imagen del espacio). |
| `UsuId` que contienen `a` minúscula | 61 | El cifrado produce `a` (imagen de `Z`). |
| `UsuClave` con `DATALENGTH = 10` | 552 | — |
| `UsuClave` que **no** terminan en `b` | 552 | Ninguna clave tiene espacios finales ⇒ **las 550 claves no vacías son de exactamente 10 caracteres**. |
| `UsuClave` vacías (`RTRIM = ''`) | 2 | Cuentas con `UsuHash` y `UsuIngreso = 1`. |
| `PERSONAS` con `PerCodUsu` no nulo | 183 (todas con `DATALENGTH = 10`) | `PerCodUsu` = **usuario en claro, en mayúsculas, con padding**. |
| `PERSONAS.PerCodUsu` que coinciden con `dec(UsuId).rstrip()` | 130 de 147 no vacías | Coincidencia del modelo; las 17 restantes son desvíos de dato del legacy (p. ej. `Ñ` → `N`). |
| Collation de la BD | `Latin1_General_CI_AS` | **Insensible a mayúsculas** ⇒ la comparación `UsuId.ToUpper() = &UsuarioEncrip` del legacy funciona aun cuando el cifrado emite `a`/`b` minúsculas. |

### 3.3 Orden de las comprobaciones de `Seg_ControlAcceso`

Transcripción del `Source` de `Seg_ControlAcceso` (leído en modo lectura), en orden de ejecución:

```text
&error = 2                                             // (0) valor inicial
&UsuarioEncrip = Udp(Seg_Encripta, &Usuario)
&SucDescr = ''
&SucNroId = 0
For Each                                                // (1) SCRTY
    where UsuId.ToUpper() = &UsuarioEncrip
    &UsuClave = UsuClave
    if not &UsuClave.IsEmpty()                         // (2) clave presente
        &PassEncriptada = Udp(Seg_Encripta, &Pass)
        if &PassEncriptada = &UsuClave                 // (3) clave coincide
            if UsuIngreso = 1                          // (4) debe cambiar su clave
                &error = 5
            else
                &error = 0
                &PerId = Seg_BusPer.Udp(&Usuario)      // (5) persona
                if &PerId >= 0
                    if &Tipo = 'PUE'                   // (6) Puesto Caminero
                        Seg_BusComi.call(&PerId, &CPCID, &SucNroId)
                        if &CPCID.IsEmpty()            // (7) sin comisión activa
                            &error = 4
                        else
                            Seg_BusResponsable.Call(&SucNroId, &CPCID, &CPCResId, &CPCResNyA)
                            Do 'BuscarNomPuesto'       // (8) descripción + prefijo de red
                            &ipcliente = &HttpRequest.GetHeader('X-Forwarded-For')
                            if &ipcliente.IsEmpty()
                                &ipcliente = RemoteAddr()
                            endif
                            if &ipcliente.IndexOf(&PueIPRed) = 0   // (9) IP no habilitada
                                &Error = 6
                                exit
                            endif
                        endif
                    else                                // Receptoria
                        Seg_BusRece.Call(&PerId, &SucNroId, &SucDescr)
                        &CPCID = 0
                    endif
                    if &SucNroId >= 0                  // (10) sucursal resuelta
                        &TurNroId = Udp(PPrim_BusTur, &SucNroId, &Tipo, &CPCID)
                        // (11) carga del contexto (10 campos) + SetContext
                    else
                        Do 'BuscarHistoriaComision'    // (12) fue responsable alguna vez
                        if &FueResponsable = 'S' and &Tipo = 'PUE'
                            // contexto parcial (4 campos) + SetContext
                            &error = 0
                        else
                            &error = 4
                        endif
                    endif
                else
                    &error = 3                        // sin persona
                endif
            endif
        else
            &error = 2                                // clave incorrecta
        endif
    else
        &error = 2                                    // clave vacía
    endif
When None
    &error = 2                                        // usuario inexistente
endfor
```

**Consecuencias observables** (a preservar):

- El **error 1 no es producible** por `Seg_ControlAcceso`: el procedimiento sólo asigna 0, 2, 3,
  4, 5 y 6. El `Case &error = 1` del `Login` es código muerto. El mensaje es el mismo que el del
  código 2.
- El **error 0 se asigna antes** de resolver persona/comisión: si después falla la resolución,
  el error queda en 4 (o 3), nunca vuelve a 0 — salvo en la rama "fue responsable", que vuelve a
  asignar 0 explícitamente.
- El chequeo de **IP sólo ocurre en la rama `&Tipo = 'PUE'`** y **después** de resolver la comisión;
  en la rama `REC` no hay chequeo de IP.
- La rama "&SucNroId < 0" (puesto/receptoría no resuelto) **vuelve a `error = 0`** cuando la persona
  fue responsable de alguna comisión cerrada y el tipo es `PUE`, y en ese caso carga un contexto
  **parcial** (4 campos).
- `Seg_BusComi` inicializa `&PueCod = -1`; si no hay comisión activa devuelve `&CPCId = 0` (que en
  GeneXus es "vacío" para un numérico) y el control de la línea (7) produce el **error 4**.

### 3.4 Matriz de códigos de resultado y mensajes verbatim

Los textos se transcriben **carácter a carácter** del `Login.Event Enter` (incluidas sus erratas:
"esta" sin tilde, "Receptoria" sin tilde). Son **contrato observable** (ISO/IEC 25030 §6).

| Código | Significado | Mensaje verbatim del legacy | Origen |
|---:|---|---|---|
| 0 | Acceso concedido | *(sin mensaje)* | `Seg_ControlAcceso` |
| 1 | Credenciales inválidas | `Usuario/Contraseña Incorrecto` | `Login` `Case &error = 1` *(inalcanzable)* |
| 2 | Credenciales inválidas | `Usuario/Contraseña Incorrecto` | `Seg_ControlAcceso` (`When None`, clave vacía, clave distinta) |
| 3 | Usuario no relacionado con una Persona | `Este Usuario no esta relacionado con una Persona del sistema` | `Seg_ControlAcceso` |
| 4 | Sin Receptoria o puesto caminero asignado | `No existe Receptoria o puesto caminero asignado` | `Seg_ControlAcceso` |
| 5 | Debe cambiar su clave (`UsuIngreso = 1`) | `Debe cambiar su clave desde el Sistema Integrado (win)` | `Seg_ControlAcceso` |
| 6 | IP no habilitada para el puesto | `Dispositivo no habilitado para el uso en el puesto caminero, contacte con el administrador` | `Seg_ControlAcceso` |

### 3.5 Contexto de sesión — SDT `Context` (13 campos)

Estructura leída con `genexus_inspect({name:"Context"})` → `sdtStructure` (`itemCount: 13`).

| # | Campo | Tipo GeneXus | Longitud | ¿Lo carga el login? | Origen del valor |
|---:|---|---|---:|---|---|
| 1 | `Usuario` | CHARACTER | 10 | Sí | `&Usuario.ToUpper()` |
| 2 | `UsuarioNombre` | CHARACTER (dominio `Nombres`) | 50 | Sí | `SCRTY.UsuNomYApe` |
| 3 | `PersonaId` | NUMERIC | 5 | Sí | `Seg_BusPer` → `PERSONAS.PerId` |
| 4 | `Comision` | NUMERIC | 6 | Sí | `Seg_BusComi` → `COMISION.CPCId` |
| 5 | `Turno` | NUMERIC (dominio `NumCodOch`) | 8 | Sí | `PPrim_BusTur` → `PPRIM_TU.TurNroId` |
| 6 | `Sucursal` | NUMERIC (dominio `CodNumCor`) | 2 | Sí | `PUEPPRIM.PueCod` o `RECEPTOR.ReceId` |
| 7 | `SucursalNombre` | CHARACTER | 30 | Sí | `PUEPPRIM.PueDesc` / `RECEPTOR.ReceDsc` / `'Sin Puesto Asignado'` |
| 8 | `Tipo` | CHARACTER | 3 | Sí | `&Tipo` (`'PUE'` / `'REC'`) |
| 9 | `Responsable` | NUMERIC | 5 | Sí | `COMISION.CPCResId` |
| 10 | `ResponsableNombre` | VARCHAR (dominio `Nombre`) | 40 | Sí | `PERSONAS.PerNomyApe` del `COMISION.CPCResId` (`CPCResNyA` es **calculado**, no columna) |
| 11 | `Form2276` | GX_SDT (`SdtF2276`) | — | **No** | Lo consume el módulo de formularios 2276. |
| 12 | `F276Id` | NUMERIC | 9 | **No** | Lo consume el módulo de formularios 2276. |
| 13 | `UltimoMenu` | CHARACTER | 30 | **No** | Lo consume la navegación de menús. |

**Persistencia**: `SetContext` serializa el SDT a XML y lo guarda en la sesión
(`&Session.Set(!"context", &Context.ToXml())`); `LoadContext` lo recupera
(`&Context.FromXml(&Session.Get(!"context"))`). El login carga **10 de los 13** campos; los 3
restantes quedan en su valor por defecto.

### 3.6 Cookie `SUCURSAL` (tipo de acceso)

- `Login.Event Start`: `&TipoIni = GetCookie('SUCURSAL')`; si no está vacía, `&Tipo = &TipoIni`.
  ⇒ el **tipo de acceso elegido en el login anterior se recuerda** en el formulario siguiente.
- `Login.Event Enter`, sólo en el camino de éxito (`&error = 0`): `&error = SetCookie('SUCURSAL', &Tipo)`.
  ⇒ la cookie **se escribe después de un acceso concedido**, nunca en el camino de error.
- El valor es el `Tipo` (`'PUE'` o `'REC'`), con longitud 3.

### 3.7 Autorización por programa

- `ListPrograms(parm(out:&ProgramNames))` arma la lista de programas accesibles: por ahora
  `WWPPrim_Turnos` (*Turnos de Comisiones*) y `WWTDetProdPri` (*Buscar Guias*), cada uno con
  `Name`, `Description` y `Link`.
- Por cada programa consulta `IsAuthorized(&name)` y **sólo lo agrega si el resultado es verdadero**.
- **`IsAuthorized` está en desuso en la KB actual**: su fuente es
  `&Authorized = True` seguido de un bloque **comentado** que habría denegado `ViewInvoices` a los
  usuarios de nivel 0. Es decir: **la función devuelve siempre `True`** y, en consecuencia,
  `ListPrograms` devuelve **siempre la lista completa**.
- `genexus_inspect` no reporta **ningún caller** de `ListPrograms` en la KB.
- `NotAuthorized` (WebPanel, `parm(in:&GxObject)`) muestra
  `Msg('You are not authorized to do this action')` en su `Event Start`.

### 3.8 Variante WS — `Seg_ControlAccesoWS`

Mismo algoritmo que `Seg_ControlAcceso` **con tres diferencias**, todas verificables en su fuente:

1. **No hay contexto de sesión**: todo el bloque que arma `&Context` y la llamada a `SetContext`
   está comentado.
2. **No hay chequeo de IP**: el bloque `X-Forwarded-For` / `RemoteAddr` / `IndexOf(&PueIPRed)`
   está comentado.
3. **No hay rama de "historia de comisión"**: el bloque `Do 'BuscarHistoriaComision'` está comentado,
   por lo que si `&SucNroId < 0` el `error` **queda en 0** y la respuesta es un "éxito" con
   `sucursal` negativa.

La salida es `SDTSalidaLoginWS` serializado a JSON con la cabecera `content-type: application/json`:

```json
{ "error": 0, "sucursal": 10, "comision": 123 }
```

`sucursal` y `comision` **sólo se asignan** en la rama `&SucNroId >= 0`; en los demás casos valen `0`
(valor por defecto del SDT).

### 3.9 Resoluciones de apoyo (verificadas)

| Procedimiento | Consulta lógica (paridad) | Orden / desempate |
|---|---|---|
| `Seg_BusPer` | `PERSONAS` donde `PerCodUsu = :usuario` | `For Each Order PerCodUsu ... defined by PerNomyApe` ⇒ gana la fila de **mayor `PerNomyApe`** entre las que coinciden. Sin coincidencia ⇒ `-1`. |
| `Seg_BusComi` | `COMISION` + `COMISIO1` **JOIN `PERSONAS`** (`COMISIO1.CPCPerId = PERSONAS.PerId`) donde `CPCPerId = :perId`, `CPCFchDes <= :hoy`, `CPCFchHas >= :hoy`, `CPCCierre = 'N'` | `defined by CPCPerNyA` ⇒ gana la fila de **mayor nombre y apellido del integrante**; como `CPCPerNyA` es **calculado** (no columna), el orden se reproduce por `PERSONAS.PerNomyApe` (R-LOG-036). Sin coincidencia ⇒ `CPCId = 0` y se busca como **responsable** (`COMISION.CPCResId = :perId`, mismas condiciones de fecha y cierre). Si `CPCFchHas = :hoy` y `CPCHorHas < :horaActual`, la comisión se descarta (`CPCId = 0`). |
| `Seg_BusResponsable` | `COMISION` donde `PueCod = :pueCod` y `CPCId = :cpcId` — **JOIN `PERSONAS`** (`COMISION.CPCResId = PERSONAS.PerId`) para el nombre | `defined by CPCCierre` ⇒ gana la fila de **mayor `CPCCierre`**. `CPCResId` **sí** es columna física (`int`, nullable); `CPCResNyA` es **calculado** ⇒ su valor se obtiene de `PERSONAS.PerNomyApe` (R-LOG-035). Valores iniciales: `CPCResId = 0`, `CPCResNyA = 'Sin Comisión'`. |
| `Seg_BusRece` | `RECEPTO1` donde `PerId = :perId` — **JOIN `RECEPTOR`** (`RECEPTO1.ReceId = RECEPTOR.ReceId`) para `ReceDsc` | `For Each Order PerId` (el `defined by RecUsuFchD` está comentado) ⇒ **la primera** fila por `PerId`. `ReceDsc` **no** está en `RECEPTO1` (3 columnas: `ReceId`, `PerId`, `RecUsuFchD`): es un atributo **relacionado** de `RECEPTOR.ReceDsc` (`char(30)`). Sin coincidencia ⇒ `ReceId = -1`, `ReceDsc = 'No Existe Receptoria'`. |
| `PPrim_BusTur` | `PPRIM_TU` donde `SucNroId = :sucNroId`, `SucTipo = :tipo`, `TurEstado = 'A'`, y (`TurCPCId = :cpcId` **o** `:tipo = 'REC'`) | Sin `Order`/`defined by` ⇒ **la primera** fila que cumpla. Sin coincidencia ⇒ `TurNroId = 0`. |

> **Corrección (revisión v2, 2026-10-22 — G-DATA contra `Rentas`)**: la nota anterior de esta
> sección era **incorrecta** en su primera mitad. Contra la BD real:
> `INFORMATION_SCHEMA.COLUMNS` devuelve **`CPCPerNyA` = 0 coincidencias** en **toda** la base
> (`COL_LENGTH('COMISIO1','CPCPerNyA')` ⇒ `NULL`) y **`CPCResNyA` = 0 coincidencias** en toda la
> base. Las tablas físicas son `COMISIO1` (3 columnas: `PueCod`, `CPCId`, `CPCPerId`) y `COMISION`
> (14 columnas, sin `CPCResNyA`).
> ⇒ `CPCPerNyA` y `CPCResNyA` son **atributos calculados** de la KB (no columnas persistidas); el
> DDL que emite `genexus_db sql_ddl` los incluye por ser una **reconstrucción heurística** del SDK
> (`accuracy: "heuristic"`), no por el generador de reorganización.
> `PerNomyApe` **sí** es una columna física de `PERSONAS` (`char(40)`, nullable, 0 nulos en las
> 183 filas) y es la **única** columna de nombre de persona de la base.
> **Consecuencia funcional**: el ordenamiento `defined by CPCPerNyA` de `Seg_BusComi` y el
> `CPCResNyA` de `Seg_BusResponsable` **requieren** unir con `PERSONAS` (por `CPCPerId` y por
> `CPCResId` respectivamente). ⇒ **R-LOG-035** y **R-LOG-036**; el JOIN pasa a ser **obligatorio**
> (a diferencia de lo que anticipaba la nota previa). La **fórmula exacta** del atributo calculado
> no es legible por el MCP (los objetos `Attribute` no exponen `parts`) ⇒ queda registrada como
> **SA-LOG-15**, con criterio de verificación y tarea asociada (**T-008**).
> La tabla física de la comisión es **`COMISION`** y la de integrantes **`COMISIO1`** (§3.1.1).

---

## 4. Requisitos (EARS)

> Convención: `CUANDO <evento>, el sistema DEBE <respuesta>` (dirigido por evento),
> `SI <condición>, ENTONCES el sistema DEBE <respuesta>` (comportamiento no deseado),
> `El sistema DEBE <respuesta>` (ubiquo), `MIENTRAS <estado>, el sistema DEBE <respuesta>`.
> Trazabilidad: objeto GeneXus de origen.

### 4.1 Codificación y credenciales

**R-LOG-001 — Normalización del identificador de usuario**
CUANDO el usuario-ingrese un identificador en el formulario o en el canal REST, el sistema DEBE
normalizarlo **a mayúsculas** y **rellenarlo con espacios a la derecha hasta 10 caracteres** antes
de cualquier comparación o codificación.

- **Criterio de verificación**: test unitario que confirma que `"yoloffg"` → `"YOLOFFG   "`,
  `"YOLOFFG"` → `"YOLOFFG   "` y `"YOLOFFG   "` → `"YOLOFFG   "`; y test de integración que
  verifica que el usuario `YOLOFFG` (con `PERSONAS.PerCodUsu = 'YOLOFFG   '` y
  `SCRTY.UsuId = '(252;;:bbb'`) autentica con `"YOLOFFG"`.
- **Trazabilidad**: `Seg_ControlAcceso` (`&Usuario : CHARACTER(10)`) + `Seg_Encripta`
  (`&Desencrip : CHARACTER(10)`) + `SCRTY.UsuId char(10)` + `PERSONAS.PerCodUsu char(10)`.
- **Nota**: el padding a 10 es lo que hace que 549 de los 552 `SCRTY.UsuId` decodifiquen a 10
  caracteres y que `PERSONAS.PerCodUsu` sea `char(10)` en las 183 filas (ver §3.2.1). La
  comparación con `PERSONAS.PerCodUsu` es además insensible a los espacios finales en SQL Server.

**R-LOG-002 — Codificación `Seg_Encripta`**
CUANDO el sistema necesite codificar un identificador de 10 caracteres, el sistema DEBE aplicar la
sustitución monoalfabética **espejo** del legacy: normalizar a mayúsculas, recorrer las
posiciones 1 a 10, y para cada posición emitir el carácter del alfabeto de 66 located en el índice
`67 − origen`, **descartando** (sin emitir) todo carácter que no pertenezca al alfabeto.

- **Criterio de verificación**: test unitario con los vectores de §3.2 (`"ABC123"` → `"@?>PON"`,
  `"1234567890"` → `"PONMLKJIHQ"`, `"Z"` → `"a"`, `" "` → `"b"`, `"ÑUEZ"` → longitud 3) y test
  contra la BD que confirma que `Seg_Encripta("YOLOFFG   ") == "(252;;:bbb"`.
- **Trazabilidad**: `Seg_Encripta` (fuente completa).

**R-LOG-003 — Reversibilidad del mecanismo**
El sistema DEBE poder **deshacer** la codificación de `R-LOG-002` aplicando la misma tabla espejo
sin la normalización a mayúsculas (`origen = 67 − destino` sobre los 66 índices).

- **Criterio de verificación**: test de ida y vuelta que descifra los 552 `SCRTY.UsuId` y verifica
  que el resultado es un usuario en mayúsculas; el 100 % de los 552 decodifican sin excepción.
- **Trazabilidad**: `Seg_Encripta` (mapa `67 − &Cont`) + verificación contra `SCRTY` (§3.2.1).
- **Nota de seguridad**: el mecanismo **no es cifrado real** (constante pública, sin clave, reversible
  en una operación). La decisión de reproducirlo está en `plan.md` §2 (**D-LOG-03**).

**R-LOG-004 — Búsqueda del usuario en `SCRTY`**
CUANDO el sistema valide un intento de acceso, el sistema DEBE buscar en `SCRTY` la fila cuya
columna `UsuId` sea **igual, sin distinguir mayúsculas**, a la codificación del usuario normalizado
(`R-LOG-001` + `R-LOG-002`).

- **Criterio de verificación**: test de integración que autentica un usuario real cuyo `UsuId`
  contiene `b` minúscula (p. ej. `YOLOFFG` → `(252;;:bbb`) y obtiene acceso concedido; test
  negativo con un usuario inexistente ⇒ código 2.
- **Trazabilidad**: `Seg_ControlAcceso` → `where UsuId.ToUpper() = &UsuarioEncrip`.
- **Nota crítica**: la comparación **debe ser insensible a mayúsculas**. 504 de los 552 `UsuId`
  contienen `b` minúscula y 61 contienen `a` minúscula, y el legacy aplica `ToUpper()` **sólo al
  lado almacenado**; el match funciona porque la collation de la BD es `Latin1_General_CI_AS`.

**R-LOG-005 — Usuario sin clave almacenada**
SI la fila de `SCRTY` encontrada tiene `UsuClave` vacía (sólo espacios o `NULL`), ENTONCES el
sistema DEBE devolver el **código 2** y NO debe buscar la persona, la comisión ni el turno.

- **Criterio de verificación**: test de integración con las 2 cuentas reales que sólo tienen
  `UsuHash` (`Lucas Emanuel Nuñez` × 2, `UsuIngreso = 1`, `UsuClave` = 10 espacios) ⇒ resultado
  **código 2**, sin efectos en BD.
- **Trazabilidad**: `Seg_ControlAcceso` → `if not &UsuClave.IsEmpty()` / `else &error = 2`.
- **Nota**: el legacy **no consulta `UsuHash`** en el login (26 usuarios lo tienen y ningún objeto de
  la KB lo usa para autenticar). Las 2 cuentas quedan, por lo tanto, **sin acceso** por este
  mecanismo, lo que es **coherente** con el mensaje del código 5 (*"Debe cambiar su clave desde el
  Sistema Integrado (win)"*): su clave se administra fuera de este sistema.

**R-LOG-006 — Verificación de la clave**
CUANDO la fila de `SCRTY` tenga `UsuClave` no vacía, el sistema DEBE codificar la clave ingresada
con `R-LOG-002` y compararla con `SCRTY.UsuClave`; si **no** coinciden, el sistema DEBE devolver
el **código 2**.

- **Criterio de verificación**: test de integración con la clave real de un usuario de prueba ⇒
  código 0; con una clave alterada ⇒ código 2; test unitario de la comparación con una clave de
  10 caracteres que contiene espacios (`b` minúscula en el cifrado) ⇒ coincide.
- **Trazabilidad**: `Seg_ControlAcceso` → `&PassEncriptada = Udp(Seg_Encripta, &Pass)` /
  `if &PassEncriptada = &UsuClave` / `else &error = 2`.

**R-LOG-007 — Clave a cambiar**
SI la fila de `SCRTY` tiene `UsuIngreso = 1`, ENTONCES el sistema DEBE devolver el **código 5** y
NO debe continuar con la resolución de persona, comisión, receptoría ni turno.

- **Criterio de verificación**: test de integración con un usuario real con `UsuIngreso = 1`
  (hay 10) y clave correcta ⇒ código 5 y mensaje
  `Debe cambiar su clave desde el Sistema Integrado (win)`.
- **Trazabilidad**: `Seg_ControlAcceso` → `if UsuIngreso = 1` / `&error = 5`.

**R-LOG-008 — Resolución de la Persona**
CUANDO las credenciales sean válidas, el sistema DEBE resolver la Persona a partir del
identificador de usuario **en claro y normalizado** buscando en `PERSONAS` la fila con
`PerCodUsu` igual al identificador; si **no** existe, el sistema DEBE devolver el **código 3**.

- **Criterio de verificación**: test de integración con un usuario de `SCRTY` sin fila en
  `PERSONAS` (sólo 147 de las 183 personas tienen `PerCodUsu` no vacío) ⇒ código 3 y mensaje
  `Este Usuario no esta relacionado con una Persona del sistema`; test con un usuario que sí tiene
  persona ⇒ se resuelve `PERSONAS.PerId`.
- **Trazabilidad**: `Seg_BusPer` + `Seg_ControlAcceso` (`&PerId = Seg_BusPer.Udp(&Usuario)`,
  `if &PerId >= 0` … `else &error = 3`).
- **Nota**: `Seg_BusPer` recibe el identificador **en claro** (no codificado), a diferencia de la
  búsqueda en `SCRTY`.

### 4.2 Contexto operativo

**R-LOG-009 — Comisión activa del tipo Puesto Caminero**
CUANDO el tipo de acceso sea `'PUE'`, el sistema DEBE resolver la comisión activa del persona
integrante (`COMISIO1.CPCPerId = persona`, `COMISION.CPCCierre = 'N'`,
`CPCFchDes <= hoy <= CPCFchHas`, `COMISION.PueCod` del turno de la comisión) y, si **no** hay
ninguna, DEBE devolver el **código 4**.

- **Criterio de verificación**: test de integración con un persona integrante de una comisión
  vigente ⇒ se resuelve `CPCId` y `PueCod`; con una persona sin comisión vigente ⇒ código 4.
- **Trazabilidad**: `Seg_BusComi` + `Seg_ControlAcceso` (línea 7: `if &CPCID.IsEmpty()`).
- **Nota**: la comisión se descarta además si `CPCFchHas = hoy` y `CPCHorHas < hora actual`
  (la hora se compara como **número** — R-LOG-038). El **orden** de las candidatas es el de
  R-LOG-036 (JOIN a `PERSONAS`).

**R-LOG-010 — Responsable de la comisión**
CUANDO el tipo de acceso sea `'PUE'` y se haya resuelto la comisión, el sistema DEBE resolver el
responsable (`COMISION.CPCResId`, físico) y su **nombre y apellido** (de `PERSONAS.PerNomyApe` vía
el `CPCResId`; `CPCResNyA` es un atributo **calculado** del legacy y **no** una columna) de esa
comisión y del puesto, con los valores por defecto `0` / `Sin Comisión` cuando no exista.

- **Criterio de verificación**: test de integración que comprueba `Context.Responsable` y
  `Context.ResponsableNombre` para una comisión con responsable y para una sin responsable
  (`0` / `Sin Comisión`).
- **Trazabilidad**: `Seg_BusResponsable` + `Seg_ControlAcceso` (línea 8).

**R-LOG-011 — Receptoría del tipo Receptoría**
CUANDO el tipo de acceso **no** sea `'PUE'`, el sistema DEBE resolver la receptoría del persona
(`RECEPTO1.PerId = persona`) y su descripción (`RECEPTOR.ReceDsc`); si no hay receptoría
(`ReceId = -1`), el sistema DEBE devolver el **código 4**.

- **Criterio de verificación**: test de integración con un persona asignada a una receptoría ⇒ se
  resuelven `ReceId` y `ReceDsc`; sin asignación ⇒ código 4 y descripción
  `No Existe Receptoria`.
- **Trazabilidad**: `Seg_BusRece` + `Seg_ControlAcceso` (rama `else // Receptoria`).

**R-LOG-012 — Puesto y prefijo de red**
CUANDO se haya resuelto el puesto (`PUEPPRIM.PueCod`), el sistema DEBE obtener de `PUEPPRIM` la
descripción (`PueDesc`) y el prefijo de red (`PueIPRed`); si el puesto no existiera, el sistema DEBE
usar la descripción `Sin Puesto Asignado`.

- **Criterio de verificación**: test de integración que comprueba `Context.SucursalNombre` para un
  puesto existente y `Sin Puesto Asignado` para el caso sin puesto.
- **Trazabilidad**: `Seg_ControlAcceso` → `Sub 'BuscarNomPuesto'`.

**R-LOG-013 — Chequeo de IP del cliente**
CUANDO el tipo de acceso sea `'PUE'` y se haya resuelto el puesto, el sistema DEBE determinar la IP
del cliente leyendo la cabecera **`X-Forwarded-For`** y, si viene vacía, usando la **dirección
remota de la conexión**; luego DEBE verificar que esa IP **empiece con** el prefijo de red
configurado en `PUEPPRIM.PueIPRed`; si **no** cumple, el sistema DEBE devolver el **código 6**.

- **Criterio de verificación**: test de integración con `X-Forwarded-For` ausente (⇒ usa la IP
  remota) y con `X-Forwarded-For` presente; test que verifica el código 6 y el mensaje
  `Dispositivo no habilitado para el uso en el puesto caminero, contacte con el administrador`
  cuando la IP no pertenece a la red del puesto.
- **Trazabilidad**: `Seg_ControlAcceso` (línea 9) → `&HttpRequest.GetHeader('X-Forwarded-For')`,
  `RemoteAddr()`, `&ipcliente.IndexOf(&PueIPRed) = 0`.
- **Alcance**: el chequeo ocurre **sólo** en la rama `'PUE'`; la rama `'REC'` no lo ejecuta.

**R-LOG-014 — Turno**
CUANDO se haya resuelto la sucursal (`SucNroId >= 0`), el sistema DEBE resolver el turno activo
(`PPRIM_TU` con `SucNroId`, `SucTipo = Tipo`, `TurEstado = 'A'` y `TurCPCId = comisión` —o
cualquiera si el tipo es `'REC'`) y, si no hay, DEBE dejar el turno en `0`.

- **Criterio de verificación**: test de integración que comprueba `Context.Turno` para una persona
  con turno activo y `0` para una sin turno.
- **Trazabilidad**: `PPrim_BusTur` + `Seg_ControlAcceso` (línea 10).

**R-LOG-015 — Rama "fue responsable de una comisión"**
SI la sucursal no se pudo resolver (`SucNroId < 0`) y la persona fue responsable de alguna comisión
cerrada (`COMISION.CPCResId = persona` y `CPCCierre = 'S'`) y el tipo de acceso es `'PUE'`, ENTONCES
el sistema DEBE cargar un **contexto parcial** (4 campos: `Usuario`, `UsuarioNombre`, `PersonaId`,
`Tipo`), DEBE devolver el **código 0** y NO debe aplicar el chequeo de IP.

- **Criterio de verificación**: test de integración con un persona que fue responsable de una
  comisión cerrada ⇒ código 0, `Context.Sucursal = 0`, `Context.Comision = 0`.
- **Trazabilidad**: `Seg_ControlAcceso` → `Sub 'BuscarHistoriaComision'` + rama `else` de
  `&SucNroId >= 0`.
- **Nota**: esta rama **no existe** en la variante WS (§3.8).

### 4.3 Contexto de sesión

**R-LOG-016 — Contenido del contexto**
CUANDO el acceso sea concedido, el sistema DEBE construir el contexto de sesión con los **13
campos** del SDT `Context` (§3.5), cargando los **10** que el legacy carga
(`Usuario`, `UsuarioNombre`, `PersonaId`, `Comision`, `Turno`, `Sucursal`, `SucursalNombre`,
`Tipo`, `Responsable`, `ResponsableNombre`) y dejando `Form2276`, `F276Id` y `UltimoMenu` en su
valor por defecto.

- **Criterio de verificación**: test unitario que compara el mapa de campos produced con los 13
  nombres del SDT; test de integración que comprueba los valores de los 10 campos Effective en la
  sesión tras un login exitoso.
- **Trazabilidad**: SDT `Context` + `Seg_ControlAcceso` (línea 11) + `SetContext`.
- **Nota**: `Context.Usuario` se guarda **en mayúsculas** (`&Usuario.ToUpper()`), a diferencia de la
  rama de historia de comisión, que lo guarda tal cual.

**R-LOG-017 — Persistencia del contexto en la sesión**
CUANDO el contexto se haya construido, el sistema DEBE persistirlo en la **sesión HTTP** de forma
que sobreviva entre peticiones, y DEBE poder recuperarlo íntegro.

- **Criterio de verificación**: test de integración que hace login, luego consulta la sesión en una
  petición posterior y verifica los 10 campos.
- **Trazabilidad**: `SetContext` (`&Session.Set(!"context", &Context.ToXml())`) + `LoadContext`
  (`&Context.FromXml(&Session.Get(!"context"))`).

### 4.4 Resultado y presentación del error

**R-LOG-018 — Matriz de códigos y mensajes**
El sistema DEBE mapear el resultado del acceso a los **7 códigos** (0 a 6) con los **mensajes
verbatim** de §3.4, y NO DEBE inventar ni corregir los textos (incluidas las erratas "esta" y
"Receptoria").

- **Criterio de verificación**: test unitario parametrizado con los 7 pares (código, mensaje) que
  falla si un texto difiere **carácter a carácter** del legacy.
- **Trazabilidad**: `Login.Event Enter` → `Do Case` sobre `&error`.

**R-LOG-019 — El login no lanza excepción**
El servicio de autenticación NO DEBE lanzar excepciones para un intento de acceso fallido: DEBE
devolver el **código 0-6** y el contexto resultante, y el controlador DEBE renderizar el mensaje
en la vista.

- **Criterio de verificación**: test unitario que invoca el servicio con credenciales inválidas y
  verifica que no se propaga ninguna excepción y que el resultado es un código; test de vista que
  verifica que un login fallido devuelve **HTTP 200** con el mensaje renderizado.
- **Trazabilidad**: `Login` (el legacy asigna el mensaje al `TextBlockPass`, no lanza error) +
  decisión de `openspec/features/03-login/README.md` §"Reglas que condicionan la spec".

**R-LOG-020 — Mensaje de error junto al campo**
SI el resultado del acceso no sea 0, ENTONCES el sistema DEBE mostrar el mensaje de §3.4 **junto al
campo de contraseña**, con el mismo tratamiento visual del `TextBlockPass` rojo del legacy.

- **Criterio de verificación**: test `@WebMvcTest` que verifica que la vista de login contiene el
  texto exacto del mensaje cuando el servicio devuelve un código != 0, y que el bloque de error
  lleva la clase visual de error (token de color rojo equivalente al `ForeColor="Red"`).
- **Trazabilidad**: `Login` (WebForm) → `<gxTextBlock id="TextBlockPass" ForeColor="Red" …>`.

**R-LOG-021 — Usuario vacío**
SI el identificador de usuario ingresado está vacío (o contiene sólo espacios), ENTONCES el
sistema DEBE mostrar el mensaje `Usuario inválido` **sin** consultar la base de datos.

- **Criterio de verificación**: test `@WebMvcTest` con usuario vacío ⇒ la vista muestra
  `Usuario inválido` y el servicio **no** fue invocado (verificable con Mockito).
- **Trazabilidad**: `Login.Event Enter` → `If &Usuario.IsEmpty() Msg('Usuario inválido')`.

### 4.5 Formulario, sesión y navegación

**R-LOG-022 — Pantalla de login**
CUANDO el usuario solicite `/login` sin una sesión autenticada, el sistema DEBE renderizar el
formulario con los campos `Usuario`, `Contraseña` y `Acceso` (tipo), con el botón `Ingresar`, con
el token CSRF y con el **tipo de acceso precargado desde la cookie `SUCURSAL`** si existe.

- **Criterio de verificación**: test `@WebMvcTest` que verifica el nombre de la vista, la presencia
  de los 3 campos con sus rótulos, el campo `_csrf` y que el selector de tipo trae el valor de la
  cookie.
- **Trazabilidad**: `Login` (WebForm + `Event Start`).

**R-LOG-023 — Cookie `SUCURSAL`**
CUANDO el acceso sea concedido, el sistema DEBE escribir la cookie `SUCURSAL` con el tipo de acceso
(`'PUE'` o `'REC'`), de modo que el login siguiente lo recupere; y NO DEBE escribirla en el camino
de error.

- **Criterio de verificación**: test `@WebMvcTest` que verifica la presencia de la cookie
  `SUCURSAL` en la respuesta de un login exitoso con su valor, y su ausencia en un login fallido;
  test que verifica que un `GET /login` posterior trae el tipo precargado.
- **Trazabilidad**: `Login.Event Start` (`GetCookie('SUCURSAL')`) + `Login.Event Enter`
  (`SetCookie('SUCURSAL', &Tipo)`).

**R-LOG-024 — Página `Home` y protección de rutas**
MIENTRAS no exista un contexto de sesión válido, el sistema DEBE servir la pantalla de login;
CUANDO exista un contexto válido, el sistema DEBE servir la pantalla `Home`.

- **Criterio de verificación**: test de integración con `MockMvc` + sesión: sin sesión ⇒
  redirección a `/login`; con contexto ⇒ `200` en `/home` con el nombre de usuario en el modelo.
- **Trazabilidad**: `Home.Event Start` (`LoadContext` → `if &Context.Usuario.IsEmpty()`).

**R-LOG-025 — Autorización por programa**
CUANDO el sistema arme el menú de programas del usuario, DEBE incluir un programa **sólo** si
`IsAuthorized(programa)` devuelve verdadero, y DEBE exponer la lista de programas autorizados
(nombre, descripción y enlace) para el menú.

- **Criterio de verificación**: test unitario que verifica que la lista incluye `WWPPrim_Turnos`
  (*Turnos de Comisiones*) y `WWTDetProdPri` (*Buscar Guias*) con su descripción; test que verifica
  que un programa no autorizado queda fuera de la lista.
- **Trazabilidad**: `ListPrograms` + `IsAuthorized`.
- **Nota**: en la KB actual `IsAuthorized` **devuelve siempre `True`** (su lógica está comentada),
  por lo que la lista completa es el resultado esperado. Ver **SA-L02**.

**R-LOG-026 — Página `NotAuthorized`**
CUANDO el usuario intente acceder a un programa para el que no está autorizado, el sistema DEBE
mostrar la página `NotAuthorized` con el mensaje `You are not authorized to do this action`.

- **Criterio de verificación**: test `@WebMvcTest` que verifica la ruta `/not-authorized`, el
  nombre de la vista y el texto exacto del mensaje.
- **Trazabilidad**: `NotAuthorized` (WebPanel) → `Event Start` → `Msg(...)`.

### 4.6 Canal REST

**R-LOG-027 — Endpoint de login**
CUANDO un cliente envíe `POST /api/v1/auth/login` con `{usuario, pass, tipo}`, el sistema DEBE
responder **HTTP 200** con el cuerpo `{error, sucursal, comision}` de la forma `SDTSalidaLoginWS`
(`error` numérico 0-6, `sucursal` y `comision` numéricos).

- **Criterio de verificación**: test de contrato con `MockMvc` que verifica el JSON exacto para un
  login exitoso y para un login fallido; y el request del endpoint en la colección Postman.
- **Trazabilidad**: `Seg_ControlAccesoWS` + `SDTEntradaLoginWS` + `SDTSalidaLoginWS`.

**R-LOG-028 — El canal REST es stateless**
El canal REST NO DEBE crear sesión, NO DEBE escribir la cookie `SUCURSAL`, NO DEBE cargar el
contexto de sesión, NO DEBE aplicar el chequeo de IP y NO DEBE exigir token CSRF.

- **Criterio de verificación**: test que verifica que la respuesta del endpoint no incluye
  `Set-Cookie` y que una segunda llamada no hereda estado; test que verifica que un
  `POST /api/v1/auth/login` **sin** token CSRF devuelve **200** (no 403).
- **Trazabilidad**: `Seg_ControlAccesoWS` (bloques de contexto y de IP comentados) +
  **D-LOG-17(a)** (aprobada 2026-10-22: el descarte de CSRF alcanza a **todo** `/api/**`, sustituye
  a D-LOG-11) + **D-LOG-19(a)** (`permitAll` en `/api/v1/**`, sustituye a D-LOG-12).

**R-LOG-029 — Diferencia de la rama "sin sucursal" en el canal REST**
SI en el canal REST la sucursal no se puede resolver, ENTONCES el sistema DEBE responder con
`error = 0` y `sucursal`/`comision` en sus valores por defecto, **sin** aplicar la rama de
historia de comisión.

- **Criterio de verificación**: test de integración con un usuario de tipo `'REC'` sin receptoría ⇒
  `error = 0`, `sucursal = 0`, `comision = 0`.
- **Trazabilidad**: `Seg_ControlAccesoWS` (bloque `BuscarHistoriaComision` comentado).

**R-LOG-030 — Credenciales de la colección Postman**
Todo endpoint nuevo del módulo DEBE estar reflejado en
`docs/postman/ATPPuestos-API.postman_collection.json` en el mismo commit.

- **Criterio de verificación**: la colección contiene una carpeta `Auth` con el request
  `POST {{baseUrl}}/api/v1/auth/login` con el body `{usuario, pass, tipo}` y un test que verifica
  el status `200`; el README de Postman registra el cambio en su *Changelog*.
- **Trazabilidad**: regla **G-POSTMAN** (guardarraíl 6 de `AGENTS.md`) + `docs/postman/README.md`.

### 4.7 Seguridad transversal

**R-LOG-031 — Protección de rutas y CSRF**
El sistema DEBE exigir una sesión autenticada para toda **ruta MVC** salvo el login y los recursos
estáticos; DEBE exigir **token CSRF** en los formularios de escritura **MVC** (los que operan con
sesión); y NO DEBE exigir token CSRF ni autenticación en las rutas **`/api/**`**, que son
*stateless* (`permitAll`, salvo que un módulo futuro las endurezca).

- **Criterio de verificación**: (a) test de integración que verifica que una petición a `/home` sin
  sesión redirige a `/login`; (b) un `POST /login` sin token CSRF ⇒ **403**; (c) un
  `POST /api/v1/productos` **sin** token CSRF ⇒ **no** es rechazado por CSRF (responde el código de
  negocio, no 403 ni 401); (d) un `POST /api/v1/auth/login` sin token ⇒ **200** con el cuerpo del SDT.
- **Trazabilidad**: `openspec/constitution/stack.md` §1 (Spring Security, sesión HTTP + CSRF) +
  **D-LOG-17(a)** (aprobada 2026-10-22, sustituye a D-LOG-11) + **D-LOG-19(a)** (sustituye a
  D-LOG-12).
- **Nota**: la política por familia de rutas es **explícita** (R-LOG-041) y no depende del
  comportamiento implícito del filtro por defecto.

**R-LOG-032 — Auditoría de usuario autenticado**
CUANDO el sistema registre una operación de escritura, DEBE usar como usuario auditor el
identificador del usuario autenticado en la sesión.

- **Criterio de verificación**: test de integración que verifica que el registro de auditoría usa
  el `Context.Usuario` de la sesión y no el valor por defecto `SISTEMA`.
- **Trazabilidad**: `atp-nucleo-compartido/src/main/resources/application.yml`
  (`app.auditoria.usuario`, que hoy tiene el valor por defecto `SISTEMA` "hasta que exista el
  módulo de login").

### 4.8 Paridad de BD verificada (revisión v2 — cierra SA-LOG-08)

**R-LOG-033 — Nombres físicos exactos de las tablas del contexto**
El sistema DEBE mapear las 8 tablas del contexto de acceso con su **nombre físico exacto**,
verificado contra `INFORMATION_SCHEMA` de `Rentas`: `SCRTY`, `PERSONAS`, `PUEPPRIM`, `RECEPTOR`,
`RECEPTO1`, **`COMISION`**, **`COMISIO1`**, `PPRIM_TU`; y NO DEBE crear, renombrar ni duplicar
ninguna de ellas.

- **Criterio de verificación**: inspección de las entidades JPA: los 8 valores de `@Table(name=…)`
  coinciden **carácter a carácter** con la tabla de §3.1.1; el arranque de la aplicación con
  `ddl-auto=none` no emite ninguna sentencia DDL; un IT lee una fila real de cada una de las 8
  tablas.
- **Trazabilidad**: G-DATA 2026-10-22 (§3.1.1) + `plan.md` §5.3 + **SA-LOG-08 cerrado**.

**R-LOG-034 — Claves primarias compuestas**
El sistema DEBE modelar las claves primarias compuestas **completas** y en el **orden** de la
base: `COMISION` (`PueCod`, `CPCId`), `COMISIO1` (`PueCod`, `CPCId`, `CPCPerId`), `PPRIM_TU`
(`SucNroId`, `SucTipo`, `TurNroId`), `RECEPTO1` (`ReceId`, `PerId`); y las simples `SCRTY.UsuId`,
`PERSONAS.PerId`, `PUEPPRIM.PueCod`, `RECEPTOR.ReceId`.

- **Criterio de verificación**: test de integración que persiste y recupera una fila de cada
  entidad con clave compuesta, y test que verifica que la consulta generada por Hibernate incluye
  **todas** las columnas de la PK en el `WHERE`.
- **Trazabilidad**: `INFORMATION_SCHEMA.KEY_COLUMN_USAGE` (PK verificada) + `plan.md` §5.3.

**R-LOG-035 — Los atributos calculados del legacy NO son columnas**
El sistema NO DEBE mapear como columna las etiquetas calculadas `COMISIO1.CPCPerNyA`,
`COMISION.CPCResNyA` y `COMISION.PueDesc`, porque **no existen** en la base; y DEBE resolver el
**nombre y apellido** del integrante (`CPCPerNyA`), del responsable (`CPCResNyA`) y de la
receptoría (`ReceDsc`) mediante **JOIN** con `PERSONAS` (por `PerId`) y con `RECEPTOR` (por
`ReceId`).

- **Criterio de verificación**: (a) inspección del mapeo: ninguna entidad declara `CPCPerNyA`,
  `CPCResNyA` ni `PueDesc` como columna de `COMISION`/`COMISIO1`; (b) IT que resuelve un
  responsable real y verifica que `Context.ResponsableNombre` **no** es la cadena vacía ni
  `Sin Comisión`; (c) IT que resuelve una receptoría real y verifica `ReceDsc` = `RECEPTOR.ReceDsc`
  de esa receptoría.
- **Trazabilidad**: G-DATA 2026-10-22 (`CPCPerNyA`/`CPCResNyA` = 0 coincidencias en toda la base)
  + `Seg_BusComi` / `Seg_BusResponsable` / `Seg_BusRece` + **SA-LOG-15** (fórmula exacta del
  atributo calculado, abierta).

**R-LOG-036 — Orden determinista de `Seg_BusComi`**
CUANDO el sistema resuelva la comisión activa de una persona, DEBE reproducir el orden del legacy
(`defined by CPCPerNyA`) **sobre el JOIN a `PERSONAS`**, es decir, gainando la fila cuyo
**nombre y apellido de persona** es el mayor entre las que cumplen `CPCFchDes <= hoy <= CPCFchHas`
y `CPCCierre = 'N'`; y, si no hay coincidencias, DEBE buscar como **responsable** con las mismas
condiciones de fecha y cierre.

- **Criterio de verificación**: IT que, para una persona con más de una comisión vigente, verifica
  que la comisión resuelta es la de mayor `PERSONAS.PerNomyApe` (y no la primera por `CPCId`);
  test que verifica el criterio de desempate cuando dos personas tienen el mismo nombre.
- **Trazabilidad**: `Seg_BusComi` (fuente completa) + `plan.md` §3.9 + **R-LOG-035**.

**R-LOG-037 — `UsuIngreso` nulo equivale a "no debe cambiar la clave"**
SI la fila de `SCRTY` tiene `UsuIngreso` **nulo** (1 de las 552 filas), ENTONCES el sistema DEBE
tratarlo **exactamente** como un valor distinto de `1` (⇒ **no** código 5), en paridad con la
comparación `if UsuIngreso = 1` del legacy.

- **Criterio de verificación**: test de integración con el usuario real que tiene `UsuIngreso`
  nulo y clave correcta ⇒ el resultado **no** es el código 5.
- **Trazabilidad**: `Seg_ControlAcceso` (`if UsuIngreso = 1`) + G-DATA 2026-10-22
  (`UsuIngreso` nullable; 10 filas con `1`, 1 con `NULL`).

**R-LOG-038 — La hora de la comisión es un número**
CUANDO el sistema compare `COMISION.CPCHorHas` con la hora actual, DEBE tratarlo como un
**número entero** (hora en formato `HHMM`, p. ej. `1730`), porque la columna física es `int`; y
DEBE aplicar el descarte de la comisión **sólo** cuando `CPCFchHas` sea **igual** al día actual.

- **Criterio de verificación**: IT con una comisión cuyo `CPCFchHas` es hoy y `CPCHorHas` anterior a
  la hora actual ⇒ la comisión se descarta (`CPCId = 0`, ⇒ código 4); IT con `CPCHorHas` posterior
  ⇒ la comisión se conserva.
- **Trazabilidad**: `Seg_BusComi` (`&Hora = val(substr(…,1,2) + substr(…,4,2))`,
  `if CPCFchHas = &Hoy / if CPCHorHas < &Hora`) + G-DATA 2026-10-22 (`CPCHorHas` `int`).

### 4.9 Endurecimiento y coherencia de la seguridad — hallazgos del QA del módulo 02 (revisión v2)

> **Política aprobada el 2026-10-22 por el responsable del proyecto** (alternativa **(a)** en las
> tres decisiones): **D-LOG-17(a)** — `/api/**` es *stateless* y **sin CSRF**, el CSRF se exige
> **sólo** en las rutas MVC con sesión · **D-LOG-18(a)** — materialización anticipada del token
> conservando `HttpSessionCsrfTokenRepository` · **D-LOG-19(a)** — `permitAll` en `/api/v1/**`, con
> la deuda **SA-LOG-14** asumida y la tarea **T-135** de seguimiento.
> ⇒ Los requisitos de esta sección son **normativos y ejecutables**; no queda ninguna aprobación
> pendiente.

**R-LOG-039 — La API no rechaza por CSRF y responde con el código de negocio**
SI una petición de **escritura** a `/api/**` no aporta token CSRF, ENTONCES el sistema **NO DEBE**
rechazarla por CSRF (ni devolver **403**, ni **401**, ni una página de error HTML): DEBE
procesarla y responder con el **código de negocio** (`201`/`200`/`204`/`400`/`404`/`409` según el
caso); y SI una petición a una **ruta MVC** de escritura no aporta token CSRF, ENTONCES el sistema
DEBE responder **HTTP 403**.

- **Criterio de verificación**: (a) `POST /api/v1/productos` **sin** token CSRF y sin credenciales
  ⇒ responde el código de negocio (**no** 403, **no** 401, **no** HTML); (b) `POST /login` sin token
  ⇒ **403**; (c) `POST /api/v1/auth/login` sin token ⇒ **200** con el cuerpo del SDT; (d) ninguno
  de los casos devuelve **500** ni la página `ERROR_INTERNO`.
- **Trazabilidad**: `docs/verificacion/02-abm-productos.md` §4 **H3** y **H6** + **D-LOG-17(a)** y
  **D-LOG-19(a)** (aprobadas 2026-10-22).
- **Nota**: con la política aprobada, el **403** queda reservado a las rutas MVC y el **401** al
  endurecimiento futuro de la API (T-135). La ausencia de CSRF en la API se mitiga según
  R-LOG-041 (nota de mitigación).

**R-LOG-040 — La colección Postman declara su autenticación y es ejecutable**
La colección Postman DEBE declarar de forma **explícita** el esquema de autenticación que usa
(bloque `auth` a nivel de colección o de request) —en este módulo, el valor que corresponde a una
API **sin credenciales** (`noauth`)— y TODOS sus requests de escritura DEBE ser **ejecutables de
punta a punta** contra la aplicación en ejecución, **sin** token CSRF y **sin** credenciales
versionadas, con el **mismo** código de estado que devuelve el backend.

- **Criterio de verificación**: (a) el JSON de la colección tiene `auth` **explícito** (no vacío ni
  heredado por sorpresa); (b) **ninguna** credencial real versionada en la colección ni en el
  environment (**G3**); (c) un recorrido completo de la colección (carpetas `Productos`, `Auth` y las
  de `01-nucleo-datos`) contra la app real devuelve **sin** errores de estado; (d) el `README` de
  Postman documenta que la API **no** exige autenticación en este módulo.
- **Trazabilidad**: **G-POSTMAN** (guardarraíl 6 de `AGENTS.md`) + `docs/verificacion/02-abm-productos.md` §4 **H3** +
  **D-LOG-17(a)** y **D-LOG-19(a)**.

**R-LOG-041 — Política CSRF explícita por ruta y mitigación de su ausencia en la API**
El `SecurityFilterChain` DEBE declarar **explícitamente** la política CSRF de **cada** familia de
rutas —**rutas MVC con sesión: exigido**; **`/api/**`: no exigido** (descarte explícito)— y NO DEBE
depender del comportamiento implícito del filtro por defecto de Spring Boot. Además, como
consecuencia de no proteger `/api/**` contra CSRF, el sistema DEBE: (i) **no** tomar la autoridad
de una petición `/api/**` de la cookie de sesión (ningún endpoint de la API DEBE leer la sesión
para autorizar), (ii) emitir la cookie de sesión `JSESSIONID` y la cookie `SUCURSAL` con
`SameSite=Lax`, y (iii) usar, cuando la API se endurezca, la cabecera **`Authorization`** para la
credencial —**nunca** una cookie.

- **Criterio de verificación**: (a) inspección de la configuración de seguridad: hay una regla CSRF
  **explícita** por familia de rutas y no queda nada en el comportamiento implícito; (b) test que
  verifica el comportamiento en las dos familias (MVC ⇒ 403 sin token; API ⇒ no 403); (c) test que
  verifica que un request a `/api/v1/**` **no** crea ni lee sesión de negocio (respuesta sin
  `Set-Cookie: JSESSIONID`); (d) test que verifica `SameSite=Lax` en `JSESSIONID` y en `SUCURSAL`.
- **Trazabilidad**: **D-LOG-17(a)** (aprobada 2026-10-22) + **D-LOG-19(a)** +
  `docs/verificacion/02-abm-productos.md` §4 **H3**/**H6** + mitigación acordada en `plan.md` §4.5.

**R-LOG-042 — El token CSRF se materializa antes de que la respuesta se confirme**
CUANDO una ruta servida por el sistema devuelva una **página HTML** (con o sin sesión previa),
el sistema DEBE **materializar** el token CSRF **antes** de que la respuesta HTTP se confirme
(*commit*), de modo que la página renderizada contenga el campo `_csrf` y **NO** se produzca un
`IllegalStateException: Cannot create a session after the response has been committed` ni una
página de error `ERROR_INTERNO`. La materialización DEBE hacerse conservando
`HttpSessionCsrfTokenRepository` (filtro propio que fuerza `csrfToken.getToken()` antes del render,
o `setRequestAttribute` con `CsrfTokenRequestAttributeHandler`), y NO DEBE cambiar el contrato del
formulario, el perfil de cookies ni el HTML/ARIA de las vistas.

- **Criterio de verificación**: (a) test de integración que pide una ruta HTML **sin cookie de
  sesión** y verifica **HTTP 200** con un `<form>` y el campo `_csrf` en el cuerpo; (b) el arranque
  de la aplicación **no** registra trazas `ERROR` asociadas a ese `IllegalStateException` durante
  esa petición; (c) el flujo de navegador (login por formulario) sigue funcionando: el `th:action`
  del formulario de login y de `/productos` sigue emitiendo el token; (d) el repositorio de token
  sigue siendo `HttpSessionCsrfTokenRepository` (**no** `CookieCsrfTokenRepository`) y el HTML
  renderizado es **idéntico** al previo.
- **Trazabilidad**: `docs/verificacion/02-abm-productos.md` §4 **H4** (causa raíz confirmada:
  `HttpSessionCsrfTokenRepository` difiere el guardado del token hasta el primer acceso, que ocurre
  en el `th:action` de `templates/nucleo/productos.html` línea 103, con la respuesta ya
  confirmada) + **D-LOG-18(a)** (aprobada 2026-10-22).

**R-LOG-043 — Toda pantalla HTML es servible sin sesión previa**
CUANDO un cliente solicite una ruta HTML protegida **sin** cookie de sesión previa, el sistema DEBE
servir la **pantalla solicitada** (HTTP **200** con su formulario y su token CSRF) y NO DEBE
devolver la página de error interna ni un código de estado distinto del de la pantalla.

- **Criterio de verificación**: test de integración que solicita `/login` y `/productos` sin
  cookie de sesión y verifica **200** + presencia de `<form>` y `_csrf` en ambos casos (regresión
  directa de H4).
- **Trazabilidad**: `docs/verificacion/02-abm-productos.md` §4 **H4** (reproducible 4/4) +
  **R-LOG-042** + **D-LOG-18(a)** (aprobada 2026-10-22).

**R-LOG-044 — La colección Postman se divide por módulo**
El sistema DEBE mantener la colección Postman **dividida en un archivo por módulo** dentro de
`docs/postman/`, con un índice (`README.md`) que apunte a cada archivo, y **ningún** archivo de
`docs/postman/` DEBE superar las **500 líneas** (G4).

- **Criterio de verificación**: `docs/postman/` contiene un archivo por módulo + el índice; el
  índice lista cada archivo y su contenido; ningún archivo del directorio supera las 500 líneas
  (`(Get-Content f | Measure-Object -Line).Lines`); la suma de requests de todos los archivos
  conserva la cobertura actual (44 + 9 + los de `Auth`).
- **Trazabilidad**: **G4** (guardarraíl 4 de `AGENTS.md`) + `docs/verificacion/02-abm-productos.md` §4 **H5**
  (la colección monolítica tiene **1990** líneas).

**R-LOG-045 — Los ejemplos de la colección reflejan el contrato real**
Los cuerpos de ejemplo de la colección Postman DEBE enviar **únicamente** los campos que el DTO de
escritura **consume**; y NO DEBE enviar campos declarados como informativos o derivados (por
ejemplo `uMedNombre`), que el backend excluye por construcción.

- **Criterio de verificación**: un test (o una revisión firmada) que compara, campo a campo, los
  cuerpos de ejemplo de la colección con las propiedades declaradas en los DTOs de escritura de
  `web/dto/`; ningún body de ejemplo contiene `uMedNombre`.
- **Trazabilidad**: `docs/verificacion/02-abm-productos.md` §4 **H7** + **R-PROD-19** del módulo 02.

**R-LOG-046 — Dialecto de Hibernate y versión de SQL Server**
El sistema DEBE fijar de forma **explícita** el dialecto de Hibernate (o corregir los parámetros
de la URL de conexión) de modo que el arranque **no** emita el aviso `HHH000511` ("the 10.0.0
version for `SQLServerDialect` is no longer supported"), y el cambio NO DEBE alterar el
comportamiento observable contra la BD real.

- **Criterio de verificación**: (a) el log de arranque **no** contiene `HHH000511`; (b) la versión
  reportada por el servidor es la real (SQL Server 2019, 15.0.2170.1) y la suite de IT completa
  sigue en verde sin diferencias en los resultados; (c) el `git diff` **no** toca
  `spring.jpa.hibernate.ddl-auto` (sigue en `none`).
- **Trazabilidad**: `docs/verificacion/02-abm-productos.md` §4 **H9** + G-DATA 2026-10-22
  (`@@VERSION` = *Microsoft SQL Server 2019 … 15.0.2170.1*).

---

## 5. Matriz de paridad (ISO/IEC 25030)

| Requisito | Comportamiento legacy | Origen GeneXus | Prueba prevista |
|---|---|---|---|
| R-LOG-001 | Padding a 10 + mayúsculas | `Seg_ControlAcceso`, `Seg_Encripta` | Unit + IT |
| R-LOG-002 | Espejo 67−i sobre 66 | `Seg_Encripta` | Unit (vectores) |
| R-LOG-003 | Reversible | `Seg_Encripta` | Unit (552 filas) |
| R-LOG-004 | `UsuId.ToUpper() = enc` | `Seg_ControlAcceso` | IT |
| R-LOG-005 | `UsuClave` vacía ⇒ 2 | `Seg_ControlAcceso` | IT (2 cuentas) |
| R-LOG-006 | `enc(pass) = UsuClave` | `Seg_ControlAcceso` | Unit + IT |
| R-LOG-007 | `UsuIngreso = 1` ⇒ 5 | `Seg_ControlAcceso` | IT |
| R-LOG-008 | `Seg_BusPer` ⇒ 3 | `Seg_BusPer` | IT |
| R-LOG-009 | `Seg_BusComi` ⇒ 4 | `Seg_BusComi` | IT |
| R-LOG-010 | `Seg_BusResponsable` | `Seg_BusResponsable` | IT |
| R-LOG-011 | `Seg_BusRece` ⇒ 4 | `Seg_BusRece` | IT |
| R-LOG-012 | `BuscarNomPuesto` | `Seg_ControlAcceso` | IT |
| R-LOG-013 | `X-Forwarded-For` / `RemoteAddr` / `PueIPRed` ⇒ 6 | `Seg_ControlAcceso` | IT |
| R-LOG-014 | `PPrim_BusTur` | `PPrim_BusTur` | IT |
| R-LOG-015 | `BuscarHistoriaComision` ⇒ 0 | `Seg_ControlAcceso` | IT |
| R-LOG-016 | 10 de 13 campos del SDT | SDT `Context` | Unit + IT |
| R-LOG-017 | `Session.Set/FromXml` | `SetContext` / `LoadContext` | IT |
| R-LOG-018 | 7 códigos + textos verbatim | `Login.Event Enter` | Unit |
| R-LOG-019 | Sin excepción | `Login` | Unit + WebMvcTest |
| R-LOG-020 | `TextBlockPass` rojo | `Login` WebForm | WebMvcTest |
| R-LOG-021 | `Msg('Usuario inválido')` | `Login.Event Enter` | WebMvcTest |
| R-LOG-022 | Formulario con 3 campos | `Login` WebForm | WebMvcTest |
| R-LOG-023 | `GetCookie` / `SetCookie('SUCURSAL')` | `Login` | WebMvcTest |
| R-LOG-024 | `Home.Event Start` | `Home` | IT |
| R-LOG-025 | `ListPrograms` + `IsAuthorized` | `ListPrograms`, `IsAuthorized` | Unit |
| R-LOG-026 | `Msg('You are not authorized…')` | `NotAuthorized` | WebMvcTest |
| R-LOG-027 | `SDTSalidaLoginWS` | `Seg_ControlAccesoWS` | MockMvc + Postman |
| R-LOG-028 | Sin sesión / sin IP | `Seg_ControlAccesoWS` | MockMvc |
| R-LOG-029 | `error = 0` sin sucursal | `Seg_ControlAccesoWS` | IT |
| R-LOG-030 | Sincronización Postman | G-POSTMAN | Inspección |
| R-LOG-031 | Sesión + CSRF | Stack §1 | IT |
| R-LOG-032 | Auditoría con el usuario de sesión | `application.yml` | IT |
| R-LOG-033 | Nombres físicos `COMISION`/`COMISIO1`/… | G-DATA §3.1.1 | Inspección + IT |
| R-LOG-034 | PK compuestas (`COMISION`, `COMISIO1`, `PPRIM_TU`, `RECEPTO1`) | G-DATA §3.1.1 | IT |
| R-LOG-035 | `CPCPerNyA`/`CPCResNyA`/`ReceDsc` son **calculados** | G-DATA + KB | Inspección + IT |
| R-LOG-036 | `defined by CPCPerNyA` | `Seg_BusComi` | IT |
| R-LOG-037 | `UsuIngreso` nulo ≠ 1 | `Seg_ControlAcceso` | IT |
| R-LOG-038 | `CPCHorHas` numérico | `Seg_BusComi` | IT |
| R-LOG-039 | 401 (sin credenciales) vs 403 (sin CSRF) | QA H3/H6 | IT |
| R-LOG-040 | Colección Postman con `auth` y ejecutable | G-POSTMAN | Recorrido + inspección |
| R-LOG-041 | Política CSRF explícita por ruta | QA H3/H6 | Inspección + IT |
| R-LOG-042 | Token CSRF materializado antes del *commit* | QA H4 | IT + log |
| R-LOG-043 | Pantalla HTML servible sin sesión previa | QA H4 | IT |
| R-LOG-044 | Colección Postman dividida por módulo (< 500 líneas) | G4 | Inspección |
| R-LOG-045 | Ejemplos de la colección = contrato real | QA H7 | Inspección / test |
| R-LOG-046 | Sin `HHH000511`; dialecto explícito | QA H9 | Log de arranque + IT |

---

## 6. Supuestos abiertos

> Cada supuesto lleva la **pregunta exacta** que hay que responder. Ninguno bloquea la
> implementación salvo indicación contraria.
>
> **Numeración**: los supuestos de este capítulo se renumeran en `plan.md` §6 con prefijo propio
> del módulo, en correspondencia **1:1** (`SA-L0x` → `SA-LOG-0x`). Allí se agregan los supuestos
> nuevos **SA-LOG-08 … SA-LOG-14** (nombre físico de las tablas de comisiones y turnos,
> conectividad de la BD, enlace de los programas del menú, uso de `UsuGrupo`/`UsuNiv`, nombres de
> campo del canal WS, valor por defecto del selector `Acceso` y endurecimiento del API de datos) y
> los **SA-LOG-15 … SA-LOG-18** de la revisión v2 (§6.1 de este capítulo).
> **SA-LOG-08/SA-LOG-09 están CERRADOS** desde la revisión v2 (2026-10-22): ver §3.1.1 y
> `plan.md` §6. **SA-LOG-16 y SA-LOG-17 también están CERRADOS** (2026-10-22): las decisiones
> **D-LOG-17** y **D-LOG-18** fueron **aprobadas por el responsable del proyecto** (alternativa
> (a) en ambas). **Quedan abiertos, pero explícitamente asumidos y con tarea**: **SA-LOG-14**
> (deuda de `permitAll` ⇒ **T-135**), **SA-LOG-15** (fórmula de los atributos calculados ⇒ **T-008**)
> y **SA-LOG-18** (alcance de G4 sobre `docs/postman/` ⇒ **T-406**). **Ninguna decisión de
> seguridad queda pendiente de aprobación humana.**

**SA-L01 — Semántica exacta de `IndexOf` sobre cadenas en el legacy**
El código hace `if &ipcliente.IndexOf(&PueIPRed) = 0 //IP NO HABILITADA`. No está documentado si
`IndexOf` devuelve la posición **0-based** (0 = empieza por el prefijo, −1 = no encontrado) o la
posición **1-based** (0 = no encontrado). Las dos lecturas dan comportamientos opuestos.
- **Impacto**: define si el chequeo de IP **deniega** o **permite**.
- **Pregunta**: *«En el runtime GeneXus 18 de `ATPPuestos`, qué valor devuelve
  `IndexOf(<ip>, <prefijo>)` cuando la IP no empieza con el prefijo, y qué devuelve cuando el
  prefijo está vacío (11 de los 14 `PUEPPRIM` tienen `PueIPRed` vacío o `NULL`)?»*
- **Resolución provisional**: se implementa la **semántica intencional** del legacy (el prefijo debe
 _matchear_ la IP) y un prefijo vacío **no restringe** el acceso. Ver `plan.md` §2 (**D-LOG-10**) y §10 (R-3).

**SA-L02 — Alcance real de `IsAuthorized`**
`IsAuthorized` devuelve siempre `True` en la KB actual (lógica comentada) y `ListPrograms` no tiene
callers registrados.
- **Pregunta**: *«¿La autorización por programa debe quedar activa en el nuevo sistema (con qué
  tabla o regla de origen), o se replica el comportamiento actual —todos los programas visibles?
  ¿Hay una fuente de autorización fuera de esta KB?»*
- **Resolución provisional**: se replica el comportamiento actual (todos los programas) pero la
  estructura queda lista para filtrar.

**SA-L03 — `UsuHash`: ¿debe el login aceptarlo?**
26 de 552 usuarios tienen `UsuHash` (`Pbkdf2-SHA256$   10000$<salt>$<hash>`, con **3 espacios
literales** en el separador) y 2 **sólo** tienen `UsuHash`. Ningún objeto de la KB los consulta.
- **Pregunta**: *«¿El login nuevo debe validar contra `UsuHash` (PBKDF2) cuando `UsuClave` está
  vacía, o se mantiene la paridad legacy (código 2) y esas cuentas siguen deriva al Sistema
  Integrado?»*
- **Resolución provisional**: paridad legacy (código 2). Ver R-LOG-005.

**SA-L04 — Padding a 10 caracteres del identificador**
La evidencia de la BD (549 de 552 `UsuId` decodifican a 10 caracteres; `PERSONAS.PerCodUsu` es
`char(10)` en las 183 filas) indica que el legacy rellena con espacios. La lectura literal del
código `Seg_Encripta` (que itera 10 posiciones) produce una salida **más corta** si la entrada es
más corta, lo que haría fallar la búsqueda en `SCRTY` salvo que el runtime GeneXus rellene las
variables `CHARACTER(n)`.
- **Pregunta**: *«En el legacy, ¿el usuario debe tipear su identificador con los espacios de
  relleno hasta 10 caracteres para poder ingresar, o el runtime rellena automáticamente la variable
  `CHARACTER(10)`?»*
- **Resolución provisional**: se rellena automáticamente (R-LOG-001). Es la única interpretación que
  deja el sistema usable y coincide con los datos.

**SA-L05 — Origen de `Context.Usuario` en la rama de historia**
En la rama "fue responsable" el legacy guarda `&Context.Usuario` **sin** `ToUpper()`, a diferencia
de la rama principal.
- **Pregunta**: *«Debe conservarse la diferencia (minúsculas permitidas en ese camino) o se
  normaliza siempre a mayúsculas?»*
- **Resolución provisional**: se normaliza siempre a mayúsculas (evita dos representaciones del
  mismo usuario en la sesión). Desvío menor y documentado.

**SA-L06 — `PueIPRed` con prefijo de red real**
Sólo 3 de los 14 puestos tienen prefijo configurado (`10.13.26`, `100.100.100`, `100.100.100`) y
`10.13.26` no parece una máscara de red sino un prefijo de texto.
- **Pregunta**: *«¿`PUEPPRIM.PueIPRed` es un prefijo de texto (comparación `startsWith`) o una
  máscara CIDR (comparación por red)?»*
- **Resolución provisional**: comparación por **prefijo de texto** (`startsWith`), que es lo que
  sugiere el `IndexOf` del legacy.

**SA-L07 — Duración y alcance de la cookie `SUCURSAL`**
El legacy no declara atributos de cookie (persistencia, `path`, `secure`, `httpOnly`); el
`SetCookie` de GeneXus usa los valores por defecto de la plataforma.
- **Pregunta**: *«¿La cookie `SUCURSAL` debe ser de sesión (sin expiración) o persistente? ¿Debe
  llevar `SameSite`/`HttpOnly`/`Secure`?»*
- **Resolución provisional**: cookie **de sesión** (`Max-Age` ausente), `HttpOnly`, `SameSite=Lax`,
  `Secure` según el perfil.

### 6.1 Supuestos nuevos de la revisión v2 (2026-10-22)

> Se registran aquí con el **mismo identificador** que usan en `plan.md` §6 (`SA-LOG-15…018`), para
> que la correspondencia sea bidireccional y no haya dos numeraciones distintas.

**SA-LOG-15 — Fórmula de los atributos calculados `CPCPerNyA` / `CPCResNyA`**
`CPCPerNyA` y `CPCResNyA` **no existen** como columnas en `Rentas` (§3.1.1) ⇒ son **atributos
calculados** de la KB. El MCP **no** expone su fórmula (los objetos `Attribute` no tienen `parts`).
- **Pregunta exacta**: *«¿Qué fórmula tienen en la KB los atributos calculados `CPCPerNyA` y
  `CPCResNyA` — un `join` a `PERSONAS.PerNomyApe` por `CPCPerId`/`CPCResId`, o algún otro origen
  (por ejemplo un valor congelado en otra estructura del legacy)?»*
- **Hipótesis de trabajo**: `PERSONAS.PerNomyApe` (única columna de nombre de persona de la base,
  `char(40)`, 0 nulos en 183 filas).
- **Criterio de verificación**: (a) IT que, para una persona con **dos** comisiones vigentes,
  comprueba que el orden reproduce "mayor nombre y apellido"; (b) IT que resuelve un
  `CPCResId` real y comprueba que el nombre corresponde a la persona de `PERSONAS`; (c) si el
  legacy responde, contraste con su salida.
- **Tarea**: **T-008**. **Impacto**: no bloquea la escritura de las entidades (no se mapean esas
  columnas) pero sí el **criterio de desempate determinista** de **R-LOG-036** y el valor de
  `Context.ResponsableNombre` (**R-LOG-035**).

**SA-LOG-16 — Política de CSRF y esquema de autenticación de `/api/**`**
La colección Postman no declara `auth` y las escrituras de `/api/**` hoy **no son ejecutables**
(H3/H6). Hay **tres** alternativas con consecuencias distintas sobre G-POSTMAN, el módulo 02 y el
modelo de seguridad del legacy.
- **Pregunta exacta**: *«¿Cuál es la política de CSRF y de autenticación de `/api/**` para este
  módulo: (a) API *stateless* sin CSRF con la autenticación a definir, (b) CSRF sólo en las rutas
  MVC y la API con token propio, o (c) CSRF en `/api/**` documentando el flujo con token en la
  colección Postman?»*
- **RESUELTO (2026-10-22)**: el responsable del proyecto **aprobó la alternativa (a)** ⇒ **D-LOG-17
  (a)**: `/api/**` es *stateless* y **sin CSRF**; el CSRF se exige **sólo** en las rutas MVC con
  sesión; la API queda `permitAll` (D-LOG-19(a)) con la deuda SA-LOG-14 asumida. Alternativas y
  análisis: `plan.md` §4.5.
- **Criterio de verificación**: la política por familia de rutas está **explícita** en la
  configuración (R-LOG-041); un `POST /api/v1/**` sin token **no** es rechazado por CSRF
  (R-LOG-039); la colección declara su `auth` de forma explícita y es ejecutable sin credenciales
  (R-LOG-040).
- **Tareas**: **T-126**, **T-127**, **T-312**, **T-313**, **T-404**, **T-136**.

**SA-LOG-17 — Mecanismo de materialización anticipada del token CSRF**
H4 tiene tres arreglos posibles con alcances distintos sobre el login por formulario, la cookie
`SUCURSAL`, el HTML/ARIA de las vistas y las pruebas del módulo 02.
- **Pregunta exacta**: *«¿Cuál es el mecanismo de materialización del token CSRF: materialización
  anticipada en el `SecurityFilterChain` conservando `HttpSessionCsrfTokenRepository`, migración a
  `CookieCsrfTokenRepository` (sin sesión), o `XorCsrfTokenRequestAttributeHandler` con
  `setRequestAttribute`?»*
- **RESUELTO (2026-10-22)**: el responsable del proyecto **aprobó la alternativa (a)** ⇒ **D-LOG-18
  (a)**: se conserva `HttpSessionCsrfTokenRepository` y el token se materializa temprano (filtro
  propio que fuerza `csrfToken.getToken()` antes del render, o `setRequestAttribute` con
  `CsrfTokenRequestAttributeHandler`). **No** se migra a `CookieCsrfTokenRepository`; **no** cambia el
  contrato del formulario, ni la cookie `SUCURSAL`, ni el HTML/ARIA de las vistas. Alternativas y
  análisis: `plan.md` §4.6.
- **Criterio de verificación**: toda pantalla HTML servida sin sesión previa devuelve **200** con su
  formulario y su `_csrf`, sin trazas ERROR (R-LOG-042, R-LOG-043); el repositorio de token sigue
  siendo `HttpSessionCsrfTokenRepository` y el HTML renderizado es idéntico al previo; sin
  regresión en `ProductoMvcCsrfTest` ni en los 12 rótulos del módulo 02.
- **Tareas**: **T-128**, **T-311**, **T-312**.

**SA-LOG-18 — Alcance de G4 sobre `docs/postman/`**
G4 dice «archivos de configuración < 500 líneas»; la colección Postman tiene **1990** líneas y vive
en `docs/`, que es territorio de `verificador-qa`/coordinación.
- **Pregunta exacta**: *«¿G4 aplica a los artefactos de `docs/postman/`? Si aplica: ¿se divide la
  colección en un archivo por módulo con un índice en `README.md`, o se registra una excepción
  explícita para los artefactos de documentación generated?»*
- **Resolución provisional**: **split por módulo** (un archivo por módulo + índice), porque además
  de cumplir G4 mejora la mantenibilidad de G-POSTMAN y no exige decisiones de seguridad.
- **Criterio de verificación**: ningún archivo de `docs/postman/` supera las 500 líneas; el índice
  lista todos los archivos; la cobertura de requests se conserva.
- **Tarea**: **T-406** (coordinación/verificador-qa; este módulo sólo redacta el requisito).

---

## 7. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| R-1 | El mecanismo de credenciales **no es cifrado**: es texto plano con transformación pública y reversible. | Alto (seguridad) | Se replica por paridad funcional; se documenta como decisión explícita (`plan.md` §2, **D-LOG-03**) y como riesgo asumido. No se agrega PBKDF2 en este módulo (sería un desvío incompatible con la BD actual). |
| R-2 | El padding a 10 caracteres puede diferir del comportamiento real del runtime GeneXus. | Medio | SA-L04; validado contra 549/552 filas de la BD real. |
| R-3 | La semántica de `IndexOf` puede invertir el resultado del chequeo de IP. | Medio | SA-L01; la implementación queda encapsulada en un único método con test dedicado. |
| R-4 | El ordenamiento de `Seg_BusComi`/`Seg_BusPer` exige el **nombre y apellido**. *(Nota revisada en la revisión v2: la afirmación previa —«`CPCPerNyA` es columna de `COMISIO1`»— era **incorrecta**.)* | Bajo | `PerNomyApe` **sí** es columna de `PERSONAS`; `CPCPerNyA` **no** es columna de `COMISIO1` (0 coincidencias en toda la BD) ⇒ el orden se reproduce con **JOIN a `PERSONAS`** (R-LOG-035, R-LOG-036, D-LOG-20). La fórmula exacta del atributo calculado queda en **SA-LOG-15** (T-008). |
| R-5 | El canal REST replica una inconsistencia del legacy (código 0 sin sucursal). | Bajo | Se replica a propósito (R-LOG-029) y queda documentada. |
| R-6 | El estado de `IsAuthorized` no es verificable contra un sistema en producción. | Bajo | SA-L02; la estructura de filtrado queda lista. |
| R-7 | La BD no tiene `IDENTITY` ni `COLUMN_DEFAULT`; el login no escribe, pero cualquier alta futura debe respetar eso. | Bajo | Sin escrituras en este módulo; la regla queda en `plan.md` §5.5. |
| R-8 | La tabla raíz de comisiones se llama **`COMISION`** y no `COMISIO`: mapear el nombre equivocado rompe **todas** las consultas de comisión del login. | **Alto** | **Cerrado** en la revisión v2 (§3.1.1, R-LOG-033) con evidencia de `INFORMATION_SCHEMA`; la entidad se escribe con `@Table(name="COMISION")` y un IT lee una fila real. |
| R-9 | `CPCPerNyA` y `CPCResNyA` **no son columnas**: si se mapean, Hibernate falla en el arranque o en la lectura; si se omiten sin JOIN, el nombre del responsable queda vacío y el orden de `Seg_BusComi` deja de ser determinista. | Medio | R-LOG-035 + R-LOG-036: JOIN a `PERSONAS` explícito; SA-LOG-15 con criterio de verificación y tarea **T-008**. |
| R-10 | La política CSRF y de autenticación de `/api/**` **no está decidida**: mientras tanto, las escrituras de la colección Postman no son ejecutables (H3/H6). | **Alto** (G-POSTMAN) | **RESUELTO (2026-10-22)**: el responsable del proyecto aprobó **D-LOG-17(a)** y **D-LOG-19(a)** ⇒ `/api/**` *stateless* y **sin CSRF**, `permitAll`; la colección queda ejecutable sin tokens ni credenciales. Nueva mitigación por la ausencia de CSRF en la API: **R-LOG-041** (nota de mitigación) + tarea **T-136**. Deuda de `permitAll`: **SA-LOG-14** + **T-135**. |
| R-11 | El token CSRF se materializa tarde ⇒ cualquier cliente **sin sesión previa** recibe la página de error en lugar de la pantalla (H4). | Medio (disponibilidad + ruido ERROR en el log) | **RESUELTO (2026-10-22)**: aprobada **D-LOG-18(a)** ⇒ materialización anticipada conservando `HttpSessionCsrfTokenRepository`, sin tocar el contrato del formulario, la cookie `SUCURSAL` ni el HTML del módulo 02. R-LOG-042 + R-LOG-043; tareas **T-128**, **T-311**. |
| R-12 | La colección Postman monolítica (1990 líneas) viola G4 y su no ejecutabilidad impide usarla como prueba de aceptación (G-POSTMAN). | Medio | R-LOG-044 + R-LOG-040: split por módulo con índice (**T-406**) y bloque `auth` (**T-404**), más un recorrido de la colección (**T-313**). |
| R-13 | Hibernate detecta SQL Server 10.0 (aviso `HHH000511`) con un driver que en realidad talks con SQL Server 2019: riesgo latente de compatibilidad de Hibernate 6.6 con esa configuración. | Bajo (latente) | R-LOG-046: dialecto explícito o parámetros de conexión corregidos, **verificado contra la BD real** sin cambio de comportamiento (**T-129**, **T-314**). |
| R-14 | `UsuIngreso` es nullable y hay **1** usuario de 552 con valor nulo: una comparación ingenua en Java (unboxing a `int`) lanzaría `NullPointerException` en el login. | Bajo | R-LOG-037: `Integer` con comparación por igualdad; IT con ese usuario real. |
| R-15 | Con **D-LOG-17(a)** `/api/**` **no** está protegida contra CSRF: un sitio de terceros podría disparar una escritura contra la API desde el navegador de un operador. | Medio | **Mitigación acordada y verificable** (R-LOG-041): la API es *stateless* y **no** toma autoridad de la cookie de sesión (no hay credencial ambiental que montar); `JSESSIONID` y `SUCURSAL` con `SameSite=Lax`; la credencial futura de la API viajará en la cabecera `Authorization`, nunca en cookie. Verificación: **T-136**. Deuda de `permitAll`: SA-LOG-14 + **T-135**. |
