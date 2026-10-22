# Plan — 01 Núcleo de Datos (piloto)

> **PORTADO (2026-10-22)** desde el repositorio `atppuestos2` (commit `974c3d6`). Acompaña a
> `spec.md` (mismo gate **G-SPEC**). Las rutas de código se actualizaron al monorepo:
> `backend/src/main/java/...` -> `atp-nucleo-compartido/src/main/java/...`.

> Descomposición técnica del módulo `01-nucleo-datos`: mapeo JPA exacto, contratos REST y
> desglose de implementación. Acompaña a `spec.md` (mismo gate **G-SPEC**).
> Estado: `APPROVED` (2026-10-16) — incluye ABM de escritura (P-01) y producto anidado (P-03).

## 1. Estrategia del piloto

Se implementa **backend/datos primero** (verificable sin UI) y **web después** (fase 2:
pantalla `ListadoLocalidades` + ABM de las maestras, **replicando el CSS/JS/HTML del origen
GeneXus** — decisión P-01), reutilizando el contrato REST ya estabilizado.

```text
KB (extraído)  →  Entidades JPA  →  Repositorios  →  Servicios  →  Controladores /api/v1
                                                                   └→ (fase 2) Angular ListadoLocalidades
```

Paquete base (a confirmar en el primer scaffold, `conventions.md` §3): `ar.com.atp.puestos`.
Estructura por **feature** (no por capa técnica) para acotar el contexto de cada módulo:

```text
backend/src/main/java/ar/com/atp/puestos/
├── nucleo/                      ← este módulo
│   ├── domain/                  ← entidades JPA
│   ├── repository/              ← Spring Data
│   ├── service/                 ← lógica de consulta + fórmulas
│   ├── web/                     ← @RestController
│   └── web/dto/                 ← records de entrada/salida
└── shared/                      ← utilidades comunes (módulo 01 → reusado por 02..10)
```

## 2. Mapeo físico ↔ JPA

> **Esquema físico validado (G-DATA, 2026-10-16)** — ver `spec.md` §4.5 (fuente de autoridad).
> Nombre de tabla = transacción truncada a 8 chars; **nombre de columna = nombre lógico truncado a
> 10 chars** (`FerDescripcion → FerDescrip`). Tipos Java según el tipo físico real (ASM-01…05 resueltos).

### 2.1 Tablas → entidades

| Transacción | Tabla física | Entidad JPA | PK |
|---|---|---|---|
| `Provincia` | `PROVINCI` | `Provincia` | `ProvCod` |
| `Localidades` | `Localida` | `Localidad` | `LocCod` |
| `Calles` | `CALLES` | `Calle` | `CallCod` |
| `CAMINOS` | `CAMINOS` | `Camino` | `CamID` |
| `Areas` | `Areas` | `Area` | `AreaId` |
| `Jurisdic` | `Jurisdic` | `Jurisdiccion` | `JurCod` |
| `Feriados` | `FERIADOS` | `Feriado` | `FerFecha` |
| `Productos` (raíz) | `PRODUCTO` | `Producto` | `ProdCod` |
| `Productos` (nivel 1) | `PRODUCT1` | `ProductoVigencia` | `ProdCod` + `ProdFchVig` |

### 2.2 Atributos → columnas y tipos Java (según tipo físico validado, §4.5)

