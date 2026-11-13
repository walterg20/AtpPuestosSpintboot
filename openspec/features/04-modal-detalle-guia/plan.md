# Plan — 04 Modal "Detalle de Carta de Porte / Guía de Tránsito" (Thymeleaf)

> **Estado**: `PROPUESTA` — pendiente de aprobación en la puerta **G-SPEC**.
> Acompaña a `spec.md` (misma puerta). Documento de arquitectura; no contiene tareas (ver `tasks.md`).
> **Paridad de BD**: `ddl-auto=none`, sin DDL, sin migraciones (G5). Las columnas de `DETPRODP`
> ya están mapeadas en `DetalleProductoPrimario` (entidad existente); **no se agregan**.
> **G-POSTMAN**: no aplica (componente puramente frontend; sin cambios en `/api/v1/**`).

---

## 1. Estrategia y ubicación

### 1.1 Qué produce este módulo

La **capa de presentación** del modal "Detalle de Guía", como evolución visual del fragmento
legacy `form506-detalle.html`. El backend **no se rediseña**: se amplía `CargaForm` con campos
de presentación (con fallbacks) y se poblán en `CargaMvcController`.

```text
DETPRODP (BD, sólo lectura) → DetalleProductoPrimario (JPA, ya mapeado)
                                    ↓
                          CargaService / CargaMvcController
                                    ↓
                          CargaForm (ampliado) + control-cargas.html
                                    ↓
                          fragments/modal-detalle-guia.html  (th:fragment="modalDetalleGuia")
                          static/css/modal-detalle-guia.css   (tokens --stitch-*)
                          static/js/modal-detalle-guia.js     (vanilla)
```

### 1.2 Dónde vive el código

| Artefacto | Ubicación | Razón |
|---|---|---|
| Form object ampliado | `atp-puestos-camineros/src/main/java/ar/com/atp/puestos/puestos/web/mvc/form/CargaForm.java` | Form-backing existente del módulo `control-cargas`. |
| Controller MVC | `atp-puestos-camineros/src/main/java/ar/com/atp/puestos/puestos/web/mvc/CargaMvcController.java` | Ya expone `/cargas/detalle`; se amplía la población del form. |
| Fragmento Thymeleaf | `atp-puestos-camineros/src/main/resources/templates/puestos/fragments/modal-detalle-guia.html` | Mismo paquete que el fragmento legacy; prefijo por módulo (D-03). |
| Plantilla principal | `atp-puestos-camineros/src/main/resources/templates/puestos/control-cargas.html` | Integración del include (R-MDG-17). |
| CSS del modal | `atp-puestos-camineros/src/main/resources/static/css/modal-detalle-guia.css` | Estilos propios, sólo tokens `--stitch-*`. |
| JS del modal | `atp-puestos-camineros/src/main/resources/static/js/modal-detalle-guia.js` | JS puntual vanilla (regla 8). |
| Fragmento legacy (deprecado) | `atp-puestos-camineros/src/main/resources/templates/puestos/fragments/form506-detalle.html` | Se mantiene, deja de usarse (D-MDG-06). |

> **El módulo desplegable `atp-puestos-camineros` es el único artefacto que se modifica.** El
> núcleo (`atp-nucleo-compartido`) no se toca: no hay nuevos endpoints ni entidades.

### 1.3 Los cimientos de vista ya existen

A diferencia de `02-abm-productos`, este módulo **no** crea shell de vistas: reutiliza el
layout `base-stitch`, `stitch-tokens.css` y los estilos de `control-cargas.css` ya existentes.
**No se modifican** (R-MDG-25).

---

## 2. (a) Paridad JPA — `DETPRODP` (sin cambios)

> **No se modifica el esquema ni la entidad.** `DetalleProductoPrimario` ya mapea `DETPRODP`
> con `ddl-auto=none`. Este módulo **sólo lee** de la entidad a través de `CargaForm`.

### 2.1 Columnas consumidas por el modal (ya mapeadas)

