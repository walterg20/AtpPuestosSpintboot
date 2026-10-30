# Tasks — 03 Login (núcleo compartido)

> **Estado de la puerta G-SPEC**: **APROBADA (revisión v2, 2026-10-22)** — el módulo puede pasar a
> implementación. Las **3 decisiones de seguridad** (**D-LOG-17**, **D-LOG-18**, **D-LOG-19**) fueron
> **aprobadas por el responsable del proyecto** con la alternativa **(a)** en las tres ⇒ la puerta
> **T-007** está **cerrada** y **ninguna** tarea queda bloqueada por aprobación humana.
> **Quedan abiertos, pero asumidos y con tarea**: **SA-LOG-14** (deuda de `permitAll` ⇒ **T-135**),
> **SA-LOG-15** (fórmula de los atributos calculados ⇒ **T-008**) y **SA-LOG-18** (alcance de G4 sobre
> `docs/postman/` ⇒ **T-406**).
> Checklist verificable del módulo `03-login`. Cada tarea es **atómica** y trazable a un requisito de
> `spec.md` (R-LOG-*) o a un punto de `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` (puerta G-SPEC)** — guardarraíl G2.
> **Ninguna tarea está marcada como completada por el analista**: el cierre y la evidencia son del
> coordinador. *(Excepción: **T-007**, cerrada por el coordinador con la aprobación escrita del
> responsable del proyecto, 2026-10-22.)*
> Tareas **B** = backend (`nucleo/domain`, `nucleo/repository`, `nucleo/service`, `nucleo/config`, `nucleo/web/rest`, `nucleo/web/dto`, `pom.xml`).
> Tareas **V** = vistas (`nucleo/web/mvc`, `templates/**`, `static/**`).
> Tareas **Q** = verificación y puertas de calidad.
> Tareas **P** = Postman / documentación.

**Leyenda de dependencias**: `→` significa "depende de". `Req:` = requisito de `spec.md`.
`Plan:` = sección de `plan.md`.

---

## A. Extracción y verificación (previa a la implementación)

- [ ] **T-001** Releer por el MCP `genexus18mcp` (alias `atppuestos`, **sólo lectura**) los 17 objetos del inventario de `spec.md` §3.1: `Login` (WebForm + Events), `Seg_ControlAcceso`, `Seg_ControlAccesoWS`, `Seg_Encripta`, `Seg_BusPer`, `Seg_BusComi`, `Seg_BusResponsable`, `Seg_BusRece`, `PPrim_BusTur`, `SetContext`, `LoadContext`, `ListPrograms`, `IsAuthorized`, `NotAuthorized`, `Home`, `Inicio`, y los SDT `Context` / `SDTEntradaLoginWS` / `SDTSalidaLoginWS`; **confirmar** los 7 mensajes verbatim (§3.4), el orden de comprobaciones (§3.3), los 13 campos del SDT `Context` y los rótulos `Acceso` / `Usuario` / `Contraseña` / `Ingresar`.
  *Sin dependencias.* · **Criterio de aceptación**: el volcado coincide carácter a carácter con `spec.md` §3; cualquier diferencia se corrige en `spec.md` **antes** de T-101.
- [ ] **T-002** **G-DATA — puerta de T-1xx.** Con `INFORMATION_SCHEMA.COLUMNS` + `KEY_COLUMN_USAGE` + `REFERENTIAL_CONSTRAINTS` de `Rentas` (**sólo `SELECT`**), determinar el nombre físico y el esquema exactos de: `SCRTY`, `PERSONAS`, `PUEPPRIM`, `RECEPTOR`, `RECEPTO1`, la tabla raíz de comisiones, la tabla de integrantes y `PPRIM_TU`; confirmar tipo, nulabilidad y PK de cada columna usada, y enumerar las columnas huérfanas.
  *Sin dependencias.* · **Criterio de aceptación**: queda escrito el nombre físico real de las 8 tablas (en particular **`COMISIO` vs `COMISION`** y la existencia de **`COMISIO1`**), la PK real de `PPRIM_TU` y la existencia de `CPCFchDes` / `CPCFchHas` / `CPCHorHas`. **Si la BD no responde**, T-002 queda bloqueado y **ninguna entidad de comisiones/turnos puede escribirse** (SA-LOG-08/09).
  > **Evidencia disponible (revisión v2, 2026-10-22)**: el G-DATA fue **re-ejecutado** y **SA-LOG-08/SA-LOG-09 están cerrados**: la tabla raíz es **`COMISION`** (PK (`PueCod`,`CPCId`), 14 columnas, 3 120 filas), **`COMISIO1`** existe con **3** columnas (PK (`PueCod`,`CPCId`,`CPCPerId`), 7 238 filas), `CPCFchDes`/`CPCFchHas` (`datetime`) y `CPCHorDes`/`CPCHorHas` (`int`) existen, y la PK de `PPRIM_TU` es (`SucNroId`,`SucTipo`,`TurNroId`). Evidencia completa en `spec.md` §3.1.1 y `plan.md` §5.3. **Hallazgo adicional**: `CPCPerNyA` y `CPCResNyA` **no son columnas** (atributos calculados) ⇒ JOIN obligatorio (D-LOG-20). *El cierre formal de T-002/T-003 lo hace el coordinador con esta evidencia.*
- [x] **T-007** **Puerta de aprobación humana (revisión v2) — CUMPLIDA (2026-10-22).** El **responsable del proyecto** aprobó por escrito las 3 decisiones de seguridad, todas con la **alternativa (a)**: **D-LOG-17** (política CSRF y autenticación de `/api/**`, `plan.md` §4.5), **D-LOG-18** (materialización anticipada del token CSRF, §4.6) y **D-LOG-19** (nivel de autenticación de `/api/v1/**`, §4.7).
  *→ T-002, T-003* · **Criterio de aceptación (cumplido)**: las 3 decisiones tienen alternativa aprobada y fechada; `plan.md` §4.5/§4.6/§4.7 quedan marcadas **APROBADA** y `spec.md` pasa a **APROBADA (revisión v2)** sin pendientes de aprobación. **Desbloquea**: T-126, T-127, T-128, T-311, T-312, T-313, T-404. **Req**: R-LOG-039, R-LOG-041, R-LOG-042. **Plan**: SA-LOG-16, SA-LOG-17. **Cierre registrado por el coordinador** con la aprobación escrita del responsable del proyecto (2026-10-22).
- [ ] **T-008** **Cerrar SA-LOG-15 — fórmula de los atributos calculados `CPCPerNyA` / `CPCResNyA`.** (a) Contrastar con el legacy (runtime GeneXus o el responsable del módulo `Comisiones`) la fórmula de ambos atributos; (b) verificar en la BD que la hipótesis `PERSONAS.PerNomyApe` (JOIN por `CPCPerId` / `CPCResId`) es consistente: para una persona con **dos** comisiones vigentes, el orden por `PERSONAS.PerNomyApe` debe dar un resultado **único y determinista**; (c) documentar la resolución en `spec.md` §3.9 y `plan.md` §6 (SA-LOG-15).
  *→ T-002* · **Criterio de aceptación**: `spec.md` §3.9 dice si la fórmula es el JOIN a `PERSONAS` o otro origen, con la evidencia; el IT de orden (T-133/T-311) tiene un resultado esperado **único**. **Req**: R-LOG-035, R-LOG-036. **Plan**: D-LOG-20, SA-LOG-15.
