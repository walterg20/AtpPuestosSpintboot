# Solicitud de despliegue — atppuestosinicial — TEST

Estado: Borrador con pendientes

## Dónde cargar la solicitud

1. Ingresa a https://app.ecom.com.ar/tickets/servlet/com.gestiondesolicitudes.gamexamplelogin.
2. Ve a **Tickets → Plantillas**.
3. Busca **Pedido de Despliegue en Kubernetes** y pulsa **+**.
4. Reemplaza el título precargado y copia únicamente el contenido de la sección indicada.

## Título para copiar

Solicitud de despliegue de aplicaciones en cluster Kubernetes TEST - atppuestosinicial

## Contenido para copiar al ticket

**Repositorio del código:** https://git.ecom.com.ar/atp/AtpPuestosSpintboot

**Framework y version:** Spring Boot 3.5.16

**Lenguaje y version:** Java 17

**Ambiente de despliegue:** TEST

**Comandos de inicialización:** No hay comandos manuales

**DNS a utilizar:** AtpPuestosSpintboot.ecomdev.ar

**Tipo de acceso:** Solo interno

---

**Base de datos y Caché:**

- **Tecnología de BD:** Existente — SQL Server (Rentas)
- **¿Utiliza Redis o Memcached?:** Por confirmar
- **Volumen persistente:** No

**Desarrolladores involucrados:** Julio Godeas (equipo por confirmar), Franco Biolchi (equipo por confirmar), Lucas Nuñes (equipo por confirmar), Velazco Walter (equipo por confirmar)

**Entrega de configuración sensible:** Por confirmar (PWPush o Passbolt)

**Consideraciones para el despliegue:** Es una version test para realizar un despligue inicial.

**Importante:** La información sensible y las variables de entorno deben ser enviadas por pwpush o compartidas a través del Passbolt de la empresa. No incluir sus valores en este ticket.

## Checklist y recordatorios — No copiar al ticket

Esta lista te ayuda a revisar qué falta antes de pedir el despliegue:

- **🔴 Falta:** comprobamos que falta algo necesario o que hay un problema que resolver.
- **🟡 Por confirmar:** hay dudas, información incompleta o algo que todavía no pudimos revisar.
- **🟢 Comprobado:** ese punto concreto está revisado y cumple lo que se estaba comprobando.

El verde no significa que toda la aplicación esté lista. Por ejemplo, encontrar un Dockerfile confirma que existe, no que funcione.

### Atención prioritaria

- **Dockerfile o Docker Compose — Falta.** No se encontró ningún archivo Dockerfile ni Compose en el repositorio. Sin estos archivos el equipo de despliegue no tiene instrucciones para preparar y ejecutar la aplicación. Siguiente paso: consultar con el equipo cuál Dockerfile deben preparar antes del despliegue.
- **Rama `test` — Por confirmar.** No se encontró la rama `test` en este equipo; falta confirmar si existe en el remoto (GitHub) y que tenga los cambios a desplegar.
- **Caché (Redis/Memcached) — Por confirmar.** No se pudo determinar si la aplicación utiliza caché. Confirmar con el equipo.
- **Entrega de configuración sensible — Por confirmar.** Falta definir si se usará PWPush o Passbolt para entregar las variables de entorno (DB_PASSWORD, SPRING_DATASOURCE_PASSWORD, etc.).
- **Equipos de los desarrolladores — Por confirmar.** Faltan los equipos de Julio Godeas, Franco Biolchi, Lucas Nuñes y Velazco Walter para completar el campo de desarrolladores.

### Revisión por puntos

| Qué revisar | Estado | Qué encontramos y qué hacer |
| --- | --- | --- |
| Dockerfile o Docker Compose: archivos que indican cómo preparar y ejecutar la aplicación | 🔴 Falta | No se encontró Dockerfile ni Compose en el repositorio. Consultar con el equipo cuál deben preparar antes del despliegue. |
| Carpeta de trabajo del contenedor: `WORKDIR /code` | 🟡 Por confirmar | No hay Dockerfile para revisar. Infraestructura solicita `WORKDIR /code`; confirmar con el equipo al preparar el Dockerfile. |
| Archivo `.env.example` de la aplicación | 🟢 Comprobado | Encontrado en la raíz del repositorio (`.env.example`). |
| Nombres de variables según el framework | 🟡 Por confirmar | El framework es Spring Boot (Java), que no está en las listas de FastAPI ni Django de la referencia. Se verificó que los nombres consumidos por la aplicación (`application.yml`) coinciden con los del `.env.example` (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_DRIVER, SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, SPRING_PROFILES_ACTIVE, SERVER_PORT, APP_BASE_URL, SECURITY_*, APP_AUDITORIA_USUARIO). Esto no confirma que la aplicación los utilice correctamente en runtime. |
| Rama test: la versión del código que se usará para este entorno | 🟡 Por confirmar | No se encontró la rama `test` en este equipo. Falta confirmar si está en el remoto (GitHub) y que contenga los cambios a desplegar. |
| Últimos cambios incluidos en esa rama | 🟡 Por confirmar | Confirmar con el equipo que los cambios estén guardados y enviados a la rama objetivo en el remoto. |

### Qué falta confirmar

- Preparar un Dockerfile (y si aplica, Compose) con `WORKDIR /code` y las rutas de COPY/startup revisadas con el equipo.
- Confirmar si existe la rama `test` en GitHub y que tenga los últimos cambios.
- Definir si la aplicación utiliza Redis o Memcached.
- Definir el canal de entrega de configuración sensible (PWPush o Passbolt) y preparar las variables por ese canal.
- Completar el equipo de cada desarrollador (Julio Godeas, Franco Biolchi, Lucas Nuñes, Velazco Walter).
- Asegurarse de que los cambios estén commiteados y pusheados a la rama objetivo antes del despliegue.

### De dónde obtuvimos los datos

- `pom.xml` — Spring Boot 3.5.16, Java 17, Maven Wrapper, módulos `atp-nucleo-compartido` y `atp-puestos-camineros`.
- `atp-nucleo-compartido/src/main/resources/application.yml` — variables de entorno consumidas por la aplicación.
- `.env.example` — nombres de variables de entorno (solo nombres, sin valores).
- `git remote -v` y `git branch -a` — URL del repositorio y ramas locales/remotas.

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