# ArnÃ©s de Control Principal â€” Replatforming GeneXus a Java/Spring Boot + Thymeleaf

## MisiÃ³n
Orquestar y supervisar la migraciÃ³n del sistema legacy de GeneXus 18 (KB `ATPPuestos`) hacia un
**monolito modular** en Java 17 / Spring Boot 3.x, con capa de presentaciÃ³n **Spring MVC +
Thymeleaf** y **JavaScript puntual**.

El proyecto es un **monorepo Maven**. Hoy contiene **sÃ³lo el alcance de ATPPuestos**; los portales
futuros (`Portal GestiÃ³n ATP`, `Portal Contribuyente`) y los mÃ³dulos de negocio compartidos
(`Planes de Pago`) quedan **fuera de alcance** y registrados en `openspec/features/00-alcance/`.

## Stack TecnolÃ³gico Target

| Capa | TecnologÃ­a | Obligatorio |
|---|---|---|
| Lenguaje | Java 17 (LTS) | SÃ­ |
| Framework | Spring Boot 3.x | SÃ­ |
| PresentaciÃ³n | **Spring MVC + Thymeleaf** | SÃ­ |
| JavaScript | **Puntual**, vanilla, sin framework ni build | SÃ­ |
| Persistencia | Spring Data JPA / Hibernate (`ddl-auto=none`) | SÃ­ |
| ValidaciÃ³n | Jakarta Bean Validation | SÃ­ |
| Base de datos | **SQL Server existente** â€” sin rediseÃ±o inicial | SÃ­ |
| Build | Maven Wrapper (`./mvnw`) | SÃ­ |
| Pruebas | JUnit 5 + Mockito + MockMvc + IT contra BD real | SÃ­ |
| Despliegue | **Monolito modular** â€” 1 JAR, 1 despliegue | SÃ­ |

- **Especificaciones**: OpenSpec bajo norma ISO/IEC 25030 y sintaxis EARS.
- **Memoria Persistente**: Engram (`.engram/` + `mem_save` / `mem_session_summary`).

## Estructura del Monorepo

```text
/
â”œâ”€â”€ pom.xml                     â† parent POM (packaging=pom) + Maven Wrapper
â”œâ”€â”€ src/ (unificado)      â† catÃ¡logo + personas + base de seguridad + API REST
â”‚   â””â”€â”€ ar.com.atp.puestos.nucleo.**  Â· ar.com.atp.puestos.shared.**
â”œâ”€â”€ src/ (unificado)      â† mÃ³dulo Puestos Camineros (login + dominio) â€” ARTEFACTO DESPLEGABLE
â”‚   â””â”€â”€ ar.com.atp.puestos.puestos.**  Â· ar.com.atp.puestos.PuestosApplication
â”œâ”€â”€ openspec/
â”‚   â”œâ”€â”€ config.yaml             â† contexto del proyecto para la IA
â”‚   â”œâ”€â”€ constitution/            â† reglas del stack, convenciones y flujo SDD
â”‚   â””â”€â”€ features/
â”‚       â”œâ”€â”€ 00-alcance/         â† alcance migrable in/out (incluye lo OUT del monorepo)
â”‚       â”œâ”€â”€ 01-nucleo-datos/    â† nÃºcleo de datos ya implementado (portado de atppuestos2)
â”‚       â”œâ”€â”€ 02-abm-productos/   â† ABM de productos en Thymeleaf (spec,plan,tasks).md
â”‚       â””â”€â”€ 03-login/           â† login completo de Puestos Camineros. (spec,plan,tasks).md
â”œâ”€â”€ docs/                       â† anÃ¡lisis KB, sistema de diseÃ±o, colecciÃ³n Postman
â”‚   â””â”€â”€ postman/                â† ColecciÃ³n Postman de la API (G-POSTMAN, obligatoria)
â”œâ”€â”€ .opencode/                  â† arnÃ©s de subagentes y comandos
â””â”€â”€ .engram/                    â† memoria persistente del agente
```

## Reglas de Control y GuardarraÃ­les Estrictos