| Campo del modal | Columna física | Tipo SQL | Nula | Tipo Java | Anotación (estado actual) | Default de aplicación |
|---|---|---|---|---|---|---|
| `chasis` | `DPPChasis` | `char(10)` | sí | `String` | `@Column(name="DPPChasis", length=10)` + `@ColumnTransformer(read="RTRIM(DPPChasis)")` | `""` (fallback en controller) |
| `acoplado` | `DPPAcopla` | `char(10)` | sí | `String` | `@Column(name="DPPAcopla", length=10)` + `RTRIM` | `""` |
| `kmFlete` | `DPPKmFle` | `numeric(10,4)` | sí | `BigDecimal` | `@Column(name="DPPKmFle", precision=10, scale=4)` | `0` |
| `tarifaKm` | `DPPTarxKm` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPTarxKm", precision=19, scale=4)` | `0` |
| `montoFlete` | `DPPMImpFle` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPMImpFle", precision=19, scale=4)` | `0` |
| `alicuota` | `DPPAlicFle` | `numeric(10,4)` | sí | `BigDecimal` | `@Column(name="DPPAlicFle", precision=10, scale=4)` | `0` |
| `impuestoFlete` | `DPPImpuFle` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPImpuFle", precision=19, scale=4)` | `0` |
| `adicional10` | `DPPAdicFle` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPAdicFle", precision=19, scale=4)` | `0` |
| `subtotalFlete` | `DPPImIBTot` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPImIBTot", precision=19, scale=4)` | `0` |
| `subtotalTraslado` | `DPPAdIBTot` | `numeric(19,4)` | sí | `BigDecimal` | `@Column(name="DPPAdIBTot", precision=19, scale=4)` | `0` |
| `totalGeneral` | `@Transient` | — | — | `BigDecimal` | `@Transient` (calculado en servicio/vista) | `0` |
| `fiscalizador` | `DPPUltPaOp` | `char(10)` | sí | `String` | `@Column(name="DPPUltPaOp", length=10)` + `RTRIM` | contexto de sesión (usuario) → `""` |
| `hashControl` | derivado | — | — | `String` | `@Transient` (no persistido) | derivado determinista → `""` |
| `expedienteDigital` | `DPPActaNro` | `int` | sí | `Integer` | `@Column(name="DPPActaNro")` | `null` → `""` |
| `domicilioFiscal` | `DPPAnuObs` | `varchar(200)` | sí | `String` | `@Column(name="DPPAnuObs", length=200)` | `""` |
| `puestoIngreso` | `DPPActaPue` | `smallint` | sí | `Short` | `@Column(name="DPPActaPue")` | contexto de sesión (puesto) → `""` |
| `fechaControl` | `DPPFchMov` | `datetime` | sí | `LocalDateTime` | `@Column(name="DPPFchMov")` | `LocalDateTime.now()` → `null` |
| `tipoComprobante` | `DPPTipoGui` | `char(1)` | sí | `String` | `@Column(name="DPPTipoGui", length=1)` + `RTRIM` | `"F"` |

### 2.2 Defaults de aplicación que exige cada `NOT NULL` sin contraparte en el DTO

> `DETPRODP` no tiene columnas `NOT NULL` obligatorias adicionales que el modal exponga; todas las
> columnas consumidas son **nullable** en la BD. Los defaults `0`/`""` son de **presentación**
> (fallbacks en el controller), no de persistencia. **No se agregan defaults de BD.**

### 2.3 Huérfanas (ya mapeadas, no expuestas por el modal)

Las columnas huérfanas de `DETPRODP` (las que existen en la BD y no en la KB) **ya están
mapeadas** en `DetalleProductoPrimario` (ver `dppLocOr`, `dppLocDe`, `dppIncluid`, `dppCorGre`,
`dppUltPSuT`, `dppUltPSuN`, `ppTrataId`, etc.). El modal **no las expone** ni las modifica.

### 2.4 Regla de paridad

- **Sin DDL**: no se agregan columnas, no se alteran tipos, no se crean FK.
- **Sin cambios en la entidad**: `DetalleProductoPrimario` no se modifica.
- **Sin cambios en repositorios/servicios**: `CargaService` no cambia.
- Los fallbacks viven **sólo** en `CargaMvcController` (D-MDG-07).

