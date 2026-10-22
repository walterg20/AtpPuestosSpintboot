# Constitución — Stack Tecnológico

> Reglas no negociables del stack objetivo. Toda spec y todo código deben cumplirlas.
> Última revisión: F1 (andamiaje del monorepo y migración a Thymeleaf).

## 1. Backend

| Área | Decisión | Obligatorio |
|---|---|---|
| Lenguaje | Java 17 (LTS) | Sí |
| Framework | Spring Boot 3.x | Sí |
| Presentación | **Spring MVC** (`@Controller` + Thymeleaf) y **Spring REST** (`@RestController`) | Sí |
| Motor de plantillas | **Thymeleaf** | Sí |
| JavaScript | **Puntual**, vanilla, sin framework ni build | Sí |
| Persistencia | Spring Data JPA / Hibernate | Sí |
| Validación | Jakarta Bean Validation (`@NotNull`, `@Size`, `@PastOrPresent`, …) | Sí |
| DTOs | Java `record` inmutables | Sí |
| Transacciones | `@Transactional` en capa `@Service` | Sí |
| Build | **Maven Wrapper** (`./mvnw`), multi-módulo | Sí |
| BD | SQL Server (driver `mssql-jdbc`) | Sí |
| API | REST/JSON, versionada bajo `/api/v1` | Sí |
| Seguridad | Spring Security, sesión HTTP + CSRF | Sí |

### Reglas de mapeo de datos (paridad)
1. Reutilizar el **esquema existente**; no generar DDL propio.
2. Nombres físicos exactos: `@Table(name = "...")`, `@Column(name = "...")`.
3. Los nombres físicos legacy son de **8 caracteres** (p. ej. `PROVINCI`, `Jurisdic`) y las columnas
   de **10** (p. ej. `FerDescripcion → FerDescrip`); el nombre lógico largo de la transacción
   **no** es el nombre de tabla.
4. Tipos: `NUMERIC` → `BigDecimal`, `DATE`/`DATETIME` → `LocalDateTime` (ojo: en la BD real
   `Feriados.FerFecha` y `Product1.ProdFchVig` son `datetime`, no `date`), `CHARACTER(n)` →
   `char(n)` fijo (rellena con espacios: `trim` al leer).
5. `hibernate.hbm2ddl.auto` en **ningún** modo contra la BD real. `ddl-auto=none` obligatorio.
6. Las **columnas huérfanas** (existen en la BD, no en la KB) se mapean con el default de
   aplicación que declare la spec. No se rediseñan ni se eliminan.

## 2. Capa de Vistas

| Área | Decisión | Obligatorio |
|---|---|---|
| Motor | Thymeleaf, prefijo `classpath:/templates/` | Sí |
| Layout | Fragmentos (`th:replace` / `th:insert`) en `templates/layout/` | Sí |
| Formularios | `th:object` + `th:field` sobre form objects de `web/mvc/**` | Sí |
| Estilos | CSS propio con **custom properties** (tokens), derivado de `ATPEvo2.css` | Sí |
| JavaScript | Vanilla, en `*/src/main/resources/static/js/` | Sí |
| Build de JS | **Ninguno**. Prohibido `npm`, bundler, transpilador | Sí |
| Externos | **Prohibidos** los CDN y recursos remotos en runtime | Sí |
| Accesibilidad | AA: contraste ≥ 4.5:1, foco visible, `label` asociado | Sí |

### Qué MAYEN necesitar JavaScript
Alta/baja de filas en grillas anidadas, `confirm()` de acciones destructivas, toggle de
contraseña, autofocus. **Nada más.** Si hace falta más, el problema es de diseño de servidor.

### Qué NO puede aparecer
Framework o librería JS, `node_modules`, paso de build de assets, CDN, styles o scripts
inline con lógica, y cualquier biblioteca que no esté versionada en el repo.

## 3. Base de Datos

| Área | Decisión | Obligatorio |
|---|---|---|
| Motor | SQL Server (existente) | Sí |
| Redesign inicial | **No** | Sí |
| Migraciones | **Ninguna** (ni Flyway, ni Liquibase, ni `schema.sql`/`data.sql`) | Sí |
| DDL | **Prohibido** en todo momento | Sí |
| Asignación de PK | La PK viaja en el body del `POST` (no hay `IDENTITY` en las tablas legacy) | Sí |
| Credenciales | Sólo por variables de entorno (`.env` no versionado) | Sí |

## 4. Pruebas

| Capa | Herramienta | Cuándo corre |
|---|---|---|
| Unitarios | JUnit 5 + Mockito | siempre (`mvn test`) |
| Vistas | `@WebMvcTest` + `MockMvc` (render, binding, redirección) | siempre |
| Integración de lectura | `*RepositoryIT` / `*IT` contra la BD real | sólo con `ATP_DB_IT=true` |
| Contrato REST | `MockMvc` contra `web/rest` | siempre |
| Paridad SQL | Comparar consultas Hibernate vs. *Navigation Report* de GeneXus | con evidencia |

> El sufijo `*IT` **no** lo levanta surefire en una corrida normal: hay que invocarlo explícito
> (`-Dtest=*IT`). En PowerShell, los argumentos de Maven con punto van **entrecomillados**.

## 5. Arquitectura y Despliegue

**Monolito modular.** Un solo artefacto desplegable (fat JAR), un solo proceso, una sola base.

| Regla | Detalle |
|---|---|
| Módulos | `atp-nucleo-compartido` (biblioteca) + `atp-puestos-camineros` (desplegable) |
| Dependencias | `web → service → repository → domain`; `domain` sin dependencias; `shared` transversal |
| Prohibido | atajo `web → repository`; `domain → web`; lógica de negocio en un controller |
| Empaquetado | `atp-puestos-camineros` depende del núcleo ⇒ 1 JAR con todo adentro |
| Sin microservicios | Ni Spring Cloud, ni Eureka, ni docker-compose multi-servicio, ni API gateway |

### Separación de las dos capas de presentación
| Capa | Paquete | Responde | Dueño |
|---|---|---|---|
| API REST | `*/web/rest/**` | JSON `{code, message}` bajo `/api/**` | `implementador-backend` |
| Vistas | `*/web/mvc/**` | HTML Thymeleaf | `implementador-vistas` |

Ambas consumen **los mismos servicios**. La lógica de negocio vive una sola vez, en `service/**`.

## 6. Entorno de Desarrollo

- **JDK 17 obligatorio**: `set JAVA_HOME=C:\Program Files\Java\jdk-17`. El `java` del PATH suele
  ser 1.8 y no compila este proyecto.
- Build aislado con el wrapper local (`./mvnw`); **no** modificar el entorno global.
- El entorno legacy GeneXus es **solo lectura**.
- Sin secretos en el repositorio; usar variables de entorno (ver `.env.example`).
- Archivos de configuración **< 500 líneas** (optimización de tokens).

## 7. Referencias
- Inventario y diagnóstico F0: `docs/analisis-ATPPuestos.md`
- Sistema de diseño: `docs/design.md`
- Proceso y puertas de calidad: `openspec/constitution/sdd-workflow.md`
- Convenciones de código: `openspec/constitution/conventions.md`
