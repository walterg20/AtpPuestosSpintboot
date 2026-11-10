# Verificación — Módulo `08-control-cargas`

**Veredicto:** `BLOQUEADO` (ver §7)

Re-verificación tras la corrección de H-CC-01 y H-CC-02. La suite unitaria y la gobernanza estática
pasan en verde y ambos hallazgos quedaron resueltos. Sin embargo, la integración contra la BD real
(`Rentas`) no puede ejecutarse porque el host `devmssqlatp.ecom.com.ar:1433` no responde TCP
desde este entorno (IP privada 10.5.2.180, sin acceso VPN). Por tanto, los 4 tests de
`CargaMarcadoIT` quedan sin evidencia de ejecución y el veredicto no puede ser `PASS`.

**Fecha:** 2026-10-30
**Agente:** verificador-qa
**Logs:**
- Unitarios: `C:\Users\walterg20\AppData\Local\Temp\opencode\build2.log`
- IT (intento, bloqueado): `C:\Users\walterg20\AppData\Local\Temp\opencode\it2.log`
- Conectividad: `Test-NetConnection devmssqlatp.ecom.com.ar -Port 1433` → `TcpTestSucceeded = False`

---

## 1. Unitarios

**Comando exacto:**

```powershell
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B clean test
```

**Resultado:** `BUILD SUCCESS`.

Resumen autoritativo por módulo:

- `atp-nucleo-compartido`: `Tests run: 367, Failures: 0, Errors: 0, Skipped: 0`
- `atp-puestos-camineros`: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`
  - `CargaServiceTest`: 9 (anulada, interna, éxito 0/1/2 previos, no-confirmado, blank, delega, no encontrado)
  - `CargaMvcControllerTest`: 19
  - `PuestosApplicationTests`: 1
  - `ProductoMvcCsrfTest`: 4
- Total: **400 tests, 0 fallos, 0 errores, 0 omitidos**.

Log completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\build2.log`.

## 2. Integración contra la BD real (IT) — BLOQUEADO

**Comando exacto intentado:**

```powershell
$env:ATP_DB_IT="true"
$env:DB_HOST="devmssqlatp.ecom.com.ar"
$env:DB_PORT="1433"
$env:DB_NAME="Rentas"
$env:DB_USER="rentas"
$env:DB_PASSWORD="<redacted>"
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Resultado:** `BUILD FAILURE` (no por código, por conectividad).

Error textual (verbatim del log):

```
com.microsoft.sqlserver.jdbc.SQLServerException: The TCP/IP connection to the host
devmssqlatp.ecom.com.ar, port 1433 has failed. Error: "Connect timed out. Verify the
connection properties. Make sure that an instance of SQL Server is running on the host
and accepting TCP/IP connections at the port. Make sure that TCP connections to the
port are not blocked by a firewall.".
```

Verificación de red:

```
Resolve-DnsName devmssqlatp.ecom.com.ar -> 10.5.2.180
Test-NetConnection 10.5.2.180 -Port 1433 -> TcpTestSucceeded = False
```

El host es una IP privada (10.5.2.180) sólo alcanzable desde la red corporativa/VPN; el entorno
de verificación no tiene acceso. Todos los `*IT` (núcleo + `CargaMarcadoIT`) fallan con el mismo
error de TCP, incluyendo `CargaMarcadoIT` (4 tests: marcado exitoso, guía anulada, guía interna,
guía duplicada).

**Estado de los tests de integración de cargas:**

- `CargaMarcadoIT.java` existe en
  `atp-puestos-camineros/src/test/java/ar/com/atp/puestos/puestos/service/` y compila (incluida
  en el `clean test` del §1, que compila fuentes de test).
- Está gateado por `@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")` y
  `@Transactional` (rollback), con 4 `@Test`.
- No se pudo ejecutar por el bloqueo de red. **Evidencia de ejecución: ninguna.**

Log del intento: `C:\Users\walterg20\AppData\Local\Temp\opencode\it2.log`.

## 3. H-CC-01 — resuelto

`CargaService.MSG_ANULADA` y `MSG_INTERNA` ahora llevan tilde, coincidiendo verbatim con el
procedimiento legacy `MarcarGuia`:

- `MSG_ANULADA = "No se puede marcar el pase de la guía porque fue anulada"` (línea 21)
- `MSG_INTERNA = "No se puede marcar el pase de una guía interna"` (línea 22)

