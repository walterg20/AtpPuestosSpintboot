# Spec — 11 Validación CUIT (Puestos Camineros)

> **Estado**: `IMPLEMENTADO (2026-11-04)` — Gate 2 APROBADO CON OBSERVACIONES (unit verde; IT bloqueado por DNS).
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye **criterio de verificación**.
> **IDs**: prefijo `R-CUIT-<NNN>`.
> **Fuente legacy**: KB `ATPPuestos` (GeneXus 18.0.13), alias `atppuestos` — **solo lectura**.
> - `Procedure Nuc_ValidaCuit` (`Root Module/Nucleo/Nuc_ValidaCuit`) — leído verbatim el 2026-11-04.
> - `WebPanel PPActaDetalle` (`Root Module/F2776/PPActaDetalle`) — validaciones verbatim.
> - `WebPanel ActaCargarDetalle` (`Root Module/F2776/ActaCargarDetalle`) — verificado: **no** usa `Nuc_ValidaCuit`.
> - `Transaction Contribu` (`CONTRIBU` en `Rentas`) — sin columna de dígito verificador (G-DATA 2026-11-04).
> **Fuente de datos**: BD `Rentas` @ `devmssqlatp.ecom.com.ar` — **G-DATA 2026-11-04, sólo `SELECT`**.
> **Módulo consumidor**: `09-formularios-acta` (R-ACTA-028), que hoy valida sólo existencia + 11 dígitos
> (decisión D-9 de ese módulo). Este módulo **completa** la validación del dígito verificador.
> **Alcance de escritura de esta spec**: `openspec/features/11-validacion-cuit/**`.
>   En la fase de implementación: `atp-nucleo-compartido/**` (servicio compartido) y
>   `atp-puestos-camineros/**` (consumo en `ActaService.validar`). **Sin DDL, sin migraciones** (G5).

---

## 1. Propósito

Implementar **completo** el procedure legacy `Nuc_ValidaCuit.Udp(...)` (validación del dígito
verificador del CUIT) para alcanzar paridad funcional con GeneXus, reemplazando la validación
parcial del módulo 09 (que sólo verifica existencia + 11 dígitos y deja pasar CUITs sintéticos
de prueba).

El resultado es un **servicio Java compartido y puro** (sin acceso a BD) que devuelve un
`boolean`, con el **algoritmo exacto** del legacy (módulo 11 estándar AFIP + validación de
prefijo de tipo de persona), y que se consume desde `ActaService.validar(...)` (módulo 09) y
desde cualquier otro módulo futuro que valide CUIT.

## 2. Alcance

### 2.1 IN

| # | Elemento |
|---|---|
| 1 | Servicio Java `ValidadorCuit` (en `atp-nucleo-compartido`) con método `esValido(Long cuit)` / `esValido(String cuit)` que replica **carácter por carácter** el algoritmo de `Nuc_ValidaCuit` (R-CUIT-001 … R-CUIT-010). |
| 2 | Validación de formato: exactamente 11 dígitos, sólo numéricos, sin espacios, sin guiones (R-CUIT-001, R-CUIT-005). |
| 3 | Validación del dígito verificador con módulo 11 estándar AFIP (pesos `5432765432`) y prefijo válido (R-CUIT-002, R-CUIT-007). |
| 4 | Integración en `ActaService.validar(...)` del módulo 09: reemplazar la validación parcial (D-9) por la completa (R-CUIT-003, R-CUIT-006). |
| 5 | Mantener los mensajes verbatim del legacy (`Debe ingresar CUIT del Vendedor/Destinatario/Transportista`) en el orden Vendedor → Destinatario → Transportista (R-CUIT-003). |
| 6 | Derivación por CUIT en `CONTRIBU` (razón social, domicilio, localidad) cuando el CUIT es válido pero no existe, dejando vacío (R-CUIT-004). |
| 7 | Tests unitarios del algoritmo con los casos límite del legacy (R-CUIT-010). |

### 2.2 OUT — explícitamente excluido

| Excluido | Motivo | Destino |
|---|---|---|
| Validación contra AFIP en tiempo real (WS de constancia de inscripción) | No existe en el legacy (el legacy sólo valida algorítmicamente) | Backlog |
| Normalización de CUIT con guiones/espacios | El legacy recibe `NUMERIC(11)` y no normaliza; la validación de formato es responsabilidad del llamador (R-CUIT-005) | Backlog |
| Endpoints REST para validación de CUIT | No hay en el legacy; es un servicio interno | Backlog |
| Vistas Thymeleaf nuevas | No hay pantalla de validación de CUIT en el legacy | Backlog |
| Carga masiva / batch de CUITs | Fuera del encargo | Backlog |

