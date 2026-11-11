# Verificación Gate 2 — Vistas migradas a base-stitch (feature/design @ a46da97)

**Agente**: verificador-qa
**Fecha**: 2026-11-10
**Veredicto**: PASS

## 1. Estado del gate

PASS. Las tres vistas migradas (`nucleo/inicio.html`, `nucleo/not-authorized.html`, `error.html`)
usan `layout/base-stitch` en todos sus `th:replace`, no quedan referencias al layout legacy
`layout/base` (sin sufijo) fuera de los archivos de layout, y la suite completa de tests queda
verde (201 tests, 0 fallos, 0 errores).

## 2. Evidencia

### 2.1 Inspección de referencias a layout legacy

Comando ejecutado:
```
grep -rn "layout/base[^-]" **/*.html
```
Resultado: **0 coincidencias**. Las únicas referencias a `layout/base` son a `layout/base-stitch`
(85 coincidencias) y las definiciones de los propios layouts (`layout/base.html`,
`layout/base-stitch.html`).

Verificación de los fragmentos usados por las vistas migradas contra `base-stitch.html`:
`head`, `topbar`, `hero`, `sesion`, `navegacion`, `footer-institucional` — todos definidos
(`th:fragment=...`).

### 2.2 Suite de tests (autoritativa)

Comando exacto:
```
set JAVA_HOME=C:\Program Files\Java\jdk-17
.\mvnw.cmd -B clean test
```
Log capturado en: `C:\Users\walterg20\AppData\Local\Temp\opencode\gate2-mvn-test.log`
Resultado final del log:
```
[INFO] Tests run: 201, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Reactor:
- ATPPuestos-Monorepo: SUCCESS
- ATPPuestos-NucleoCompartido: SUCCESS (22.119 s)
- ATPPuestos-PuestosCamineros: SUCCESS (16.046 s)

### 2.3 Tests relevantes (MockMvc)

| Clase | Tests | Resultado |
|---|---|---|
| `AuthMvcHomeTest` | 3 | PASS (renderiza `nucleo/home`, view model completo, navegación stitch) |
| `AuthMvcNotAuthorizedTest` | 3 | PASS (renderiza `nucleo/not-authorized`, mensaje verbatim, sin traza) |
| `AuthMvcSecurityTest` | 8 | PASS (rutas `/`, `/home`, `/not-authorized`: redirects 302; CSRF 403/200) |
| `GlobalExceptionHandlerTest` | 12 | PASS (500/404/405 uniformes, vista `error` para canal HTML) |

### 2.4 Renderizado verificado (MockMvc + runtime)

- `GET /` → 302 a `/home` (test `getRaizSinSesionRedirigeALogin`, `getRaizConSesionRedirigeAHome`).
- `GET /home` → 200, vista `nucleo/home` (AuthMvcHomeTest).
- `GET /inicio` → 200 en runtime (sesión autenticada, `stitch-*` presente, 63 ocurrencias).
- `GET /not-authorized` → 200, vista `nucleo/not-authorized` (AuthMvcNotAuthorizedTest) y
  render runtime con 39 ocurrencias `stitch-*`.
- `GET /error` (BasicErrorController, Accept: text/html) → renderiza `error.html` con
  `stitch-body`, `stitch-topbar`, `stitch-tokens.css` (evidencia runtime capturada).

### 2.5 Vistas migradas inspeccionadas

- `nucleo/inicio.html`: `head/topbar/hero/sesion/navegacion/footer-institucional` de `base-stitch`.
- `nucleo/not-authorized.html`: idem, además de alerta de error con mensaje verbatim.
- `error.html`: idem, con operadores elvis para los dos orígenes (handler y BasicErrorController).

## 3. Hallazgos y riesgos

1. **Ningún hallazgo bloqueante.** Las tres vistas migradas cumplen el criterio de usar
   `base-stitch`.
2. **Cobertura parcial (no bloqueante)**: no existe un test MockMvc dedicado para `GET /inicio`
   ni para el render de `error.html` en el canal HTML (los tests de `GlobalExceptionHandler`
   cubren el canal REST). Se verificaron por inspección de fragmentos y por render runtime
   con sesión autenticada. Recomendación: agregar tests `AuthMvcInicioTest` y un caso de
   render de `error` en `GlobalExceptionHandlerTest` para cerrar la brecha.
3. **Riesgo de jar stale**: `spring-boot:run` consume el jar instalado en `~/.m2`; fue
   necesario `mvn install -DskipTests` para que el runtime reflejara las vistas migradas.
   Los tests `@WebMvcTest` usan `target/classes` directamente y sí ven los cambios sin install.
   (No es un defecto de código, es una nota de proceso.)

## 4. Próximo paso

Gate 2 PASS. Proceder con la revisión de gobernanza (G-POSTMAN, archivos <500 líneas, ISO 25030)
y, si se desea cerrar la brecha de cobertura, agregar tests para `/inicio` y render HTML de
`error`. No se requieren cambios en código productivo.