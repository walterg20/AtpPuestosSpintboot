# Plan — 00 Alcance migrable

> **Estado**: `APPROVED` — puerta **G-SPEC**.
> Documento de arquitectura. No contiene tareas (ver `tasks.md`).

---

## 1. Estructura del monorepo

```text
AtpPuestosSpintboot/
├── pom.xml                       parent POM (packaging=pom) + Maven Wrapper
├── atp-nucleo-compartido/        biblioteca: catálogo + personas + base de seguridad + API REST
├── atp-puestos-camineros/        ARTEFACTO DESPLEGABLE: login + dominio de Puestos + vistas
├── openspec/                     specs (EARS)
├── docs/                         análisis, diseño, Postman
└── .opencode/                    arnés
```

### Por qué dos módulos y no uno

La separación no es especulativa: la dependencia **Puestos → Catálogos** ya existe hoy (el
formulario de un puesto necesita Localidades y Provincias; el ABM de productos necesita el
catálogo de productos) y **persiste** hacia los portales futuros. Extraer el núcleo ahora cuesta
un movimiento mecánico de paquetes; hacerlo después, con el núcleo ya mezclado con el dominio de
puestos, costaría un refactor mucho más invasivo.

Además, el código heredado del proyecto anterior (`atppuestos2`) ya estaba neatly separado por
capas en `ar.com.atp.puestos.nucleo.**` y `ar.com.atp.puestos.shared.**`: el split del monorepo
**no requirió cambiar ni un import**.

### Artifactos

| Artefacto | Tipo | Contenido |
|---|---|---|
| `atp-nucleo-compartido` | jar (library) | `nucleo.**` + `shared.**` |
| `atp-puestos-camineros` | jar (boot) | `puestos.**` + `PuestosApplication` + el núcleo (dependencia) |

Un solo artefacto ejecutable: `atp-puestos-camineros-0.1.0-SNAPSHOT.jar` (fat jar con el núcleo
adentro). ⇒ **monolito modular**, 1 despliegue.

## 2. Capas y regla de dependencia

```text
web (rest | mvc)  →  service  →  repository  →  domain
        ↑                ↑
   shared (transversal)  ← no depende de nadie
```

| Capa | Paquete | Responsabilidad | No debe contener |
|---|---|---|---|
| `domain` | `nucleo.domain` | Entidades JPA + validadores del dominio | Lógica de negocio, conocimiento de la BD |
| `repository` | `nucleo.repository` | Acceso a datos (Spring Data) | Reglas de negocio |
| `service` | `nucleo.service` | Casos de uso, transacciones, mapeo | Conocimiento de HTTP o de Thymeleaf |
| `web.rest` | `nucleo.web.rest` | Contrato JSON `/api/v1` | Reglas de negocio |
| `web.mvc` | `nucleo.web.mvc` | Páginas Thymeleaf | Reglas de negocio |
| `dto` | `nucleo.web.dto` | Contrato de datos de la API | Comportamiento |
| `shared` | `shared.error`, `shared.web` | Errores uniformes, health | Reglas de un módulo concreto |

**Prohibido**: `web → repository`; `domain → web`; lógica de negocio en un controller; DTOs de
formulario dentro de `dto/**` (van en `web/mvc/**`).

## 3. Paridad de BD (G-DATA)

Base: `Rentas` @ `devmssqlatp.ecom.com.ar` (SQL Server). Verificado con
`INFORMATION_SCHEMA.COLUMNS` / `KEY_COLUMN_USAGE`.

| Transacción (KB) | Tabla física | PK | Nota |
|---|---|---|---|
| Provincia | `PROVINCI` | `ProvCod` | truncado a 8 |
| Localidades | `Localida` | `LocCod` | truncado a 8 |
| Calles | `CALLES` | `CallCod` | |
| CAMINOS | `CAMINOS` | `CamID` | |
| Areas | `Areas` | `AreaId` | `decimal(10,0)` ⇒ `BigDecimal` |
| Jurisdic | `Jurisdic` | `JurCod` | |
| Feriados | `FERIADOS` | `FerFecha` | `FerDescripcion → FerDescrip` (10) |
| Productos (0) | `PRODUCTO` | `ProdCod` | `smallint` |
| Productos (1) | `PRODUCT1` | (`ProdCod`,`ProdFchVig`) | `datetime`, no `date` |
| Personas | `PERSONAS` | `PerId` | `PerCodUsu` ← usuario (cifrado) |

**Módulos de seguridad** (relevados para `03-login`):

| Legacy | Tabla | PK | Columnas relevantes |
|---|---|---|---|
| Usuarios | `SCRTY` | `UsuId` `char(10)` | `UsuClave` `char(10)`, `UsuNomYApe`, `UsuGrupo`, `UsuNiv`, `UsuIngreso`, `UsuLegajo`, `UsuFoto`, `UsuIdComarb`, `UsuAppFchAdhesion`, `UsuHash` |
| Puestos | `PUEPPRIM` | `PueCod` | `PueDesc` `char(20)`, `PueIPRed` `varchar(40)` |
| Receptoría | `RECEPTOR` | `ReceId` | `ReceDsc` `char(30)` |
| Comisión | `COMISION` | (`PueCod`,`CPCId`) | `CPCCierre`, `CPCResId` |
| Turnos PPrim | `PPRIM_TU` | (`SucNroId`,`TurNroId`) | |

