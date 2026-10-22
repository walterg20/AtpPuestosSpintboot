# Sistema de Diseño — Derivado de la UI Legacy (`ATPEvo2`)

> Base para la capa Thymeleaf. **Derivado, no copiado**: no se reutilizan ThemeClass ni imágenes
> del Design System Unanimo (ver `openspec/features/00-alcance/spec.md` R-ALC-001).
> **Fuente analizada**: `ATPEvo2.css` (3.754 líneas) del ambiente `Gen18`.
> **Implementación**: CSS propio con *custom properties* (tokens) en
> `atp-nucleo-compartido/src/main/resources/static/css/`. Sin framework, sin CDN, sin build.

## 1. Principios

1. **Paridad de identidad, no de markup**: se preserva la *percepción* (marca, jerarquía,
   densidad) con HTML semántico y CSS propio, no con componentes de terceros.
2. **Densidad administrativa**: es un sistema de gestión (grillas, formularios, listados). Se
   prioriza legibilidad y uso de teclado sobre efectos.
3. **Accesibilidad AA**: contraste ≥ 4.5:1 en texto; foco visible; navegación por teclado.
4. **Tokens, no literales**: ningún color ni tamaño de fuente escrito en una plantilla. Todo sale
   de `var(--atp-*)`.

## 2. Color

### 2.1 Tokens observados en el CSS legacy

| Rol observado | Valor legacy | Fuente |
|---|---|---|
| **Marca / primario** | `#C60B44` (carmesí) | 23 apariciones |
| Texto principal | `#333333`, `#000000` | — |
| Texto secundario | `#555555`, `#747474`, `#797979` | — |
| Superficie base | `#FFFFFF` | 99 apariciones |
| Superficie alterna | `#F0F0F0`, `#E5E5E5`, `#E4E5E6` | — |
| Bordes / divisor | `#D3D4D3`, `#CCCCCC`, `#C0C0C0` | — |
| Deshabilitado | `#A9A9A9`, `#969696`, `#828282` | — |
| Acento informativo | `#1D6893`, `#00008B` | — |
| Acento alerta | `#9D1F1F`, `#FF0000`, `#FF8000` | — |

### 2.2 Tokens del proyecto

```css
:root {
  /* Marca */
  --atp-primary:          #C60B44;   /* carmesí institucional */
  --atp-primary-dark:     #9D1F1F;   /* estados pressed / encabezados */
  --atp-primary-contrast: #FFFFFF;

  /* Neutrales (superficie / texto) */
  --atp-surface:          #FFFFFF;
  --atp-surface-alt:      #F0F0F0;
  --atp-border:           #D3D4D3;
  --atp-text:             #333333;
  --atp-text-muted:       #555555;
  --atp-disabled:         #A9A9A9;

  /* Semánticos */
  --atp-info:             #1D6893;
  --atp-warn:             #FF8000;
  --atp-error:            #C0392B;   /* ajustado desde #FF0000 para contraste AA */
  --atp-success:          #2E7D32;   /* nuevo: el legacy no define uno claro */
}
```

> **Desvíos intencionales, documentados como decisiones**:
> - El legacy usa `#FF0000` puro para error (contraste bajo sobre blanco). Se adopta `#C0392B` por
>   accesibilidad.
> - El texto secundario del legacy usa `#747474`, que mide **3,61:1** sobre blanco: por debajo del
>   mínimo AA de 4,5:1. Se adopta **`#555555`** (5,77:1 sobre `#FFFFFF`, 7,46:1 sobre `#F0F0F0`),
>   el valor más oscuro de la paleta secundaria del legacy. **No se toca el fondo**: oscurecer el
>   texto alcanza el mínimo.
> - El legacy no define color de éxito: `#2E7D32` es una incorporación.

## 3. Tipografía

- **Legacy**: `Trebuchet MS`, `Verdana`, `Arial`, `Tahoma`, `Microsoft Sans Serif`.
- **Propuesta**: pila del sistema, sin descarga de fuentes (requisito: sin CDN).

