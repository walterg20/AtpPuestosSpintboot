# Módulo 03 — Login completo de Puestos Camineros

> **Estado**: `PENDIENTE` — la spec todavía **no** existe. Este archivo es un marcador de posición,
> **no** una spec aprobada.

Este módulo especifica el **login completo** de Puestos Camineros, con las características
verificadas del legacy GeneXus.

## Objetos legacy de referencia (leídos de la KB, modo lectura)

| Objeto | Qué aporta |
|---|---|
| `Login` (WebComponent) | Campos `Usuario`, `UsuClave` (password), `Tipo`; `TextBlockPass` rojo; botón "Ingresar"; `Event Start` lee la cookie `SUCURSAL`; `Event Enter` llama a `Seg_ControlAcceso` y mapea el código de error a un mensaje |
| `Seg_ControlAcceso` (Procedure) | Algoritmo completo de acceso: cifrado del usuario, búsqueda en `SCRTY`, comparación de clave, resolución de persona, sucursal/comisión, turno y carga del contexto |
| `Seg_Encripta` (Procedure) | Mecanismo de verificación de credenciales: sustitución monoalfabética reversible de 10 caracteres (alfabeto de 66, mapeo espejo `67-i`, normalización a mayúsculas) |
| `Seg_ControlAccesoWS` (Procedure) | Variante JSON stateless, sin sesión ni chequeo de IP |
| `SDTEntradaLoginWS` / `SDTSalidaLoginWS` | Contrato del WS: `{Usuario, Pass, Tipo}` → `{error, sucursal, comision}` |
| `Seg_BusPer` (Procedure) | Resuelve la Persona desde `PERSONAS.PerCodUsu`; devuelve `-1` si no hay match |
| `Seg_BusComi` / `Seg_BusRece` / `Seg_BusResponsable` | Resolución de puesto/comisión y de receptoría |
| `SetContext` (Procedure) + SDT `Context` | Contexto de sesión: 13 campos, serializado a XML en la sesión |
| `ListPrograms` (Procedure) + `IsAuthorized` | Tabla de autorización: el menú se filtra por programa autorizado |
| `NotAuthorized` (WebPanel) | Página "Not Authorized" |

## Contrato de resultado (verificado)

| Código | Significado | Mensaje del legacy |
|---:|---|---|
| 0 | Acceso concedido | — |
| 1 | Credenciales inválidas | *Usuario/Contraseña Incorrecto* |
| 2 | Credenciales inválidas | *Usuario/Contraseña Incorrecto* |
| 3 | Usuario no relacionado con una Persona | *Este Usuario no esta relacionado con una Persona del sistema* |
| 4 | Sin Receptoria o puesto caminero asignado | *No existe Receptoria o puesto caminero asignado* |
| 5 | Debe cambiar su clave (`UsuIngreso = 1`) | *Debe cambiar su clave desde el Sistema Integrado (win)* |
| 6 | IP no habilitada para el puesto | *Dispositivo no habilitado para el uso en el puesto caminero, contacte con el administrador* |

## Decisiones ya tomadas (2026-10-22)

- **Autenticación**: Spring Security con **sesión HTTP + CSRF** para la UI Thymeleaf.
- **Verificación de credenciales**: se **reproduce el cifrado legacy `Seg_Encripta` tal cual**,
  sin PBKDF2. Cobertura 550/552 usuarios; las 2 cuentas con `UsuClave` vacía tienen
  `UsuIngreso = 1` y el legacy ya las deriva al error 5, con lo que el desvío es coherente.
- **Canal WS**: se mantiene `POST /api/v1/auth/login` (stateless) con la forma de
  `SDTSalidaLoginWS`, por el consumo mobile del legacy.
- **Alcance**: **sólo el login de ATPPuestos**. No se implementa el perfil de actor externo
  (contribuyente) del Portal Contribuyente.

## Datos ya verificados

- Esquema físico de `SCRTY`, `PERSONAS`, `PUEPPRIM`, `RECEPTOR`, `COMISION`, `PPRIM_TU`:
  `docs/analisis-ATPPuestos.md` §15.
- Columnas huérfanas de `SCRTY` y cobertura de credenciales: idem §15.2 / §15.3.
- Grupos (`UsuGrupo`) y niveles (`UsuNiv`) presentes: idem §15.4.

## Reglas que condicionan la spec

- El login **no lanza excepción**: devuelve el código 0-6 y la vista lo muestra junto al campo
  (paridad con el `TextBlockPass` rojo del legacy). El `GlobalExceptionHandler` queda para los
  errores no previstos.
- El contexto de sesión replica el SDT `Context` de 13 campos.
- La cookie `SUCURSAL` replica la persistencia del tipo de acceso entre sesiones.
- Sin DDL: `SCRTY` y las demás tablas se mapean tal como están.
