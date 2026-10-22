# Tasks — 03 Login (núcleo compartido)

> Checklist verificable del módulo `03-login`. Cada tarea es **atómica** y trazable a un requisito de
> `spec.md` (R-LOG-*) o a un punto de `plan.md`.
> **Ninguna tarea de código se inicia sin aprobar `spec.md` (puerta G-SPEC)** — guardarraíl G2.
> **Ninguna tarea está marcada como completada**: el cierre y la evidencia son del coordinador.
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
- [ ] **T-125** Crear `nucleo.config.SeguridadConfig` con la `SecurityFilterChain`: `permitAll` para `/login`, `/api/v1/auth/login`, `/error` y los recursos estáticos; `authenticated()` para el resto; **`LoginUrlAuthenticationEntryPoint`** (⇒ `302` a `/login`); **CSRF habilitado** con descarte **únicamente** en `POST /api/v1/auth/login`; `sessionFixation().changeSessionId()`.
  *→ T-101, T-122* · **Criterio de aceptación**: `GET /home` sin sesión ⇒ `302` a `/login`; `POST /login` **sin** token CSRF ⇒ `403`; `POST /login` **con** token ⇒ `200`/`302`; `POST /api/v1/auth/login` **sin** token ⇒ `200`. **Req**: R-LOG-031, R-LOG-028. **Plan**: D-LOG-11, D-LOG-12, SA-LOG-14.
- [ ] **T-130** Derivar el usuario auditor de la escritura del `Context.Usuario` de la sesión, en lugar del valor por defecto `SISTEMA` de `application.yml` (`app.auditoria.usuario`).
  *→ T-109, T-124* · **Criterio de aceptación**: el test verifica que una escritura posterior a un login usa `Context.Usuario` y **no** `SISTEMA`; sin sesión ⇒ sigue `SISTEMA`. **Req**: R-LOG-032. **Plan**: §5.5.
- [ ] **T-131** Verificar que **todos** los repositorios del módulo son de sólo lectura (`@Transactional(readOnly=true)`), que el `diff` **no** toca `spring.jpa.hibernate.ddl-auto` y que no hay DDL, migraciones, `schema.sql`/`data.sql`, Flyway ni Liquibase.
  *→ T-102, T-103, T-120* · **Criterio de aceptación**: el `git diff` no contiene `CREATE TABLE`/`ALTER TABLE` ni referencias a migraciones; la BD queda sin cambios al terminar la suite. **Plan**: G5, §5.5.

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
- [ ] **T-310** Revisar y resolver los **supuestos abiertos SA-LOG-01…SA-LOG-14** con los responsables y actualizar `spec.md` §6 y `plan.md` §6 con la resolución (o la decisión de postergar con motivo). **Prioridad: SA-LOG-08/09** (nombre físico de las tablas), porque bloquean las entidades.
  *→ T-001…T-109* · **Criterio de aceptación**: cada supuesto tiene una resolución escrita o una decisión de postergar con motivo y fecha.

---

## E. G-POSTMAN y documentación

- [ ] **T-401** **G-POSTMAN**: agregar a `docs/postman/ATPPuestos-API.postman_collection.json` la carpeta **`Auth`** con el request `POST {{baseUrl}}/api/v1/auth/login` (body `{ "usuario": …, "pass": …, "tipo": "PUE" }`) y su test de **status 200** + presencia de `error`/`sucursal`/`comision`, y un segundo request de credenciales inválidas con test `error = 2`; actualizar `docs/postman/README.md` (§"Cobertura actual" y *Changelog*) **en el mismo commit** que el backend.
  *→ T-120* · **Criterio de aceptación**: la colección contiene la carpeta `Auth`; el commit incluye backend **y** colección. **Req**: R-LOG-030. **Plan**: §8.1.
- [ ] **T-402** Verificar que la colección **no** altera los 44 requests preexistentes (mismo `raw`, mismo body, mismos tests) y que ningún endpoint del núcleo cambió de contrato en este módulo.
  *→ T-401, T-305* · **Criterio de aceptación**: `git diff` de la colección muestra **únicamente** la carpeta `Auth` y la línea del `info.description`; el diff de `nucleo/web/rest/**` fuera de `AuthController` es vacío. **Plan**: D-LOG-12.
- [ ] **T-403** Actualizar `docs/analisis-ATPPuestos.md` §15 con el resultado del G-DATA re-ejecutado (T-002): nombre físico real de las tablas de comisiones, `COMISIO1`, columnas de fechas de la comisión y PK real de `PPRIM_TU`.
  *→ T-002* · **Criterio de aceptación**: §15 refleja la evidencia de `INFORMATION_SCHEMA` y la nota de §15.1 sobre `COMISION` queda corregida. **Plan**: §5.4, SA-LOG-08, R-9.

---

## F. Orden de ejecución y paralelismo

### 6.1 Secuencia

```text
T-001, T-002, T-003, T-004, T-005   (paralelos entre sí; T-002/T-003 bloquean T-103)
        │
        ▼
T-006  (documentación oficial)
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
| 2 | **T-002 es una puerta**: si el nombre físico de las tablas de comisiones/turnos no queda confirmado, T-103 (parcial), T-108 y el resto de T-1xx quedan bloqueados y se escala al coordinador. |
| 3 | La lógica de negocio va **siempre** en `service/**`; ni el controller REST ni el MVC implementan reglas (AGENTS.md regla 9). |
| 4 | Los **mensajes verbatim** no se tocan, traducen ni capitalizan. |
| 5 | La KB es de **solo lectura**: T-001/T-002/T-006 son de lectura; nunca compilar, guardar, regenerar ni borrar objetos. |
| 6 | `tasks.md` se actualiza (con evidencia) **por el coordinador**, no por los implementadores. |