- [ ] **T-003** Verificar la conectividad con `devmssqlatp.ecom.com.ar:1433` desde el entorno de build/CI y dejar constancia del resultado (DNS, TCP y login). En la sesión de redacción de `plan.md` el host **no resuelve por DNS** («el nombre DNS no existe»).
  *Sin dependencias.* · **Criterio de aceptación**: T-002 puede ejecutarse, o queda documentado el bloqueo con su plan de contingencia.
- [ ] **T-004** Inventariar el estado real del núcleo: presencia/ausencia de `spring-boot-starter-security` y de `spring-boot-starter-thymeleaf` en el `pom.xml`; existencia de `web/mvc/**`, `templates/layout/base.html`, `templates/nucleo/**`, `static/css/tokens.css`, `static/css/nucleo.css`, `static/js/**`; y el estado de `app.auditoria.usuario` en `application.yml`.
  *Sin dependencias.* · **Criterio de aceptación**: el inventario queda en `plan.md` §1.2 y §4.4, y las tareas que dependen de un archivo existente se marcan como **dependencia de `02-abm-productos`**.
- [ ] **T-005** Confirmar la forma del canal `/api/**`: `ApiError` (`{code, message}`) y las reglas del `GlobalExceptionHandler` global, para que el `400` del login WS use los mismos `code` que el resto de la API.
  *Sin dependencias.* · **Criterio de aceptación**: la tabla de errores de `plan.md` §4.1 usa los `code` reales del advice.
- [ ] **T-006** Corregir la documentación oficial con los hallazgos de esta sesión: (a) `00-alcance/plan.md` §5 (la fila `templates/puestos/` → login/home/not-authorized pasa al **núcleo**, D-LOG-01/02); (b) `spec.md` §3.9 (nota sobre `CPCPerNyA`/`PerNomyApe`: **sí son columnas** de `COMISIO1`/`PERSONAS` según el DDL de la KB ⇒ el ordenamiento usa el nombre **almacenado** y no exige `LEFT JOIN` a `PERSONAS`); (c) `spec.md` §7 R-4, en el mismo sentido.
  *→ T-001, T-002* · **Criterio de aceptación**: los tres documentos quedan consistentes con `plan.md` §10 R-4/R-9 y con la evidencia de T-001/T-002.

---

## B. Backend

- [ ] **T-101** Añadir `spring-boot-starter-security` al `pom.xml` de `atp-nucleo-compartido` (y `spring-boot-starter-thymeleaf` si `02-abm-productos` no lo dejó; **sin** `thymeleaf-layout-dialect`, SA-P09 / D-07 de `00-alcance`). Verificar que el prefijo de resolución de plantillas sigue siendo `classpath:/templates/`.
  *→ T-001, T-004* · **Criterio de aceptación**: `./mvnw -pl atp-nucleo-compartido dependency:tree` muestra las dos dependencias; una plantilla de prueba resuelve. **Req**: R-LOG-031.
- [ ] **T-102** Crear la entidad `nucleo.domain.Usuario` (`@Table(name="SCRTY")`) con **sólo** las 4 columnas leídas (`UsuId` PK `char(10)`, `UsuClave` `char(10)`, `UsuNomYApe` `char(35)`, `UsuIngreso` `smallint`), con `@ColumnTransformer(read="RTRIM(…)")` en las `char` y `UsuarioRepository` con `findByUsuId` usando **`UPPER()` explícito en ambos lados** (`UPPER(UsuId) = UPPER(:usuarioEncrip)`).
  *→ T-002* · **Criterio de aceptación**: el test verifica que un `UsuId` real con `b` minúscula (`YOLOFFG` → `(252;;:bbb`) se encuentra, y que la consulta generada contiene `upper(`. **Req**: R-LOG-004. **Plan**: D-LOG-05, D-LOG-14, §5.1.
- [ ] **T-103** Crear las entidades y repositorios de contexto: `Persona` (`PERSONAS`: `PerId` PK, `PerNomyApe`, `PerCodUsu`), `Puesto` (`PUEPPRIM`: `PueCod` PK, `PueDesc`, `PueIPRed`), `Receptor` (`RECEPTOR`: `ReceId` PK, `ReceDsc`), `ReceptorPersona` (`RECEPTO1`: PK compuesta `ReceId`+`PerId`) y `TurnoPPrim` (`PPRIM_TU`: `SucNroId`, `SucTipo`, `TurNroId`, `TurEstado`, `TurCPCId`). Los repositorios traducen **exactamente** las consultas lógicas de `spec.md` §3.9, incluido el orden (`Seg_BusPer` → mayor `PerNomyApe`; `Seg_BusRece` → primera fila por `PerId`; `PPrim_BusTur` → primera fila que cumpla).
  *→ T-002* · **Criterio de aceptación**: `PersonaRepositoryIT` resuelve un usuario real; `ReceptorRepositoryIT` devuelve la **primera** fila por `PerId` (no la última); `TurnoRepositoryIT` devuelve la primera fila activa. **Req**: R-LOG-008, R-LOG-011, R-LOG-012, R-LOG-014.
- [ ] **T-104** Crear `nucleo.service.VerificadorDeClave` (interfaz: `codificar`, `coincideCon`, `decodificar`) y su implementación `nucleo.service.CodificadorSegEncripta` con el alfabeto de **66** caracteres, la regla espejo **`67 − origen`**, el recorrido de posiciones 1..10, la normalización a mayúsculas y el **descarte** de los caracteres fuera del alfabeto.
  *→ T-001* · **Criterio de aceptación**: los vectores de `spec.md` §3.2 se reproducen exactamente (`"ABC123"`→`"@?>PON"`, `"1234567890"`→`"PONMLKJIHQ"`, `"Z"`→`"a"`, `" "`→`"b"`, `"ÑUEZ"`→ longitud 3). **Req**: R-LOG-001, R-LOG-002, R-LOG-003. **Plan**: D-LOG-03, D-LOG-09.
- [ ] **T-105** Crear `nucleo.service.MensajesAcceso` con la **tabla única** de los 7 pares (código → mensaje **verbatim**, con las erratas "esta" y "Receptoria") y su test parametrizado que falla si un texto difiere **carácter a carácter** del legacy.
  *→ T-001* · **Criterio de aceptación**: el test compara contra las cadenas literales de `spec.md` §3.4, no contra una copia del propio código. **Req**: R-LOG-018, R-LOG-019. **Plan**: D-LOG-04.
- [ ] **T-106** Crear los value objects: `Credencial` (usuario, clave, tipo), `ResultadoAcceso` (código, contexto, sucursal, comision), `ContextoSesion` (**13 campos**, con los 3 no cargados en su valor por defecto), `PerfilAcceso` (`INTERNO`, `WS`), `DatosPeticion` (IP del cliente) y `ProgramaAutorizado` (nombre, descripción, enlace).
  *→ T-105* · **Criterio de aceptación**: `ContextoSesion` expone los 13 nombres del SDT `Context` (verificación por reflexión contra la lista de `spec.md` §3.5). **Req**: R-LOG-016. **Plan**: D-LOG-06, D-LOG-08.
