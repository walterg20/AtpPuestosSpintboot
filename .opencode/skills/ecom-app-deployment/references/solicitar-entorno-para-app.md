# Solicitar entorno para aplicación (copia local)

> **Estado:** referencia histórica desactualizada. Para preparar solicitudes usa [las reglas actuales](request-checklist.md) y [la plantilla editable](../assets/deployment-request.md). Docker y ramas se revisan fuera del ticket; la navegación, los campos y las reglas de DNS actuales están en esas referencias.
>
> **Fuente de esta copia:** [wiki.ecom.com.ar/es/ECOM-Desarrollo/solicitar-entorno-para-app](https://wiki.ecom.com.ar/es/ECOM-Desarrollo/solicitar-entorno-para-app) (requiere VPN e inicio de sesión en la wiki).
> **Última edición en la wiki:** 13/08/2025.
> **Última sincronización de esta copia:** 2026-10-21.
>
> Se conserva el contenido original como antecedente, no como plantilla vigente. Las fechas siguientes corresponden a la copia previa; este cambio no representa una nueva sincronización ni una verificación de la wiki. Las imágenes de la página original no se incluyen.

En esta guía veremos cómo solicitar un entorno para aplicaciones y conceptos generales importantes.

## Introducción obligatoria

Para los despliegues utilizamos dos clusters de Kubernetes independientes, uno para pruebas y uno para producción. El código de los proyectos se versiona utilizando un GitLab on premise y las imágenes buildeadas de Docker se almacenan en el Container Registry del mismo.

En Docker las imágenes se generan a partir del código existente en el repositorio, por lo que al reiniciar el sitio, ya sea por una actualización o fallo, se perderá todo lo que no estaba en la imagen al momento del inicio. En casos en los que sea necesario subir archivos persistentes, por ejemplo para un portal de noticias, es necesario utilizar PVC (Persistent Volume Claim), el cual debe ser solicitado previamente.

Nuestro servidor GitLab está organizado por grupos, los cuales corresponden a equipos definidos en Active Directory, facilitando la administración de permisos en las distintas herramientas. Además es necesario contemplar lo siguiente en la solicitud:

- **URL del sitio:** la URL/dominio debe coincidir con el slug del repositorio git (último campo de la URL). Ejemplo: si el dominio va a ser `gisloteria.chaco.gob.ar`, la URL del repositorio debería ser `https://git.ecom.com.ar/equipo11/gisloteria`; en este caso su slug (último campo de la URL) es `gisloteria`. Esto sirve para organizar todo lo relacionado al proyecto.
- **URL del repositorio:** debe crearse en base al nombre que va a tener el dominio y el slug debe ser único. Hay dos tipos de agrupaciones:
  - **Desarrollos monolíticos:** sitios donde todo lo necesario para el proyecto se encuentra en el mismo repositorio. Ej.: `https://git.ecom.com.ar/equipo11/bienal`.
  - **Microservicios:** hay que agruparlos bajo un subgrupo y el último campo tiene que ser `nombre_tipo`. Esta relación debe respetarse. Los tipos acordados son:
    1. **API:** si el microservicio es una API, el repositorio debería tener un grupo con el nombre del mismo y el slug sería `proyecto_api`. Ej.: `https://git.ecom.com.ar/equipo21/chacobus/chacobus_api`.
    2. **WEB:** si el microservicio es una web, el repositorio debería tener un grupo con el nombre del mismo y el slug sería `proyecto_web`. Ej.: `https://git.ecom.com.ar/equipo21/chacobus/chacobus_web`.
    3. **APP:** si el microservicio es una app mobile, el repositorio debería tener un grupo con el nombre del mismo y el slug sería `proyecto_app`. Ej.: `https://git.ecom.com.ar/equipo21/chacobus/chacobus_app`.
    4. **OTRO:** si el microservicio realiza una función particular, el repositorio debería tener un grupo y el slug sería `proyecto_nombreservicio`. Ej.: `https://git.ecom.com.ar/equipo21/chacobus/chacobus_traccar`.
    5. **Proyecto:** si el microservicio contiene un desarrollo web (ej.: API + vistas) y se va a utilizar, por ejemplo, como backend, el slug va a ser el nombre del proyecto (URL). Ej.: `https://git.ecom.com.ar/equipo20/productores/productores`.

    El nombre/descripción del proyecto es indiferente; lo importante es el slug, que se puede definir al crear el proyecto y también puede ser editado por su creador.
- **Tipo de acceso:** es necesario aclarar si tiene que ser visible desde fuera de la red. Al ser accesible por internet, también lo será por intranet.
- **Observaciones:** campo opcional por si es necesario agregar información complementaria.
- **Comandos:** comandos que tienen que ejecutarse en cada actualización, después de que el pod ya está funcionando. Si se necesitan varios, separarlos con `&&`. Ejemplo de comando inicial: `php artisan migrate`.
- **Volumen persistente (PVC):** se define de forma relativa a la carpeta del proyecto. Ejemplo: `public/uploads` o `/public/uploads`. **Importante:** el usuario php no puede crear carpetas en el PVC; si van a existir subcarpetas en `public/uploads`, es necesario agregarlo como observación.

Los repositorios deben contar con al menos dos ramas: `main` para el entorno de producción y `test` para el entorno de pruebas.

Las aplicaciones cuentan con CI/CD solo en las ramas definidas más arriba. El estado del despliegue puede verse en los pipelines de GitLab; en caso de error, notificarlo y adjuntar captura.

## Cómo solicitar un entorno

El jefe de equipo o de proyecto debe solicitarlo a través del sistema "Gestión Tickets", al que se accede desde MIECOM. Para facilitar la solicitud se utiliza una plantilla, detallada más abajo. Para crearla hay que ir al menú superior derecho y hacer clic en "Plantillas"; luego escribir "kube" en el buscador y hacer clic en el botón "+".

## Plantilla de ticket "Solicitud de despliegue de aplicaciones en cluster Kubernetes"

- **Repositorio del código:** enlace al repositorio git.
- **Framework (symfony, laravel, etc.):** nombre del framework utilizado y su versión.
- **Lenguaje utilizado (php, python, etc.):** nombre y versión del lenguaje.
- **¿En qué ambiente/s es necesario desplegar? (TEST / PROD):** se puede solicitar un solo entorno por ticket.
- **¿Está armado el Dockerfile? (especificar ruta):** los proyectos deben contar con un Dockerfile funcional, preferentemente en la raíz del proyecto.
- **¿Está armado el docker-compose? (especificar ruta):** es necesario que exista un docker-compose para poder replicar la infraestructura del sitio.
- **Comandos necesarios para el funcionamiento del sitio:** deben ir en orden; no se ejecutará ningún comando que no esté solicitado en el ticket. En el caso de Laravel/Symfony, tener en cuenta que al momento del build se ejecuta `composer install --no-scripts`, por lo que es necesario aclarar los comandos post-cmd. Se hace de esta forma para tener más control y evitar demoras ejecutando `cache:clear`, ya que al no ser persistente, la caché se limpia en cada actualización.
- **Comandos necesarios para cada actualización (por ejemplo: `doctrine:migration:migrate`):** especificar los comandos necesarios al momento de cada actualización del sitio.
- **DNS a utilizar:** definir el dominio/URL que va a tener el sitio; debe coincidir con el nombre de la aplicación y del proyecto en git.
- **¿La aplicación es de acceso solo interno o "interno y externo"?:** los sitios solo serán accesibles internamente (VPN/intranet), a menos que se solicite lo contrario.
- **Base de datos:**
  - ¿Existe? (indicar datos de acceso): IP del servidor y nombre de la base de datos.
  - Tecnología de la BD (MariaDB, PostgreSQL, etc.).
- **¿Utiliza Redis o Memcached?:** definir el sistema de caché y su versión.
- **¿Necesita volumen persistente?:** en caso afirmativo, indicar el tamaño de cada volumen. Todo el contenido del sitio se elimina y vuelve a descargar en cada actualización, por lo que es necesario definir qué carpetas deben persistir; esto es muy importante para sitios en los que se suben archivos. El directorio definido por convención es `uploads`. Se recomienda usar un solo directorio; dentro de él se pueden definir todas las subcarpetas necesarias, pero hay que crearlas manualmente, por lo que deben figurar en el ticket.
  - **Importante:** al crear un PVC se monta un directorio sobre la ruta, lo cual sobrescribe su contenido.
- **Información sensible:** por seguridad, no se dejan a la vista datos que puedan representar una vulnerabilidad. Para compartir esta información se utiliza pb2 o, en su defecto, pwpush. El link debe tener clave y una duración de 5 días; se comparte solo el link y la clave puede enviarse por privado al área.
  - Indicar por dónde se enviará la información sensible (passwords, tokens, etc.).
- **Info complementaria:** cualquier información que ayude a comprender el escenario.
