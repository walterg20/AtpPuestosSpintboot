# Referencia de diseño — Stitch "Modern Spring Boot Portal"

**Origen:** https://stitch.withgoogle.com/projects/6019790973415672987
**Descargado:** 2026-11-05
**Rama:** design/stitch-modern-portal

## Archivos disponibles
- `openspec/features/99-stitch-design/design.md` — DESIGN.md completo (paleta, tipografía, layout, elevación, shapes, components).
- `openspec/features/99-stitch-design/tokens.json` — tokens estructurados (namedColors, typography, spacing, roundness).
- `openspec/features/99-stitch-design/screens/` — HTML de 6 pantallas (comisiones, turnos, carga-manual, formularios-actas, control-cargas, login).

## Cómo usar
- Al implementar o rediseñar una vista Thymeleaf, leer `design.md` y `tokens.json` para la paleta, tipografía y componentes.
- Los HTML en `screens/` sirven como referencia visual del layout moderno.
- El nuevo template reemplaza el layout base actual (`layout/base.html` → `layout/base-stitch.html`).

## Notas
- Paleta primaria: #0f2a4a (navy), secondary: #0284c7 (cyan), tertiary: #059669 (emerald).
- Tipografía: Inter (display, headline, body, label, data-mono).
- Layout: 12 cols, max-width 1600px, gutters 1.5rem, margins 2rem.
- Status badges: Abierto (verde), Cerrado (gris), Demorado (amber).