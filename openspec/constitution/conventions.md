# Constitución — Convenciones y Guardarraíles

> Reglas operativas de trabajo del repositorio. Aplican a humanos y a subagentes.

## 1. Guardarraíles absolutos

| # | Regla | Consecuencia de violación |
|---|---|---|
| G1 | **Entorno legacy GeneXus = SOLO LECTURA.** Prohibido modificar, compilar, "Build All" o eliminar objetos/tablas en la KB original. | Bloqueo de la tarea; la KB es la fuente de verdad inmutable. |
| G2 | **Spec-Driven Development.** Ningún código de aplicación sin una spec aprobada en `openspec/features/<modulo>/`. | El código no se acepta ni se mergea. |
| G3 | **Sin secretos en el repositorio.** Credenciales, tokens y cadenas de conexión sólo por variables de entorno. | Remoción del commit y rotación del secreto. |
| G4 | **Archivos de configuración < 500 líneas.** | Refactor/partición antes de continuar. |
| G5 | **Sin DDL ni migraciones.** La BD es existente: se mapea, no se rediseña. | Reversión del cambio. |
| G6 | **JavaScript puntual.** Vanilla, sin framework, sin bundler, sin CDN, sin `npm` en el build. | Rechazo del PR. |
| G7 | **Monolito modular.** Regla de dependencia `web → service → repository → domain`; prohibido `web → repository`. | Corrección de la capa. |

## 2. Estructura de carpetas

```text
/
├── pom.xml                     ← parent POM (packaging=pom) + Maven Wrapper
├── atp-nucleo-compartido/      ← catálogo + personas + base de seguridad + API REST
│   ├── src/main/java/ar/com/atp/puestos/
│   │   ├── nucleo/             ← domain/ repository/ service/ web/{rest,dto,mvc}/
│   │   └── shared/             ← transversal: error/, web/
│   ├── src/main/resources/
│   │   ├── application.yml     ← configuración de la aplicación (fuente única)
│   │   ├── templates/          ← error.html + templates/nucleo/
│   │   └── static/             ← css/ (tokens) + js/
│   └── src/test/{java,resources}/
├── atp-puestos-camineros/      ← módulo desplegable
│   ├── src/main/java/ar/com/atp/puestos/
│   │   ├── PuestosApplication.java
│   │   └── puestos/            ← domain/ service/ web/{mvc,rest}/
│   ├── src/main/resources/
│   │   ├── templates/puestos/
│   │   └── static/js/
│   └── src/test/{java,resources}/
├── openspec/                   ← especificaciones formales (EARS)
│   ├── config.yaml
│   ├── constitution/           ← stack.md, conventions.md, sdd-workflow.md
│   └── features/<NN>-<modulo>/ ← spec.md, plan.md, tasks.md
├── docs/                       ← análisis, diseño y colección Postman
│   └── postman/
├── .opencode/                  ← arnés de subagentes y comandos
└── .engram/                    ← memoria persistente del agente
```

> **Una sola `application.yml`**: vive en el **núcleo**. Spring Boot toma el *primer*
> `classpath:/application.yml` que encuentra y **no** fusiona varios. Si el módulo desplegable
> trajera el suyo, uno de los dos sería ignorado en silencio. Lo específico de un módulo va en
> `application-<perfil>.yml` propios de ese módulo.

## 3. Nomenclatura

- **Módulos de spec**: `openspec/features/<NN>-<modulo-kebab>/` (p. ej. `03-login`).
- **Artefactos por módulo**: `spec.md`, `plan.md`, `tasks.md`.
- **Rutas REST**: `/api/v1/<recurso-plural>`. **Rutas de página**: `/<recurso>` (sin prefijo).
- **Entidades JPA**: nombre lógico en PascalCase; `@Table`/`@Column` con el nombre **físico exacto**.
- **Paquetes Java**: `ar.com.atp.puestos.<capa>`, con `<capa>` ∈ `domain | repository | service |
  dto | shared | web.rest | web.mvc`.
- **Vistas Thymeleaf**: `kebab-case.html`, en la subcarpeta del módulo (`templates/nucleo/`,
  `templates/puestos/`). **Nunca** dos plantillas con el mismo nombre en módulos distintos.
- **IDs de specs/requisitos**: `R-<NNN>` para requisitos EARS; `T-<NNN>` para tareas.
  Cuando el módulo tenga prefijo propio (p. ej. `R-PROD-<NN>`), se usa ese.

## 4. Idiomas

- Documentación, specs y mensajes de commit: **español**.
- Código (identificadores), nombres de tabla/columna y rutas técnicas: **inglés** o el nombre
  físico heredado.
- Commits: *Conventional Commits* (`feat:`, `fix:`, `docs:`, `chore:`, `spec:`).

## 5. Requisitos en sintaxis EARS

Cada requisito de negocio se redacta en EARS, en una de estas formas:

- **Ubiquo**: `El sistema deberá <respuesta>.`
- **Dirigido por evento**: `CUANDO <disparador>, el sistema deberá <respuesta>.`
- **Dirigido por estado**: `MIENTRAS <estado>, el sistema deberá <respuesta>.`
- **Comportamiento no deseado**: `SI <condición>, ENTONCES el sistema deberá <respuesta>.`
- **Opcional**: `DONDE <característica>, el sistema deberá <respuesta>.`

Todo requisito incluye **criterio de verificación** (cómo se comprueba: test, inspección, demo).

## 6. Paridad funcional (ISO/IEC 25030)

- La paridad se mide por **comportamiento observable** (entradas → salidas → efectos en BD), no por
  igualdad estructural del código.
- Cada módulo declara su **matriz de paridad**: requisito legacy ↔ requisito nuevo ↔ prueba.
- Los desvíos intencionales se documentan como **decisiones** con su justificación.
- Los **textos visibles al usuario** (mensajes de error, rótulos) son parte del contrato: se
  transcriben **verbatim** del legacy, incluidas sus erratas, y se declaran como tales.

## 7. Sobre la KB y las herramientas de introspección

- La KB se lee con el servidor MCP `genexus18mcp` (alias `atppuestos` para `Gx18\ATPPuestos`).
- El MCP actual soporta `major` 16/17/18: **las KB de GeneXus 15 no son accesibles** con esta
  herramienta. Para ellas hay que usar otro canal (exportación `.xpz` / `.xml`).
- El índice de la KB puede estar en construcción: si una búsqueda vuelve vacía, esperá con
  `genexus_lifecycle action=status wait=30 freshness=current` y reintentá; **no** concluyas que
  el objeto no existe.
