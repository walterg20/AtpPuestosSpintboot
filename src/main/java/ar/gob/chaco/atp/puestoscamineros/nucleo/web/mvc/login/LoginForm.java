package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form-backing object del formulario de login (plan §4.3).
 *
 * <p>Es el objeto de binding de {@code th:object} de {@code templates/nucleo/login.html}. Los tres
 * campos reproducen los atributos del legacy: {@code &Tipo} (rótulo <em>Acceso</em>), {@code &Usuario}
 * (rótulo <em>Usuario</em>) y {@code &UsuClave} (rótulo <em>Contraseña</em>).
 *
 * <p><strong>La clave nunca se vuelve a renderizar</strong>: tras un intento fallido el controlador
 * la deja en {@code null}, de modo que el HTML de la respuesta no la contiene (plan §4.3).
 *
 * <p><strong>Nota sobre {@code usuario}</strong>: el campo <em>no</em> lleva {@code @NotBlank}. El
 * legacy muestra el mensaje propio {@code Usuario inválido} cuando el identificador viene vacío
 * (R-LOG-021) <strong>sin</strong> consultar la base de datos; con {@code @NotBlank} el mensaje
 * sería el del binder y el botón quedaría deshabilitado de forma permanente. La regla de
 * emptiness la aplica el controlador; el binder sólo acota el largo (10 caracteres, el
 * {@code CHARACTER(10)} del legacy).
 */
public class LoginForm {

    /** Identificador del usuario: {@code CHARACTER(10)} en el legacy. */
    @Size(max = 10, message = "El usuario no puede tener más de 10 caracteres.")
    private String usuario;

    /** Clave del usuario: {@code CHARACTER(10)} en el legacy. */
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(max = 10, message = "La contraseña no puede tener más de 10 caracteres.")
    private String clave;

    /** Tipo de acceso: {@code PUE} (Puesto Caminero) o {@code REC} (Receptoría). */
    @NotBlank(message = "El acceso es obligatorio.")
    @Pattern(regexp = "PUE|REC", message = "El acceso debe ser PUE o REC.")
    private String tipo;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
