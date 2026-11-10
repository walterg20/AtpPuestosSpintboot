# Tasks — 09 Formularios Acta (Puestos Camineros)

> **Estado de la puerta G-SPEC**: **APROBADA (2026-11-04)** — por el usuario (pedido "comenza").
> implementación hasta que `spec.md` + `plan.md` sean aprobados por un humano.
> Checklist verificable del módulo `09-formularios-acta`. Cada tarea es **atómica** y trazable a un
> requisito de `spec.md` (`R-ACTA-*`) o a una sección de `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` (puerta G-SPEC)** — guardarraíl G2.
> **Ninguna tarea está marcada como completada por el analista**: el cierre y la evidencia son del
> coordinador.
>
> Tareas **A** = verificación previa (KB + BD). **B** = dominio. **C** = repositorio.
> Tareas **D** = servicio. **E** = web MVC. **F** = vistas. **G** = CSS/JS.
> Tareas **H** = pruebas. **I** = gobernanza / documentación.
>
> **Leyenda de dependencias**: `→` significa "depende de". `Req:` = requisito de `spec.md`.
> `Plan:` = sección de `plan.md`.

---

## A. Verificación previa (previa a la implementación)

- [x] **T-ACTA-001** Releer por el MCP `genexus18mcp` (alias `atppuestos`, **sólo lectura**) el
  WebPanel `ActaListadoWC`, el WebPanel `PPActaDetalle` (partes `Events` + `WebForm`), el WebPanel
  `ActaCargarDetalle`, el WebPanel `F2276CargarDetalle`, la transacción `F2276` (partes `Rules` +
  `Source`) y los procedimientos `ActaDeterminaTipoFormulario`, `F2276_GenNroForm`,
  `ProPri_VerManual`, `PPrim_VerificaGenMan`, `F2276_BusProTarifa`, `F2276_BusProTarCM`,
  `F2276_RegPasePuestoV2`, `PPrim_TurnoEstado`, `F2276_DevRazSocial`, `F2276_BusDom`,
  `F2276_BusLoc`, `Nuc_CalculoImpIB`, `BuscarVersionActual`, `F2276_CalImpFl`, `F2276_CalTarFl` y
  `BuscarOrigenRetencion`; **confirmar** los 27 mensajes verbatim de `spec.md` §3.9, el
  `ControlWhere`/`ControlOrder` de `Grid276` y las 3 columnas de las `ControlValues` de los combos.
  *Sin dependencias.* · **Criterio de aceptación**: el volcado coincide carácter a carácter con
  `spec.md` §3.11/§3.12; cualquier diferencia se corrige en `spec.md` **antes** de T-ACTA-101.
  **Req**: R-ACTA-001…R-ACTA-040, R-ACTA-045…R-ACTA-047.
  > **Estado 2026-11-04**: los 15 procedures ya fueron leídos y volcados a `spec.md` §3.11/§3.12
  > (10 `IN`, 5 `OUT`). Esta tarea queda como **verificación de fidelidad** antes de implementar.

- [x] **T-ACTA-002** **G-DATA — puerta de T-Bxx.** Con `INFORMATION_SCHEMA.COLUMNS` +
  `KEY_COLUMN_USAGE` + `sys.tables` + `sys.foreign_keys` de `Rentas` (**sólo `SELECT`**), confirmar
  el esquema físico de `F2276` (54 columnas), `F22761` (15), `F2276PAS` (9), `GES_SUCN` (5) y
  `FORMSUCU` (8): nombre, tipo, nulabilidad, precisión, PK, ausencia de `IDENTITY` y ausencia de FK;
  enumerar las columnas huérfanas (`F2276.PueCod`, `F2276.CPCId`, `F22761.F276Corrig`) y confirmar
  que **no** existen `F276GenMan`, `F276Total`, `F276ImIBTot`, `F276ImCMTot`, `F276AdiTot`,
  `F276ImpTotal`, `F276ProCan`, `F276CodBar`, `F276PaseSD` ni las columnas de localidad/provincia.
  *Sin dependencias.* · **Criterio de aceptación**: el resultado coincide con `plan.md` §2.1–§2.4.2;
  si algún nombre o tipo difiere, se actualizan `spec.md` §3.10 y `plan.md` §2 **antes** de
  T-ACTA-101. **Si la BD no responde**, las tareas de dominio quedan bloqueadas.
  > **Estado 2026-11-04**: G-DATA **verificado** — `F2276`=54, `F22761`=15, `F2276PAS`=9,
  > `GES_SUCN`=5, `FORMSUCU`=8; `F276PaseSD` **no existe** (COUNT=0). La tarea queda como
  > **re-verificación** en el entorno del implementador.

- [x] **T-ACTA-003** **SA-ACTA-03 — RESUELTO (2026-11-04).** La fuente de la numeración es el
  **contador persistente `GES_SUCN.SucUltNum`** (por `SucNroId`, `SucTipo`, `SucNuForm = 2276`),
  **no** `MAX(F276Id) + 1` ni `FORMSUCU`. Evidencia: Source verbatim de `F2276_GenNroForm`
  (`spec.md` §3.11) + contraste empírico `GES_SUCN` vs `MAX(F2276.F276Id)` (sucursales 3 y 7
  difieren). *Tarea reabierta como verificación de despliegue*: confirmar en el entorno de
  implementación que
  `SELECT SucNroId, SucNuId, SucUltNum FROM GES_SUCN WHERE SucNuForm=2276 AND SucTipo='PUE';`
  coincide con lo registrado en `spec.md` §3.11, y **cerrar SA-ACTA-14** (`SucNuId`) y
  **SA-ACTA-15** (desfase del contador) con el DBA.
  *→ T-ACTA-002* · **Criterio de aceptación**: el estado de `GES_SUCN` está documentado en el
  informe de la tarea y SA-ACTA-14/15 quedan resueltos o con decisión de postergar firmada.
  **Req**: R-ACTA-036.

- [x] **T-ACTA-004** Inventariar el entorno de destino: presencia de `templates/puestos/**`,
  `static/css/tokens.css`, `static/js/**`, `ProveedorNavegacion`, `GestorContextoSesion`,
  `ServicioEstadoDeForm`, `puestos.domain.Contribuyente`, `nucleo.domain.Producto` /
  `ProductoVigencia` / `Calle` / `Localidad` / `Provincia` / `Turno`, y la seguridad de `03-login`
  (CSRF, sesión).
  *Sin dependencias.* · **Criterio de aceptación**: el inventario queda registrado en `plan.md` §1
  y §2.4–§2.5 y las tareas dependientes quedan marcadas con su dependencia.

- [x] **T-ACTA-005** Confirmar el **contrato de no regresión** con `06-turnos`: `Formulario2276` y
  `Formulario2276Repositorio` **no** se modifican; la nueva entidad `Acta` convive con la proyección
  de lectura sobre la misma tabla.
  *→ T-ACTA-002* · **Criterio de aceptación**: queda escrito en `plan.md` §1 y en el diff de
  implementación no hay cambios en
  `atp-nucleo-compartido/.../domain/Formulario2276.java` ni en
  `atp-nucleo-compartido/.../repository/Formulario2276Repositorio.java`.
  **Req**: R-ACTA-059.

