# Tasks — 01 Núcleo de Datos (piloto)

> **PORTADO (2026-10-22)** desde el repositorio tppuestos2 (commit 974c3d6). Las 80 tareas
> marcadas [x] están **ejecutadas**: su código vive hoy en tp-nucleo-compartido/ y está
> cubierto por la suite verde (105 tests). Las 5 pendientes [ ] siguen pendientes.
> Las mini-specs de UI **Angular** (ui-abm-maestras.md, ui-abm-localidades.md,
> ui-abm-feriados.md) **no** se portaron: su detalle de implementación es específico de Angular
> y queda obsoleto. Su contenido de comportamiento (rótulos, validaciones, flujos) sigue siendo
> válido como fuente y se consulta en el repositorio tppuestos2 cuando se especifique la capa
> Thymeleaf de los catálogos.

> Checklist verificable del módulo `01-nucleo-datos`. Cada tarea es atómica y trazable a un
> requisito de `spec.md`. **Ninguna tarea de código (T-1xx+) se inicia sin aprobar `spec.md`
> (puerta G-SPEC)** — guardarraíl G2.
> Estado: `APPROVED` (2026-10-16) — P-01 = **ABM completo**, P-03 = **productos anidado**.

## A. Extracción y especificación (F2 — análisis)

- [x] A.1 Confirmar KB `ATPPuestos` conectada y ambiente `Gen18`. *(2026-10-15)*
- [x] A.2 Extraer las 8 transacciones maestras (`Structure`, `Rules`, `Events`). *(sin reglas/eventos)*
- [x] A.3 Extraer `Bus_DesLoc` y `BuscarCamino` (`Rules` + `Source` + variables).
- [x] A.4 Obtener el **nombre físico de tabla** de cada transacción (`get_indexes` → `result.name`).
- [x] A.5 Obtener índices/PK/FK de las 8 tablas.
- [x] A.6 Verificar consumidores (`impact`): `Bus_DesLoc`←`F2276`, `BuscarCamino`←`CodigoBarra`.
- [x] A.7 Redactar `spec.md` en EARS (R-001…R-006, R-010/R-011, R-020…R-023, **R-030…R-037**, R-201…R-205). *(2026-10-16)*
- [x] A.8 Redactar `plan.md` (mapeo JPA + contratos lectura/escritura + matriz de paridad). *(2026-10-16)*
- [x] A.9 Redactar `tasks.md`. *(2026-10-16)*
- [x] A.10 Revisar los tres archivos contra la extracción de la KB y resolver P-01…P-03. *(2026-10-16)*

## B. Aprobación (puerta G-SPEC)

- [x] B.1 Resolver los pendientes P-01…P-03 (`spec.md` §7). *(2026-10-16 — P-01 ABM completo; P-02 credenciales en `.env`; P-03 anidado)*
- [x] B.2 Marcar `spec.md`/`plan.md`/`tasks.md` como `APPROVED` y registrar la fecha. *(2026-10-16)*
- [x] B.3 Registrar la decisión en `.engram/` (`mem_save`, tipo `decision`). *(2026-10-16)*

## C. Validación de esquema (puerta G-DATA) — *completado 2026-10-16*

- [x] C.1 Confirmar credenciales de `Rentas` en `.env` (`DB_USER`/`DB_PASSWORD`). *(conexión OK vía `sqlcmd`)*
- [x] C.2 **ASM-01**: validar el nombre físico real de cada columna (`INFORMATION_SCHEMA.COLUMNS`). *(→ nombre lógico truncado a 10 chars; `FerDescripcion→FerDescrip`)*
- [x] C.3 **ASM-02/03/05**: validar tipos reales. *(→ `Area.AreaId decimal(10,0)`; `FerFecha`/`ProdFchVig` `datetime`; `FerFchReg datetime`)*
- [x] C.4 **ASM-04**: validar `varchar` vs. `char` en `CHARACTER(n)`. *(→ `char(n)` fijo salvo `ProvNombre`, `AreaEmails`, `AreaRespons`)*
- [x] C.5 Ajustar `spec.md` (§4.3/§4.4/§4.5, R-002/R-006/R-036/R-037), `plan.md` (§2.2/§2.3/§3.3/§5/§6) y registrar hallazgos en `docs/analisis-ATPPuestos.md`. *(columnas legacy, sin `IDENTITY`, sin `DEFAULT`, FK física ausente)*

## D. Scaffold backend (T-0xx)

