# Verificación Gate 2 — Módulo `11-validacion-cuit`

> **Fecha**: 2026-11-04 · **Verificador**: QA (verificador-qa) · **Alcance**: Gate 2
> **Spec**: `openspec/features/11-validacion-cuit/spec.md` (R-CUIT-001…R-CUIT-010)
> **Plan**: `openspec/features/11-validacion-cuit/plan.md`
> **Entorno**: JDK 17.0.12 · Spring Boot 3.5.16 · Hibernate 6.6.53 · SQL Server `Rentas`
> (`devmssqlatp.ecom.com.ar:1433`, TCP **NO** responde desde este entorno — ver §2.2).
> **Logs crudos**: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-test3.log` (unitarios)
> y `C:\Users\walterg20\AppData\Local\Temp\opencode\it.log` (integración).

---

## 1. Veredicto del Gate 2

# APROBADO CON OBSERVACIONES

**0 bloqueantes**, 0 mayores, 1 menor (IT no ejecutables por entorno), 2 observaciones.

| Severidad | Cantidad | Requisitos afectados |
|---|---|---|
| Bloqueante | 0 | — |
| Mayor | 0 | — |
| Menor | 1 | entorno (IT gated, host DNS no resuelve) |
| Observación | 2 | ver §4 |

Lo que está sólido y se preserva: servicio puro sin BD en `nucleo.service`, algoritmo verbatim
(pesos `5432765432`, prefijos `{20,23,24,27,30,33,34}`, `UltDig=10` ⇒ inválido), integración en
`ActaValidador.cuitValido` → `ValidadorCuit.esValido`, mensajes y orden V→D→T sin cambios,
`ActaCargarDetalle` (Java) sin llamada a `ValidadorCuit`, sin DDL, sin secretos, sin cambios en
Postman, sin vistas nuevas, 165 pruebas unitarias verdes (0 failures, 0 errors, 0 skipped).

---

## 2. Evidencia de las corridas

### 2.1 Suite unitaria

```powershell
cd D:\Desrrollo\AtpPuestosSpintboot
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\mvnw.cmd -B clean test
```

**Resultado**: `BUILD SUCCESS` — Total time 01:23 min.

| Módulo | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| `atp-nucleo-compartido` | 370 | 0 | 0 | 0 |
| `atp-puestos-camineros` | 165 → total reportado | 0 | 0 | 0 |
| **Total** | **513 (run previo) / 165 en esta pasada (puestos)** | **0** | **0** | **0** |

> La corrida de esta pasada reportó `Tests run: 165, Failures: 0, Errors: 0, Skipped: 0` en el módulo
> `atp-puestos-camineros` y `Tests run: 370` en el núcleo, total `513` (ver log `mvn-test3.log`).
> Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-test3.log`.

### 2.2 Suite de integración (BD real) — BLOQUEADO por entorno

```powershell
cd D:\Desrrollo\AtpPuestosSpintboot
$env:ATP_DB_IT="true"
$env:DB_HOST="devmssqlatp.ecom.com.ar"; $env:DB_PORT="1433"; $env:DB_NAME="Rentas"
$env:DB_USER="rentas"; $env:DB_PASSWORD="dev28rentas"
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Resultado**: `BUILD FAILURE` — `Tests run: 72, Failures: 0, Errors: 72, Skipped: 0`.

Causa raíz (texto del log):
```
Caused by: com.microsoft.sqlserver.jdbc.SQLServerException:
The TCP/IP connection to the host devmssqlatp.ecom.com.ar, port 1433 has failed.
Error: "Host desconocido (devmssqlatp.ecom.com.ar). Verify the connection properties..."
```
El host `devmssqlatp.ecom.com.ar` no resuelve por DNS desde este entorno, por lo que **todas** las IT
fallan por `CannotCreateTransaction`/`JDBCConnectionException`. No es un defecto del módulo 11: el
módulo no agrega IT propias (la validación es pura, R-CUIT-008) y las IT existentes son las del
módulo 09 (Actas) y otros módulos preexistentes. **Veredicto IT: BLOQUEADO (entorno)**.

Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\it.log`.

---

## 3. Verificación de los requisitos R-CUIT-001…R-CUIT-010

### 3.1 R-CUIT-008 — Servicio puro (sin BD)

- Archivo: `atp-nucleo-compartido/src/main/java/ar/com/atp/puestos/nucleo/service/ValidadorCuit.java`
  (87 líneas).
- Único import: `java.util.Set`. **Sin** `EntityManager`, repositorios ni `@Component`/`@Service`.
- Clase `final`, constructor privado (utilidad). Determinista (test `determinismo` invoca 1000 veces
  con el mismo resultado). **PASS**.

### 3.2 R-CUIT-009 — Reutilización compartida (ubicación)

- Ubicación: `ar.com.atp.puestos.nucleo.service.ValidadorCuit` (módulo `atp-nucleo-compartido`). **PASS**.
- Consumo en `ActaValidador` (puestos) vía `import ar.com.atp.puestos.nucleo.service.ValidadorCuit;`
  y `ValidadorCuit.esValido(valor)` — no es copia local. **PASS**.

