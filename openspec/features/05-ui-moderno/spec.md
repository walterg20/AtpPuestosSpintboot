# Spec — Diseño moderno (base template) `05-ui-moderno`

**Estado:** APPROVED (2026-10-23)  
**Módulo:** `05-ui-moderno` — capa de presentación (Spring MVC + Thymeleaf)  
**Objetivo:** Base de template moderna (header, nav horizontal sticky, footer) para todas las
pantallas, con colores de `ATPEvo2.css` y **imágenes dummy (SVG)** donde falten assets. No
busca pixel-perfect con el legacy; busca una base limpia, responsive y accesible.

**Fuente de color:** `docs/design.md` (tokens `--atp-*` derivados de `ATPEvo2.css`).

---

## 1. Principios

1. **Base única**: `templates/layout/base.html` con fragmentos `head`, `cabecera`, `pie`.
2. **Header**: imagen real `head960.png` (1024×150) + barra con logos (SVG dummy) y título.
3. **Nav horizontal sticky**: menú de programas autorizados, se queda fijo al hacer scroll.
4. **Main**: contenedor centrado (max-width 1024px).
5. **Footer**: logos (SVG dummy) + versión, fondo neutro.
6. **Imágenes dummy**: SVG escalables para logos/fondo cuando no hay asset real.
7. **Tokens only**: sin literales de color/tipografía en plantillas.

## 2. Requisitos (EARS)

- **R-UM-001:** El sistema SHALL renderizar `layout/base.html` con header (imagen `head960.png`
  + barra de logos/título), nav horizontal sticky, main centrado (max-width 1024px) y footer.
- **R-UM-002:** El nav horizontal SHALL ser sticky (`position: sticky; top: 0`) con z-index 100.
- **R-UM-003:** Las imágenes institucionales faltantes SHALL servirse como SVG dummy desde
  `static/img/` (`logo-ecom.svg`, `logo-atp.svg`, `login-bg.svg`).
- **R-UM-004:** La vista `/login` SHALL mostrar el form centrado (max-width 400px, fondo
  `login-bg.svg`) con campos Tipo/Usuario/Clave, botón "Ingresar" y bloque de error.
- **R-UM-005:** Las plantillas SHALL usar únicamente tokens `var(--atp-*)` para color y
  tipografía.
- **R-UM-006:** El footer SHALL incluir logos y versión sin fondo de imagen (neutro).

## 3. Fuera de alcance
- Pixel-perfect con el legacy (tablas, fondos de imagen, Microsoft Sans Serif).
- Modo oscuro.