# Spec — 00 Alcance migrable

> **Estado**: `APPROVED` — puerta **G-SPEC**.
> **Sintaxis**: EARS (ISO/IEC 25030). Cada requisito incluye criterio de verificación.
> **Alcance de escritura**: `openspec/features/**` y `docs/**`. Sin cambios en el código.
>
> Este documento fija **qué entra y qué sale** del replatform. No describe comportamiento de
> negocio: eso lo hacen los módulos `02-abm-productos` y `03-login`.

---

## 1. Propósito

Delimitar el alcance del repositorio `AtpPuestosSpintboot` y dejar **registrado y mapeado** el
resto del parque de aplicaciones legacy, de modo que lo que queda fuera no se pierda ni se
reimplemente por ignorancia.

## 2. Contexto del parque de aplicaciones

El relevamiento de las KB del entorno (ver `docs/analisis-ATPPuestos.md` §14) encontró que el
sistema no es una aplicación sino un **conjunto** de aplicaciones y bases:

| Aplicación / KB actual | KB real | Objetos | Base de datos | Destino previsto |
|---|---|---:|---|---|
| **Puestos Camineros** | `Gx18\ATPPuestos` | 1.615 | `RENTAS` | **Módulo del Portal** |
| **Gestión ATP** (web) | `Gx18\ATPWebGX18` | 10.867 | `Default`, `Domiciliario`, `MarcaYSenial` | **Portal Gestión ATP** y módulos internos |
| ↳ Gestión de Agentes | `ATPWebGX18/ABM_8_Agentes` | — | — | Módulo del Portal |
| ↳ Planes de Pago | `ATPWebGX18/ABM_11_13_14_Domicilio` | — | `ATPDomiciliarioWeb` | Módulo de negocio usado por uno o ambos portales |
| ↳ Contribuyente | `ATPWebGX18/Contribuyentes` | — | `ATPPadronWeb` | **Portal Contribuyente** |
| (módulos internos) | `Gx18\ATPGestion` | 4.500 | `RentasTDA`, `ATPDomicILIARIO`, `RENTASTestigo`, `RENTASImportacion` | Gestión ATP y módulos internos |

Bases en `devmssqlatp.ecom.com.ar`: `RENTAS`, `RentasTDA`, `RENTASTestigo`, `RENTASImportacion`,
`ATPDomiciliarioWeb`, `ATPPadronWeb`, `ATPDevelopment`, `ATPWiki`, `RENTASWiki`.

> **Hallazgo estructural**: `ATPPadronWeb` (destino = Portal Contribuyente) contiene **sólo tablas
> de proceso** (`Proc*`, `AuditProcesos`, `Progress`), sin datos de negocio: ya es un *proxy thin*.
> Y `ATPDomiciliarioWeb` tiene `PlanActuacion` + 9 `PlanParam*` + `Solicitud*`: es un módulo de
> planes de pago con base propia. La topología legacy ya se parece al destino propuesto.

## 3. Alcance

### 3.1 IN — dentro de este repositorio

- **Módulo Puestos Camineros** (`Gx18\ATPPuestos`), con su núcleo de datos:
  - 8 catálogos: Provincia, Localidad, Calle, Camino, Área, Jurisdicción, Feriado, Producto.
  - Producto con su nivel 1 de vigencias (`PRODUCT1`).
  - Personas (`PERSONAS`) y su vínculo con el usuario.
- **Login completo de Puestos Camineros** (spec `03-login`).\n- **Control de Cargas** de Puestos Camineros (spec `08-control-cargas`): validación de puesto, búsqueda por barcode, marcado de formulario, ver detalle, ver tabla, pagos. El **reporte en PDF** queda **fuera de alcance** (OUT) y se integra más adelante.
- **API REST `/api/v1`** sobre el núcleo de datos (contrato ya existente y verificado).
- **Capa de presentación Thymeleaf** para lo anterior.

### 3.2 OUT — fuera de este repositorio (con su destino registrado)

