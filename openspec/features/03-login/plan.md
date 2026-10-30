# Plan — 03 Login (núcleo compartido)

> **Estado**: `APROBADA (revisión v2)` — puerta **G-SPEC** superada. Las **3 decisiones de seguridad**
> (**D-LOG-17**, **D-LOG-18**, **D-LOG-19**) fueron **aprobadas el 2026-10-22 por el responsable del
> proyecto** (alternativa **(a)** en las tres) ⇒ **no queda nada pendiente de aprobación humana**.
> Acompaña a `spec.md` (misma puerta). Documento de arquitectura; no contiene tareas (ver `tasks.md`).
> Contrato de datos: `openspec/features/00-alcance/plan.md` §3 y `docs/analisis-ATPPuestos.md` §13/§15.
> Paridad de BD: **G-DATA re-ejecutado el 2026-10-22** contra `Rentas` (§5.3) ⇒ **SA-LOG-08 y
> SA-LOG-09 cerrados**.

---

## 1. Contexto y goals

### 1.1 Qué produce este módulo

El **login completo**: verificación de credenciales contra `SCRTY`, resolución del contexto
operativo (persona → comisión/receptoría → puesto → turno), chequeo de IP del puesto, código de
resultado 0-6 con mensaje **verbatim**, persistencia del tipo de acceso en la cookie `SUCURSAL`,
autorización por programa y **dos canales**: formulario Thymeleaf (sesión) y API REST (stateless).

```text
KB (legacy, sólo lectura) → spec EARS → [servicio nuevo] → web/mvc (Thymeleaf) + web/rest (JSON)
                                                     ↑
                        ServicioAcceso / VerificadorDeClave / repositorios del núcleo
```

El módulo **no** reimplementa la seguridad: **replica** el mecanismo observable del legacy
(cifrado reversible por sustitución, no cifrado real) y sus límites, dejándolos documentados.

### 1.2 Dónde vive el código — **MÓDULO DEL NÚCLEO COMPARTIDO**

> **Decisión estructural (D-LOG-01).** Este módulo **arranca como módulo del núcleo compartido**
> (`atp-nucleo-compartido`), **no** como módulo exclusivo de `atp-puestos-camineros`.
> El login es transversal: lo consume la UI Thymeleaf de Puestos Camineros **y** el canal
> WS/mobile que ya consume la KB (`Seg_ControlAccesoWS`).

| Artefacto | Ubicación | Razón |
|---|---|---|
| Entidades JPA (sólo lectura) | `atp-nucleo-compartido/src/main/java/ar/com/atp/puestos/nucleo/domain/` | Convención plana por capa del núcleo (`domain`/`repository`/`service`/`web`). |
| Repositorios | `.../nucleo/repository/` | Idem. |
| Servicio de acceso | `.../nucleo/service/` | Idem. |
| Verificador de clave | `.../nucleo/service/VerificadorDeClave.java` + `CodificadorSegEncripta.java` | Interfaz + implementación (D-LOG-03). |
| Controller MVC | `.../nucleo/web/mvc/AuthMvcController.java` | Capa de vistas del núcleo. |
| Form objects | `.../nucleo/web/mvc/login/` | Los DTOs de formulario **no** van en `web/dto/**` (D-05 de `00-alcance`). |
| Controller REST | `.../nucleo/web/rest/AuthController.java` | `web/rest` es el canal `/api/**` (D-04 de `00-alcance`). |
| DTOs REST | `.../nucleo/web/dto/AuthLoginRequestDto.java`, `AuthLoginResponseDto.java` | `record` inmutables (stack.md §1). |
| Configuración de seguridad | `.../nucleo/config/SeguridadConfig.java` | Filtro de Spring Security (núcleo). |
| Plantillas | `atp-nucleo-compartido/src/main/resources/templates/nucleo/` | Prefijo de módulo (D-03 de `00-alcance`). |
| JS | `.../static/js/login.js` | Toggle de contraseña + autofocus (permitido por `docs/design.md` §8). |

> **El módulo desplegable `atp-puestos-camineros` no se toca**: hereda el núcleo en el fat JAR.
> Sus archivos de perfil (`application-puestos-camineros.yml`) siguen siendo suyos.

### 1.3 Los 2 perfiles de acceso

| Perfil | Origen legacy | Contexto | Chequeo de IP | Cookie | Sesión | Canal |
|---|---|---|---|---|---|---|
| **INTERNO** | `Seg_ControlAcceso` | Sí (10 de 13 campos) | Sí (rama `PUE`) | Sí | Sí | UI Thymeleaf |
| **WS** | `Seg_ControlAccesoWS` | **No** (bloque comentado) | **No** (bloque comentado) | **No** | **No** | API REST |

El perfil es un **strategy** (`PerfilAcceso {INTERNO, WS}`) elegido por el **adaptador**: el
controller MVC pide INTERNO, el controller REST pide WS. No hay *property* que lo cambie: el
perfil es una consecuencia del canal, exactamente como en la KB (dos procedimientos distintos).

### 1.4 Goals

| # | Goal |
|---|---|
| G-1 | Reproducir **exactamente** el resultado observable del login: los 7 códigos y sus mensajes verbatim. |
| G-2 | Reutilizar la base existente sin DDL: `ddl-auto=none`, nombres físicos exactos. |
| G-3 | Dejar la UI y el canal REST sobre **el mismo servicio** (`web → service → repository → domain`). |
| G-4 | Permitir que un futuro mecanismo de clave (PBKDF2) entre **sin tocar** los adaptadores. |
| G-5 | Cerrar el riesgo "pantalla expuesta" que dejó abierto `02-abm-productos` (SA-P07). |

---

## 2. Decisiones de diseño

