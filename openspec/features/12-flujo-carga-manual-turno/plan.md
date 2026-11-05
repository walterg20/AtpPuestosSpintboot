# Plan — 12 Flujo de Carga Manual de Turno (Puestos Camineros)

> **Estado**: `IMPLEMENTADO (2026-11-04)` — Gate 2 PASS.
> Documento de arquitectura. No contiene tareas (ver `tasks.md`).
> **Fuente**: KB `ATPPuestos` (solo lectura) + `Rentas` `INFORMATION_SCHEMA` (G-DATA 2026-11-04).
> Requisitos: `spec.md` (`R-TURNO-*`). Decisiones: `spec.md` §7 (`D-TURNO-*`).
> **Dependencia**: `09-formularios-acta` (formularios, servicio, entidades y plantilla reutilizados).

---

## 1. Estructura del módulo (delta sobre `09-formularios-acta`)

```text
atp-puestos-camineros/
└── src/main/java/ar/com/atp/puestos/puestos/
    ├── service/
    │   └── ActaService.java            (se EXTENDEN: validarNumeroManual, obtenerTipoActaManual,
    │                                    y guardar(... ) gana el parámetro `manual`)
    └── web/
        └── mvc/
            ├── ActasController.java     (se EXTENDEN: GET /actas/nueva acepta turNroId+manual;
            │                             POST /actas/guardar acepta manual+turNroId)
            └── (NUEVO) CargaManualController.java  (GET /actas/carga-manual)
atp-puestos-camineros/
└── src/main/resources/
    ├── templates/puestos/
    │   ├── turnos-abiertos.html        (NUEVO — listado de turnos abiertos)
    │   └── acta-formulario.html        (se ajusta: F276Id editable si manual=true)
    └── static/js/
        └── actas.js                    (se ajusta: mostrar/ocultar F276Id según modo)
atp-nucleo-compartido/
└── src/main/java/ar/com/atp/puestos/nucleo/repository/
    └── TurnoRepositorio.java          (se EXTENDEN: listarAbiertosPorSucursal(...))
```

> **No se crean** entidades JPA nuevas: se reutilizan `nucleo.domain.Turno` y
> `puestos.domain.RangoManualFormulario` (de `09-formularios-acta`).

### 1.1 Paquetes y dependencias

| Capa | Paquete | Puede depender de |
|---|---|---|
| Dominio | `ar.com.atp.puestos.puestos.domain` | nada (G7) |
| Repositorio (núcleo) | `ar.com.atp.puestos.nucleo.repository` | `domain` |
| Servicio | `ar.com.atp.puestos.puestos.service` | `domain`, `repository`, `nucleo.service` |
| Web MVC | `ar.com.atp.puestos.puestos.web.mvc` | `service`, `nucleo.web.mvc` |

**Prohibido**: `web → repository`, `domain → web`, lógica de negocio en el controller (G7).

---

## 2. Paridad JPA — sin tablas nuevas

No se agregan entidades. Se reutilizan:

| Tabla | Entidad existente | Uso en este módulo |
|---|---|---|
| `PPRIM_TU` | `nucleo.domain.Turno` | Listado de turnos abiertos (R-TURNO-002, R-TURNO-005) y validación de estado (R-TURNO-004, R-TURNO-016). |
| `FORMSUCU` | `puestos.domain.RangoManualFormulario` | Validación del número manual y derivación de la clasificación (R-TURNO-010, R-TURNO-011). `ForRecFin` se deriva (`ForRecIni + ForRecCant - 1`). |

Columnas huérfanas: ninguna nueva. `ForRecFin` **no** se mapea (fórmula, ver `spec.md` §3.5).

### 2.1 Default de aplicación

Sin defaults nuevos. Se reutilizan los de `09-formularios-acta` §2.6.

---

## 3. Contratos

### 3.1 MVC — rutas, métodos y atributos de modelo

