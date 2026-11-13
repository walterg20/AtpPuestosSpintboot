package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T-105 — Paridad de restricciones entre los form objects de la pantalla y los DTO de escritura
 * (D-31).
 *
 * <p>El DTO es la fuente de verdad del servidor; el form object replica sus restricciones para
 * dar el feedback inmediato en el navegador. Este test impide que se desincronicen: por cada
 * atributo, <strong>toda</strong> restricción declarada en el DTO debe estar declarada en el form
 * object con <strong>el mismo límite</strong>.
 *
 * <p>Sentido de la comparación: el formulario puede ser <em>más estricto</em> (por ejemplo mientras
 * el refuerzo de validación del backend aún no está publicado), pero nunca más laxo: si el
 * servidor acepta un valor, la pantalla no puede rechazarlo antes de llegar al servidor.
 */
class ProductoFormParidadRestriccionesTest {

    @Test
    @DisplayName("El form del producto no es más laxo que ProductoEscrituraDto")
    void formProductoNoEsMasLaxoQueElDto() {
        assertParidad(ProductoForm.class, ProductoEscrituraDto.class,
                "prodCod", "prodDesc", "prodTipCal", "tpPriID", "uMedNroId", "uMedNombre",
                "prodAcop", "prodAnula", "vigencias");
    }

    @Test
    @DisplayName("El form de la vigencia no es más laxo que ProductoVigenciaEscrituraDto")
    void formVigenciaNoEsMasLaxoQueElDto() {
        assertParidad(ProductoVigenciaForm.class, ProductoVigenciaEscrituraDto.class,
                "prodFchVig", "prodImpu1", "prodImpu2", "prodImpu3", "prodImpu4");
    }

    @Test
    @DisplayName("El form del producto declara las restricciones del plan §4.3")
    void formProductoDeclaraLasRestriccionesDelPlan() {
        Set<String> prodCod = restriccionesDeCampo(ProductoForm.class, "prodCod");
        assertThat(prodCod).contains("notnull", "min:0", "max:32767");

        Set<String> prodDesc = restriccionesDeCampo(ProductoForm.class, "prodDesc");
        assertThat(prodDesc).contains("notblank", "size:0:25");

        Set<String> uMedNroId = restriccionesDeCampo(ProductoForm.class, "uMedNroId");
        assertThat(uMedNroId).contains("size:0:3");

        Set<String> prodAcop = restriccionesDeCampo(ProductoForm.class, "prodAcop");
        assertThat(prodAcop).contains("size:0:1");

        Set<String> vigencias = restriccionesDeCampo(ProductoForm.class, "vigencias");
        assertThat(vigencias).contains("notnull");
    }

    @Test
    @DisplayName("El form de la vigencia declara las restricciones del plan §4.3")
    void formVigenciaDeclaraLasRestriccionesDelPlan() {
        assertThat(restriccionesDeCampo(ProductoVigenciaForm.class, "prodFchVig"))
                .contains("notnull");

        for (String campo : new String[] {"prodImpu1", "prodImpu2", "prodImpu3", "prodImpu4"}) {
            assertThat(restriccionesDeCampo(ProductoVigenciaForm.class, campo))
                    .as(campo)
                    .contains("notnull", "digits:10:2", "decimalmin:-9999999999.99:true",
                            "decimalmax:9999999999.99:true");
        }
    }

