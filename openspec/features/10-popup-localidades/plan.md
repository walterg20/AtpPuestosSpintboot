# Plan de Implementación — Módulo 10: Popup de Selección Manual de Localidades

> **Módulo**: `10-popup-localidades` — IMPLEMENTADO (2026-11-04), Gate 2 PASS (unit 177)
> **Spec de referencia**: `openspec/features/10-popup-localidades/spec.md`
> **Convenciones**: `openspec/constitution/conventions.md`, `openspec/constitution/stack.md`

---

## 1. Arquitectura de la solución

El módulo agrega **una página MVC** (`/localidades`) y **modifica el formulario de alta de Acta**
(`/actas/nueva`) para habilitar el popup cuando la derivación por CUIT falle. No agrega endpoints
REST ni cambia el esquema de BD.

### 1.1 Componentes nuevos / modificados

| Capa | Componente | Acción | Paquete |
|---|---|---|---|
| `web/mvc` | `LocalidadesController` | **Nuevo** | `puestos.web.mvc` |
| `web/mvc` | `ActaFormularioController` | **Modificar** (agregar flag de popup) | `puestos.web.mvc` |
| `service` | `LocalidadPopupService` (opcional) | **Nuevo** (wrapper sobre `LocalidadRepository`) | `puestos.service` |
| `domain` | `Localidad`, `Provincia` | **Sin cambios** | `nucleo.domain` |
| `repository` | `LocalidadRepository` | **Sin cambios** | `nucleo.repository` |
| `templates` | `puestos/localidades-seleccion.html` | **Nuevo** | `templates/puestos/` |
| `templates` | `puestos/acta-formulario.html` | **Modificar** (ícono ⇧) | `templates/puestos/` |
| `static/js` | `puestos/localidades-popup.js` | **Nuevo** (JS puntual vanilla) | `static/js/puestos/` |

### 1.2 Flujo de interacción

```
Formulario de alta (/actas/nueva)
  └─ Usuario ingresa CUIT
      └─ POST parcial / IsValid → BuscarInfoPorCuitLite
          ├─ CUIT existe → LocCod, LocNom, ProvNombre derivados; ícono ⇧ deshabilitado
          └─ CUIT no existe → campos vacíos; ícono ⇧ habilitado
                └─ Click ícono ⇧ → window.open('/localidades') (JS puntual)
                      └─ Página /localidades (Thymeleaf, server-side)
                            └─ Usuario filtra/selecciona fila
                                  └─ JS: window.opener.setLocalidad(cod, nom, prov); window.close()
                                        └─ Formulario padre actualiza los 3 campos
```

---

## 2. Tabla de paridad legacy → JPA

**Sin cambios**: el módulo reutiliza las entidades y repositorios ya mapeados en
`01-nucleo-datos`. Se listan por referencia.

| Trn legacy | Tabla física | Entidad JPA | @Table | Columnas clave (ya mapeadas) |
|---|---|---|---|---|
| `Localidades` | `LOCALIDA` | `nucleo.domain.Localidad` | `@Table(name="Localida")` | `LocCod` (PK, int, NOT NULL), `LocNom` (char 50), `LocNomProv` (char 19), `LocCodPost` (smallint), `ProvCod` (smallint, FK lógica), `LocCodAfip` (int, NOT NULL, huérfana, default 0) |
| `Provincia` | `PROVINCI` | `nucleo.domain.Provincia` | `@Table(name="PROVINCI")` | `ProvCod` (PK, smallint, NOT NULL), `ProvNombre` (varchar 40, NOT NULL), `ProvCodAfip` (int, NOT NULL, huérfana, default 0) |

**Columnas huérfanas (ya mapeadas, sin rediseño)**:
- `LOCALIDA.LocCodAfip` → default aplicación `0`.
- `PROVINCI.ProvCodAfip` → default aplicación `0`.

**Defaults de aplicación exigidos por NOT NULL**:
- `LocCodAfip = 0` (ya en la entidad).
- `ProvCodAfip = 0` (ya en la entidad).

