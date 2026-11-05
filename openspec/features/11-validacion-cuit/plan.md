# Plan — 11 Validación CUIT (Puestos Camineros)

> **Estado**: `IMPLEMENTADO (2026-11-04)` — Gate 2 APROBADO CON OBSERVACIONES.
> Documento de arquitectura. No contiene tareas (ver `tasks.md`).
> **Fuente**: KB `ATPPuestos` (solo lectura) + `Rentas` `INFORMATION_SCHEMA` (G-DATA 2026-11-04).
> Requisitos: `spec.md` (`R-CUIT-*`). Decisiones: `spec.md` §5 (`D-CUIT-*`).

---

## 1. Estructura del módulo

```text
atp-nucleo-compartido/
└── src/main/java/ar/com/atp/puestos/nucleo/
    └── service/
        └── ValidadorCuit.java          (servicio puro — módulo 11)
atp-nucleo-compartido/
└── src/test/java/ar/com/atp/puestos/nucleo/service/
    └── ValidadorCuitTest.java          (tests unitarios del algoritmo)
atp-puestos-camineros/
└── src/main/java/ar/com/atp/puestos/puestos/service/
    └── ActaService.java                (EDITADO: validar() usa ValidadorCuit)
atp-puestos-camineros/
└── src/test/java/ar/com/atp/puestos/puestos/service/
    └── ActaServiceTest.java            (EDITADO: casos de CUIT inválido por dv)
```

> **No se toca** `atp-nucleo-compartido/.../domain/*` ni repositorios: la validación es pura (R-CUIT-008).
> **No se tocan** plantillas ni CSS: no hay vistas nuevas (D-CUIT-04).

### 1.1 Paquetes y dependencias

| Capa | Paquete | Puede depender de |
|---|---|---|
| Servicio (núcleo) | `ar.com.atp.puestos.nucleo.service` | nada (G7) |
| Servicio (puestos) | `ar.com.atp.puestos.puestos.service` | `nucleo.service` |

**Prohibido**: `web → repository`, `domain → web`, lógica de negocio en el controller (G7).

---

## 2. Paridad JPA — no aplica

Este módulo **no mapea tablas ni columnas**: la validación es puramente algorítmica (§3.1 de `spec.md`).
`CONTRIBU` ya está mapeada en `puestos.domain.Contribuyente` (módulo 09, §2.5 de su plan). No se
agregan entidades, repositorios ni columnas.

---

## 3. Contratos

### 3.1 REST — sin endpoints nuevos

| Método | Ruta | Estado |
|---|---|---|
| — | — | El módulo **no** expone API (D-CUIT-04). No hay cambios en `docs/postman/ATPPuestos-API.postman_collection.json`. |

### 3.2 MVC — sin rutas nuevas

El módulo no agrega rutas de página. El consumo se hace en `ActaService.validar(...)` (módulo 09),
que ya está cableado a `POST /actas/guardar`. La única diferencia observable es que los CUITs con
dígito verificador incorrecto ahora se rechazan (antes pasaban).

### 3.3 Contrato del servicio `ValidadorCuit`

```java
// nucleo/service/ValidadorCuit.java
public final class ValidadorCuit {

    private static final Set<Integer> PREFIJOS_VALIDOS =
            Set.of(20, 23, 24, 27, 30, 33, 34);

    /** Valida un CUIT recibido como Long (NUMERIC(11,0) del legacy). */
    public static boolean esValido(Long cuit) {
        if (cuit == null) return false;
        String s = String.format("%011d", cuit);
        return esValido(s);
    }

    /** Valida un CUIT recibido como String de 11 dígitos. */
    public static boolean esValido(String cuit) {
        if (cuit == null) return false;
        if (cuit.length() != 11) return false;
        for (int i = 0; i < 11; i++) {
            if (!Character.isDigit(cuit.charAt(i))) return false;
        }
        // primeros 10 dígitos
        String docu = cuit.substring(0, 10);
        int acu = 0;
        String pesos = "5432765432";
        for (int i = 0; i < 10; i++) {
            int digito = docu.charAt(9 - i) - '0';   // i=10..1 → posición 9-i
            int peso = pesos.charAt(9 - i) - '0';
            acu += digito * peso;
        }
        int resul = acu / 11;          // Int(Acu/11)
        int resto = acu - resul * 11;  // Acu - Resul*11
        int ultDig = 11 - resto;
        if (ultDig == 11) ultDig = 0;
        int ult = cuit.charAt(10) - '0';          // último dígito informado
        int dosPriDig = Integer.parseInt(docu.substring(0, 2));
        return ultDig == ult && PREFIJOS_VALIDOS.contains(dosPriDig);
    }
}
```

