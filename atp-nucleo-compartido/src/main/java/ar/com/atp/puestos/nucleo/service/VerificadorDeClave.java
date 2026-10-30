package ar.com.atp.puestos.nucleo.service;

/**
 * Única puerta al mecanismo de credenciales (D-LOG-09).
 *
 * <p>La implementación vigente es {@link CodificadorSegEncripta}, que reproduce el
 * {@code Seg_Encripta} del legacy <em>tal cual</em> (D-LOG-03). Una implementación alternativa
 * (p. ej. PBKDF2 sobre {@code UsuHash}, SA-LOG-03) puede entrar por esta misma interfaz
 * <strong>sin tocar</strong> {@link ServicioAcceso} ni los adaptadores.
 */
public interface VerificadorDeClave {

    /**
     * Codifica un texto con el mecanismo vigente (equivale a {@code Seg_Encripta}).
     *
     * @param texto texto en claro (se normaliza a mayúsculas)
     * @return el texto codificado
     */
    String codificar(String texto);

    /**
     * Compara un texto codificado con el valor almacenado.
     *
     * <p>La comparación ignora el relleno de espacios final en ambos lados (paridad con el
     * {@code RTRIM} del legacy, plan §10 R-8).
     *
     * @param codificado texto codificado (recién calculado)
     * @param almacenado valor almacenado en la base
     * @return {@code true} si coinciden
     */
    boolean coincideCon(String codificado, String almacenado);

    /**
     * Deshace la codificación (reversibilidad del mecanismo — R-LOG-003).
     *
     * <p>Quita antes el relleno de espacios final del {@code char(10)} de la base (el alfabeto no
     * produce espacios, de modo que un espacio final es siempre relleno) ⇒ reproduce el dato
     * verificado de la spec §3.2.1: 3 de los 552 {@code UsuId} decodifican a 9 caracteres y no a
     * 10.
     *
     * @param codificado texto codificado
     * @return el texto en claro
     */
    String decodificar(String codificado);
}