---

## 3. (b) Contrato REST

> **No hay cambios en el contrato REST.** El modal es puramente frontend y consume el
> `CargaForm` ya poblado por `CargaMvcController` (rutas MVC `/cargas/*`).

### 3.1 Endpoints MVC existentes (sin cambios)

| Método | Ruta | Uso en el modal | Respuesta |
|---|---|---|---|
| GET | `/cargas` | Página principal (modal cerrado) | vista `puestos/control-cargas` |
| GET | `/cargas/detalle?dppNroForm=…&dpptipoGui=…&duplicado=…&conCuit=…` | Abre el modal (R-MDG-01) | vista `puestos/control-cargas` con `mostrarModal=true` |
| POST | `/cargas/marcar` | Marcar guía (éxito → modal `modalDetalleGuia` con banner verde; anulada/interna → sólo leyenda en alert superior) | vista `puestos/control-cargas` con `mostrarModal = (mensaje == MSG_EXITO)` |

### 3.2 G-POSTMAN

**No aplica**: no se agregan, modifican ni eliminan endpoints REST (`/api/v1/**`). La colección
Postman no cambia por este módulo.

---

## 4. (c) Contrato de vistas Thymeleaf

### 4.1 Rutas y plantillas

| Método | Ruta | Vista | Uso |
|---|---|---|---|
| GET | `/cargas` | `puestos/control-cargas` | Página principal |
| GET | `/cargas/detalle?…` | `puestos/control-cargas` | Página con modal abierto |

| Plantilla | Rol |
|---|---|
| `templates/puestos/control-cargas.html` | Página principal; incluye el modal (R-MDG-17). |
| `templates/puestos/fragments/modal-detalle-guia.html` | Fragmento `modalDetalleGuia` (nuevo). |
| `templates/puestos/fragments/form506-detalle.html` | Fragmento legacy `modal` (deprecado, se mantiene). |
| `templates/layout/base-stitch.html` | Layout base (no se modifica). |

### 4.2 View model (atributos del modelo)

El modal consume el atributo de modelo `cargaForm` (`CargaForm`), ya presente. No se agregan
atributos nuevos al modelo MVC.

### 4.3 Form object ampliado — `CargaForm`

Se añaden los campos listados en §2.1, todos **opcionales** y con fallbacks resueltos en el
controller. Getters/setters en camelCase (`getChasis`, `setKmFlete`, …).

```java
// Nuevos campos (todos opcionales)
private String chasis;
private String acoplado;
private BigDecimal kmFlete;
private BigDecimal tarifaKm;
private BigDecimal montoFlete;
private BigDecimal alicuota;
private BigDecimal impuestoFlete;
private BigDecimal adicional10;
private BigDecimal subtotalFlete;
private BigDecimal subtotalTraslado;
private BigDecimal totalGeneral;
private String fiscalizador;
private String hashControl;
private Integer expedienteDigital;
private String domicilioFiscal;
private String puestoIngreso;
private LocalDateTime fechaControl;
private String tipoComprobante;
```

> **No llevan validación Bean Validation** (son de sólo lectura / presentación). El controller
> aplica los fallbacks antes de renderizar.

### 4.4 Fallbacks en el controller (`CargaMvcController`)

En los métodos `detalle(...)` y `marcar(...)`, tras poblar los campos legacy, se resuelven los
campos del modal:

```java
// Pseudo-código (la implementación real va en tasks.md)
DetalleProductoPrimario dp = ...; // si está disponible
ContextoSesion ctx = contexto;

cargaForm.setChasis(nvlStr(dp, DetalleProductoPrimario::getDppChasis, ""));
cargaForm.setAcoplado(nvlStr(dp, DetalleProductoPrimario::getDppAcopla, ""));
cargaForm.setKmFlete(nvlBd(dp, DetalleProductoPrimario::getDppKmFle, BigDecimal.ZERO));
// ... (ver tabla §2.1)
cargaForm.setFiscalizador(resolveFiscalizador(dp, ctx)); // dppUltPaOp → ctx.usuario() → ""
cargaForm.setHashControl(resolveHash(dp, ctx));          // derivado determinista → ""
cargaForm.setExpedienteDigital(dp == null ? null : dp.getDppActaNro()); // null → "" en vista
cargaForm.setDomicilioFiscal(nvlStr(dp, DetalleProductoPrimario::getDppAnuObs, ""));
cargaForm.setPuestoIngreso(resolvePuesto(dp, ctx));     // dppActaPue → ctx puesto → ""
cargaForm.setFechaControl(dp == null || dp.getDppFchMov() == null ? LocalDateTime.now() : dp.getDppFchMov());
cargaForm.setTipoComprobante(resolveTipo(dp));           // dppTipoGui → dppTipoPr → "F"
```

> **`detalle(...)` actual no carga el `DetalleProductoPrimario`**: sólo recibe parámetros. La
> ampliación **puede** (opcional, SA-MDG-06) consultar la BD para poblar los campos del modal;
> si no, todos los campos caen en los fallbacks. Esta decisión se cierra en `tasks.md`.

