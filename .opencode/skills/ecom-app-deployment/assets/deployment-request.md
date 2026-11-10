# Solicitud de despliegue — [Aplicación] — [TEST / PROD]

Estado: [Borrador con pendientes / Listo para revisión]

## Dónde cargar la solicitud

1. Ingresa a https://app.ecom.com.ar/tickets/servlet/com.gestiondesolicitudes.gamexamplelogin.
2. Ve a **Tickets → Plantillas**.
3. Busca **Pedido de Despliegue en Kubernetes** y pulsa **+**.
4. Reemplaza el título precargado y copia únicamente el contenido de la sección indicada.

## Título para copiar

Solicitud de despliegue de aplicaciones en cluster Kubernetes [TEST / PROD] - [Nombre confirmado de la aplicación]

## Contenido para copiar al ticket

**Repositorio del código:** [URL del repositorio]

**Framework y version:** [Nombre y versión / No utiliza framework]

**Lenguaje y versio:** [Nombre y versión]

**Ambiente de despliegue:** [TEST / PROD]

**Comandos de inicialización:** [Comandos manuales confirmados en orden / No hay comandos manuales]

**DNS a utilizar:** [Hostname confirmado]

**Tipo de acceso:** [Solo interno / Interno y externo]

---

**Base de datos y Caché:**

- **Tecnología de BD:** [No utiliza / Existente — motor y referencia no sensible / Solicitar nueva — motor]
- **¿Utiliza Redis o Memcached?:** [No / Sí — servicio confirmado]
- **Volumen persistente:** [No / Sí — ruta(s) confirmada(s)]

**Desarrolladores involucrados:** [Nombre, apellido y equipo de cada desarrollador]

**Entrega de configuración sensible:** [PWPush — enlace si se proporcionó / Compartida o pendiente de compartir por Passbolt, según confirmación]

**Consideraciones para el despliegue:** [Detalles confirmados / Sin observaciones adicionales]

**Importante:** La información sensible y las variables de entorno deben ser enviadas por pwpush o compartidas a través del Passbolt de la empresa. No incluir sus valores en este ticket.

## Checklist y recordatorios — No copiar al ticket

Esta lista te ayuda a revisar qué falta antes de pedir el despliegue:

- **🔴 Falta:** comprobamos que falta algo necesario o que hay un problema que resolver.
- **🟡 Por confirmar:** hay dudas, información incompleta o algo que todavía no pudimos revisar.
- **🟢 Comprobado:** ese punto concreto está revisado y cumple lo que se estaba comprobando.

El verde no significa que toda la aplicación esté lista. Por ejemplo, encontrar un Dockerfile confirma que existe, no que funcione.

### Atención prioritaria

[Destacar primero todos los puntos rojos: **qué falta o qué problema hay**, por qué importa y qué hacer. Agregar las dudas importantes en amarillo sin presentarlas como fallas confirmadas. Si no hay puntos prioritarios, indicarlo.]

### Revisión por puntos

[Ordenar las filas: primero rojo, luego amarillo y al final verde. Elegir un único estado por fila.]

| Qué revisar | Estado | Qué encontramos y qué hacer |
| --- | --- | --- |
| Dockerfile o Docker Compose: archivos que indican cómo preparar y ejecutar la aplicación | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Archivo encontrado y su ubicación. Si confirmamos que falta, destacarlo en negrita y explicar qué preparar con tu equipo.] |
| Carpeta de trabajo del contenedor: `WORKDIR /code` | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Indicar Dockerfile y etapa revisada. Si usa otra carpeta, sugerir `WORKDIR /code` y revisar rutas relativas con tu equipo. Si no se puede determinar, explicar qué falta confirmar; no editar automáticamente.] |
| Archivo `.env.example` de la aplicación | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Ruta encontrada o ausencia confirmada. Si falta, sugerir prepararlo con nombres de variables y valores vacíos, sin copiar un .env real.] |
| Nombres de variables según el framework | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Comparar los nombres aplicables de FastAPI o Django según la referencia. Enumerar faltantes o nombres a ajustar, sin valores; aclarar grupos no aplicables y dudas. Para otros frameworks, indicar que esas listas no aplican, sin afirmar una validación no realizada.] |
| Rama [test / main]: la versión del código que se usará para este entorno | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Indicar qué se comprobó. Si solo tenemos información guardada en este equipo, aclarar qué falta confirmar en GitLab; no afirmar que la rama no existe allí.] |
| Últimos cambios incluidos en esa rama | [🔴 Falta / 🟡 Por confirmar / 🟢 Comprobado] | [Confirmar con tu equipo que los cambios estén guardados y enviados a esa rama en GitLab. Usar verde solo si se comprobó; si faltan cambios confirmados, rojo; si no sabemos, amarillo.] |

### Qué falta confirmar

[Lista breve: qué falta, por qué importa y cuál es el siguiente paso. Si no falta nada, indicarlo sin afirmar que el despliegue fue probado.]

### De dónde obtuvimos los datos

[Archivos que respaldan los datos, por ejemplo el nombre y versión del framework. Usar rutas breves; no incluir secretos ni volcados de comandos de Git.]

### Entrega segura

- Preparar por PWPush o Passbolt solo la configuración necesaria para este entorno; no pegar secretos ni el contenido del .env en el ticket, el chat o este documento.
- Dejar en blanco los usuarios y contraseñas de los nuevos recursos que creará el equipo de despliegue; ese equipo los asignará.
- Revisar solo los nombres de variables de `.env.example`, nunca mostrar sus valores. Encontrar el archivo o sus nombres no confirma que la aplicación los utilice correctamente.
- La contraseña de PWPush se comparte por el canal privado autorizado, nunca junto al enlace en el ticket.
- La existencia de archivos Docker no acredita su funcionamiento; la existencia de una rama no acredita que esté actualizada.

## Herramientas y gestión de accesos — No copiar al ticket

Estas herramientas te ayudan a consultar el estado del despliegue y a manejar la configuración de forma segura:

- **Argo CD:** permite consultar el estado del despliegue de tu aplicación.
- **Vault:** guarda la configuración y los datos sensibles del entorno. Los desarrolladores solo tienen acceso en TEST.
- **Passbolt:** permite compartir contraseñas de forma segura dentro de la empresa.

| Herramienta | TEST | PROD |
| --- | --- | --- |
| Argo CD | `argocd.ecomdev.ar` | https://argocd.ecom.com.ar/ |
| Vault | `vault.ecomdev.ar:8200` | Los desarrolladores no tienen acceso. |
| Passbolt | https://pb2.ecom.com.ar/ | https://pb2.ecom.com.ar/ |

Para acceder, los miembros deben estar dados de alta en el grupo cuyo nombre corresponda a su equipo. Solo pueden acceder a las aplicaciones de su propio equipo. Estas referencias no confirman que el acceso esté concedido ni que los servicios estén disponibles.

### Si no ves las aplicaciones de tu equipo

El **líder de equipo** debe realizar la solicitud administrativa formal mediante un **ticket dirigido a Jefatura**, para que luego se derive a **Jefatura de Infraestructura**.

En ese pedido de acceso debe indicar, por cada persona:

- Nombre y apellido.
- Equipo.
- Nombre de usuario LDAP (tu usuario corporativo), nunca su contraseña.

Esta gestión es independiente del ticket de despliegue; no agregar estos datos administrativos al contenido para copiar. No se solicitaron altas ni se modificaron permisos mediante este documento.