No se genera DDL ni migraciones (G5, `ddl-auto=none`).

---

## 3. Contratos REST

**Ninguno nuevo.** El módulo es MVC-only (paridad con D-ACTA-04 y D-LOC-05). La colección
Postman (`docs/postman/ATPPuestos-API.postman_collection.json`) **no se modifica**.

Si en el futuro se agrega un endpoint REST para el popup, G-POSTMAN obliga a actualizar la
colección en el mismo commit.

---

## 4. Contrato de vistas (Thymeleaf)

### 4.1 Página nueva: `/localidades`

| Atributo | Valor |
|---|---|
| Ruta de página | `/localidades` |
| Plantilla | `templates/puestos/localidades-seleccion.html` |
| Extiende | `layout/base` (fragmentos `cabecera`, `sesion`, `navegacion`, `pie`) |
| Controlador | `LocalidadesController` (`@Controller`) |
| Método HTTP | `GET` (listado/paginación/filtros), `POST` (selección, si aplica) |

**View model** (`LocalidadesSeleccionModel` o atributos del modelo):

| Campo | Tipo Java | Origen | Descripción |
|---|---|---|---|
| `localidades` | `Page<Localidad>` | `LocalidadRepository.findAllConProvincia()` o filtrado | Grilla paginada |
| `filtroLocCod` | `Integer` | query param | Filtro código (>=) |
| `filtroLocNom` | `String` | query param | Filtro nombre (LIKE) |
| `filtroProvNombre` | `String` | query param | Filtro provincia (LIKE) |
| `filtroLocCodPost` | `Integer` | query param | Filtro código postal (>=) |
| `page` | `int` | query param (default 0) | Página actual |
| `size` | `int` | query param (default 5) | Tamaño de página |
| `totalPages` | `int` | `Page.getTotalPages()` | Total de páginas |
| `totalElements` | `long` | `Page.getTotalElements()` | Total de elementos |

**Reglas de validación visibles**:
- Los filtros son opcionales (todos nullable).
- El código de localidad y el código postal son numéricos (validación `@Min(0)` o binding).
- Si el binding falla, re-renderizar con los valores previos y mensaje de error por campo.

**Comportamiento ante error**:
- `403` si no hay sesión autenticada (redirección a login).
- `400` si los filtros tienen formato inválido (re-render con errores).
- `500` sólo ante error interno (no esperado).

**Columnas de la grilla** (paridad con legacy `Grid1`):

| # | Encabezado | Atributo | Visible | Clickable |
|---|---|---|---|---|
| 1 | (selector) | `&LinkSelection` (ícono selectRow) | Sí | Sí (ReturnOnClick) |
| 2 | `Cod. Loc.` | `LocCod` | Sí | No |
| 3 | (nombre, título contextual) | `LocNom` | Sí | Sí (ReturnOnClick) |
| 4 | (provincia oculta) | `LocNomProv` | **No** (paridad `ColVisible=False`) | No |
| 5 | `Provincia` | `ProvNombre` | Sí | No |
| 6 | (código postal) | `LocCodPost` | Sí | No |

**Filtros** (Fieldset `Filters`):

| Campo | Variable legacy | Tipo | Filtro SQL |
|---|---|---|---|
| Código de localidad | `&cLocCod` | NUMERIC 5 | `LocCod >= :cLocCod` |
| Nombre | `&cLocNom` | CHAR 20 | `LocNom LIKE :cLocNom` |
| Provincia | `&cLocNomProv` | CHAR 15 | `LocNomProv LIKE :cLocNomProv` (o `ProvNombre LIKE`) |
| Código postal | `&cLocCodPost` | NUMERIC 4 | `LocCodPost >= :cLocCodPost` |

**Acciones**:
- `Seleccionar` (ReturnOnClick en columnas 1 y 3): asigna los 3 valores al `window.opener` y cierra.
- `Cancelar`: cierra la ventana popup sin seleccionar.

### 4.2 Modificación de `/actas/nueva` (acta-formulario.html)

