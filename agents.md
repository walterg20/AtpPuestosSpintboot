# Arnés de Control Principal — Replatforming GeneXus a Java/Spring Boot + Thymeleaf

## Misión
Orquestar y supervisar la migración del sistema legacy de GeneXus 18 (KB `ATPPuestos`) hacia un
**monolito modular** en Java 17 / Spring Boot 3.x, con capa de presentación **Spring MVC +
Thymeleaf** y **JavaScript puntual**.

El proyecto es un **monorepo Maven**. Hoy contiene **sólo el alcance de ATPPuestos**; los portales
futuros (`Portal Gestión ATP`, `Portal Contribuyente`) y los módulos de negocio compartidos
(`Planes de Pago`) quedan **fuera de alcance** y registrados en `openspec/features/00-alcance/`.

## Stack Tecnológico Target

| Capa | Tecnología | Obligatorio |
|---|---|---|
| Lenguaje | Java 17 (LTS) | Sí |
| Framework | Spring Boot 3.x | Sí |
| Presentación | **Spring MVC + Thymeleaf** | Sí |
| JavaScript | **Puntual**, vanilla, sin framework ni build | Sí |
| Persistencia | Spring Data JPA / Hibernate (`ddl-auto=none`) | Sí |
| Validación | Jakarta Bean Validation | Sí |
| Base de datos | **SQL Server existente** — sin rediseño inicial | Sí |
| Build | Maven Wrapper (`./mvnw`) | Sí |
| Pruebas | JUnit 5 + Mockito + MockMvc + IT contra BD real | Sí |
| Despliegue | **Monolito modular** — 1 JAR, 1 despliegue | Sí |

- **Especificaciones**: OpenSpec bajo norma ISO/IEC 25030 y sintaxis EARS.
- **Memoria Persistente**: Engram (`.engram/` + `mem_save` / `mem_session_summary`).

## Estructura del Monorepo

```text
/
├── pom.xml                     ← parent POM (packaging=pom) + Maven Wrapper
├── atp-nucleo-compartido/      ← catálogo + personas + base de seguridad + API REST
│   └── ar.com.atp.puestos.nucleo.**  · ar.com.atp.puestos.shared.**
├── atp-puestos-camineros/      ← módulo Puestos Camineros (login + dominio) — ARTEFACTO DESPLEGABLE
│   └── ar.com.atp.puestos.puestos.**  · ar.com.atp.puestos.PuestosApplication
├── openspec/
│   ├── config.yaml             ← contexto del proyecto para la IA
│   ├── constitution/            ← reglas del stack, convenciones y flujo SDD
│   └── features/
│       ├── 00-alcance/         ← alcance migrable in/out (incluye lo OUT del monorepo)
│       ├── 01-nucleo-datos/    ← núcleo de datos ya implementado (portado de atppuestos2)
│       ├── 02-abm-productos/   ← ABM de productos en Thymeleaf (spec,plan,tasks).md
│       └── 03-login/           ← login completo de Puestos Camineros. (spec,plan,tasks).md
├── docs/                       ← análisis KB, sistema de diseño, colección Postman
│   └── postman/                ← Colección Postman de la API (G-POSTMAN, obligatoria)
├── .opencode/                  ← arnés de subagentes y comandos
└── .engram/                    ← memoria persistente del agente
```

## Reglas de Control y Guardarraíles Estrictos

1. **Entorno Legacy Lectura Exclusiva**: queda **PROHIBIDO** modificar, editar, compilar o eliminar
   cualquier objeto o tabla en la Base de Conocimiento (KB) original de GeneXus. Nunca ejecutar
   *Build All*, *Regenerate* ni *Reorganize* sobre la KB.
2. **Spec-Driven Development (SDD)**: ningún subagente escribirá código de aplicación sin una
   especificación previa aprobada en `openspec/features/<modulo>/`.
3. **Paridad de Base de Datos**: reutilizar la estructura relacional de SQL Server existente
   mapeando tablas y columnas exactas con `@Table(name = "...")` y `@Column(name = "...")`.
   **Sin rediseño inicial**: nada de DDL, ni `schema.sql`/`data.sql`, ni Flyway/Liquibase, ni
   migraciones. Las columnas huérfanas del legacy (las que existen en la BD y no en la KB) se
   **mapean**, no se rediseñan.
4. **Límite de Archivos**: mantener todos los archivos de configuración por debajo de las **500
   líneas** para optimizar el consumo de tokens.
5. **Seguridad**: no incluir contraseñas, tokens o secretos en texto plano. Todo por variables de
   entorno (`.env` no se versiona; `.env.example` documenta, no resuelve).
6. **Colección Postman Sincronizada (G-POSTMAN)**: al trabajar en el backend, TODO endpoint
   agregado, modificado o eliminado DEBE reflejarse en
   `docs/postman/ATPPuestos-API.postman_collection.json` en el mismo commit. Es una regla
   obligatoria de mantenimiento. Ver `docs/postman/README.md`.
7. **Monolito Modular**: 1 solo artefacto desplegable. Regla de dependencia
   `web → service → repository → domain`; `domain` no depende de nada; `shared` es transversal.
   **Prohibido** el atajo `web → repository` y prohibido `domain → web`.
8. **JavaScript Puntual**: el JavaScript es **vanilla**, vive en `*/src/main/resources/static/js/`,
   y existe **sólo** para lo que Thymeleaf no resuelve solo (alta/baja de filas en grillas
   anidadas, confirmaciones, toggle de contraseña, autofocus). **Prohibido**: frameworks,
   bundlers, `npm` en el build de Maven, y CDNs o recursos externos en runtime.
9. **UI Thymeleaf sin duplicar la lógica de negocio**: las reglas viven en `service/**`; tanto
   `web/rest` como `web/mvc` son adaptadores sin reglas propias.

## Módulos de Spec Actuales

| Módulo | Alcance | Nota |
|---|---|---|
| `00-alcance` | Alcance migrable in/out | Registra también lo OUT del monorepo con su mapeo |
| `01-nucleo-datos` | Núcleo de datos ya implementado (portado) | Es la spec que originó el código de `atp-nucleo-compartido/` |
| `02-abm-productos` | ABM de `PRODUCTO` + `PRODUCT1` (vigencias) en Thymeleaf | 2 niveles, 12 campos raíz + grilla anidada |
| `03-login` | Login completo de Puestos Camineros | Cifrado legacy, códigos 0-6, contexto de sesión |

## Entorno de Datos

- KB: `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13) — **solo lectura**.
- BD de metadatos KB: `GX_KB_ATPPuestos`.
- BD de la aplicación: `Rentas` @ `devmssqlatp.ecom.com.ar` (SQL Server) — credenciales por `.env`.
- Diagnóstico completo de la KB: `docs/analisis-ATPPuestos.md`.

## Puesta en Marcha

```bash
# Build + tests (JDK 17; el java del PATH suele ser 1.8)
set JAVA_HOME=C:\Program Files\Java\jdk-17
./mvnw clean test

# Integración contra la BD real (gated)
set ATP_DB_IT=true
./mvnw test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"

# Ejecutar la app
./mvnw -pl atp-puestos-camineros spring-boot:run
```

## Diseño Stitch

Refs: openspec/features/99-stitch-design/design.md, tokens.json, screens/*.html, openspec/features/00-alcance/ui-stitch-reference.md.

Implementacion: templates/layout/base-stitch.html (head, cabecera, sesion, navegacion, pie); static/css/stitch-tokens.css (--stitch-*); base.html queda como fallback.
