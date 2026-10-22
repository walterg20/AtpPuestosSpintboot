# Plan — 03 Login (núcleo compartido)

> **Estado**: `PROPUESTA` — pendiente de aprobación en la puerta **G-SPEC**.
> Acompaña a `spec.md` (misma puerta). Documento de arquitectura; no contiene tareas (ver `tasks.md`).
> Contrato de datos: `openspec/features/00-alcance/plan.md` §3 y `docs/analisis-ATPPuestos.md` §13/§15.
> Paridad de BD: `docs/analisis-ATPPuestos.md` §15 (G-DATA 2026-10-22) — **verificar de nuevo** (§5.4, T-002).

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
| **D-LOG-11** | **CSRF se desactiva únicamente** en `POST /api/v1/auth/login` (cliente stateless sin cookies); el resto de `/api/**` y todas las rutas MVC **mantienen CSRF**. | El WS legacy no tiene sesión ni cookies ⇒ el token CSRF no tiene sentido y rompería al cliente mobile. | (a) CSRF global → el endpoint WS quedaría inutilizable; (b) CSRF global desactivado → debilita los formularios de escritura. | El alcance del descarte es **una** ruta, declarada de forma explícita y testeada. |
| **D-LOG-12** | El resto de `/api/v1/**` (los 44 requests de `01-nucleo-datos`) queda **`permitAll`** en este módulo. | Hoy esa API **no tiene ninguna seguridad** (no hay `spring-boot-starter-security` en el núcleo). Endurecerla en este módulo rompería la colección Postman y la suite de `01`/`02` (G-POSTMAN + no-regresión). | (a) `authenticated()` para todo `/api/v1/**` → 44 requests → `401`; (b) Basic auth sobre toda la API → invención de contrato. | **No hay regresión de seguridad** respecto del estado actual. El endurecimiento queda como tarea posterior y explícita (§10, R-11, SA-LOG-14). |
| **D-LOG-13** | La autorización por programa se replica tal cual: `IsAuthorized` **devuelve siempre `True`** ⇒ la lista completa. La estructura queda **filtrable**. | `IsAuthorized` tiene su lógica comentada; `ListPrograms` no tiene callers (SA-LOG-02). | (a) Inventar una regla por grupo/nivel → comportamiento no verificable en el legacy. | `ServicioAutorizacion.programasAutorizados()` devuelve siempre los 2 programas, pero el filtrado es un `filter` de una línea. |
| **D-LOG-14** | Las entidades JPA del login mapean **sólo las columnas que el login lee**. El módulo **no escribe** en ninguna de estas tablas ⇒ no hay columnas huérfanas que requieran default de aplicación. | La regla de columnas huérfanas (R-036 de `01-nucleo-datos`) aplica a **escrituras**. | (a) Mapear las 5 columnas huérfanas de `SCRTY` (`UsuLegajo`, `UsuFoto` `varbinary(max)`, `UsuIdComarb`, `UsuAppFchAdhesion`, `UsuHash`) → sin destino y riesgo de mapeo erróneo. | `UsuHash` (PBKDF2) queda **fuera** (SA-LOG-03). Si un módulo futuro escribe en `SCRTY`, deberá mapearlas con sus defaults. |
| **D-LOG-15** | El **controller MVC** es el que escribe la cookie `SUCURSAL` y el que registra la autenticación en la sesión; el **servicio** no conoce cookies ni `HttpServletRequest`. | El legacy escribe la cookie desde el evento `Enter` del formulario, no desde el procedimiento. | (a) Cookie en el servicio → acopla `service` al contenedor web. | `service` no importa nada de `jakarta.servlet`; la IP del cliente también se resuelve en el adaptador y se pasa al servicio. |
| **D-LOG-16** | El login **no** usa `AuthenticationManager`: el controlador invoca `ServicioAcceso` para conservar el **código 0-6** y el mensaje verbatim, y registra la autenticación en la sesión vía `SecurityContextRepository`. | `AuthenticationProvider` lanzaría `BadCredentialsException` y **perdería el código**, que es el dato de negocio que hay que mostrar. | (a) `AuthenticationManager` con excepción portadora del código → el mensaje verbatim quedaría en el adapter y habría que mapear 2 veces. | Spring Security aporta **protección de rutas + CSRF + sesión**; la **decisión de acceso** es de negocio y vive en el servicio. |

