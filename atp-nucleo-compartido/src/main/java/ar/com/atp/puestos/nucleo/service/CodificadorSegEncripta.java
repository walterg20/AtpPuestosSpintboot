package ar.com.atp.puestos.nucleo.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Implementación vigente del mecanismo de credenciales: reproducción <strong>exacta</strong> del
 * procedimiento {@code Seg_Encripta} del legacy (spec 03-login §3.2, D-LOG-03).
 *
 * <p><strong>No es cifrado</strong>: es una sustitución monoalfabética <em>espejo</em> sobre un
 * alfabeto de 66 caracteres, con la regla {@code destino = 67 − origen}. Es reversible en una
 * operación y su constante es pública: el riesgo está documentado y asumido (spec §7, R-1).
 *
 * <p>Algoritmo (traducción literal del {@code Source}):
 *
 * <ol>
 *   <li>el texto se normaliza a mayúsculas;</li>
 *   <li>se recorre el alfabeto de 66 caracteres: {@code ' '..'`'} (ASCII 32-96) con el
 *       apostrophe (ASCII 39) sustituido por {@code 'a'} en el índice 8, más {@code 'b'} en el
 *       índice 66;</li>
 *   <li>se recorren las posiciones 1 a 10 del texto; para cada una se busca su índice
 *       {@code origen} (comparando sólo los índices 1 a 65: el 66 nunca se compara) y se emite el
 *       carácter del índice {@code 67 − origen};</li>
 *   <li>los caracteres <strong>fuera del alfabeto se descartan</strong> (no se emite nada), de modo
 *       que la salida puede ser más corta que la entrada.</li>
 * </ol>
 */
@Component
public class CodificadorSegEncripta implements VerificadorDeClave {

    /**
     * Alfabeto de 66 caracteres: ASCII 32-96 ({@code ' '..'`'}) con el apostrophe (ASCII 39)
     * sustituido por {@code 'a'} (índice 8) y {@code 'b'} añadido al final (índice 66).
     */
    private static final String ALFABETO =
            " !\"#$%&a()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`b";

    /** Índice del último carácter <strong>comparable</strong> (el 66, {@code 'b'}, sólo se emite). */
    private static final int ULTIMO_ORIGEN_COMPARABLE = 65;

    /** Número de posiciones que recorre el legacy ({@code &Co} de 1 a 10). */
    private static final int LARGO_ENTRADA = 10;

    /** Suma de índices de la regla espejo: {@code destino = 67 − origen}. */
    private static final int FACTOR_ESPEJO = 67;

    @Override
    public String codificar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String normalizado = texto.toUpperCase(Locale.ROOT);
        StringBuilder salida = new StringBuilder(LARGO_ENTRADA);
        for (int posicion = 0; posicion < LARGO_ENTRADA && posicion < normalizado.length(); posicion++) {
            int origen = indiceDe(normalizado.charAt(posicion));
            if (origen < 1 || origen > ULTIMO_ORIGEN_COMPARABLE) {
                // Caracteres fuera del alfabeto: se descartan (la salida queda más corta).
                continue;
            }
            salida.append(ALFABETO.charAt(FACTOR_ESPEJO - origen - 1));
        }
        return salida.toString();
    }

    @Override
    public boolean coincideCon(String codificado, String almacenado) {
        if (codificado == null || almacenado == null) {
            return false;
        }
        return rtrim(codificado).equals(rtrim(almacenado));
    }

    @Override
    public String decodificar(String codificado) {
        if (codificado == null || codificado.isEmpty()) {
            return "";
        }
        // El relleno del char(10) de la base (espacios finales) NO es parte del texto codificado:
        // el alfabeto no produce espacios (la imagen del espacio es 'b'), de modo que un espacio
        // final es siempre relleno. Por eso se quita antes de decodificar: reproduce el dato
        // verificado de la spec (§3.2.1): 3 de los 552 UsuId decodifican a 9 caracteres
        // ("PASICHG  ", "GMAANEZ  ", "GNUEZ    ") porque el Ñ original se perdió.
        String sinRelleno = rtrim(codificado);
        StringBuilder salida = new StringBuilder(sinRelleno.length());
        for (int posicion = 0; posicion < sinRelleno.length(); posicion++) {
            int destino = indiceDe(sinRelleno.charAt(posicion));
            if (destino < 1) {
                // Fuera del alfabeto: se descarta.
                continue;
            }
            int origen = FACTOR_ESPEJO - destino;
            if (origen < 1 || origen > ALFABETO.length()) {
                continue;
            }
            salida.append(ALFABETO.charAt(origen - 1));
        }
        return salida.toString();
    }

    /** Índice (1-based) del carácter en el alfabeto; {@code 0} si no está. */
    private static int indiceDe(char caracter) {
        return ALFABETO.indexOf(caracter) + 1;
    }

    /** {@code RTRIM} de SQL Server: quita los espacios finales (y sólo ellos). */
    private static String rtrim(String texto) {
        int fin = texto.length();
        while (fin > 0 && texto.charAt(fin - 1) == ' ') {
            fin--;
        }
        return texto.substring(0, fin);
    }
}