---

## 3. Modelo legacy verificado

### 3.1 `Procedure Nuc_ValidaCuit` — firma y variables

**Ruta**: `Root Module/Nucleo/Nuc_ValidaCuit`.
**Firma (verbatim, `Rules`)**:
```
Parm(in:&CUIT, out:&Valido);
```

**Variables** (según `genexus_read` del 2026-11-04):

| Variable | Tipo | Longitud | Decimales | Rol |
|---|---|---|---|---|
| `&CUIT` | `NUMERIC` | 11 | 0 | entrada (CUIT a validar) |
| `&Valido` | `Boolean` | 1 | — | salida (`True`/`False`) |
| `&Docu` | `NUMERIC` | 10 | 0 | primeros 10 dígitos del CUIT |
| `&Acu` | `NUMERIC` | 10 | 2 | acumulador de la suma ponderada |
| `&I` | `NUMERIC` | 10 | 2 | índice del bucle (10 → 1) |
| `&Resul` | `NUMERIC` | 10 | 2 | cociente entero `Acu / 11` |
| `&Resto` | `NUMERIC` | 10 | 0 | resto `Acu - Resul*11` |
| `&UltDig` | `NUMERIC` | 2 | 0 | dígito verificador calculado |
| `&DosPriDig` | `NUMERIC` | 2 | 0 | primeros 2 dígitos del CUIT |
| `&Ult` | `NUMERIC` | 2 | 0 | último dígito del CUIT (el verificador informado) |

**No toca tablas**: es un cálculo puro (no hay `For Each`, `New`, `Delete`).

### 3.2 `Procedure Nuc_ValidaCuit` — Source verbatim

```text
&Docu=0

&Acu = 0
&UltDig=0

&Docu = val(substr(str(&Cuit,11,0),1,10))


&I = 10
Do While  &i>=1
    &Acu = &Acu + val(substr(str(&Docu,10,0),&i, 1))*val(substr('5432765432',&i,1))
    &I -= 1
EndDo

&resul=Int( &Acu/11 )

&Resto = &Acu - &Resul*11

&UltDig = 11 - &resto
if &UltDig = 11
    &UltDig  = 0
endif
//
//
&Valido = False
//
//
&DosPriDig= val(substr(str(&Docu,10,0),1,2))
//
&Ult = &Cuit - Int( &Cuit/10 )*10
If &UltDig = &Ult .And. 
   (&DosPriDig = 20 .Or. &DosPriDig = 23 .Or. &DosPriDig = 24 .Or. &DosPriDig = 27 .Or. 
    &DosPriDig = 30 .Or. &DosPriDig = 33 .Or. &DosPriDig = 34)
  &Valido = True
Endif
```

### 3.3 Algoritmo observable (traducción a pasos)

1. **Formatear** el CUIT a 11 caracteres con ceros a la izquierda: `str(&Cuit, 11, 0)`.
2. **Tomar los primeros 10 dígitos** como `&Docu` (`val(substr(..., 1, 10))`).
3. **Suma ponderada**: para `i = 10` hasta `1`:
   `Acu += dígito_i(Docu) × peso_i`, donde `peso_i = substr('5432765432', i, 1)`.
   Los pesos de izquierda a derecha (i=10→1) son `5,4,3,2,7,6,5,4,3,2` — es decir, el
   **módulo 11 estándar de AFIP** (no es una variante).
4. **Cociente entero**: `Resul = Int(Acu / 11)`.
5. **Resto**: `Resto = Acu - Resul * 11`.
6. **Dígito verificador calculado**: `UltDig = 11 - Resto`; si `UltDig = 11` ⇒ `UltDig = 0`.
   (Caso `Resto = 1` ⇒ `UltDig = 10`, que **nunca** coincide con un dígito 0–9, por lo que el
   CUIT se considera inválido — comportamiento idéntico al estándar AFIP, donde dv=10 invalida.)