```css
:root {
  --atp-font:      "Segoe UI", "Trebuchet MS", Verdana, Arial, sans-serif;
  --atp-font-mono: "Cascadia Mono", Consolas, "Courier New", monospace;

  --atp-text-display: 24px;  /* título de módulo */
  --atp-text-title:   18px;  /* encabezado de sección */
  --atp-text-body:    14px;  /* texto de grilla y formularios */
  --atp-text-caption: 12px;  /* ayudas y metadatos */
}
```

## 4. Espaciado y forma

- Escala base **4px**: `4, 8, 12, 16, 24, 32` → `--atp-space-1..6`.
- Radio de borde: **4px** (`--atp-radius`).
- Elevación: sombras discretas (`--atp-shadow-1`, `--atp-shadow-2`); sin decoración superflua.

## 5. Mapeo legacy → componente

| Legacy (clases CSS) | Componente objetivo (HTML + tokens) |
|---|---|
| `Grid`, `GridNoData`, `FreeStyleGrid` | `<table class="atp-table">` + paginación + estado vacío |
| `Form`, `input-group`, `has-float-label` | `<form th:object>` + `<label>` + `input` con tokens |
| `BaseRBtn`, botones de acción | `.atp-btn` / `.atp-btn--primary` / `.atp-btn--ghost` |
| `WorkWith*`, `ResponsiveSlide*` | patrón Listado + Detalle (panel lateral o inline) |
| `Calendar*` | `<input type="date">` (nativo, sin librería) |
| Navegación "Dolphin"/`Sidebar` | `<nav>` + `<aside>` con tokens |
| `gx-responsive-popup` | panel inline (Thymeleaf, sin modal JS) |
| Alertas/mensajes (`Mensaje`) | `.atp-alert` (variantes `info`/`warn`/`error`/`success`) |

## 6. Patrones de interacción

- **Listados**: filtros arriba, grilla paginada, acciones por fila.
- **Alta/edición**: formulario en panel o inline; validación en línea y en el servidor.
- **Confirmaciones destructivas**: confirmación explícita (`confirm()` — es JS puntual permitido).
- **Estados**: toda vista de datos define `loading`, `empty` y `error`.
- **Errores**: mensaje legible + código de correlación; nunca trazas crudas al usuario.
  El `GlobalExceptionHandler` entrega la vista `error` con `codigo`, `mensaje` y `ruta`.

## 7. Accesibilidad (AA)

- Contraste de texto ≥ 4.5:1; componentes ≥ 3:1.
- Foco visible en todos los controles; orden de tabulación lógico.
- Etiquetas `<label for>` asociadas a cada campo; errores anunciados y ligados al campo.
- Grillas navegables por teclado; `aria-sort` en columnas ordenables.
- Formularios de escritura **siempre** con `th:action` (⇒ token CSRF).

## 8. JavaScript puntual — inventario permitido

| Archivo | Responsabilidad | Por qué no es JS de aplicación |
|---|---|---|
| `js/vigencias.js` | Alta/baja de filas de la grilla anidada de vigencias | La grilla se renderiza en el servidor; el JS sólo clona una fila de plantilla |
| `js/confirmaciones.js` | `confirm()` de acciones destructivas | Reutiliza el diálogo nativo: sin librería |
| `js/login.js` | Toggle de contraseña y autofocus | Puramente cosmético |

**Prohibido**: frameworks, bundlers, `npm` en el build, CDN, y cualquier lógica de negocio en JS.
Si una pantalla necesita más, el problema es de diseño de servidor.

## 9. Pendientes

- Extraer la paleta exacta de `Unanimo_chameleon` / DesignSystems para confirmar matices.
- Definir modo oscuro (no presente en el legacy) — decisión pendiente.
- Validar la tipografía con el responsable (¿se requiere la fuente institucional exacta?).