| Método | Ruta | Plantilla / destino | Atributos del `Model` | Notas |
|---|---|---|---|---|
| `GET` | `/actas/carga-manual` | `puestos/turnos-abiertos` | `turnos` (`Page<Turno>` o `List<Turno>`), `contexto`, `programas`, `rutaActual` | Listado de turnos abiertos filtrado por sucursal/tipo (R-TURNO-002, R-TURNO-005). |
| `GET` | `/actas/nueva` | `puestos/acta-formulario` | (los de `09` +) `manual` (`boolean`), `turNroId` (`Integer`) | **Se extiende** el endpoint existente: acepta `turNroId` y `manual` opcionales (R-TURNO-003, R-TURNO-008). Si `manual=true`, el `TurNroId` se toma del parámetro; si no, del contexto (compatibilidad con `09`). |
| `POST` | `/actas/guardar` | `puestos/acta-formulario` (200) ó `redirect:/actas` (302) | (los de `09` +) `manual`, `turNroId` | **Se extiende** el endpoint existente: acepta los campos ocultos `manual` y `turNroId` (R-TURNO-012, R-TURNO-013). |

**Parámetros de consulta de `GET /actas/carga-manual`**

| Parámetro | Tipo | Semántica |
|---|---|---|
| `page` | `int` (0-based) | `Pageable` (si se pagina; ver SA-TURNO-03) |
| `size` | `int` (default **10**) | `Pageable` |

### 3.2 View models

Se reutilizan los de `09-formularios-acta` §3.3. Extensiones:

```java
// service/model/ActaAlta.java — sin cambios; el servicio sabe si es manual por el flag del controller

// web/mvc/form/ActaForm.java — se AGREGAN dos campos (binding, no negocio):
public class ActaForm {
    // ... (campos existentes de 09) ...
    private boolean manual;          // true ⇒ modo manual (R-TURNO-008)
    private Integer turNroId;         // turno seleccionado (sólo lectura en la vista; R-TURNO-003)
    // f276Id ya existe en ActaForm? En 09 no se bindea (oculto). En modo manual se bindea y valida.
}
```

> `ActaForm.f276Id` (Nro. Acta) **ya** está en la entidad `Acta` (`f276Id`). En `09` el campo se
> oculta en la vista y el servicio lo genera. En modo manual se muestra y se valida
> (R-TURNO-008…R-TURNO-010).

### 3.3 Contrato del servicio `ActaService` (extensiones)

```java
// service/ActaService.java — se AGREGAN (sin cambiar las firmas existentes):
@Transactional(readOnly = true)
boolean esTurnoAbierto(Integer sucNroId, String sucTipo, Integer turNroId);
    // PPrim_TurnoEstado ⇒ 'A' (R-TURNO-004, R-TURNO-016)

@Transactional(readOnly = true)
List<RangoManualFormulario> rangosManuales(Integer sucNroId, String sucTipo);
    // FORMSUCU por sucursal/tipo (R-TURNO-010, R-TURNO-011)

@Transactional(readOnly = true)
boolean validarNumeroManual(Integer sucNroId, String sucTipo, Integer f276Id,
        String clasif);   // ForRecIni <= f276Id <= ForRecIni + ForRecCant - 1
                           // y ForRecTipo acorde a clasif (R-TURNO-010)

@Transactional(readOnly = true)
String obtenerTipoActaManual(Integer sucNroId, String sucTipo, Integer f276Id);
    // 'ACTI' ⇒ 'E', 'ACTS' ⇒ 'S' (R-TURNO-011)

// guardar(...) gana un flag `manual` (o se detecta por ActaForm.manual):
@Transactional
ResultadoAltaActa guardar(ActaAlta alta, ContextoSesion ctx, boolean manual);
    // si manual=true: NO llama a ServicioNumeracionActa; usa alta.f276Id;
    //                TurNroId = alta.turNroId (turno seleccionado);
    //                valida número manual (R-TURNO-009, R-TURNO-010).
    // si manual=false: comportamiento idéntico a 09 (R-TURNO-014).
```