| # | Decisión | Contexto | Alternativas descartadas | Consecuencias |
|---|---|---|---|---|
| **D-LOG-01** | El módulo vive en el **núcleo compartido** (`atp-nucleo-compartido`), no en `atp-puestos-camineros`. | El login sirve a la UI **y** al canal WS/mobile ya consume la KB. `00-alcance/plan.md` §5 asignaba `templates/puestos/` a Puestos Camineros. | (a) Módulo exclusivo de Puestos Camineros → obligaría al canal WS a depender del artefacto desplegable; (b) paquete nuevo `nucleo/seguridad/**` → rompe la convención plana por capa del núcleo. | Actualizar `00-alcance/plan.md` §5 (T-006). El núcleo queda con la lógica de acceso; el fat JAR no cambia. |
| **D-LOG-02** | Plantillas en `templates/nucleo/` (`login.html`, `home.html`, `not-authorized.html`, `fragments/menu-programas.html`). | Prefijo de módulo (D-03 de `00-alcance`): evita la colisión silenciosa del resolver de Thymeleaf. | `templates/puestos/` → reservado a pantallas exclusivas de Puestos Camineros; obligaría a duplicar el layout. | Reutiliza el layout y los tokens que crea `02-abm-productos` (T-201…T-203 de ese módulo). |
| **D-LOG-03** | El mecanismo de clave se reproduce **tal cual** (`Seg_Encripta`), **sin PBKDF2**, detrás de la interfaz `VerificadorDeClave`. | Paridad funcional: 550 de 552 usuarios tienen `UsuClave`; las 2 restantes ya quedan fuera en el legacy (código 2 / código 5). | (a) PBKDF2 sobre `UsuHash` → cambiaría el resultado observable para esas 2 cuentas (SA-LOG-03); (b) reescribir el algoritmo → rompería los 552 `UsuId`. | Riesgo de seguridad **documentado y asumido** (R-1). La interfaz deja la puerta abierta (§3.4). |
| **D-LOG-04** | El login **no lanza excepciones** en la capa de servicio: devuelve un resultado con **código 0-6**. El mensaje verbatim lo decide **una tabla única** (`MensajesAcceso`) compartida por los dos adaptadores. | El legacy asigna el mensaje a un `TextBlockPass`, no a una excepción (R-LOG-019). | (a) Excepción de dominio con el código → el mensaje quedaría en el adapter y habría dos copias. | Un solo lugar decide el texto; el test parametrizado de R-LOG-018 tiene un único blanco. |
| **D-LOG-05** | La comparación de `UsuId` se hace con **`UPPER()` explícito en ambos lados** (`UPPER(UsuId) = UPPER(:usuarioEncrip)`). | El legacy compara `UsuId.ToUpper() = &UsuarioEncrip` y depende de la collation `Latin1_General_CI_AS`. 504 de 552 `UsuId` contienen `b` minúscula. | (a) `UsuId = :enc` sin `UPPER()` → depende de la collation; frágil si cambia el `COLLATE`. | La paridad no depende de la collation de la BD. |
| **D-LOG-06** | El perfil de acceso (`INTERNO`/`WS`) es un **strategy** elegido por el adaptador (canal), **no** una propiedad de configuración. | En la KB el perfil es la **procedura** invocada: la UI llama `Seg_ControlAcceso`, el WS llama `Seg_ControlAccesoWS`. | (a) `app.login.perfil` configurable → permite desalinear el canal de su semántica legacy. | El canal determina el comportamiento ⇒ paridad garantizada. La estructura admite un tercer perfil futuro. |
| **D-LOG-07** | La UI Thymeleaf usa **Spring Security con sesión HTTP + CSRF**; la cookie `SUCURSAL` es **de sesión** (`Max-Age` ausente), `HttpOnly`, `SameSite=Lax`, `path=/`. | Decisión ya tomada en `03-login/README.md` §"Decisiones ya tomadas" y `stack.md` §1. `SA-LOG-07` (atributos de cookie) no está definido por el legacy. | (a) Cookie persistente → el legacy no lo declara; (b) `HttpOnly=false` → expondría el tipo de acceso a scripts. | El toggle de contraseña del JS (`login.js`) sigue funcionando con `HttpOnly` (no lee el valor). |
| **D-LOG-08** | El contexto de sesión replica el SDT `Context` (**13 campos**, 10 cargados) y se persiste en la **sesión HTTP** bajo la clave `"context"`. | `SetContext` → `&Session.Set(!"context", &Context.ToXml())`; `LoadContext` → `&Context.FromXml(&Session.Get(!"context"))`. | (a) Serializar a XML como el legacy → sin interoperabilidad con el resto de Spring; se conserva el **mismo nombre de clave** para que la traza sea legible. | `ContextoSesion` es un `record` de 13 campos; los 3 no cargados quedan en su valor por defecto (R-LOG-016). |
| **D-LOG-09** | `VerificadorDeClave` es la **única** puerta al mecanismo de clave. `CodificadorSegEncripta` es su implementación actual. | D-LOG-03 deja la puerta a PBKDF2. | (a) Lógica de codificación dentro de `ServicioAcceso` → ataría el servicio a un solo algoritmo. | Un `record`/clase de entrada y otro de salida (`Credencial`, `ResultadoAcceso`) no dependen del algoritmo. |
| **D-LOG-10** | El chequeo de IP queda **encapsulado en un único método** (`VerificadorIpCliente.esCompatible(ip, prefijo)`) con test dedicado. | `SA-LOG-01` (semántica de `IndexOf`) y `SA-LOG-06` (prefijo de texto vs máscara) siguen abiertos. | (a) `String.startsWith` en línea en el servicio → imposible de testear aislado y disperso. | Un solo punto de cambio cuando se resuelvan los supuestos. |
| **D-LOG-11** | **SUSTITUIDA por D-LOG-17 (2026-10-22)** — *histórico, se conserva*. ~~CSRF se desactiva únicamente en `POST /api/v1/auth/login`~~ ⇒ el descarte de CSRF alcanza ahora a **todo `/api/**`** (D-LOG-17(a)). El **fundamento** de esta decisión sí sigue vigente: el WS legacy es *stateless* y sin cookies ⇒ el token CSRF no tiene sentido para él. | La alternativa original era demasiado restrictiva: con CSRF global el resto de la API quedaba inutilizable (H3/H6). | (a) CSRF global → el endpoint WS quedaría inutilizable; (b) CSRF global desactivado → debilita los formularios de escritura. | **No se usa como decisión vigente**: toda referencia a la política CSRF apunta a **D-LOG-17**. |
| **D-LOG-12** | **SUSTITUIDA por D-LOG-19 (2026-10-22)** — *histórico, se conserva*. ~~El resto de `/api/v1/**` (los 44 requests de `01-nucleo-datos`) queda `permitAll` en este módulo~~ ⇒ el `permitAll` **se mantiene**, pero **por decisión expresa** (D-LOG-19(a)) y con **deuda explícita** (SA-LOG-14 + tarea **T-135**), no como efecto colateral de no querer romper la colección. | Hoy esa API **no tiene ninguna seguridad** (no hay `spring-boot-starter-security` en el núcleo). Endurecerla en este módulo rompería la colección Postman y la suite de `01`/`02` (G-POSTMAN + no-regresión). | (a) `authenticated()` para todo `/api/v1/**` → 44 requests → `401`; (b) Basic auth sobre toda la API → invención de contrato + **G3**. | **No hay regresión de seguridad** respecto del estado actual. El endurecimiento queda como **tarea posterior y explícita**: **T-135** (SA-LOG-14, riesgo R-10). |
| **D-LOG-13** | La autorización por programa se replica tal cual: `IsAuthorized` **devuelve siempre `True`** ⇒ la lista completa. La estructura queda **filtrable**. | `IsAuthorized` tiene su lógica comentada; `ListPrograms` no tiene callers (SA-LOG-02). | (a) Inventar una regla por grupo/nivel → comportamiento no verificable en el legacy. | `ServicioAutorizacion.programasAutorizados()` devuelve siempre los 2 programas, pero el filtrado es un `filter` de una línea. |
| **D-LOG-14** | Las entidades JPA del login mapean **sólo las columnas que el login lee**. El módulo **no escribe** en ninguna de estas tablas ⇒ no hay columnas huérfanas que requieran default de aplicación. | La regla de columnas huérfanas (R-036 de `01-nucleo-datos`) aplica a **escrituras**. | (a) Mapear las 5 columnas huérfanas de `SCRTY` (`UsuLegajo`, `UsuFoto` `varbinary(max)`, `UsuIdComarb`, `UsuAppFchAdhesion`, `UsuHash`) → sin destino y riesgo de mapeo erróneo. | `UsuHash` (PBKDF2) queda **fuera** (SA-LOG-03). Si un módulo futuro escribe en `SCRTY`, deberá mapearlas con sus defaults. |
| **D-LOG-15** | El **controller MVC** es el que escribe la cookie `SUCURSAL` y el que registra la autenticación en la sesión; el **servicio** no conoce cookies ni `HttpServletRequest`. | El legacy escribe la cookie desde el evento `Enter` del formulario, no desde el procedimiento. | (a) Cookie en el servicio → acopla `service` al contenedor web. | `service` no importa nada de `jakarta.servlet`; la IP del cliente también se resuelve en el adaptador y se pasa al servicio. |
| **D-LOG-16** | El login **no** usa `AuthenticationManager`: el controlador invoca `ServicioAcceso` para conservar el **código 0-6** y el mensaje verbatim, y registra la autenticación en la sesión vía `SecurityContextRepository`. | `AuthenticationProvider` lanzaría `BadCredentialsException` y **perdería el código**, que es el dato de negocio que hay que mostrar. | (a) `AuthenticationManager` con excepción portadora del código → el mensaje verbatim quedaría en el adapter y habría que mapear 2 veces. | Spring Security aporta **protección de rutas + CSRF + sesión**; la **decisión de acceso** es de negocio y vive en el servicio. |
| **D-LOG-17** | **APROBADA (2026-10-22, responsable del proyecto) — alternativa (a)**. **Política CSRF / autenticación de `/api/**`:** `/api/**` queda **stateless y SIN CSRF**; el CSRF se exige **sólo** en las **rutas MVC con sesión** (`/login`, `/productos`, `/logout`, `/home`). La colección Postman queda **ejecutable** y declara de forma explícita el bloque `auth` que corresponde. | H3/H6 del QA del módulo 02: `POST /api/v1/productos` con Basic y sin token ⇒ **401** (no 403); la colección no declaraba `auth` ⇒ sus 44+9+1 requests de escritura **no eran ejecutables**. Con el CSRF global + `permitAll` la colección seguía bloqueada. | (a) API *stateless* sin CSRF + autenticación a definir ← **APROBADA**; (b) CSRF sólo en MVC + API con token propio; (c) CSRF en `/api/**` con flujo de token documentado en la colección. | **Vigente**: sustituye a **D-LOG-11**. `/api/**` **no** rechaza por CSRF ⇒ un `POST /api/v1/**` **no** devuelve 403 por token ausente (R-LOG-039). **Mitigación acordada**: (i) la API es *stateless* y **no** toma autoridad de la cookie de sesión ⇒ no hay credencial ambiental que un atacante pueda montado; (ii) `JSESSIONID` con **`SameSite=Lax`** ⇒ el navegador no envía la cookie de sesión en POST intersitio; (iii) cuando la API se endurezca (T-135), la credencial viajará en la cabecera **`Authorization`**, **nunca** en cookie ⇒ CSRF inaplicable por construcción. Sin credenciales versionadas (**G3**). |
| **D-LOG-18** | **APROBADA (2026-10-22, responsable del proyecto) — alternativa (a)**. **Materialización anticipada del token CSRF:** se conserva `HttpSessionCsrfTokenRepository` y el token se **materializa temprano** (filtro propio que fuerza `csrfToken.getToken()` **antes** del render, o `setRequestAttribute` con `CsrfTokenRequestAttributeHandler`). **NO** se migra a `CookieCsrfTokenRepository`. | H4 del QA: `HttpSessionCsrfTokenRepository` difiere el guardado del token hasta el primer acceso (el `th:action` de `templates/nucleo/productos.html`, línea 103), con la respuesta ya confirmada ⇒ `IllegalStateException: Cannot create a session after the response has been committed` ⇒ **200 con la página de error** y 3 trazas ERROR por petición (reproducible 4/4). En `03-login` es **estructural**: el mismo `SecurityFilterChain` protege `/login` y `/home`. | (a) materialización anticipada conservando el repositorio de sesión ← **APROBADA**; (b) `CookieCsrfTokenRepository` (token en cookie, sin sesión); (c) `XorCsrfTokenRequestAttributeHandler` + `setRequestAttribute` (complemento opcional, **no** corrige H4 por sí solo). | **Vigente**. **No** cambia el contrato del formulario (`th:action` sigue emitiendo `_csrf`), **no** cambia la cookie `SUCURSAL`, **no** altera el HTML/ARIA de las vistas ⇒ **cero regresión** en `ProductoMvcCsrfTest` y en los 12 rótulos del módulo 02. |
| **D-LOG-19** | **APROBADA (2026-10-22, responsable del proyecto) — alternativa (a)**. **Nivel de autenticación de `/api/v1/**`:** `permitAll` en este módulo, con la **deuda explícita** en SA-LOG-14 y el riesgo R-10; el endurecimiento queda como **tarea posterior y explícita** (**T-135**). Sin credenciales versionadas (**G3**). | H3/H6: exigir autenticación rompería la colección Postman salvo que las credenciales se versionen en el repo (**G3**). El legacy expone su canal WS **sin sesión** (`Seg_ControlAccesoWS`). | (a) `permitAll` ← **APROBADA**; (b) `authenticated()` con sesión ⇒ la colección deja de ser ejecutable; (c) HTTP Basic contra `SCRTY` ⇒ exige credenciales en un artefacto versionado (**G3**). | **Vigente**: sustituye a **D-LOG-12**. Sin regresión respecto del estado actual; la deuda queda **documentada** y con dueño (SA-LOG-14, R-10, **T-135**). |
| **D-LOG-20** | Los atributos **calculados** del legacy (`CPCPerNyA`, `CPCResNyA`, `PueDesc` de la transacción `Comisiones`, `ReceDsc` de `Seg_BusRece`) **NO se mapean como columnas**: el sistema los resuelve con **JOIN** a `PERSONAS` (`PerId`) y a `RECEPTOR` (`ReceId`). | G-DATA 2026-10-22: `CPCPerNyA` y `CPCResNyA` **no existen** en ninguna tabla de `Rentas`; `COL_LENGTH('COMISIO1','CPCPerNyA')` ⇒ `NULL`; `COMISIO1` tiene 3 columnas y `COMISION` 14. El DDL de `genexus_db sql_ddl` los emite por ser una reconstrucción **heurística** del SDK. | (a) Mapearlos como columnas ⇒ Hibernate falla en el arranque o la lectura; (b) omitirlos sin JOIN ⇒ `Context.ResponsableNombre` vacío y orden no determinista. | R-LOG-035 + R-LOG-036; el JOIN pasa a ser **obligatorio** (la nota previa de `spec.md` §3.9 era incorrecta). La fórmula exacta queda abierta en **SA-LOG-15** (**T-008**). |
| **D-LOG-21** | Se fija **explícitamente** el dialecto de Hibernate (`org.hibernate.dialect.SQLServerDialect` con la versión mínima soportada) **o** se corrigen los parámetros de la URL de conexión, de modo que el arranque **no** emita `HHH000511`. El `ddl-auto` **no** se toca. | H9 del QA: el driver `mssql-jdbc 12.10` con `encrypt=true;trustServerCertificate=true` reporta la versión **10.0** y Hibernate avisa que el mínimo es 11.0, aunque el servidor real es **SQL Server 2019 (15.0.2170.1)** (verificado con `@@VERSION`). Impacto actual nulo; riesgo latente de compatibilidad de Hibernate 6.6. | (a) fijar el dialecto explícito (recomendado: no cambia la URL ni el comportamiento observable); (b) cambiar los parámetros de conexión (`encrypt`/`trustServerCertificate`) ⇒ puede alterar la conexión en otros entornos. | R-LOG-046 + tarea **T-129** (configuración) y **T-314** (verificación con la suite completa en verde). |

---

## 3. Arquitectura objetivo

### 3.1 Capas y paquetes

```text
ar.com.atp.puestos.nucleo
├── domain/                       (sólo lectura; sin dependencias)
│   ├── Usuario.java              @Table(name="SCRTY")          PK UsuId
│   ├── Persona.java              @Table(name="PERSONAS")       PK PerId
│   ├── Puesto.java               @Table(name="PUEPPRIM")       PK PueCod
│   ├── Receptor.java             @Table(name="RECEPTOR")       PK ReceId
│   ├── ReceptorPersona.java      @Table(name="RECEPTO1")       PK (ReceId, PerId)
│   ├── Comision.java             @Table(name="COMISION")       PK (PueCod, CPCId)   ✔ SA-LOG-08
│   ├── ComisionIntegrante.java   @Table(name="COMISIO1")       PK (PueCod, CPCId, CPCPerId) ✔ SA-LOG-08
│   └── TurnoPPrim.java           @Table(name="PPRIM_TU")       PK (SucNroId, SucTipo, TurNroId) ✔ SA-LOG-08
├── repository/                   (Spring Data JPA; sólo SELECT)
│   ├── UsuarioRepository.java
│   ├── PersonaRepository.java
│   ├── PuestoRepository.java
│   ├── ReceptorRepository.java
│   ├── ComisionRepository.java   (JOIN a PERSONAS — D-LOG-20)
│   └── TurnoRepository.java
├── service/
│   ├── ServicioAcceso.java       fachada del login (INTERNO / WS)
│   ├── Credencial.java           record(usuario, clave, tipo)
│   ├── ResultadoAcceso.java      record(codigo, contexto, sucursal, comision)
│   ├── ContextoSesion.java       record de 13 campos (SDT Context)
│   ├── PerfilAcceso.java         enum {INTERNO, WS}
│   ├── DatosPeticion.java        record(ipCliente)  ← lo resuelve el adaptador
│   ├── VerificadorDeClave.java   interfaz (D-LOG-03/09)
│   ├── CodificadorSegEncripta.java  implementación (Seg_Encripta)
│   ├── MensajesAcceso.java       tabla código → mensaje verbatim (7 entradas)
│   ├── ServicioAutorizacion.java ListPrograms + IsAuthorized
│   └── ProgramaAutorizado.java   record(nombre, descripcion, link)
├── config/
│   └── SeguridadConfig.java      SecurityFilterChain + CSRF + logout
└── web/
    ├── dto/AuthLoginRequestDto.java, AuthLoginResponseDto.java
    ├── rest/AuthController.java              POST /api/v1/auth/login
    └── mvc/
        ├── AuthMvcController.java             /login, /home, /not-authorized
        └── login/LoginForm.java               form-backing object
```

**Regla de dependencia**: `web → service → repository → domain`; `domain` no depende de nada;
`shared` es transversal. **Prohibido** `web → repository` y `domain → web`
(`AGENTS.md` regla 7, `stack.md` §5).

### 3.2 Ruta de la petición MVC (perfil INTERNO)

```text
1. GET /login
   ├─ SecurityFilterChain: ruta permitAll
   ├─ AuthMvcController.loginForm()
   ├─ lee la cookie "SUCURSAL" (si existe) → precarga el selector de Acceso
   └─ renderiza la vista "nucleo/login" con loginForm (usuario/clave/tipo) y mensajeError = null

2. POST /login   (formulario con th:action ⇒ token _csrf)
   ├─ SecurityFilterChain: CSRF habilitado ⇒ sin token ⇒ 403
   ├─ BindingResult: @NotBlank/@Size → si falla, se re-renderiza con errores (200)
   ├─ AuthMvcController.login(...)
   │   ├─ usuario vacío ⇒ mensaje "Usuario inválido" (R-LOG-021) SIN tocar la BD
   │   ├─ resolveIp(request): X-Forwarded-For → si vacío, request.getRemoteAddr()
   │   ├─ ServicioAcceso.autenticar(INTERNO, credencial, new DatosPeticion(ip))
   │   │   ├─ VerificadorDeClave.codificar(usuario) → usuarioEncrip
   │   │   ├─ UsuarioRepository.findByUsuId(UPPER(UsuId) = UPPER(:usuarioEncrip))
   │   │   ├─ if (!found || UsuClave vacía) → código 2
   │   │   ├─ if (!VerificadorDeClave.coincideCon(codificar(clave), UsuClave)) → código 2
   │   │   ├─ if (UsuIngreso == 1) → código 5
   │   │   ├─ PersonaRepository.findByPerCodUsu(usuario en claro) → sin match → código 3
   │   │   ├─ if (tipo == "PUE") → comisión activa (+ responsable + puesto + prefijo de red)
   │   │   │     ├─ sin comisión → código 4
   │   │   │     └─ VerificadorIpCliente.esCompatible(ip, PueIPRed) → false ⇒ código 6
   │   │   │  else → receptoría (sin match ⇒ SucNroId = -1)
   │   │   ├─ if (SucNroId >= 0) → turno + ContextoSesion (10 campos)
   │   │   │  else → rama "fue responsable": contexto parcial (4 campos) + código 0, ó código 4
   │   │   └─ devuelve ResultadoAcceso(codigo, contexto, sucursal, comision)
   │   ├─ if (codigo != 0) → re-renderiza "nucleo/login" con mensajeError = MensajesAcceso.para(codigo)  (HTTP 200)
   │   └─ if (codigo == 0)
   │       ├─ securityContextRepository.saveContext(authentication)  (session fixation: changeSessionId)
   │       ├─ GestorContextoSesion.guardar(request, contexto)     → session["context"]
   │       ├─ ResponseCookie "SUCURSAL" = tipo  (HttpOnly, SameSite=Lax, sin Max-Age, path=/)
   │       └─ redirect → GET /home
   └─ SecurityFilterChain: rutas no permitAll ⇒ authenticated()
```

