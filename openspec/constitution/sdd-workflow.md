# Constitución — Flujo SDD y Puertas de Calidad

> Cómo fluye el trabajo desde la KB legacy hasta código con paridad verificada.

## 1. Fases

```text
F0 Diagnóstico  →  F1 Constitución  →  F2+ Ciclo por módulo SDD
```

- **F0 — Diagnóstico**: inventario de la KB, ambientes, BD. Entregable: `docs/analisis-ATPPuestos.md`. ✅
- **F1 — Constitución**: monorepo Maven de 2 módulos, `openspec/`, arnés de subagentes reescrito
  para Thymeleaf, `.engram/`. ✅ (2026-10-22)
- **F2+ — Ciclo por módulo**: repetir el flujo de §2 para cada módulo del alcance.

## 2. Ciclo por módulo

```text
(0) Alcance        → definir in/out y prioridad             [spec: 00-alcance]
(1) Análisis       → analista-gx extrae de la KB y de la BD
(2) Spec EARS      → spec.md + plan.md + tasks.md           [PUERTA: spec aprobada]
(3) Implementación → implementador-backend + implementador-vistas (en paralelo)
(4) Verificación   → verificador-qa (tests, paridad SQL, seguridad)
(5) Cierre         → actualizar matriz de paridad + resumen en Engram + archivar
```

## 3. Puertas de calidad (gates)

| Puerta | Entre fases | Criterio de paso |
|---|---|---|
| **G-SPEC** | (2) → (3) | `spec.md` en sintaxis EARS, cada requisito con criterio de verificación; `plan.md` con mapeo JPA exacto, contrato REST y **contrato de vistas**; `tasks.md` casillado y verificable. **Aprobación humana.** |
| **G-DATA** | (1) → (2) | Esquema físico identificado contra la BD real (tabla, columna, tipo, nulabilidad, PK/FK) y **columnas huérfanas** cubiertas. Si no hay acceso a la BD, derivar de la KB y marcarlo como supuesto. |
| **G-TEST** | (4) → (5) | Suite verde; criterios de aceptación cubiertos; paridad SQL revisada vs. Navigation Report. |
| **G-SEC** | (4) → (5) | Sin secretos en repo; CSRF activo; sin vulnerabilidades conocidas; validación de entrada presente. |

## 4. Roles (subagentes `.opencode/agents/`)

| Subagente | Produce | Escribe en |
|---|---|---|
| `coordinador` | Plan de subtareas, validación de entregables, `tasks.md` | `openspec/features/*/tasks.md` |
| `analista-gx` | `spec.md`, `plan.md`, `tasks.md` | `openspec/features/<modulo>/` |
| `implementador-backend` | Entidades JPA, repositorios, servicios, seguridad, API REST | `*/src/main/java/**/{domain,repository,service,dto,shared,web/rest}/**` |
| `implementador-vistas` | Controllers MVC, plantillas Thymeleaf, tokens CSS, JS puntual | `*/src/main/java/**/web/mvc/**`, `*/src/main/resources/{templates,static}/**` |
| `verificador-qa` | Tests, auditoría de paridad y seguridad, informe | `docs/**` (read-only sobre el código) |

El **coordinador no escribe código**: delega y controla.

> **Dos implementadores, un solo árbol.** Escriben en el mismo repositorio pero en **rutas
> disjuntas**. La frontera es `web/rest` (backend) vs `web/mvc` + `templates` + `static` (vistas).
> Si alguna vez esa frontera se rompe, el síntoma es un conflicto de escritura silencioso.

## 5. Uso de OpenSpec CLI

La carpeta `openspec/` combina la estructura nativa de OpenSpec (`changes/`, `specs/`,
`config.yaml`) con nuestro layout (`constitution/`, `features/`).

- Validar: `openspec validate` / `openspec list` cuando aplique.
- El contexto del proyecto para la IA vive en `openspec/config.yaml` (campo `context`).
- No ejecutar `openspec init` de nuevo: recrea la estructura nativa y podría pisar `agents.md`
  (colisión de mayúsculas en Windows).

## 6. Memoria persistente (Engram)

- **Módulo**: `mem_save` con `topic_key: spec/<modulo>` al cerrar cada módulo.
- **Sesión**: `mem_save` de cada decisión, hallazgo o convención nueva, y `mem_session_summary`
  al cerrar.
- `.engram/` es portable y trackeable; la BD real de Engram vive fuera del repo.
- **Proceso**: `mem_save` de hallazgos de base de datos (esquema físico, columnas huérfanas,
  formatos de dato) es lo que evita repetirlos en el próximo módulo.

## 7. Comandos de build de referencia

```bash
set JAVA_HOME=C:\Program Files\Java\jdk-17

./mvnw clean test                                              # unitarios + vistas (siempre)
./mvnw test "-Dtest=*IT" "-Dsurefire.failIfNoSpecifiedTests=false"   # integración (ATP_DB_IT=true)
./mvnw -pl atp-puestos-camineros spring-boot:run                # levantar la app
```

> En PowerShell los argumentos de Maven con punto van **entrecomillados**: si no, PowerShell los
> parte y Maven interpreta un fragmento como fase del ciclo de vida.
