package ar.com.atp.puestos.support;

import java.text.Normalizer;
import java.util.Comparator;

/**
 * Comparador que reproduce el orden primario del collation espanol de SQL Server usado por
 * Rentas, para validar las clausulas {@code ORDER BY} en los IT de lectura de T-404
 * (plan.md §5).
 *
 * <p>No alcanza con {@link String#CASE_INSENSITIVE_ORDER} ni con {@link java.text.Collator}:
 * <ul>
 *   <li>Java ordena por code point, ubicando la letra enie (U+00D1) despues de {@code Z};
 *       la collation de Rentas es accent-insensitive y la pliega a {@code N}
 *       (evidencia: "NANDUBAY" ordena antes que "NAPALPI").</li>
 *   <li>{@code Collator} ignora el espacio en todos los niveles de fuerza, mientras que SQL Server
 *       le asigna un peso primario menor que el de las letras (evidencia: "CAMPO DE LA CHOZA"
 *       ordena antes que "CAMPO DECASO").</li>
 * </ul>
 *
 * <p>Por eso se calcula una clave de orden primaria: signos y espacios ({@code 0x01}) &lt; digitos
 * ({@code 0x20..0x29}) &lt; letras ({@code 0x41+indice}). Mayusculas y acentos (incluida la enie)
 * se pliegan, de modo que las diferencias de caso o acento resultan en comparacion {@code 0}
 * (empate aceptado por {@code isSortedAccordingTo}), sin producir falsos negativos.
 */
public final class SqlCollation {

    private static final char PUNCT_WEIGHT = 0x01;
    private static final char DIGIT_BASE = 0x20;
    private static final char LETTER_BASE = 0x41;

    private SqlCollation() {
    }

    /** Orden primario del collation de Rentas (case/accent-insensitive; enie plegada a {@code N}). */
    public static Comparator<String> spanish() {
        return Comparator.comparing(SqlCollation::primaryKey);
    }

    private static String primaryKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder key = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (isMark(c)) {
                continue;
            }
            char upper = Character.toUpperCase(c);
            if (upper >= 'A' && upper <= 'Z') {
                key.append((char) (LETTER_BASE + (upper - 'A')));
            } else if (upper >= '0' && upper <= '9') {
                key.append((char) (DIGIT_BASE + (upper - '0')));
            } else {
                key.append(PUNCT_WEIGHT);
            }
        }
        return key.toString();
    }

    private static boolean isMark(char c) {
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}