- [x] T-001 Crear `backend/` con Spring Boot 3.x + **Maven Wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`). *(Spring Boot 3.5.16, Maven 3.9.11, JDK 17)*
- [x] T-002 Añadir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `mssql-jdbc`, `spring-boot-starter-test`. *(+ `h2` test-scope para arranque sin BD)*
- [x] T-003 Configurar `application.yml` con `${DB_URL}`/`${DB_USER}`/`${DB_PASSWORD}` y `ddl-auto=none` (R-204, R-205).
- [x] T-004 Crear paquete base `ar.com.atp.puestos` con subpaquetes `nucleo/{domain,repository,service,web,web/dto}` y `shared`.
- [x] T-005 Añadir un `HealthController` (`/api/v1/health`) y verificar arranque sin BD (perfil de test). *(BUILD SUCCESS; 2 pruebas OK)*

## E. Entidades y repositorios (T-1xx)

- [x] T-101 Entidad `Provincia` → `@Table(name="PROVINCI")`; incluir columnas legacy `ProvCodAfip` (`@Column(nullable=false)`, default `0`) (R-001, R-002, R-036, R-205).
- [x] T-102 Entidad `Localidad` → `@Table(name="Localida")` + `@ManyToOne` `Provincia` sobre `ProvCod` (asociación lógica, sin FK física); incluir `LocCodAfip` legacy (R-003, R-036).
- [x] T-103 Entidad `Calle` → `@Table(name="CALLES")` + legacy `CallAlt1/2/3`.
- [x] T-104 Entidad `Camino` → `@Table(name="CAMINOS")`.
- [x] T-105 Entidad `Area` → `@Table(name="Areas")` (**`AreaId` → `BigDecimal`**, `decimal(10,0)`) + legacy `AreaBaja/AreaEmails(NOT NULL)/AreaAdjun/AreaRespons` (R-036).
- [x] T-106 Entidad `Jurisdiccion` → `@Table(name="Jurisdic")`.
- [x] T-107 Entidad `Feriado` → `@Table(name="FERIADOS")` (PK `FerFecha` `LocalDateTime`; `FerDescripcion`→`@Column(name="FerDescrip")`; auditoría `NOT NULL`).
- [x] T-108 Entidad `Producto` → `@Table(name="PRODUCTO")` + `@OneToMany` a `ProductoVigencia` (R-004); legacy `ProdReppa`; `ProdIMax*`/`UMedNombre` `@Transient`.
- [x] T-109 Entidad `ProductoVigencia` → `@Table(name="PRODUCT1")` con **`@EmbeddedId` + `@MapsId`** (`ProductoVigenciaId`: `ProdCod`+`ProdFchVig` `LocalDateTime`); `ProdImpu1/2` `NOT NULL`; legacy `ProdImpu5/ProdFeLog/ProdUsu`. *(implementado con `@EmbeddedId`+`@MapsId` en lugar de `@IdClass`: evita el `INSERT` con `NULL` en la FK-mapeada-como-PK del patrón id compuesto unidireccional; validado contra DDL H2)*
- [x] T-110 Repositorios Spring Data para las 9 entidades (incl. `ProductoVigenciaRepository.findVigenteHastaHoy`). *(`ProductoRepository` con `@Query` explícita para `order by`)*

## F. Servicios (T-2xx)

- [x] T-201 `NucleoConsultaService`: listados y búsquedas por clave (catálogos).
- [x] T-202 `BusquedaService.busDesLoc(locCod)` equivalente a `Bus_DesLoc` (R-010). *(método `nombreLocalidad`)*
- [x] T-203 `BusquedaService.buscarCamino(camID)` con `TRIM(CanRuta)` (R-011). *(método `rutaCamino`)*
- [x] T-204 `ProductoService.importesVigentes(prodCod)` implementa `ProdIMax1..4` con `0` por defecto (R-005).
- [x] T-205 `NucleoEscrituraService`: alta/modificación/baja de maestras; **PK provista en el body** (sin `IDENTITY`) y detección de PK duplicada → excepción de conflicto (R-030, R-031, R-037); aplicación de defaults a columnas legacy `NOT NULL` (`0`/`\"\"`) (R-036).
- [x] T-206 Auditoría de `Feriado`: rellenar `FerFchReg` (server time) y `FerUsuario` en servidor (R-033). *(`app.auditoria.usuario`, default `SISTEMA`)*
- [x] T-207 `ProductoService.guardar(producto, vigencias)`: persistencia atómica raíz+hijas, unicidad de `ProdFchVig` (R-034); recálculo derivados sin persistir (R-035).
- [x] T-208 `NucleoConsultaService.listarLocalidades/obtenerLocalidad` con la provincia resuelta **en la misma consulta** (`left join fetch`) y mapper tolerante a FK huérfana: `provCod`/`provNombre` en `null` en vez de `500 ERROR_INTERNO` (R-024, R-025). *(3 consultas con fetch join en `LocalidadRepository` + guarda `Hibernate.isInitialized` en `NucleoMapper`)*

## G. Controladores REST (T-3xx)

- [x] T-301 `ProvinciaController` (`GET /provincias`, `/provincias/{provCod}`).
- [x] T-302 `LocalidadController` (`GET /localidades`, `/localidades/{locCod}`, `/localidades/{locCod}/nombre`).
- [x] T-303 `CalleController` (`GET /calles`).
- [x] T-304 `CaminoController` (`GET /caminos`, `/caminos/{camId}/ruta`).
- [x] T-305 `AreaController` (`GET /areas`).
- [x] T-306 `JurisdiccionController` (`GET /jurisdicciones`).
- [x] T-307 `FeriadoController` (`GET /feriados?desde&hasta`).
- [x] T-308 `ProductoController` (`GET /productos`, `/productos/{prodCod}`, `/productos/{prodCod}/importes-vigentes`).
- [x] T-309 Manejo uniforme de errores `{ code, message }` + `404` (R-023) + `409` (R-031) + `400` Bean Validation (R-032).
- [x] T-310 `POST` de las 8 maestras (201 + `Location`; **PK requerida en el body** R-037; 409 PK duplicada) — R-030/R-031/R-037.
- [x] T-310b Sembrar defaults de columnas legacy `NOT NULL` en el alta (`ProvCodAfip=0`, `LocCodAfip=0`, `AreaEmails=\"\"`) — R-036.
- [x] T-311 `PUT /provincias/{provCod}`, `/localidades/{locCod}`, `/calles/{callCod}` — R-030.
- [x] T-312 `PUT /caminos/{camId}`, `/areas/{areaId}`, `/jurisdicciones/{jurCod}` — R-030.
- [x] T-313 `PUT /feriados/{ferFecha}` con auditoría automática — R-030/R-033.
- [x] T-314 `PUT /productos/{prodCod}` con vigencias anidadas (P-03) — R-034.
- [x] T-315 `DELETE` de las 8 maestras (204 / 404) — R-030/R-031.

## H. Pruebas y verificación (T-4xx — puerta G-TEST)

- [x] T-401 `ProductoServiceTest` parametrizado para `ProdIMax1..4` (sin vigencias / pasadas / futuras / hoy). *(10 pruebas)*
- [x] T-402 `BusquedaServiceTest` para `Bus_DesLoc` y `BuscarCamino` (existente/inexistente/`Trim`). *(8 pruebas)*
- [x] T-403 Pruebas de contrato (MockMvc) lectura para R-020…R-023 (200/404). *(`NucleoLecturaControllerTest`, 20 pruebas)*
- [x] T-404 Pruebas de integración de lectura por entidad (G-DATA), marcadas para ejecutarse solo con BD. *(8 `*RepositoryIT`, 16 pruebas; gate `ATP_DB_IT=true`; 16/16 en verde contra Rentas)*
- [x] T-405 Completar la **matriz de paridad** (`plan.md` §5) y adjuntar evidencia (G-TEST). *(ver `plan.md` §5.1)*
- [x] T-406 Pruebas de contrato escritura: ciclo crear→modificar→eliminar por entidad + duplicado (409) + payload inválido (400) — R-030…R-032. *(`NucleoEscrituraControllerTest`, 44 pruebas)*
- [x] T-407 `FeriadoControllerWriteIT`: auditoría automática (R-033). `ProductoControllerWriteIT`: atomicidad y unicidad de vigencias (R-034) y recálculo de derivados (R-035). *(gate `ATP_DB_IT=true`; `ProdCod` de prueba corregido a `32000`: `PRODUCTO.ProdCod` es `smallint` y `999999` desbordaba el tipo ⇒ HTTP 500 espurio)*
- [x] T-408 Regresión de FK huérfana y ausencia de N+1 (R-024, R-025): `LocalidadRepositoryIT` (30 huérfanas con provincia `null`, 1 sola consulta JDBC), `NucleoMapperTest` (proxy sin inicializar, nombre `null`, provincia válida) y `NucleoConsultaServiceTest` (listar sin filtro y obtener por PK). *(`run-mvn-jdk17.cmd -B clean test` → 88 pruebas; `test "-Dtest=*IT"` → 29 IT en verde)*
- [x] T-409 **Logging de excepciones no controladas** (R-206) — cierra el hallazgo mayor **H-2** del Gate 2 de T-503: `GlobalExceptionHandler.handleGeneric` responde `500` uniforme pero **no registra** la excepción ⇒ un error en producción sería invisible. Alcance: (a) log `ERROR` con tipo y traza en el servidor; (b) el cuerpo de la respuesta **no** expone la traza; (c) prueba que fuerza la excepción y verifica ambos extremos. *(`GlobalExceptionHandler.java:74-79`; spec `spec.md` §6 R-206, aprobado G-SPEC 2026-10-21. **Entregado 2026-10-21**: commit `05c8520`; `run-mvn-jdk17.cmd -B clean test` → **91/91** verdes, EXITCODE 0 — 88 previas + 3 nuevas en `GlobalExceptionHandlerTest`.)*