| Entidad | Atributo lógico | Columna (física) | Tipo SQL real | Tipo Java |
|---|---|---|---|---|
| `Provincia` | `ProvCod` | `ProvCod` | `smallint` | `Integer` |
| | `ProvNombre` | `ProvNombre` | `varchar(40)` | `String` |
| | *(legacy)* | `ProvCodAfip` | `int NOT NULL` | `Integer` (default `0`) |
| `Localidad` | `LocCod` | `LocCod` | `int` | `Integer` |
| | `LocNom` | `LocNom` | `char(50)` | `String` (trim) |
| | `LocNomProv` | `LocNomProv` | `char(19)` | `String` (trim) |
| | `LocCodPost` | `LocCodPost` | `smallint` | `Integer` |
| | `ProvCod` (FK lógica) | `ProvCod` | `smallint` | `@ManyToOne Provincia` |
| | *(legacy)* | `LocCodAfip` | `int NOT NULL` | `Integer` (default `0`) |
| `Calle` | `CallCod` | `CallCod` | `int` | `Integer` |
| | `CallNom` | `CallNom` | `char(50)` | `String` (trim) |
| | *(legacy)* | `CallAlt1/2/3` | `char(50)` | `String` |
| `Camino` | `CamID` | `CamID` | `char(3)` | `String` |
| | `CanRuta` | `CanRuta` | `char(100)` | `String` (trim) |
| `Area` | `AreaId` | `AreaId` | **`decimal(10,0)`** | `BigDecimal` |
| | `AreaDesc` | `AreaDesc` | `char(50)` | `String` (trim) |
| | *(legacy)* | `AreaBaja` | `smallint` | `Integer` |
| | *(legacy)* | `AreaEmails` | `varchar(255) NOT NULL` | `String` (default `""`) |
| | *(legacy)* | `AreaAdjun` | `bit` | `Boolean` |
| | *(legacy)* | `AreaRespons` | `varchar(100)` | `String` |
| `Jurisdiccion` | `JurCod` | `JurCod` | `smallint` | `Integer` |
| | `JurNom` | `JurNom` | `char(20)` | `String` (trim) |
| `Feriado` | `FerFecha` | `FerFecha` | **`datetime`** | `LocalDateTime` |
| | `FerDescripcion` | **`FerDescrip`** | `char(100) NOT NULL` | `String` (trim) |
| | `FerFchReg` | `FerFchReg` | `datetime NOT NULL` | `LocalDateTime` (auditoría) |
| | `FerUsuario` | `FerUsuario` | `char(10) NOT NULL` | `String` (auditoría) |
| | `FerTipo` | `FerTipo` | `char(1)` | `String` |
| `Producto` | `ProdCod` | `ProdCod` | `smallint` | `Integer` |
| | `ProdDesc` | `ProdDesc` | `char(25) NOT NULL` | `String` (trim) |
| | `ProdTipCal` | `ProdTipCal` | `smallint NOT NULL` | `Integer` |
| | `TPPriID` | `TPPriID` | `smallint NOT NULL` | `Integer` |
| | `ProdIMax1..4` | (no persistido) | — | derivado (`BigDecimal`) |
| | `UMedNroId` | `UMedNroId` | `char(3)` | `String` |
| | `UMedNombre` | (no persistido) | — | derivado (`String`, `@Transient`) |
| | `ProdAcop` | `ProdAcop` | `char(1)` | `String` |
| | *(legacy)* | `ProdReppa` | `char(1)` | `String` |
| | `ProdAnula` | `ProdAnula` | `smallint NOT NULL` | `Integer` |
| `ProductoVigencia` | `ProdCod` | `ProdCod` | `smallint` | (parte de PK) |
| | `ProdFchVig` | `ProdFchVig` | **`datetime`** | `LocalDateTime` |
| | `ProdImpu1..4` | `ProdImpu1..4` | `money` (1/2 `NOT NULL`) | `BigDecimal` |
| | *(legacy)* | `ProdImpu5` | `money` | `BigDecimal` |
| | *(legacy)* | `ProdFeLog` | `datetime` | `LocalDateTime` |
| | *(legacy)* | `ProdUsu` | `char(10)` | `String` |

**Notas de mapeo:**

- `ProdIMax1..4` **no se persisten**: son fórmulas GeneXus (campo calculado). Se exponen como
  propiedades derivadas (`@Transient`/método del servicio) calculadas desde las vigencias (§2.4).
- `UMedNombre` es un atributo **derivado** en la KB (no existe columna física): `@Transient`.
- `Localidad.ProvCod` se mapea como `@ManyToOne` a `Provincia` (columna `ProvCod`), como
  **asociación lógica** — no hay FK física en la BD (spec §4.3); la integridad la garantiza la app.