7. **Dígito verificador informado**: `Ult = Cuit mod 10` (último dígito del CUIT).
8. **Prefijo**: `DosPriDig = primeros 2 dígitos de Docu` (primeros 2 del CUIT).
9. **Válido** si y sólo si:
   - `UltDig == Ult` **Y**
   - `DosPriDig ∈ {20, 23, 24, 27, 30, 33, 34}`.

> **No hay caso especial para `UltDig = 10`** (no se convierte a 9): el legacy deja 10 y la
> comparación con `Ult` (0–9) falla siempre. Se replica tal cual.

### 3.4 `WebPanel PPActaDetalle` — validaciones verbatim (orden)

En `Sub 'Validar'` (transcrito verbatim en `spec.md` §3.2 de `09-formularios-acta`):

```text
Case not Nuc_ValidaCuit.Udp(&F2276.F276CuitVe)
    &Error = 'S'
    msg("Debe ingresar CUIT del Vendedor")
Case not Nuc_ValidaCuit.Udp(&F2276.F276CuitDe)
    &Error = 'S'
    msg("Debe ingresar CUIT del Destinatario")
Case not Nuc_ValidaCuit.Udp(&F2276.F276CuitTr)
    &Error = 'S'
    msg("Debe ingresar CUIT del Transportista")
```

El `Case` se evalúa **en ese orden** (Vendedor → Destinatario → Transportista) y devuelve el
**primer** mensaje de error. No hay mensaje "CUIT inválido" separado: el llamador usa los
mensajes anteriores.

### 3.5 `WebPanel ActaCargarDetalle` — no usa `Nuc_ValidaCuit`

Verificado el 2026-11-04: el `Events` de `ActaCargarDetalle` no contiene ninguna referencia a
`Nuc_ValidaCuit`. **No** se replica validación de CUIT en la carga manual de detalle.

### 3.6 `Transaction Contribu` (CONTRIBU) — sin columna de dígito verificador

Verificado contra `INFORMATION_SCHEMA.COLUMNS` de `Rentas` el 2026-11-04:

- PK: `ConCuit` (`decimal(11,0)`, `NOT NULL`).
- **No existe** columna de dígito verificador ni de "CUIT válido".
- La validación es **puramente algorítmica**: no consulta la BD.

Columnas relevantes para la derivación (R-CUIT-004): `ConCuit`, `ConRazSoc` (`char(35)`),
`ConRazSoc`/`CallCodr`/`ConNMzanaR` (domicilio), `LocCodR` (localidad). Sin columna de
provincia (se deriva de `Localida`/`PROVINCI`).

### 3.7 Otros usos en la KB

Búsqueda `genexus_search_source` (pattern `Nuc_ValidaCuit`) sobre la KB el 2026-11-04:
**3 hits, todos en `PPActaDetalle` (líneas 93, 96, 99)**. No se encontraron otros usos en los
objetos con source indexado. Si en el futuro se descubre otro uso, este módulo es la fuente
única de la validación y se reutiliza.

---

## 4. Requisitos EARS

### 4.1 Validación de formato

#### R-CUIT-001 — Longitud exacta
> **EL SISTEMA DEBE** validar que el CUIT tenga **exactamente 11 dígitos**; cualquier valor con
> menos o más dígitos (incluyendo `null`/vacío) **DEBE** considerarse inválido.

* *Criterio de verificación*: test unitario con CUITs de 10, 11 y 12 dígitos que verifica
  `false`/`true`/`false` respectivamente.

#### R-CUIT-005 — Sólo numérico
> **SI** el CUIT contiene caracteres no numéricos (letras, guiones, espacios, puntos), **ENTONCES
> EL SISTEMA DEBE** considerarlo inválido y **NO DEBE** intentar normalizarlo.

* *Criterio de verificación*: test unitario con `"20-12345678-9"`, `"2012345678A"`, `" 20123456789"`
  que verifica `false` en todos los casos.

> **Nota**: el legacy recibe `NUMERIC(11)` y no normaliza. La normalización (si se desea) es
> responsabilidad del llamador, fuera de este módulo (OUT §2.2).

### 4.2 Validación del dígito verificador

#### R-CUIT-002 — Algoritmo del legacy
> **EL SISTEMA DEBE** validar el dígito verificador del CUIT aplicando **exactamente** el algoritmo
> de `Nuc_ValidaCuit` (§3.3): módulo 11 estándar AFIP con pesos `5432765432`, resto, dígito
> calculado `11 - resto` (con `11 → 0`), comparado con el último dígito del CUIT.