### 3.4 Contrato del repositorio `TurnoRepositorio` (extensión)

```java
// nucleo/repository/TurnoRepositorio.java — se AGREGA (lectura, sin DDL):
@Query("select t from Turno t where t.id.sucNroId = :sucNroId "
        + "and t.id.sucTipo = :sucTipo and t.turEstado = 'A' "
        + "order by t.id.sucNroId asc, t.id.sucTipo asc, t.id.turNroId asc")
Page<Turno> listarAbiertosPorSucursal(@Param("sucNroId") Integer sucNroId,
        @Param("sucTipo") String sucTipo, Pageable pageable);
```

Equivalente al `ControlWhere` de `TurnosListadoAbiertos` (`TurEstado='A'; SucTipo=...; SucNroId=...`).

---

## 4. Contrato de vistas (Thymeleaf)

### 4.1 Rutas y plantillas

| Ruta | Plantilla | Fragmentos incluidos |
|---|---|---|
| `/actas/carga-manual` | `puestos/turnos-abiertos` | `layout/base :: head/cabecera/sesion/navegacion/pie`, `fragments/menu :: menu` |
| `/actas/nueva?manual=true` | `puestos/acta-formulario` (reutilizada) | ídem + `puestos/fragments/acta-productos :: grilla` |

### 4.2 `turnos-abiertos.html` — estructura

```text
<main class="atp-pagina atp-turnos-abiertos__page">
  <header class="atp-turnos-abiertos__cabecera">
    <h1 class="atp-pagina__titulo">Seleccione un Turno</h1>
  </header>

  <table class="atp-table" th:if="${not #lists.isEmpty(turnos.content)}">
    <thead><tr>
      <th scope="col">Turno</th>
      <th scope="col">Fecha apertura</th>
      <th scope="col">Seleccionar</th>
    </tr></thead>
    <tbody>
      <tr th:each="t : ${turnos.content}">
        <td th:text="${t.id.turNroId}">0</td>
        <td th:text="${t.turFchApe}">—</td>
        <td>
          <a class="atp-btn atp-btn--primary"
             th:href="@{/actas/nueva(turNroId=${t.id.turNroId},manual=true)}">Seleccionar</a>
        </td>
      </tr>
    </tbody>
  </table>

  <p class="atp-vacio" th:if="${#lists.isEmpty(turnos.content)}">Sin turnos abiertos.</p>

  <!-- Paginación (si aplica; ver SA-TURNO-03) -->
</main>
```

**Reglas de la vista**
| # | Regla | Requisito |
|---|---|---|
| V-T1 | El título es `Seleccione un Turno` (paridad con el legacy). | R-TURNO-002 |
| V-T2 | La grilla sólo se renderiza si hay turnos; si no, estado vacío. | R-TURNO-006, R-TURNO-015 |
| V-T3 | Cada fila tiene un enlace `Seleccionar` que navega a `/actas/nueva?turNroId=...&manual=true`. | R-TURNO-003 |

### 4.3 Ajuste de `acta-formulario.html` (modo manual)

Se agrega, **sólo si `manual = true`**, el campo `Nro. Acta` (`F276Id`) editable en el fieldset
"Datos del Acta", con su `label` y validación. En modo manual se oculta el selector de generación
automática (no aplica). El resto de la plantilla es idéntico a `09-formularios-acta` §4.3.

```text
<!-- Sólo en modo manual -->
<div class="atp-field" th:if="${manual}">
  <label for="f276Id" class="atp-field__etiqueta">Nro. Acta</label>
  <input id="f276Id" type="number" class="atp-field__control"
         th:field="*{f276Id}" th:errorclass="atp-field__control--error">
  <p class="atp-field__error" th:if="${#fields.hasErrors('f276Id')}"
     th:errors="*{f276Id}" role="alert"></p>
</div>
<!-- Campos ocultos para preservar el modo -->
<input type="hidden" th:field="*{manual}">
<input type="hidden" th:field="*{turNroId}">
```

