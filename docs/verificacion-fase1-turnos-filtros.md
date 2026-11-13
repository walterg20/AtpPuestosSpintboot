# Verificación QA — Fase 1 (filtros) listado de Turnos

**Veredicto:** PASS

## Comando ejecutado

```
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
cd D:\Desrrollo\ATPPuestoLab\AtpPuestosSpintboot
.\mvnw.cmd -pl atp-nucleo-compartido test "-Dtest=TurnoMvcControllerTest"
```

Resultado: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` · `BUILD SUCCESS` (exit 0).
Log completo: `C:\Users\walterg20\AppData\Local\Temp\opencode\turno-test.log`.

## 1. GET /turnos?busqueda=5&estado=A devuelve 200 y acepta filtros

Confirmado por el test `listarAceptaFiltrosDeBusqueda` (línea 300 de `TurnoMvcControllerTest`):

```java
mockMvc.perform(get("/turnos").param("busqueda", "5").param("estado", "A"))
    .andExpect(status().isOk())
    .andExpect(model().attribute("busqueda", "5"))
    .andExpect(model().attribute("estado", "A"));
```

El controller `TurnoMvcController.listar` (líneas 94-124) declara `@RequestParam` para `busqueda`, `desde`, `hasta` y `estado`, los parsea y construye `FiltrosTurno`, pasándolos a `ServicioTurnos.listar(contexto, filtros, pageable)`.

## 2. TurnoRepositorio.listarPorSucursalConFiltros existe con filtros opcionales

Archivo: `atp-nucleo-compartido/src/main/java/ar/com/atp/puestos/nucleo/repository/TurnoRepositorio.java` (líneas 93-118).

```java
@Query("select t from Turno t where t.id.sucNroId = :sucNroId "
    + "and t.id.sucTipo = :sucTipo "
    + "and (:busqueda is null or t.id.turNroId = :busqueda or t.turCpcId = :busqueda) "
    + "and (:desde is null or t.turFchApe >= :desde) "
    + "and (:hasta is null or t.turFchApe <= :hasta) "
    + "and (:estado is null or trim(:estado) = '' or t.turEstado = trim(:estado)) "
    + "order by t.id.sucNroId asc, t.id.sucTipo asc, t.id.turNroId desc")
Page<Turno> listarPorSucursalConFiltros(@Param("sucNroId") Integer sucNroId,
    @Param("sucTipo") String sucTipo, @Param("busqueda") Integer busqueda,
    @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
    @Param("estado") String estado, Pageable pageable);
```

Todos los filtros son opcionales (`null`/en blanco los desactiva). Servicio `ServicioTurnos.listar(contexto, FiltrosTurno, pageable)` (líneas 110-119) los conecta con el repositorio.

## 3. Form de filtros en turnos.html conectado

Archivo: `atp-nucleo-compartido/src/main/resources/templates/nucleo/turnos.html` (líneas 85-109).

- `th:action="@{/turnos}"` con `method="get"` (línea 85) ⇒ CSRF-safe (GET).
- `name="busqueda"` (línea 88), `name="desde"` (línea 92), `name="hasta"` (línea 96), `name="estado"` (línea 100) ⇒ atributos `name` presentes y vinculados a los `@RequestParam` del controller.
- Valores repoblados con `th:value="${busqueda}"`, `${desde}`, `${hasta}` y `th:selected` para estado.

## 4. Tests

- `TurnoMvcControllerTest`: 16 tests, 0 fallos, 0 errores, 0 skipped. BUILD SUCCESS.
- El stacktrace visible en el log corresponde al test `siElListadoNoSeCargaSeMuestraElEstadoDeError`, que deliberadamente provoca una excepción (comportamiento esperado del test) y la captura como error de carga; no es un fallo.

## Conclusión

Fase 1 (filtros) del listado de Turnos está implementada y verificada: endpoint, repositorio con filtros opcionales, form Thymeleaf conectado y tests en verde. No se modificó código.