---

## B. Dominio

- [x] **T-ACTA-101** Crear `puestos.domain.ActaId` (`@Embeddable`, PK compuesta `F276Id` +
  `SucTipo` + `SucNroId`) y `puestos.domain.Acta` (`@Table(name = "F2276")`) con las **54 columnas**
  de `plan.md` §2.1, con `@Column(name = "...")` exacto, `@ColumnTransformer(read = "RTRIM(...)")`
  en todos los `char(n)`, y **sin** `@Column` para los atributos fórmula
  (`F276Total`, `F276AdiTot`, `F276ImpTotal`, `F276ImIBTot`, `F276ImCMTot`, `F276ProCan`,
  `F276GenMan`, `F276VenLoc/Prov`, `F276DestLoc/Prov`, `F276TranLoc/Prov`, `F276CodBar`, `FCalFlete`).
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con
  `INFORMATION_SCHEMA.COLUMNS`; los `char(n)` se leen con `RTRIM`. **Req**: R-ACTA-050, R-ACTA-052.
  **Plan**: §2.1, D-ACTA-01, D-ACTA-07.

- [x] **T-ACTA-102** Crear `puestos.domain.ActaProductoId` (`@Embeddable`, PK
  `F276Id`+`SucTipo`+`SucNroId`+`ProdCod`+`F276RZona`) y `puestos.domain.ActaProducto`
  (`@Table(name = "F22761")`) con las **15 columnas** de `plan.md` §2.2, incluida la huérfana
  `F276Corrig`.
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila; el mapeo coincide con la BD.
  **Req**: R-ACTA-047, R-ACTA-050, R-ACTA-051. **Plan**: §2.2.

- [x] **T-ACTA-103** Crear `puestos.domain.ActaPaseId` (`@Embeddable`, PK
  `SucTipo`+`SucNroId`+`F276Id`+`F276PPaseI`) y `puestos.domain.ActaPase`
  (`@Table(name = "F2276PAS")`) con las **9 columnas** de `plan.md` §2.3.
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila; el mapeo coincide con la BD.
  **Req**: R-ACTA-048, R-ACTA-050. **Plan**: §2.3.

- [x] **T-ACTA-104** Crear `puestos.domain.ContadorFormulario` (`@Table(name = "GES_SUCN")`) con las
  **5 columnas** físicas de `plan.md` §2.4.1 (`SucNroId`, `SucTipo`, `SucNuForm`, `SucNuId`,
  `SucUltNum`), PK (`SucNroId`, `SucTipo`, `SucNuForm`), `RTRIM` en los `char(3)` y `SucNuId` con
  default de aplicación `'MAN'` (D-ACTA-19, SA-ACTA-14).
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con
  `INFORMATION_SCHEMA.COLUMNS` (`GES_SUCN` = 5 columnas, sin `IDENTITY`, sin FK).
  **Req**: R-ACTA-036, R-ACTA-050. **Plan**: §2.4.1, D-ACTA-19.

- [x] **T-ACTA-105** Crear `puestos.domain.RangoManualFormulario` (`@Table(name = "FORMSUCU")`) con
  las **8 columnas** físicas (`SucNroId`, `SucTipo`, `SucNuForm`, `ForRecIni`, `ForRecCant`,
  `ForRecFech`, `ForRecUsu`, `ForRecTipo`) y **sin** `SucUltNum` ni `ForRecFin` (no existen). Esta
  tabla **no** participa de la numeración: sólo de los rangos manuales (D-ACTA-20).
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con la BD.
  **Req**: R-ACTA-008, R-ACTA-036. **Plan**: §2.4.2, D-ACTA-20.