* *Criterio de verificación*: test unitario con un CUIT válido conocido (p. ej. `20123456786`)
  que verifica `true`, y con el mismo CUIT con el último dígito alterado que verifica `false`.

#### R-CUIT-007 — Prefijo válido
> **EL SISTEMA DEBE** rechazar el CUIT si sus **primeros 2 dígitos** no pertenecen al conjunto
> `{20, 23, 24, 27, 30, 33, 34}` (prefijos de tipo de persona AFIP), incluso si el dígito
> verificador coincide.

* *Criterio de verificación*: test unitario con CUIT `11123456789` (prefijo 11, dv correcto para
  el cuerpo) que verifica `false`; con `20123456786` (prefijo 20) que verifica `true`.

#### R-CUIT-010 — Paridad total con el legacy
> **EL SISTEMA DEBE** devolver el mismo resultado booleano que `Nuc_ValidaCuit.Udp(...)` para **todo**
> CUIT de 11 dígitos numéricos, incluyendo los casos límite: `Resto = 0` (`UltDig = 0`),
> `Resto = 1` (`UltDig = 10` ⇒ inválido), y CUITs con prefijo fuera del conjunto.

* *Criterio de verificación*: test unitario parametrizado con ≥ 20 casos (válidos e inválidos)
  transcritos del algoritmo; comparación con una implementación de referencia en Python/JS que
  replica el source verbatim.

### 4.3 Consumo en el módulo 09 (Actas)

#### R-CUIT-003 — Mensaje verbatim en PPActaDetalle
> **SI** el CUIT del Vendedor, del Destinatario o del Transportista **no** es un CUIT válido
> (R-CUIT-002), **ENTONCES EL SISTEMA DEBE** rechazar el alta con el mensaje verbatim
> correspondiente (`Debe ingresar CUIT del Vendedor`, `Debe ingresar CUIT del Destinatario`,
> `Debe ingresar CUIT del Transportista`), evaluando los tres roles **en ese orden**
> (Vendedor → Destinatario → Transportista) y devolviendo el **primer** error.

* *Criterio de verificación*: test MockMvc parametrizado (3 casos) que compara el texto renderizado
  y verifica que sólo se muestra un mensaje; test que verifica el orden cuando dos CUITs son
  inválidos (sólo aparece el del Vendedor).

#### R-CUIT-006 — Validación en alta y carga manual
> **EL SISTEMA DEBE** validar el CUIT en el formulario de alta de Actas (`PPActaDetalle`); **NO
> DEBE** validar CUIT en la carga manual de detalle (`ActaCargarDetalle`), porque ese panel no lo
> hace en el legacy (§3.5).

* *Criterio de verificación*: revisión del `ActaService.validar(...)` que confirma la llamada a
  `ValidadorCuit.esValido(...)` para los 3 CUITs del alta; revisión de `ActaCargarDetalle` (Java)
  que confirma la **ausencia** de llamada.

### 4.4 Derivación por CUIT

#### R-CUIT-004 — Derivación cuando el CUIT es válido pero no existe
> **CUANDO** el CUIT sea válido (R-CUIT-002) pero **no exista** en `CONTRIBU`, **EL SISTEMA DEBE**
> dejar vacíos la Razón Social, el Domicilio y la Localidad del rol correspondiente (paridad con
> R-ACTA-018 y con `F2276_DevRazSocial`/`F2276_BusDom`/`F2276_BusLoc`, que devuelven vacío
> `when none`), **sin** rechazar el alta por ese motivo.

* *Criterio de verificación*: test unitario del servicio de derivación con un CUIT válido inexistente
  que verifica que los campos quedan vacíos y que no se lanza excepción; IT contra `Rentas` con
  un CUIT válido inexistente.

### 4.5 Reutilización y pureza

#### R-CUIT-008 — Servicio puro (sin BD)
> **EL SISTEMA DEBE** implementar la validación como un **servicio puro** (sin acceso a repositorio
> ni a BD), determinista y sin efectos secundarios, reutilizable por cualquier módulo.

* *Criterio de verificación*: revisión del código que confirma que `ValidadorCuit` no inyecta
  repositorios ni `EntityManager`; test que invoca el método 1000 veces con el mismo input y
  verifica el mismo resultado.

