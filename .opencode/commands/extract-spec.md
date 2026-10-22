---
description: "Ingeniería inversa de un módulo GeneXus -> spec.md, plan.md y tasks.md (EARS / ISO 25030)"
agent: analista-gx
subagent: true
---

Módulo o alcance solicitado: $ARGUMENTS

Ejecutá el ciclo completo de ingeniería inversa y dejá las especificaciones en
`openspec/features/<modulo>/`.

1. Identificá el módulo y el prefijo de carpeta dentro de `openspec/`. Si el pedido es ambiguo,
   revisá `openspec/features/` y elegí el nombre consistente con lo ya establecido
   (`02-abm-productos`, `03-login`; `01-nucleo-datos` ya está aprobado y portado).
2. Inventariá los objetos GeneXus involucrados (transacciones, procedimientos, web panels,
   web components) usando el servidor MCP `genexus18mcp`. Si el módulo es de GeneXus 15, trabajá
   sobre la exportación `.xpz` / `.xml` o los fuentes disponibles.
3. Contrastá con la **BD real** (G-DATA) usando `INFORMATION_SCHEMA` y las credenciales de `.env`.
   De ahí salen los nombres físicos exactos, los tipos, la nulabilidad y las **columnas huérfanas**
   (las que están en la BD y no en la KB). Es el paso que más regresiones evita.
4. Extraé estructura relacional, reglas declarativas, barridos `For Each` con sus `Order`,
   `parm()` y comportamiento de pantallas. Si el módulo toca autenticación, transcribí el
   mecanismo de verificación de credenciales **carácter por carácter** y la tabla de códigos de
   resultado.
5. Redactá los tres entregables en `openspec/features/<modulo>/`:
   - `spec.md` — requerimientos EARS con id estable y trazabilidad al objeto de origen.
   - `plan.md` — tabla de paridad legacy → JPA con nombres exactos, contratos REST `/api/v1`,
     **contrato de vistas Thymeleaf** (ruta, plantilla, view model, validaciones visibles) y
     supuestos abiertos.
   - `tasks.md` — tareas atómicas y verificables con id `T-NNN`.
6. Dejá explícitos los supuestos abiertos y las dudas que sólo el usuario puede resolver. No los
   rellenes inventando comportamiento.
7. Guardá el resumen del análisis en Engram (`mem_save`).

Recordá: la KB GeneXus es de sólo lectura absoluta y no se genera DDL. No marques tareas como
completadas.

Devolvé un resumen compacto (no los archivos completos): rutas de los tres entregables, cantidad
de requerimientos, tablas mapeadas, supuestos abiertos y riesgos.