- [ ] **T-107** Implementar `nucleo.service.ServicioAcceso` — **perfil INTERNO**, verificación de credenciales en el **orden exacto** de `spec.md` §3.3: codificar usuario → buscar en `SCRTY` (sin match ⇒ 2) → `UsuClave` vacía ⇒ 2 → codificar clave y comparar (`RTRIM` en ambos lados) ⇒ 2 → `UsuIngreso = 1` ⇒ 5 → resolver Persona (sin match ⇒ 3). **El servicio no lanza excepciones**: devuelve el código.
  *→ T-102, T-104, T-106* · **Criterio de aceptación**: el test invoca el servicio con credenciales inválidas y verifica que **no** se propaga ninguna excepción y que el resultado es un código; los 6 casos (2 por usuario inexistente, 2 por clave, 3 sin persona, 5 por `UsuIngreso`) pasan. **Req**: R-LOG-004…R-LOG-008, R-LOG-019, R-LOG-021.
- [ ] **T-108** Implementar `nucleo.service.ServicioAcceso` — **resolución del contexto operativo**: rama `'PUE'` (comisión activa como integrante y, si no, como responsable; `CPCFchHas = hoy` y `CPCHorHas < hora` ⇒ se descarta; responsable con defaults `0` / `Sin Comisión`; puesto y `PueIPRed`), rama receptoría (`ReceId = -1` ⇒ `No Existe Receptoria`), turno (`PPRIM_TU`, `0` si no hay), rama `"&SucNroId >= 0"` (contexto de 10 campos) y rama **"fue responsable"** (contexto parcial de 4 campos ⇒ código 0; si no ⇒ código 4).
  *→ T-103, T-107* · **Criterio de aceptación**: los tests cubren los 4 caminos (PUE con comisión, PUE sin comisión ⇒ 4, REC con y sin receptoría, PUE por historia de comisión ⇒ 0 con contexto parcial) y verifican los **10 campos** del contexto. **Req**: R-LOG-009…R-LOG-016.
- [ ] **T-109** Crear `nucleo.web.mvc.GestorContextoSesion` (guardar/recuperar `ContextoSesion` en la `HttpSession` bajo la clave **`"context"`**) y `nucleo.service.UsuarioAutenticado` (record `usuario` + `contexto`, con `getName()` ⇒ usuario en mayúsculas).
  *→ T-106, T-108* · **Criterio de aceptación**: el test verifica que la clave de sesión es exactamente `"context"` (paridad con `SetContext`) y que el contexto sobrevive entre dos peticiones. **Req**: R-LOG-017. **Plan**: D-LOG-08.
- [ ] **T-110** Crear `nucleo.service.ServicioAutorizacion` que arma la lista de programas (`WWPPrim_Turnos` / *Turnos de Comisiones*, `WWTDetProdPri` / *Buscar Guias*) y la filtra con un `VerificadorAutorizacion.isAuthorized(nombre)` **desacoplado** cuya implementación actual devuelve siempre `true` (paridad con `IsAuthorized`).
  *→ T-001* · **Criterio de aceptación**: el test verifica los 2 programas con su descripción, y que un programa no autorizado queda fuera cuando se inyecta un verificador que devuelve `false`. **Req**: R-LOG-025. **Plan**: D-LOG-13, SA-LOG-02, SA-LOG-11.
- [ ] **T-111** Crear `nucleo.service.VerificadorIpCliente` con **un único método** `esCompatible(String ip, String prefijo)` que encapsula la comparación por **prefijo de texto** (`startsWith`), con prefijo vacío ⇒ **no restringe**, y su test dedicado con los 3 casos (coincide, no coincide, prefijo vacío).
  *→ T-001* · **Criterio de aceptación**: el test documenta la **semántica intencional** elegida (SA-LOG-01) y deja constancia de que la semántica literal de `IndexOf` sigue sin confirmar. **Req**: R-LOG-013. **Plan**: D-LOG-10.
- [ ] **T-120** Crear `nucleo.web.dto.AuthLoginRequestDto` (`usuario`, `pass`, `tipo` con `@NotBlank`/`@Size`, más `@JsonAlias` a `Usuario`/`Pass`/`Tipo`) y `AuthLoginResponseDto` (`error`, `sucursal`, `comision`), y `nucleo.web.rest.AuthController` con `POST /api/v1/auth/login` que invoca el **perfil WS** y devuelve **siempre HTTP 200**.
  *→ T-107, T-108, T-105* · **Criterio de aceptación**: `MockMvc` verifica el JSON exacto `{"error":0,"sucursal":10,"comision":123}` y `{"error":2,"sucursal":0,"comision":0}`; un body inválido responde `400` con el `code` del advice. **Req**: R-LOG-027. **Plan**: D-LOG-06, §4.1, SA-LOG-12.
- [ ] **T-121** Suite de contrato del canal WS: que la respuesta **no** incluye `Set-Cookie`, que una segunda llamada **no** hereda estado, que el código 6 **no** es producible, que el usuario `'REC'` sin receptoría devuelve `error=0` con `sucursal=0` y `comision=0`, y que el body con alias legacy (`Usuario`/`Pass`/`Tipo`) se acepta.
  *→ T-120* · **Criterio de aceptación**: los 5 tests pasan. **Req**: R-LOG-028, R-LOG-029. **Plan**: §4.1, §4.2.
- [ ] **T-122** Crear `nucleo.web.mvc.login.LoginForm` (form-backing object: `usuario`, `clave`, `tipo` con las validaciones de `plan.md` §4.3; **nunca** renderizar la clave) y `nucleo.web.mvc.AuthMvcController` con `GET /login`, `POST /login`, `GET /home`, `GET /not-authorized` y `GET /` (⇒ `302` a `/home`). El `POST` invoca `ServicioAcceso` con el **perfil INTERNO** y la IP resuelta por el adaptador (`X-Forwarded-For` ⇒ si vacío, `getRemoteAddr()`).
  *→ T-107, T-108* · **Criterio de aceptación**: `MockMvc` devuelve 200 + nombre de vista `nucleo/login` en el GET; el controller **no** contiene ninguna referencia a `RestTemplate`/`WebClient`; la IP se resuelve en el adaptador, no en el servicio. **Req**: R-LOG-022, R-LOG-024. **Plan**: D-LOG-15, D-LOG-16, §3.2.
- [ ] **T-123** Implementar la cookie `SUCURSAL` **en el controller** (D-LOG-15): escritura **sólo** en el camino de éxito, con el valor del tipo de acceso, `HttpOnly`, `SameSite=Lax`, `path=/` y **sin `Max-Age`**; lectura en `GET /login` para precargar el selector de tipo.
  *→ T-122* · **Criterio de aceptación**: el login exitoso emite `Set-Cookie: SUCURSAL=PUE; HttpOnly; Path=/; SameSite=Lax` **sin** `Max-Age`; el fallido **no** emite cookie; un `GET /login` posterior trae el tipo precargado. **Req**: R-LOG-023. **Plan**: D-LOG-07, SA-LOG-07.
- [ ] **T-124** Registrar la autenticación en la sesión: `SecurityContextHolder` + `SecurityContextRepository` (sesión HTTP), **mitigación de fijación de sesión** (`changeSessionId`), y `POST /logout` que invalida la sesión y borca la cookie `SUCURSAL`.
  *→ T-109, T-122, T-125* · **Criterio de aceptación**: el `JSESSIONID` **cambia** entre antes y después del login; el logout invalida la sesión. **Req**: R-LOG-017, R-LOG-031. **Plan**: §3.7.