### 3.3 Ruta de la petición REST (perfil WS)

```text
1. POST /api/v1/auth/login
   ├─ SecurityFilterChain: permitAll + CSRF ignorado en ESTA ruta (D-LOG-17(a) — descarte de
   │  CSRF para TODO /api/**)
   ├─ @Valid @RequestBody AuthLoginRequestDto → 400 {code:"VALIDACION"} si viola @Size
   ├─ AuthController.login(dto)
   │   ├─ ServicioAcceso.autenticar(WS, credencial, DatosPeticion(sin IP))
   │   │   (perfil WS: sin contexto, sin chequeo de IP, sin rama "historia")
   │   └─ devuelve AuthLoginResponseDto(error, sucursal, comision)
   └─ HTTP 200 + {"error":N,"sucursal":N,"comision":N}
```

**Sin sesión, sin cookie, sin contexto** (R-LOG-028). El código 6 **no es producible** por este canal.

### 3.4 Verificador de clave (`Seg_Encripta` legacy, tal cual)

```text
VerificadorDeClave                        (interfaz — D-LOG-09)
├── String codificar(String texto)          → Seg_Encripta (Espejo 67−i sobre 66)
├── boolean coincideCon(String codificado, String almacenado)
└── String decodificar(String codificado)   → reversibilidad (R-LOG-003)

CodificadorSegEncripta                     (implementación actual — D-LOG-03)
```

| Regla | Valor (verificado en `spec.md` §3.2) |
|---|---|
| Normalización | `ToUpper()` |
| Alfabeto | 66 caracteres: ASCII 32-96 con `'a'` en el índice 8 y `'b'` en el 66 |
| Regla | `destino = 67 − origen`; `origen` ∈ 1..65 (el índice 66 nunca se compara) |
| Recorrido | posiciones 1..10 |
| Caracteres fuera del alfabeto | **se descartan** (la salida queda más corta) |
| Salida | `CHARACTER(10)` |

> **Puerta futura**: una implementación `VerificadorDeClavePbkdf2` (SA-LOG-03) puede entrar por la
> misma interfaz **sin tocar** `ServicioAcceso`, los adaptadores ni el contrato de salida.
> El `CodificadorSegEncripta` es `@Component` y se inyecta por interfaz ⇒ el reemplazo es
> un cambio de bean, no una refactorización.

### 3.5 Contexto de sesión

| # | Campo | Tipo | Tipo Java | Cargado por el login | Origen |
|---:|---|---|---|---|---|
| 1 | `Usuario` | CHARACTER(10) | `String` | Sí | `usuario` en **mayúsculas** |
| 2 | `UsuarioNombre` | CHARACTER(50) | `String` | Sí | `SCRTY.UsuNomYApe` |
| 3 | `PersonaId` | NUMERIC(5) | `Integer` | Sí | `PERSONAS.PerId` |
| 4 | `Comision` | NUMERIC(6) | `Integer` | Sí | raíz de comisiones `CPCId` |
| 5 | `Turno` | NUMERIC(8) | `Integer` | Sí | `PPRIM_TU.TurNroId` |
| 6 | `Sucursal` | NUMERIC(2) | `Integer` | Sí | `PUEPPRIM.PueCod` / `RECEPTOR.ReceId` |
| 7 | `SucursalNombre` | CHARACTER(30) | `String` | Sí | `PueDesc` / `ReceDsc` / `Sin Puesto Asignado` |
| 8 | `Tipo` | CHARACTER(3) | `String` | Sí | `'PUE'` / `'REC'` |
| 9 | `Responsable` | NUMERIC(5) | `Integer` | Sí | `CPCResId` |
| 10 | `ResponsableNombre` | VARCHAR(40) | `String` | Sí | `CPCResNyA` |
| 11 | `Form2276` | GX_SDT | `Object` | **No** | módulo 2276 (fuera de alcance) |
| 12 | `F276Id` | NUMERIC(9) | `Integer` | **No** | módulo 2276 (fuera de alcance) |
| 13 | `UltimoMenu` | CHARACTER(30) | `String` | **No** | navegación de menús (fuera de alcance) |

- Persistencia: `session.setAttribute("context", ContextoSesion)` (D-LOG-08).
- La rama "fue responsable" carga **4 campos** (`Usuario`, `UsuarioNombre`, `PersonaId`, `Tipo`)
  y deja los 6 restantes en su valor por defecto (R-LOG-015).
- `Context.Usuario` se guarda en **mayúsculas** en la rama principal; en la rama de historia el
  legacy lo guarda tal cual → **normalizado siempre** (SA-LOG-05, desviación menor documentada).

### 3.6 Autorización por programa

| Programa | Descripción | Enlace legacy | Enlace nuevo |
|---|---|---|---|
| `WWPPrim_Turnos` | `Turnos de Comisiones` | `WWPPrim_Turnos.Link()` | ⚠ **SA-LOG-10** (no hay equivalente en el sistema nuevo) |
| `WWTDetProdPri` | `Buscar Guias` | `WWTDetProdPri.Link()` | ⚠ **SA-LOG-10** |

`ServicioAutorizacion.programasAutorizados()` arma la lista y la filtra con
`VerificadorAutorizacion.isAuthorized(nombre)`, cuya implementación actual devuelve **siempre
`true`** (D-LOG-13). La página `NotAuthorized` muestra el mensaje verbatim.

### 3.7 Modelo de sesión y seguridad

| Aspecto | Decisión | Detalle |
|---|---|---|
| Almacenamiento | `HttpSession` | `JSESSIONID` (identificador de sesión del contenedor). |
| Contexto de seguridad | `SecurityContextHolder` + `SecurityContextRepository` (HTTP session) | `SessionCreationPolicy.IF_REQUIRED`. |
| Principal | `UsuarioAutenticado(usuario, ContextoSesion)` | `record`; `getName()` ⇒ el usuario en mayúsculas. |
| Fijación de sesión | `sessionFixation().changeSessionId()` | Se aplica al autenticar. |
| CSRF | Habilitado **sólo en las rutas MVC con sesión**; **descarte explícito** en todo `/api/**` | `HttpSessionCsrfTokenRepository`; el `POST /login` lo exige (R-LOG-031). Política vigente: **D-LOG-17(a)** (aprobada 2026-10-22), que **sustituye a D-LOG-11**. |
| Materialización del token | **Anticipada** (D-LOG-18(a), aprobada 2026-10-22) | Se conserva `HttpSessionCsrfTokenRepository` y un filtro propio fuerza `csrfToken.getToken()` **antes** del render ⇒ se elimina el `IllegalStateException` de H4 sin tocar el contrato del formulario, la cookie `SUCURSAL` ni el HTML del módulo 02. |
| Rutas permitidas | `/login`, `/api/v1/auth/login`, `/error`, `/css/**`, `/js/**`, `/images/**` | Todo lo demás: `authenticated()`. **`/api/v1/**` (no-login) queda `permitAll`** — **D-LOG-19(a)** (aprobada 2026-10-22), que **sustituye a D-LOG-12**; deuda explícita: SA-LOG-14 + T-135. |
| Respuesta a no autenticado | Redirección `302` a `/login` (rutas MVC) | `LoginUrlAuthenticationEntryPoint` (R-LOG-024). En `/api/**` **no** hay 401 por credenciales ausentes mientras D-LOG-19 esté vigente; el **403** queda reservado a las rutas MVC sin token CSRF (R-LOG-039). |
| Logout | `POST /logout` con CSRF | Invalida la sesión y borca la cookie `SUCURSAL`. |
| Cookies | `SUCURSAL` (tipo) | `HttpOnly`, `SameSite=Lax`, `path=/`, **sin `Max-Age`** (de sesión) — D-LOG-07. `JSESSIONID` también con `SameSite=Lax` (mitigación acordada en §4.5). La alternativa (b) de D-LOG-18 (`CookieCsrfTokenRepository`) **quedó descartada**: agregaría una cookie `XSRF-TOKEN` legible por JS y cambiaría este perfil. |
| Credenciales | Sin `UserDetailsService`/`AuthenticationProvider` | D-LOG-16: la decisión de acceso es de negocio (`ServicioAcceso`). |

### 3.8 Rutas (resumen)

| Canal | Método | Ruta | Perfil | Respuesta |
|---|---|---|---|---|
| MVC | GET | `/login` | INTERNO | vista `nucleo/login` |
| MVC | POST | `/login` | INTERNO | vista `nucleo/login` (200) o `302 → /home` |
| MVC | GET | `/home` | INTERNO | vista `nucleo/home` (protegida) |
| MVC | GET | `/not-authorized` | INTERNO | vista `nucleo/not-authorized` (protegida) |
| MVC | GET | `/` | — | `302 → /home` |
| MVC | POST | `/logout` | INTERNO | `302 → /login` |
| REST | POST | `/api/v1/auth/login` | WS | `200` + `{error, sucursal, comision}` |

---

## 4. Diseño de detalle

### 4.1 Contrato REST — `POST /api/v1/auth/login`

**Request** (`AuthLoginRequestDto`):

```json
{ "usuario": "YOLOFFG", "pass": "********", "tipo": "PUE" }
```

| Clave | Tipo | Restricción | Alias legacy |
|---|---|---|---|
| `usuario` | `String` | `@NotBlank`, `@Size(max=10)` | `Usuario` (SDT `SDTEntradaLoginWS`) |
| `pass` | `String` | `@NotBlank`, `@Size(max=10)` | `Pass` |
| `tipo` | `String` | `@NotBlank`, `@Size(max=3)`, valores `PUE`/`REC` | `Tipo` |

> Los alias (`@JsonAlias`) se incluyen **por compatibilidad** con el nombre de campo del SDT legacy
> (`Usuario`/`Pass`/`Tipo`); el contrato canónico es en minúsculas (SA-LOG-12).

**Response** (`AuthLoginResponseDto`):

```json
{ "error": 0, "sucursal": 10, "comision": 123 }
```

| Clave | Tipo | Significado |
|---|---|---|
| `error` | `int` | Código 0-6 (matriz de §4.3) |
| `sucursal` | `int` | `SucNroId` resuelto; **0** si no se resolvió (`NUMERIC 4`) |
| `comision` | `int` | `CPCId` resuelto; **0** si no se resolvió (`NUMERIC 6`) |

> `jackson.default-property-inclusion: non_null` omite los `null`; las 3 claves son **números**
> y nunca `null` ⇒ **siempre presentes**.

#### Matriz de estados y errores

| Situación | HTTP | Cuerpo | Nota |
|---|---|---|---|
| Acceso concedido | `200` | `{"error":0,"sucursal":N,"comision":M}` | |
| Credenciales inválidas | `200` | `{"error":2,"sucursal":0,"comision":0}` | **No** se usa `401` (paridad con el WS legacy) |
| Usuario sin Persona | `200` | `{"error":3,…}` | |
| Sin comisión/receptoría | `200` | `{"error":4,…}` | |
| Debe cambiar su clave | `200` | `{"error":5,…}` | |
| IP no habilitada | `200` | `{"error":6,…}` | **No producible** por el canal WS (R-LOG-028) |
| Cuerpo ausente o malformado | `400` | `{"code":"CUERPO_INVALIDO","message":…}` | `GlobalExceptionHandler` (canal `/api/**`) |
| Campo que viola `@Size`/`@NotBlank` | `400` | `{"code":"VALIDACION","message":…}` | Ídem |
| Ruta inexistente / método no soportado | `404`/`405` | `{"code":…,"message":…}` | Advice global (D-02 de `00-alcance`) |

> El WS legacy **nunca** devuelve un estado de error: siempre `200` con el cuerpo. Por eso el
> endpoint devuelve `200` también para un acceso denegado (R-LOG-027).
>
> **Política de seguridad del canal (vigente)**: `/api/**` es ***stateless* y sin CSRF** — el
> descarte de CSRF aplica a **todo** `/api/**` (**D-LOG-17(a)**, aprobado 2026-10-22, sustituye a
> D-LOG-11) — y la autenticación es `permitAll` en este módulo (**D-LOG-19(a)**, sustituye a
> D-LOG-12; deuda **SA-LOG-14** + tarea **T-135**). Consecuencia: este endpoint **no** exige token
> CSRF y **no** devuelve **401** por credenciales ausentes; devuelve **200** con el cuerpo del SDT
> (éxito o código de negocio). El **403** queda reservado a las rutas MVC sin token CSRF
> (R-LOG-039, R-LOG-041).

