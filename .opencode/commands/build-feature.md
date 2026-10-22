---
description: "Fase de implementación: delega backend y vistas en paralelo a partir de la spec aprobada"
agent: coordinador
subagent: false
---

Módulo objetivo: $ARGUMENTS

Ejecutá la fase de implementación de la especificación aprobada.

1. Confirmá que `openspec/features/<modulo>/spec.md`, `plan.md` y `tasks.md` existen y están
   completos. Si falta la spec aprobada, detenete y reportá el bloqueo en lugar de improvisar.
2. Identificá en `tasks.md` las tareas pendientes que correspondan a backend y a vistas.
3. Lanzá **en paralelo**, en el mismo bloque de llamadas a la herramienta `subagent`:
   - `implementador-backend` — con las tareas de backend, las rutas de la spec y el criterio de
     aceptación. Escribe en `*/src/main/java/**/{domain,repository,service,dto,shared,web/rest}/**`.
   - `implementador-vistas` — con las tareas de vistas, el **contrato de vistas** del `plan.md`
     (plantilla, view model, validaciones) y el criterio de aceptación. Escribe en
     `*/src/main/java/**/web/mvc/**`, `*/src/main/resources/templates/**` y `static/**`.
   Si sólo un lado tiene trabajo pendiente, lanzá únicamente el que corresponde.
4. Cuando ambos terminen, lanzá `verificador-qa` sobre lo producido y exigí evidencia (comandos
   ejecutados y su resultado).
5. Con el veredicto en mano, actualizá `tasks.md`: marcá sólo lo que tenga evidencia verde y dejá
   explícito lo pendiente con su motivo.
6. Verificá G-POSTMAN: si la implementación agregó, modificó o eliminó endpoints, la colección
   `docs/postman/ATPPuestos-API.postman_collection.json` debe estar actualizada en el mismo
   commit.
7. Guardá el resumen de la fase en Engram y proponé el mensaje de commit.

Recordá: JDK 17 es obligatorio (`set JAVA_HOME=C:\Program Files\Java\jdk-17`); el `java` del PATH
suele ser 1.8 y no compila este proyecto.

No escribas código de aplicación vos mismo: tu trabajo es delegar, validar los gates y mantener el
estado del arnés.