- `Area.AreaId` es `decimal(10,0)` → `BigDecimal` (su rango excede `int`).
- `Feriado.FerFecha` y `ProductoVigencia.ProdFchVig` son `datetime` → `LocalDateTime` (ASM-03).
- Las longitudes de `@Size` en los DTO (§3.3) siguen la **longitud lógica** de la KB (regla de
  negocio); las columnas físicas son `char(n)` más anchas y se documentan en §2.3.

### 2.3 Columnas legacy y valores por defecto (G-DATA)

Las columnas siguientes existen en `Rentas` pero **no** en la KB. Se mapean todas (para lectura fiel
y porque varias son `NOT NULL` y romperían el `INSERT`), con tipo Java según §4.5:

- `Provincia.ProvCodAfip`, `Localidad.LocCodAfip` → `@Column(nullable = false)`; en el alta, si el
  cliente no los envía, el servicio aplica `0` (R-036).
- `Area.AreaEmails` (`NOT NULL`) → default `""`; `Area.AreaBaja`, `Area.AreaAdjun`, `Area.AreaRespons`
  → opcionales (R-036).
- `Calle.CallAlt1/2/3`, `Producto.ProdReppa`, `ProductoVigencia.ProdImpu5/ProdFeLog/ProdUsu`
  → opcionales, se persisten `null`/valor entrante.
- Ninguna PK es `IDENTITY`: el `POST` recibe la PK en el body (R-037).

**Anchuras físicas vs. lógicas (para longitud de columna, no de validación):** `LocNom`
`char(50)`/lógico 20, `LocNomProv` `char(19)`/lógico 15, `CallNom` `char(50)`/lógico 22.

### 2.4 Semántica de las fórmulas (`ProdIMax*`)

Equivalente SQL de `ProdIMax1` (idéntico para 2..4 cambiando `ProdImpu n`):

```sql
SELECT TOP 1 ProdImpu1
FROM Product1
WHERE ProdCod = :prodCod AND ProdFchVig <= CAST(GETDATE() AS date)
ORDER BY ProdFchVig DESC;
-- si no hay fila → 0
```

Implementación: método de servicio `importesVigentes(prodCod)` que consulta
`ProductoVigenciaRepository.findVigenteHastaHoy(prodCod, hoy)` y devuelve `0` (`BigDecimal.ZERO`)
si está vacío.

## 3. Contratos REST (OpenAPI 3.0, resumen)

Base: `/api/v1`. **Lectura + escritura (ABM, P-01)**. Errores uniformes `{ "code", "message" }`
(R-023, R-031, R-032). `productos` usa formato **anidado** con vigencias (P-03).

### 3.1 Lectura

| Método | Ruta | Descripción | Parámetros | Respuesta |
|---|---|---|---|---|
| GET | `/provincias` | Lista provincias | — | `ProvinciaDto[]` |
| GET | `/provincias/{provCod}` | Provincia por código | path `provCod` | `ProvinciaDto` \| 404 |
| GET | `/localidades` | Lista localidades | query `provCod?` | `LocalidadDto[]` |
| GET | `/localidades/{locCod}` | Localidad por código | path `locCod` | `LocalidadDto` \| 404 |
| GET | `/localidades/{locCod}/nombre` | **`Bus_DesLoc`** | path `locCod` | `{ "locNom": "..." }` (vacío si no existe) |
| GET | `/calles` | Lista calles | query `q?` (contiene) | `CalleDto[]` |
| GET | `/caminos` | Lista caminos | — | `CaminoDto[]` |
| GET | `/caminos/{camId}/ruta` | **`BuscarCamino`** | path `camId` | `{ "canRuta": "<trim>" }` |
| GET | `/areas` | Lista áreas | — | `AreaDto[]` |
| GET | `/jurisdicciones` | Lista jurisdicciones | — | `JurisdiccionDto[]` |
| GET | `/feriados` | Feriados por rango | query `desde`, `hasta` | `FeriadoDto[]` |
| GET | `/productos` | Productos con vigencias | query `anulados?` | `ProductoDto[]` |
| GET | `/productos/{prodCod}` | Producto + vigencias | path `prodCod` | `ProductoDto` \| 404 |
| GET | `/productos/{prodCod}/importes-vigentes` | Fórmulas `ProdIMax1..4` | path `prodCod` | `ImportesVigentesDto` |