### 4.2 Matriz de códigos y mensajes (tabla única — `MensajesAcceso`)

| Código | Significado | Mensaje **verbatim** | ¿Producido por el canal WS? |
|---:|---|---|---|
| 0 | Acceso concedido | *(sin mensaje)* | Sí |
| 1 | Credenciales inválidas | `Usuario/Contraseña Incorrecto` | No (rama muerta del legacy) |
| 2 | Credenciales inválidas | `Usuario/Contraseña Incorrecto` | Sí |
| 3 | Usuario sin Persona | `Este Usuario no esta relacionado con una Persona del sistema` | Sí |
| 4 | Sin Receptoria o puesto caminero | `No existe Receptoria o puesto caminero asignado` | Sí |
| 5 | Debe cambiar su clave | `Debe cambiar su clave desde el Sistema Integrado (win)` | Sí |
| 6 | IP no habilitada | `Dispositivo no habilitado para el uso en el puesto caminero, contacte con el administrador` | **No** (sin chequeo de IP) |

> Los textos **no se corrigen**: se conservan las erratas "esta" y "Receptoria" (R-LOG-018).
> El canal REST **no** incluye el mensaje: el SDT `SDTSalidaLoginWS` sólo tiene 3 campos.

### 4.3 Contratos de vistas (Thymeleaf)

| Método | Ruta | Vista | Modelo |
|---|---|---|---|
| GET | `/login` | `nucleo/login` | `loginForm` (`LoginForm`), `mensajeError` (`String`, `null`), `tipos` (`List<TipoAcceso>`) |
| POST | `/login` | `nucleo/login` (error) | idem + `tieneErrores` (`boolean`) |
| GET | `/home` | `nucleo/home` | `contexto` (`ContextoSesion`), `programas` (`List<ProgramaAutorizado>`), `textoInicio` (`String`) |
| GET | `/not-authorized` | `nucleo/not-authorized` | `mensaje` (`String`) |

#### `LoginForm` (form-backing object, `web/mvc/login/`)

| Campo | Tipo | Control | `th:field` | Validación | Notas |
|---|---|---|---|---|---|
| `usuario` | `String` | `text`, `maxlength=10` | `usuario` | `@NotBlank`, `@Size(max=10)` | `autocomplete="username"`, `autofocus` |
| `clave` | `String` | `password`, `maxlength=10` | `clave` | `@NotBlank`, `@Size(max=10)` | `autocomplete="current-password"`; **nunca** se vuelve a renderizar tras un error |
| `tipo` | `String` | `select` | `tipo` | `@NotBlank`, `@Pattern("PUE\|REC")` | Opciones `PUE` (Puesto Caminero) y `REC` (Receptoría); precargado desde la cookie `SUCURSAL` |

> El selector de tipo **no** renderiza una opción vacía: el legacy con `Tipo` vacío cae en la
> rama de Receptoría (`&Tipo = 'PUE'` es la única condición), lo que es un efecto colateral
> undesirable. Es la **única desviación funcional deliberada** de la UI (SA-LOG-13).

#### Elementos y texto verbatim de la vista `nucleo/login`

| Elemento legacy | Thymeleaf | Texto |
|---|---|---|
| `gxAttribute var:5` (`&Tipo`) | `<select th:field="*{tipo}">` | rótulo **`Acceso`** |
| `gxAttribute var:6` (`&Usuario`) | `<input type="text" th:field="*{usuario}">` | rótulo **`Usuario`** |
| `gxAttribute var:7` (`&UsuClave`, `IsPassword=True`) | `<input type="password" th:field="*{clave}">` | rótulo **`Contraseña`** |
| `gxButton BtnIngresar` (evento `Enter`) | `<button type="submit">` | **`Ingresar`** |
| `gxTextBlock TextBlockPass` (`ForeColor="Red"`) | `<p class="atp-alert atp-alert--error" th:text="${mensajeError}">` | *(vacío en el estado inicial)* |

> La clave **no** se re-renderiza en el HTML tras un error (el `LoginForm` del `POST` se
> reconstruye con la clave en blanco).

#### Vista `nucleo/home`

| Elemento | Origen legacy | Contenido |
|---|---|---|
| `TextBlock1` de `Inicio` | `Inicio.Event Start` | `El objetivo del Sistema de Control de Puestos Camineros y Receptorías es el registro y seguimiento de los formularios 2276 que serán cargados en el sistema, según se vayan generando por los puestos y receptorías.Otra funcionalidad importante es el control de pase por puestos camineros de los formularios 506 y 509.` (verbatim, **incluida** la falta de espacio antes de "Otra") |
| Datos del contexto | `SetContext` | los 10 campos cargados, con sus rótulos |
| Menú de programas | `ListPrograms` | los 2 programas con nombre y descripción |

#### Estados y navegación

| Vista | Estado | Comportamiento |
|---|---|---|
| `nucleo/login` | `empty` | El formulario es el estado inicial (no hay datos que listar). |
| `nucleo/login` | `error` | `mensajeError` junto al campo de contraseña, en rojo (equivalente a `ForeColor="Red"`). |
| `nucleo/login` | `con errores de binding` | Mensajes junto a cada campo; `Ingresar` deshabilitado si `tieneErrores`. |
| `nucleo/home` | `empty` (programas) | `Sin programas disponibles` + link `Recargar`. |
| `nucleo/home` | `error` | `.atp-alert--error` con el mensaje; **sin** traza ni nombre de excepción. |
| `nucleo/not-authorized` | — | Sólo el mensaje `You are not authorized to do this action`. |

> El estado `loading` **no aplica**: el render es 100 % servidor (R-ALC-006, D-28 de `02-abm`).

### 4.4 CSS y JS

| Archivo | Contenido | Regla |
|---|---|---|
| `static/css/tokens.css` | *(ya existe — `02-abm-productos`)* | Sin literales de color fuera de este archivo. |
| `static/css/nucleo.css` | *(ya existe)* | Se le agregan, si hace falta, `.atp-login` y `.atp-alert--error` (reutilizado). |
| `static/js/login.js` | Toggle de contraseña (mostrar/ocultar) + `autofocus` | Permitido por `docs/design.md` §8. **Sin** `fetch`, sin validación, sin framework. |

> El toggle de contraseña **no** lee el valor de la cookie `SUCURSAL` (es `HttpOnly`).

### 4.5 Política de CSRF y autenticación de `/api/**` — **APROBADA (alternativa (a))**

> **Decisión**: **D-LOG-17** — **APROBADA el 2026-10-22 por el responsable del proyecto**
> (alternativa **(a)**). Cierra **SA-LOG-16**. **Sustituye a D-LOG-11**.
> **Origen**: hallazgos **H3** y **H6** de `docs/verificacion/02-abm-productos.md` §4.

**Política vigente (normativa):**

| Familia de rutas | CSRF | Autenticación | Fuente de autoridad |
|---|---|---|---|
| **MVC con sesión** (`/login`, `/productos`, `/logout`, `/home`, `/not-authorized`) | **Exigido** (token en el formulario) | `authenticated()` (salvo `/login`) | `HttpSession` + `SecurityContext` |
| **`/api/**`** (incluido `POST /api/v1/auth/login`) | **NO exigido** (descarte explícito) | `permitAll` (D-LOG-19) | **ninguna**: el canal es *stateless* |

**Síntoma medido.** `POST /api/v1/productos` con HTTP Basic y **sin** token CSRF ⇒ **HTTP 401**
(`WWW-Authenticate: Basic realm="Realm"`), no 403. Con sesión de navegador, el mismo POST sin token
⇒ **403** (correcto). Causa: el `CsrfFilter` corre **antes** que el `BasicAuthenticationFilter`, así
que en el POST con Basic el request todavía es anónimo cuando el CSRF corta, y el
`AccessDeniedHandlerImpl` delega en el `AuthenticationEntryPoint` ⇒ 401 en lugar de 403. La
colección `docs/postman/ATPPuestos-API.postman_collection.json` **no declara bloque `auth`** (ni a
nivel de colección ni de request; sólo variables `baseUrl`).

**Interacción con lo ya decidido (histórico).** ~~D-LOG-11~~ (**sustituida** por D-LOG-17)
desactivaba el CSRF **únicamente** en `POST /api/v1/auth/login`; ~~D-LOG-12~~ (**sustituida** por
D-LOG-19) dejaba el resto de `/api/v1/**` en `permitAll`. Con CSRF global + `permitAll`, un
`POST /api/v1/productos` seguía dando **403** por CSRF ⇒ la colección Postman **no era ejecutable**
(H3/H6).

| # | Alternativa | Descripción | G-POSTMAN | Módulo 02 | Legacy | Coste / riesgo |
|---|---|---|---|---|---|---|
| **(a)** ← **APROBADA** | **API `/api/**` *stateless* sin CSRF** | El CSRF se desactiva en **todo `/api/**`**; la autenticación de la API **no se resuelve en este módulo** (ver **D-LOG-19**). | ✅ La colección queda **ejecutable** sin credenciales ni pre-requests ⇒ **G-POSTMAN se cumple**. | ✅ Intacto: `/productos` sigue siendo MVC con `th:action` + CSRF; no se toca `ProductoMvcCsrfTest` ni los 12 rótulos. | ✅ Coherente: el canal WS legacy (`Seg_ControlAccesoWS`) es **stateless y sin cookies** ⇒ el token CSRF no tiene sentido. | ⚠️ `/api/**` queda **sin CSRF** ⇒ mitigación acordada (abajo) y deuda de `permitAll` (SA-LOG-14, T-135). |
| **(b)** | **CSRF sólo en las rutas MVC; la API con token propio** | `SecurityFilterChain` separado: MVC con CSRF de sesión; `/api/**` con un token propio (bearer) emitido por la aplicación. | ⚠️ La colección necesita **un pre-request** que obtenga y reenvíe el token ⇒ rompe la directriz «un request por endpoint» y agrega estado entre requests. | ✅ Intacto. | ⚠️ Inventa un mecanismo que **no existe** en el legacy (no hay tokens ni portador de token). | ⚠️ El token tendría que viajan en un artefacto versionado ⇒ roza **G3**. |
| **(c)** | **Mantener CSRF en `/api/**` y documentar el flujo con token** | Se exige token CSRF también en la API; la colección obtiene el token con un `GET` previo y lo reenvía en la cabecera. | ⚠️ La colección sigue siendo ejecutable, pero **depende del orden** entre requests (frágil) y exige el bloque `auth`/pre-request. | ⚠️ Con sesión, el POST sin token sigue dando 403 ⇒ hay que documentar el flujo. | ❌ El WS legacy no manda tokens CSRF ⇒ **rompe la paridad** del canal stateless. | ⚠️ 403 vs 401 confuso (H3/H6). |

**Alternativa aprobada: (a)** — el responsable del proyecto la aprobó el 2026-10-22. Argumento,
punto por punto:

1. **G-POSTMAN (criterio de la puerta):** la colección debe ser **ejecutable por cualquiera** que
   clone el repositorio. Con (a) no hace falta ninguna credencial versionada (lo que además evita
   una violación de **G3**) ni un pre-request con estado. Con (b) y (c) la colección pasa a depender
   de un token que hay que obtener primero.
2. **Módulo 02:** (a) no toca **ni una línea** de la UI: `/productos` sigue siendo un formulario MVC
   con `th:action` y `_csrf`, y sus pruebas (`ProductoMvcCsrfTest`, los 12 rótulos, el renumerado
   `aria`/HTML de las filas) quedan **intactas**. (b) y (c) obligan a revisar el contrato del
   formulario y la forma de la colección de `02`.
3. **Modelo de seguridad del legacy:** el único canal *stateless* del legacy (`Seg_ControlAccesoWS`)
   no tiene sesión ni cookies ⇒ **no puede** usar un token CSRF de sesión. (a) es la única
   alternativa que no **inventa** un mecanismo ausente del legacy.
4. **Deuda explícita:** el riesgo que (a) deja abierto (escrituras sin autenticar) **ya existe** hoy
   (**D-LOG-19**) ⇒ (a) **no introduce ninguna regresión**, y deja la deuda **documentada** en lugar de
   esconderla detrás de un 403 confuso.
5. **Desacoplamiento:** (a) es reversible: más adelante se puede añadir un token propio a `/api/**`
   **sin** tocar las rutas MVC ni el contrato del formulario.

**Consecuencias de la decisión aprobada y mitigación acordada** (la API deja de estar protegida
contra CSRF):

