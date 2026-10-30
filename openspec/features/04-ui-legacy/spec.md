# Spec — Portación del diseño legacy a Thymeleaf (UI `04-ui-legacy`)

**Estado:** APPROVED (borrador aprobado por el usuario, 2026-10-23)  
**Módulo:** `04-ui-legacy` — capa de presentación (Spring MVC + Thymeleaf)  
**Alcance:** portar la **identidad visual y el layout** del MasterPage legacy (`MasterNuevo`) a
Thymeleaf, sin copiar `ThemeClass` ni el Design System Unanimo (R-ALC-001). Los colores,
tipografía y espaciado ya están derivados en `docs/design.md` (tokens `--atp-*`).

**Fuente legacy (solo lectura):** KB `F:\Modelo\Gx18\ATPPuestos` (GeneXus 18 U13).

---

## 1. Propósito

Garantizar que el login, home e inicio del replatform **se perciban idénticos** al sistema
legacy (marca institucional, jerarquía, densidad administrativa), usando HTML semántico y
CSS propio con tokens, sin frameworks ni CDN.

## 2. Alcance

### 2.1 IN
- Layout base Thymeleaf replicando `RwdMasterPage` (header con imagen cabecera `head960`,
  `UserInfoNuevo`, menú `TabsMenu`, `ContentHolder`, footer con logos ECOM/ATP).
- Vista de login replicando el WebComponent `Login` (campos Tipo/Usuario/Clave, fondo,
  mensajes por código de error 0–6, redirección a Home tras éxito).
- Vista Home replicando la lógica de `Home` (Login si sin sesión, Inicio si con sesión).
- Vista Inicio replicando el texto descriptivo de `Inicio`.
- Fragmento de menú server-side a partir de `ServicioAutorizacion`/`ProgramaAutorizado`
  (reemplaza `TabsMenu.Create()` condicional por Tipo).
- Extracción de assets institucionales (logos, imagen cabecera, fondo login, favicon) a
  `static/img/`.
- Fragmento de bloque de usuario (`UserInfo`/`UserInfoNuevo`) con "Salir".

### 2.2 OUT
- ABM de productos (spec `02-abm-productos`).
- Lógica de negocio de login/autorización (spec `03-login`).
- Modo oscuro y fuentes institucionales exactas (pendientes en `docs/design.md §9`).
- Copia de `ThemeClass` o del Design System Unanimo (prohibido, R-ALC-001).

## 3. Requisitos (EARS)

- **R-UI-001:** El sistema SHALL renderizar un layout base (`templates/layout/base.html`) con
  header (imagen cabecera + logos institucionales), nav (menú), main (`ContentHolder`) y
  footer (logos ECOM/ATP + versión).
- **R-UI-002:** El sistema SHALL mostrar el bloque de usuario (nombre, sucursal/receptoría,
  turno, comisión) únicamente cuando exista sesión activa; en ausencia de sesión SHALL
  ocultarlo y redirigir a `/login`.
- **R-UI-003:** El sistema SHALL renderizar el menú de navegación en el servidor a partir de
  los programas autorizados del usuario, replicando la selección por Tipo de sucursal
  (PUE: 5 ítems, RECEP: 4 ítems, COM: 1 ítem) y la estructura recursiva `SmoothNavMenuData`.
- **R-UI-004:** La vista `/login` SHALL replicar el WebComponent `Login`: campos Tipo
  (char 3, invitemsg "Acceso"), Usuario (char 10), Clave (char 10, password), fondo
  `login.login.png`, botón "Ingresar", y bloque de error con los mensajes literales por código
  0–6. Tras éxito (código 0) SHALL establecer la cookie `SUCURSAL` y redirigir a `/home`.
- **R-UI-005:** El controlador Home (`GET /home`) SHALL replicar la lógica de `Home`: si no hay
  sesión SHALL mostrar Login; si hay sesión SHALL ocultar Login y renderizar Inicio.
- **R-UI-006:** La vista `/inicio` SHALL mostrar el texto descriptivo del sistema.
- **R-UI-007:** Los assets institucionales (logos, imagen cabecera, fondo login, favicon) SHALL
  extraerse de la KB (solo lectura) y servirse desde `static/img/` (y `static/favicon.ico`).
- **R-UI-008:** Las plantillas Thymeleaf SHALL usar únicamente tokens `var(--atp-*)` para
  color y tipografía; no SHALL contener literales de color ni tamaño de fuente.
- **R-UI-009:** Los formularios de escritura SHALL usar `th:action` (⇒ token CSRF) y mostrar
  los errores de validación junto al campo.

## 4. Supuestos y decisiones

- **D-UI-001:** MasterPage a replicar: **`RwdMasterPage`** (master activo, con callers:
  `TDetProdPri`, `TurnosListado`, `WWTDetProdPri`, `ViewTDetProdPri`, `WPFormularioPago`).
  Usa `UserInfoNuevo` (con "Salir" integrado). `MasterNuevo` (sin callers) queda como referencia
  secundaria.
- **D-UI-002:** El menú se renderiza server-side (Thymeleaf) a partir de
  `ServicioAutorizacion`/`ProgramaAutorizado`, no se reutiliza `TabsMenu` (WebComponent
  Angular/Unanimo). DataProviders efectivos: `DPDolphinMenuActas` + `DPDolphinMenuDataRecepteria`
  (+ `DPMenuComisiones` si Comisión=0). `DPDolphinMenuData` (flat) es código muerto → descartar.
- **D-UI-003:** El bloque de usuario se implementa como fragmento `fragments/user-info.html`,
  replicando **`UserInfoNuevo`** (nombre, puesto/receptoría, turno, comisión; visibilidad
  condicional por Tipo; botón "Salir" integrado).
- **D-UI-004:** Assets se extraen con `genexus_io` (asset_read) o export de Image, sin modificar
  la KB. Imagen de cabecera = `head960`; footer = logos ATP/ECOM (versión comentada en legacy).
- **D-UI-005:** El footer muestra logos ECOM/ATP. La versión no se muestra en el legacy (comentada);
  se mantiene constante de build opcional.

## 5. Supuestos (cerrados, 2026-10-23)

- ASM-UI-01 ✅: imagen de cabecera = `head960`.
- ASM-UI-02 ✅: footer = logos ATP/ECOM (versión comentada en legacy).
- ASM-UI-03 ✅: `DPDolphinMenuData` (flat) es código muerto → descartar; usar Actas +
  Recepteria (+ Comisiones).
- ASM-UI-04 ✅: migrar `UserInfoNuevo` (master activo `RwdMasterPage`).

## 6. Trazabilidad

- `docs/design.md` §1–§9 (sistema de diseño, tokens, JS puntual).
- `docs/analisis-ATPPuestos.md` §5, §7 (WebPanels, MasterPages, imágenes, menús).
- Engram: `topic_key: spec/masterpage-thymeleaf` (obs #147).
- Spec `03-login` (login completo) y `02-abm-productos` (ABM en Thymeleaf).