- [ ] **T-125** Crear `nucleo.config.SeguridadConfig` con la `SecurityFilterChain`: `permitAll` para `/login`, `/api/v1/auth/login`, `/error` y los recursos estáticos; `authenticated()` para el resto de las **rutas MVC**; **`permitAll` para `/api/v1/**` (D-LOG-19(a))**; **`LoginUrlAuthenticationEntryPoint`** (⇒ `302` a `/login`); **CSRF habilitado en las rutas MVC con descarte explícito en todo `/api/**` (D-LOG-17(a))**; `sessionFixation().changeSessionId()`. *(El detalle fino de la política CSRF por familia de rutas y de la materialización anticipada del token se implementa en T-126 y T-128.)*
  *→ T-101, T-122* · **Criterio de aceptación**: `GET /home` sin sesión ⇒ `302` a `/login`; `POST /login` **sin** token CSRF ⇒ `403`; `POST /login` **con** token ⇒ `200`/`302`; `POST /api/v1/auth/login` **sin** token ⇒ `200`; `POST /api/v1/productos` **sin** token ⇒ **no** es rechazado por CSRF (código de negocio, **no** 403/401). **Req**: R-LOG-028, R-LOG-031, R-LOG-039, R-LOG-041. **Plan**: D-LOG-17(a), D-LOG-19(a), §3.7, SA-LOG-14, SA-LOG-16.
- [ ] **T-130** Derivar el usuario auditor de la escritura del `Context.Usuario` de la sesión, en lugar del valor por defecto `SISTEMA` de `application.yml` (`app.auditoria.usuario`).
  *→ T-109, T-124* · **Criterio de aceptación**: el test verifica que una escritura posterior a un login usa `Context.Usuario` y **no** `SISTEMA`; sin sesión ⇒ sigue `SISTEMA`. **Req**: R-LOG-032. **Plan**: §5.5.
- [ ] **T-131** Verificar que **todos** los repositorios del módulo son de sólo lectura (`@Transactional(readOnly=true)`), que el `diff` **no** toca `spring.jpa.hibernate.ddl-auto` y que no hay DDL, migraciones, `schema.sql`/`data.sql`, Flyway ni Liquibase.
  *→ T-102, T-103, T-120* · **Criterio de aceptación**: el `git diff` no contiene `CREATE TABLE`/`ALTER TABLE` ni referencias a migraciones; la BD queda sin cambios al terminar la suite. **Plan**: G5, §5.5.
- [ ] **T-126** *(revisión v2)* Implementar en `nucleo.config.SeguridadConfig` la **política CSRF explícita por ruta** aprobada en **D-LOG-17(a)**: **exigido** en las rutas MVC con sesión (`/login`, `/productos`, `/logout`, `/home`, `/not-authorized`) y **descartado explícitamente en todo `/api/**`**; y el `AccessDeniedHandler`/`AuthenticationEntryPoint` que devuelvan **403** (token CSRF ausente) en las rutas MVC, **nunca** 200 ni HTML de error.
  *→ T-125* · **Criterio de aceptación**: la configuración tiene una regla CSRF **explícita** por familia de rutas (no el comportamiento implícito del filtro por defecto); un `POST /login` sin token ⇒ **403**; un `POST /api/v1/productos` **sin** token ⇒ **NO** es rechazado por CSRF (responde el código de negocio, **no** 403, **no** 401, **no** HTML); un `POST /api/v1/auth/login` sin token ⇒ **200**. **Req**: R-LOG-039, R-LOG-041. **Plan**: D-LOG-17(a), §4.5, SA-LOG-16. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-127** *(revisión v2)* Implementar el esquema de autenticación de `/api/**` aprobado en **D-LOG-19(a)**: **`permitAll`** en este módulo, **sin** credenciales y **sin** cadena de autenticación nueva; y **documentar** que la API no toma autoridad de la cookie de sesión (ningún endpoint `/api/**` lee la `HttpSession` para autorizar).
  *→ T-125* · **Criterio de aceptación**: el esquema queda implementado **exactamente** como la alternativa aprobada (D-LOG-19(a)); **ninguna** credencial queda versionada (**G3**); la colección Postman se puede ejecutar **sin** credenciales y **sin** token CSRF; un request a `/api/v1/**` no devuelve `Set-Cookie: JSESSIONID`. **Req**: R-LOG-040, R-LOG-041. **Plan**: D-LOG-19(a), §4.7, SA-LOG-14. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-128** *(revisión v2)* Implementar el mecanismo de materialización anticipada del token CSRF aprobado en **D-LOG-18(a)**: conservar `HttpSessionCsrfTokenRepository` y **materializar el token temprano** (filtro propio que fuerza `csrfToken.getToken()` **antes** del render, o `setRequestAttribute` con `CsrfTokenRequestAttributeHandler`), **sin** migrar a `CookieCsrfTokenRepository`, **sin** alterar el contrato del formulario (`th:action` sigue emitiendo `_csrf`), **sin** cambiar la cookie `SUCURSAL` y **sin** tocar el HTML/ARIA de las vistas del módulo 02.
  *→ T-125* · **Criterio de aceptación**: un `GET` de una ruta HTML **sin cookie de sesión** devuelve **200** con `<form>` y `_csrf`; el log **no** registra `IllegalStateException: Cannot create a session after the response has been committed`; el repositorio de token sigue siendo `HttpSessionCsrfTokenRepository`; el HTML renderizado es **idéntico** al previo; `ProductoMvcCsrfTest` y el test de los 12 rótulos del módulo 02 siguen verdes. **Req**: R-LOG-042, R-LOG-043. **Plan**: D-LOG-18(a), §4.6, SA-LOG-17. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-129** *(revisión v2)* Fijar de forma explícita el dialecto de Hibernate (o corregir los parámetros de la URL de conexión) para que el arranque **no** emita `HHH000511`, **sin** tocar `spring.jpa.hibernate.ddl-auto` (sigue `none`) y sin alterar el comportamiento observable contra `Rentas`.
  *→ T-101* · **Criterio de aceptación**: el log de arranque **no** contiene `HHH000511`; la suite de IT completa sigue verde con los mismos resultados; el `git diff` no toca la sección `ddl-auto`. **Req**: R-LOG-046. **Plan**: D-LOG-21, §5.4.
- [ ] **T-132** Crear las entidades `nucleo.domain.Comision` (`@Table(name="COMISION")`) y `nucleo.domain.ComisionIntegrante` (`@Table(name="COMISIO1")`) con sus **PK compuestas** completas (`PueCod`+`CPCId` y `PueCod`+`CPCId`+`CPCPerId`), mapeando **sólo** las columnas que el login lee (`CPCFchDes`, `CPCFchHas`, `CPCHorDes`, `CPCHorHas`, `CPCCierre`, `CPCResId`) y **sin** declarar `CPCPerNyA` ni `CPCResNyA` (no existen: D-LOG-20).
  *→ T-002* · **Criterio de aceptación**: la inspección del mapeo muestra los 8 `@Table`/`@Id` correctos y **ninguna** columna `CPCPerNyA`/`CPCResNyA`; un IT persiste y recupera una fila real de cada entidad y la consulta generada incluye **todas** las columnas de la PK en el `WHERE`. **Req**: R-LOG-033, R-LOG-034, R-LOG-035. **Plan**: D-LOG-20, §5.3.
