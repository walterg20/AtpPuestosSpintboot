# Spec — Cabecera y footer institucionales uniformes (14-cabecera-footer-uniformes)

**Estado:** APPROVED (validado por el usuario, 2026-11-10)
**Módulo:** 14-cabecera-footer-uniformes — capa de presentación (Spring MVC + Thymeleaf)
**Depende de:** 04-ui-legacy, 05-ui-moderno, 99-stitch-design (base-stitch)

## 1. Propósito
Garantizar que **todas** las vistas del monolito (núcleo + puestos) compartan la **misma cabecera institucional** (topbar gubernamental + hero con camión/distintivo) y el **mismo footer institucional** (logos ATP/ECOM + copyright + status), replicando exactamente el chrome ya implementado en `nucleo/login.html`, sin duplicar markup inline.

## 2. Alcance
### IN
- Extraer `topbar`, `hero` y `footer-institucional` como fragmentos reutilizables en `layout/base-stitch.html`.
- Refactorizar `login.html` para consumir esos fragmentos (visual idéntico).
- Migrar todas las vistas Stitch (`puestos/*`) a consumir `topbar + hero + sesion + navegacion + footer-institucional`.
- Migrar las vistas del layout viejo (`nucleo/productos.html`, `nucleo/turnos.html`) a `base-stitch` con los nuevos fragmentos (paridad visual con el login).
- Mantener `layout/base.html` como fallback hasta que no queden referencias.

### OUT
- Rediseño del contenido de cada vista (formularios, tablas, grillas anidadas) — solo cambia el chrome.
- Optimización de imágenes pesadas (tarea aparte).
- Modo oscuro.

## 3. Requisitos (EARS)
- **R-CH-001:** El sistema SHALL definir en `layout/base-stitch.html` los fragmentos `topbar`, `hero` y `footer-institucional`, replicando verbatim el markup de `nucleo/login.html`.
- **R-CH-002:** `login.html` SHALL consumir dichos fragmentos y renderizar idéntico al estado actual.
- **R-CH-003:** Toda vista Stitch (`puestos/*`) SHALL renderizar `topbar`, `hero`, `sesion` (si autenticado), `navegacion` (si autenticado) y `footer-institucional`, en ese orden.
- **R-CH-004:** Las vistas del layout viejo migradas (`productos`, `turnos`) SHALL renderizar el mismo chrome institucional.
- **R-CH-005:** Ninguna vista migrada SHALL contener topbar/hero/footer inline duplicado.
- **R-CH-006:** Todas las vistas migradas SHALL usar únicamente tokens `var(--stitch-*)` / `var(--atp-*)` ya existentes; sin CDN ni literales de color.
- **R-CH-007:** Los controllers MVC y la lógica Thymeleaf (`th:object`, `th:field`, bindings) NO SHALL cambiar; solo el chrome.
- **R-CH-008:** Los tests `@WebMvcTest`/MockMvc existentes SHALL seguir pasando (render, binding, redirección).

## 4. Riesgos
- Migración de `productos`/`turnos` (layout viejo → stitch) es de alto churn y puede romper tests de render.
- El hero con camión (~1 MB) en todas las páginas impacta performance; optimización diferida.
- El hero ocupa ~200 px verticales en pantallas de trabajo.

## 5. Trazabilidad
- `openspec/features/99-stitch-design/screens/login.html` (referencia visual).
- `openspec/features/99-stitch-design/design.md` (tokens y componentes).
- `atp-nucleo-compartido/.../templates/nucleo/login.html` (implementación de referencia).