**Columnas huérfanas** (en la BD, no en la KB) — se mapean, no se rediseñan: `Areas.AreaBaja`,
`Areas.AreaEmails`, `Areas.AreaAdjun`, `Areas.AreaRespons`, `CALLES.CallAlt1..3`,
`Localida.LocCodAfip`, `PROVINCI.ProvCodAfip`, `PRODUCTO.ProdReppa`,
`PRODUCT1.ProdImpu5/ProdFeLog/ProdUsu`, y las 5 de `SCRTY` listadas arriba.

**Sin `IDENTITY` y sin `COLUMN_DEFAULT`** en ninguna tabla ⇒ la PK viaja en el body del `POST` y
el duplicado responde `409`.

## 4. Contrato de API REST

Se **conserva** el contrato existente (44 requests, Postman sincronizado):

| Recurso | Endpoints |
|---|---|
| Health | `GET /api/v1/health` |
| Provincias, Localidades, Calles, Caminos, Areas, Jurisdicciones, Feriados | listar / obtener / crear / actualizar / eliminar (+ variantes por recurso) |
| Productos | `GET /api/v1/productos?anulados=`, `GET /api/v1/productos/{prodCod}`, `GET /api/v1/productos/{prodCod}/importes-vigentes`, `POST|PUT|DELETE` |

Códigos: `POST` → `201` + `Location`; `PUT` → `200`; `DELETE` → `204`; inexistente → `404`;
duplicado → `409`; validación → `400`. Cuerpo de error uniforme `{code, message}`.

**Regla de canal**: `/api/**` ⇒ JSON; cualquier otra ruta ⇒ HTML Thymeleaf. La implementa el
`GlobalExceptionHandler` (advice global que negocia según el prefijo de ruta).

> **Por qué el advice es global y no acotado por paquete**: las excepciones de ruta inexistente
> (404) y método no soportado (405) las lanza el `DispatcherServlet`, no un controlador. Un advice
> acotado a `web.rest` no se activaría para ellas y las respuestas 404/405 perderían el cuerpo
> uniforme que fija R-209. Verificado empíricamente: con el advice acotado, 4 tests de
> `GlobalExceptionHandlerTest` fallan.

## 5. Contrato de vistas (Thymeleaf)

| Prefijo de plantilla | Contenido | Módulo |
|---|---|---|
| `templates/layout/` | Layout base y fragmentos reutilizables | núcleo |
| `templates/nucleo/` | Pantallas de los catálogos | núcleo |
| `templates/puestos/` | Login, home, not-authorized | Puestos Camineros |
| `templates/error.html` | Página de error por defecto (JSON→HTML) | núcleo |
| `static/css/` | Tokens CSS + estilos | núcleo |
| `static/js/` | JS puntual | por módulo |

Las plantillas de cada módulo van en su **subcarpeta**: el resolver de Thymeleaf toma el primer
`classpath:/templates/<nombre>` del classpath, así que dos módulos con el mismo nombre de
plantilla colisionarían silenciosamente.

**Reglas**: `th:object` + `th:field` con form objects de `web/mvc/**`; formularios de escritura
con `th:action` (⇒ CSRF); errores de validación junto al campo; sin literales de color o
tipografía fuera del archivo de tokens; JS vanilla y puntual.

## 6. Configuración

`application.yml` vive en el **núcleo** (fuente única). Spring Boot toma el primer
`classpath:/application.yml` y **no** fusiona varios; si el módulo desplegable trajera el suyo,
uno de los dos sería ignorado en silencio y la configuración efectiva dependería del orden de
classpath.

| Perfil | Datasource | Cuándo |
|---|---|---|
| (base) | SQL Server real por variables de entorno | runtime |
| `test` | H2 en memoria, `ddl-auto=none` | tests (archivo en `src/test/resources` de cada módulo) |
| `dev` | SQL Server real | tests de integración gated por `ATP_DB_IT=true` |

## 7. Decisiones

| # | Decisión | Justificación |
|---|---|---|
| D-01 | Un solo `application.yml`, en el núcleo | Evita la colisión silenciosa por orden de classpath |
| D-02 | Advice de errores **global** con negociación por prefijo de ruta | Las 404/405 las lanza el DispatcherServlet, fuera de todo controller |
| D-03 | Plantillas en subcarpeta por módulo | Evita la colisión de nombres en el resolver de Thymeleaf |
| D-04 | `nucleo.web.controller` renombrado a `nucleo.web.rest` | Hace la frontera de escritura entre los dos implementadores inequívoca |
| D-05 | DTOs de formulario en `web/mvc/**`, no en `dto/**` | Evita colisión con el contrato de la API REST |
| D-06 | Núcleo y módulo de puestos con **el mismo prefijo de paquete** (`ar.com.atp.puestos`) | Split de paquete entre jars es legal en classpath plano; evita renombrar 90 archivos |
| D-07 | Sin `thymeleaf-layout-dialect` | Los fragmentos de Thymeleaf (`th:replace`) alcanzan; una dependencia menos |
| D-08 | La API REST se conserva además de la UI Thymeleaf | La KB legacy tiene consumidores WS/mobile y el contrato ya está verificado |