| # | Consecuencia | Mitigación | Verificación |
|---|---|---|---|
| 1 | Un `POST` a `/api/**` **no** es rechazado por CSRF ⇒ un sitio de terceros podría disparar una escritura contra la API desde el navegador de un operador. | La API es **stateless** y **no** toma autoridad de la cookie de sesión: **no hay credencial ambiental** que montar ⇒ el vector no es explotable mientras la API no use cookies. | **T-136** |
| 2 | Si `/api/**` llegara a usar la cookie de sesión, el vector se abriría. | `JSESSIONID` (y `SUCURSAL`) con **`SameSite=Lax`** ⇒ el navegador **no** envía la cookie en un POST intersitio. | **T-136** |
| 3 | La API no exige credenciales en este módulo (`permitAll`, D-LOG-19). | La deuda queda **explícita** (SA-LOG-14, riesgo R-10) con tarea de seguimiento (**T-135**). | **T-135** |
| 4 | Cuando la API se endurezca, la credencial podría viajar en cookie ⇒ volvería el vector CSRF. | La credencial de la API debe viajar **siempre** en la cabecera **`Authorization`**, **nunca** en cookie ⇒ CSRF inaplicable por construcción. | **T-135**, **T-136** |
| 5 | La colección Postman necesita declarar su esquema de autenticación. | Se declara de forma **explícita** el bloque `auth` que corresponde a una API sin credenciales (`noauth`), y el README documenta que la API **no** exige autenticación en este módulo. **Ninguna** credencial real versionada (**G3**). | **T-404** |

**Efecto en los códigos de estado (H3/H6).** Con la política aprobada, un `POST /api/v1/**`
**no** devuelve **403** por token CSRF ausente (el filtro de CSRF **no** actúa sobre `/api/**`) ni
**401** por falta de credenciales (la API es `permitAll`): la petición **se procesa** y responde con
el código de negocio (`201`/`200`/`204`/`400`/`404`). El **403** queda reservado a las **rutas MVC**
sin token CSRF; el **401** queda reservado al endurecimiento futuro de la API (T-135).
Ver **R-LOG-039** y **R-LOG-041**.

### 4.6 Materialización anticipada del token CSRF — **APROBADA (alternativa (a))**

> **Decisión**: **D-LOG-18** — **APROBADA el 2026-10-22 por el responsable del proyecto**
> (alternativa **(a)**). Cierra **SA-LOG-17**.
> **Origen**: hallazgo **H4** de `docs/verificacion/02-abm-productos.md` §4.

**Causa raíz confirmada.** `HttpSessionCsrfTokenRepository` difiere el guardado del token hasta el
primer acceso; ese acceso ocurre en el `th:action` de `templates/nucleo/productos.html`
(línea 103), cuando la respuesta **ya está confirmada** (la página pesa ~56 KB y el panel de
colección con 244 filas se renderiza **antes** del formulario ⇒ se supera el buffer de Tomcat de
8 KB ⇒ *flush* ⇒ *commit*) ⇒ `IllegalStateException: Cannot create a session after the response has
been committed`. El cliente recibe **200 con la página de error** (`ERROR_INTERNO`) en lugar de la
pantalla, y quedan 3 trazas ERROR por petición (reproducible 4/4).

| # | Alternativa | Descripción | Login por formulario | Cookie `SUCURSAL` | HTML / ARIA / renumerado | Pruebas del módulo 02 | Riesgo |
|---|---|---|---|---|---|---|---|
| **(a)** ← **APROBADA** | **Materialización anticipada** conservando `HttpSessionCsrfTokenRepository` | Un filtro propio (o un `OncePerRequestFilter`) fuerza `csrfToken.getToken()` **al inicio** de la petición, antes de que el render escriba ⇒ la sesión se crea a tiempo y el token queda disponible para el `th:action`. Equivalente aceptado: `setRequestAttribute` con `CsrfTokenRequestAttributeHandler`. | ✅ Sin cambios: el `th:action` sigue emitiendo `_csrf`. | ✅ Sin cambios (la cookie no se ve afectada). | ✅ Sin cambios: el HTML es idéntico. | ✅ `ProductoMvcCsrfTest` y los 12 rótulos **intactos**. | ⚠️ Un filtro propio es código de infraestructura nuevo (mantenimiento). |
| **(b)** | **`CookieCsrfTokenRepository`** (token en cookie, sin sesión) | El token vive en una cookie (`XSRF-TOKEN`) y **no** usa la sesión ⇒ el error "Cannot create a session…" desaparece. | ⚠️ Requiere `CsrfTokenRequestAttributeHandler` + `setRequestAttribute` para que el `<input hidden>` reciba el valor; con el handler por defecto (XOR) el formulario **no** encuentra el token ⇒ hay que tocar la plantilla. | ⚠️ **Agrega una cookie** al perfil ⇒ cambia el conjunto de cookies de la app y obliga a revisar el test de cookies del login. | ⚠️ Cambia el HTML del campo oculto ⇒ revalidar el renumerado/`aria` de las vistas del módulo 02. | ⚠️ **Toca `ProductoMvcCsrfTest`** y posiblemente el test de los 12 rótulos. | ⚠️ La cookie del token CSRF **no** puede ser `HttpOnly` (el JS tendría que leerla) ⇒ cambia el modelo de cookies. |
| **(c)** | **`XorCsrfTokenRequestAttributeHandler` + `setRequestAttribute`** | Defense-in-depth frente a BREACH: el token del HTML va enmascarado. | ✅ Funciona con el `th:action` de Spring. | ✅ Sin cambios. | ⚠️ El **valor** del campo `_csrf` cambia (enmascarado) ⇒ los tests que comparan el token **deben** adaptación. | ⚠️ Requeriría adaptación de las pruebas que comparan el token literal. | ⚠️ **Por sí solo NO corrige H4**: el acceso diferido al token sigue creando la sesión tarde ⇒ **no** sirve como solución única. |

**Alternativa aprobada: (a)** — aprobada por el responsable del proyecto el 2026-10-22, con
**(c) como complemento opcional** (no excluyente) si más adelante se considera necesario.

- Es el **cambio mínimo** que ataca la **causa raíz**: la sesión se crea y el token se guarda
  **antes** del render, no durante.
- Es la única alternativa que **no toca** el contrato del formulario (`th:action` sigue emitiendo
  `_csrf` con el mismo valor), **no** cambia el perfil de cookies (`SUCURSAL` intacta), **no**
  cambia el HTML/ARIA de las vistas y **no** obliga a revalidar los tests del módulo 02
  (`ProductoMvcCsrfTest`, los 12 rótulos, el renumerado de la grilla).
- **(b)** funciona, pero introduce una cookie nueva legible por JS, obliga a tocar la plantilla del
  campo oculto y altera el perfil de cookies ⇒ regresión más amplia para el mismo resultado.
- **(c)** **no resuelve H4 por sí solo** ⇒ queda como complemento de endurecimiento, no como
  sustituto.

**Efecto esperado:** `GET /login`, `GET /home` y cualquier pantalla HTML servida **sin** cookie de
sesión previa devuelven **200 con su formulario y su `_csrf`** (R-LOG-042, R-LOG-043) y el log
**no** registra trazas ERROR por ese `IllegalStateException`.

### 4.7 Nivel de autenticación de `/api/v1/**` — **APROBADA (alternativa (a))**

> **Decisión**: **D-LOG-19** — **APROBADA el 2026-10-22 por el responsable del proyecto**
> (alternativa **(a)**). **Sustituye a D-LOG-12**. Mantiene **SA-LOG-14** como deuda **asumida**.

| # | Alternativa | G-POSTMAN | G3 (secretos) | Legacy | Impacto |
|---|---|---|---|---|---|
| **(a)** ← **APROBADA** | **`permitAll`** (estado actual) | ✅ La colección se ejecuta tal cual, sin credenciales. | ✅ Sin credenciales en el repo. | ✅ El WS legacy no tiene sesión. | Sin regresión; **deuda documentada** (SA-LOG-14, R-10) con tarea **T-135**. |
| **(b)** | **`authenticated()` con sesión** | ❌ Los 44+9 requests pasarían a `401`/`302` ⇒ colección no ejecutable. | ✅ | ⚠️ El WS legacy no usa sesión. | Rompe G-POSTMAN y la suite de `01`/`02`. |
| **(c)** | **HTTP Basic contra `SCRTY`** | ⚠️ Requiere credenciales en la colección o en el environment versionado ⇒ **G3**. | ❌ | ⚠️ El legacy no expone un canal Basic. | Introduce un secreto en el repositorio o una collection no reproducible. |

**Consecuencias de (a), asumidas de forma explícita:**

1. `/api/v1/**` (44 requests de `01-nucleo-datos` + 9 de `02-abm-productos` + los de `Auth`) queda
   **accesible sin autenticación** en este módulo ⇒ **deuda de seguridad asumida**: **SA-LOG-14**,
   riesgo **R-10**.
2. El endurecimiento queda como **tarea posterior y explícita: T-135** (token propio de la
   aplicación, viajando en la cabecera `Authorization`, **sin** credenciales versionadas ⇒ **G3**).
3. La colección Postman declara de forma explícita el bloque `auth` que corresponde a una API sin
   credenciales (`noauth`) ⇒ el ejecutor no hereda nada por sorpresa (**T-404**).

---

## 5. Paridad de BD

> **No se modifica el esquema.** Estructura existente reutilizada (`ddl-auto=none`).
> **Sin DDL, sin migraciones, sin `schema.sql`/`data.sql`, sin Flyway/Liquibase** (G5).
> Fuente de autoridad: `INFORMATION_SCHEMA` de `Rentas` (G-DATA 2026-10-22, `docs/analisis-ATPPuestos.md` §15)
> contrastada con las estructuras lógicas de la KB (`genexus_db sql_ddl`).
> **Este módulo no escribe en ninguna de estas tablas** ⇒ no hay defaults de aplicación que declarar.

### 5.1 `SCRTY` (PK `UsuId`) — 16 columnas físicas

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación | Estado |
|---|---|---|---|---|---|---|
| `UsuId` | `UsuId` | `char(10)` | NO | `String` | `@Id @Column(name="UsuId", nullable=false, length=10)` | G-DATA ✓ |
| `UsuClave` | `UsuClave` | `char(10)` | **sí** | `String` | `@Column(name="UsuClave", length=10)` + `@ColumnTransformer(read="RTRIM(UsuClave)")` | G-DATA ✓ (0 nulos en los 552 datos; **nullable** en el esquema) |
| `UsuNomYApe` | `UsuNomYApe` | `char(35)` | **sí** | `String` | `@Column(name="UsuNomYApe", length=35)` + `RTRIM` | G-DATA ✓ (0 nulos en los 552 datos; **nullable** en el esquema) |
| `UsuIngreso` | `UsuIngreso` | `smallint` | sí | `Integer` | `@Column(name="UsuIngreso")` | G-DATA ✓ — **1 de las 552 filas es `NULL`** ⇒ R-LOG-037 |
| `UsuNiv`, `UsuGrupo`, `UsuTipDoc`, `UsuNroDoc`, `UsuNotiAre`, `UsuArea`, `UsuEMail` | — | — | — | — | **No mapeadas** | El login no las lee (D-LOG-14) |
| `UsuLegajo` (`int`), `UsuFoto` (`varbinary(max)`), `UsuIdComarb` (`varchar(15)`), `UsuAppFchAdhesion` (`datetime`), `UsuHash` (`varchar(256)`) | — | — | — | — | **No mapeadas** (huérfanas) | El módulo no escribe ⇒ sin default; `UsuHash` fuera de alcance (SA-LOG-03) |

> **Corrección de la revisión v2**: `UsuClave` y `UsuNomYApe` son **nullable** en el esquema (la
> versión anterior los declaraba `NOT NULL`). No cambia el comportamiento: los 552 registros tienen
> valor.

### 5.2 `PERSONAS` (PK `PerId`) — 4 columnas físicas

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación | Estado |
|---|---|---|---|---|---|---|
| `PerId` | `PerId` | `int` | NO | `Integer` | `@Id @Column(name="PerId", nullable=false)` | G-DATA ✓ |
| `PerNomyApe` | `PerNomyApe` | `char(40)` | **sí** | `String` | `@Column(name="PerNomyApe", length=40)` + `RTRIM` | G-DATA ✓ (0 nulos en las 183 filas; **nullable** en el esquema) |
| `PerCodUsu` | `PerCodUsu` | `char(10)` | **sí** | `String` | `@Column(name="PerCodUsu", length=10)` | G-DATA ✓ (0 nulos; 147 con contenido y 36 con espacios) |
| `AreaId` | — | — | — | — | **No mapeada** | El login no la lee (`decimal(10,0)`, nullable) |

### 5.3 Las 6 tablas de contexto operativo — verificadas el 2026-10-22

> **G-DATA 2026-10-22** (`INFORMATION_SCHEMA` de `Rentas`, sólo `SELECT`). **SA-LOG-08 cerrado.**
> Servidor: *Microsoft SQL Server 2019 … 15.0.2170.1*. Esquema por defecto del usuario de
> aplicación: `Rentas`.

