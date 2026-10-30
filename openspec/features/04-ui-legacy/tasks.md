# Tasks — Portación del diseño legacy a Thymeleaf (`04-ui-legacy`)

## Fase A — Assets (completada 2026-10-23)
- [x] T-UI-A1: Extraer `logo_ecom` → `static/img/logo-ecom.png`.
- [x] T-UI-A2: Extraer `atp_logo_direccion` → `static/img/logo-atp.png`.
- [x] T-UI-A3: Extraer imagen cabecera `head960` → `static/img/header.png`.
- [x] T-UI-A4: Extraer fondo login `login.login.png` → `static/img/login-bg.png` (asset tomado de Temp; confirmar).
- [x] T-UI-A5: `favicon.ico` placeholder (`webpanel.ico`) → `static/favicon.ico` (pendiente definir institucional).
- [ ] T-UI-A6: Fuentes SourceSansPro (pendiente decisión D-UI).

## Fase B — Layout base (completada 2026-10-23)
- [x] T-UI-B1: Reescribir `templates/layout/base.html` replicando `RwdMasterPage`.
- [x] T-UI-B2: Crear `fragments/user-info.html` (UserInfoNuevo).
- [x] T-UI-B3: Crear `fragments/menu.html` (lista server-side; recursividad en Fase D).
- [x] T-UI-B4: `nucleo.css` cubre header/nav/footer con tokens.

## Fase C — Vistas (completada 2026-10-23)
- [x] T-UI-C1: Ajustar `templates/nucleo/login.html` al WebComponent `Login`.
- [x] T-UI-C2: Ajustar `templates/nucleo/home.html` a la lógica de `Home`.
- [x] T-UI-C3: Crear `templates/nucleo/inicio.html` con texto descriptivo.

## Fase D — Menú server-side (completada 2026-10-23)
- [x] T-UI-D1: Extender `ServicioAutorizacion` para exponer programas por Tipo.
- [x] T-UI-D2: Implementar fragmento de menú recursivo en Thymeleaf.
- [x] T-UI-D3: Tests de menú (PUE/RECEP/COM).

## Fase E — Fragmento de usuario
- [x] T-UI-E1: Implementar `fragments/user-info.html` con visibilidad condicional.
- [x] T-UI-E2: Botón "Salir" (POST logout, limpieza de Context).
- [ ] T-UI-E3: Tests de user-info.

## Fase F — Verificación (PASS, 2026-10-23)
- [x] T-UI-F1: Pruebas Thymeleaf por pantalla (render, binding, redirección, error) — PASS.
- [x] T-UI-F2: Inspección de tokens (sin literales en plantillas) — PASS.
- [x] T-UI-F3: CSRF activo en formularios de escritura — PASS.
- [ ] T-UI-F4: Contraste AA y navegación por teclado — pendiente (manual).
- [x] T-UI-F5: `mem_save` de hallazgos relevantes — H-1 resuelto.