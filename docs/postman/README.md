# Colección Postman — ATPPuestos API

Artefacto **obligatorio** del backend. Debe mantenerse **sincronizado** con los
controladores (`atp-nucleo-compartido/src/main/java/.../nucleo/web/rest/`) en cada cambio de
contrato REST.

## Archivos

| Archivo | Propósito |
|---|---|
| `ATPPuestos-API.postman_collection.json` | Colección Postman **v2.1.0** de todos los endpoints. |
| `ATPPuestos-Local.postman_environment.json` | Environment local (`baseUrl`, códigos de ejemplo). |

## Regla obligatoria (G-POSTMAN)

> **Cada vez que se agregue, modifique o elimine un endpoint del backend, la colección
> de Postman DEBE actualizarse en el mismo cambio (mismo commit).**

Lineamientos:

1. **Un request por endpoint real.** No inventar rutas que el código no exponga.
2. **Actualizar el `info.description`** si cambia la versión del API o la base (`/api/v1`).
3. **Bodies alineados a los DTOs de escritura** (`*EscrituraDto`), respetando nombres
   JSON, campos `@NotNull`/`@NotBlank` y tamaños `@Size`.
4. **Variables, no valores fijos**: usar `{{baseUrl}}` y las variables de recurso
   (`{{provCod}}`, `{{locCod}}`, `{{callCod}}`, `{{camID}}`, `{{areaId}}`, `{{jurCod}}`,
   `{{ferFecha}}`, `{{prodCod}}`).
5. **Scripts de test `201`** en los POST capturan el id del recurso creado en la
   variable de colección correspondiente (encadenable).
6. **Estados esperados**: POST → `201` + `Location`; PUT → `200`; DELETE → `204`;
   GET inexistente → `404`; duplicado → `409`; validación → `400`.
7. La colección se **versiona en Git** junto al código del backend (no es un secreto).

## Dos canales de respuesta

| Canal | Prefijo | Formato | Productor |
|---|---|---|---|
| API REST | `/api/**` | JSON, cuerpo de error `{code, message}` | `implementador-backend` (`web/rest`) |
| Páginas | resto | HTML Thymeleaf | `implementador-vistas` (`web/mvc`) |

El `GlobalExceptionHandler` (advice **global**) decide el canal por el prefijo de ruta: las 404 y
405 las lanza el `DispatcherServlet`, fuera de todo controller, así que un advice acotado por
paquete no las cubriría. Esta colección documenta **sólo el canal `/api/**`**.

## Cómo importar

1. Postman → **Import** → seleccionar ambos archivos `.json`.
2. Seleccionar el environment **ATPPuestos - Local** (arriba a la derecha).
3. Ajustar `baseUrl` si el backend corre en otro host/puerto (`application.yml`,
   `server.port`, por defecto `8080`).

## Cobertura actual (módulos `01-nucleo-datos` y `02-abm-productos`)

9 carpetas / 46 requests:

- **Health** — `GET /api/v1/health`
- **Provincias** — listar, obtener, crear, actualizar, eliminar
- **Localidades** — listar (filtro `provCod`/todas), obtener, `nombre` (`Bus_DesLoc`), ABM
- **Calles** — listar (filtro `q`/todas), obtener, ABM
- **Caminos** — listar, obtener, `ruta` (`BuscarCamino`), ABM
- **Areas** — listar, ABM
- **Jurisdicciones** — listar, ABM
- **Feriados** — listar (rango `desde`/`hasta` obligatorio), ABM
- **Productos** — listar (filtro `anulados`), obtener, `importes-vigentes` (`ProdIMax1..4`), ABM
  y los dos casos de validación del DTO de escritura (`400 VALIDACION`)

> **Validación del DTO de escritura de Productos (T-102 / D-32).** `prodCod` está acotado al rango
> `0…32767` de la columna física `PRODUCTO.ProdCod` (`smallint`) y los 4 importes de cada vigencia
> al tipo lógico `NUMERIC(12,2)` (hasta 2 decimales, `±9999999999.99`). Antes de T-102, un
> `prodCod` fuera de rango o un importe con 3 decimales llegaban a SQL Server y la API respondía
> `500 ERROR_INTERNO`; ahora responden `400 VALIDACION` (hallazgo N-4). Los requests
> *"Crear producto - prodCod fuera de rango (400 VALIDACION)"* y
> *"Crear producto - importe con 3 decimales (400 VALIDACION)"* cubren ambos casos.
> Los 7 requests felices de `Productos` quedan **intactos**.

> **Tolerancia a FK huérfana (R-024/R-025/R-207).** `GET /api/v1/localidades` sin `provCod`
> devuelve las 3989 localidades de `Localida`, incluidas las 30 con `ProvCod = 0`
> (referencia huérfana: `PROVINCI` sólo tiene `ProvCod` 1..24). Esas filas llegan con
> `provCod = 0` —el **valor físico** de la columna `Localida.ProvCod` (R-207)— y **sin**
> `provNombre`: la provincia no existe y no se inventa. La clave `provCod` está **siempre**
> presente en el JSON (la inclusión `non_null` de `application.yml` sólo omite los `null`).
> El request "Listar localidades (todas)" verifica ambas cosas como regresión.

## Changelog

| Fecha | Cambio | Tarea |
|---|---|---|
| 2026-10-21 | `LocalidadDto.provCod` expone el **valor físico** de la FK `Localida.ProvCod`: las 30 filas huérfanas devuelven `provCod = 0` (antes la clave se **omitía** del JSON y el cliente recibía `undefined`) y `provNombre` sigue ausente. Se actualizaron los scripts de test de los 3 endpoints de lectura y se agregaron ejemplos guardados con el cuerpo real. | T-410 (R-207) |
| 2026-10-22 | Estructura del proyecto reorganizada en monorepo Maven: los controladores pasaron de `nucleo/web/controller/` a `nucleo/web/rest/`. **La colección no cambia**: ni rutas, ni cuerpos, ni estados. | T-003 |
| 2026-10-22 | **T-102 / D-32**: refuerzo de la validación del DTO de escritura de `Productos` (`prodCod` en `0…32767`; los 4 importes con `@Digits(integer=10, fraction=2)`). **Cambio de contrato REST**: un `prodCod` fuera de rango o un importe con 3 decimales pasan de `500 ERROR_INTERNO` a `400 VALIDACION`. Se agregaron 2 requests de prueba a la carpeta `Productos`; los 7 requests previos quedan intactos. | T-102 / T-306 |

## Fuente de verdad

Los contratos se derivan de:

- `openspec/features/01-nucleo-datos/spec.md` (reglas R-001…R-037).
- `openspec/features/01-nucleo-datos/plan.md` (diseño de endpoints y DTOs).
- Código vigente en `atp-nucleo-compartido/src/main/java/.../nucleo/web/rest/`.

Si hay divergencia, **el código y la especificación mandan**; corregir la colección.