| Excluido | Motivo | Dónde vive / por qué |
|---|---|---|
| Portal Gestión ATP | Es otra aplicación (10.867 objetos) | Repo del portal; este repo sólo aporta el módulo de Puestos |
| Gestión de Agentes | Módulo del portal, no de Puestos | `ATPWebGX18/ABM_8_Agentes` |
| **Planes de Pago** | **Decisión del equipo (2026-10-22): descartado de este alcance** | Base `ATPDomiciliarioWeb`, propia; otro repo |
| Portal Contribuyente | Otra aplicación, con base propia | `ATPPadronWeb`; requiere un login de actor externo, que **no** es el de Puestos |
| Módulos internos de Gestión ATP | Otra KB | `Gx18\ATPGestion` |
| EOs/SDTs/ThemeClass del Design System Unanimo | Patrón de plataforma, no dominio | Reutilizar el design system, no reimplementar |\n| Reporte en PDF de Control de Cargas | Fuera de alcance de `08-control-cargas` (se integra más adelante) | Backlog (módulo futuro) |
| Plantillas mobile (`SD*`), `Deprecated`, `iOS`, `Social`, `Store`, `Purchases`, `Media` | Fuera del negocio | — |
| **Gx15** (`Gx15\ATPPuestos`, `ATPGestion`, `ATPWeb`, `ATPWebIA`, `ATPWebModulos`, `ATPDomiciliarios`) | Sin análisis posible | El MCP actual soporta `major` 16/17/18 ⇒ **Gx15 no es accesible** con la herramienta vigente. Requiere otro canal de lectura. |

## 4. Requisitos

| ID | Requisito (EARS) | Criterio de verificación |
|---|---|---|
| R-ALC-001 | El sistema DEBERÁ reconstruir el módulo Puestos Camineros limitándose a los catálogos, las personas y el login definidos en §3.1. | Inventario de entidades en `plan.md` y cobertura de `tasks.md` |
| R-ALC-002 | El sistema NO DEBERÁ modificar el esquema de la base de datos existente. | `ddl-auto=none`; sin migraciones; sin `schema.sql`/`data.sql`; sin DDL |
| R-ALC-003 | El sistema DEBERÁ mapear los nombres físicos exactos de tablas y columnas de la base `RENTAS`, incluidos los nombres truncados a 8 y 10 caracteres. | Tabla de paridad de `plan.md` contrastada contra `INFORMATION_SCHEMA` |
| R-ALC-004 | El sistema DEBERÁ persistir las columnas huérfanas de la base de datos (las que no existen en la KB) aplicando el default de aplicación declarado en la spec del módulo. | Test de escritura que las verifica tras un alta |
| R-ALC-005 | El sistema DEBERÁ exponer el núcleo de datos mediante la API REST `/api/v1` con el cuerpo uniforme `{code, message}` ante error. | Tests de `web/rest` |
| R-ALC-006 | El sistema DEBERÁ presentar el núcleo de datos en páginas renderizadas por el servidor, sin aplicación de una sola página. | Test de render Thymeleaf por pantalla |
| R-ALC-007 | El sistema NO DEBERÁ incluir en este repositorio el Portal Gestión ATP, Gestión de Agentes, Planes de Pago ni el Portal Contribuyente. | Revisión de la estructura del repo: ningún paquete, plantilla ni endpoint de esos dominios |
| R-ALC-008 | El sistema DEBERÁ dejar registrada en `docs/analisis-ATPPuestos.md` §14 la correspondencia entre aplicación legacy y destino previsto, con su KB y su base de datos. | Inspección del documento |
| R-ALC-009 | El sistema NO DEBERÁ alterar ni generar artefactos de la base de conocimiento KB original. | Traza de acceso de sólo lectura; ningún *Build All* ni *Regenerate* |
| R-ALC-010 | SI el análisis de una KB de GeneXus 15 resultara necesario, ENTONCES el sistema DEBERá registrarlo como supuesto abierto con el canal de lectura alternativo requerido. | Sección de supuestos abiertos de la spec del módulo |

## 5. Supuestos abiertos

| # | Supuesto | Impacto si resulta falso |
|---|---|---|
| ASM-01 | El destino "Planes de Pago usado por uno o ambos portales" se resolverá en otro repositorio, sin código compartido con éste. | Habría que extraer el módulo a una biblioteca Maven compartida, lo que cambia la estructura del monorepo. |
| ASM-02 | Los portales futuros usarán su propia autenticación de actor externo (contribuyente), distinta de la de Puestos Camineros. | Si el Portal Contribuyente debiera reutilizar el login de Puestos, habría que revisar el alcance de `03-login`. |
| ASM-03 | Las columnas huérfanas de la BD son deseadas y no residuo de una migración abortada. | Si fueran residuo, "sin rediseño inicial" impediría limpiarlas: quedarían mapeadas por obligación. |
| ASM-04 | Las 6 KB de GeneXus 15 no contienen objetos de negocio ausentes en las de Gx18. | Si los contienen, parte del alcance no estaría cubierta. Requiere un canal de lectura para Gx15. |

## 6. Trazabilidad

- Inventario y diagnóstico: `docs/analisis-ATPPuestos.md` (F0, §13 G-DATA, §14 portfolio).
- Reglas del stack: `openspec/constitution/stack.md`.
- Módulos derivados: `01-nucleo-datos` (ya implementado), `02-abm-productos`, `03-login`, `08-control-cargas`.