- [ ] **T-133** Crear `nucleo.repository.ComisionRepository` con las consultas lógicas de `spec.md` §3.9, **incluidos los JOIN** a `PERSONAS` (por `CPCPerId` y por `CPCResId`) que resuelven los atributos calculados, y el **orden determinista** del legacy (`defined by CPCPerNyA` ⇒ mayor `PERSONAS.PerNomyApe`; `defined by CPCCierre` en `Seg_BusResponsable`); y el JOIN de `Seg_BusRece` a `RECEPTOR` por `ReceId` para `ReceDsc`.
  *→ T-132, T-103, T-008* · **Criterio de aceptación**: (a) el IT resuelve el **nombre** del responsable (no vacío ni `Sin Comisión`); (b) el IT de orden, para una persona con dos comisiones vigentes, devuelve la de **mayor** `PERSONAS.PerNomyApe` y no la primera por `CPCId`; (c) `Seg_BusRece` devuelve `ReceDsc` de `RECEPTOR`; (d) `CPCHorHas` se compara como **número** y el descarte sólo aplica con `CPCFchHas = hoy`. **Req**: R-LOG-009, R-LOG-010, R-LOG-011, R-LOG-035, R-LOG-036, R-LOG-038. **Plan**: D-LOG-20, §3.9.
- [ ] **T-135** *(revisión v2)* **Deuda SA-LOG-14 — endurecimiento posterior de `/api/v1/**` (FUERA del alcance de 03-login, no bloquea este módulo).** Diseñar e implementar un token propio de la aplicación para los endpoints de datos de `/api/v1/**`, que viaje **siempre** en la cabecera **`Authorization`** (nunca en cookie ⇒ CSRF inaplicable por construcción) y **sin** credenciales versionadas en el repositorio (**G3**), de modo que la superficie expuesta por **D-LOG-19(a)** quede cerrada sin romper G-POSTMAN.
  *→ T-127 (seguimiento de la deuda)* · **Criterio de aceptación**: existe un documento de diseño (en `openspec/features/00-alcance/` o el módulo de seguridad que corresponda) con el esquema del token, su ciclo de vida y el plan de migración de la colección Postman; la deuda **SA-LOG-14** y el riesgo **R-10** quedan con dueño y fecha. **Req**: R-LOG-040, R-LOG-041. **Plan**: D-LOG-19(a), §4.7, SA-LOG-14, R-10. **Nota**: no bloquea la implementación ni el cierre de 03-login; es la contrapartida explícita de la deuda asumida.
- [ ] **T-136** *(revisión v2)* **Verificar la mitigación acordada por la ausencia de CSRF en `/api/**`** (R-LOG-041): (a) ningún endpoint de `/api/**` lee la `HttpSession` para autorizar (⇒ no hay credencial ambiental ⇒ el vector CSRF no es explotable); (b) `JSESSIONID` y `SUCURSAL` se emiten con `SameSite=Lax`; (c) la configuración deja constancia de que la credencial futura de la API viajará en `Authorization` (**nunca** en cookie).
  *→ T-126, T-127* · **Criterio de aceptación**: (a) test que verifica que un `POST /api/v1/**` no devuelve `Set-Cookie: JSESSIONID` ni crea sesión de negocio; (b) test que verifica `SameSite=Lax` en `JSESSIONID` y en `SUCURSAL`; (c) revisión de la configuración con la regla documentada. **Req**: R-LOG-041. **Plan**: D-LOG-17(a), §4.5 (tabla de mitigación). **Desbloqueada** por T-007 (2026-10-22).

---

## C. Vistas

- [ ] **T-201** Crear `templates/nucleo/login.html`: título de la pantalla, los **3 rótulos verbatim** (`Acceso`, `Usuario`, `Contraseña`), el control de contraseña como `type="password"`, el botón **`Ingresar`**, el campo oculto `_csrf` (vía `th:action`) y el banner de error junto al campo de contraseña con la clase visual de error (equivalente a `ForeColor="Red"`).
  *→ T-122* · **Criterio de aceptación**: el HTML renderizado contiene `Acceso`, `Usuario`, `Contraseña` e `Ingresar` **en ese orden**, el campo `_csrf` y el `type="password"`; el mensaje de error se renderiza dentro del bloque de error. **Req**: R-LOG-020, R-LOG-022. **Plan**: §4.3, D-LOG-02.
- [ ] **T-202** Implementar el selector de **Acceso** (`<select th:field="*{tipo}">` con las opciones `PUE` y `REC`, sin opción vacía), la precarga del valor desde la cookie `SUCURSAL` y `static/js/login.js` (toggle de contraseña + `autofocus`).
  *→ T-123* · **Criterio de aceptación**: el `login.js` **no** contiene `fetch`/`XMLHttpRequest`/`eval` ni lógica de negocio; no lee la cookie `SUCURSAL` (es `HttpOnly`); el toggle muestra y oculta la clave. **Plan**: D-LOG-07, `docs/design.md` §8.
- [ ] **T-203** Implementar los estados de la vista de login: estado inicial (banner vacío), estado de error (`mensajeError` verbatim junto a la contraseña) y estado de errores de binding (mensaje junto a cada campo, `Ingresar` deshabilitado cuando `tieneErrores`); **conservar** el usuario y el tipo enviados, **nunca** la clave.
  *→ T-122* · **Criterio de aceptación**: tras un login fallido el HTML contiene el `usuario` y el `tipo` enviados, **no** contiene el valor de la clave, y `Ingresar` queda deshabilitado si hay errores de binding. **Req**: R-LOG-020, R-LOG-021.
- [ ] **T-204** Crear `templates/nucleo/home.html`: los datos del contexto (los 10 campos cargados, con sus rótulos), la lista de programas autorizados (nombre + descripción) y el texto de `Inicio` **verbatim** (incluida la falta de espacio antes de "Otra").
  *→ T-110, T-109* · **Criterio de aceptación**: el HTML contiene la cadena de `Inicio` carácter a carácter; el contexto refleja los 10 campos de la sesión. **Req**: R-LOG-016, R-LOG-024, R-LOG-025.
- [ ] **T-205** Crear `templates/nucleo/not-authorized.html` (mensaje `You are not authorized to do this action`) y `templates/nucleo/fragments/menu-programas.html` (lista de programas, reutilizable; estado `empty` ⇒ `Sin programas disponibles` + link `Recargar`).
  *→ T-110* · **Criterio de aceptación**: el test verifica la ruta `/not-authorized`, el nombre de vista y el texto **exacto** del mensaje; el fragmento renderiza los 2 programas. **Req**: R-LOG-025, R-LOG-026. **Plan**: SA-LOG-10.
- [ ] **T-206** Verificar el CSS: reutilizar `static/css/tokens.css` y `static/css/nucleo.css` (creados por `02-abm-productos`); **ningún** literal de color o tipografía fuera del archivo de tokens; agregar sólo las clases nuevas que hagan falta (`.atp-login`, `.atp-alert--error`).
  *→ T-004, T-201* · **Criterio de aceptación**: `grep` sobre `static/css/**` no encuentra literales hexadecimales ni nombres de fuente fuera de `tokens.css`. **Plan**: §4.4, `docs/design.md` §2.2.
