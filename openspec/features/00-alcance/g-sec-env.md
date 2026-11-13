# Regla G-SEC-ENV — No leer ni exponer secretos del `.env`

**Estado**: APROBADO (gobernanza del arnés).

## Regla

El agente **NO DEBE** leer, imprimir ni exponer en ninguna respuesta los valores de variables
sensibles definidas en `.env` (ni en `application.yml`, ni en variables de entorno del sistema).
Esto incluye, pero no se limita a:

- `DB_PASSWORD`
- `JWT_SECRET`
- `SPRING_DATASOURCE_PASSWORD`
- tokens, API keys y cualquier credencial.

## Comportamiento esperado

1. Si el usuario solicita credenciales, el agente debe responder únicamente con instrucciones de
   dónde encontrarlas (archivo local, gestor de secretos) y **nunca** con el valor real.
2. Si necesita usar una credencial para una operación, debe referenciar la variable de entorno por
   nombre, no su valor.
3. El agente no debe ejecutar comandos que impriman el contenido de `.env` ni de archivos de
   configuración que contengan secretos.

## Justificación

Evitar la exposición accidental de secretos en logs, respuestas o memoria persistente (Engram),
alineado con la regla 5 de `AGENTS.md` ("Seguridad: no incluir contraseñas, tokens o secretos en
texto plano").

## Aplicación

Esta regla se aplica a todos los subagentes del arnés (`analista-gx`, `implementador-backend`,
`implementador-vistas`, `verificador-qa`, `coordinador`) y al agente principal.