package ar.com.atp.puestos.nucleo.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la peticion del canal WS ({@code POST /api/v1/auth/login}) — forma del SDT
 * {@code SDTEntradaLoginWS} del legacy (spec 03-login §4.1).
 *
 * <p>El contrato canonico usa claves en minusculas; los <strong>alias legacy</strong>
 * ({@code Usuario}, {@code Pass}, {@code Tipo}) se aceptan por compatibilidad con el cliente
 * existente (SA-LOG-12).
 *
 * @param usuario identificador del usuario (maximo 10 caracteres, como el {@code CHARACTER(10)}
 *        del legacy)
 * @param pass clave del usuario (maximo 10 caracteres)
 * @param tipo tipo de acceso: {@code PUE} (puesto caminerero) o {@code REC} (receptoria)
 */
public record AuthLoginRequestDto(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 10, message = "El usuario no puede superar los 10 caracteres")
        @JsonAlias("Usuario")
        String usuario,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 10, message = "La contrasena no puede superar los 10 caracteres")
        @JsonAlias("Pass")
        String pass,

        @NotBlank(message = "El tipo de acceso es obligatorio")
        @Size(max = 3, message = "El tipo de acceso no puede superar los 3 caracteres")
        @Pattern(regexp = "PUE|REC", message = "El tipo de acceso debe ser PUE o REC")
        @JsonAlias("Tipo")
        String tipo) {
}