### 3.2 Escritura (ABM — R-030…R-035)

| Método | Ruta | Descripción | Body | Respuesta |
|---|---|---|---|---|
| POST | `/provincias`, `/localidades`, `/calles`, `/caminos`, `/areas`, `/jurisdicciones`, `/feriados`, `/productos` | Alta | DTO de escritura (§3.3) | 201 + `Location` \| 400 \| 409 (PK existente) |
| PUT | `…/{id}` | Modificación completa | DTO de escritura | 200 \| 400 \| 404 |
| DELETE | `…/{id}` | Baja | — | 204 \| 404 |

Notas de contrato:

- `feriados` (POST): el body **no** incluye `FerFchReg`/`FerUsuario` — se rellenan en servidor (R-033).
- `productos` (POST/PUT): body `ProductoEscrituraDto` con `vigencias[]` anidadas; se persiste
  raíz + vigencias atómicamente y se valida unicidad de `ProdFchVig` por `ProdCod` (R-034).
- Tras cada escritura de vigencias, `GET …/importes-vigentes` refleja el recálculo (R-035, sin persistir).

### 3.3 DTOs (records)

Salida (lectura):

```java
record ProvinciaDto(Integer provCod, String provNombre) {}
record LocalidadDto(Integer locCod, String locNom, Integer locCodPost,
                    Integer provCod, String provNombre) {}
record CalleDto(Integer callCod, String callNom) {}
record CaminoDto(String camID, String canRuta) {}
record AreaDto(BigDecimal areaId, String areaDesc) {}
record JurisdiccionDto(Integer jurCod, String jurNom) {}
record FeriadoDto(LocalDateTime ferFecha, String ferDescripcion, String ferTipo,
                  LocalDateTime ferFchReg, String ferUsuario) {}
record ProductoVigenciaDto(LocalDateTime prodFchVig, BigDecimal prodImpu1,
                           BigDecimal prodImpu2, BigDecimal prodImpu3, BigDecimal prodImpu4) {}
record ProductoDto(Integer prodCod, String prodDesc, Integer prodTipCal, Integer tpPriID,
                   String uMedNroId, String uMedNombre, String prodAcop, Integer prodAnula,
                   List<ProductoVigenciaDto> vigencias) {}
record ImportesVigentesDto(Integer prodCod, BigDecimal prodIMax1, BigDecimal prodIMax2,
                           BigDecimal prodIMax3, BigDecimal prodIMax4) {}
```

Entrada (escritura — con `jakarta.validation`):

```java
record ProvinciaEscrituraDto(@NotNull Integer provCod, @NotBlank @Size(max = 40) String provNombre) {}
record LocalidadEscrituraDto(@NotNull Integer locCod, @NotBlank @Size(max = 20) String locNom,
                             @Size(max = 15) String locNomProv, Integer locCodPost,
                             @NotNull Integer provCod) {}
record CalleEscrituraDto(@NotNull Integer callCod, @NotBlank @Size(max = 22) String callNom) {}
record CaminoEscrituraDto(@NotBlank @Size(max = 3) String camID,
                          @Size(max = 100) String canRuta) {}
record AreaEscrituraDto(@NotNull BigDecimal areaId, @NotBlank @Size(max = 50) String areaDesc) {}
record JurisdiccionEscrituraDto(@NotNull Integer jurCod, @NotBlank @Size(max = 20) String jurNom) {}
record FeriadoEscrituraDto(@NotNull LocalDateTime ferFecha, @NotBlank @Size(max = 100) String ferDescripcion,
                           @Size(max = 1) String ferTipo) {}   // auditoría se rellena (R-033)
record ProductoVigenciaEscrituraDto(@NotNull LocalDateTime prodFchVig, @NotNull BigDecimal prodImpu1,
                                    @NotNull BigDecimal prodImpu2, @NotNull BigDecimal prodImpu3,
                                    @NotNull BigDecimal prodImpu4) {}
record ProductoEscrituraDto(@NotNull Integer prodCod, @NotBlank @Size(max = 25) String prodDesc,
                            Integer prodTipCal, Integer tpPriID, @Size(max = 3) String uMedNroId,
                            @Size(max = 40) String uMedNombre, @Size(max = 1) String prodAcop,
                            Integer prodAnula,
                            @Valid @NotNull List<ProductoVigenciaEscrituraDto> vigencias) {}
```

