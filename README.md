# ATPPuestos â€” Replatforming GeneXus 18 â†’ Java 17 / Spring Boot + Thymeleaf

ReimplementaciÃ³n del sistema legacy **GeneXus 18** (`ATPPuestos`) como **monolito modular** en
**Java 17 + Spring Boot 3.x**, con capa de presentaciÃ³n **Spring MVC + Thymeleaf** y **JavaScript
puntual**. La base de datos es la existente (SQL Server `Rentas`): se mapea, no se rediseÃ±a.

El arnÃ©s de control estÃ¡ en [`agents.md`](./agents.md). Las especificaciones estÃ¡n en
[`openspec/`](./openspec/) (sintaxis EARS, norma ISO/IEC 25030).

## Estado de las fases

| Fase | DescripciÃ³n | Estado |
|---|---|---|
| **F0** | DiagnÃ³stico e inventario de la KB | âœ… [`docs/analisis-ATPPuestos.md`](./docs/analisis-ATPPuestos.md) |
| **F1** | ConstituciÃ³n del monorepo y del arnÃ©s | âœ… (andamiaje + arnÃ©s reescrito) |
| **F1.1** | Alcance migrable in/out | âœ… [`openspec/features/00-alcance/`](./openspec/features/00-alcance/) |
| **F2** | Spec `02-abm-productos` | â³ pendiente |
| **F3** | Spec `03-login` | â³ pendiente |
| **F4+** | Ciclo SDD por mÃ³dulo | â³ pendiente de aprobaciÃ³n del alcance |

## Estructura

```text
.
â”œâ”€â”€ agents.md                     # ArnÃ©s de control principal
â”œâ”€â”€ pom.xml                       # Parent POM del monorepo + Maven Wrapper
â”œâ”€â”€ atp-nucleo-compartido/        # Biblioteca: catÃ¡logos, personas, seguridad base, API REST
├── src/                        # Código de la aplicación (dominio, web, recursos)
â”œâ”€â”€ openspec/
â”‚   â”œâ”€â”€ config.yaml               # Contexto del proyecto para OpenSpec
â”‚   â”œâ”€â”€ constitution/             # stack.md Â· conventions.md Â· sdd-workflow.md
â”‚   â””â”€â”€ features/
â”‚       â”œâ”€â”€ 00-alcance/           # {spec,plan,tasks}.md
â”‚       â”œâ”€â”€ 01-nucleo-datos/      # {spec,plan,tasks}.md â€” nÃºcleo ya implementado (portado)
â”‚       â”œâ”€â”€ 02-abm-productos/     # {spec,plan,tasks}.md â€” pendiente
â”‚       â””â”€â”€ 03-login/             # {spec,plan,tasks}.md â€” pendiente
â”œâ”€â”€ docs/
â”‚   â”œâ”€â”€ analisis-ATPPuestos.md    # DiagnÃ³stico F0 + G-DATA + mapa del portfolio
â”‚   â”œâ”€â”€ design.md                 # Sistema de diseÃ±o (derivado de ATPEvo2.css)
â”‚   â””â”€â”€ postman/                  # ColecciÃ³n Postman de la API (G-POSTMAN, obligatoria)
â”œâ”€â”€ .opencode/                    # Subagentes y comandos del arnÃ©s
â””â”€â”€ .engram/                      # Memoria persistente (portable, trackeable)
```

## Arquitectura

Monolito modular: **un solo artefacto desplegable**, con capas lÃ³gicas separadas y una regla de
dependencia estricta.

```text
web (rest | mvc)  â†’  service  â†’  repository  â†’  domain
        â†‘                â†‘
   shared (transversal)
```

| Capa | QuÃ© es | QuiÃ©n la escribe |
|---|---|---|
| `web/rest` | Contrato JSON `/api/v1` | `implementador-backend` |
| `web/mvc` | PÃ¡ginas Thymeleaf | `implementador-vistas` |
| `service` | Reglas de negocio (una sola vez) | `implementador-backend` |
| `repository` / `domain` | Acceso a datos y entidades | `implementador-backend` |
| `templates` / `static` | Vistas, tokens CSS, JS puntual | `implementador-vistas` |

**Dos canales de respuesta**: `/api/**` devuelve JSON con el cuerpo uniforme `{code, message}`;
el resto de las rutas devuelven HTML. El `GlobalExceptionHandler` negocia segÃºn el prefijo de ruta.

## Reglas no negociables

1. **La KB GeneXus legacy es SOLO LECTURA.** Prohibido compilar/modificar/eliminar objetos.
2. **Spec-Driven Development**: sin spec aprobada en `openspec/features/<modulo>/`, no hay cÃ³digo.
3. **Paridad de BD**: reutilizar el esquema SQL Server preservando nombres fÃ­sicos exactos.
   **Sin DDL, sin migraciones, sin rediseÃ±o.**
4. **Monolito modular**: regla de dependencia `web â†’ service â†’ repository â†’ domain`.
5. **JavaScript puntual**: vanilla, sin framework, sin bundler, sin CDN, sin `npm` en el build.
6. **Sin secretos en el repo**; sÃ³lo variables de entorno (ver `.env.example`).
7. Archivos de configuraciÃ³n **< 500 lÃ­neas**.

Detalle completo en [`openspec/constitution/`](./openspec/constitution/).

## Entorno de datos

- KB: `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13) â€” **solo lectura**.
- BD de metadatos KB: `GX_KB_ATPPuestos`.
- BD de la aplicaciÃ³n: `Rentas` @ `devmssqlatp.ecom.com.ar` (SQL Server) â€” credenciales en `.env`.

## Puesta en marcha

```bash
# JDK 17 es obligatorio: el java del PATH suele ser 1.8
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Copiar .env.example a .env y completar DB_USER / DB_PASSWORD
copy .env.example .env

# Build + tests
.\mvnw.cmd -B clean test

# IntegraciÃ³n contra la BD real (gated)
set ATP_DB_IT=true
.\mvnw.cmd -B test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"

# Levantar la app
.\\mvnw.cmd spring-boot:run
```

> En PowerShell los argumentos de Maven con punto van **entrecomillados**: sin comillas,
> `-Dsurefire.x` se parte y Maven lo interpreta como una fase del ciclo de vida.

---

_Proyecto en F1 â€” 2026. La KB original se conserva intacta como fuente de verdad._