### 4.4 Comportamiento ante error

| Situación | Comportamiento | Código HTTP |
|---|---|---|
| Sin sesión | Redirección al login. | 302 |
| CSRF inválido | Rechazo del `POST`. | 403 |
| Turno cerrado / inexistente | Rechazo con `No se puede generar la guia porque el turno asociado esta cerrado`. | 200 |
| Número manual inválido (`0` o fuera de rango) | Rechazo con el mensaje verbatim correspondiente. | 200 |
| Error de binding (`@Valid`) | Re-render con `th:errors` por campo. | 200 |
| Regla de negocio violada | Re-render con banner del mensaje verbatim. | 200 |
| Éxito | `redirect:/actas` con flash `mensajeExito`. | 302 |

---

## 5. Matriz de paridad con el legacy

| Operación legacy | Requisito | Implementación | Prueba |
|---|---|---|---|
| Menú → `TurnosListadoAbiertos` | R-TURNO-001 | `CargaManualController` + menú | `MvcMenuCargaManual` |
| Grilla turnos abiertos | R-TURNO-002, R-TURNO-005 | `TurnoRepositorio.listarAbiertosPorSucursal` | `TurnosAbiertosIT` |
| Selección → `PPActaDetalle(TurNroId,'M')` | R-TURNO-003 | `/actas/nueva?manual=true` | `MvcSeleccionTurno` |
| Turno cerrado | R-TURNO-004, R-TURNO-016 | `ActaService.esTurnoAbierto` | `ActaServiceTest` |
| Estado vacío | R-TURNO-006, R-TURNO-015 | vista `turnos-abiertos` | `MvcTurnosAbiertosVacio` |
| `F276Id` editable + validación | R-TURNO-008…R-TURNO-011 | `ActaForm` + `ActaService` | `MvcFormularioManual` |
| Sin numeración automática | R-TURNO-013 | `ActaService.guardar(manual=true)` | `ActaManualIT` |
| Reutilización de `09` | R-TURNO-007, R-TURNO-014 | `ActaService` (sin duplicar) | suites de `09` en verde |

---

## 6. Decisiones

Ver `spec.md` §7 (`D-TURNO-01` … `D-TURNO-07`). Resumen operativo:

| # | Decisión | Efecto |
|---|---|---|
| D-TURNO-01 | El flujo de "Carga Manual" exige seleccionar un turno abierto (verificado SÍ). | Se agrega `/actas/carga-manual`. |
| D-TURNO-02 | Se reutiliza la misma plantilla y servicio con flag `manual`. | Un solo `ActaForm` con `manual` y `turNroId`. |
| D-TURNO-03 | `F276Id` editable en modo manual, validado contra `FORMSUCU` (rango inclusivo). | `ActaService` agrega `validarNumeroManual` y `obtenerTipoActaManual`. |
| D-TURNO-04 | Sin numeración automática (`GES_SUCN`) en modo manual. | `guardar(..., manual=true)` omite `ServicioNumeracionActa`. |
| D-TURNO-05 | `TurNroId` persistido proviene del turno seleccionado. | `BeforeInsert` rama `else`. |
| D-TURNO-06 | Sin endpoints REST nuevos. | Postman sin cambios. |
| D-TURNO-07 | Se reutiliza/extiende `TurnoRepositorio` (sólo lectura). | Método `listarAbiertosPorSucursal`. |

---

## 7. Supuestos abiertos

Ver `spec.md` §6 (SA-TURNO-01…SA-TURNO-05).

---

## 8. Alcance excluido

- Impresión del acta y reportes PDF.
- WS `WSActa` (F506 / multa).
- Cierre de turno y lotes (`06-turnos`).
- Endpoints REST nuevos.
- E2E automatizado.