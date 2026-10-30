# MCP de GeneXus (genexus-mcp) — configuración y uso

## ¿Qué es?
`genexus-mcp` es un paquete **de terceros** publicado en npm
(maintainer `lennix1337`, repo `github.com/lennix1337/Genexus18MCP`, licencia MIT).
**No es el MCP oficial de GeneXus** (GeneXus no publica uno propio). Es un servidor MCP
comunitario que habla con la KB de GeneXus 16/17/18 (y compatibilidad legacy).
La versión configurada en este entorno es `3.9.2` (la última publicada es `4.0.1`).

## ¿Con qué herramienta se conecta?
El que está **activo en esta sesión es OpenCode** (config en
`~/.config/opencode/opencode.jsonc`). También aparece configurado en **Cursor**
(`~/.cursor/mcp.json` con el alias `genexus18mcp`), por lo que está disponible en ambos.
El agente actual (OpenCode) lo consume como MCP local.

## ¿Qué operaciones permite?
El servidor expone decenas de tools. Las relevantes para análisis (solo lectura, que es lo
que usamos en este proyecto):

- **Listar/buscar objetos**: `genexus_list_objects`, `genexus_query`
- **Leer código/fuente**: `genexus_read`, `genexus_inspect`
- **Analizar dependencias/impacto**: `genexus_analyze`
  (modes: `context`, `deep_context`, `callers`, `impact`, `navigation`, `hierarchy`,
  `table_relations`, `code_metrics`, `kb_stats`, etc.)
- **Propiedades y estructura**: `genexus_properties`, `genexus_layout`
- **Comparar/diff**: `genexus_compare`, `genexus_kb_diff`
- **KB/entorno**: `genexus_kb` (list/open/select), `genexus_doctor`, `genexus_db`
  (drift, sample data, reorg preview)
- **Formato**: `genexus_format`

⚠️ También expone operaciones de **escritura/build/deploy**
(`genexus_edit`, `genexus_create`, `genexus_delete_object`, `genexus_lifecycle`
build/reorg, `genexus_deploy`, `genexus_gxserver` commit/update).
**En este proyecto están prohibidas** por la regla de "KB solo lectura" (no Build All,
no Reorganize, no modificar objetos). Se usan únicamente para lectura/análisis.

## Configuración (la actual)
En `C:\Users\walterg20\.config\opencode\opencode.jsonc`:

```jsonc
"genexus18mcp": {
  "type": "local",
  "command": ["npx.cmd", "-y", "genexus-mcp@3.9.2"],
  "enabled": true,
  "environment": {
    "GX_CONFIG_PATH": "F:\\Modelo\\Gx18\\ATPPuestos\\config.json"
  }
}
```

## Cómo se instala (en OpenCode)
1. Tener Node.js/npm y GeneXus 18 instalado (KB en `F:\Modelo\Gx18\ATPPuestos`).
2. Crear/editar `~/.config/opencode/opencode.jsonc` y agregar el bloque `mcp.genexus18mcp`
   anterior (ajustar `GX_CONFIG_PATH` a tu `config.json`).
3. OpenCode descarga automáticamente vía `npx -y genexus-mcp@latest` la primera vez.
4. Reiniciar OpenCode. El MCP queda disponible como `tools.genexus18mcp.*`.

También se puede usar en **Cursor** con el mismo comando en `~/.cursor/mcp.json`
(ya configurado).

## Advertencia
Es un proyecto comunitario de terceros, no mantenido por GeneXus. Verificá la versión
antes de actualizar (`@latest`) y no ejecutes operaciones de escritura sobre la KB
(solo lectura, según `AGENTS.md`).