---

## 3. Arquitectura objetivo

### 3.1 Capas y paquetes

```text
ar.com.atp.puestos.nucleo
├── domain/                       (sólo lectura; sin dependencias)
│   ├── Usuario.java              @Table(name="SCRTY")
│   ├── Persona.java              @Table(name="PERSONAS")
│   ├── Puesto.java               @Table(name="PUEPPRIM")
│   ├── Receptor.java             @Table(name="RECEPTOR")
│   ├── ReceptorPersona.java      @Table(name="RECEPTO1")
│   ├── Comision.java             @Table(name="COMISIO")      ⚠ SA-LOG-08
│   ├── ComisionIntegrante.java   @Table(name="COMISIO1")     ⚠ SA-LOG-08
│   └── TurnoPPrim.java           @Table(name="PPRIM_TU")
├── repository/                   (Spring Data JPA; sólo SELECT)
│   ├── UsuarioRepository.java
│   ├── PersonaRepository.java
│   ├── PuestoRepository.java
│   ├── ReceptorRepository.java
│   ├── ComisionRepository.java
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
   ├─ SecurityFilterChain: permitAll + CSRF ignorado en ESTA ruta (D-LOG-11)
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
| CSRF | Habilitado por defecto | `HttpSessionCsrfTokenRepository`; el `POST /login` lo exige (R-LOG-031). Descartado **sólo** en `POST /api/v1/auth/login` (D-LOG-11). |
| Rutas permitidas | `/login`, `/api/v1/auth/login`, `/error`, `/css/**`, `/js/**`, `/images/**` | Todo lo demás: `authenticated()`. |
| Respuesta a no autenticado | Redirección `302` a `/login` | `LoginUrlAuthenticationEntryPoint` (R-LOG-024). |
| Logout | `POST /logout` con CSRF | Invalida la sesión y borca la cookie `SUCURSAL`. |
| Cookies | `SUCURSAL` (tipo) | `HttpOnly`, `SameSite=Lax`, `path=/`, **sin `Max-Age`** (de sesión) — D-LOG-07. |
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
| `tipo` | `String` | `select` | `tipo` | `@NotBlank`, `@Pattern("PUE|REC")` | Opciones `PUE` (Puesto Caminero) y `REC` (Receptoría); precargado desde la cookie `SUCURSAL` |

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

---

## 5. Paridad de BD

> **No se modifica el esquema.** Estructura existente reutilizada (`ddl-auto=none`).
> **Sin DDL, sin migraciones, sin `schema.sql`/`data.sql`, sin Flyway/Liquibase** (G5).
> Fuente de autoridad: `INFORMATION_SCHEMA` de `Rentas` (G-DATA 2026-10-22, `docs/analisis-ATPPuestos.md` §15)
> contrastada con las estructuras lógicas de la KB (`genexus_db sql_ddl`).
> **Este módulo no escribe en ninguna de estas tablas** ⇒ no hay defaults de aplicación que declarar.

### 5.1 `SCRTY` (PK `UsuId`)

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación | Estado |
|---|---|---|---|---|---|---|
| `UsuId` | `UsuId` | `char(10)` | NO | `String` | `@Id @Column(name="UsuId", nullable=false, length=10)` | G-DATA ✓ |
| `UsuClave` | `UsuClave` | `char(10)` | NO | `String` | `@Column(name="UsuClave", nullable=false, length=10)` + `@ColumnTransformer(read="RTRIM(UsuClave)")` | G-DATA ✓ |
| `UsuNomYApe` | `UsuNomYApe` | `char(35)` | NO | `String` | `@Column(name="UsuNomYApe", nullable=false, length=35)` + `RTRIM` | G-DATA ✓ |
| `UsuIngreso` | `UsuIngreso` | `smallint` | sí | `Integer` | `@Column(name="UsuIngreso")` | G-DATA ✓ (KB: el único nullable) |
| `UsuNiv`, `UsuGrupo`, `UsuTipDoc`, `UsuNroDoc`, `UsuNotiAre`, `UsuArea`, `UsuEMail` | — | — | — | — | **No mapeadas** | El login no las lee (D-LOG-14) |
| `UsuLegajo`, `UsuFoto`, `UsuIdComarb`, `UsuAppFchAdhesion`, `UsuHash` | — | — | — | — | **No mapeadas** (huérfanas) | El módulo no escribe ⇒ sin default; `UsuHash` fuera de alcance (SA-LOG-03) |

### 5.2 `PERSONAS` (PK `PerId`)

| Atributo | Columna física | Tipo SQL | Nula | Tipo Java | Anotación | Estado |
|---|---|---|---|---|---|---|
| `PerId` | `PerId` | `int` | NO | `Integer` | `@Id @Column(name="PerId", nullable=false)` | G-DATA ✓ |
| `PerNomyApe` | `PerNomyApe` | `char(40)` | NO | `String` | `@Column(name="PerNomyApe", nullable=false, length=40)` + `RTRIM` | G-DATA ✓ |
| `PerCodUsu` | `PerCodUsu` | `char(10)` | ? | `String` | `@Column(name="PerCodUsu", length=10)` | Nombre ✓; **nulabilidad a confirmar** (T-002) |
| `AreaId` | — | — | — | — | **No mapeada** | El login no la lee |

### 5.3 Las 6 tablas de contexto operativo

| Tabla física | Entidad | Columnas que mapea el login | Estado |
|---|---|---|---|
| `PUEPPRIM` | `Puesto` | `PueCod` (`smallint`, PK), `PueDesc` (`char(20)`), `PueIPRed` (`varchar(40)`) | G-DATA ✓ |
| `RECEPTOR` | `Receptor` | `ReceId` (`smallint`, PK), `ReceDsc` (`char(30)`) | G-DATA ✓ |
| `RECEPTO1` | `ReceptorPersona` | `ReceId` + `PerId` (PK compuesta) | **Nombre y columnas a confirmar** (T-002) |
| `COMISIO` ⚠ | `Comision` | `PueCod`, `CPCId`, `CPCFchDes`, `CPCFchHas`, `CPCHorHas`, `CPCCierre`, `CPCResId`, `CPCResNyA` | ⚠ **SA-LOG-08** |
| `COMISIO1` ⚠ | `ComisionIntegrante` | `PueCod`, `CPCId`, `CPCPerId`, `CPCPerNyA` | ⚠ **SA-LOG-08** |
| `PPRIM_TU` | `TurnoPPrim` | `SucNroId`, `SucTipo`, `TurNroId`, `TurEstado`, `TurCPCId` | **PK a confirmar** (T-002) |

> **Divergencia KB ↔ BD detectada (hallazgo nuevo, ver §10 R-9)**: `docs/analisis-ATPPuestos.md` §15.1
> documenta la tabla raíz de comisiones como **`COMISION`** (9 caracteres), mientras que la
> regla de truncación a 8 caracteres de GeneXus aplicada al objeto `Comision` de la KB da
> **`COMISIO`**; y la tabla de integrantes (`Comisio1` → `COMISIO1`) **no figura** en §15.1.
> Además §15.1 no lista `CPCFchDes`/`CPCFchHas`/`CPCHorHas`, que los procedimientos **sí usan**.
> ⇒ **T-002 es una puerta** para escribir las entidades de comisiones y turnos.

### 5.4 Estado de la verificación G-DATA

| Comprobación | Resultado |
|---|---|
| `INFORMATION_SCHEMA.COLUMNS` de las 8 tablas | **Pendiente de re-ejecución** (T-002): en la sesión de redacción de este plan la BD `devmssqlatp.ecom.com.ar` **no fue resoluble** (DNS: «el nombre DNS no existe») ⇒ no se pudo re-confirmar en vivo. |
| Evidencia disponible | `docs/analisis-ATPPuestos.md` §15 (G-DATA 2026-10-22) + `spec.md` §3.2.1 (552 filas de `SCRTY`). |
| Estructuras lógicas de la KB | Verificadas en esta sesión con `genexus_db sql_ddl` (sólo lectura): `SCRTY`, `Personas`, `PuePPrim`, `RECEPTOR` (+`RECEPTO1`), `Comisio1`, `PPRIM_TU`. |
| Contraste KB ↔ BD | **Pendiente** (SA-LOG-08 / SA-LOG-09). |

### 5.5 Reglas de BD aplicables

| Regla | Detalle |
|---|---|
| `ddl-auto` | `none` — **no se toca** `application.yml` en esa sección (T-308). |
| Lectura | Sólo `SELECT`. El módulo **no** inserta, actualiza ni borra. |
| `char(n)` fijo | Se lee con `RTRIM` (`@ColumnTransformer(read="RTRIM(…)")`), como en `Producto.ProdDesc`. |
| Nombres físicos | Exactos; **no** se renombra nada. Los truncados (`UsuNotiAre`, `PerNomyApe`) se respetan tal cual. |
| Columnas huérfanas | No se mapean porque no se leen ni se escriben (D-LOG-14). Si un módulo futuro escribe, debe mapearlas con su default (regla R-036 de `01-nucleo-datos`). |
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
| **SA-LOG-08** *(nuevo)* | Nombre físico y esquema de las tablas de comisiones y turnos | «¿Cuál es el nombre físico exacto de la tabla raíz de comisiones (`COMISIO` por truncación de `Comision` o `COMISION` según `docs/analisis-ATPPuestos.md` §15.1)? ¿Existe `COMISIO1`? ¿Existen `CPCFchDes`/`CPCFchHas`/`CPCHorHas`? ¿Cuál es la PK real de `PPRIM_TU`: `(SucNroId, SucTipo, TurNroId)` (KB) o `(SucNroId, TurNroId)` (§15.1)?» | `INFORMATION_SCHEMA` de `Rentas` | DBA / infraestructura | **Bloqueante para T-1xx** (no se puede escribir la entidad sin el nombre real) |
| **SA-LOG-09** *(nuevo)* | La BD no fue alcanzable en la sesión de redacción | «¿Se puede re-ejecutar el G-DATA contra `devmssqlatp.ecom.com.ar` desde el entorno de build/CI? En la sesión de este plan el host no resuelve por DNS.» | Conectividad | Infraestructura | Toda la §5 depende de él |
| **SA-LOG-10** *(nuevo)* | Enlace de los programas del menú | «`ListPrograms` usa `WWPPrim_Turnos.Link()` y `WWTDetProdPri.Link()`, que apuntan a WebPanels legacy sin equivalente en el sistema nuevo. ¿Qué ruta MVC debe mostrar el menú de `Home`, o el menú se muestra sin enlace hasta que existan esos módulos?» | Ruta destino | Legacy / arquitectura | Enlace del menú de `Home` |
| **SA-LOG-11** *(nuevo)* | Uso de `UsuGrupo` / `UsuNiv` para autorizar | «En el legacy `IsAuthorized` siempre devuelve `True` y no mira el grupo ni el nivel (hay 23 grupos y 10 niveles en `SCRTY`). ¿El sistema nuevo debe filtrar por grupo/nivel, o se replica el legacy?» | Regla de autorización | Negocio / seguridad | Contenido del menú |
| **SA-LOG-12** *(nuevo)* | Nombres de campo del canal WS | «El SDT legacy serializa `{Usuario, Pass, Tipo}` (con mayúscula). ¿El cliente mobile existente envía esos nombres o los del contrato nuevo en minúsculas `{usuario, pass, tipo}`? Si son los legacy, el DTO debe llevar `@JsonAlias` (o sólo los legacy).» | Código del cliente mobile | Legacy / WS | Compatibilidad del contrato REST |
| **SA-LOG-13** *(nuevo)* | Valor por defecto del selector `Acceso` | «En el legacy `&Tipo` arranca vacío y un tipo vacío cae en la rama de Receptoría. ¿El formulario nuevo debe exigir un valor (valor por defecto `PUE`) o replicar el vacío?» | Decisión de UX/negocio | Producto / legacy | 1 rama del algoritmo |
| **SA-LOG-14** *(nuevo)* | Endurecimiento del API de datos | «¿Cuándo se exige autenticación a los endpoints `/api/v1/**` que no son de login (los 44 requests de `01-nucleo-datos`)? Endurecerlos ahora rompe la colección Postman y la suite vigente.» | Plan de securing | Arquitectura / seguridad | Superficie expuesta |

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
| R-LOG-028 | D-LOG-06, D-LOG-11 | T-121, T-125 |
| R-LOG-029 | D-LOG-06 | T-121, T-304 |
| R-LOG-030 | D-LOG-01 | T-401 |
| R-LOG-031 | D-LOG-07, D-LOG-11 | T-124, T-125, T-301 |
| R-LOG-032 | D-LOG-07 | T-130, T-305 |

---

## 8. Impacto en G-POSTMAN y en las pruebas de vistas

### 8.1 Colección Postman — `docs/postman/ATPPuestos-API.postman_collection.json`

| Acción | Detalle |
|---|---|
| **Agregar** | Carpeta **`Auth`** con **1 request**: `POST {{baseUrl}}/api/v1/auth/login`, body `{ "usuario": …, "pass": …, "tipo": "PUE" }`, test que verifica **status 200** y la presencia de las 3 claves `error`, `sucursal`, `comision`. |
| **Agregar** (opcional) | Un segundo request "login con credenciales inválidas" dentro de la misma carpeta, con un test que verifica `error = 2` — es el caso de negocio más importante del canal WS. |
| **Modificar** | **Ninguno** de los 44 requests existentes: este módulo **no** cambia ningún endpoint preexistente (D-LOG-12 deja `/api/v1/**` igual que está). |
| **Eliminar** | Ninguno. |
| **Documentación** | `docs/postman/README.md`: actualizar §"Cobertura actual" (de 9 carpetas / 44 requests a **10 carpetas / 45-46 requests**) y agregar una fila al *Changelog*. |

> **Regla G-POSTMAN**: el cambio de la colección y el del backend van en el **mismo commit** (T-401).
> Si T-120 cambia la forma del body, la colección se actualiza en ese mismo commit.

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

### 9.2 Base de datos

| Legacy | Nuevo | Prueba |
|---|---|---|
| `SCRTY` (11 columnas KB + 5 huérfanas) | `Usuario` (4 columnas leídas) | `UsuarioRepositoryIT` |
| `PERSONAS` | `Persona` (3 columnas) | `PersonaRepositoryIT` |
| `PUEPPRIM` | `Puesto` (3 columnas) | `PuestoRepositoryIT` |
| `RECEPTOR` + `RECEPTO1` | `Receptor`, `ReceptorPersona` | `ReceptorRepositoryIT` |
| comisiones | `Comision`, `ComisionIntegrante` | `ComisionRepositoryIT` — **bloqueado por SA-LOG-08** |
| `PPRIM_TU` | `TurnoPPrim` | `TurnoRepositoryIT` — **bloqueado por SA-LOG-08** |
| `char(n)` fijo | `RTRIM` al leer | Test de un valor con espacios finales |
| `UsuId` cifrado | `UPPER()` en ambos lados | Test con `UsuId` que contiene `b` minúscula |
| `CPCPerNyA` / `PerNomyApe` como columnas | ordenamiento por el **nombre almacenado** | Test de orden (§10 R-9) |
| Sin escrituras | la BD queda sin cambios | `git diff` + verificación de la BD al terminar |

### 9.3 Seguridad

| Aspecto | Criterio de cierre |
|---|---|
| Rutas | `/home` y `/not-authorized` sin sesión ⇒ `302` a `/login`. |
| CSRF | `POST /login` sin token ⇒ `403`; con token ⇒ `200`/`302`. |
| CSRF en WS | `POST /api/v1/auth/login` **sin** token ⇒ `200` (descartado sólo en esa ruta). |
| Cookie | `SUCURSAL` con `HttpOnly` y `SameSite=Lax`; **ausente** en el login fallido. |
| Sesión | Fijación de sesión mitigada (`changeSessionId` en el login). |
| Clave en claro | La clave **nunca** aparece en el HTML renderizado ni en los logs. |
| Sin secretos | Ninguna credencial en el repositorio; `.env` no versionado. |
| Superficie REST | `/api/v1/**` sin autenticación → **deuda documentada** (SA-LOG-14), sin regresión respecto del estado actual. |

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
| **R-4** | El ordenamiento de `Seg_BusComi`/`Seg_BusPer` exige el **nombre y apellido**. | Medio | Verificado en esta sesión: `CPCPerNyA` **sí** es columna de `COMISIO1` y `PerNomyApe` de `PERSONAS` (KB DDL) ⇒ **no** hace falta el `LEFT JOIN` a `PERSONAS` que anticipaba `spec.md` §7 R-4. El ordenamiento debe usar el nombre **almacenado** (`CPCPerNyA`), no el actual de la persona. Confirmar el nombre físico (SA-LOG-08). |
| **R-5** | El canal REST replica la inconsistencia del legacy (código 0 sin sucursal). | Bajo | Se replica a propósito (R-LOG-029) y queda documentada. |
| **R-6** | El estado de `IsAuthorized` no es verificable contra producción. | Bajo | SA-LOG-02 + D-LOG-13 (estructura filtrable). |
| **R-7** | El nombre físico real de las tablas de comisiones/turnos **no está confirmado** (y la BD no fue alcanzable al redactar este plan). | **Alto** | SA-LOG-08/SA-LOG-09 + **T-002 como puerta** de T-1xx. Si el nombre real difiere, las entidades se corrigen **antes** de escribir los repositorios. |
| **R-8** | `UsuClave` es `char(10)` y la clave se compara como `CHARACTER(10)`: una clave con espacios finales se codifica distinto según el runtime. | Bajo | La comparación usa `RTRIM` en ambos lados antes de igualar (mismo criterio que `Producto.ProdDesc`); test dedicado. |
| **R-9** | `docs/analisis-ATPPuestos.md` §15.1 documenta `COMISION` (9 caracteres) y omite `COMISIO1` y las columnas de fechas de la comisión. | Alto | SA-LOG-08 + T-002: el documento se corrige con el resultado del G-DATA re-ejecutado. |
| **R-10** | `/api/v1/**` (44 requests) queda **sin autenticación** después de instalar Spring Security. | Medio | D-LOG-12: no hay regresión (hoy no hay seguridad). SA-LOG-14 + tarea de seguimiento. |
| **R-11** | El login cambia el comportamiento global de Spring Security en el núcleo (filtro, CSRF, redirección). | Medio | Suite de `01`/`02` completa en verde (T-301) antes de cerrar; `permitAll` explícito para `/api/v1/**` salvo el login. |
| **R-12** | La cookie `SUCURSAL` es `HttpOnly` ⇒ el `login.js` no puede leerla (no lo necesita: el toggle de contraseña no la usa). | Bajo | Verificado al implementar `login.js`. |
| **R-13** | El cliente mobile existente podría enviar `{Usuario, Pass, Tipo}` (mayúsculas) ⇒ el contrato en minúsculas lo rechazaría. | Medio | `@JsonAlias` en el DTO (SA-LOG-12) + T-120. |
| **R-14** | Un `usuario` de más de 10 caracteres: el legacy lo trunca, el nuevo lo rechaza con `400`. | Bajo | Documentado en §4.1; validación `@Size(max=10)` (rechazar es más seguro que truncar). |
