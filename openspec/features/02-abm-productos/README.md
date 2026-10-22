# Módulo 02 — ABM de Productos (Thymeleaf)

> **Estado**: `PENDIENTE` — la spec todavía **no** existe. Este archivo es un marcador de posición,
> **no** una spec aprobada.

Este módulo especifica el ABM de `PRODUCTO` con su nivel 1 de vigencias (`PRODUCT1`), en la
**capa de presentación Thymeleaf**.

## Contexto

- El **backend** ya está implementado y verificado: `atp-nucleo-compartido` expone
  `GET/POST/PUT/DELETE /api/v1/productos` y `GET /api/v1/productos/{prodCod}/importes-vigentes`.
  Su contrato está en `openspec/features/01-nucleo-datos/spec.md` (R-001…R-037) y en
  `docs/postman/`.
- La **UI** es lo que falta: hoy no hay ninguna pantalla Thymeleaf de productos.
- La **fuente de verdad del comportamiento** es
  `openspec/features/01-nucleo-datos/ui-abm-productos.md` (portada de `atppuestos2`): rótulos
  verbatim de los 12 campos del nivel 0 y los 5 del nivel 1, tipos, nulabilidad, reglas
  `R-PROD-*` y decisiones D-19/D-20/D-22. **Su sección de arquitectura (shell Angular,
  `mat-table`, punto de extensión con `<ng-content>`) queda obsoleta** y hay que reformularla para
  Thymeleaf.

## Entregables esperados

| Archivo | Contenido |
|---|---|
| `spec.md` | Requisitos EARS con id `R-PROD-<NN>`, criterio de verificación y trazabilidad al objeto GeneXus |
| `plan.md` | Paridad JPA (referida a `01-nucleo-datos`), contrato REST (ya existente) y **contrato de vistas Thymeleaf**: ruta, plantilla, view model, validaciones visibles, comportamiento ante error, y el patrón de la grilla anidada editable |
| `tasks.md` | Tareas atómicas `T-<NNN>`, con las de backend separadas de las de vistas |

## Reglas que condicionan la spec

- Thymeleaf con `th:object` + `th:field`; los form objects en `web/mvc/**`.
- La grilla anidada de vigencias: alta/baja de filas con **JavaScript puntual** (ver
  `docs/design.md` §8), nunca con una grilla en memoria.
- Los 4 derivados `ProdIMax1..4` **no** se persisten y se exponen aparte: en la UI son de sólo
  lectura y no viajan en el body de escritura.
- La PK `prodCod` viaja en el body del `POST` (R-037).
- Sin DDL: se reutiliza el esquema existente.