- [ ] **T-207** Verificar el comportamiento responsive de las 3 vistas a 375 px y 1440 px (apilado del formulario, scroll horizontal de la lista de programas).
  *→ T-201, T-204* · **Criterio de aceptación**: inspección visual en los 2 anchos sin desbordes ni cortes de texto.
- [ ] **T-208** Aplicar los requisitos de accesibilidad AA: `<label for>` en cada control, `aria-live` en el banner de error, foco visible, contraste ≥ 4,5:1 y `autocomplete`/`autofocus` en los campos de credenciales.
  *→ T-201, T-204, T-205* · **Criterio de aceptación**: `axe-core` sin hallazgos críticos en los 3 estados (inicial, error, home). **Plan**: `docs/design.md` §7.
- [ ] **T-209** Verificar (o crear, si `02-abm-productos` no lo dejó) `templates/layout/base.html` con el que las 3 vistas se renderizan, y que enlaza `tokens.css`, `nucleo.css` y `js/login.js` sin recursos externos.
  *→ T-004* · **Criterio de aceptación**: las 3 vistas renderizan dentro del layout; **ninguna** referencia a CDN ni a un recurso remoto.

---

## D. Verificación y puertas de calidad

- [ ] **T-301** Suite de vistas (`@WebMvcTest` + `MockMvc`): render del login con los 3 rótulos verbatim y el token CSRF, tipo precargado desde la cookie, `Usuario inválido` **sin** invocar el servicio (Mockito), mensaje de código ≠ 0 junto a la contraseña, cookie `SUCURSAL` presente/ausente, redirección a `/home`, protección de `/home` (302), render de `Home` con el contexto y los programas, `NotAuthorized`, `POST /logout`, `POST /login` sin CSRF ⇒ 403. **≥ 24 pruebas nuevas**.
  *→ T-121, T-122, T-123, T-124, T-125, T-201, T-204, T-205* · **Criterio de aceptación**: `./mvnw clean test` en verde y **sin regresión** sobre las suites de `01-nucleo-datos` y `02-abm-productos`. **Req**: R-LOG-020, R-LOG-022…R-LOG-024, R-LOG-026, R-LOG-031. **Plan**: §8.2.
- [ ] **T-302** Pruebas de los caminos de error de la UI: los **6** códigos alcanzables (2, 3, 4, 5, 6 y el 0) renderizados **verbatim**, `Usuario inválido` sin tocar la BD, y el servicio **no** invocado en ese caso.
  *→ T-107, T-108, T-201* · **Criterio de aceptación**: el test compara el texto renderizado contra las cadenas literales de `spec.md` §3.4. **Req**: R-LOG-018, R-LOG-019, R-LOG-020, R-LOG-021.
- [ ] **T-303** Test parametrizado de los **7** pares (código, mensaje) de `MensajesAcceso` **más** el test de contrato JSON del endpoint WS (éxito y fracaso) y la verificación de que el **código 1** no es producible por `ServicioAcceso`.
  *→ T-105, T-120* · **Criterio de aceptación**: el test falla si un texto difiere carácter a carácter; el WS devuelve `200` con `{"error":2,…}` para credenciales inválidas. **Req**: R-LOG-018, R-LOG-027.
- [ ] **T-304** Pruebas de integración contra `Rentas` (`ATP_DB_IT=true`, con rollback): login de un usuario real (usuario cuya `UsuId` cifrada contenga `b` minúscula), login con clave incorrecta ⇒ 2, usuario sin `PERSONAS` ⇒ 3, usuario con `UsuIngreso = 1` ⇒ 5, usuario de tipo `'REC'` sin receptoría, resolución del contexto (10 campos), persistencia del contexto en la sesión entre dos peticiones, y los **552** `UsuId` descifrados sin excepción.
  *→ T-102, T-103, T-108, T-109, T-304* · **Criterio de aceptación**: `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` en verde y la BD **sin cambios** al terminar. **Req**: R-LOG-001…R-LOG-017, R-LOG-025, R-LOG-029. **Plan**: §9.1, §9.2.
- [ ] **T-305** Verificación de BD y de auditoría: confirmar que el login **no** escribe (conteos de filas antes/después en las 8 tablas), que `ddl-auto` sigue en `none`, que el usuario auditor de una escritura posterior al login es `Context.Usuario` (no `SISTEMA`), y que el **orden** de `Seg_BusComi` usa el nombre **almacenado** (`CPCPerNyA`).
  *→ T-130, T-131, T-108* · **Criterio de aceptación**: los conteos son idénticos antes y después; la auditoría usa el usuario de la sesión. **Req**: R-LOG-032. **Plan**: §9.2, §9.3, R-4/R-9.
- [ ] **T-306** Prueba de navegador real: login fallido (mensaje junto a la contraseña), login exitoso (redirección a `/home`, cookie `SUCURSAL` presente, `JSESSIONID` **cambiado**), navegación a `Home`, `NotAuthorized` y `logout`; captura de evidencia.
  *→ T-301* · **Criterio de aceptación**: evidencia del flujo completo; el `SUCURSAL` **no** es visible por `document.cookie` (es `HttpOnly`). **Req**: R-LOG-023, R-LOG-024, R-LOG-031.
- [ ] **T-307** **G-SEC**: escaneo de secretos en el diff; CSRF activo en los formularios; sin scripts o estilos inline con lógica; sin `fetch`/`XMLHttpRequest` en `login.js`; sin logging de la clave.
  *→ T-125, T-202* · **Criterio de aceptación**: escaneo sin hallazgos; el token CSRF está presente en el HTML renderizado.
- [ ] **T-308** **G5**: verificar que el diff **no** contiene DDL, migraciones, `schema.sql`/`data.sql`, Flyway/Liquibase ni cambios en `spring.jpa.hibernate.ddl-auto`.
  *→ T-131* · **Criterio de aceptación**: el diff no toca `application.yml` en la sección `ddl-auto`; el `git diff --stat` no muestra archivos de esquema.
- [ ] **T-309** Cerrar la **matriz de paridad** de `plan.md` §9 (funcional, BD y seguridad) con la evidencia de cada prueba y registrar el resultado en Engram (`mem_save`, `topic_key: spec/03-login`).
  *→ T-301…T-308* · **Criterio de aceptación**: cada fila de la matriz referencia su prueba y su resultado; el resumen queda guardado en Engram.
- [ ] **T-310** Revisar y resolver los **supuestos abiertos SA-LOG-01…SA-LOG-07** y **SA-LOG-10…SA-LOG-15** con los responsables y actualizar `spec.md` §6 y `plan.md` §6 con la resolución (o la decisión de postergar con motivo). **SA-LOG-08/09 están cerrados** (revisión v2) y **SA-LOG-16/17/18** se resuelven por la aprobación humana de **T-007**.
  *→ T-001…T-109* · **Criterio de aceptación**: cada supuesto tiene una resolución escrita o una decisión de postergar con motivo y fecha.
