# Tasks — 11 Validación CUIT (Puestos Camineros)

> **Estado**: `BORRADOR (v1, 2026-11-04)` — pendiente de aprobación humana (puerta **G-SPEC**).
> Lista ordenada y atómica. **Nada se marca como completo**: el coordinador lo hace con evidencia.
> **IDs**: `T-CUIT-<NNN>`. Dependencias entre paréntesis.

---

## Fase 0 — Verificación (G-DATA / G-SPEC)

- **T-CUIT-001** — Verificar el source verbatim de `Nuc_ValidaCuit` en la KB y transcribirlo en `spec.md` §3.2. *(Dep: ninguna)* — **Hecho** (2026-11-04, source leído por MCP).
- **T-CUIT-002** — Verificar las columnas de `CONTRIBU` en `Rentas` (`INFORMATION_SCHEMA.COLUMNS`) y confirmar la ausencia de columna de dígito verificador. *(Dep: ninguna)* — **Hecho** (2026-11-04).
- **T-CUIT-003** — Verificar que `ActaCargarDetalle` no usa `Nuc_ValidaCuit` (lectura del WebPanel). *(Dep: ninguna)* — **Hecho** (2026-11-04).
- **T-CUIT-004** — Verificar el orden de las validaciones en `PPActaDetalle.Validar` (Vendedor → Destinatario → Transportista). *(Dep: ninguna)* — **Hecho** (2026-11-04).
- **T-CUIT-005** — (Supuesto abierto SA-CUIT-01) Auditar otros objetos de la KB que podrían usar `Nuc_ValidaCuit` (búsqueda extendida fuera del índice de source). *(Dep: T-CUIT-001)* — **Criterio de hecho**: lista documentada de objetos que invocan `Nuc_ValidaCuit` (o confirmación de que sólo es `PPActaDetalle`).

## Fase 1 — Implementación del servicio (núcleo)

- **T-CUIT-010** — Crear `atp-nucleo-compartido/.../nucleo/service/ValidadorCuit.java` con `esValido(Long)` y `esValido(String)` replicando verbatim el algoritmo (§3.3 de `spec.md`). *(Dep: T-CUIT-001)* — **Criterio de hecho**: el archivo compila (`./mvnw -pl atp-nucleo-compartido compile`) y los pesos/prefijos coinciden con el source.
- **T-CUIT-011** — (Opcional, si se decide) Exponer `ValidadorCuit` como bean Spring (`@Component`) para inyección. *(Dep: T-CUIT-010)* — **Criterio de hecho**: bean disponible para inyección en `ActaService`.
- **T-CUIT-012** — Agregar tests unitarios `ValidadorCuitTest` con ≥ 20 casos (válidos, inválidos por dv, inválidos por prefijo, inválidos por formato, casos límite `Resto=0` y `Resto=1`). *(Dep: T-CUIT-010)* — **Criterio de hecho**: `./mvnw -pl atp-nucleo-compartido test -Dtest=ValidadorCuitTest` verde.

## Fase 2 — Integración en el módulo 09 (Actas)

- **T-CUIT-020** — Editar `ActaService.validar(...)` (módulo 09) para usar `ValidadorCuit.esValido(...)` en lugar de la validación parcial (D-9). *(Dep: T-CUIT-010)* — **Criterio de hecho**: los 3 CUITs se validan con el algoritmo completo; los mensajes y el orden no cambian.
- **T-CUIT-021** — Actualizar `ActaServiceTest` (módulo 09) con casos de CUIT inválido por dígito verificador (antes pasaban). *(Dep: T-CUIT-020)* — **Criterio de hecho**: los tests nuevos fallan con la validación parcial y pasan con la completa.
- **T-CUIT-022** — Resolver SA-CUIT-04: decidir si los tests del módulo 09 que usan CUITs sintéticos deben actualizarse a CUITs válidos. *(Dep: T-CUIT-021)* — **Criterio de hecho**: decisión documentada en `spec.md` §6 y tests actualizados.
- **T-CUIT-023** — Verificar que `ActaCargarDetalle` (Java) **no** llama a `ValidadorCuit`. *(Dep: T-CUIT-020)* — **Criterio de hecho**: revisión de código (grep) sin llamadas.

## Fase 3 — Verificación (G-TEST / G-SEC)

- **T-CUIT-030** — Ejecutar `./mvnw clean test` (unitarios + vistas) y confirmar que todo sigue verde. *(Dep: T-CUIT-012, T-CUIT-021)* — **Criterio de hecho**: build verde.
- **T-CUIT-031** — (Opcional) Ejecutar IT contra `Rentas` si se agregan tests de derivación por CUIT. *(Dep: T-CUIT-030)* — **Criterio de hecho**: IT verde con `ATP_DB_IT=true`.
- **T-CUIT-032** — Verificar que no se modificó la KB legacy ni se generó DDL. *(Dep: T-CUIT-030)* — **Criterio de hecho**: `git diff` sin cambios en `F:\Modelo\Gx18\ATPPuestos` ni en scripts de migración.
- **T-CUIT-033** — Verificar que no se agregaron endpoints REST (Postman sin cambios) ni vistas nuevas. *(Dep: T-CUIT-030)* — **Criterio de hecho**: `docs/postman/ATPPuestos-API.postman_collection.json` sin cambios.

## Fase 4 — Cierre

- **T-CUIT-040** — Actualizar la matriz de paridad del módulo 09 (R-ACTA-028) para referenciar la validación completa. *(Dep: T-CUIT-020)* — **Criterio de hecho**: `openspec/features/09-formularios-acta/plan.md` §5 actualizado.
- **T-CUIT-041** — Guardar resumen en Engram (`mem_save`, `topic_key: spec/11-validacion-cuit`). *(Dep: T-CUIT-030)* — **Criterio de hecho**: observación guardada.
- **T-CUIT-042** — Resumen de sesión (`mem_session_summary`). *(Dep: T-CUIT-041)* — **Criterio de hecho**: resumen guardado.

---

## Notas

- El módulo es pequeño y acotado: no agrega tablas, columnas, endpoints ni vistas. Su único efecto
  observable es endurecer la validación de CUIT en el alta de Actas (R-CUIT-003) y proveer un
  servicio reutilizable (R-CUIT-009).
- SA-CUIT-01 (usos en otros objetos) se resuelve en T-CUIT-005; si se descubre otro uso, se
  reutiliza `ValidadorCuit` (D-CUIT-02) sin duplicar.
- SA-CUIT-04 (CUITs sintéticos de test) es el mayor riesgo: los tests del módulo 09 que usan
  CUITs con dv incorrecto dejarán de pasar y deberán actualizarse a CUITs válidos (D-CUIT-03).
