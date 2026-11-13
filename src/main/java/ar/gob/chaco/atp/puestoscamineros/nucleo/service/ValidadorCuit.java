package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.util.Set;

/**
 * Validador del dígito verificador del CUIT, reproduciendo verbatim el procedure legacy
 * {@code Nuc_ValidaCuit} (módulo 11, spec {@code 11-validacion-cuit}).
 *
 * <p>Servicio <strong>puro</strong>: sin acceso a repositorio ni a BD, determinista y sin efectos
 * secundarios (R-CUIT-008). Se reutiliza desde cualquier módulo (R-CUIT-009).
 *
 * <p>Algoritmo (§3.3 de la spec):
 * <ol>
 *   <li>CUIT con exactamente 11 dígitos numéricos (sin normalización).</li>
 *   <li>Primeros 10 dígitos forman el cuerpo {@code Docu}.</li>
 *   <li>Suma ponderada con pesos {@code 5432765432} (módulo 11 estándar AFIP).</li>
 *   <li>{@code UltDig = 11 - (Acu mod 11)}; si {@code UltDig == 11} ⇒ {@code 0} (caso
 *       {@code Resto = 0}). Caso {@code Resto = 1} ⇒ {@code UltDig = 10}, que nunca coincide con
 *       un dígito 0–9 ⇒ inválido.</li>
 *   <li>Prefijo (primeros 2 dígitos) debe pertenecer a {@code {20,23,24,27,30,33,34}}.</li>
 * </ol>
 */
public final class ValidadorCuit {

    /** Conjunto de prefijos válidos (tipo de persona AFIP), según el source verbatim. */
    private static final Set<Integer> PREFIJOS_VALIDOS =
            Set.of(20, 23, 24, 27, 30, 33, 34);

    /** Pesos del módulo 11 estándar AFIP, de izquierda a derecha sobre los 10 dígitos del cuerpo. */
    private static final String PESOS = "5432765432";

    private ValidadorCuit() {
        // Clase de utilidad: no instanciable.
    }

    /**
     * Valida un CUIT recibido como {@link Long} (equivalente al {@code NUMERIC(11,0)} del legacy).
     *
     * @param cuit CUIT a validar (puede ser {@code null})
     * @return {@code true} si el CUIT es válido
     */
    public static boolean esValido(Long cuit) {
        if (cuit == null || cuit < 0L) {
            return false;
        }
        String s = String.format("%011d", cuit);
        return esValido(s);
    }

    /**
     * Valida un CUIT recibido como {@link String} de 11 dígitos. No normaliza: si contiene
     * caracteres no numéricos, espacios o guiones, se considera inválido (R-CUIT-005).
     *
     * @param cuit CUIT a validar (puede ser {@code null})
     * @return {@code true} si el CUIT es válido
     */
    public static boolean esValido(String cuit) {
        if (cuit == null) {
            return false;
        }
        if (cuit.length() != 11) {
            return false;
        }
        for (int i = 0; i < 11; i++) {
            if (!Character.isDigit(cuit.charAt(i))) {
                return false;
            }
        }
        // Primeros 10 dígitos forman el cuerpo (Docu).
        String docu = cuit.substring(0, 10);
        int acu = 0;
        for (int i = 0; i < 10; i++) {
            int digito = docu.charAt(i) - '0';
            int peso = PESOS.charAt(i) - '0';
            acu += digito * peso;
        }
        int resul = acu / 11;          // Int(Acu / 11)
        int resto = acu - resul * 11;  // Acu - Resul * 11
        int ultDig = 11 - resto;
        if (ultDig == 11) {
            ultDig = 0;
        }
        int ult = cuit.charAt(10) - '0';          // último dígito informado
        int dosPriDig = (docu.charAt(0) - '0') * 10 + (docu.charAt(1) - '0');
        return ultDig == ult && PREFIJOS_VALIDOS.contains(dosPriDig);
    }
}