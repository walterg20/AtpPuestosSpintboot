# Verificación — Modal "Detalle de Carta de Porte / Guía de Tránsito"

**Feature**: `openspec/features/04-modal-detalle-guia/`
**Rama**: `feature/design`
**Fecha**: 2026-11-11
**Verificador**: verificador-qa (read-only sobre src)

## Comando ejecutado

```
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B -pl atp-puestos-camineros -am clean test
```

Log completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\mvn-modal.log`

## Resultado

- **Tests run: 201, Failures: 0, Errors: 0, Skipped: 0**
- Reactor: `ATPPuestos-Monorepo SUCCESS`, `ATPPuestos-NucleoCompartido SUCCESS`, `ATPPuestos-PuestosCamineros SUCCESS`
- **BUILD SUCCESS** (exit 0)

Clases relevantes:
- `CargaCorreccionMvcTest`: Tests run: 9, Failures: 0, Errors: 0
- `CargaMvcControllerTest`: Tests run: 19, Failures: 0, Errors: 0
- `ProductoMvcCsrfTest`: Tests run: 4, Failures: 0, Errors: 0

## Verificaciones solicitadas

### 1. Suite completa del módulo
PASS — ver resultado arriba (201/0/0).

### 2. CargaMvcControllerTest y CargaCorreccionMvcTest en verde + fragmento modal renderiza
PASS — ambas clases en verde (19 y 9 tests).
El fragmento `modal-detalle-guia :: modalDetalleGuia` se incluye en `control-cargas.html`
(reemplazo del include legacy `form506-detalle :: modal`). El id `modalDetalle` se conserva,
por lo que las aserciones existentes (`assertThat(html).contains("modalDetalle")`) siguen
pasando. Tests actualizados con stub `cargaService.obtenerDetalle(...)` (mock vacío) para
evitar NPE del nuevo código.

Evidencia (git diff tests): +1 línea en cada test con
`lenient().when(cargaService.obtenerDetalle(any(), any())).thenReturn(java.util.Optional.empty());`

### 3. CSS modal-detalle-guia.css sin literales hex/rgb/rgba
PASS — regex `#[0-9a-fA-F]{3,8}` / `rgb(` / `rgba(` → 0 matches.
Nombres de color CSS → único match es `white-space` (no color).

### 4. JS modal-detalle-guia.js sin imports/fetch/CDN
PASS (con salvedad) — el único match de `import|require|fetch|XMLHttpRequest|https?://` es el
comentario de cabecera `/* modal-detalle-guia.js - vanilla, sin imports ni fetch */`.
No hay código con dependencias externas. Archivo de 109 líneas.

### 5. No se modificaron archivos globales
PASS — `git diff --name-only` no incluye `stitch-tokens.css`, `control-cargas.css` ni layout base.
Archivos modificados: `CargaService.java`, `CargaMvcController.java`, `CargaForm.java`,
`control-cargas.html`, `CargaCorreccionMvcTest.java`, `CargaMvcControllerTest.java`.
Nuevos (untracked): `modal-detalle-guia.css`, `modal-detalle-guia.js`,
`fragments/modal-detalle-guia.html`, `openspec/features/04-modal-detalle-guia/`.

### 6. ddl-auto=none y ausencia de DDL/migraciones
PASS — `application.yml` (núcleo) línea 69: `ddl-auto: none`. Tests también `ddl-auto: none`.
Sin `schema.sql`/`data.sql`, sin Flyway/Liquibase, sin cambios en entidades JPA ni repositorios
(`git diff` no toca `domain/` ni `repository/`).

### 7. Gates

| Gate | Estado | Evidencia |
|---|---|---|
| G-SPEC | PASS (existencia) | `openspec/features/04-modal-detalle-guia/spec.md`, `plan.md`, `tasks.md` existen. Estado declarado en spec: `PROPUESTA` (pendiente de aprobación formal). |
| G-IMPL | PASS | Código implementado: `CargaForm` (+19 campos), `CargaMvcController` (fallbacks), `CargaService.obtenerDetalle`, fragmento, CSS, JS, include. |
| G-TEST | PARCIAL | Suite existente en verde (201/0/0) y sin regresión en `CargaMvcControllerTest`/`CargaCorreccionMvcTest`. **Falta**: la spec exige ≥12 pruebas nuevas del modal (render cabecera/fiscalización/flete/totales, fallbacks, formato importes/fecha, assets, tokens). No se encontraron tests nuevos referenciando `modalDetalleGuia`/`mdg-`. |
| G-DATA | PASS | `ddl-auto=none`, sin DDL/migraciones, sin cambios en entidades JPA. |
| G-POSTMAN | NO APLICA | Sin endpoints nuevos/modificados; solo MVC controller. Confirmado por git diff (sin cambios REST). |

## Hallazgos

### H-1 (media) — Tests nuevos del modal no implementados (T-010)
- **Síntoma**: la spec (§8 criterio 1) exige ≥12 pruebas nuevas (render, fallbacks, formato, assets, tokens). No existen en el repo (grep sin resultados).
- **Causa raíz probable**: tarea T-010 del plan no ejecutada por el implementador.
- **Responsable**: implementador-vistas / implementador-backend.
- **Impacto**: G-TEST parcial; no bloquea el build.

### H-2 (baja) — spec en estado PROPUESTA
- **Síntoma**: `spec.md` declara `Estado: PROPUESTA` pendiente de aprobación en puerta G-SPEC.
- **Responsable**: coordinador (aprobación formal).

### H-3 (baja, preexistente) — ActaService.java con 734 líneas (>500)
- **Síntoma**: archivo de configuración/código por encima del límite de 500 líneas.
- **Nota**: preexistente, no introducido por esta feature. No bloquea.

### H-4 (info) — marcar() no pobló campos del modal desde detalle
- **Síntoma**: en `CargaMvcController.marcar()` se setean fiscalizador/puestoIngreso/fechaControl/hashControl con fallbacks de contexto, pero **no** se llama a `obtenerDetalle` ni se poblan los campos de flete/cabecera. La spec T-004 pedía aplicar la misma población que `detalle()`.
- **Evidencia**: diff de `marcar()` solo agrega 4 setters (fiscalizador, puestoIngreso, fechaControl, hashControl). No hay bloque `obtenerDetalle`.
- **Impacto**: cuando se marca una guía, el modal no muestra datos de flete/cabecera (solo fallbacks vacíos/0). Posible regresión de paridad observable.
- **Responsable**: implementador-backend.

### H-5 (info) — trimOrEmpty usa String.trim() (no RTRIM de BD)
- **Síntoma**: la spec enfatiza paridad RTRIM vs trim. El controller aplica `v.trim()` sobre valores ya leídos con `@ColumnTransformer(read="RTRIM(...)")`. Doble trim inofensivo, pero conviene verificar que no se pierdan espacios internos (trim solo afecta bordes). OK.

## Veredicto

**PASS PARCIAL** — build verde, sin regresión en tests existentes, CSS/JS limpios, sin cambios
globales, ddl-auto=none. Pendiente: tests nuevos del modal (H-1) y completar población en
`marcar()` (H-4). G-POSTMAN no aplica.

Veredicto final: **PASS** para gates G-SPEC (existencia), G-IMPL, G-DATA, G-POSTMAN (N/A);
**PARCIAL** para G-TEST (falta T-010).