| Tabla física | Entidad | PK física | Columnas que mapea el login | Filas | Estado |
|---|---|---|---|---:|---|
| `PUEPPRIM` | `Puesto` | `PueCod` (`smallint`) | `PueCod`, `PueDesc` (`char(20)`, nullable), `PueIPRed` (`varchar(40)`, nullable) | 14 | G-DATA ✓ — 11 de 14 con `PueIPRed` vacío/nulo |
| `RECEPTOR` | `Receptor` | `ReceId` (`smallint`) | `ReceId`, `ReceDsc` (`char(30)`, nullable) | 24 | G-DATA ✓ (`AreaId` y `ReceNLiq` no se mapean) |
| `RECEPTO1` | `ReceptorPersona` | (`ReceId` `smallint`, `PerId` `int`) | `ReceId`, `PerId` | 69 | G-DATA ✓ — `RecUsuFchD` (`datetime`, nullable) **no** se mapea (el `defined by` del legacy está comentado) |
| **`COMISION`** | `Comision` | (`PueCod` `smallint`, `CPCId` `int`) | `PueCod`, `CPCId`, `CPCFchDes` (`datetime`), `CPCFchHas` (`datetime`), `CPCHorDes` (`int`), `CPCHorHas` (`int`), `CPCCierre` (`char(1)`), `CPCResId` (`int`) | 3 120 | G-DATA ✓ — **`CPCResNyA` NO existe** (atributo calculado ⇒ JOIN a `PERSONAS`, D-LOG-20) |
| **`COMISIO1`** | `ComisionIntegrante` | (`PueCod` `smallint`, `CPCId` `int`, `CPCPerId` `int`) | `PueCod`, `CPCId`, `CPCPerId` | 7 238 | G-DATA ✓ — **`CPCPerNyA` NO existe** (atributo calculado ⇒ JOIN a `PERSONAS`, D-LOG-20) |
| `PPRIM_TU` | `TurnoPPrim` | (`SucNroId` `smallint`, `SucTipo` `char(3)`, `TurNroId` `int`) | `SucNroId`, `SucTipo`, `TurNroId`, `TurEstado` (`char(1)`), `TurCPCId` (`smallint`, nullable) | 54 768 | G-DATA ✓ — `TurCPCId` es `NULL` en 499 filas |

**Columnas huérfanas de las 6 tablas de contexto** (existen en la BD, no en la KB y no las lee el
login ⇒ **no se mapean**, sin default de aplicación porque el módulo no escribe):
`COMISION`: `CPCFeLog` (`datetime`), `CPCUsuLog` (`char(10)`), `CPCEstado` (`char(1)`),
`CPCFchCie` (`datetime`), `CPCSnNum` (`decimal(10,0)`), `CPCFePag` (`datetime`) ·
`PPRIM_TU`: `TurUsuario` (`char(10)`), `TurFchApe`, `TurFchCie`, `TurHraApe`, `TurHraCie`
(`datetime`), `TurFchPago` (`datetime`), `SNSinNum` (`decimal(10,0)`) ·
`RECEPTO1`: `RecUsuFchD` (`datetime`) · `RECEPTOR`: `AreaId`, `ReceNLiq` (`int`) ·
`PERSONAS`: `AreaId` (`decimal(10,0)`).

**Distribuciones verificadas** (útiles para los IT):

| Dato | Valor |
|---|---|
| `COMISION.CPCCierre` | `'N'` = 32 · `'S'` = 3 083 · `'P'` = 1 · vacío = 4 (total 3 120) |
| `COMISION.CPCFchDes` / `CPCFchHas` nulos | 0 / 0 |
| `COMISION.CPCResId` nulos | 0 de 3 120 |
| `PPRIM_TU.TurEstado` | `'A'` = 125 · `'P'` = 1 · `'C'` = 54 642 |
| `PPRIM_TU.SucTipo` | `'PUE'` = 27 197 · `'REC'` = 27 571 |
| `SCRTY.UsuIngreso` | `1` = 10 · `NULL` = 1 · resto = 541 |
| `SCRTY.UsuClave` vacía (`LTRIM(RTRIM())=''`) | 2 |
| `PUEPPRIM.PueIPRed` vacía/nula | 11 de 14 |
| `COMISIO1` integrantes distintos (`CPCPerId`) | 139 |

> **Divergencia KB ↔ BD resuelta**: `docs/analisis-ATPPuestos.md` §15.1 acierta con **`COMISION`**
> (la hipótesis del truncamiento a 8 caracteres de `plan.md` §5.3 era **incorrecta**) y
> `COMISIO1` **sí** existe; lo que §15.1 no registra es que `CPCPerNyA` / `CPCResNyA` **no son
> columnas** (el DDL del MCP las emite por ser una reconstrucción heurística del SDK) ⇒ la tarea
> **T-403** corrige §15 con la evidencia de esta revisión.
> `dbo.AU_COMISION` (17 columnas, esquema `dbo`) es una tabla de **auditoría de eventos** de otra
> aplicación: **no** se mapea y **no** se confunde con `COMISION`.

### 5.4 Estado de la verificación G-DATA

| Comprobación | Resultado |
|---|---|
| `INFORMATION_SCHEMA.TABLES` / `COLUMNS` / `KEY_COLUMN_USAGE` de las 8 tablas | **EJECUTADA** el 2026-10-22 contra `devmssqlatp.ecom.com.ar:1433` (sólo `SELECT`) ⇒ **SA-LOG-08 cerrado** (§5.3, `spec.md` §3.1.1). |
| Conectividad | **Resuelta**: el host resuelve y acepta el login con las credenciales de `.env` ⇒ **SA-LOG-09 cerrado**. El servidor es **SQL Server 2019 (15.0.2170.1)**. |
| Estructuras lógicas de la KB | Verificadas con `genexus_read` (sólo lectura): transacciones `Comisiones` (`Root Module/Comisiones/Comisiones`), tabla `Comisio1`, procedimientos `Seg_BusComi`, `Seg_BusResponsable`, `Seg_BusRece`, `Seg_BusPer`, `PPrim_BusTur`, `Seg_BusPuesto` (sin callers). |
| Contraste KB ↔ BD | **Hecho**: la única divergencia detectada es la de los **atributos calculados** (`CPCPerNyA`, `CPCResNyA`, `PueDesc`, `ReceDsc`) ⇒ D-LOG-20 y **SA-LOG-15** (fórmula exacta no legible por el MCP). |
| Claves foráneas | **Ninguna** entre las 8 tablas: las relaciones son lógicas y las reproduce el repositorio (R-LOG-035/036). |

### 5.5 Reglas de BD aplicables

| Regla | Detalle |
|---|---|
| `ddl-auto` | `none` — **no se toca** `application.yml` en esa sección (T-308). |
| Lectura | Sólo `SELECT`. El módulo **no** inserta, actualiza ni borra. |
| `char(n)` fijo | Se lee con `RTRIM` (`@ColumnTransformer(read="RTRIM(…)")`), como en `Producto.ProdDesc`. |
| Nombres físicos | Exactos; **no** se renombra nada. Los truncados (`UsuNotiAre`, `PerNomyApe`) se respetan tal cual. |
| Columnas huérfanas | No se mapean porque no se leen ni se escriben (D-LOG-14). Inventario completo en §5.3. Si un módulo futuro escribe, debe mapearlas con su default (regla R-036 de `01-nucleo-datos`). |
| Atributos calculados del legacy | `CPCPerNyA`, `CPCResNyA`, `PueDesc` (transacción `Comisiones`) y `ReceDsc` (`Seg_BusRece`) **no son columnas**: se resuelven por JOIN (D-LOG-20, R-LOG-035). **Prohibido** declararlos como `@Column`. |
| Sin FK físicas | No hay claves foráneas entre las 8 tablas ⇒ las relaciones se resuelven en los repositorios (JOIN explícito), no por asociación JPA automática. |
| Sin `IDENTITY` / `COLUMN_DEFAULT` | Irrelevante aquí (no hay alta). La regla de "la PK viaja en el body" no aplica a este módulo. |
| Auditoría | `app.auditoria.usuario` deja de ser `SISTEMA` cuando hay sesión autenticada (R-LOG-032). |

---

## 6. Supuestos abiertos

> Cada supuesto lleva la **pregunta exacta** que hay que responder. Los `SA-L0x` del `spec.md` §6
> se renumeran aquí como `SA-LOG-0x` para que el módulo tenga numeración propia.

| ID | Supuesto | Pregunta exacta | Evidencia faltante | Quién lo define | Impacto |
|---|---|---|---|---|---|
| **SA-LOG-01** (= SA-L01) | Semántica de `IndexOf` en el chequeo de IP | «¿Qué devuelve `IndexOf(<ip>, <prefijo>)` en el runtime GeneXus 18 cuando la IP **no** empieza con el prefijo, y qué devuelve cuando el prefijo está vacío (11 de los 14 `PUEPPRIM` lo tienen)?» | Runtime GeneXus | Legacy / soporte ATP | Define si el chequeo **deniega** o **permite** |
| **SA-LOG-02** (= SA-L02) | Alcance real de `IsAuthorized` | «¿La autorización por programa debe quedar activa (con qué tabla o regla) o se replica el comportamiento actual —todos los programas visibles? ¿Hay una fuente de autorización fuera de esta KB?» | Fuente de autorización | Legacy / negocio | Define el contenido del menú de `Home` |
| **SA-LOG-03** (= SA-L03) | `UsuHash` (PBKDF2) | «¿El login nuevo debe validar contra `UsuHash` cuando `UsuClave` está vacía, o se mantiene la paridad legacy (código 2) y esas 2 cuentas siguen derivando al Sistema Integrado?» | Decisión de producto | Producto / seguridad | 2 de 552 cuentas |
| **SA-LOG-04** (= SA-L04) | Padding a 10 del identificador | «¿El runtime rellena automáticamente la variable `CHARACTER(10)` o el usuario debe tipear los espacios?» | Runtime GeneXus | Legacy | Sin padding el login es inusable |
| **SA-LOG-05** (= SA-L05) | `Context.Usuario` en la rama de historia | «¿Se conserva la diferencia (minúsculas permitidas en ese camino) o se normaliza siempre a mayúsculas?» | Decisión de negocio | Legacy | Representación duplicada del usuario en sesión |
| **SA-LOG-06** (= SA-L06) | `PueIPRed`: prefijo de texto o máscara | «¿`PUEPPRIM.PueIPRed` es un prefijo de texto (`startsWith`) o una máscara CIDR?» | Configuración de red | Infraestructura / legacy | 3 de los 14 puestos tienen prefijo |
| **SA-LOG-07** (= SA-L07) | Atributos de la cookie `SUCURSAL` | «¿De sesión o persistente? ¿`SameSite`/`HttpOnly`/`Secure`?» | Configuración de despliegue | Infraestructura | D-LOG-07 fija un valor provisional |
| **SA-LOG-08** | Nombre físico y esquema de las tablas de comisiones y turnos | — | — | — | **CERRADO (2026-10-22, revisión v2)**: `INFORMATION_SCHEMA` de `Rentas` confirma **`COMISION`** (raíz, PK (`PueCod`,`CPCId`), 14 columnas, 3 120 filas), **`COMISIO1`** (PK (`PueCod`,`CPCId`,`CPCPerId`), **3** columnas, 7 238 filas), `CPCFchDes`/`CPCFchHas` (`datetime`) y `CPCHorDes`/`CPCHorHas` (`int`) existentes, y PK de `PPRIM_TU` = (`SucNroId`,`SucTipo`,`TurNroId`). Evidencia: `spec.md` §3.1.1 y §5.3 de este plan. ⇒ **T-002 puede cerrarse** con esta evidencia. |
| **SA-LOG-09** | La BD no fue alcanzable en la sesión de redacción | — | — | — | **CERRADO (2026-10-22, revisión v2)**: `devmssqlatp.ecom.com.ar:1433` resuelve, acepta el login y responde con `sqlcmd` (SQL Server 2019, 15.0.2170.1) desde el entorno de trabajo. ⇒ **T-003 puede cerrarse**. |
| **SA-LOG-10** *(nuevo)* | Enlace de los programas del menú | «`ListPrograms` usa `WWPPrim_Turnos.Link()` y `WWTDetProdPri.Link()`, que apuntan a WebPanels legacy sin equivalente en el sistema nuevo. ¿Qué ruta MVC debe mostrar el menú de `Home`, o el menú se muestra sin enlace hasta que existan esos módulos?» | Ruta destino | Legacy / arquitectura | Enlace del menú de `Home` |
| **SA-LOG-11** *(nuevo)* | Uso de `UsuGrupo` / `UsuNiv` para autorizar | «En el legacy `IsAuthorized` siempre devuelve `True` y no mira el grupo ni el nivel (hay 23 grupos y 10 niveles en `SCRTY`). ¿El sistema nuevo debe filtrar por grupo/nivel, o se replica el legacy?» | Regla de autorización | Negocio / seguridad | Contenido del menú |
| **SA-LOG-12** *(nuevo)* | Nombres de campo del canal WS | «El SDT legacy serializa `{Usuario, Pass, Tipo}` (con mayúscula). ¿El cliente mobile existente envía esos nombres o los del contrato nuevo en minúsculas `{usuario, pass, tipo}`? Si son los legacy, el DTO debe llevar `@JsonAlias` (o sólo los legacy).» | Código del cliente mobile | Legacy / WS | Compatibilidad del contrato REST |
| **SA-LOG-13** *(nuevo)* | Valor por defecto del selector `Acceso` | «En el legacy `&Tipo` arranca vacío y un tipo vacío cae en la rama de Receptoría. ¿El formulario nuevo debe exigir un valor (valor por defecto `PUE`) o replicar el vacío?» | Decisión de UX/negocio | Producto / legacy | 1 rama del algoritmo |
| **SA-LOG-14** *(nuevo)* | Endurecimiento del API de datos | «¿Cuándo se exige autenticación a los endpoints `/api/v1/**` que no son de login (los 44 requests de `01-nucleo-datos`)? Endurecerlos ahora rompe la colección Postman y la suite vigente.» | Plan de securing | Arquitectura / seguridad | **DEUDA ASUMIDA** (no bloqueante): **D-LOG-19(a)**, aprobada 2026-10-22 ⇒ `permitAll` en este módulo, con el riesgo **R-10** registrado y el seguimiento en la tarea **T-135** (token propio de la app en la cabecera `Authorization`, sin credenciales versionadas ⇒ G3) |
| **SA-LOG-15** *(nuevo, revisión v2)* | Fórmula de los atributos calculados `CPCPerNyA` / `CPCResNyA` | «¿Qué fórmula tienen en la KB los atributos calculados `CPCPerNyA` y `CPCResNyA` — un `join` a `PERSONAS.PerNomyApe` por `CPCPerId`/`CPCResId`, o algún otro origen (p. ej. un valor congelado en otra estructura del legacy)?» El MCP **no** expone la fórmula (los objetos `Attribute` no tienen `parts`). | Runtime GeneXus / definición de atributos calculados | Legacy | **ABIERTO, asumido**: hipótesis de trabajo `PERSONAS.PerNomyApe` (única columna de nombre de persona de la base, `char(40)`, 0 nulos). **No bloquea** la escritura de las entidades (esas columnas no se mapean) pero sí el criterio de desempate de **R-LOG-036** ⇒ tarea **T-008** |
| **SA-LOG-16** *(nuevo, revisión v2)* | Política de CSRF y esquema de autenticación de `/api/**` | — | — | — | **CERRADO (2026-10-22)**: el responsable del proyecto **aprobó la alternativa (a)** de **D-LOG-17** ⇒ `/api/**` *stateless* y **sin CSRF**; CSRF sólo en las rutas MVC con sesión. Alternativas y análisis: §4.5 |
| **SA-LOG-17** *(nuevo, revisión v2)* | Mecanismo de materialización anticipada del token CSRF | — | — | — | **CERRADO (2026-10-22)**: el responsable del proyecto **aprobó la alternativa (a)** de **D-LOG-18** ⇒ se conserva `HttpSessionCsrfTokenRepository` y el token se materializa temprano (filtro propio). Alternativas y análisis: §4.6 |
| **SA-LOG-18** *(nuevo, revisión v2)* | Alcance de G4 sobre `docs/postman/` | «¿G4 aplica a los artefactos de `docs/postman/`? Si aplica: ¿se divide la colección en un archivo por módulo con un índice en `README.md`, o se registra una excepción explícita para los artefactos de documentación generated?» | Decisión de coordinación | Coordinación / verificador-qa | **ABIERTO, asumido**: resolución provisional = **split por módulo** (un archivo por módulo + índice) ⇒ tarea **T-406**. `docs/` es territorio de `verificador-qa`/coordinación: este módulo sólo redacta el requisito |
| **SA-LOG-18** *(nuevo, revisión v2)* | Alcance de G4 sobre `docs/postman/` | «¿G4 aplica a los artefactos de `docs/postman/`? Si aplica: ¿se divide la colección en un archivo por módulo con un índice en `README.md`, o se registra una excepción explícita para los artefactos de documentación generated?» | Decisión de coordinación | Coordinación / verificador-qa | Resolución provisional: **split por módulo** (un archivo por módulo + índice) ⇒ tarea **T-406**. `docs/` es territorio de `verificador-qa`/coordinación: este módulo sólo redacta el requisito |