> **Nota de implementación**: los pesos se aplican de derecha a izquierda (i=10→1) igual que el
> legacy (`Do While &i>=1`). La traducción a Java debe preservar ese orden; el resultado es el
> mismo porque la multiplicación es conmutativa, pero se documenta para trazabilidad.

### 3.4 Integración en `ActaService.validar(...)`

```java
// en ActaService (módulo 09), reemplazar la validación parcial (D-9):
// ANTES (parcial):
//   if (alta.f276CuitVe() == null || String.valueOf(alta.f276CuitVe()).length() != 11) ...
// DESPUÉS (completa):
if (!ValidadorCuit.esValido(alta.f276CuitVe())) {
    return new ResultadoAltaActa(false, "Debe ingresar CUIT del Vendedor", null);
}
if (!ValidadorCuit.esValido(alta.f276CuitDe())) {
    return new ResultadoAltaActa(false, "Debe ingresar CUIT del Destinatario", null);
}
if (!ValidadorCuit.esValido(alta.f276CuitTr())) {
    return new ResultadoAltaActa(false, "Debe ingresar CUIT del Transportista", null);
}
```

El orden de evaluación se mantiene (Vendedor → Destinatario → Transportista) para devolver el
**primer** error (R-CUIT-003).

---

## 4. Contrato de vistas (Thymeleaf)

No hay vistas nuevas. El único efecto observable es en el formulario de alta de Actas
(`/actas/nueva`): los CUITs con dígito verificador incorrecto ahora se rechazan con el mismo
mensaje verbatim (`Debe ingresar CUIT del ...`), en lugar de pasar la validación parcial.

No se modifican plantillas, CSS ni JS.

---

## 5. Matriz de paridad con el legacy

| Operación legacy | Requisito | Implementación | Prueba |
|---|---|---|---|
| `Nuc_ValidaCuit.Udp(...)` (Boolean) | R-CUIT-001/002/007/010 | `ValidadorCuit.esValido(...)` | `ValidadorCuitTest` |
| Validación en `PPActaDetalle.Validar` | R-CUIT-003 | `ActaService.validar(...)` | `ActaServiceTest#validaciones` |
| Sin validación en `ActaCargarDetalle` | R-CUIT-006 | (ausencia) | revisión de código |
| Derivación por CUIT inexistente | R-CUIT-004 | `ActaService.derivarPorCuit(...)` | `ActaServiceTest` |
| Pureza (sin BD) | R-CUIT-008 | `ValidadorCuit` sin repositorios | revisión de código |
| Reutilización compartida | R-CUIT-009 | ubicación en `nucleo.service` | revisión de imports |

---

## 6. Decisiones

Ver `spec.md` §5 (`D-CUIT-01` … `D-CUIT-07`). Resumen operativo:

| # | Decisión | Efecto en la implementación |
|---|---|---|
| D-CUIT-01 | Algoritmo verbatim (incluyendo `UltDig = 10` ⇒ inválido y prefijo). | `ValidadorCuit.esValido(...)` con pesos y prefijos exactos. |
| D-CUIT-02 | Servicio puro en `atp-nucleo-compartido`. | `ar.com.atp.puestos.nucleo.service.ValidadorCuit`. |
| D-CUIT-03 | Reemplaza la validación parcial (D-9) del módulo 09. | Editar `ActaService.validar(...)`. |
| D-CUIT-04 | Sin REST ni vistas nuevas. | Sin cambios en Postman ni plantillas. |
| D-CUIT-05 | `ActaCargarDetalle` no valida CUIT. | Sin cambios en ese panel. |
| D-CUIT-06 | Derivación deja vacío si no existe. | Reutiliza derivación del módulo 09. |
| D-CUIT-07 | Sobrecarga `esValido(Long)` y `esValido(String)`. | Validación de formato estricta en la variante `String`. |

---

## 7. Supuestos abiertos

Ver `spec.md` §6 (`SA-CUIT-01` … `SA-CUIT-04`).

---

## 8. Alcance excluido

- Validación contra AFIP en tiempo real.
- Normalización de CUIT con guiones/espacios (OUT §2.2 de `spec.md`).
- Endpoints REST y vistas Thymeleaf nuevas.
- Carga masiva / batch de CUITs.
- Modificación de la KB legacy (G1).
- DDL / migraciones (G5).
- Modificación de `Contribu` o de su mapeo JPA.
