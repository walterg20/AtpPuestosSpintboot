# genexus18mcp — cómo está instalado y qué tener en cuenta

> **Propósito**: acceso del agente a la **KB GeneXus 18 legacy** (`F:\Modelo\Gx18\ATPPuestos`) en
> modo lectura para el replatforming (F0 inventario, F2 extracción de specs, fase 2 extracción
> de diseño web).
> **Regla de oro (R-201)**: la KB es **solo lectura**. Nunca se invoca ninguna tool de escritura,
> compilación o creación de objetos sobre ella.

## 1. Qué es

[`genexus-mcp`](https://www.npmjs.com/package/genexus-mcp) es un servidor MCP (Node.js) que expone
la API/SDK de GeneXus 18 como **54 tools** para que un agente lea la KB, su estructura, su
source, sus objetos y su metadata sin abrir el IDE.

- **Versión fijada**: `genexus-mcp@3.9.2` (fijada explícitamente, no `@latest`, para que el
  comportamiento del agente sea reproducible).
- **Runtime**: se ejecuta vía `npx` en Windows → el ejecutable debe ser **`npx.cmd`** (no `npx`),
  si no, OpenCode no puede lanzarlo.

## 2. Configuración (OpenCode)

Registro en `C:\Users\walterg20\.config\opencode\opencode.jsonc`, bloque `mcp`:

```jsonc
"genexus18mcp": {
  "type": "local",
  "command": [
    "npx.cmd",
    "-y",
    "genexus-mcp@3.9.2"
  ],
  "enabled": true,
  "environment": {
    "GX_CONFIG_PATH": "F:\\Modelo\\Gx18\\ATPPuestos\\config.json"
  }
}
```

### Qué significa cada línea

| Campo | Valor | Por qué |
|---|---|---|
| `command` | `npx.cmd -y genexus-mcp@3.9.2` | Descarga/ejecuta el paquete sin instalarlo globalmente. `-y` evita el prompt de npm. **Windows: usar `npx.cmd`**. |
| `GX_CONFIG_PATH` | `F:\Modelo\Gx18\ATPPuestos\config.json` | Archivo `config.json` **de la carpeta de la KB** (no el de la instalación de GeneXus). Es el que le dice al SDK a qué KB apuntar. |

### Requisitos previos en la máquina

1. **GeneXus 18 instalado** con licencia válida (el SDK requiere la instalación aunque no abramos el IDE). Versión observada de la KB: **18.0.13.186702**, índice "Booting", SDK nativo (`native-sdk`).
2. **Node.js** disponible en el PATH (`node`/`npx`).
3. La KB en disco accesible en `F:\Modelo\Gx18\ATPPuestos\` (incluido su `config.json`).

## 3. Tools disponibles (54)

Agrupadas por uso:

| Grupo | Tools (principal) | Uso en este proyecto |
|---|---|---|
| **Lectura** ★ | `genexus_read` (parts: `Structure`, `Rules`, `Events`, `Source`), `genexus_structure` (`get_indexes` → nombre físico de tabla, PK/índices), `genexus_list_objects`, `genexus_search_source`, `genexus_query`, `genexus_module` | **F0 y F2**: extracción de transacciones, procedimientos e índices. |
| Análisis | `genexus_analyze`, `genexus_inspect`, `genexus_memory`, `genexus_data_view` | Impacto/consumidores de objetos (`Bus_DesLoc`→`F2276`, `BuscarCamino`→`CodigoBarra`). |
| Navegación/UI | `genexus_navigation`, `genexus_layout`, `genexus_edit_form`, `genexus_browser` | **Fase 2**: extraer el HTML/CSS/JS de los WebPanels para replicar el diseño ABM. |
| Metadata | `genexus_properties`, `genexus_variable`, `genexus_api`, `genexus_doc`, `genexus_generator_reference` | Tipos de atributos, variables, referencias. |
| Diferencias | `genexus_kb_diff`, `genexus_compare`, `genexus_kb_version` | Comparar versiones/objetos. |
| ⚠️ **Escritura/compilación** (PROHIBIDAS sobre la KB legacy) | `genexus_create`, `genexus_edit`, `genexus_edit_and_build`, `genexus_delete_object`, `genexus_apply_pattern`, `genexus_refactor`, `genexus_merge`, `genexus_transfer`, `genexus_deploy`, `genexus_kb_import`, `genexus_run_object`, `genexus_test`, `genexus_sandbox`, `genexus_gam`, `genexus_gxserver`, `genexus_versioning`, … | **No usar contra `ATPPuestos`.** Solo se usarían si en el futuro se crea una KB *nueva* de destino. |
| Utilidades | `genexus_whoami`, `genexus_doctor`, `genexus_sdk_probe`, `genexus_kb`, `genexus_format`, `genexus_io`, `genexus_security`, … | Diagnóstico de conexión/SDK. |

## 4. Gotchas aprendidos (F0/F2)

1. **Colisión de nombres**: existe la carpeta `Feriados` *y* la transacción `Feriados`. Siempre
   pasar `type: 'Transaction'` explícito en las consultas para que no devuelva la carpeta.
2. **El MCP no expone nombres físicos de columna** (solo de tabla vía `get_indexes`). De ahí el
   supuesto **ASM-01**: columna = nombre lógico del atributo, a validar contra la BD `Rentas`
   en la puerta **G-DATA**.
3. **Regla de 8 caracteres**: el nombre físico de tabla es el nombre de la transacción truncado a
   8 chars (`Localidades→Localida`, `Provincia→PROVINCI`, `Productos→Producto`).
   Confirmado con `genexus_structure get_indexes` → `result.name`.
4. **Transactions sin reglas**: muchas maestras tienen `Rules`/`Events` **vacíos** (datos puros).
   No asumir lógica de negocio donde no la hay; verificar siempre con `genexus_read`.
5. **Arranque en frío**: la primera tool de una sesión puede tardar varios segundos porque
   `npx` descarga el paquete y el SDK levanta la conexión a la KB. Si falla, verificar
   GeneXus instalado/licenciado y que `GX_CONFIG_PATH` apunte al `config.json` **de la KB**.
6. **Reproducibilidad**: mantener la versión fijada (`3.9.2`). Si se sube, re-verificar los
   formatos de respuesta de `genexus_read`/`genexus_structure` antes de confiar en los parsers.

## 5. Verificación rápida

Pedirle al agente: *"verificá que la KB esté conectada con genexus18mcp"* — Internamente se llama
a `genexus_whoami` / `genexus_kb` y debe devolver la KB `ATPPuestos` (GX18). Si el servidor no
aparece en la lista de MCPs de la sesión, revisar el bloque `mcp` del `opencode.jsonc` y el
requisito `npx.cmd` (punto 2).