**Notas de escritura (G-DATA):**

- La **PK viaja en el body** del `POST` (no hay columnas `IDENTITY` en `Rentas`, R-037); su unicidad
  se valida antes de insertar (duplicado → `409`, R-031).
- Las columnas **legacy `NOT NULL`** (`Provincia.ProvCodAfip`, `Localidad.LocCodAfip`,
  `Area.AreaEmails`) **no** forman parte del DTO: el servicio aplica sus defaults `0`/`0`/`\"\"` en el
  alta (R-036, plan §2.3).
- `Feriado` recalcula auditoría en servidor (`FerFchReg` = ahora, `FerUsuario` = usuario) aunque el
  DTO reciba `ferFchReg`/`ferUsuario` por compatibilidad de lectura (R-033).
- Tipos físicos: `Areas.AreaId` es `BigDecimal`; `FerFecha`/`ProdFchVig` son `LocalDateTime` (§2.2).

## 4. Componentes a construir

| Capa | Artefacto | Requisitos |
|---|---|---|
| Entidades | `Provincia`, `Localidad`, `Calle`, `Camino`, `Area`, `Jurisdiccion`, `Feriado`, `Producto`, `ProductoVigencia` | R-001…R-006 |
| Repositorios | `ProvinciaRepository`, `LocalidadRepository`, `CalleRepository`, `CaminoRepository`, `AreaRepository`, `JurisdiccionRepository`, `FeriadoRepository`, `ProductoRepository`, `ProductoVigenciaRepository` | R-001…R-006 |
| Servicios | `NucleoConsultaService` (catálogos), `ProductoService` (vigencias + fórmulas + alta/modificación atómica), `BusquedaService` (`Bus_DesLoc`, `BuscarCamino`), `NucleoEscrituraService` (ABM genérico de maestras + `FeriadoAuditoria`) | R-005, R-010, R-011, R-030…R-035 |
| Controladores | `ProvinciaController`, `LocalidadController`, `CalleController`, `CaminoController`, `AreaController`, `JurisdiccionController`, `FeriadoController`, `ProductoController` (lectura + `POST`/`PUT`/`DELETE`) | R-020…R-023, R-030…R-035 |
| Config | `application.yml` con `${DB_URL}`/`${DB_USER}`/`${DB_PASSWORD}`, `spring.jpa.hibernate.ddl-auto=none` | R-204, R-205 |
| Frontend (fase 2) | `localidad-list` + pantallas **ABM** de las 8 maestras (componentes standalone + servicio HTTP + rutas), **replicando CSS/JS/HTML del origen GeneXus** (extraído vía MCP de los WebPanels) | R-020, R-021, R-030…R-035, P-01 |

## 5. Matriz de paridad (R-202)

