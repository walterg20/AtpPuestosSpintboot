# Tasks — 04 Modal "Detalle de Carta de Porte / Guía de Tránsito"

> **Estado**: `APROBADO` — puerta **G-SPEC** cerrada (2026-11-11).
> Lista ordenada y atómica de tareas verificables. **No marcar como completo** (eso lo hace el
> coordinador con evidencia).
> **Dependencias**: `00-alcance`, `01-nucleo-datos` (contrato REST y BD), `02-abm-productos` (patrón
> de vistas Thymeleaf y tests), `03-login` (no bloquea; el modal no introduce seguridad).
> **Reglas**: `ddl-auto=none` (sin DDL), G-POSTMAN no aplica, JS puntual vanilla, sin modificar
> estilos globales/layout.

---

## T-001 — Crear el directorio del feature y verificar el estado inicial

- **Dependencias**: ninguna.
- **Descripción**: Crear `openspec/features/04-modal-detalle-guia/` (ya creado por esta spec) y
  verificar que `./mvnw clean test` está en verde antes de empezar.
- **Hecho cuando**: la suite existente pasa sin cambios; el directorio contiene `spec.md`,
  `plan.md`, `tasks.md`.

## T-002 — Ampliar `CargaForm` con los campos del modal

- **Dependencias**: T-001.
- **Descripción**: Agregar a `CargaForm.java` los campos listados en `plan.md` §4.3
  (`chasis`, `acoplado`, `kmFlete`, `tarifaKm`, `montoFlete`, `alicuota`, `impuestoFlete`,
  `adicional10`, `subtotalFlete`, `subtotalTraslado`, `totalGeneral`, `fiscalizador`,
  `hashControl`, `expedienteDigital`, `domicilioFiscal`, `puestoIngreso`, `fechaControl`,
  `tipoComprobante`) con getters/setters en camelCase. Tipos: `String`, `BigDecimal`, `Integer`,
  `LocalDateTime` (importar `java.time.LocalDateTime`). Todos opcionales (sin Bean Validation).
- **Hecho cuando**: el archivo compila (`./mvnw -pl atp-puestos-camineros compile`); no hay cambios
  en otros archivos.

## T-003 — Ampliar `CargaMvcController.detalle(...)` con fallbacks del modal

- **Dependencias**: T-002.
- **Descripción**: En `CargaMvcController.detalle(...)`, tras poblar los campos legacy, resolver
  los campos del modal con los fallbacks de `plan.md` §4.4 (cadena vacía para texto, `BigDecimal.ZERO`
  para importes, `LocalDateTime.now()` para `fechaControl`, `"F"` para `tipoComprobante`). Resolver
  `fiscalizador` (DPPUltPaOp → contexto.usuario() → `""`), `puestoIngreso` (DPPActaPue → contexto
  puesto → `""`), `hashControl` (derivado determinista → `""`). Si no hay `DetalleProductoPrimario`
  disponible, aplicar todos los fallbacks.
- **Hecho cuando**: `GET /cargas/detalle?dppNroForm=1234567890` renderiza con los fallbacks sin
  `null`/`undefined`; test `MvcModalFallbacks` en verde.

## T-004 — Ampliar `CargaMvcController.marcar(...)` con fallbacks del modal

- **Dependencias**: T-002, T-003.
- **Descripción**: Aplicar la misma población de campos del modal en `marcar(...)` (R-MDG-01).
- **Hecho cuando**: `POST /cargas/marcar` renderiza el modal con los campos poblados; test en verde.

## T-005 — Crear el fragmento Thymeleaf `modal-detalle-guia.html`

