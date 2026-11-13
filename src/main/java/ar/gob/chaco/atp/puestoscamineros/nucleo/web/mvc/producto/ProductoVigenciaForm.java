package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form object de una fila del nivel 1 ({@code PRODUCT1}).
 *
 * <p>La fecha se maneja como {@link LocalDate} y se envía a medianoche (SA-09, R-PROD-23); los
 * importes respetan el tipo lógico {@code NUMERIC(12,2)}: 2 decimales y rango
 * {@code ±9999999999.99} (R-PROD-08).
 *
 * <p>Los mensajes de validación son los del plan §4.6 (presentación); los del DTO de escritura son
 * los del backend.
 */
public class ProductoVigenciaForm {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MENSAJE_DECIMALES = "Máximo 2 decimales.";

    private static final String MENSAJE_RANGO = "Fuera de rango.";

    /** Fecha de vigencia: obligatoria (clave del nivel 1). */
    @NotNull(message = "La fecha de vigencia es obligatoria.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate prodFchVig;

    /** Importe Fijo Entrada. */
    @NotNull(message = MENSAJE_RANGO)
    @Digits(integer = 10, fraction = 2, message = MENSAJE_DECIMALES)
    @DecimalMin(value = "-9999999999.99", message = MENSAJE_RANGO)
    @DecimalMax(value = "9999999999.99", message = MENSAJE_RANGO)
    private BigDecimal prodImpu1;

    /** Imp.Cosech.Mec Entrada. */
    @NotNull(message = MENSAJE_RANGO)
    @Digits(integer = 10, fraction = 2, message = MENSAJE_DECIMALES)
    @DecimalMin(value = "-9999999999.99", message = MENSAJE_RANGO)
    @DecimalMax(value = "9999999999.99", message = MENSAJE_RANGO)
    private BigDecimal prodImpu2;

    /** Importe Fijo Salida. */
    @NotNull(message = MENSAJE_RANGO)
    @Digits(integer = 10, fraction = 2, message = MENSAJE_DECIMALES)
    @DecimalMin(value = "-9999999999.99", message = MENSAJE_RANGO)
    @DecimalMax(value = "9999999999.99", message = MENSAJE_RANGO)
    private BigDecimal prodImpu3;

    /** Imp.Cosech.Mec Salida. */
    @NotNull(message = MENSAJE_RANGO)
    @Digits(integer = 10, fraction = 2, message = MENSAJE_DECIMALES)
    @DecimalMin(value = "-9999999999.99", message = MENSAJE_RANGO)
    @DecimalMax(value = "9999999999.99", message = MENSAJE_RANGO)
    private BigDecimal prodImpu4;

    /**
     * Texto de la fecha en el formato del legacy ({@code dd/MM/yyyy}); es un dato derivado de
     * {@link #getProdFchVig()} y no se envía (R-PROD-23).
     */
    public String getProdFchVigTexto() {
        return prodFchVig == null ? "" : prodFchVig.format(FORMATO_FECHA);
    }

    public LocalDate getProdFchVig() {
        return prodFchVig;
    }

    public void setProdFchVig(LocalDate prodFchVig) {
        this.prodFchVig = prodFchVig;
    }

    public BigDecimal getProdImpu1() {
        return prodImpu1;
    }

    public void setProdImpu1(BigDecimal prodImpu1) {
        this.prodImpu1 = prodImpu1;
    }

    public BigDecimal getProdImpu2() {
        return prodImpu2;
    }

    public void setProdImpu2(BigDecimal prodImpu2) {
        this.prodImpu2 = prodImpu2;
    }

    public BigDecimal getProdImpu3() {
        return prodImpu3;
    }

    public void setProdImpu3(BigDecimal prodImpu3) {
        this.prodImpu3 = prodImpu3;
    }

    public BigDecimal getProdImpu4() {
        return prodImpu4;
    }

    public void setProdImpu4(BigDecimal prodImpu4) {
        this.prodImpu4 = prodImpu4;
    }
}