### 4.5 Fragmento Thymeleaf — `modal-detalle-guia.html`

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<body>
  <div th:fragment="modalDetalleGuia"
       class="mdg-modal" id="modalDetalle" role="dialog" aria-modal="true"
       aria-labelledby="mdg-titulo" aria-describedby="mdg-descripcion"
       th:if="${cargaForm != null and cargaForm.mostrarModal != null and cargaForm.mostrarModal}">
    <div class="mdg-modal__backdrop" data-mdg-close></div>
    <div class="mdg-modal__panel" role="document">
      <header class="mdg-modal__header">
        <h2 id="mdg-titulo" class="mdg-modal__title">Detalle de Guía</h2>
        <button type="button" class="mdg-modal__close" data-mdg-close
                aria-label="Cerrar detalle de guía">
          <span class="material-symbols-outlined" aria-hidden="true">close</span>
        </button>
      </header>
      <p id="mdg-descripcion" class="mdg-sr-only">Detalle de la carta de porte / guía de tránsito seleccionada.</p>

      <!-- Cabecera -->
      <section class="mdg-seccion" aria-labelledby="mdg-sec-cabecera">
        <h3 id="mdg-sec-cabecera" class="mdg-seccion__titulo">Cabecera</h3>
        <dl class="mdg-dl">
          <div class="mdg-dl__row"><dt>Nro. formulario</dt><dd th:text="${cargaForm.dppNroForm}">—</dd></div>
          <div class="mdg-dl__row"><dt>CUIT</dt><dd th:text="${cargaForm.conCuit}">—</dd></div>
          <div class="mdg-dl__row"><dt>Razón social</dt><dd th:text="${cargaForm.conRazSoc}">—</dd></div>
          <div class="mdg-dl__row"><dt>Tipo de guía</dt><dd th:text="${cargaForm.dpptipoGui}">—</dd></div>
          <div class="mdg-dl__row"><dt>Tipo de comprobante</dt><dd th:text="${cargaForm.tipoComprobante}">—</dd></div>
          <div class="mdg-dl__row"><dt>Fecha de control</dt><dd th:text="${cargaForm.fechaControl != null} ? ${#temporals.format(cargaForm.fechaControl,'dd/MM/yyyy HH:mm')} : ''">—</dd></div>
          <div class="mdg-dl__row"><dt>Puesto de ingreso</dt><dd th:text="${cargaForm.puestoIngreso}">—</dd></div>
        </dl>
      </section>

      <!-- Fiscalización -->
      <section class="mdg-seccion" aria-labelledby="mdg-sec-fiscalizacion">
        <h3 id="mdg-sec-fiscalizacion" class="mdg-seccion__titulo">Fiscalización</h3>
        <dl class="mdg-dl">
          <div class="mdg-dl__row"><dt>Fiscalizador</dt><dd th:text="${cargaForm.fiscalizador}">—</dd></div>
          <div class="mdg-dl__row"><dt>Hash de control</dt><dd class="mdg-mono" th:text="${cargaForm.hashControl}">—</dd></div>
          <div class="mdg-dl__row"><dt>Expediente digital</dt><dd th:text="${cargaForm.expedienteDigital}">—</dd></div>
          <div class="mdg-dl__row"><dt>Domicilio fiscal</dt><dd th:text="${cargaForm.domicilioFiscal}">—</dd></div>
        </dl>
      </section>

      <!-- Flete -->
      <section class="mdg-seccion" aria-labelledby="mdg-sec-flete">
        <h3 id="mdg-sec-flete" class="mdg-seccion__titulo">Flete</h3>
        <dl class="mdg-dl mdg-dl--grid">
          <div class="mdg-dl__row"><dt>Chasis</dt><dd th:text="${cargaForm.chasis}">—</dd></div>
          <div class="mdg-dl__row"><dt>Acoplado</dt><dd th:text="${cargaForm.acoplado}">—</dd></div>
          <div class="mdg-dl__row"><dt>Km de flete</dt><dd th:text="${cargaForm.kmFlete}">0</dd></div>
          <div class="mdg-dl__row"><dt>Tarifa por km</dt><dd th:text="${#numbers.formatDecimal(cargaForm.tarifaKm,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row"><dt>Monto de flete</dt><dd th:text="${#numbers.formatDecimal(cargaForm.montoFlete,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row"><dt>Alícuota</dt><dd th:text="${#numbers.formatDecimal(cargaForm.alicuota,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row"><dt>Impuesto de flete</dt><dd th:text="${#numbers.formatDecimal(cargaForm.impuestoFlete,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row"><dt>Adicional 10%</dt><dd th:text="${#numbers.formatDecimal(cargaForm.adicional10,1,2)}">0,00</dd></div>
        </dl>
      </section>

      <!-- Totales -->
      <section class="mdg-seccion" aria-labelledby="mdg-sec-totales">
        <h3 id="mdg-sec-totales" class="mdg-seccion__titulo">Totales</h3>
        <dl class="mdg-dl">
          <div class="mdg-dl__row mdg-dl__row--total"><dt>Subtotal flete</dt><dd th:text="${#numbers.formatDecimal(cargaForm.subtotalFlete,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row mdg-dl__row--total"><dt>Subtotal traslado</dt><dd th:text="${#numbers.formatDecimal(cargaForm.subtotalTraslado,1,2)}">0,00</dd></div>
          <div class="mdg-dl__row mdg-dl__row--grand"><dt>Total general</dt><dd th:text="${#numbers.formatDecimal(cargaForm.totalGeneral,1,2)}">0,00</dd></div>
        </dl>
      </section>

      <!-- Mensajes legacy verbatim -->
      <div class="mdg-alert mdg-alert--success" role="status" aria-live="polite"
           th:if="${cargaForm.mensaje != null and cargaForm.mensaje == 'La guia ha sido marcada con exito'}">
        <p th:text="${cargaForm.mensaje}">La guia ha sido marcada con exito</p>
      </div>
      <div class="mdg-alert mdg-alert--warn" role="status" aria-live="polite"
           th:if="${cargaForm.duplicado != null and cargaForm.duplicado > 1}">
        <p>Guia Duplicada , ya paso por algun puesto</p>
      </div>

      <!-- Acciones -->
      <footer class="mdg-modal__footer">
        <button type="button" class="mdg-btn mdg-btn--secondary" data-mdg-print>
          <span class="material-symbols-outlined" aria-hidden="true">print</span>
          Imprimir
        </button>
        <button type="button" class="mdg-btn mdg-btn--secondary" data-mdg-download>
          <span class="material-symbols-outlined" aria-hidden="true">download</span>
          Descargar
        </button>
        <button type="button" class="mdg-btn mdg-btn--primary" data-mdg-close>
          Cerrar
        </button>
      </footer>
    </div>
  </div>