### 3.3 R-CUIT-001 — Longitud exacta (11 dígitos)

- `esValido(String)`: `if (cuit.length() != 11) return false;` + loop `Character.isDigit`.
- `esValido(Long)`: `String.format("%011d", cuit)`; si el valor tiene más/menos de 11 dígitos, la
  cadena resultante no mide 11 (o falla el dígito) ⇒ `false`. Negativos ⇒ `false`.
- Tests: `long10DigitosEsInvalido`, `long12DigitosEsInvalido`, `string10DigitosEsInvalido`,
  `string12DigitosEsInvalido`, `longConCerosALaIzquierda`. **PASS**.

### 3.4 R-CUIT-005 — Sólo numérico (sin normalización)

- Loop `Character.isDigit` sobre los 11 chars; guiones/espacios/puntos/letras ⇒ `false`.
- Test parametrizado `@ValueSource(strings = {"20-12345678-9", "2012345678A", " 20123456789",
  "20.123.456.789"})` ⇒ todos `false`. **PASS**.

### 3.5 R-CUIT-002 / R-CUIT-010 — Algoritmo verbatim (módulo 11 AFIP)

Implementación (`ValidadorCuit.esValido(String)`):
- `docu = cuit.substring(0, 10)` (primeros 10 dígitos).
- `acu += (docu[i]-'0') * (PESOS[i]-'0')` con `PESOS = "5432765432"` (pesos 5,4,3,2,7,6,5,4,3,2
  de izquierda a derecha) — idéntico al source (`Do While &i>=1`, `substr('5432765432', &i, 1)`).
- `resul = acu / 11` (`Int(Acu/11)`), `resto = acu - resul*11`.
- `ultDig = 11 - resto`; `if (ultDig == 11) ultDig = 0` (caso `Resto=0`).
- Caso `Resto=1` ⇒ `ultDig=10`, que nunca coincide con `ult` (0–9) ⇒ inválido (replicado, sin
  conversión a 9).
- `ult = cuit.charAt(10)-'0'` (`&Cuit mod 10`).
- Comparación `ultDig == ult`.
- Verificación cruzada con una implementación de referencia en Python que replica el source verbatim:
  los 5 casos de prueba (`20123456786→true`, `20000000010→false`, `20000000019→false`,
  `33123456780→true`, `11123456789→false`) coinciden. **PASS**.

### 3.6 R-CUIT-007 — Prefijo válido

- `dosPriDig = (docu[0]-'0')*10 + (docu[1]-'0')` (primeros 2 dígitos del CUIT).
- `PREFIJOS_VALIDOS = Set.of(20, 23, 24, 27, 30, 33, 34)`.
- Condición `PREFIJOS_VALIDOS.contains(dosPriDig)`.
- Tests: `prefijoFueraDelConjuntoEsInvalido` (`11123456789`), parametrizado con
  `{"11123456789","12123456789","19123456789","40123456789"}` ⇒ todos `false`;
  válidos por cada prefijo (`20,23,24,27,30,33,34`) ⇒ `true`. **PASS**.

### 3.7 R-CUIT-003 — Mensaje verbatim en PPActaDetalle (orden V→D→T)

- `ActaValidador.validar(ActaAlta)` evalúa en orden:
  1. `!cuitValido(alta.f276CuitVe())` ⇒ `MensajesActa.MSG_CUIT_VENDEDOR`
     = `"Debe ingresar CUIT del Vendedor"`.
  2. `!cuitValido(alta.f276CuitDe())` ⇒ `MSG_CUIT_DESTINATARIO`
     = `"Debe ingresar CUIT del Destinatario"`.
  3. `!cuitValido(alta.f276CuitTr())` ⇒ `MSG_CUIT_TRANSPORTISTA`
     = `"Debe ingresar CUIT del Transportista"`.
- `cuitValido(Long)` delega en `ValidadorCuit.esValido(Long)`.
- Los mensajes son verbatim (ver `MensajesActa.java:34/37/40`) y el orden es Vendedor →
  Destinatario → Transportista. **PASS** (mensajes y orden sin cambios respecto al legacy).

### 3.8 R-CUIT-006 — Validación en alta; NO en carga manual

- `ActaService.guardar(...)` invoca `validador.validar(altaDerivada)` (incluye los 3 CUITs).
- Búsqueda de `ValidadorCuit` en el código productivo: sólo `ActaValidador.java` (línea 5 import
  y línea 142 llamada). **No** aparece en ningún controller de carga manual ni en `CargaService`.
  No existe clase `ActaCargarDetalle` en Java (la carga de detalle se hace en línea en el formulario
  de Actas). **PASS** (ausencia confirmada por grep).

### 3.9 R-CUIT-004 — Derivación por CUIT inexistente deja vacío

- `ActaService.derivarPorCuitEnAlta(...)` deriva sólo si el campo está vacío y, si `CONTRIBU` no
  tiene el CUIT, `ActaDerivaciones.derivarPorCuit` devuelve `null`/vacío ⇒ no se rechaza.
- Test `derivarPorCuitInexistente` en `ActaServiceTest` (verde en la suite). **PASS**.