- [ ] **T-311** *(revisión v2)* Test de regresión de **H4**: solicitar por `MockMvc` (sin cookie de sesión) cada ruta **HTML** del sistema (`/login`, `/home`, `/productos`) y verificar **HTTP 200**, presencia de `<form>` y del campo `_csrf`, y **ausencia** de la página de error `ERROR_INTERNO`; verificar además que la **sesión** creada por la petición contiene el token (la materialización fue temprana) y que el log **no** registra trazas ERROR asociadas.
  *→ T-128* · **Criterio de aceptación**: los 3 requests devuelven 200 con `<form>` y `_csrf` (nunca la página de error); el test falla si el cuerpo contiene `ERROR_INTERNO`; el log de la aplicación no registra trazas ERROR por esas peticiones; el repositorio de token sigue siendo `HttpSessionCsrfTokenRepository`. **Req**: R-LOG-042, R-LOG-043. **Plan**: D-LOG-18(a), §4.6. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-312** *(revisión v2)* Test de códigos de seguridad: (a) escritura **MVC** sin token CSRF ⇒ **403**; (b) escritura a **`/api/**`** **sin** token CSRF y **sin** credenciales ⇒ **NO** es rechazada por CSRF (responde el código de negocio: **no** 403, **no** 401, **no** HTML de error); (c) `POST /api/v1/auth/login` sin token ⇒ **200** con el cuerpo del SDT; (d) **ninguno** de los casos devuelve **500** ni la página `ERROR_INTERNO`.
  *→ T-126, T-127* · **Criterio de aceptación**: los 4 casos pasan según la política aprobada (D-LOG-17(a) + D-LOG-19(a)); el caso (b) **no** reproduce el 401 de H3 ni devuelve 403. **Req**: R-LOG-039, R-LOG-041. **Plan**: D-LOG-17(a), D-LOG-19(a), §4.5, §4.7. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-313** *(revisión v2)* Recorrido **completo** de la colección Postman contra la aplicación real (carpetas de `01-nucleo-datos`, `Productos` y `Auth`), verificando que **ningún** request de escritura falla por seguridad (**ni 401, ni 403**) y que el código de estado de cada uno coincide con el del backend; el recorrido se ejecuta **sin token CSRF** y **sin credenciales**.
  *→ T-401, T-404* · **Criterio de aceptación**: el recorrido termina sin requests con error de estado por CSRF/autenticación; ningún request envía cabecera ni parámetro `_csrf`; la evidencia queda en el informe de QA. **Req**: R-LOG-040. **Plan**: G-POSTMAN, §8.1.1. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-314** *(revisión v2)* Verificación del dialecto: arrancar la aplicación contra `Rentas` y confirmar que el log **no** contiene `HHH000511`; volver a correr la suite de IT completa y comparar resultados.
  *→ T-129* · **Criterio de aceptación**: el arranque no emite `HHH000511`; `ATP_DB_IT=true ./mvnw test "-Dtest=*IT"` en verde y con los mismos resultados que antes del cambio; `ddl-auto` sigue en `none`. **Req**: R-LOG-046. **Plan**: D-LOG-21.

---

## E. G-POSTMAN y documentación

- [ ] **T-401** **G-POSTMAN**: agregar a `docs/postman/ATPPuestos-API.postman_collection.json` la carpeta **`Auth`** con el request `POST {{baseUrl}}/api/v1/auth/login` (body `{ "usuario": …, "pass": …, "tipo": "PUE" }`) y su test de **status 200** + presencia de `error`/`sucursal`/`comision`, y un segundo request de credenciales inválidas con test `error = 2`; actualizar `docs/postman/README.md` (§"Cobertura actual" y *Changelog*) **en el mismo commit** que el backend.
  *→ T-120* · **Criterio de aceptación**: la colección contiene la carpeta `Auth`; el commit incluye backend **y** colección. **Req**: R-LOG-030. **Plan**: §8.1.
- [ ] **T-402** Verificar que la colección **no** altera los 44 requests preexistentes (mismo `raw`, mismo body, mismos tests) y que ningún endpoint del núcleo cambió de contrato en este módulo.
  *→ T-401, T-305* · **Criterio de aceptación**: `git diff` de la colección muestra **únicamente** la carpeta `Auth` y la línea del `info.description`; el diff de `nucleo/web/rest/**` fuera de `AuthController` es vacío. **Plan**: D-LOG-12.
- [ ] **T-403** Actualizar `docs/analisis-ATPPuestos.md` §15 con el resultado del G-DATA re-ejecutado (T-002): nombre físico real de las tablas de comisiones, `COMISIO1`, columnas de fechas de la comisión y PK real de `PPRIM_TU`.
  *→ T-002* · **Criterio de aceptación**: §15 refleja la evidencia de `INFORMATION_SCHEMA` y la nota de §15.1 sobre `COMISION` queda corregida. **Plan**: §5.4, SA-LOG-08, R-9.
- [ ] **T-404** *(revisión v2)* **G-POSTMAN — bloque de autenticación**: declarar en la colección el bloque `auth` **explícito** que corresponde a una API **sin credenciales** en este módulo (`noauth`), a nivel de colección, y **documentarlo** en `docs/postman/README.md` (§"Cobertura actual", §"Reglas" y *Changelog*) en el mismo commit; **sin ninguna credencial real versionada** (**G3**).
  *→ T-401* · **Criterio de aceptación**: la colección tiene `auth` explícito (`noauth`) y **ninguna** credencial real en la colección ni en el environment; ningún request de escritura queda bloqueado por 401/403 en el recorrido de T-313; el README documenta que la API **no** exige autenticación en este módulo y que el endurecimiento queda pendiente (SA-LOG-14 + T-135). **Req**: R-LOG-040. **Plan**: D-LOG-17(a), D-LOG-19(a), §8.1.1. **Desbloqueada** por T-007 (2026-10-22).
- [ ] **T-405** *(revisión v2)* **G-POSTMAN — alineación de los ejemplos con el contrato real**: quitar de los cuerpos de ejemplo los campos que los DTOs de escritura **no consumen** (p. ej. `uMedNombre`, declarado como informativo/derivado en `web/dto/ProductoEscrituraDto`), de modo que los ejemplos no induzcan a error.
  *→ T-401* · **Criterio de aceptación**: ningún body de ejemplo de la colección contiene `uMedNombre`; la revisión campo a campo contra `web/dto/*EscrituraDto` no encuentra campos divergentes. **Req**: R-LOG-045. **Plan**: §8.1.1.
- [ ] **T-406** *(revisión v2)* **G4 — dividir la colección Postman por módulo**: separar `docs/postman/ATPPuestos-API.postman_collection.json` (1990 líneas) en **un archivo por módulo** dentro de `docs/postman/`, con un índice en `docs/postman/README.md` que apunte a cada archivo y describa su contenido; ningún archivo del directorio debe superar las **500** líneas.
  *→ T-401, T-404* · **Criterio de aceptación**: `docs/postman/` contiene un archivo por módulo + `README.md` como índice; `(Get-Content <archivo> | Measure-Object -Line).Lines` ≤ 500 en todos; la cobertura total de requests se conserva (44 + 9 + los de `Auth`); el README lista cada archivo. **Req**: R-LOG-044. **Plan**: SA-LOG-18, §8.1.1. **Nota**: `docs/` es territorio de `verificador-qa`/coordinación — este módulo sólo redacta el requisito y la tarea.

---

## F. Orden de ejecución y paralelismo

### 6.1 Secuencia

