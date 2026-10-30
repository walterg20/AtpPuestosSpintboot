package ar.com.atp.puestos.nucleo.web.mvc.login;

/**
 * Tipos de acceso del formulario de login (spec 03-login §3.1, plan §4.3).
 *
 * <p>El legacy usa el valor {@code Tipo} de la variable {@code &Tipo}: {@code 'PUE'} cuando el
 * acceso es por Puesto Caminero y cualquier otro valor cae en la rama de Receptoría. El selector
 * nuevo <strong>no</strong> renderiza una opción vacía (SA-LOG-13): el único desvío funcional
 * deliberado de la UI, porque en el legacy un tipo vacío caía silenciosamente en la rama de
 * Receptoría.
 *
 * <p>Los <strong>códigos</strong> ({@code PUE} / {@code REC}) son parte del contrato observable:
 * son los valores de la cookie {@code SUCURSAL} y los que viaja al servicio de acceso.
 */
public enum TipoAcceso {

    /** Puesto Caminero: el acceso por puesto (el legacy compara {@code &Tipo = 'PUE'}). */
    PUE("PUE", "Puesto Caminero"),

    /** Receptoría: el acceso por receptoría. */
    REC("REC", "Receptoría");

    /** Código que viaja al servicio y que se persiste en la cookie {@code SUCURSAL}. */
    private final String codigo;

    /** Texto visible de la opción del selector. */
    private final String descripcion;

    TipoAcceso(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    /** @return el código del tipo de acceso ({@code PUE} / {@code REC}) */
    public String codigo() {
        return codigo;
    }

    /** @return el texto visible de la opción del selector */
    public String descripcion() {
        return descripcion;
    }

    /**
     * Traduce un código (por ejemplo, el valor de la cookie {@code SUCURSAL}) al tipo de acceso.
     *
     * @param codigo código a traducir; puede ser {@code null}
     * @return el tipo de acceso o {@code null} si el código no corresponde a ninguno
     */
    public static TipoAcceso desdeCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (TipoAcceso tipo : values()) {
            if (tipo.codigo.equalsIgnoreCase(codigo.trim())) {
                return tipo;
            }
        }
        return null;
    }
}