Se agrega un ícono ⇧ junto al campo `Localidad` de cada fieldset de parte (Vendedor,
Destinatario, Transportista). El ícono es un `<button type="button">` con una clase CSS
`localidad-popup-trigger` y un atributo `data-popup-enabled` que se setea server-side según
exista localidad derivada.

**Reglas visibles**:
- Si `LocCod` está vacío (CUIT no existente): `data-popup-enabled="true"`, clase `is-enabled`,
  cursor pointer, click abre el popup.
- Si `LocCod` tiene valor (CUIT existente): `data-popup-enabled="false"`, clase `is-disabled`,
  `aria-disabled="true"`, `disabled`, sin acción.

**JS puntual** (`static/js/puestos/localidades-popup.js`):
- Listener en `.localidad-popup-trigger[data-popup-enabled="true"]` → `window.open('/localidades', 'localidadesPopup', 'width=800,height=600')`.
- Función global `window.setLocalidad(locCod, locNom, provNombre)` que actualiza los campos del
  formulario padre y cierra el popup.

---

## 5. Contrato de la página `/localidades` (controlador)

### 5.1 `LocalidadesController`

```java
@Controller
@RequestMapping("/localidades")
public class LocalidadesController {
    @GetMapping
    public String listar(@RequestParam(required=false) Integer filtroLocCod,
                         @RequestParam(required=false) String filtroLocNom,
                         @RequestParam(required=false) String filtroProvNombre,
                         @RequestParam(required=false) Integer filtroLocCodPost,
                         @RequestParam(defaultValue="0") int page,
                         @RequestParam(defaultValue="5") int size,
                         Model model, HttpSession session) { ... }

    // Selección vía JS (window.opener), no POST server-side.
}
```

**Comportamiento**:
- Verifica sesión autenticada; si no, redirige a login (302).
- Aplica los filtros del legacy (LocCod >=, LocNom LIKE, LocNomProv/ProvNombre LIKE, LocCodPost >=).
- Pagina con `Pageable` (size default 5, sort LocCod asc).
- Renderiza `puestos/localidades-seleccion`.

### 5.2 Integración con `ActaFormularioController`

El controlador del formulario de alta debe exponer en el modelo un flag por cada parte indicando si
la localidad fue derivada (LocCod != null). Este flag se usa en la plantilla para habilitar/deshabilitar
el ícono ⇧.

---

## 6. Decisiones

| # | Decisión | Justificación |
|---|---|---|
| D-LOC-01 | Popup como página Thymeleaf en ventana nueva (`window.open`). | Paridad con `prompt()` del legacy; mantiene la lógica server-side (G7). |
| D-LOC-02 | Reutilizar entidades/repositorios sin modificación. | Ya mapeados y probados. |
| D-LOC-03 | Selección vía `window.opener.setLocalidad(...)` + `window.close()`. | Patrón estándar para popups; evita recarga. |
| D-LOC-04 | Habilitación basada en LocCod vacío. | Evita cambios en el DTO de 09. |
| D-LOC-05 | Sin endpoints REST nuevos. | Paridad con D-ACTA-04; no hay consumidor API. |
| D-LOC-06 | Provincia como label (formulario) / columna (popup). | Paridad con legacy. |
| D-LOC-07 | Paginación default 5. | Paridad con `Grid1.MaxRows=5`. |
| D-LOC-08 | Sin modificación de entidades/repositorios. | Ya existen los métodos necesarios. |

---

## 7. Supuestos abiertos

Ver spec §6. Los supuestos SA-LOC-01 a SA-LOC-08 quedan abiertos hasta validación.

---

## 8. Alcance excluido

- Modificación de `Localidad`, `Provincia`, `LocalidadRepository`.
- Endpoints REST (`/api/v1/localidades`).
- Cambios en el esquema de BD.
- Impresión de localidades.
- Combo de provincia en el popup (paridad con legacy: filtro por texto).
- Modal bloqueante (paridad deprecada: ventana nueva no modal).