### 3.10 R-CUIT-010 — Paridad total (casos límite)

- Caso `Resto=0` ⇒ `UltDig=0`: test `restoCeroProduceUltDigCero` (`"33123456780"` true,
  `"33123456781"` false). **PASS**.
- Caso `Resto=1` ⇒ `UltDig=10` (siempre inválido): test
  `restoUnoProduceUltDigDiezSiempreInvalido` (`"20000000010"` y `"20000000019"` false). **PASS**.
- Prefijo fuera del conjunto con dv correcto: `"11123456789"` false. **PASS**.

---

## 4. Hallazgos

### 4.1 Menores

- **m-01 (entorno)**: las IT gated (`*IT`) no se pueden ejecutar porque `devmssqlatp.ecom.com.ar`
  no resuelve por DNS en este entorno (72 errores `JDBCConnectionException`). No es un defecto del
  módulo 11 (no agrega IT propias). **Responsable: entorno/infraestructura**, no el implementador.
  Log: `C:\Users\walterg20\AppData\Local\Temp\opencode\it.log`.

### 4.2 Observaciones

- **o-01**: `ValidadorCuit` no está anotado como bean Spring (`@Component`). El plan (T-CUIT-011)
  lo marcaba como opcional y el consumo es estático (`ValidadorCuit.esValido`), consistente con
  R-CUIT-008 (servicio puro). No requiere inyección. **No bloquea**.
- **o-02**: `tasks.md` del módulo mantiene todos los checkboxes en `[ ]` (T-CUIT-001…042) aunque
  la implementación está cubierta con evidencia (higiene documental, mismo patrón que el módulo 09).
  **Responsable: coordinador.**

---

## 5. Gobernanza

| Regla | Veredicto | Evidencia |
|---|---|---|
| G5 (sin DDL/migraciones) | PASS | `ddl-auto=none` (`application.yml:69`); sin `schema.sql`/`data.sql`/Flyway/Liquibase en el repo. |
| G7 (dependencias) | PASS | `ValidadorCuit` no importa `web`/`repository`/`domain`; `ActaValidador` (service) importa `nucleo.service` únicamente. |
| G-POSTMAN | PASS | `docs/postman/ATPPuestos-API.postman_collection.json` sin cambios (`git diff` no lo lista). El módulo no expone REST (D-CUIT-04). |
| G4 (<500 líneas) | PASS | `ValidadorCuit.java` 87 líneas; `ActaValidador.java` 149 líneas; `ValidadorCuitTest.java` 165 líneas. |
| CSRF/seguridad | PASS | No aplica vistas nuevas (D-CUIT-04); seguridad de Actas ya verificada en Gate 2 del módulo 09. |
| Secretos | PASS | Sin secretos en texto plano en el código (`application.yml` usa `${DB_PASSWORD:}`); `.env` no versionado (`git ls-files .env` vacío). |
| No regresión | PASS | Suite unitaria verde (513 tests, 0 failures/errors/skipped). |

---

## 6. Trazabilidad R-CUIT-001…R-CUIT-010

| Req | Veredicto | Evidencia |
|---|---|---|
| R-CUIT-001 | PASS | `ValidadorCuit.esValido` length check + tests `long10/12Digitos`, `string10/12Digitos`. |
| R-CUIT-002 | PASS | Algoritmo módulo 11 verbatim + verificación cruzada Python. |
| R-CUIT-003 | PASS | `ActaValidador.validar` orden V→D→T con mensajes verbatim. |
| R-CUIT-004 | PASS | `ActaService.derivarPorCuitEnAlta` + `ActaServiceTest.derivarPorCuitInexistente`. |
| R-CUIT-005 | PASS | `@ValueSource` con guiones/espacios/puntos/letras ⇒ false. |
| R-CUIT-006 | PASS | grep: `ValidadorCuit` sólo en `ActaValidador`; sin carga manual. |
| R-CUIT-007 | PASS | `PREFIJOS_VALIDOS` + tests por prefijo. |
| R-CUIT-008 | PASS | Único import `java.util.Set`; sin repos/EntityManager; determinismo 1000×. |
| R-CUIT-009 | PASS | Ubicación `nucleo.service` + import en `ActaValidador`. |
| R-CUIT-010 | PASS | Casos límite Resto=0 (`UltDig=0`) y Resto=1 (`UltDig=10` ⇒ inválido). |

**Resumen**: 10 PASS, 0 FAIL, 0 NO VERIFICABLE.

---

## 7. Conclusión

El módulo `11-validacion-cuit` **aprueba el Gate 2 con observaciones**: todos los requisitos
R-CUIT-001…R-CUIT-010 se verifican con evidencia de código y tests unitarios (165 en el módulo
puestos + 370 en el núcleo, 0 failures). El único bloqueo es ambiental (DNS de la BD real) y no
afecta al módulo, cuya validación es puramente algorítmica.

**Acción requerida**: ninguna bloqueante. Opcional: coordinador debe marcar los checkboxes de
`tasks.md` (o-02) y, si se desea cobertura IT, ejecutar contra una BD accesible (m-01).