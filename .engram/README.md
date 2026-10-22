# Memoria persistente del agente (Engram)

Esta carpeta es el **espejo portable y trackeable** de la memoria del agente. La base de datos real
de Engram vive fuera del repositorio; lo que se guarda acá es el resumen versionable.

## Qué se guarda acá

- Resúmenes de sesión (`mem_session_summary`).
- Decisiones de arquitectura y sus justificaciones.
- Hallazgos de la KB y del esquema físico de la base de datos.
- Convenciones que deben sobrevivir a un cambio de máquina o de equipo.

## Reglas

1. **Nada de secretos.** Ni credenciales, ni cadenas de conexión, ni tokens.
2. **Nada voluminoso.** La memoria es un índice, no un volcado: lo bulky vive en
   `openspec/features/**` y `docs/**`, y desde la memoria se apunta a la ruta.
3. **Todo hallazgo de BD se guarda.** El esquema físico (nombres truncados, columnas huérfanas,
   formatos de dato) es lo que más tiempo ahorra entre módulos: es la razón de existir del
   `topic_key` `domain/security-schema`.
4. **Toda decisión lleva su "por qué".** Sin el porqué, una decisión vieja se re-discute sin
   contexto.

## Convenciones de `topic_key`

| Prefijo | Contenido |
|---|---|
| `architecture/…` | Decisiones de estructura, stack, límites de módulo |
| `domain/…` | Modelo de datos y reglas de negocio del legacy |
| `discovery/…` | Hallazgos verificables (KB, BD, entorno) |
| `spec/…` | Resumen de una spec de módulo |
| `context/…` | Estado y antecedentes del proyecto |

## Cómo se usa

- **Guardar**: `mem_save` con `title`, `type`, `topic_key` y el cuerpo en
  `**What** / **Why** / **Where** / **Learned**`.
- **Recuperar**: `mem_context` (sesión reciente) y `mem_search` (búsqueda por texto).
- **Al cerrar una sesión**: `mem_session_summary` con la estructura
  `Goal / Instructions / Discoveries / Accomplished / Next Steps / Relevant Files`.