#### R-CUIT-009 — Reutilización compartida
> **EL SISTEMA DEBE** ubicar `ValidadorCuit` en `atp-nucleo-compartido` (paquete
> `ar.com.atp.puestos.nucleo.service` o `shared`) para que cualquier módulo (Actas, Turnos,
> Contribuyentes) lo consuma sin duplicar lógica.

* *Criterio de verificación*: revisión de la ubicación del archivo y de los imports en
  `ActaService` que confirman que consume el del núcleo (no una copia local).

---

## 5. Decisiones

Ver `plan.md` §5. Resumen operativo:

| # | Decisión | Efecto en la implementación |
|---|---|---|
| D-CUIT-01 | El algoritmo se replica **verbatim** (incluyendo el caso `UltDig = 10` ⇒ inválido y la validación del prefijo). | `ValidadorCuit.esValido(...)` con los pesos y el conjunto de prefijos exactos. |
| D-CUIT-02 | El servicio es **puro** (sin BD) y vive en `atp-nucleo-compartido`. | `ar.com.atp.puestos.nucleo.service.ValidadorCuit`. |
| D-CUIT-03 | Reemplaza la validación parcial (D-9) del módulo 09: `ActaService.validar(...)` pasa a usar `ValidadorCuit`. | Editar `ActaService` (sin cambiar mensajes ni orden). |
| D-CUIT-04 | No se exponen endpoints REST ni vistas nuevas. | Sin cambios en Postman ni plantillas. |
| D-CUIT-05 | `ActaCargarDetalle` **no** valida CUIT (paridad con el legacy). | Sin cambios en ese panel. |
| D-CUIT-06 | La derivación por CUIT (R-CUIT-004) deja vacío si no existe; no rechaza. | Reutiliza `F2276_DevRazSocial`/`BusDom`/`BusLoc` (ya implementados en 09). |
| D-CUIT-07 | El método acepta `Long` y `String` (este último valida formato estricto). | Sobrecarga `esValido(Long)` y `esValido(String)`. |

---

## 6. Supuestos abiertos

| ID | Supuesto / pregunta | Impacto | Estado | Bloquea a |
|---|---|---|---|---|
| SA-CUIT-01 | La búsqueda `genexus_search_source` cubrió sólo los **13 objetos con source indexado** de la KB (de 1615 totales). ¿Podría haber otros usos de `Nuc_ValidaCuit` en objetos no indexados (p. ej. transacciones con reglas que invoquen procedures)? | Medio | Abierto | T-CUIT-005 (auditoría de usos) |
| SA-CUIT-02 | ¿El CUIT se almacena siempre como `NUMERIC(11,0)` en todos los módulos futuros, o habrá variantes `char(11)`/`varchar`? Afecta la firma del método (`Long` vs `String`). | Bajo | Abierto | D-CUIT-07 |
| SA-CUIT-03 | ¿El conjunto de prefijos válidos `{20,23,24,27,30,33,34}` es estable, o hay CUITs de personas jurídicas con prefijos como `30` ya incluidos y otros (p. ej. `27` para mujeres) que el legacy ya contempla? El source es la fuente de verdad; no se amplía. | Bajo | Cerrado (source verbatim) | — |
| SA-CUIT-04 | ¿El módulo 09 (D-9) debe seguir aceptando CUITs sintéticos de test (los que hoy pasan por la validación parcial)? Con la validación completa, los CUITs sintéticos con dv incorrecto serán rechazados. | Alto | Abierto | T-CUIT-007 (actualización de tests del módulo 09) |

---

## 7. Alcance excluido

- Validación contra AFIP en tiempo real (WS de constancia de inscripción).
- Normalización de CUIT con guiones/espacios (OUT §2.2).
- Endpoints REST y vistas Thymeleaf nuevas.
- Carga masiva / batch de CUITs.
- Modificación de la KB legacy (G1).
- DDL / migraciones (G5).

---

## 8. Referencias

- `openspec/features/09-formularios-acta/spec.md` §3.2 (PPActaDetalle, validaciones verbatim), §3.3 (rules de F2276).
- `openspec/features/09-formularios-acta/plan.md` §2.5 (entidad `Contribuyente`), §3.4 (`ActaService`).
- KB: `Procedure Nuc_ValidaCuit` (leído el 2026-11-04, verbatim en §3.2).
- BD: `INFORMATION_SCHEMA.COLUMNS` de `CONTRIBU` (verificado el 2026-11-04).