Los tests `CargaServiceTest.guiaAnulada`/`guiaInterna` y `CargaMvcControllerTest.marcarAnulada`/
`marcarInterna` usan las constantes del servicio, por lo que se mantienen consistentes. El resto
de los mensajes (`La guia ha sido marcada con exito`, `Guia Duplicada , ya paso por algun puesto`,
`Usuario sin Puesto Asignado`) se conservan sin tilde, como el legacy los escribe. **PASS**.

## 4. H-CC-02 — resuelto

- `CargaServiceTest.java` existe con 9 pruebas unitarias (Mockito): no encontrado, anulada, interna,
  éxito con 0/1/2 pases previos, pase no confirmado no incrementa, blank, delega. **PASS** (ver §1).
- `CargaMarcadoIT.java` existe con 4 pruebas de integración (`@SpringBootTest`, `@Transactional`,
  rollback) contra `Rentas`. Compila, pero **no se pudo ejecutar** (§2, BLOQUEADO).

## 5. G-POSTMAN

La colección `docs/postman/ATPPuestos-API.postman_collection.json` incorpora la carpeta **`Cargas`**
con 2 requests sincronizados con `CargaRestController`:

- `GET {{baseUrl}}/api/v1/cargas/formularios?dppNroForm=1234567890`
- `POST {{baseUrl}}/api/v1/cargas/marcar`

El diff de la colección muestra únicamente la carpeta `Cargas` agregada. Cumple R-CC-19. **PASS**.

## 6. Paridad de BD y gobernanza

- `spring.jpa.hibernate.ddl-auto=none` en `application.yml` (núcleo) y `application-test.yml`
  (ambos módulos). **PASS**.
- Sin DDL/migraciones/`schema.sql`/`data.sql`/Flyway/Liquibase en el diff. **PASS**.
- Sin secretos en texto plano (`.env` no versionado; sólo `.env.example` documenta). **PASS**.
- Archivos de configuración por debajo de 500 líneas (máximo observado: 98 líneas). **PASS**.
- Sin cambios en `nucleo/web/rest/**` (R-CC-16): el diff sólo toca Postman y `00-alcance`. **PASS**.
- Vistas Thymeleaf: los formularios de escritura usan `th:action` (CSRF activo); los errores de
  validación se muestran junto al campo; sin literales de color o tipografía fuera de `tokens.css`
  (el CSS del módulo usa únicamente custom properties). **PASS**.
- JavaScript puntual (`control-cargas.js`): vanilla, sin `fetch`/`XMLHttpRequest`/`eval`. **PASS**.

## 7. No regresión (unitaria)

Suites de `01-nucleo-datos`, `02-abm-productos` y `03-login` permanecen verdes (incluidas en los
400 tests unitarios). **PASS** (sólo a nivel unitario; la regresión de IT no pudo verificarse por
el bloqueo de red).

## 8. Resumen de verificación

| Criterio | Resultado |
|---|---|
| Unitarios (`mvn clean test`) | PASS (400 tests) |
| IT contra BD real (`CargaMarcadoIT`, 4 tests) | BLOQUEADO (DB inaccesible) |
| H-CC-01 (mensajes verbatim con tilde) | PASS |
| H-CC-02 (CargaServiceTest + CargaMarcadoIT existen) | PASS (unit) / BLOQUEADO (IT) |
| G-POSTMAN (carpeta Cargas) | PASS |
| ddl-auto=none / sin DDL | PASS |
| Secretos en texto plano | PASS |
| Configs < 500 líneas | PASS |
| No regresión (unitaria) | PASS |
| Cobertura IT de cargas | BLOQUEADO |

**Conclusión:** el código productivo compila, las constantes verbatim quedaron corregidas (H-CC-01)
y las pruebas unitarias/IT exigidas (H-CC-02) existen y pasan a nivel unitario. La gobernanza
estática (G-POSTMAN, ddl-auto, secretos, tamaño de configs) se cumple. Sin embargo, no es posible
declarar verde la integración contra `Rentas` porque el host de base de datos no responde desde
este entorno (IP privada 10.5.2.180, sin acceso VPN). El veredicto es **BLOQUEADO** hasta que se
re-ejecute `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` desde un entorno con acceso a la red
corporativa, verificando que los 4 tests de `CargaMarcadoIT` pasen en verde con rollback. No se
requiere cambio en el código para desbloquear: sólo acceso de red a `devmssqlatp.ecom.com.ar:1433`.