</body>
</html>
```

### 4.6 CSS — `modal-detalle-guia.css`

Reglas:
- **Sólo tokens `--stitch-*`**. Sin literales hex/rgb/rgba/nombres de color.
- Prefijo de clase `mdg-` (modal detalle guía) para evitar colisiones.
- Responsive: `@media (max-width: 640px)`.

```css
/* modal-detalle-guia.css — sólo tokens --stitch-*. */
.mdg-modal {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.mdg-modal__backdrop {
  position: absolute;
  inset: 0;
  background: var(--stitch-inverse-surface);
  opacity: 0.5;
}
.mdg-modal__panel {
  position: relative;
  background: var(--stitch-surface-container-lowest);
  border-radius: var(--stitch-radius-lg);
  box-shadow: var(--stitch-shadow-3);
  max-width: 60rem;
  width: 100%;
  max-height: 90vh;
  overflow: auto;
  padding: var(--stitch-space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--stitch-space-md);
}
.mdg-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--stitch-space-sm);
}
.mdg-modal__title {
  font-size: var(--stitch-headline-md-size);
  font-weight: var(--stitch-headline-md-weight);
  color: var(--stitch-primary);
  margin: 0;
}
.mdg-modal__close {
  background: transparent;
  border: none;
  color: var(--stitch-on-surface-variant);
  cursor: pointer;
  padding: var(--stitch-space-xs);
  border-radius: var(--stitch-radius);
}
.mdg-modal__close:hover { background: var(--stitch-surface-container-highest); }
.mdg-seccion { display: flex; flex-direction: column; gap: var(--stitch-space-xs); }
.mdg-seccion__titulo {
  font-size: var(--stitch-label-lg-size);
  font-weight: var(--stitch-label-lg-weight);
  color: var(--stitch-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: var(--stitch-label-lg-tracking);
  margin: 0;
}
.mdg-dl { margin: 0; display: flex; flex-direction: column; gap: var(--stitch-space-xs); }
.mdg-dl--grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--stitch-space-xs); }
.mdg-dl__row { display: flex; justify-content: space-between; gap: var(--stitch-space-sm); }
.mdg-dl__row dt { font-weight: 600; color: var(--stitch-on-surface); }
.mdg-dl__row dd { margin: 0; color: var(--stitch-on-surface-variant); }
.mdg-dl__row--total { border-top: 1px solid var(--stitch-outline-variant); padding-top: var(--stitch-space-xs); }
.mdg-dl__row--grand { font-weight: 700; color: var(--stitch-primary); }
.mdg-mono { font-family: monospace; font-size: var(--stitch-data-mono-size); }
.mdg-alert { padding: var(--stitch-space-sm); border-radius: var(--stitch-radius); font-size: var(--stitch-body-sm-size); }
.mdg-alert--success { background: var(--stitch-on-tertiary-container); color: var(--stitch-on-tertiary); }
.mdg-alert--warn { background: var(--stitch-warning); color: var(--stitch-on-primary); }
.mdg-modal__footer {
  display: flex;
  gap: var(--stitch-space-sm);
  justify-content: flex-end;
  padding-top: var(--stitch-space-sm);
  border-top: 1px solid var(--stitch-outline-variant);
}
.mdg-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--stitch-space-xs);
  padding: var(--stitch-space-xs) var(--stitch-space-md);
  border-radius: var(--stitch-radius);
  border: none;
  cursor: pointer;
  font-size: var(--stitch-label-md-size);
  font-weight: var(--stitch-label-md-weight);
}
.mdg-btn--primary { background: var(--stitch-primary); color: var(--stitch-on-primary); }
.mdg-btn--primary:hover { background: var(--stitch-primary-container); }
.mdg-btn--secondary { background: var(--stitch-surface-container); color: var(--stitch-on-surface-variant); }
.mdg-btn--secondary:hover { background: var(--stitch-surface-container-high); }
.mdg-sr-only {
  position: absolute;
  width: 1px; height: 1px;
  padding: 0; margin: -1px;
  overflow: hidden; clip: rect(0,0,0,0);
  white-space: nowrap; border: 0;
}
@media (max-width: 640px) {
  .mdg-modal__panel { max-width: 100%; margin: var(--stitch-space-sm); }
  .mdg-dl--grid { grid-template-columns: 1fr; }
  .mdg-modal__footer { flex-direction: column; }
  .mdg-btn { width: 100%; justify-content: center; }
}
```

> **Nota**: `font-family: monospace` y `border-top: 1px solid` son propiedades, no colores;
> los colores provienen de tokens. El test de tokens (R-MDG-20) sólo busca literales de color.

### 4.7 JS — `modal-detalle-guia.js`

Alcance **estrictamente mecánico** (R-MDG-22, regla 8):

| Operación | Qué hace | Qué **no** hace |
|---|---|---|
| Abrir (ya abierto por servidor) | Enfoca el primer elemento focuseable del panel; guarda el disparador. | No llama al servidor, no cambia el DOM estructural. |
| Cerrar (`data-mdg-close`) | Oculta el modal (`display:none` / clase), restaura el foco al disparador. | No navega, no recarga. |
| `Escape` | Cierra el modal (equivalente). | — |
| Click en backdrop | Cierra el modal. | — |
| Tabulación | Atrapa el foco dentro del panel (cíclica). | — |
| Imprimir (`data-mdg-print`) | Llama `window.print()`. | No genera PDF, no llama al servidor. |
| Descargar (`data-mdg-download`) | Muestra un `alert`/toast informativo ("Descarga disponible próximamente"). | No descarga archivos, no llama al servidor. |

```js
// modal-detalle-guia.js (vanilla, sin imports, sin fetch)
(function () {
  'use strict';
  function init() {
    var modal = document.getElementById('modalDetalle');
    if (!modal) return;
    var trigger = document.activeElement;
    var focusables = function () {
      return modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    };
    function close() {
      modal.style.display = 'none';
      if (trigger && typeof trigger.focus === 'function') trigger.focus();
    }
    modal.querySelectorAll('[data-mdg-close]').forEach(function (el) {
      el.addEventListener('click', close);
    });
    var backdrop = modal.querySelector('.mdg-modal__backdrop');
    if (backdrop) backdrop.addEventListener('click', close);
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') close();
      if (e.key === 'Tab') {
        var list = Array.prototype.slice.call(focusables());
        if (!list.length) return;
        var first = list[0], last = list[list.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    });
    var printBtn = modal.querySelector('[data-mdg-print]');
    if (printBtn) printBtn.addEventListener('click', function () { window.print(); });
    var dlBtn = modal.querySelector('[data-mdg-download]');
    if (dlBtn) dlBtn.addEventListener('click', function () {
      window.alert('Descarga disponible próximamente.');
    });
    // autofocus
    var firstFocusable = focusables()[0];
    if (firstFocusable) firstFocusable.focus();
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
```

### 4.8 Integración en `control-cargas.html`

Cambios mínimos:

1. Reemplazar el include legacy:
   ```html
   <!-- antes -->
   <div th:if="${cargaForm != null and cargaForm.mostrarModal != null and cargaForm.mostrarModal}">
     <div th:replace="~{puestos/fragments/form506-detalle :: modal}"></div>
   </div>
   <!-- después -->
   <div th:if="${cargaForm != null and cargaForm.mostrarModal != null and cargaForm.mostrarModal}">
     <div th:replace="~{puestos/fragments/modal-detalle-guia :: modalDetalleGuia}"></div>
   </div>
   ```

2. Agregar carga de assets (al final del `<body>`, junto a los existentes):
   ```html
   <link rel="stylesheet" th:href="@{/css/modal-detalle-guia.css}">
   <script th:src="@{/js/modal-detalle-guia.js}" defer></script>
   ```

> **No se modifican** `stitch-tokens.css`, `control-cargas.css` ni el layout base.

---

## 5. (d) Decisiones

| # | Decisión | Justificación |
|---|---|---|
| **D-MDG-01** | El modal se renderiza por servidor (`mostrarModal=true`), no por AJAX | Paridad legacy + R-ALC-006 (render por servidor). |
| **D-MDG-02** | Los campos del modal se añaden a `CargaForm` (form-backing) | Reutiliza el form existente y el flujo `/cargas/detalle`. |
| **D-MDG-03** | `hashControl` es derivado determinista (placeholder) | No hay endpoint ni columna dedicada. |
| **D-MDG-04** | "Descargar" es placeholder (sin endpoint) | Fuera de alcance (§2.2). |
| **D-MDG-05** | "Imprimir" usa `window.print()` | Sin librería de PDF. |
| **D-MDG-06** | El fragmento legacy se mantiene deprecado | Evita romper tests existentes. |
| **D-MDG-07** | Los fallbacks se resuelven en el controller | La vista sólo renderiza; el servicio no cambia. |
| **D-MDG-08** | Las clases CSS usan prefijo `mdg-` | Evita colisiones con `atp-*` y `stitch-*`. |
| **D-MDG-09** | El JS usa IIFE y `data-mdg-*` para hooks | No invasivo, sin dependencias. |
| **D-MDG-10** | Los importes se formatean en el servidor (Thymeleaf `#numbers`) | Paridad y accesibilidad (SA-MDG-02). |

---

## 6. Supuestos explícitos

| ID | Supuesto | Se resuelve en |
|---|---|---|
| SA-MDG-01 | El modal abre automáticamente al cargar `/cargas/detalle` | `spec.md` §7 |
| SA-MDG-02 | Los importes se formatean en el servidor | `spec.md` §7 |
| SA-MDG-03 | `hashControl` es derivado determinista | `spec.md` §7 |
| SA-MDG-04 | El fragmento legacy se mantiene deprecado | `spec.md` §7 |
| SA-MDG-05 | Prueba de navegador disponible en CI | `spec.md` §7 |
| SA-MDG-06 | Los campos `expedienteDigital`/`puestoIngreso` mapean a columnas con fallback a sesión | `spec.md` §7 |

---

## 7. Alcance excluido

- Modificación del contrato REST (no hay endpoints nuevos ni cambios).
- Cualquier DDL, migración, `schema.sql`/`data.sql`, Flyway o Liquibase (G5).
- Modificación de la entidad `DetalleProductoPrimario` o de `CargaService`.
- Lógica de negocio en el modal o en el JS.
- Frameworks JS, bundlers, CDNs.
- Modificación de estilos globales/layout (`stitch-tokens.css`, `control-cargas.css`, `base-stitch`).
- Impresión real a PDF / descarga real (placeholders).
- Autenticación/autorización (módulo `03-login`).
- E2E automatizado (fuera del módulo; las pruebas de navegador son manuales/asistidas).

---

## 8. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El include legacy se mantiene referenciado en tests | Bajo | D-MDG-06: mantener el fragmento; actualizar tests que busquen `modalDetalle` |
| Los fallbacks no cubren todos los `null` | Medio | R-MDG-11 + test con form vacío |
| El JS de foco no funciona en todos los navegadores | Bajo | Usar APIs estándar; prueba en Chrome/Edge |
| La carga de CSS del modal rompe el test `cssSinHex` | Bajo | El test sólo revisa `control-cargas.css`; el nuevo CSS tiene su propio test |
| `DetalleProductoPrimario` no se carga en `/cargas/detalle` | Medio | SA-MDG-06: fallbacks a sesión/valores por defecto; campos vacíos aceptables |

---

## 9. Matriz de paridad (R-202)

| Legacy / BD / Stitch | Nuevo (EARS) | Prueba |
|---|---|---|
| `form506-detalle.html` (fragment `modal`) | R-MDG-01, R-MDG-17 | `CargaCorreccionMvcTest` |
| `DETPRODP` (columnas físicas) | R-MDG-07…R-MDG-10 | Test de render del modal |
| `CargaForm` (campos existentes) | R-MDG-07, R-MDG-11 | Test de render |
| Stitch "Detalle de Guía - Modal Control de Cargas" | R-MDG-07…R-MDG-16 | Inspección visual + axe |
| Design System "Gestión Caminera Chaco" | R-MDG-20, R-MDG-25 | Test de tokens |
| `control-cargas.html` (include actual) | R-MDG-17, R-MDG-18 | Suite existente verde |
| AGENTS.md regla 8 (JS puntual) | R-MDG-22 | Revisión del JS |
| G5 (sin DDL) | R-MDG-26 | Revisión del diff |