1. **Entorno Legacy Lectura Exclusiva**: queda **PROHIBIDO** modificar, editar, compilar o eliminar
   cualquier objeto o tabla en la Base de Conocimiento (KB) original de GeneXus. Nunca ejecutar
   *Build All*, *Regenerate* ni *Reorganize* sobre la KB.
2. **Spec-Driven Development (SDD)**: ningÃºn subagente escribirÃ¡ cÃ³digo de aplicaciÃ³n sin una
   especificaciÃ³n previa aprobada en `openspec/features/<modulo>/`.
3. **Paridad de Base de Datos**: reutilizar la estructura relacional de SQL Server existente
   mapeando tablas y columnas exactas con `@Table(name = "...")` y `@Column(name = "...")`.
   **Sin rediseÃ±o inicial**: nada de DDL, ni `schema.sql`/`data.sql`, ni Flyway/Liquibase, ni
   migraciones. Las columnas huÃ©rfanas del legacy (las que existen en la BD y no en la KB) se
   **mapean**, no se rediseÃ±an.
4. **LÃ­mite de Archivos**: mantener todos los archivos de configuraciÃ³n por debajo de las **500
   lÃ­neas** para optimizar el consumo de tokens.
5. **Seguridad**: no incluir contraseÃ±as, tokens o secretos en texto plano. Todo por variables de
   entorno (`.env` no se versiona; `.env.example` documenta, no resuelve).
6. **ColecciÃ³n Postman Sincronizada (G-POSTMAN)**: al trabajar en el backend, TODO endpoint
   agregado, modificado o eliminado DEBE reflejarse en
   `docs/postman/ATPPuestos-API.postman_collection.json` en el mismo commit. Es una regla
   obligatoria de mantenimiento. Ver `docs/postman/README.md`.
7. **Monolito Modular**: 1 solo artefacto desplegable. Regla de dependencia
   `web â†’ service â†’ repository â†’ domain`; `domain` no depende de nada; `shared` es transversal.
   **Prohibido** el atajo `web â†’ repository` y prohibido `domain â†’ web`.
8. **JavaScript Puntual**: el JavaScript es **vanilla**, vive en `*/src/main/resources/static/js/`,
   y existe **sÃ³lo** para lo que Thymeleaf no resuelve solo (alta/baja de filas en grillas
   anidadas, confirmaciones, toggle de contraseÃ±a, autofocus). **Prohibido**: frameworks,
   bundlers, `npm` en el build de Maven, y CDNs o recursos externos en runtime.
9. **UI Thymeleaf sin duplicar la lÃ³gica de negocio**: las reglas viven en `service/**`; tanto
   `web/rest` como `web/mvc` son adaptadores sin reglas propias.

## MÃ³dulos de Spec Actuales

| MÃ³dulo | Alcance | Nota |
|---|---|---|
| `00-alcance` | Alcance migrable in/out | Registra tambiÃ©n lo OUT del monorepo con su mapeo |
| `01-nucleo-datos` | NÃºcleo de datos ya implementado (portado) | Es la spec que originÃ³ el cÃ³digo de `src/ (unificado)` |
| `02-abm-productos` | ABM de `PRODUCTO` + `PRODUCT1` (vigencias) en Thymeleaf | 2 niveles, 12 campos raÃ­z + grilla anidada |
| `03-login` | Login completo de Puestos Camineros | Cifrado legacy, cÃ³digos 0-6, contexto de sesiÃ³n |

## Entorno de Datos

- KB: `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13) â€” **solo lectura**.
- BD de metadatos KB: `GX_KB_ATPPuestos`.
- BD de la aplicaciÃ³n: `Rentas` @ `devmssqlatp.ecom.com.ar` (SQL Server) â€” credenciales por `.env`.
- DiagnÃ³stico completo de la KB: `docs/analisis-ATPPuestos.md`.

## Puesta en Marcha

```bash
# Build + tests (JDK 17; el java del PATH suele ser 1.8)
set JAVA_HOME=C:\Program Files\Java\jdk-17
./mvnw clean test

# IntegraciÃ³n contra la BD real (gated)
set ATP_DB_IT=true
./mvnw test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"

# Ejecutar la app
./mvnw spring-boot:run
```


