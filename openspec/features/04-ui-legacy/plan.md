# Plan — Portación del diseño legacy a Thymeleaf (`04-ui-legacy`)

## 1. Objetivo
Implementar el layout base y las vistas login/home/inicio replicando el MasterPage legacy
`MasterNuevo`, con assets institucionales y menú server-side, respetando los tokens de
`docs/design.md`.

## 2. Fases

### Fase A — Assets (lectura de KB)
- Extraer con `genexus_io` (asset_read) / Image export: `logo_ecom`, `atp_logo_direccion`,
  imagen cabecera, fondo `login.login.png`, `favicon.ico`, fuentes SourceSansPro (4 ttf).
- Depositar en `atp-nucleo-compartido/src/main/resources/static/img/` y `static/`.
- Verificar rutas y tamaños.

### Fase B — Layout base Thymeleaf
- Modificar `templates/layout/base.html` para replicar `RwdMasterPage`:
  - Header con imagen cabecera (100% ancho) + logos ECOM/ATP.
  - Nav: fragmento `fragments/menu.html` (tabs recursivos) + fragmento `fragments/user-info.html`.
  - Main: `<main th:replace="~{::content}">` / content holder.
  - Footer: logos + versión (constante de build o `BuscarVersionActual`).
- Incluir `login.js` (toggle contraseña, autofocus) y `confirmaciones.js` donde corresponda.

### Fase C — Vistas
- `templates/nucleo/login.html`: formulario Tipo/Usuario/Clave con `th:action`, bloque de error
  por código, fondo `login.login.png`, botón "Ingresar".
- `templates/nucleo/home.html`: lógica Home (Login si sin sesión, Inicio si con sesión).
- `templates/nucleo/inicio.html`: texto descriptivo + menú activo.

### Fase D — Menú server-side
- Extender `ServicioAutorizacion` para exponer `ProgramaAutorizado` por Tipo (PUE/RECEP/COM).
- Fragmento `fragments/menu.html` que itera los programas y renderiza tabs anidados
  (`SmoothNavMenuData`), replicando la visibilidad condicional.

### Fase E — Fragmento de usuario
- `fragments/user-info.html` replicando `UserInfo`/`UserInfoNuevo`: nombre, puesto/receptoría,
  turno, comisión; visibilidad condicional por Tipo; botón "Salir" (POST a logout).

### Fase F — Verificación
- Pruebas Thymeleaf por pantalla (render, binding, redirección, error).
- Verificación de tokens (sin literales de color/tipografía en plantillas).
- CSRF activo en formularios de escritura.
- Contraste AA y navegación por teclado.

## 3. Dependencias
- Spec `03-login` (autenticación/contexto de sesión) debe estar implementado.
- `docs/design.md` (tokens) ya aprobado.

## 4. Riesgos
- Discrepancia de visibilidad de "Salir" entre MasterPages (resuelto en D-UI-001).
- Menú depende del Tipo y Comisión (lógica condicional a portar en Fase D).
- Assets referenciados por id interno requieren export manual (Fase A).