---

## 7. Trazabilidad (requerimiento ↔ decisión ↔ tarea)

| Req. | Decisión | Tarea(s) |
|---|---|---|
| R-LOG-001 | D-LOG-09 | T-104, T-106 |
| R-LOG-002 | D-LOG-03, D-LOG-09 | T-104 |
| R-LOG-003 | D-LOG-03 | T-104, T-304 |
| R-LOG-004 | D-LOG-05 | T-102, T-107 |
| R-LOG-005 | D-LOG-03, D-LOG-14 | T-107 |
| R-LOG-006 | D-LOG-03 | T-104, T-107 |
| R-LOG-007 | D-LOG-03 | T-107 |
| R-LOG-008 | D-LOG-05 | T-103, T-107 |
| R-LOG-009 | D-LOG-06, D-LOG-16 | T-108 |
| R-LOG-010 | D-LOG-01 | T-108 |
| R-LOG-011 | D-LOG-01 | T-103, T-108 |
| R-LOG-012 | D-LOG-01 | T-103 |
| R-LOG-013 | D-LOG-10 | T-111 |
| R-LOG-014 | D-LOG-01 | T-103 |
| R-LOG-015 | D-LOG-06, D-LOG-08 | T-108 |
| R-LOG-016 | D-LOG-08 | T-106, T-108, T-109 |
| R-LOG-017 | D-LOG-08, D-LOG-07 | T-109, T-124 |
| R-LOG-018 | D-LOG-04 | T-105, T-302, T-303 |
| R-LOG-019 | D-LOG-04, D-LOG-16 | T-105, T-107, T-302 |
| R-LOG-020 | D-LOG-04 | T-201, T-203 |
| R-LOG-021 | D-LOG-04 | T-107, T-301, T-302 |
| R-LOG-022 | D-LOG-02, D-LOG-07 | T-122, T-201 |
| R-LOG-023 | D-LOG-07, D-LOG-15 | T-123, T-301 |
| R-LOG-024 | D-LOG-07, D-LOG-16 | T-122, T-301 |
| R-LOG-025 | D-LOG-13 | T-110, T-205, T-304 |
| R-LOG-026 | D-LOG-13 | T-205, T-301 |
| R-LOG-027 | D-LOG-06 | T-120, T-121, T-303, T-402 |
| R-LOG-028 | D-LOG-06, D-LOG-17, D-LOG-19 | T-121, T-125 |
| R-LOG-029 | D-LOG-06 | T-121, T-304 |
| R-LOG-030 | D-LOG-01 | T-401 |
| R-LOG-031 | D-LOG-07, D-LOG-17 | T-124, T-125, T-301 |
| R-LOG-032 | D-LOG-07 | T-130, T-305 |
| R-LOG-033 | D-LOG-20 | T-002, T-132, T-305 |
| R-LOG-034 | D-LOG-20 | T-132, T-305 |
| R-LOG-035 | D-LOG-20 | T-133, T-305 |
| R-LOG-036 | D-LOG-20 | T-008, T-133, T-311 |
| R-LOG-037 | D-LOG-05 | T-102, T-304 |
| R-LOG-038 | D-LOG-05 | T-133, T-304 |
| R-LOG-039 | D-LOG-17, D-LOG-19 | T-126, T-312 |
| R-LOG-040 | D-LOG-17, D-LOG-19 | T-313, T-404 |
| R-LOG-041 | D-LOG-17 | T-126, T-136 |
| R-LOG-042 | D-LOG-18 | T-128, T-311 |
| R-LOG-043 | D-LOG-18 | T-128, T-311 |
| R-LOG-044 | D-LOG-07 | T-406 |
| R-LOG-045 | D-LOG-19 | T-405 |
| R-LOG-046 | D-LOG-21 | T-129, T-314 |

---

## 8. Impacto en G-POSTMAN y en las pruebas de vistas

### 8.1 Colección Postman — `docs/postman/ATPPuestos-API.postman_collection.json`

| Acción | Detalle |
|---|---|
| **Agregar** | Carpeta **`Auth`** con **1 request**: `POST {{baseUrl}}/api/v1/auth/login`, body `{ "usuario": …, "pass": …, "tipo": "PUE" }`, test que verifica **status 200** y la presencia de las 3 claves `error`, `sucursal`, `comision`. |
| **Agregar** (opcional) | Un segundo request "login con credenciales inválidas" dentro de la misma carpeta, con un test que verifica `error = 2` — es el caso de negocio más importante del canal WS. |
| **Modificar** | **Ninguno** de los 44 requests existentes: este módulo **no** cambia ningún endpoint preexistente (**D-LOG-19(a)** deja `/api/v1/**` igual que está). |
| **Eliminar** | Ninguno. |
| **Documentación** | `docs/postman/README.md`: actualizar §"Cobertura actual" (de 9 carpetas / 44 requests a **10 carpetas / 45-46 requests**) y agregar una fila al *Changelog*. |

> **Regla G-POSTMAN**: el cambio de la colección y el del backend van en el **mismo commit** (T-401).
> Si T-120 cambia la forma del body, la colección se actualiza en ese mismo commit.

#### 8.1.1 Revisión v2 — ejecutabilidad de la colección (H3/H6, H5, H7)

> **Política vigente (aprobada el 2026-10-22)**: `/api/**` es *stateless* y **sin CSRF**
> (**D-LOG-17(a)**) y **no exige credenciales** en este módulo (**D-LOG-19(a)**) ⇒ la colección
> **no** necesita pre-requests ni tokens CSRF.

| Acción | Detalle | Requisito |
|---|---|---|
| **Aplicado** | La política CSRF y el esquema de autenticación de `/api/**` quedan **aprobados** (D-LOG-17(a) y D-LOG-19(a)) ⇒ la API **no** rechaza por CSRF y la colección es ejecutable tal cual. | R-LOG-039, R-LOG-040, R-LOG-041 |
| **Agregar** | Bloque `auth` **explícito** a nivel de colección con el valor que corresponde a una API **sin credenciales** (`noauth`), para que el ejecutor no herede nada por sorpresa; **sin ninguna credencial versionada** (**G3**). | R-LOG-040 (T-404) |
| **Corregir** | Los cuerpos de ejemplo que envían campos **derivados o informativos** (p. ej. `uMedNombre`) se alinean con el contrato real de los DTOs de escritura. | R-LOG-045 (T-405) |
| **Dividir** | La colección monolítica (**1990** líneas) se divide en **un archivo por módulo** dentro de `docs/postman/`, con el índice en `docs/postman/README.md`; ningún archivo supera las **500** líneas (G4). | R-LOG-044 (T-406) |
| **Verificar** | Un recorrido completo de la colección contra la app real (carpetas de `01-nucleo-datos`, `Productos` y `Auth`) **sin** errores de estado y **sin** token CSRF. | R-LOG-040 (T-313) |

### 8.2 Pruebas de vistas que se agregan (`@WebMvcTest` + `MockMvc`)

| Suite | Cobertura | Mínimo |
|---|---|---|
| `MvcLoginTest` | render del formulario, 3 rótulos verbatim, token CSRF, tipo precargado desde la cookie, `Usuario inválido`, mensaje de código ≠ 0 junto a la contraseña, re-render sin la clave, cookie `SUCURSAL` presente/ausente, redirección a `/home` | ≥ 12 pruebas |
| `MvcHomeTest` | protección de rutas (302 a `/login`), render con contexto, lista de programas, estado `empty` | ≥ 5 pruebas |
| `MvcNotAuthorizedTest` | ruta, nombre de vista y texto exacto del mensaje | ≥ 2 pruebas |
| `MvcSecurityTest` | `POST /login` sin CSRF ⇒ 403; `GET /home` sin sesión ⇒ 302; `POST /logout` ⇒ sesión invalidada | ≥ 4 pruebas |
| `MvcAuditoriaTest` | el usuario auditor de una escritura posterior usa `Context.Usuario` y no `SISTEMA` | ≥ 1 prueba |

> **Total mínimo del módulo: ≥ 24 pruebas de vistas**, sin regresión sobre las suites de
> `01-nucleo-datos` y `02-abm-productos`.

---

## 9. Matriz de paridad a cerrar (QA)

### 9.1 Funcional

| Legacy (KB) | Requisito | Prueba prevista |
|---|---|---|
| `Seg_Encripta` (espejo 67−i) | R-LOG-002, R-LOG-003 | `CodificadorSegEncriptaTest` (vectores + ida y vuelta sobre las 552 filas) |
| `Seg_ControlAcceso` — orden de comprobaciones | R-LOG-004…R-LOG-015 | `ServicioAccesoTest` (12 casos, uno por código) + `ServicioAccesoIT` |
| `Login.Event Enter` — `Do Case` sobre `&error` | R-LOG-018 | `MensajesAccesoTest` (7 pares, comparación carácter a carácter) |
| `Login.Event Enter` — `If &Usuario.IsEmpty()` | R-LOG-021 | `MvcLoginTest` |
| `Login.Event Start` — `GetCookie('SUCURSAL')` | R-LOG-022, R-LOG-023 | `MvcLoginTest` |
| `Login.Event Enter` — `SetCookie('SUCURSAL', &Tipo)` | R-LOG-023 | `MvcLoginTest` |
| `SetContext` / `LoadContext` | R-LOG-016, R-LOG-017 | `ServicioAccesoIT` + `MvcHomeTest` |
| `Home.Event Start` | R-LOG-024 | `MvcHomeTest` |
| `ListPrograms` + `IsAuthorized` | R-LOG-025 | `ServicioAutorizacionTest` |
| `NotAuthorized` | R-LOG-026 | `MvcNotAuthorizedTest` |
| `Seg_ControlAccesoWS` | R-LOG-027, R-LOG-028, R-LOG-029 | `AuthControllerTest` (MockMvc) + `AuthControllerIT` |
| `docs/design.md` §5/§7/§8 | R-LOG-020, R-LOG-031 | `axe-core` + revisión del JS |
| **Física de las tablas** (`COMISION`, `COMISIO1`, `PPRIM_TU`, `RECEPTO1`) | R-LOG-033, R-LOG-034 | `ComisionRepositoryIT` + inspección de `@Table`/`@Id` |
| **Atributos calculados** (`CPCPerNyA`, `CPCResNyA`, `ReceDsc`) | R-LOG-035 | `ComisionRepositoryIT` (nombre del responsable y de la receptoría) |
| **`defined by CPCPerNyA`** (orden de `Seg_BusComi`) | R-LOG-036 | `ComisionRepositoryIT` (persona con 2 comisiones vigentes) |
| **`UsuIngreso` nulo** | R-LOG-037 | `ServicioAccesoIT` (el usuario real con `UsuIngreso IS NULL`) |
| **`CPCHorHas` numérico** | R-LOG-038 | `ComisionRepositoryIT` (comisión que vence hoy) |