| Legacy | Nuevo (EARS) | Prueba |
|---|---|---|
| Tabla `PROVINCI` (tx `Provincia`) | R-001, R-002, R-020 | `ProvinciaRepositoryIT` |
| Tabla `Localida` + FK `ProvCod` | R-003, R-020, R-021 | `LocalidadRepositoryIT` |
| Tabla `Calles` | R-001, R-020 | `CalleRepositoryIT` |
| Tabla `CAMINOS` | R-001, R-020 | `CaminoRepositoryIT` |
| Tabla `Areas` | R-001, R-020 | `AreaRepositoryIT` |
| Tabla `Jurisdic` | R-001, R-020 | `JurisdiccionRepositoryIT` |
| Tabla `Feriados` | R-006, R-021 | `FeriadoRepositoryIT` |
| Tabla `Producto` + `Product1` (nivel 1) | R-004, R-020 | `ProductoRepositoryIT` |
| Fórmulas `ProdIMax1..4` | R-005 | `ProductoServiceTest` (parametrizado) |
| Proc `Bus_DesLoc` (`For Each ... Where LocCod`) | R-010, R-022 | `BusquedaServiceTest` |
| Proc `BuscarCamino` (`Trim(CanRuta)`) | R-011, R-022 | `BusquedaServiceTest` |
| WebPanel `ListadoLocalidades` | R-020, R-021 | E2E Playwright (fase 2) |
| ABM escritura 8 maestras (`Commit`/`Delete` legacy) | R-030, R-031, R-032 | `*ControllerWriteIT` (MockMvc, 201/400/404/409) |
| `Feriados` auditoría (`FerFchReg`, `FerUsuario`) | R-033 | `FeriadoControllerWriteIT` |
| `Productos` nivel 0 + nivel 1 atómico | R-034, R-035 | `ProductoControllerWriteIT` |
| Columnas **legacy** sin contraparte en la KB (`ProvCodAfip`, `LocCodAfip`, `AreaEmails`, `CallAlt1/2/3`, `ProdReppa`, `ProdImpu5`, `ProdFeLog`, `ProdUsu`) | R-036 | `*RepositoryIT` (defaults `0`/`\"\"` en alta, lectura fiel) |
| PK **provista por el cliente** (sin `IDENTITY` en `Rentas`) | R-037 | `*ControllerWriteIT` (sin PK → 400; PK duplicada → 409) |
| Columna física truncada `FerDescripcion → FerDescrip` (ASM-01) | R-205 | `FeriadoRepositoryIT` (`@Column(name = \"FerDescrip\")`) |

### 5.1 Evidencia G-TEST

Puerta **G-TEST** verificada el **2026-10-16** con el Maven Wrapper sobre JDK 17:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\mvnw.cmd -B clean test
```

Resultado: **`Tests run: 80, Failures: 0, Errors: 0, Skipped: 0` → BUILD SUCCESS**.

| Suite (T-xxx) | Clase | Pruebas | Resultado |
|---|---|---|---|
| T-401 | `ProductoServiceTest` (parametrizado `ProdIMax1..4`) | 10 | OK |
| T-402 | `BusquedaServiceTest` (`Bus_DesLoc`, `BuscarCamino`) | 8 | OK |
| T-403 | `NucleoLecturaControllerTest` (`@WebMvcTest`, 7 recursos) | 20 | OK |
| T-406 | `NucleoEscrituraControllerTest` (`@WebMvcTest`, 8 recursos) | 40 | OK |
| — | `PuestosApplicationTests` + `HealthControllerTest` (pre-existentes) | 2 | OK |
| **Total** | | **80** | **0 fallos** |

**Pruebas de integración (T-404/T-407)**: por diseño **no** las ejecuta surefire (sufijo `*IT`); en esta corrida se **compilaron** con el resto (`test-compile` OK) pero quedaron **omitidas** por el gate `@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")`. Para ejecutarlas contra `Rentas` (perfil `dev`, `ddl-auto=none`, rollback transaccional) hay que exportar las credenciales de BD y `ATP_DB_IT=true`:

```powershell
$env:ATP_DB_IT = "true"
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

**Corrida T-404 (lectura) contra `Rentas` — 2026-10-16**: cargando las credenciales de `.env` (sin pisar los defaults con valores vacíos) y `ATP_DB_IT=true` se ejecutaron las 8 clases `*RepositoryIT` (`-Dtest=*RepositoryIT`). Resultado: **`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` → BUILD SUCCESS**, es decir 16/16 en verde sobre datos reales (paridad de lectura, G-DATA). T-407 (escritura con rollback) **no** se ejecutó en esta pasada.

Ajustes necesarios para alcanzar esa paridad:

- **Nombres de columna legacy (R-205)**: sin estrategia explícita, Hibernate derivaba `snake_case` y producía 14 errores `SQL-207` (*Invalid column name*). Se fijaron `PhysicalNamingStrategyStandardImpl` e `ImplicitNamingStrategyJpaCompliantImpl` en `application.yml` para que respete los nombres físicos declarados en cada `@Column`.
- **`RTRIM` (SQL) ≡ `stripTrailing()` (Java)**, no `trim()`: el legacy tiene valores con espacio inicial (p. ej. `" DODERO"`).
- **Ordenamiento (collation de Rentas)**: ni `String.CASE_INSENSITIVE_ORDER` ni `java.text.Collator` la reproducen. La collation es case/accent-insensitive, pliega la eñe a `N` (`ÑANDUBAY` ordena antes que `NAPALPI`) y ordena espacio/signos antes que dígitos y letras (`CAMPO DE LA CHOZA` antes que `CAMPO DECASO`). Se implementó `ar.com.atp.puestos.support.SqlCollation.spanish()` (clave de orden primaria) y se usa en `LocalidadRepositoryIT` y `ProductoRepositoryIT`.
- **`FeriadoRepositoryIT`**: `hasSameSizeAs(findAll())` no aplica (hay fechas nulas o fuera de rango); se reemplazó por un conteo con la misma semántica del predicado.

## 6. Riesgos y mitigaciones

| Riesgo | Estado | Mitigación / resultado |
|---|---|---|
| Nombre físico de columna desconocido (ASM-01) | **Resuelto** (G-DATA) | Validado: columna = nombre lógico truncado a **10 chars** (`FerDescripcion → FerDescrip`). Mapear con `@Column(name=...)` explícito. |
| Mapeo de tipos NUMERIC/DATE (ASM-02…05) | **Resuelto** (G-DATA) | ASM-02 parcialmente falso (`Area.AreaId decimal(10,0)` → `BigDecimal`); ASM-03 falso (`datetime`); ASM-04 falso (`char(n)`); ASM-05 confirmado. Ver `spec.md` §4.5. |
| Columnas legacy/no-KB y `NOT NULL` sin `DEFAULT` | **Resuelto** (G-DATA) | Mapear todas las columnas (R-036); aplicar defaults `0`/`\"\"` en el alta (§2.3). |
| PK sin `IDENTITY` | **Resuelto** (G-DATA) | PK explícita en el body del `POST` (R-037); validar unicidad → `409`. |
| Fórmulas `max(...)` malinterpretadas | Abierto | Semántica documentada (§2.4) con pruebas de borde; revisar con el responsable. |
| `Productos` de dos niveles | Abierto | Carga explícita de vigencias + orden por `ProdFchVig`; prueba con N vigencias. |
| Colisión de nombre `Feriados` (transacción vs. carpeta) | Abierto | Usar `type: 'Transaction'` explícito en las consultas MCP. |

## 7. Secuencia de ejecución

1. [x] Aprobar `spec.md`, `plan.md`, `tasks.md` (**G-SPEC**) — *2026-10-16*.
2. [x] Validar esquema contra `Rentas` (**G-DATA**) — *2026-10-16*: ASM-01…05 resueltos, columnas legacy y PK sin `IDENTITY` documentadas (`spec.md` §4.4/§4.5).
3. [x] Scaffold `backend/` (Spring Boot, Maven Wrapper) + config por env. *(2026-10-16)*
4. [x] Implementar entidades + repositorios (T-101…T-109). *(2026-10-16)*
5. [x] Implementar servicios de consulta/fórmulas/búsquedas (T-201…T-204). *(2026-10-16)*
6. [x] Implementar controladores REST lectura (T-301…T-309) y **escritura ABM (T-310…T-318)**. *(2026-10-16)*
7. [x] Pruebas (T-401…T-406) y matriz de paridad. *(2026-10-16, G-TEST: 80/80; T-404: 16/16 IT de lectura en verde — ver §5.1)*
8. [ ] (Fase 2) Scaffold `frontend/` + `ListadoLocalidades` + **pantallas ABM con diseño del origen** (T-501…T-506).