    @Test
    @DisplayName("Los 4 derivados (ProdIMax1..4) no declaran validación: son de sólo lectura")
    void losDerivadosNoDeclaranValidacion() {
        for (String campo : new String[] {"prodIMax1", "prodIMax2", "prodIMax3", "prodIMax4"}) {
            assertThat(restriccionesDeCampo(ProductoForm.class, campo))
                    .as(campo)
                    .isEmpty();
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Compara, atributo por atributo, las restricciones del DTO con las del form object. */
    private static void assertParidad(Class<?> form, Class<?> dto, String... atributos) {
        for (String atributo : atributos) {
            Set<String> delDto = restriccionesDeComponente(dto, atributo);
            Set<String> delForm = restriccionesDeCampo(form, atributo);
            assertThat(delForm)
                    .as("restricciones del form object %s.%s frente a %s.%s",
                            form.getSimpleName(), atributo, dto.getSimpleName(), atributo)
                    .containsAll(delDto);
        }
    }

    /** Restricciones declaradas en un campo del form object (campo o getter). */
    private static Set<String> restriccionesDeCampo(Class<?> tipo, String atributo) {
        Set<String> resultado = new TreeSet<>();
        Field campo = campo(tipo, atributo);
        resultado.addAll(restricciones(campo.getAnnotations()));
        Method getter = getter(tipo, atributo);
        if (getter != null) {
            resultado.addAll(restricciones(getter.getAnnotations()));
        }
        return resultado;
    }

    /** Restricciones declaradas en un componente de record del DTO. */
    private static Set<String> restriccionesDeComponente(Class<?> tipo, String atributo) {
        Set<String> resultado = new TreeSet<>();
        for (RecordComponent componente : tipo.getRecordComponents()) {
            if (componente.getName().equals(atributo)) {
                resultado.addAll(restricciones(componente.getAccessor().getAnnotations()));
                Field campo = campo(tipo, atributo);
                if (campo != null) {
                    resultado.addAll(restricciones(campo.getAnnotations()));
                }
                return resultado;
            }
        }
        throw new AssertionError("El DTO " + tipo.getSimpleName() + " no declara el atributo "
                + atributo);
    }

    private static Field campo(Class<?> tipo, String atributo) {
        try {
            return tipo.getDeclaredField(atributo);
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("La clase " + tipo.getSimpleName() + " no declara el campo "
                    + atributo, ex);
        }
    }

    private static Method getter(Class<?> tipo, String atributo) {
        String capitalized = atributo.substring(0, 1).toUpperCase() + atributo.substring(1);
        for (String prefijo : new String[] {"get", "is"}) {
            try {
                return tipo.getDeclaredMethod(prefijo + capitalized);
            } catch (NoSuchMethodException ex) {
                // se sigue con el otro prefijo
            }
        }
        return null;
    }

    /** Normaliza las restricciones de Bean Validation a valores comparables. */
    private static Set<String> restricciones(Annotation[] anotaciones) {
        Set<String> resultado = new LinkedHashSet<>();
        for (Annotation anotacion : anotaciones) {
            String clave = clave(anotacion);
            if (clave != null) {
                resultado.add(clave);
            }
        }
        return new TreeSet<>(resultado);
    }

    private static String clave(Annotation anotacion) {
        String tipo = anotacion.annotationType().getSimpleName();
        return switch (tipo) {
            case "NotNull" -> "notnull";
            case "NotBlank" -> "notblank";
            case "NotEmpty" -> "notempty";
            case "Size" -> "size:" + ((Size) anotacion).min() + ":" + ((Size) anotacion).max();
            case "Min" -> "min:" + ((Min) anotacion).value();
            case "Max" -> "max:" + ((Max) anotacion).value();
            case "Digits" -> "digits:" + ((Digits) anotacion).integer() + ":"
                    + ((Digits) anotacion).fraction();
            case "DecimalMin" -> "decimalmin:" + decimal(((DecimalMin) anotacion).value()) + ":"
                    + ((DecimalMin) anotacion).inclusive();
            case "DecimalMax" -> "decimalmax:" + decimal(((DecimalMax) anotacion).value()) + ":"
                    + ((DecimalMax) anotacion).inclusive();
            case "Positive" -> "positive";
            case "PositiveOrZero" -> "positiveorzero";
            case "Pattern" -> "pattern:" + ((Pattern) anotacion).regexp();
            // @Valid es sólo la cascada: no restringe valores por sí mismo.
            case "Valid" -> null;
            default -> null;
        };
    }

    private static String decimal(String valor) {
        if (valor == null) {
            return "";
        }
        return new BigDecimal(valor).stripTrailingZeros().toPlainString();
    }
}