### 9.2 Base de datos

| Legacy | Nuevo | Prueba |
|---|---|---|
| `SCRTY` (11 columnas KB + 5 huérfanas) | `Usuario` (4 columnas leídas) | `UsuarioRepositoryIT` |
| `PERSONAS` | `Persona` (3 columnas) | `PersonaRepositoryIT` |
| `PUEPPRIM` | `Puesto` (3 columnas) | `PuestoRepositoryIT` |
| `RECEPTOR` + `RECEPTO1` | `Receptor`, `ReceptorPersona` | `ReceptorRepositoryIT` |
| comisiones (`COMISION`, `COMISIO1`) | `Comision`, `ComisionIntegrante` (PK compuestas) | `ComisionRepositoryIT` — **desbloqueado**: SA-LOG-08 cerrado |
| `PPRIM_TU` | `TurnoPPrim` (PK `SucNroId`+`SucTipo`+`TurNroId`) | `TurnoRepositoryIT` — **desbloqueado**: SA-LOG-08 cerrado |
| `char(n)` fijo | `RTRIM` al leer | Test de un valor con espacios finales |
| `UsuId` cifrado | `UPPER()` en ambos lados | Test con `UsuId` que contiene `b` minúscula |
| `CPCPerNyA` / `CPCResNyA` **calculados** (no columnas) | JOIN a `PERSONAS` + orden por `PerNomyApe` | Test de orden (§10 R-4, R-LOG-035/036) |
| Sin escrituras | la BD queda sin cambios | `git diff` + verificación de la BD al terminar |

### 9.3 Seguridad

| Aspecto | Criterio de cierre |
|---|---|
| Rutas | `/home` y `/not-authorized` sin sesión ⇒ `302` a `/login`. |
| CSRF | `POST /login` sin token ⇒ `403`; con token ⇒ `200`/`302`. |
| CSRF en la API | `POST /api/v1/auth/login` **sin** token ⇒ `200`; y **todo `/api/**` queda sin CSRF** (D-LOG-17(a)) ⇒ **ningún** request de la API es rechazado por token ausente. |
| Cookie | `SUCURSAL` con `HttpOnly` y `SameSite=Lax`; **ausente** en el login fallido. `JSESSIONID` con `SameSite=Lax`. |
| Sesión | Fijación de sesión mitigada (`changeSessionId` en el login). |
| Clave en claro | La clave **nunca** aparece en el HTML renderizado ni en los logs. |
| Sin secretos | Ninguna credencial en el repositorio; `.env` no versionado. |
| Superficie REST | `/api/v1/**` sin autenticación → **deuda asumida y documentada** (SA-LOG-14, R-10, tarea T-135), sin regresión respecto del estado actual. |
| **Códigos de seguridad** (revisión v2, vigente) | `/api/**` **no** devuelve 403 por token CSRF ausente ni 401 por credenciales ausentes (es `permitAll`, D-LOG-19(a)) ⇒ responde el código de negocio. El **403** queda reservado a las rutas MVC sin token CSRF; el **401**, al endurecimiento futuro de la API (T-135). (R-LOG-039) |
| **CSRF explícito** (revisión v2, vigente) | La política CSRF está declarada **explícitamente por familia de rutas**: exigida en MVC, descartada en `/api/**` (R-LOG-041). |
| **Mitigación sin CSRF en la API** (revisión v2, vigente) | La API **no** toma autoridad de la cookie de sesión; `JSESSIONID` y `SUCURSAL` con `SameSite=Lax`; la credencial futura de la API viajará en `Authorization` (R-LOG-041, T-136). |
| **Materialización del token** (revisión v2, vigente) | Toda pantalla HTML servida sin sesión previa devuelve **200 + `<form>` + `_csrf`**, sin `IllegalStateException` ni trazas ERROR (R-LOG-042, R-LOG-043). |
| **Colección Postman** (revisión v2, vigente) | Declara su `auth` explícitamente (`noauth`), es ejecutable de punta a punta **sin token CSRF** y está dividida en un archivo por módulo, todos < 500 líneas (R-LOG-040, R-LOG-044, R-LOG-045). |
| **Dialecto** (revisión v2) | El arranque **no** emite `HHH000511`; la suite completa sigue verde sin cambios de comportamiento (R-LOG-046). |

---

## 10. Convenciones y riesgos

### 10.1 Convenciones

| # | Convención |
|---|---|
| 1 | Java 17, Spring Boot 3.x, Spring MVC + Thymeleaf, Spring Data JPA, `ddl-auto=none`. |
| 2 | `web → service → repository → domain`; prohibido `web → repository`; `domain` sin dependencias. |
| 3 | DTOs = `record` inmutables; form objects en `web/mvc/**` (no en `web/dto/**`). |
| 4 | Los **mensajes de error son verbatim**: no se traducen, no se capitalizan, no se corrigen. |
| 5 | Nada de secretos en el repositorio; credenciales sólo por variables de entorno. |
| 6 | JavaScript vanilla, puntual, sin framework/bundler/CDN; sólo `login.js`. |
| 7 | Archivos de configuración < 500 líneas. |
| 8 | Documentación y specs en español; requisitos en EARS; calidad ISO/IEC 25030. |
| 9 | La KB es de **solo lectura**: nunca compilar, guardar, regenerar ni borrar objetos. |

### 10.2 Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| **R-1** | El mecanismo de credenciales **no es cifrado**: es texto plano con transformación pública y reversible. | Alto (seguridad) | Reproducido **por paridad** (D-LOG-03); riesgo **asumido y documentado**. PBKDF2 queda detrás de `VerificadorDeClave` (D-LOG-09) para activarlo sin refactorizar (SA-LOG-03). |
| **R-2** | El padding a 10 caracteres puede diferir del runtime GeneXus. | Medio | SA-LOG-04; validado contra 549 de 552 filas de la BD real. |
| **R-3** | La semántica de `IndexOf` puede invertir el resultado del chequeo de IP. | Medio | SA-LOG-01 + D-LOG-10 (un solo método, con test dedicado). |
| **R-4** | El ordenamiento de `Seg_BusComi`/`Seg_BusPer` exige el **nombre y apellido**. *(Revisado en la revisión v2: la afirmación previa —«`CPCPerNyA` **sí** es columna de `COMISIO1`»— era **incorrecta**.)* | Medio | G-DATA 2026-10-22: `CPCPerNyA` tiene **0 coincidencias** en toda la base ⇒ es un atributo **calculado** ⇒ el orden se reproduce con **JOIN a `PERSONAS`** (`PerNomyApe`, que **sí** es columna). Ver R-LOG-035/036 y D-LOG-20; fórmula exacta en SA-LOG-15 (T-008). |
| **R-5** | El canal REST replica la inconsistencia del legacy (código 0 sin sucursal). | Bajo | Se replica a propósito (R-LOG-029) y queda documentada. |
| **R-6** | El estado de `IsAuthorized` no es verificable contra producción. | Bajo | SA-LOG-02 + D-LOG-13 (estructura filtrable). |
| **R-7** | El nombre físico real de las tablas de comisiones/turnos **no está confirmado** (y la BD no fue alcanzable al redactar este plan). | **Alto** | SA-LOG-08/SA-LOG-09 + **T-002 como puerta** de T-1xx. Si el nombre real difiere, las entidades se corrigen **antes** de escribir los repositorios. |
| **R-8** | `UsuClave` es `char(10)` y la clave se compara como `CHARACTER(10)`: una clave con espacios finales se codifica distinto según el runtime. | Bajo | La comparación usa `RTRIM` en ambos lados antes de igualar (mismo criterio que `Producto.ProdDesc`); test dedicado. |
| **R-9** | `docs/analisis-ATPPuestos.md` §15.1 documenta `COMISION` (acierta) pero omite `COMISIO1`, las columnas de fechas de la comisión y la PK real de `PPRIM_TU`. | Alto | **Resuelto** (SA-LOG-08, §5.3): el documento se corrige con el G-DATA re-ejecutado (**T-403**). |
| **R-10** | `/api/v1/**` (44+9 requests) queda **sin autenticación** después de instalar Spring Security. | Medio | **DEUDA ASUMIDA** por decisión aprobada (D-LOG-19(a), 2026-10-22): `permitAll` en este módulo, sin regresión respecto del estado actual (hoy tampoco hay seguridad). Seguimiento explícito: **SA-LOG-14** + tarea **T-135** (token propio de la app en la cabecera `Authorization`, sin credenciales versionadas ⇒ G3). |
| **R-11** | El login cambia el comportamiento global de Spring Security en el núcleo (filtro, CSRF, redirección). | Medio | Suite de `01`/`02` completa en verde (T-301) antes de cerrar; `permitAll` explícito para `/api/v1/**` salvo el login. |
| **R-12** | La cookie `SUCURSAL` es `HttpOnly` ⇒ el `login.js` no puede leerla (no lo necesita: el toggle de contraseña no la usa). | Bajo | Verificado al implementar `login.js`. |
| **R-13** | El cliente mobile existente podría enviar `{Usuario, Pass, Tipo}` (mayúsculas) ⇒ el contrato en minúsculas lo rechazaría. | Medio | `@JsonAlias` en el DTO (SA-LOG-12) + T-120. |
| **R-14** | Un `usuario` de más de 10 caracteres: el legacy lo trunca, el nuevo lo rechaza con `400`. | Bajo | Documentado en §4.1; validación `@Size(max=10)` (rechazar es más seguro que truncar). |
| **R-15** *(revisión v2)* | Mapear `COMISIO` en vez de `COMISION` (o al revés) rompe **todas** las consultas de comisión del login. | **Alto** | **Cerrado** (SA-LOG-08, §5.3, R-LOG-033): evidencia de `INFORMATION_SCHEMA` + `@Table(name="COMISION")` + IT que lee una fila real. |
| **R-16** *(revisión v2)* | `CPCPerNyA` / `CPCResNyA` **no son columnas**: mapearlas hace fallar Hibernate; omitirlas sin JOIN deja el nombre del responsable vacío y el orden no determinista. | Medio | D-LOG-20 (JOIN a `PERSONAS`) + R-LOG-035/036; fórmula exacta en SA-LOG-15 con tarea **T-008**. |
| **R-17** *(revisión v2)* | La política CSRF y de autenticación de `/api/**` **no está aprobada**: la colección Postman no es ejecutable (H3/H6) y el 401/403 es confuso. | **Alto** (G-POSTMAN) | **RESUELTO (2026-10-22)**: el responsable del proyecto aprobó **D-LOG-17(a)** y **D-LOG-19(a)** ⇒ `/api/**` sin CSRF y `permitAll`; la colección queda ejecutable sin tokens ni credenciales (§4.5, §4.7). |
| **R-18** *(revisión v2)* | El token CSRF se materializa tarde ⇒ las pantallas HTML no son servibles sin sesión previa (H4). | Medio | **RESUELTO (2026-10-22)**: aprobada **D-LOG-18(a)** ⇒ materialización anticipada conservando `HttpSessionCsrfTokenRepository`, sin tocar el contrato del formulario, la cookie `SUCURSAL` ni el HTML del módulo 02 (§4.6). Implementación **T-128**, prueba **T-311**. |
| **R-19** *(revisión v2)* | La colección Postman monolítica (1990 líneas) viola G4 y no sirve como prueba de aceptación. | Medio | R-LOG-044 (split por módulo, **T-406**) + R-LOG-040 (recorrido ejecutable, **T-313**). |
| **R-20** *(revisión v2)* | Hibernate detecta SQL Server 10.0 (`HHH000511`) ⇒ riesgo latente de compatibilidad con el driver/configuración actual. | Bajo | D-LOG-21 (dialecto explícito, **T-129**) + verificación **T-314**. |
| **R-21** *(revisión v2)* | `UsuIngreso` es nullable y hay **1** usuario de 552 con `NULL`: un *unboxing* a `int` lanzaría `NullPointerException` en el login. | Bajo | R-LOG-037 (`Integer` + comparación por igualdad) + IT con ese usuario real. |
| **R-22** *(revisión v2)* | Con D-LOG-17(a) **`/api/**` no está protegida contra CSRF**: un sitio de terceros podría disparar una escritura contra la API desde el navegador de un operador. | Medio | **Mitigación acordada y verificable**: la API es *stateless* y **no** toma autoridad de la cookie de sesión (no hay credencial ambiental que montar); `JSESSIONID` y `SUCURSAL` con `SameSite=Lax`; la credencial futura de la API viajará en la cabecera `Authorization`, nunca en cookie. Verificación: **T-136**. Deuda de `permitAll`: SA-LOG-14 + **T-135**. |