- **Dependencias**: T-002.
- **Descripción**: Crear `templates/puestos/fragments/modal-detalle-guia.html` con
  `th:fragment="modalDetalleGuia"` siguiendo la plantilla de `plan.md` §4.5: cabecera, fiscalización,
  flete, totales, mensajes legacy verbatim, acciones (Cerrar/Imprimir/Descargar). Usar `th:text`,
  `#numbers.formatDecimal` para importes y `#temporals.format` para fechas. Incluir
  `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, `aria-describedby`.
- **Hecho cuando**: el archivo existe; `GET /cargas/detalle` renderiza el fragmento; test
  `detalleRenderizaModal` (actualizado) en verde.

## T-006 — Crear `modal-detalle-guia.css`

- **Dependencias**: T-005.
- **Descripción**: Crear `static/css/modal-detalle-guia.css` con las clases `mdg-*` de `plan.md` §4.6,
  usando **exclusivamente** tokens `--stitch-*`. Incluir responsive `@media (max-width: 640px)`.
- **Hecho cuando**: el archivo existe; test de tokens (sin literales hex/rgb/rgba) en verde.

## T-007 — Crear `modal-detalle-guia.js`

- **Dependencias**: T-005.
- **Descripción**: Crear `static/js/modal-detalle-guia.js` con la IIFE de `plan.md` §4.7: cerrar
  (`data-mdg-close`), backdrop, Escape, atrapamiento de foco, restauración de foco, imprimir
  (`window.print()`), descargar (alert placeholder), autofocus. Vanilla, sin `fetch`/`XMLHttpRequest`/
  `import`/`require`.
- **Hecho cuando**: el archivo existe; revisión sin dependencias externas; prueba de navegador
  (cerrar, Escape, backdrop, imprimir, descargar) en verde.

## T-008 — Integrar el fragmento y los assets en `control-cargas.html`

- **Dependencias**: T-005, T-006, T-007.
- **Descripción**: Reemplazar el include legacy `form506-detalle :: modal` por
  `modal-detalle-guia :: modalDetalleGuia` (R-MDG-17). Agregar `<link>` y `<script>` de los nuevos
  assets al final del `<body>`. No modificar `stitch-tokens.css`, `control-cargas.css` ni el layout.
- **Hecho cuando**: `GET /cargas/detalle` renderiza el nuevo fragmento; la suite existente sigue
  verde; `git diff` no toca archivos globales.

## T-009 — Actualizar tests existentes que referencien el fragmento legacy

- **Dependencias**: T-008.
- **Descripción**: Actualizar `CargaCorreccionMvcTest` (y cualquier test que busque `modalDetalle`/
  `atp-modal`) para reflejar el nuevo fragmento (`modalDetalle` sigue existiendo como id; el texto
  `modalDetalleGuia`/`mdg-modal` puede usarse como marcador). Mantener el contrato de los tests
  existentes (presencia de `modalDetalle`, `Cerrar`, `cerrarModal` → ahora `data-mdg-close`).
- **Hecho cuando**: la suite `CargaCorreccionMvcTest` está en verde sin debilitar las aserciones.

## T-010 — Crear tests nuevos del modal (render, fallbacks, formato, assets, tokens)

- **Dependencias**: T-008.
- **Descripción**: Agregar pruebas en `CargaCorreccionMvcTest` (o una nueva clase
  `ModalDetalleGuiaMvcTest`) que cubran: R-MDG-07 (cabecera), R-MDG-08 (fiscalización),
  R-MDG-09 (flete), R-MDG-10 (totales), R-MDG-11 (fallbacks sin `null`), R-MDG-12 (formato de
  importes), R-MDG-13 (formato de fecha), R-MDG-19 (assets), R-MDG-20 (tokens sin hex).
- **Hecho cuando**: ≥ 12 pruebas nuevas en verde; cobertura de los requisitos listados.

## T-011 — Prueba de navegador (abrir/cerrar/Escape/backdrop/foco/imprimir/descargar)

- **Dependencias**: T-007, T-008.
- **Descripción**: Ejecutar prueba de navegador (Playwright/Selenium) que abre el modal, lo cierra
  por botón, Escape y backdrop, tabula dentro del modal, invoca imprimir (mock `window.print`) y
  descargar (mensaje placeholder), y verifica la restauración de foco.
- **Hecho cuando**: la prueba pasa sin errores; evidencia (screenshot/log) registrada.

## T-012 — Auditoría de accesibilidad (axe-core + lighthouse)

- **Dependencias**: T-008.
- **Descripción**: Ejecutar `axe-core` in-page en dos estados (modal cerrado y modal abierto) y
  `lighthouse_audit` de accesibilidad; corregir hallazgos críticos.
- **Hecho cuando**: sin hallazgos críticos; reporte adjunto.

## T-013 — Verificación de paridad BD y sin DDL

- **Dependencias**: T-008.
- **Descripción**: Verificar `ddl-auto=none`, ausencia de DDL/migraciones, sin cambios en
  `DetalleProductoPrimario`, `CargaService` ni endpoints REST. Verificar G-POSTMAN (sin cambios).
- **Hecho cuando**: `git diff` confirma; `./mvnw clean test` en verde; IT (`*IT`) en verde con
  rollback.

## T-014 — Guardar resumen en Engram

- **Dependencias**: T-013.
- **Descripción**: Guardar en Engram (`mem_save`, `topic_key: spec/04-modal-detalle-guia`) el
  resumen: objetos inventariados, decisiones tomadas, supuestos abiertos, riesgos.
- **Hecho cuando**: la observación queda persistida y se devuelve el resumen compacto al coordinador.

---

## Estado de ejecución (coordinador, 2026-11-11)

| Tarea | Estado | Evidencia |
|---|---|---|
| T-001 | ✅ Completado | `./mvnw clean test` verde antes de empezar; directorio creado con spec/plan/tasks. |
| T-002 | ✅ Completado | `CargaForm.java` ampliado (+18 campos); `./mvnw -pl atp-puestos-camineros compile` OK. |
| T-003 | ✅ Completado | `CargaMvcController.detalle(...)` con fallbacks; `MvcModalFallbacks` en verde. |
| T-004 | ✅ Completado | `CargaMvcController.marcar(...)` poblado igual que `detalle()`; `ResultadoMarcado.detalle` agregado. |
| T-005 | ✅ Completado | `fragments/modal-detalle-guia.html` con `th:fragment="modalDetalleGuia"`; render OK. |
| T-006 | ✅ Completado | `modal-detalle-guia.css` creado; 0 literales hex/rgb/rgba (regex). |
| T-007 | ✅ Completado | `modal-detalle-guia.js` IIFE vanilla, 109 líneas, sin fetch/CDN. |
| T-008 | ✅ Completado | `control-cargas.html` include reemplazado + link/script assets. |
| T-009 | ✅ Completado | `CargaCorreccionMvcTest` y `CargaMvcControllerTest` actualizados con stub `obtenerDetalle`. |
| T-010 | ✅ Completado | `CargaModalDetalleGuiaMvcTest` con 14 tests en verde. |
| T-011 | ⏸ Pendiente | Sin entorno de navegador en CI local; cobertura automatizada cubre render/cierre server-side. |
| T-012 | ⏸ Pendiente | axe-core/lighthouse no ejecutados en CI local. |
| T-013 | ✅ Completado | `ddl-auto=none` mantenido; sin DDL/migraciones; `git diff` sin archivos globales. |
| T-014 | ✅ Completado | Memoria persistida en Engram (ver mem_save). |

**Gate final**: G-SPEC ✅, G-IMPL ✅, G-TEST ✅ (215/0/0), G-DATA ✅, G-POSTMAN ✅ (no aplica).
**Veredicto global**: PASS condicionado a T-011/T-012 (prueba de navegador y accesibilidad), que quedan como backlog.