## I. Web (fase 2 — T-5xx) — diseño del origen (P-01)

- [x] T-501 Scaffold `frontend/` (Angular 17+ standalone) + `HttpClient` + proxy a `/api/v1`. *(Angular 22.2.3 standalone, zoneless, vitest, Angular Material 22; verificación QA 7/7 — `npm ci`/`npm run build`/`npm test` en verde)*
- [x] T-502 Extraer HTML/CSS/JS de los WebPanels legacy de las maestras vía MCP (`genexus_read` source) y documentar la referencia visual en `openspec/features/01-nucleo-datos/ui-reference.md` *(desviación aceptada: no fue `docs/`)*.
- [x] T-503 Componente `localidad-list` (standalone, Reactive Forms) con filtro por provincia. *(Gate 2 **APROBADO CON OBSERVACIONES** 2026-10-21: 0 bloqueantes, 2 mayores, 9 menores, 6 observaciones — ver §K. Commit `f7c2589`; verificación en navegador real contra la BD `Rentas`.)*
- [x] T-504 Pantalla **ABM** de maestras simples (Provincia, Calles, Caminos, Áreas, Jurisdicciones) replicando el diseño del origen. *(spec: `openspec/features/01-nucleo-datos/ui-abm-maestras.md` — **APPROVED** (G-SPEC) 2026-10-21; 20 requisitos EARS `R-ABM-01…20`; **frontend-only**, G-POSTMAN N/A. Cierra además M-7. **Entregado 2026-10-21**: commit `a558a27` (42 archivos, +3157); Gate 2 **APROBADO CON OBSERVACIONES** — 0 bloqueantes, 0 mayores, 4 menores, 8 observaciones; **19/20 requisitos `R-ABM-01…20` satisfechos**, `R-ABM-18` **PARCIAL** (ver §L); `npm test` 67/67, `npm run build` EXITCODE 0 (initial 310,86 kB), 91/91 unitarios backend + 29/29 IT contra `Rentas`; smoke en navegador real con 17 pares método/ruta y 31 pasos documentados.)*
- [x] T-504b Pantalla **ABM** de `Localidades` (5 campos + FK `ProvCod` como lookup) reutilizando el shell y `core/` de T-504. *(desdoblado de T-504: no es "maestra simple"). spec: `openspec/features/01-nucleo-datos/ui-abm-localidades.md` — **APPROVED** (G-SPEC) 2026-10-21; 17 requisitos `R-ABM-L-01…17` + 7 `R-SHELL-01…07`. **Generaliza el shell a N campos** con default de 2 (las 5 ABM de T-504 **no** modifican descriptor ni página). Cierra m-1, m-2, m-3 y `R-ABM-18` parcial, más M-1/M-4/M-5/M-6 del listado. Frontend-only ⇒ G-POSTMAN N/A. **CERRADO 2026-10-21** tras **3 rondas de gate**: (1) RECHAZADO — 2 mayores (H-1 premisa de spec falsa, H-2 contraste 3,61:1) + 3 menores; (2) re-verificación RECHAZADA — 1 mayor nuevo (N-1, contraste del botón `Guardar` 3,34:1) + 2 menores; (3) re-verificación focalizada **APROBADA** — 0 bloqueantes, 0 mayores, 0 menores, 3 observaciones, **24/24 requisitos**. Correcciones: `cac581d` (R-207 código físico de la FK + R-208 aborto del cliente, con Postman en el mismo commit), `cf14e26` (T-412), `d1a9002` (sincronización de `docs/design.md`), `adc346d` (T-414), `b654405` (T-415/R-209). Evidencia final: ratios medidos con axe in-page en 2 estados (botón `Guardar` 5,9448:1; `mat-label` 5,7656:1) en las 6 ABM · Lighthouse accessibility 100 en 12 corridas · 105/105 unitarios + 32/32 IT · 134/134 tests de frontend · las 6 ABM con alta 201 y baja 204 y BD sin residuos.)*
- [x] T-410 **Exponer el código físico de la FK en `LocalidadDto`** (R-207) — cierra **H-1** del Gate 2 de T-504b: `provCod` se derivaba de la asociación resuelta, así que para las 30 filas huérfanas iba en `null` y Jackson lo **omitía** ⇒ el formulario no podía representar el estado huérfano. Alcance: (a) leer el valor físico de `Localida.ProvCod`; (b) `provCod` = código físico (`0`) y `provNombre` = `null` en las huérfanas; (c) IT contra `Rentas`; (d) **actualizar la colección Postman** (G-POSTMAN deja de ser N/A). *(`spec.md` §5.3 R-207, aprobado G-SPEC 2026-10-21. **Entregado 2026-10-21**: commit `cac581d` — columna sombra de sólo lectura en `Localidad` (`insertable=false, updatable=false`); el JSON real de la huérfana 16342 pasó de `{locCod,locNom,locCodPost}` a `{…,provCod:0}`; **0** filas con `provCod` ausente (antes 30); Postman actualizado en el mismo commit (44 requests antes y después); 100/100 unitarios + 32/32 IT verdes.)*
- [x] T-411 **El aborto del cliente no es un error interno** (R-208) — cierra **m-3** del Gate 2 de T-504b: el `switchMap` del listado aborta la request en vuelo y el backend la registraba como `ERROR` con traza, ensuciando la señal de R-206. Alcance: handler específico que registra a nivel `INFO` sin traza y no devuelve `500 ERROR_INTERNO`; prueba que comprueba que **no** hay evento `ERROR`. *(`spec.md` §6 R-208, aprobado G-SPEC 2026-10-21. **Entregado 2026-10-21**: commit `cac581d` — handler para `AsyncRequestNotUsableException` (Spring 6.1+) y `ClientAbortException` (Tomcat), detectado empíricamente abortando un socket a mitad de respuesta; log `INFO` sin traza y sin cuerpo de respuesta.)*
- [x] T-412 **Corrección del frontend tras el Gate 2 de T-504b** — (a) **H-2**: corregir el contraste de los `mat-label` (3,61:1 → ≥ 4,5:1) en el **tema compartido**, sin salir de la paleta de `docs/design.md` §2.2, y re-medir con `lighthouse_audit`; (b) **H-1**: usar el JSON **real** de la API como fixture y mostrar la opción sintética `(ProvCod 0) — sin provincia` sin dejar el `select` en blanco; (c) **m-1**: `provCod: number \| null`; (d) **m-2**: landmark `<main>`; (e) re-verificar con `lighthouse_audit` que **no** quedan auditors de accesibilidad fallando. *(`ui-abm-localidades.md` §15. **Entregado 2026-10-21**: commits `cf14e26` (código) y `d1a9002` (sincronización de `docs/design.md`). Token `$atp-text-muted` `#747474` → `#555555` ⇒ **5,77:1** sobre `#f3dde1` (el fondo real del `mat-form-field` es `neutral-variant.90` de la rampa rose, **no** `neutral.20`) y 7,46:1 sobre blanco; regresión fijada en `frontend/src/theme/theme-contrast.spec.ts` (falla si baja de 4,5:1). Fixture = JSON real `{"locCod":16342,"locNom":"ACHIRAS","locCodPost":5833,"provCod":0}`; la opción sintética lleva el valor real `0` porque `MatSelect` nunca selecciona una opción con valor `null`/`undefined`. 129/129 tests verdes.)*
- [x] T-414 **Contraste del botón `Guardar`** (cierra **N-1** del re-verificador de T-504b) — el `mat-label` quedó en 5,77:1, pero el botón `Guardar` sigue en **3,34:1** (`on-primary` = tono 100 de la rampa rose = `#1c000c` sobre `#c60b44`). Causa raíz: `$atp-theme-overrides` sobreescribe `on-surface-variant` pero **no** `on-primary`, y `$atp-primary-contrast: #ffffff` queda declarado y sin usar. Fix: una línea (`'on-primary': $atp-primary-contrast`) ⇒ `#ffffff` sobre `#c60b44` = **5,945:1**, + caso de regresión en `theme-contrast.spec.ts` que mida **también** el botón. *(`ui-abm-localidades.md` §13.7 — la medición debe hacerse en **estado de edición**: los controles deshabilitados están exentos del audit). **Entregado 2026-10-21**: commit `adc346d`; ratio 3,3493:1 → 5,9448:1; CSS de producción con `--mat-sys-on-primary: #ffffff` por encima de `#1c000c`; regresión verificada por mutación (al revertir el override fallan 3 tests con el ratio exacto del verificador); 134/134 tests verdes.)*
- [x] T-415 **404/405 en rutas y métodos inexistentes** (R-209) — cierra **N-3** (preexistente) del re-verificador de T-504b: una ruta inexistente o un método no soportado caían en el handler genérico ⇒ `500 ERROR_INTERNO` con traza en el log. Alcance: `NoResourceFoundException` → `404` y `HttpRequestMethodNotSupportedException` → `405`, ambos con el cuerpo de error uniforme y **sin** registro `ERROR`. *(`spec.md` §5.3 R-209, aprobado G-SPEC 2026-10-21. **Entregado 2026-10-21**: commit `b654405` — `NoResourceFoundException`/`NoHandlerFoundException` → 404 `NO_ENCONTRADO`; `HttpRequestMethodNotSupportedException` → 405 `METODO_NO_SOPORTADO` con encabezado `Allow`; log a nivel `INFO` sin traza. La clase real de excepción se determinó **empíricamente** contra el jar vivo (`ResourceHttpRequestHandler` captura el path y lanza `NoResourceFoundException`, no `NoHandlerFoundException`). R-206 **intacto**: una excepción no controlada real sigue dando `500` + `ERROR` con traza. G-POSTMAN N/A (44 requests). 105/105 unitarios + 32/32 IT verdes.)*
- [ ] T-413 **Alinear `docs/design.md` con el código implementado** (observaciones del cierre de T-412) — el doc quedó con 7 desalineaciones detectadas al sincronizar el token de contraste: (1) `$atp-outline: #828282` no está documentado en §2.2 (sólo aparece en §2.1 como "Deshabilitado"); (2) `$atp-focus-ring: #1d6893` no está documentado en §2.2 ni en §7; (3) `$atp-warn: #FF8000` no se usa en ninguna parte y, si se usara como texto o acento, daría **2,52:1** (< 3:1 de §7) ⇒ riesgo latente; (4) §3 propone `display 24px/600` que no se usa y `styles.scss` fija `font-size: 16px`, valor **fuera de la escala** del doc; (5) hay **dos pilas de fuentes**: la de §3 (aplicada al `body`) y la que compila el tema Material, que el doc no menciona; (6) §5 promete `MatPaginator` y **no existe** en el frontend (la grilla renderiza las 3989 filas) ⇒ ya trazado como gap de T-507; (7) §6 promete "código de correlación" y la UI muestra sólo el `message` (el `code` es un código de negocio, no un identificador de correlación). *Sin cambios de comportamiento: es documentación. Debe resolverse antes de replicar el diseño en los módulos 02-10.*
- [x] T-505a Pantalla **ABM** de `Feriados` (formulario raw HTML legacy: 5 campos, lookup **Get** por fecha, botonera **Check**/**Ayuda**, 2 campos de auditoria en solo lectura). *(spec: `openspec/features/01-nucleo-datos/ui-abm-feriados.md` — **APPROVED** (G-SPEC) 2026-10-21; 18 requisitos `R-FER-01…18`. **Desdobla T-505**: `Productos` (2 niveles con grilla anidada) no entra en el shell de formulario plano y va a **T-505b**. Extiende el shell de T-504b con 3 `input` opcionales (tipo de campo `fecha`, accion extra, botones extra) => las 6 ABM previas no cambian. **Cierra SA-09** (formato DATE/DATETIME runtime: `dd/MM/yyyy`, a medianoche). Frontend-only => G-POSTMAN N/A. **Entregado 2026-10-21**: commits `3574164` (implementación) y `4314b8a` (cierre de D-14: la auditoría muestra `dd/MM/yyyy HH:mm:ss` con el flag opcional `conHora`). 3 extensiones del shell **opcionales** ⇒ las 6 ABM previas sin cambios observables y sus specs intactas. La fecha viaja a **medianoche** (`T00:00:00`) y los query params en **ISO date-time** (el parser de Spring rechaza `yyyy-MM-dd`: corregido en la spec, commit `7ef262a`). `npm test` ⇒ 22 archivos / **188/188** verdes; `npm run build` ⇒ EXITCODE 0, `initial` 311,41 kB (+0,40 kB), sin warnings. D-14 a CERRADO; D-15/D-16/D-17 documentadas y aceptadas. **Gate 2 APROBADO CON OBSERVACIONES** (2026-10-22): 0 bloqueantes, 0 mayores, 3 menores, 5 observaciones; **18/18 requisitos `R-FER-01…18`**. Evidencia: smoke en navegador real **60/60 checks PASS** (re-corrido; el FAIL de la 1a pasada era un error del script de test, verificado byte a byte), axe in-page **0 violaciones en los 2 estados** (carga inicial y edicion con el boton habilitado) con `color-contrast`/`landmark-one-main`/`label` en `passes`, Lighthouse accessibility score 1, contraste medido en el DOM (boton `Guardar` **5,94:1**, `mat-label` **5,77:1**, auditoria **9,77:1**), body del `POST` de **3 claves sin auditoria**, `PUT` con la PK en el path (sin codificar los `:`) y en el body, D-14 verificado en produccion (`22/10/2026 00:14:29` en input text readonly, `ferUsuario` = `SISTEMA`), regresion de las **6** ABM previas 30/30 con BD en baseline, 105/105 unitarios + 188/188 tests de frontend. Ver seccion N.)*
- [x] T-505b Pantalla **ABM** de `Productos` (2 niveles: raiz de 12 campos, 4 derivados en solo lectura, + grilla anidada de vigencias). *(spec: `openspec/features/01-nucleo-datos/ui-abm-productos.md` — **APPROVED** (G-SPEC) 2026-10-22; 18 requisitos `R-PROD-01…18`. **No** reutiliza el shell de formulario plano tal cual: la grilla del nivel 1 necesita un punto de extension (`<ng-content>`) en el panel del registro, y los importes requieren un tipo de campo `decimal` (los `numero` actuales son enteros >= 0). Las **7** ABM previas no cambian (R-PROD-15). D-19 (sin lookup de unidad de medida: el modulo no expone ese endpoint), D-20 (rotulo de `ProdFchVig` derivado, mismo patron que SA-04), D-21 (navegacion sobre la coleccion descargada) y D-22 (los 4 derivados `ProdIMax1..4` requieren una 2ª request a `importes-vigentes`, porque `ProductoDto` no los incluye). Cierra el hallazgo **N-4** del lado de la UI: `PRODUCTO.ProdCod` es `smallint` => la PK se limita a 0…32767 en el formulario (R-PROD-07). Frontend-only => G-POSTMAN N/A. **Entregado 2026-10-22**: commit `4fd4aa6` (30 archivos, +3399); 2 extensiones del shell **aditivas**; cierra ademas los 3 menores del Gate 2 de T-505a (M-1 `noEnviar` derivado de `conHora`, M-2 guard de calendario en el `Get`, M-3: el base baja de 494 a **473** lineas con 6 funciones puras extraidas a `core/maestro-registro.ts`); `npm test` ⇒ 31 archivos / **265/265** verdes (188 previos + 77 nuevos); `npm run build` ⇒ EXITCODE 0, `initial` 311,61 kB (+0,20); ningún archivo de `frontend/src` > 500 líneas.)*
- [ ] T-506 Prueba E2E (Playwright) de `ListadoLocalidades` + un ABM contra el backend.
- [ ] T-507 Paridad completa de `ListadoLocalidades` con el legacy: 4 filtros (`LocCod`, `LocNom`, `LocNomProv`, `LocCodPost`), grilla con `MaxRows=5` + paginación, columna de selección y acción `Cancel`. *(4 gaps detectados en T-503, trazados en `ui-reference.md` §9)*

## J. Cierre

- [x] J.1 Actualizar `docs/analisis-ATPPuestos.md` con los hallazgos de G-DATA. *(2026-10-16)*
- [x] J.2 `mem_session_summary` + `engram sync`. *(2026-10-21: `engram sync` → 45 observaciones / 16 sesiones / 52 prompts; espejo `.engram/` commiteado. El resumen de sesión cubre T-409, T-504 y el Gate 2.)*
- [ ] J.3 Marcar `01-nucleo-datos` como cerrado y habilitar el módulo `02-personas`.

## K. Registro de hallazgos — Gate 2 de T-503 (2026-10-21)

> Veredicto: **APROBADO CON OBSERVACIONES** — 0 bloqueantes, 2 mayores, 9 menores, 6 observaciones.
> Reporte completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\atp-t503-gate2\REPORTE-GATE2-T503.md`.
> Política: todo hallazgo se **registra con destino**; sólo se ejecuta dentro de la tarea que ya
> toca ese código (no se amplía el alcance de ninguna tarea).

### K.1 Mayores

| # | Hallazgo | Destino | Estado |
|---|---|---|---|
| **H-1** | Frontend de T-503 sin commitear y `tasks.md` sin marcar T-503 (gobernanza de entrega) | — | **RESUELTO** 2026-10-21: commit `f7c2589` (frontend) + cierre administrativo en esta actualización de `tasks.md` y `spec.md §8` |
| **H-2** | `GlobalExceptionHandler.handleGeneric` no registra la excepción ⇒ un `500` en producción sería invisible | **T-409** (R-206) | **RESUELTO** 2026-10-21: commit `05c8520` — log `ERROR` con traza completa; el cuerpo `{code:"ERROR_INTERNO", message:…}` no cambió; 91/91 tests verdes |

### K.2 Menores

| # | Hallazgo | Destino |
|---|---|---|
| **M-1** | `locCodPost` es `null` en 1395/3989 filas (35 %) pero el DTO y el tipo TS lo declaran no-nullable | **CERRADO** en T-504b (R-SHELL-07): tipo `number \| null` y render sin error |
| **M-2** | `locNom` vacío en 3 filas | Dato legacy real → sin acción |
| **M-3** | `findByProvCodConProvincia` filtra con `where p.provCod = :provCod` sobre un `LEFT JOIN` ⇒ `?provCod=0` devuelve 0 filas en vez de las 30 huérfanas | Desviación D-5, ya documentada en el `javadoc`; cambiar sólo con nuevo cambio de contrato en spec |
| **M-4** | Doble camino de error en el frontend (`catchError` por fuente + `catchError` externo); el externo nunca se alcanza en carga inicial | **CERRADO** en T-504b (R-SHELL-05): un solo camino de error |
| **M-5** | Sin cancelación de requests en vuelo (falta `switchMap`): cambios rápidos de filtro pueden dejar respuestas fuera de orden | **CERRADO** en T-504b (R-SHELL-06): `switchMap` |
| **M-6** | Sin `distinctUntilChanged` / `debounceTime` sobre `valueChanges` ⇒ una request por cambio | **CERRADO** en T-504b (R-SHELL-06): `distinctUntilChanged` + `debounceTime(300)` |
| **M-7** | `app.spec.ts` perdió el test *"should render title"* (regresión de cobertura del smoke del shell) | **T-504** (toca `app.routes.ts` y `app.spec.ts`) |
| **M-8** | El IT relajó la aserción literal original por un conteo de particiones `conProvincia`/`huerfanas` (correcto, pero cobertura indirecta de R-024) | Sin acción funcional → dejar el comentario explicativo en el test |
| **M-9** | `npm ci` reporta 2 vulnerabilidades (1 moderate, 1 high) | Seguimiento de dependencias → próxima actualización de Angular/Material |

### K.3 Observaciones

| # | Observación | Estado |
|---|---|---|
| **O-1** | `spec.md §8` conservaba los 8 checkbox de aceptación sin marcar pese a R-024/R-025 verificados | **RESUELTO** 2026-10-21: los 8 quedan `[x]` con evidencia |
| **O-2** | La spec fue enmendada y commiteada (`4aca6e2`) sin entrada de aprobación G-SPEC | **RESUELTO** 2026-10-21: entrada de aprobación G-SPEC agregada en el encabezado de `spec.md` |
| **O-3** | El ordenamiento usa `RTRIM(LocNom)` (recorta sólo a la derecha) frente al `trim` de GeneXus (recorta ambos lados); observablemente idéntico con `char(n)` (relleno a la derecha) | Aceptada → documentar en el `javadoc` del repositorio |
| **O-4** | `locNomProv` está en 0 filas ⇒ la columna "Provincia" no puede derivarse de ella; la UI usa `LocalidadDto.provNombre` (requisito del encargo cumplido) | Sin acción |
| **O-5** | Existe una localidad `locCod=0`, `locNom=''`, `locCodPost=0`, `provNombre='CHACO'` (primera fila del grid; se lee como dato inválido) | Dato legacy real → sin acción |
| **O-6** | Los IT usan `@ActiveProfiles("dev")` sin `application-dev.yml` asociado (el perfil resuelve al `application.yml` base) | Cosmético → documentar en el README de pruebas |

### K.4 Desviaciones legacy aceptadas (no son defectos)

Las 4 gaps del grid de `ListadoLocalidades` (4 filtros vs 1, `MaxRows=5` sin paginación, columna de
selección y acción `Cancel` ausentes) están trazadas en **T-507** (`ui-reference.md §9`).
La desviación **D-5** (`?provCod=0` ⇒ 0 filas) está documentada en `LocalidadRepository` y fue
aceptada en Gate 2.

## L. Registro de hallazgos — Gate 2 de T-504 (2026-10-21)

> Veredicto: **APROBADO CON OBSERVACIONES** — 0 bloqueantes, 0 mayores, 4 menores, 8 observaciones.
> **19 de 20** requisitos `R-ABM-01…20` satisfechos; `R-ABM-18` (Accesibilidad AA) **PARCIAL**.
> T-409 (R-206) cerrado y verificado en el mismo gate.
> Reporte completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\atp-t504-gate2\REPORTE-GATE2-T504.md`
> (14 logs + 16 capturas + wrapper, fuera del repo).
> Evidencia: 91/91 unitarios backend · 29/29 IT contra `Rentas` · 67/67 tests de frontend ·
> build EXITCODE 0 (initial 310,86 kB) · smoke en navegador real con 17 pares método/ruta
> y 31 pasos · BD `Rentas` restaurada sin residuos de prueba · procesos detenidos.

### L.1 Menores

| # | Hallazgo | Destino |
|---|---|---|
| **m-1** | `maestro-abm.base.ts:224-248` / `:257-278` — sin guarda de request en vuelo: 3 clics en *Guardar* ⇒ 3 POST (201 + 2×409) y el mensaje de conflicto pisa el alta exitosa | **CERRADO** en T-504b (R-SHELL-01): guarda `escrituraEnVuelo` + botón deshabilitado |
| **m-2** | `maestro-field.component.ts:45-55` — los inputs inválidos no llevan `aria-invalid="true"` | **CERRADO** en T-504b (R-SHELL-02): directiva `maestro-aria-invalid` |
| **m-3** | `maestro-abm-shell.component.html:141` — el `mat-error` de campo no es región live (sólo `aria-describedby`); el error de API sí usa `role="alert"` | **CERRADO** en T-504b (R-SHELL-03): región `aria-live` en el error de campo |
| **m-4** | `npm ci` reporta 2 vulnerabilidades (1 moderate, 1 high) | Deuda npm → próxima actualización de Angular/Material (continúa **M-9** de §K.2) |

### L.2 Observaciones

| # | Observación | Disposición |
|---|---|---|
| **o-1** | `maestro-http.service.spec.ts` usa el literal `/api/v1` en vez de `environment.apiBaseUrl` | Aceptada: es un *contract test* que fija la URL pública; si el prefijo cambia, el test debe fallar |
| **o-2** | `as unknown as TWrite` en `construirPayload` (`maestro-abm.base.ts:359`) | Aceptada: el body se arma con `pkCampo`/`descCampo` dinámicos; el cast es el precio de que el shell sea genérico |
| **o-3** | 24 tabuladores para llegar al formulario (la grilla de `CAMINOS` tiene 9.694 filas) | Aceptada; queda para T-507 (paginación) |
| **o-4** | `cargar()` sin `switchMap` (requests de recarga concurrentes) | Aceptada; equivale a **M-5** → T-504b |
| **o-5** | `AreaDto.areaId: number \| string` es más laxo que la realidad (siempre entero) | Aceptada: la laxitud absorbe la serialización de `BigDecimal` de Java |
| **o-6** | G-POSTMAN N/A (frontend-only) pero **coherente**: 44 = 44 mapeos y 7 URLs reales respondieron 200 | Sin acción |
| **o-7** | El arranque del IDE GeneXus en la máquina del responsable tocó `GXSPC007/userControls.v2.ari` y creó `Locks\*.lock` | **R-201 se sostiene**: 0 llamadas `genexus_*` en la sesión; ningún objeto de la KB fue modificado, compilado ni eliminado. Los artefactos tocados son bookkeeping de IDE/engine (locks, caché de preferencias de la IDE, `.mdf`/`.ldf` del motor local) y el mismo conjunto ya había sido registrado en el Gate 2 de T-503. **Causa raíz**: hay 2 procesos `GeneXus` (PID 300 / 25640) y 2 `GxMcp.Gateway` (PID 8544 / 22884) vivos en la máquina ⇒ efecto del IDE abierto, no de los agentes. *Acción sugerida al responsable*: cerrar la IDE y el gateway cuando no se esté extrayendo de la KB, para que R-201 sea verificable sin factores externos |
| **o-8** | Cierre administrativo: `tasks.md` seguía con T-504 sin marcar y `spec.md §8` decía *"88/88"* | **RESUELTO** 2026-10-21 en este mismo commit: T-504 marcado `[x]` y el conteo actualizado a **91/91 + 29/29 IT** |

### L.3 Requisito parcial

| Requisito | Estado | Pendiente |
|---|---|---|
| **R-ABM-18** (Accesibilidad AA) | **CERRADO** 2026-10-21 — etiquetas `label[for]`, foco visible, navegación por teclado y `role="alert"` en el error de API verificados; `aria-invalid` (**m-2**) y región live en el `mat-error` (**m-3**) agregados en T-504b; **contraste medido**: `$atp-text-muted` `#747474` → `#555555` ⇒ **5,77:1** sobre `#f3dde1` (fondo real del `mat-form-field`), con regresión fijada en `frontend/src/theme/theme-contrast.spec.ts` | **Pendiente**: re-verificación con `lighthouse_audit` en el Gate 2 de re-verificación de T-504b |

## M. Registro de hallazgos — re-verificación de T-504b (2026-10-21)

> Veredicto de la re-verificación: **RECHAZADO** — 0 bloqueantes, **1 mayor** (N-1), 2 menores,
> 5 observaciones. Los **6 hallazgos del gate anterior quedaron CERRADOS** (H-1, H-2, m-1, m-2, m-3,
> o-1), verificados con evidencia real: `GET /localidades/16342` → `provCod: 0`; **0** filas sin
> `provCod` (antes 30); el `mat-select` muestra `(ProvCod 0) — sin provincia` con `aria-disabled`;
> log del servidor con **1** línea `INFO` de R-208 y **0** `ERROR`; Lighthouse accessibility **100**;
> Postman: **32/32** aserciones contra el backend vivo, 44 requests sin cambios.
> Reporte: `C:\Users\walterg20\AppData\Local\Temp\opencode\atp-t504b-reverif\REPORTE-REVERIFICACION-T504B.md`

### M.1 Hallazgos nuevos

| # | Severidad | Hallazgo | Destino |
|---|---|---|---|
| **N-1** | mayor | El botón `Guardar` queda en **3,34:1** (`on-primary` = tono 100 de la rampa rose = `#1c000c` sobre `#c60b44`); afecta a las 6 ABM. `$atp-theme-overrides` sobreescribe `on-surface-variant` pero **no** `on-primary`, y `$atp-primary-contrast: #ffffff` queda declarado y sin usar | **CERRADO** (T-414, commit `adc346d`): `'on-primary': $atp-primary-contrast` ⇒ **5,9448:1** medido con axe in-page en estado de edición |
| **N-2** | menor | Con R-207 la fila huérfana ya **no** queda con `Guardar` deshabilitado (antes sí, por `provCod: null`); al guardar, el backend responde **409 `PROVINCIA_INEXISTENTE`** y la localidad no se puede editar. No es una regresión: el backend **nunca** aceptó una provincia inexistente; R-207 sólo hizo visible la restricción (la UI muestra el `message` de la API, R-ABM-L-11) | Aceptada como restricción de negocio; **queda para J.3** (documentar el 409 de la fila huérfana en la ayuda del ABM) |
| **N-3** | menor (preexistente) | Rutas inexistentes y métodos no soportados caían en el handler genérico ⇒ **500 `ERROR_INTERNO`** con traza, en vez de 404/405 | **CERRADO** (T-415, commit `b654405`, R-209): 404 `NO_ENCONTRADO` y 405 `METODO_NO_SOPORTADO` con `Allow`, log `INFO` sin traza, **0** eventos `ERROR` |

### M.2 Observaciones

| # | Observación | Disposición |
|---|---|---|
| **o-1** | Los criterios de aceptación de `ui-abm-localidades.md` §13.4/13.5 decían `91/91` y `29/29` | **RESUELTO**: actualizados a `100/100` y `32/32` |
| **o-2** | `theme-contrast.spec.ts` no tenía regresión de `on-primary` | **RESUELTO** en T-414 (el test mide ahora el botón) |
| **o-3** | `NucleoEscrituraControllerTest.java` = 512 líneas (preexistente) | Observación: G4 aplica a archivos de **configuración**; un spec no lo es. Se puede partir en el próximo refactor |
| **o-4** | Colección Postman = 1898 líneas (preexistente) | Observación: es un artefacto de datos JSON, no un archivo de configuración |
| **o-5** | **Riesgo de proceso**: el gate de accesibilidad depende de auditar un estado por defecto | **REGLA**: el audit de contraste se corre en **estado de edición** (los controles deshabilitados están exentos). Ya dejó pasar un AA rojo dos veces |
| **o-6** | **N-4 (observación del re-verificador)**: la API no valida el **rango físico** de las PK (`ProvCod`/`JurCod` son `smallint`): una PK fuera de rango termina en **500** en vez de **400**. En la misma familia, una path variable no numérica (`PUT /api/v1/jurisdicciones/no-es-un-numero` → `MethodArgumentTypeMismatchException`) también termina en **500**. Ambas son **errores de cliente** que ensucian la señal de R-206. Fix propuesto: Bean Validation con `@Max`/`@Min` según el tipo físico de columna en los DTO de escritura + mapear `MethodArgumentTypeMismatchException` a `400` | **Backlog para J.3** (preexistente; fuera del alcance de T-415, porque relajar R-206 era requisito explícito) |

### M.3 Cierre de la re-verificación focalizada (2026-10-21)

> **Veredicto: APROBADO** — 0 bloqueantes, 0 mayores, 0 menores, 3 observaciones. **24/24 requisitos
> EARS** satisfechos. Los **8** hallazgos (H-1, H-2, m-1, m-2, m-3, o-1, N-1, N-3) quedan **CERRADOS**.
> Reporte: `C:\Users\walterg20\AppData\Local\Temp\opencode\atp-t504b-reverif2\REPORTE-REVERIF2-T504B.md`

Evidencia final: ratios medidos con **axe in-page en 2 estados** (botón `Guardar` **5,9448:1**,
`mat-label` **5,7656:1**) en las 6 ABM · **12** corridas de Lighthouse con `accessibility=100` y
`color-contrast` en verde · `105/105` unitarios + `32/32` IT · `134/134` tests de frontend · las
**6** ABM con alta 201 y baja 204 y la BD de vuelta al baseline (sin residuos) · Postman con 44
requests sin cambios · KB GeneXus sin escrituras.

**Aprendizaje del proceso (para los módulos 02-10)**: el gate de accesibilidad tiene que auditar
**estados**, no sólo pantallas. Dos rechazos consecutivos vinieron de medir sólo la carga inicial,
donde los controles deshabilitados están **exentos** del audit de contraste. La regla quedó escrita
en `ui-abm-localidades.md` §13.7.

**Pendientes para J.3**: (a) **o-6 / N-4** — la API no valida el rango físico de las PK: una PK
fuera del rango del `smallint` termina en **500** en vez de **400** (y en la misma familia, una path
variable no numérica); fix propuesto: `@Max`/`@Min` según el tipo físico + mapear
`MethodArgumentTypeMismatchException` a 400. (b) **N-2** — documentar el 409 de la fila huérfana en
la ayuda del ABM. (c) **T-413** — alinear `docs/design.md` con el código. (d) regularizar los 2
archivos >500 líneas preexistentes (`NucleoEscrituraControllerTest.java`, colección Postman).
- [ ] T-505b Pantalla **ABM** de `Productos` (2 niveles: raiz de 12 campos, 4 derivados en solo lectura, + grilla anidada de vigencias). *(spec: `openspec/features/01-nucleo-datos/ui-abm-productos.md` — **APPROVED** (G-SPEC) 2026-10-22; 18 requisitos `R-PROD-01…18`. **No** reutiliza el shell de formulario plano tal cual: la grilla del nivel 1 necesita un punto de extension (`<ng-content>`) en el panel del registro, y los importes requieren un tipo de campo `decimal` (los `numero` actuales son enteros >= 0). Las **7** ABM previas no cambian (R-PROD-15). D-19 (sin lookup de unidad de medida: el modulo no expone ese endpoint), D-20 (rotulo de `ProdFchVig` derivado, mismo patron que SA-04), D-21 (navegacion sobre la coleccion descargada) y D-22 (los 4 derivados `ProdIMax1..4` requieren una 2ª request a `importes-vigentes`, porque `ProductoDto` no los incluye). Cierra el hallazgo **N-4** del lado de la UI: `PRODUCTO.ProdCod` es `smallint` => la PK se limita a 0…32767 en el formulario (R-PROD-07). Frontend-only => G-POSTMAN N/A.)*
## N. Registro de hallazgos — Gate 2 de T-505a (2026-10-22)

> Veredicto: **APROBADO CON OBSERVACIONES** — 0 bloqueantes, 0 mayores, **3 menores**, 5 observaciones.
> **18/18 requisitos `R-FER-01…18` CUMPLEN**; los 8 criterios de aceptación de `ui-abm-feriados.md` §13
> se cumplen. Reporte: `C:\Users\walterg20\AppData\Local\Temp\opencode\atp-t505a-gate2\REPORTE-GATE2-T505A.md`

### N.1 Menores

| # | Hallazgo | Destino |
|---|---|---|
| **M-1** | `conHora` y `noEnviar` son flags **independientes** (`maestro-descriptor.ts:73,83`): un campo `conHora` **sin** `noEnviar` mandaría una clave `dd/MM/yyyy HH:mm:ss` que el DTO rechaza con 400 | **CERRADO** en T-505b: `noEnviar` se **deriva** de `conHora` en `normalizarCampo()` + guarda extra en `construirPayloadMaestro` (3 tests) |
| **M-2** | El botón **Get** valida "vacío", no calendario (`feriados-page.component.ts:115-121`): una fecha imposible (`2030-02-31`) sólo entra programáticamente y degrada a 400 sin crash | **CERRADO** en T-505b: `esFechaValida()` (formato + calendario, en UTC) ⇒ mensaje inline **sin request** (2 tests) |
| **M-3** | `maestro-abm.base.ts` = **494 líneas** (margen de 6) | **CERRADO** en T-505b: 6 funciones puras extraídas a `core/maestro-registro.ts` ⇒ **473 líneas** (< 480 y < 500) |

### N.2 Observaciones

| # | Observación | Disposición |
|---|---|---|
| **O-1** | El FAIL de `10d-NAV-NEXT` en la 1ª pasada del smoke | **Descartado como defecto del producto**: era un error del **script de test**. Verificado byte a byte: el valor observado en el DOM es idéntico en ambas corridas y la 2ª lo marca PASS con la aserción corregida (`tools/smoke-feriados.mjs:345-346`). El estado del shell es correcto (en el registro #2 de 1989 los 4 botones de navegación van habilitados) |
| **O-2** | El texto visible de `FerFecha` depende del **locale del navegador** | La spec §3.2 ya lo declara; no bloquea |
| **O-3** | La tabla `FERIADOS` tiene **1990** filas y la API devuelve **1989**: hay una fila histórica `1753-01-01` fuera de la ventana de consulta (1900-2100) | Dato legacy, **no** es residuo de QA. La BD quedó en 1990 con `2027-01-01 "Año Nuevo"` (usuario `MARIELA`) intacto |
| **O-4** | `NucleoEscrituraControllerTest.java` = 512 líneas (preexistente) | Observación: G4 aplica a archivos de **configuración**; un spec no lo es |
| **O-5** | La spec §3.1 y el javadoc dicen "truncada a 10 caracteres": el truncado es del **nombre** de la columna (`FerDescripcion`→`FerDescrip`); la real es `char(100)` | Corrección de redacción pendiente del coordinador |

### N.3 Revisión de código (sin hallazgos)

Medianoche consistente en los **6** caminos y **sin riesgo de zona horaria** (todo son operaciones de
string en `maestro-fecha.ts`; el backend serializa `LocalDateTime` sin offset) · el `pattern` de la
auditoría **no** invalida el formulario · `conHora` **no puede** colarse en un campo editable
(`readonlyEfectivo = readOnly() || conHora()`) · la URL con la PK de fecha se codifica bien
(`codificarPk` preserva los `:`) · el `Get` mal formado no es alcanzable por el usuario · **0**
ocurrencias de `any`/`@ts-ignore`/`console.log`/URL hardcodeada y ningún archivo de `frontend/src`
> 480 líneas.