# ATPPuestos — Replatforming GeneXus 18 → Java 17 / Spring Boot + Thymeleaf

Reimplementación del sistema legacy **GeneXus 18** (`ATPPuestos`) como **monolito modular** en
**Java 17 + Spring Boot 3.x**, con capa de presentación **Spring MVC + Thymeleaf** y **JavaScript
puntual**. La base de datos es la existente (SQL Server `Rentas`): se mapea, no se rediseña.

El arnés de control está en [`agents.md`](./agents.md). Las especificaciones están en
[`openspec/`](./openspec/) (sintaxis EARS, norma ISO/IEC 25030).

## Estado de las fases

| Fase | Descripción | Estado |
|---|---|---|
| **F0** | Diagnóstico e inventario de la KB | ✅ [`docs/analisis-ATPPuestos.md`](./docs/analisis-ATPPuestos.md) |
| **F1** | Constitución del monorepo y del arnés | ✅ (andamiaje + arnés reescrito) |
| **F1.1** | Alcance migrable in/out | ✅ [`openspec/features/00-alcance/`](./openspec/features/00-alcance/) |
| **F2** | Spec `02-abm-productos` | ⏳ pendiente |
| **F3** | Spec `03-login` | ⏳ pendiente |
| **F4+** | Ciclo SDD por módulo | ⏳ pendiente de aprobación del alcance |

## Estructura

```text
.
├── agents.md                     # Arnés de control principal
├── pom.xml                       # Parent POM del monorepo + Maven Wrapper
├── atp-nucleo-compartido/        # Biblioteca: catálogos, personas, seguridad base, API REST
├── atp-puestos-camineros/        # Artefacto desplegable: login + dominio + vistas Thymeleaf
├── openspec/
│   ├── config.yaml               # Contexto del proyecto para OpenSpec
│   ├── constitution/             # stack.md · conventions.md · sdd-workflow.md
│   └── features/
│       ├── 00-alcance/           # {spec,plan,tasks}.md
│       ├── 01-nucleo-datos/      # {spec,plan,tasks}.md — núcleo ya implementado (portado)
│       ├── 02-abm-productos/     # {spec,plan,tasks}.md — pendiente
│       └── 03-login/             # {spec,plan,tasks}.md — pendiente
├── docs/
│   ├── analisis-ATPPuestos.md    # Diagnóstico F0 + G-DATA + mapa del portfolio
│   ├── design.md                 # Sistema de diseño (derivado de ATPEvo2.css)
│   └── postman/                  # Colección Postman de la API (G-POSTMAN, obligatoria)
├── .opencode/                    # Subagentes y comandos del arnés
└── .engram/                      # Memoria persistente (portable, trackeable)
```

## Arquitectura

Monolito modular: **un solo artefacto desplegable**, con capas lógicas separadas y una regla de
dependencia estricta.

```text
web (rest | mvc)  →  service  →  repository  →  domain
        ↑                ↑
   shared (transversal)
```

| Capa | Qué es | Quién la escribe |
|---|---|---|
| `web/rest` | Contrato JSON `/api/v1` | `implementador-backend` |
| `web/mvc` | Páginas Thymeleaf | `implementador-vistas` |
| `service` | Reglas de negocio (una sola vez) | `implementador-backend` |
| `repository` / `domain` | Acceso a datos y entidades | `implementador-backend` |
| `templates` / `static` | Vistas, tokens CSS, JS puntual | `implementador-vistas` |

**Dos canales de respuesta**: `/api/**` devuelve JSON con el cuerpo uniforme `{code, message}`;
el resto de las rutas devuelven HTML. El `GlobalExceptionHandler` negocia según el prefijo de ruta.

## Reglas no negociables

1. **La KB GeneXus legacy es SOLO LECTURA.** Prohibido compilar/modificar/eliminar objetos.
2. **Spec-Driven Development**: sin spec aprobada en `openspec/features/<modulo>/`, no hay código.
3. **Paridad de BD**: reutilizar el esquema SQL Server preservando nombres físicos exactos.
   **Sin DDL, sin migraciones, sin rediseño.**
4. **Monolito modular**: regla de dependencia `web → service → repository → domain`.
5. **JavaScript puntual**: vanilla, sin framework, sin bundler, sin CDN, sin `npm` en el build.
6. **Sin secretos en el repo**; sólo variables de entorno (ver `.env.example`).
7. Archivos de configuración **< 500 líneas**.

Detalle completo en [`openspec/constitution/`](./openspec/constitution/).

## Entorno de datos

- KB: `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13) — **solo lectura**.
- BD de metadatos KB: `GX_KB_ATPPuestos`.
- BD de la aplicación: `Rentas` @ `devmssqlatp.ecom.com.ar` (SQL Server) — credenciales en `.env`.

## Puesta en marcha

```bash
# JDK 17 es obligatorio: el java del PATH suele ser 1.8
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Copiar .env.example a .env y completar DB_USER / DB_PASSWORD
copy .env.example .env

# Build + tests
.\mvnw.cmd -B clean test

# Integración contra la BD real (gated)
set ATP_DB_IT=true
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"

# Levantar la app
.\mvnw.cmd -pl atp-puestos-camineros spring-boot:run
```

> En PowerShell los argumentos de Maven con punto van **entrecomillados**: sin comillas,
> `-Dsurefire.x` se parte y Maven lo interpreta como una fase del ciclo de vida.

---

_Proyecto en F1 — 2026. La KB original se conserva intacta como fuente de verdad._