- [x] **T-ACTA-106** Crear `puestos.domain.VersionSistema` (`@Table(name = "VERSIONE")`) con las
  5 columnas `VerApliId` (char(3)), `VerDescr` (varchar 40), `VerVersion` (smallint),
  `VerRevisio` (smallint) y `VerBuild` (decimal), para resolver `F276Vers`
  (`BuscarVersionActual`, `spec.md` §3.12 #11).
  *→ T-ACTA-002* · **Criterio de aceptación**: la entidad compila y el mapeo coincide con la BD.
  **Req**: R-ACTA-045. **Plan**: §2.5.

- [x] **T-ACTA-107** Verificar que el diff **no** introduce DDL, migraciones, `schema.sql`/`data.sql`,
  Flyway/Liquibase ni cambios en `spring.jpa.hibernate.ddl-auto` (debe seguir en `none`).
  *→ T-ACTA-101, T-ACTA-102, T-ACTA-103, T-ACTA-104, T-ACTA-105, T-ACTA-106* · **Criterio de
  aceptación**: `git diff` sin `CREATE TABLE`/`ALTER TABLE`. **Req**: R-ACTA-050. **Plan**: G5.

---

## C. Repositorio

- [x] **T-ACTA-111** Crear `puestos.repository.ActaRepository` (`JpaRepository<Acta, ActaId>`) con:
  - `Page<Acta> buscar(String sucTipo, Integer sucNroId, Integer turNroId, String f276GenWeb,
    String excluirEstad, Integer f276Id, Long cuit, String razonSocial, LocalDate desde,
    LocalDate hasta, Pageable pageable)` — equivalente al `ControlWhere` del legacy **más** los
    filtros de UX;
  - `Optional<Acta> buscarMaximo(...)` / `long maxF276Id(...)` — **sólo para diagnóstico y para
    contrastar el contador** (SA-ACTA-15); **NO** se usa para numerar (D-ACTA-21);
  - `long contarActasPendientes(...)` (para R-ACTA-043);
  - queries nativas si el filtrado por CUIT/razón social sobre los tres roles no es expresable en
    JPQL.
  *→ T-ACTA-101* · **Criterio de aceptación**: las consultas se generan correctamente y el `WHERE`
  base reproduce `F276Estad <> 'A' AND F276GenWeb = 'A' AND TurNroId=? AND SucNroId=? AND
  SucTipo=?`. **Req**: R-ACTA-003, R-ACTA-004, R-ACTA-011, R-ACTA-043. **Plan**: §3.4.

- [x] **T-ACTA-112** Crear `puestos.repository.ActaProductoRepository` con
  `List<ActaProducto> findByActa(ActaId id)`, `sumImpuestoIb(ActaId id)` y `sumImpuestoCm(ActaId id)`
  (para el total derivado) y `deleteByActa(ActaId id)`.
  *→ T-ACTA-102* · **Criterio de aceptación**: las consultas se generan correctamente.
  **Req**: R-ACTA-007, R-ACTA-047. **Plan**: §2.1, D-ACTA-07.

- [x] **T-ACTA-113** Crear `puestos.repository.ActaPaseRepository` con
  `List<ActaPase> findByActa(Integer f276Id, String sucTipo, Integer sucNroId)` y
  `int maxIndicePase(...)` (índice correlativo del pase).
  *→ T-ACTA-103* · **Criterio de aceptación**: las consultas se generan correctamente.
  **Req**: R-ACTA-048. **Plan**: §2.3.

- [x] **T-ACTA-114** Crear `puestos.repository.ContadorFormularioRepositorio` con el **incremento
  atómico** del contador: `int incrementarAtomico(Integer sucNroId, String sucTipo, Integer
  sucNuForm)` (query nativa `UPDATE GES_SUCN SET SucUltNum = SucUltNum + 1 WHERE SucNroId = ?
  AND SucTipo = ? AND SucNuForm = ?`, devolviendo el valor incrementado) y `Optional<ContadorFormulario>
  buscar(...)`; el servicio inserta `(SucNroId, SucTipo, SucNuForm, 'MAN', 1)` si no existe
  (D-ACTA-19).
  *→ T-ACTA-104* · **Criterio de aceptación**: test de integración que verifica que dos
  incrementos consecutivos devuelven `n+1` y `n+2` sobre la misma fila, y que la fila insertada
  tiene `SucNuId = 'MAN'` y `SucUltNum = 1`. **Req**: R-ACTA-036. **Plan**: §2.4.1, §3.4, D-ACTA-19.

- [x] **T-ACTA-115** Crear `puestos.repository.RangoManualFormularioRepositorio` con
  `List<RangoManualFormulario> findBySucursalYFormulario(Integer sucNroId, String sucTipo, Integer
  sucNuForm)` y el método que determine si un `F276Id` cae en un rango manual
  (`ForRecIni <= f276Id < ForRecIni + ForRecCant`) — `ProPri_VerManual` / `PPrim_VerificaGenMan`.
  *→ T-ACTA-105* · **Criterio de aceptación**: los casos límite (`ForRecIni` y
  `ForRecIni + ForRecCant - 1` dentro; `ForRecIni - 1` y `ForRecIni + ForRecCant` fuera) se
  verifican en test unitario. **Req**: R-ACTA-008, R-ACTA-036. **Plan**: §2.4.2, D-ACTA-20.

- [x] **T-ACTA-116** Crear `puestos.repository.VersionSistemaRepositorio` con
  `Optional<VersionSistema> buscarPorAplicacion(String verApliId)` (para `BuscarVersionActual`).
  *→ T-ACTA-106* · **Criterio de aceptación**: la consulta se genera correctamente y devuelve la
  fila de `VERSIONE` con `VerApliId = 'SWP'`. **Req**: R-ACTA-045. **Plan**: §2.5.

---

## D. Servicio

- [x] **T-ACTA-121** Implementar en `puestos.service.ActaService` el método `listar(...)`: aplica el
  filtro base del turno, los filtros de UX, arma `Page<ActaFila>` y por cada fila calcula
  **contraparte** (CUIT / razón social en el orden vendedor → destinatario → transportista),
  **total** (`F276AdiTot + F276ImpTotal`) e **indicador "Gen. Manual?"** (`Sí`/`No`) consultando
  `FORMSUCU`.
  *→ T-ACTA-111, T-ACTA-112, T-ACTA-115* · **Criterio de aceptación**: test unitario con las 4
  combinaciones de contraparte, con total conocido y con rango manual conocido
  (`ForRecIni=100, ForRecCant=50` ⇒ `Sí` para 100 y 149, `No` para 99 y 150).
  **Req**: R-ACTA-001, R-ACTA-003, R-ACTA-004, R-ACTA-006, R-ACTA-007, R-ACTA-008, R-ACTA-011.
  **Plan**: §3.4, D-ACTA-07, D-ACTA-20.

- [x] **T-ACTA-122** Implementar en `ActaService` el método `validar(ActaForm form, ContextoSesion
  ctx)` que reproduce **en el orden exacto** el `Sub 'Validar'` del legacy y devuelve **el primer**
  mensaje verbatim (nunca todos). El orden es: motivo de acta → nro. de acta (modo manual) → tipo
  de movimiento → CUIT vendedor → CUIT destinatario → CUIT transportista → razón social (3) →
  datos del transporte → detalle de productos → máximo 6 productos → duplicado de producto →
  precio inferior al registrado → fecha futura → turno cerrado.
  *→ T-ACTA-121* · **Criterio de aceptación**: test parametrizado que compara el mensaje devuelto
  contra la cadena literal de `spec.md` §3.9 y un test con 4 infracciones simultáneas que verifica
  que sólo se devuelve el primer mensaje. **Req**: R-ACTA-026…R-ACTA-040. **Plan**: §3.4.

- [x] **T-ACTA-123** Implementar en `ActaService` el método `guardar(ActaForm form, ContextoSesion
  ctx)`: valida, **genera el `F276Id` con `ServicioNumeracionActa`** (misma transacción), arma la
  cabecera `F2276` con los **11 valores por defecto**
  (`F276GenWeb='A'`, `F276Operad=usuario.toUpperCase()`, `CPCPerId=personaId`, `F276Estad='C'`,
  `F276Multa=0`, `F276ForPag='EFE'`, `F276FchMov`, `F276FeGra=now`, `TurNroId`, `SucTipo/SucNroId`
  del contexto, `F276Vers` de `VERSIONE`), calcula `F276FormTi` y persiste cabecera + detalle + pase
  **en una única transacción** (`@Transactional`).
  *→ T-ACTA-122, T-ACTA-128, T-ACTA-129, T-ACTA-134* · **Criterio de aceptación**: IT que inserta
  un Acta y compara los 11 valores con la fila leída de `F2276`; ante un error intermedio la tabla
  queda sin cambios. **Req**: R-ACTA-044, R-ACTA-045, R-ACTA-046, R-ACTA-049. **Plan**: §2.6.

- [x] **T-ACTA-124** Implementar en `ActaService` el método `guardarDetalle(...)`: una fila en
  `F22761` por producto, con `F276RCant`, `F276RFijo`, `F276RSPre`, `F276RPreCM`, `F276RAlicu=1`,
  `F276RMarCM='N'`, `F276RImpIB`, `F276RImpCM` y `F276RZona` (SA-ACTA-06), con las **fórmulas
  verificadas** de `spec.md` §3.12 (`F2276_BusProTarifa`, `F2276_BusProTarCM`, `Nuc_CalculoImpIB`,
  `F276RImpCM = F276RCant * F276RPreCM`).
  *→ T-ACTA-123, T-ACTA-133* · **Criterio de aceptación**: IT con 2 productos que verifica las 2 filas
  y sus importes contra los valores calculados a mano. **Req**: R-ACTA-047. **Plan**: §2.2, §2.6.

- [x] **T-ACTA-125** Implementar en `ActaService` el método `registrarPase(...)` que replica
  `F2276_RegPasePuestoV2`: índice correlativo, `F276PFecha=now`, `F276PUsuar=usuario`,
  `F276PaseST=tipo`, `F276PaseSN=sucursal`, `F276PaseCo=comisión`.
  *→ T-ACTA-123* · **Criterio de aceptación**: IT que verifica la fila de `F2276PAS` y que el índice
  es correlativo. **Req**: R-ACTA-048. **Plan**: §2.3.

- [x] **T-ACTA-126** Implementar en `ActaService` el método `derivarPorCuit(Long cuit)` que devuelve
  razón social, domicilio, código de localidad y nombre de provincia del contribuyente, replicando
  `F2276_DevRazSocial` (vacío si no existe), `F2276_BusDom` (domicilio = `CallNom + ' ' + manzana`)
  y `F2276_BusLoc` (`LocCodR`); la provincia **no** se persiste.
  *→ T-ACTA-121* · **Criterio de aceptación**: test unitario con un contribuyente conocido;
  si el contribuyente no existe, devuelve vacíos sin excepción (paridad `For each` sin match).
  **Req**: R-ACTA-017, R-ACTA-018. **Plan**: §2.5, D-ACTA-17.

- [x] **T-ACTA-127** Implementar en `ActaService` el método `precioProducto(Integer prodCod, String
  clasif, LocalDate fecha, boolean marCM)` que replica `F2276_BusProTarifa` / `F2276_BusProTarCM`:
  vigencia de `PRODUCT1` con `ProdFchVig <= fecha` (la más reciente), `ProdImpu1`/`ProdImpu3` según
  `clasif='S'`, y `ProdImpu2`/`ProdImpu4` para cosecha mecánica; `0` si no hay vigencia.
  *→ T-ACTA-121* · **Criterio de aceptación**: test unitario con 2 vigencias que verifica que se toma
  la vigente a la fecha y la columna correcta según la clasificación.
  **Req**: R-ACTA-022, R-ACTA-037. **Plan**: §2.5.

- [x] **T-ACTA-128** Implementar en `ActaService` el método `tipoFormulario(Long cuit, String
  clasif)` que replica `ActaDeterminaTipoFormulario`: `1` (E sin infracción), `2` (E con
  infracción), `3` (S); infracción = contribuyente existe y `ConEstado` ∉ {12, 14}.
  *→ T-ACTA-121* · **Criterio de aceptación**: test parametrizado con (clasificación, estado) y el
  caso "contribuyente inexistente" ⇒ `1`. **Req**: R-ACTA-046. **Plan**: §2.5.

- [x] **T-ACTA-129** Implementar `puestos.service.ServicioNumeracionActa.siguienteNumeroActa(sucNroId,
  sucTipo)` sobre el **contador `GES_SUCN`**: incremento **atómico**
  (`ContadorFormularioRepositorio.incrementarAtomico`) dentro de la transacción del alta
  (`Propagation.MANDATORY`); si la fila **no existe**, insertar
  `(SucNroId, SucTipo, SucNuForm = 2276, 'MAN', 1)` y **devolver 1** (paridad exacta con el
  `when none` del legacy). **NO** usar `MAX(F276Id) + 1` (D-ACTA-21).
  *→ T-ACTA-003, T-ACTA-114* · **Criterio de aceptación**: IT que verifica (a) 3 números consecutivos
  sobre una sucursal de prueba, (b) que el número coincide con `GES_SUCN.SucUltNum` + 1, (c) que no
  colisiona con la PK `(F276Id, SucTipo, SucNroId)`, (d) que la inserción de la fila inexistente deja
  `SucNuId = 'MAN'` y `SucUltNum = 1`, y (e) que **dos incrementos concurrentes** devuelven números
  distintos. **Req**: R-ACTA-036. **Plan**: §2.4.1, §3.4, D-ACTA-13, D-ACTA-19, D-ACTA-21.
  > SA-ACTA-03 **cerrado**: la fuente es `GES_SUCN.SucUltNum` (no `MAX(F276Id)+1`).

- [x] **T-ACTA-130** Implementar en `ActaService` el método `prepararFormulario(ContextoSesion ctx)`
  que: (a) verifica con `ServicioEstadoDeForm.hayFormularioAbierto(sucursal, tipo, turno)` que el
  turno no tiene formularios abiertos; (b) verifica que el turno no está cerrado; (c) arma el
  `ActaForm` con los defaults (`f276FchMov` = hoy, `f276ActaTi='N'`, `f276Clasif='N'`) y el catálogo
  de productos con `ProdAnula = 0`.
  *→ T-ACTA-121, T-ACTA-126* · **Criterio de aceptación**: `@WebMvcTest` con
  `ServicioEstadoDeForm` simulado en `true` y `false`; el template del formulario sólo se renderiza
  con el bloqueo inactivo. **Req**: R-ACTA-041, R-ACTA-042. **Plan**: D-ACTA-05.

- [x] **T-ACTA-131** Implementar la regla de cierre sin Actas pendientes: un método que cuente las
  Actas del turno con `F276Estad` distinto de `C` y de `A` (incluido `NULL`) y lo integre en el
  flujo de cierre de turno/lote (`06-turnos`), o dejar constancia de por qué no se integra en este
  módulo (SA-ACTA-04).
  *→ T-ACTA-111* · **Criterio de aceptación**: test unitario con un turno que tiene un Acta
  `F276Estad` vacío (bloquea) y otro con todas `C`/`A` (permite). **Req**: R-ACTA-043.

- [x] **T-ACTA-132** Crear `puestos.service.ResultadoAltaActa` (`record` inmutable:
  `boolean ok, String mensaje, Integer f276Id`) y la clase de mensajes verbatim
  `puestos.service.MensajesActa` con las constantes de `spec.md` §3.9.
  *Sin dependencias.* · **Criterio de aceptación**: las constantes coinciden **carácter a carácter**
  con `spec.md` §3.9 (test de igualdad de cadenas). **Req**: R-ACTA-052.

- [x] **T-ACTA-133** Implementar en `ActaService` el método `impuestoIb(Integer prodTipCal, BigDecimal
  cant, BigDecimal mImpo, BigDecimal fijo, BigDecimal alicu)` que replica **exactamente**
  `Nuc_CalculoImpIB` (`spec.md` §3.12 #10): `TipCal = 1` ("PorTonelada") y `TipCal = 2` ("PorUnidad")
  ⇒ `Calculo1 = Round(cant * fijo, 2)` y `Calculo2 = Round(mImpo * alicu / 100, 2)`;
  `TipCal = 3` ("PorMontoFijo") ⇒ `Calculo1 = fijo` y el mismo `Calculo2`;
  resultado = **`MAX(Calculo1, Calculo2)`**; `TipCal` ∉ {1,2,3} ⇒ `0`.
  *→ T-ACTA-121* · **Criterio de aceptación**: test parametrizado con los 3 `ProdTipCal` + el caso
  fuera de rango, comparando contra los valores calculados a mano con `BigDecimal` y redondeo a 2
  decimales (`HALF_UP` como en GeneXus). **Req**: R-ACTA-047. **Plan**: §3.4, `spec.md` §3.12.

- [x] **T-ACTA-134** Implementar en `ActaService` el método `versionSistema()` que replica
  `BuscarVersionActual`: toma la fila de `VERSIONE` con `VerApliId = 'SWP'` (ordenada por
  `VerDescr`) y devuelve `VerVersion + "." + VerRevisio + "." + VerBuild`; si no hay fila,
  devuelve cadena vacía (paridad: `&Version` queda sin asignar).
  *→ T-ACTA-121, T-ACTA-116* · **Criterio de aceptación**: test unitario con una fila de `VERSIONE`
  conocida que verifica el string exacto. **Req**: R-ACTA-045. **Plan**: §2.5, §3.4.

- [x] **T-ACTA-135** Implementar en `ActaService` el método `estadoTurno(Integer sucNroId, String
  sucTipo, Integer turNroId)` que replica `PPrim_TurnoEstado` (lee `TurEstado` de `PPRIM_TU`) y usarlo
  en la validación de turno cerrado del alta (`'C'` ⇒ `No se puede generar la guia porque el turno
  asociado esta cerrado`).
  *→ T-ACTA-121* · **Criterio de aceptación**: test unitario con turno `'C'` y `'A'` verificando el
  mensaje verbatim; IT que verifica que no se inserta nada con el turno cerrado.
  **Req**: R-ACTA-035, R-ACTA-042. **Plan**: §3.4, `spec.md` §3.12.

---

## E. Web MVC

- [x] **T-ACTA-141** Crear `puestos.web.mvc.ActasController` con `GET /actas`: recibe `Pageable`,
  `FiltroActasForm` y `HttpServletRequest`; puebla `contexto` (vía `GestorContextoSesion`),
  `programas` y `rutaActual` (vía `ProveedorNavegacion`); delega en `ActaService.listar(...)` y
  vuelca `actas` y `filtroActas` al `Model`; devuelve la vista `puestos/actas-lista`.
  *→ T-ACTA-121, T-ACTA-130* · **Criterio de aceptación**: `@WebMvcTest` comprueba `GET /actas` → 200,
  vista `puestos/actas-lista`, atributos `actas`/`filtroActas`/`contexto`; el controller **no**
  importa repositorios. **Req**: R-ACTA-001, R-ACTA-010, R-ACTA-012. **Plan**: §3.2.

- [x] **T-ACTA-142** Implementar `GET /actas/nueva` en `ActasController`: delega en
  `ActaService.prepararFormulario(...)`; si hay bloqueo (lote/turno) renderiza la vista con
  `bloqueado=true` y el motivo, **sin** el formulario; si no, renderiza `puestos/acta-formulario`
  con `actaForm` y `productosDisponibles`.
  *→ T-ACTA-141* · **Criterio de aceptación**: `@WebMvcTest` con las 2 ramas; en la rama bloqueada el
  HTML **no** contiene los campos del formulario. **Req**: R-ACTA-014, R-ACTA-041, R-ACTA-042.

- [x] **T-ACTA-143** Implementar `POST /actas/guardar` en `ActasController` con
  `@Valid @ModelAttribute("actaForm") ActaForm form, BindingResult result`: si `result` tiene
  errores ⇒ re-render de `puestos/acta-formulario` (200) con los errores por campo; si el servicio
  devuelve error de negocio ⇒ re-render con el banner del mensaje verbatim; si es exitoso ⇒
  `redirect:/actas` con flash `mensajeExito = El acta se ha creado correctamente`.
  *→ T-ACTA-142* · **Criterio de aceptación**: `@WebMvcTest` con los 3 caminos (binding inválido,
  error de negocio, éxito) y verificación del flash. **Req**: R-ACTA-025, R-ACTA-040, R-ACTA-052.
  **Plan**: §3.2, §4.5, D-ACTA-12.

- [x] **T-ACTA-144** Crear los form objects `ActaForm` (con las validaciones Bean Validation de
  `plan.md` §3.3), `ActaProductoForm`, `FiltroActasForm` y el view model `ActaFila` (contrato del servicio, `service/model/**`)
  (todos `record` o clases JavaBeans con getters/setters compatibles con `th:field`).
  *Sin dependencias.* · **Criterio de aceptación**: los form objects compilan y `th:field` puede
  enlazar cada campo. **Req**: R-ACTA-014, R-ACTA-055. **Plan**: §3.3.

- [x] **T-ACTA-145** Verificar que el controller **no** accede a repositorios ni a la BD
  directamente (regla `web → service → repository → domain`).
  *→ T-ACTA-141, T-ACTA-142, T-ACTA-143* · **Criterio de aceptación**: `grep` del paquete
  `puestos.web.mvc` sin imports de `puestos.repository` ni de `nucleo.repository`.
  **Req**: R-ACTA-001. **Plan**: §1.1.

---

## F. Vistas

- [x] **T-ACTA-151** Crear `templates/puestos/actas-lista.html` que **extiende** `layout/base`
  (`head`, `cabecera`, `sesion`, `navegacion`, `pie`) e implementa: título, botón **"Crear Acta"**
  (`<a th:href="@{/actas/nueva}">` con clase de token, sin estilo inline), grilla con los
  encabezados `Acto Nro.`, `CUIT`, `Razón Social`, `Gen. Manual?`, `Usuario Gen.`, `Imprimir`, filas
  con `th:each`, banner `Debe iniciar un turno...` y estado vacío.
  *→ T-ACTA-141* · **Criterio de aceptación**: test de vista que verifica los 6 encabezados en orden,
  el enlace del botón, el banner y la ausencia de `<header>`/`<footer>` duplicados.
  **Req**: R-ACTA-002, R-ACTA-005, R-ACTA-009, R-ACTA-013, R-ACTA-056. **Plan**: §4.2.

- [x] **T-ACTA-152** Agregar a `actas-lista.html` el formulario de **filtros** (método `GET`,
  `th:object="${filtroActas}"`) con los campos `f276Id`, `cuit`, `razonSocial`, `desde`, `hasta` y el
  botón `Buscar`.
  *→ T-ACTA-151* · **Criterio de aceptación**: test de vista que verifica los 5 campos y que el
  `action` conserva la ruta `/actas`. **Req**: R-ACTA-011. **Plan**: §4.2.

- [x] **T-ACTA-153** Agregar a `actas-lista.html` la **paginación**: links con `th:each` sobre
  `#numbers.sequence(0, totalPages-1)` que preservan `page`, `size`, `sort` y todos los filtros.
  *→ T-ACTA-152* · **Criterio de aceptación**: test de vista que verifica que el enlace de la
  página 2 contiene los query params del filtro aplicado. **Req**: R-ACTA-010, R-ACTA-012.

- [x] **T-ACTA-154** Crear `templates/puestos/acta-formulario.html` que extiende `layout/base` e
  implementa el fieldset **"Datos del Acta"** (`Motivo de Acta`, `Tipo Movimiento`, `Fecha Acta`
  read-only) con los `ControlValues` verbatim, **sin** el campo `Nro. Acta`.
  *→ T-ACTA-142* · **Criterio de aceptación**: test de vista que compara el conjunto de
  `(valor, texto)` de cada `<select>` contra las tablas de `spec.md` §3.2 y verifica que `Nro. Acta`
  no se renderiza. **Req**: R-ACTA-015, R-ACTA-016. **Plan**: §4.3.

- [x] **T-ACTA-155** Implementar en `acta-formulario.html` los tres fieldsets **Vendedor**,
  **Destinatario** y **Transportista** (5 campos cada uno: CUIT, Razón Social, Domicilio, Localidad,
  Provincia), con Localidad y Provincia de sólo lectura y `label` asociado a cada control.
  *→ T-ACTA-154* · **Criterio de aceptación**: test de vista que verifica las 3 leyendas, los 15
  controles con `label for` y los `readonly` de Localidad/Provincia.
  **Req**: R-ACTA-017, R-ACTA-018, R-ACTA-055. **Plan**: §4.3.

- [x] **T-ACTA-156** Implementar en `acta-formulario.html` el fieldset **"Datos del Transporte"**
  (`Patente Chasis`, `Patente Acoplado`, `T. de Comp.` con las 6 opciones + `Seleccionar...`,
  `N° Comp.`, `Flete (Km)`).
  *→ T-ACTA-155* · **Criterio de aceptación**: test de vista que verifica los 5 rótulos y las 6
  opciones del selector. **Req**: R-ACTA-019.

- [x] **T-ACTA-157** Implementar en `acta-formulario.html` el fieldset **"Productos"**: grilla con
  `Código`, `Descripción`, `Tn.` y `Eliminar`, botón `+` (`#acta-agregar-producto`) y filas
  dinámicas con `th:each="p, iter : *{productos}"` usando `__${iter.index}__`.
  *→ T-ACTA-156* · **Criterio de aceptación**: test de vista que verifica los encabezados, el botón
  `+` y el nombre del campo indexado. **Req**: R-ACTA-020. **Plan**: §4.3, D-ACTA-11.

- [x] **T-ACTA-158** Agregar a `acta-formulario.html` el banner de error de negocio
  (`atp-alert--error`, `role="alert"`) y, por campo, `th:errorclass` + `th:errors` con
  `aria-describedby`.
  *→ T-ACTA-154* · **Criterio de aceptación**: test de vista que verifica la presencia de
  `th:errors` por campo y del banner con el mensaje verbatim.
  **Req**: R-ACTA-040, R-ACTA-055. **Plan**: §4.3, §4.5.

- [x] **T-ACTA-159** Implementar en `acta-formulario.html` las **acciones**: `Confirmar` (submit con
  `confirm('¿Confirma que desea enviar el Formulario de Acta?')`) y `Cancelar` (enlace a `/actas`).
  *→ T-ACTA-157, T-ACTA-158* · **Criterio de aceptación**: test de vista que verifica el texto del
  `confirm` y el `href` de Cancelar. **Req**: R-ACTA-024, R-ACTA-025. **Plan**: §4.3.

- [x] **T-ACTA-160** Mostrar el mensaje de éxito `El acta se ha creado correctamente` como flash en
  `actas-lista.html` tras el `redirect` del alta.
  *→ T-ACTA-143* · **Criterio de aceptación**: test MockMvc que verifica el texto renderizado tras
  el `redirect`. **Req**: R-ACTA-045, R-ACTA-052. **Plan**: §3.2.

---

## G. CSS / JS

- [x] **T-ACTA-161** Agregar a `atp-nucleo-compartido/src/main/resources/static/css/tokens.css` los
  tokens nuevos `--atp-actas-*` de `plan.md` §4.4 (reutilizando los existentes cuando haya
  equivalencia) y **ningún** literal en las hojas nuevas.
  *→ T-ACTA-004* · **Criterio de aceptación**: `grep` sobre `static/css/actas.css` sin literales
  hexadecimales. **Req**: R-ACTA-053. **Plan**: §4.4, D-ACTA-10.

- [x] **T-ACTA-162** Crear `static/css/actas.css` con el estilo de la grilla de listado y de los
  fieldsets del formulario, **basado sólo en `var(--atp-*)`**, replicando la composición del Figma
  (`2030:2`): fieldsets lado a lado, leyenda sobre el borde, botones `Confirmar`/`Cancelar` de
  120×35 con radio 7 px, botón `+` de 32 px.
  *→ T-ACTA-161, T-ACTA-151, T-ACTA-154* · **Criterio de aceptación**: `grep` sin literales
  hexadecimales ni nombres de fuente fuera de `tokens.css`; inspección visual contra el Figma.
  **Req**: R-ACTA-053, R-ACTA-054.

- [x] **T-ACTA-163** Crear `static/js/actas.js` (vanilla, sin framework) con: alta de fila de
  producto (clonando una fila plantilla), baja de fila, forzado de cantidad a `1` y deshabilitado
  cuando `ProdTipCal = 3`, y autofocus. **Sin** `fetch`, `XMLHttpRequest`, `eval` ni lógica de
  negocio (la validación vive en el servicio).
  *→ T-ACTA-157* · **Criterio de aceptación**: `grep` sin `fetch`/`XMLHttpRequest`/`eval`; prueba de
  navegador que agrega y quita una fila y verifica el forzado de la cantidad.
  **Req**: R-ACTA-020, R-ACTA-021, R-ACTA-024. **Plan**: D-ACTA-11.

---

## H. Pruebas

- [x] **T-ACTA-171** Suite `@WebMvcTest` de `ActasController` (`ActasMvcTest`): render de `/actas`
  (columnas, botón, banner de turno inactivo, estado vacío, paginación), render de `/actas/nueva`
  (2 ramas: bloqueada y disponible) y `POST /actas/guardar` (binding inválido, error de negocio,
  éxito con flash). **≥ 15 pruebas nuevas**.
  *→ T-ACTA-141, T-ACTA-142, T-ACTA-143* · **Criterio de aceptación**: `./mvnw clean test` en verde.
  **Req**: R-ACTA-001, R-ACTA-002, R-ACTA-009…R-ACTA-013, R-ACTA-014, R-ACTA-025, R-ACTA-040, R-ACTA-041.

- [x] **T-ACTA-172** Suite de tests de vista (`MockMvc` + `content()`) que verifique: los 6
  encabezados de la grilla en orden, los `ControlValues` verbatim de los 3 selectores, los 15
  controles de las partes con `label for`, los `readonly` de Localidad/Provincia, el `th:errors` por
  campo y la ausencia de `Nro. Acta`.
  *→ T-ACTA-154, T-ACTA-155, T-ACTA-156, T-ACTA-158* · **Criterio de aceptación**: los tests
  comparan contra las cadenas literales de `spec.md` §3.2 y §3.9.
  **Req**: R-ACTA-015…R-ACTA-019, R-ACTA-055.

- [x] **T-ACTA-173** Suite unitaria de `ActaService` (`ActaServiceTest`): contraparte (4 casos),
  total, "Gen. Manual?" (4 casos límite), precio de producto (2 vigencias × 2 clasificaciones),
  impuesto IB (`Nuc_CalculoImpIB`, 4 casos), versión del sistema, estado del turno, tipo de
  formulario (5 casos), numeración, y validaciones con los **13 mensajes** de `spec.md` §3.9
  comparados carácter a carácter.
  *→ T-ACTA-121, T-ACTA-127, T-ACTA-128, T-ACTA-129, T-ACTA-133, T-ACTA-134, T-ACTA-135* ·
  **Criterio de aceptación**: suite en verde y los mensajes comparados contra las constantes de
  `MensajesActa`.
  **Req**: R-ACTA-006, R-ACTA-007, R-ACTA-008, R-ACTA-022, R-ACTA-026…R-ACTA-040, R-ACTA-045,
  R-ACTA-046, R-ACTA-047.

- [x] **T-ACTA-174** IT contra `Rentas` (`ATP_DB_IT=true`, con rollback) `ActaListadoIT`: el listado
  paginado devuelve **exactamente** las filas que satisface el `ControlWhere` legacy (comparación con
  un `SELECT` equivalente) y respeta el `ControlOrder`.
  *→ T-ACTA-111* · **Criterio de aceptación**: `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` en verde y
  la BD **sin cambios** al terminar. **Req**: R-ACTA-003, R-ACTA-004, R-ACTA-010.

- [x] **T-ACTA-175** IT contra `Rentas` `ActaAltaIT`: alta completa de un Acta con 2 productos;
  verificación de la cabecera (11 valores por defecto + `F276Id` generado), del detalle (`F22761`)
  y del pase (`F2276PAS`); y verificación de que un error intermedio **revierte** toda la operación
  (incluido el contador `GES_SUCN`).
  *→ T-ACTA-123, T-ACTA-124, T-ACTA-125* · **Criterio de aceptación**: conteos iguales antes y
  después del test fallido; la BD queda sin cambios.
  **Req**: R-ACTA-045, R-ACTA-047, R-ACTA-048, R-ACTA-049.

- [x] **T-ACTA-175b** IT contra `Rentas` `ActaNumeracionIT`: verificar que
  `ServicioNumeracionActa.siguienteNumeroActa(...)` devuelve el valor siguiente al
  `GES_SUCN.SucUltNum` de la sucursal, que **no** colisiona con la PK `(F276Id, SucTipo, SucNroId)`,
  que la inserción de la fila inexistente usa `SucNuId = 'MAN'` y `SucUltNum = 1`, y que **dos
  incrementos concurrentes** sobre la misma sucursal devuelven números **distintos** (desvío
  consciente del legacy, D-ACTA-19).
  *→ T-ACTA-129, T-ACTA-114* · **Criterio de aceptación**: el test falla si el número devuelto no es
  `SucUltNum + 1` o si dos ejecuciones concurrentes devuelven el mismo valor; la BD queda sin
  cambios (rollback). **Req**: R-ACTA-036. **Plan**: §2.4.1, D-ACTA-19, D-ACTA-21.

- [x] **T-ACTA-176** IT de mapeo `ActaMapeoIT`: comparar el mapeo JPA contra
  `INFORMATION_SCHEMA.COLUMNS` (nombres, tipos, nulabilidad) y verificar el `RTRIM` de los `char(n)`
  leyendo un `F276Operad` real (`'SEBAM     '` ⇒ `'SEBAM'`).
  *→ T-ACTA-101, T-ACTA-102, T-ACTA-103, T-ACTA-104, T-ACTA-105, T-ACTA-106* · **Criterio de
  aceptación**: el test falla si falta o sobra alguna de las **54/15/9/5/8** columnas de
  `F2276`/`F22761`/`F2276PAS`/`GES_SUCN`/`FORMSUCU`. **Req**: R-ACTA-050, R-ACTA-051, R-ACTA-052.

- [x] **T-ACTA-177** Test de CSS/JS: `actas.css` sin literales hexadecimales ni fuentes externas;
  `actas.js` sin `fetch`/`XMLHttpRequest`/`eval`; `actas-lista.html` y `acta-formulario.html` sin
  `bootstrap`/`tailwind`/`cdn.`/`https://`/`node_modules`.
  *→ T-ACTA-161, T-ACTA-162, T-ACTA-163* · **Criterio de aceptación**: `grep` sin hallazgos.
  **Req**: R-ACTA-053.

- [x] **T-ACTA-178** Inspección visual responsive a **375 px** y **1440 px** de ambas pantallas, sin
  desbordes horizontales.
  *→ T-ACTA-162* · **Criterio de aceptación**: capturas de evidencia de los 2 anchos.
  **Req**: R-ACTA-054.

- [x] **T-ACTA-179** Accesibilidad: `axe-core` in-page sin hallazgos críticos en ambas pantallas;
  verificación de foco visible y navegación por teclado (incluida la grilla de productos).
  *→ T-ACTA-157, T-ACTA-159* · **Criterio de aceptación**: informe sin hallazgos críticos.
  **Req**: R-ACTA-055.

- [x] **T-ACTA-180** **No regresión**: `./mvnw clean test` deja en verde las suites de
  `01-nucleo-datos`, `02-abm-productos`, `03-login`, `06-turnos` y `08-control-cargas`; y el diff
  **no** toca `Formulario2276` ni `Formulario2276Repositorio`.
  *→ T-ACTA-171…T-ACTA-179* · **Criterio de aceptación**: build en verde y `git diff` sin cambios en
  los dos archivos del núcleo. **Req**: R-ACTA-059.

---

## I. Gobernanza, documentación y cierre

- [x] **T-ACTA-181** **G-POSTMAN**: al no agregarse endpoints REST (D-ACTA-04), confirmar que
  `docs/postman/ATPPuestos-API.postman_collection.json` **no** requiere cambios y dejar constancia
  en `docs/postman/README.md` (§"Cobertura actual" y *Changelog*) de que el módulo `09` es sólo MVC.
  *→ T-ACTA-141* · **Criterio de aceptación**: la colección queda sin diff y el `README` registra la
  decisión. **Req**: R-ACTA-058.

- [x] **T-ACTA-182** **G-SEC**: escaneo de secretos sobre el diff; CSRF activo en el `POST` de alta;
  sin scripts o estilos inline con lógica; sin `fetch`/`XMLHttpRequest` en `actas.js`; los
  `char(n)` no exponen datos fuera de la BD.
  *→ T-ACTA-143, T-ACTA-163* · **Criterio de aceptación**: escaneo sin hallazgos.
  **Req**: R-ACTA-052, R-ACTA-057.

- [ ] **T-ACTA-183** Actualizar `docs/analisis-ATPPuestos.md` con el resultado del G-DATA
  re-ejecutado (T-ACTA-002): esquema físico de `F2276`/`F22761`/`F2276PAS`/**`GES_SUCN`**/
  **`FORMSUCU`**, columnas huérfanas y atributos fórmula de `F2276`; y registrar que la
  **numeración de Actas usa `GES_SUCN.SucUltNum`** (no `FORMSUCU`, no `MAX(F276Id)+1`).
  *→ T-ACTA-002* · **Criterio de aceptación**: el documento refleja la evidencia de
  `INFORMATION_SCHEMA`.

- [ ] **T-ACTA-184** Revisar y resolver los supuestos abiertos SA-ACTA-01…SA-ACTA-16 con los
  responsables; actualizar `spec.md` §6 y `plan.md` §7 con la resolución (o la decisión de postergar
  con motivo y fecha).
  *→ T-ACTA-001…T-ACTA-135* · **Criterio de aceptación**: cada supuesto tiene una resolución
  escrita o una decisión de postergar con motivo y fecha. **SA-ACTA-03 ya está cerrado**; quedan
  obligatorios antes del cierre **SA-ACTA-14** (default de `SucNuId`) y **SA-ACTA-15** (desfase del
  contador).

- [x] **T-ACTA-185** Cerrar la matriz de paridad de `spec.md` §5.2 y `plan.md` §5 con la evidencia
  de cada prueba, y guardar el resumen en Engram (`mem_save`, `topic_key: spec/09-formularios-acta`)
  con los objetos inventariados, las decisiones `D-ACTA-*` y los supuestos abiertos.
  *→ T-ACTA-180…T-ACTA-184* · **Criterio de aceptación**: cada fila de la matriz referencia su
  prueba y su resultado; observación guardada en Engram.

- [x] **T-ACTA-186** Verificar que todos los archivos tocados quedan por debajo de **500 líneas**
  (G4) y que no se versionaron credenciales ni cadenas de conexión (G3).
  *→ T-ACTA-180* · **Criterio de aceptación**: conteo de líneas y escaneo de secretos sin hallazgos.
  **Req**: R-ACTA-060.

---

## Orden de ejecución y paralelismo

```text
T-ACTA-001, T-ACTA-002, T-ACTA-004   (paralelos; T-ACTA-002 bloquea T-ACTA-101..T-ACTA-107)
        │  T-ACTA-003 (verificación de GES_SUCN; SA-ACTA-03 ya cerrado)
        ▼
T-ACTA-101 ──┬── T-ACTA-102 ──┬── T-ACTA-103
             ├── T-ACTA-104 ──┤   (ContadorFormulario   → GES_SUCN)
             ├── T-ACTA-105 ──┤   (RangoManualFormulario→ FORMSUCU)
             └── T-ACTA-106 ──┘   (VersionSistema        → VERSIONE)
        ▼
T-ACTA-111, T-ACTA-112, T-ACTA-113, T-ACTA-114, T-ACTA-115, T-ACTA-116
        ▼
T-ACTA-121 ──┬── T-ACTA-122 ──┬── T-ACTA-126 ──┬── T-ACTA-127
             │                ├── T-ACTA-128 ──┤
             │                ├── T-ACTA-130 ──┤
             │                ├── T-ACTA-131 ──┤
             │                ├── T-ACTA-133 ──┤   (Nuc_CalculoImpIB)
             │                ├── T-ACTA-134 ──┤   (BuscarVersionActual)
             │                └── T-ACTA-135 ──┘   (PPrim_TurnoEstado)
             └── T-ACTA-129 (numeración atómica sobre GES_SUCN)
        ▼
T-ACTA-123 ──┬── T-ACTA-124
             └── T-ACTA-125
        ▼
T-ACTA-141 ──┬── T-ACTA-142 ── T-ACTA-143
             └── T-ACTA-144
        ▼
T-ACTA-151 ── T-ACTA-152 ── T-ACTA-153
T-ACTA-154 ── T-ACTA-155 ── T-ACTA-156 ── T-ACTA-157 ── T-ACTA-158 ── T-ACTA-159
        ▼
T-ACTA-161 ── T-ACTA-162 ── T-ACTA-163
        ▼
T-ACTA-171…T-ACTA-180  →  T-ACTA-181…T-ACTA-186
```

> `T-ACTA-175b` (IT de numeración) corre después de `T-ACTA-129` y `T-ACTA-175`.

### Qué puede hacer `implementador-backend` y `implementador-vistas` en paralelo

| Implementador | Escribe **sólo** en | Puede empezar cuando |
|---|---|---|
| `implementador-backend` | `puestos/domain/**`, `puestos/repository/**`, `puestos/service/**`, `puestos/web/mvc/**` | T-ACTA-002 cerrada (T-ACTA-101…T-ACTA-145) |
| `implementador-vistas` | `templates/puestos/**`, `static/css/actas.css`, `static/js/actas.js` | T-ACTA-144 cerrada y T-ACTA-141 con el contrato de vista definido (T-ACTA-151…T-ACTA-163) |

> Nota de alcance (D-ACTA-20): `GES_SUCN` (numeración) y `FORMSUCU` (rangos manuales) son tablas
> **distintas** con propósitos distintos; ambas entidades las crea el **backend** y las consume sólo
> el servicio.

### Colisiones a evitar

| Archivo | Conflicto | Regla |
|---|---|---|
| `puestos/web/mvc/ActasController.java` | Lo escriben los dos | **Backend** lo crea (T-ACTA-141) con el contrato de vista completo; **vistas** sólo consume el nombre de vista y los nombres de atributo del modelo. |
| `puestos/web/mvc/form/*.java` | Los escriben los dos | **Backend** los crea (T-ACTA-144); **vistas** sólo usa `th:field`. |
| `templates/puestos/**` | Sólo vistas | El backend no crea plantillas. |
| `static/css/tokens.css` | Lo crea `02-abm-productos` (y `08-control-cargas`) | **Vistas** sólo **agrega** tokens `--atp-actas-*`; si el archivo no existe, se avisa al coordinador. |
| `docs/postman/ATPPuestos-API.postman_collection.json` | Sólo backend | Las vistas **no** tocan la colección (G-POSTMAN). |
| `atp-nucleo-compartido/.../domain/Formulario2276.java` y `.../repository/Formulario2276Repositorio.java` | **Nadie** | No se tocan (R-ACTA-059). |

### Reglas de coordinación

| Regla | Detalle |
|---|---|
| 1 | Ninguna tarea de código arranca antes de la **aprobación de `spec.md` + `plan.md`** (puerta G-SPEC). |
| 2 | **T-ACTA-002 es una puerta**: sin el esquema físico confirmado, T-ACTA-101…T-ACTA-107 quedan bloqueadas. |
| 3 | **T-ACTA-003 ya cerró SA-ACTA-03**: la numeración usa `GES_SUCN.SucUltNum` con incremento atómico; **prohibido** usar `MAX(F276Id)+1` (D-ACTA-19/21). |
| 4 | La lógica de negocio va **siempre** en `service/**`; el controller MVC no implementa reglas. |
| 5 | Los **mensajes verbatim** no se tocan, traducen ni capitalizan. |
| 6 | La KB es de **solo lectura**: T-ACTA-001/002/003 son de lectura; nunca compilar, guardar, regenerar ni borrar objetos. |
| 7 | `tasks.md` se actualiza (con evidencia) **por el coordinador**, no por los implementadores. |
| 8 | **Sin DDL**: T-ACTA-107 y T-ACTA-180 verifican que no hay cambios en `spring.jpa.hibernate.ddl-auto`. |