```text
T-001, T-002, T-003, T-004, T-005   (paralelos entre sí; T-002/T-003 bloquean T-103 y T-132)
        │
        ▼
T-006  (documentación oficial)   ·   T-008 (fórmula de los atributos calculados)
        │
        ▼
T-101 ──┬── T-102 ──┐
        │            ├── T-107 ── T-108 ── T-109 ── T-122 ── T-123 ── T-124
        ├── T-103 ───┘         │                                  │
        ├── T-104 ── T-105 ── T-106                                    │
        │            └── T-110 ── T-111                                 │
        │                                                                   │
        └── T-120 ── T-121                                                │
                     T-125 ─────────────────────────────────────────────────┘
                                   │
                                   ▼
                    T-130, T-131  →  T-201…T-209  →  T-301…T-310  →  T-401…T-403

╔══════════════════════════════════════════════════════════════════════════╗
║  RAMA DE LA REVISIÓN v2 (seguridad) — DESBLOQUEADA (T-007 cerrada)       ║
╚══════════════════════════════════════════════════════════════════════════╝
T-007  (APROBACIÓN HUMANA de D-LOG-17 / D-LOG-18 / D-LOG-19) — CERRADA 2026-10-22
        │
        ├── T-126 (política CSRF explícita) ── T-127 (permitAll + sin autoridad de cookie)
        ├── T-128 (materialización anticipada del token)
        ├── T-129 (dialecto Hibernate / URL)
        ├── T-132 (entidades COMISION / COMISIO1) ── T-133 (repos con JOIN) ── T-311
        │
        ▼
        T-311 (regresión H4) ── T-312 (403 en MVC / API sin 403) ── T-313 (recorrido Postman)
        │
        ├── T-136 (mitigación: sin autoridad de cookie + SameSite=Lax + Authorization)
        ├── T-135 (deuda SA-LOG-14 — seguimiento, NO bloquea este módulo)
        │
        ▼
        T-404 (bloque auth noauth) ── T-405 (ejemplos) ── T-406 (split por módulo)
        │
        ▼
        T-314 (verificación del dialecto)
```

### 6.2 Qué puede hacer `implementador-backend` y `implementador-vistas` **en paralelo, sin colisión**

| Implementador | Escribe **sólo** en | Puede empezar cuando |
|---|---|---|
| `implementador-backend` | `nucleo/domain/**`, `nucleo/repository/**`, `nucleo/service/**`, `nucleo/config/**`, `nucleo/web/rest/**`, `nucleo/web/dto/**`, `nucleo/web/mvc/AuthMvcController.java`, `nucleo/web/mvc/login/**`, `pom.xml` | T-002/T-004 cerradas (T-101..T-125, T-130, T-131) |
| `implementador-vistas` | `nucleo/web/mvc/**` (**sólo** los form objects y el controller si los coordina), `templates/nucleo/**`, `static/**` | T-004 cerrada y T-122 con el contrato de la vista definido (T-201…T-209) |

**Colisiones a evitar**:

| Archivo | Conflicto | Regla |
|---|---|---|
| `nucleo/web/mvc/AuthMvcController.java` | Lo escriben los dos | **Backend** lo crea (T-122) con el contrato de vista completo; **vistas** sólo consume el nombre de vista y los nombres de atributo del modelo. |
| `nucleo/web/mvc/login/LoginForm.java` | Lo escriben los dos | **Backend** lo crea (T-122) con los nombres de campo; **vistas** sólo usa `th:field`. |
| `templates/nucleo/**` | Sólo vistas | El backend no crea plantillas. |
| `static/css/tokens.css`, `static/css/nucleo.css` | Los crea `02-abm-productos` | **Vistas** sólo **agrega** clases; si el archivo no existe, se crea en T-206 y se avisa al coordinador. |
| `docs/postman/ATPPuestos-API.postman_collection.json` | Sólo backend (T-401) | Las vistas **no** tocan la colección (G-POSTMAN: el canal `/api/**` es del backend). |
| `application.yml` (núcleo) | Sólo backend | **No** se toca `spring.jpa.hibernate.ddl-auto` (G5). |

### 6.3 Reglas de coordinación

| Regla | Detalle |
|---|---|
| 1 | Ninguna tarea de código arranca antes de la **aprobación de `spec.md` + `plan.md`** (puerta G-SPEC). |
| 2 | **T-002 es una puerta**: si el nombre físico de las tablas de comisiones/turnos no queda confirmado, T-103 (parcial), T-108 y el resto de T-1xx quedan bloqueados y se escala al coordinador. **Revisión v2**: T-002/T-003 tienen la evidencia (SA-LOG-08/09 cerrados) ⇒ la puerta está satisfecha y T-132/T-133 quedan habilitadas. |
| 3 | La lógica de negocio va **siempre** en `service/**`; ni el controller REST ni el MVC implementan reglas (AGENTS.md regla 9). |
| 4 | Los **mensajes verbatim** no se tocan, traducen ni capitalizan. |
| 5 | La KB es de **solo lectura**: T-001/T-002/T-006/T-008 son de lectura; nunca compilar, guardar, regenerar ni borrar objetos. |
| 6 | `tasks.md` se actualiza (con evidencia) **por el coordinador**, no por los implementadores. |
| 7 | **Revisión v2 — puerta de seguridad**: **CUMPLIDA** (T-007, 2026-10-22): las 3 decisiones de seguridad fueron **aprobadas por el responsable del proyecto** (alternativa (a) en las tres). ⇒ **T-126, T-127, T-128, T-311, T-312, T-313 y T-404 quedan DESBLOQUEADAS**. No se implementa ninguna alternativa "por las suyas" si contradice la aprobada. |
| 8 | **Revisión v2 — G3**: la política aprobada **no** admite credenciales **versionadas** en la colección Postman ni en el repositorio ⇒ la colección declara `noauth` explícito (T-404). |
| 9 | **Revisión v2 — no regresión del módulo 02**: T-128 y T-132/T-133 no pueden alterar el HTML, los rótulos, el renumerado `aria`/HTML de la grilla ni las cookies del módulo 02; `ProductoMvcCsrfTest` y el test de los 12 rótulos deben seguir verdes. |
| 10 | **Revisión v2 — coherencia de las decisiones**: **D-LOG-11** y **D-LOG-12** están **sustituidas** por **D-LOG-17(a)** y **D-LOG-19(a)**; toda referencia en código, pruebas o documentación debe apuntar a la decisión vigente (no a las sustituidas). |
| 11 | **Revisión v2 — deuda explícita**: la ausencia de CSRF en `/api/**` se mitiga según **R-LOG-041** (**T-136**) y la deuda de `permitAll` sigue con **T-135**; **ninguna** de las dos se puede dar por cerrada sin su tarea. |

---

## G. Cierre del modulo (registrado 2026-10-30)

- **Build**: `./mvnw clean test` -> **BUILD SUCCESS** (nucleo + puestos, 0 failures).
- **Secretos**: escaneo sin hallazgos reales (falsos positivos documentados).
- **Paridad de BD**: `ddl-auto: none` sin cambios; sin DDL/migraciones.
- **Archivos > 500 lineas**: solo PDFs y spec/plan (no configuracion).
- **Commits de cierre**: `5cf3a7e feat(nucleo)` + `8630d91 docs(spec)`.
- **Pendiente (no bloquea el cierre del build)**: validacion de las tareas de integracion contra `Rentas` (`ATP_DB_IT=true`) y verificacion manual de accesibilidad/contraste (T-UI-F4). El coordinador debe marcar `[x]` individualmente con evidencia cuando se ejecuten.
- **Estado**: modulo **commiteado y estable**; listo para continuar con otra funcionalidad.
