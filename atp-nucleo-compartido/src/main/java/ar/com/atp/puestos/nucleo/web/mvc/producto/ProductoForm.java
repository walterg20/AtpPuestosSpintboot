package ar.com.atp.puestos.nucleo.web.mvc.producto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Form object de la pantalla de Productos (nivel 0, 12 campos).
 *
 * <p>Es el objeto de binding del formulario Thymeleaf ({@code th:object}). Reproduce las
 * restricciones del DTO de escritura {@code ProductoEscrituraDto} (D-31) para dar el feedback
 * inmediato en el navegador, sin dejar de delegar la validación definitiva en el servidor.
 *
 * <p>Los 4 derivados ({@code prodIMax1..4}) y {@code uMedNombre} se renderizan pero <strong>no se
 * envían</strong>: el mapeo {@code form → ProductoEscrituraDto} es campo por campo (D-30), de modo
 * que quedan fuera por construcción (R-PROD-03, R-PROD-19).
 */
public class ProductoForm {

    /** Clave primaria del producto: rango {@code 0…32767} de la columna {@code smallint}. */
    @NotNull(message = "El código del producto es obligatorio.")
    @Min(value = 0, message = "El código debe estar entre 0 y 32767.")
    @Max(value = 32767, message = "El código debe estar entre 0 y 32767.")
    private Integer prodCod;

    /** Descripción del producto: {@code char(25)}. */
    @NotBlank(message = "El producto es obligatorio.")
    @Size(max = 25, message = "Máximo 25 caracteres.")
    private String prodDesc;

    /** Tipo de cálculo: opcional; si se deja vacío se envía ausente (R-PROD-29). */
    private Integer prodTipCal;

    /** Tipo de producción: opcional; si se deja vacío se envía ausente (R-PROD-29). */
    private Integer tpPriID;

    /** Derivado {@code Importe de Entrada}: sólo lectura, no se persiste ni se envía. */
    private BigDecimal prodIMax1;

    /** Derivado {@code Importe C. Mec. Entrada}: sólo lectura, no se persiste ni se envía. */
    private BigDecimal prodIMax2;

    /** Derivado {@code Importe Salida}: sólo lectura, no se persiste ni se envía. */
    private BigDecimal prodIMax3;

    /** Derivado {@code Importre C. Mec. Salida}: sólo lectura, no se persiste ni se envía. */
    private BigDecimal prodIMax4;

    /** Código de la unidad de medida: {@code char(3)}. */
    @Size(max = 3, message = "Máximo 3 caracteres.")
    private String uMedNroId;

    /**
     * Unidad de medida: <strong>sólo lectura</strong>. {@code PRODUCTO} no tiene la columna y no
     * existe la transacción {@code UMed} en la KB, así que no hay origen posible (R-PROD-19).
     */
    @Size(max = 40)
    private String uMedNombre;

    /** Acopiador: {@code char(1)}, opcional. */
    @Size(max = 1, message = "Máximo 1 carácter.")
    private String prodAcop;

    /** Prod Anulado: opcional; si se deja vacío se envía ausente (R-PROD-29). */
    private Integer prodAnula;

    /** Nivel 1 ({@code PRODUCT1}): nunca {@code null}; vacío significa "sin vigencias". */
    @Valid
    @NotNull
    private List<ProductoVigenciaForm> vigencias = new ArrayList<>();

    public Integer getProdCod() {
        return prodCod;
    }

    public void setProdCod(Integer prodCod) {
        this.prodCod = prodCod;
    }

    public String getProdDesc() {
        return prodDesc;
    }

    public void setProdDesc(String prodDesc) {
        this.prodDesc = prodDesc;
    }

    public Integer getProdTipCal() {
        return prodTipCal;
    }

    public void setProdTipCal(Integer prodTipCal) {
        this.prodTipCal = prodTipCal;
    }

    public Integer getTpPriID() {
        return tpPriID;
    }

    public void setTpPriID(Integer tpPriID) {
        this.tpPriID = tpPriID;
    }

    public BigDecimal getProdIMax1() {
        return prodIMax1;
    }

    public void setProdIMax1(BigDecimal prodIMax1) {
        this.prodIMax1 = prodIMax1;
    }

    public BigDecimal getProdIMax2() {
        return prodIMax2;
    }

    public void setProdIMax2(BigDecimal prodIMax2) {
        this.prodIMax2 = prodIMax2;
    }

    public BigDecimal getProdIMax3() {
        return prodIMax3;
    }

    public void setProdIMax3(BigDecimal prodIMax3) {
        this.prodIMax3 = prodIMax3;
    }

    public BigDecimal getProdIMax4() {
        return prodIMax4;
    }

    public void setProdIMax4(BigDecimal prodIMax4) {
        this.prodIMax4 = prodIMax4;
    }

    public String getUMedNroId() {
        return uMedNroId;
    }

    public void setUMedNroId(String uMedNroId) {
        this.uMedNroId = uMedNroId;
    }

    public String getUMedNombre() {
        return uMedNombre;
    }

    public void setUMedNombre(String uMedNombre) {
        this.uMedNombre = uMedNombre;
    }

    public String getProdAcop() {
        return prodAcop;
    }

    public void setProdAcop(String prodAcop) {
        this.prodAcop = prodAcop;
    }

    public Integer getProdAnula() {
        return prodAnula;
    }

    public void setProdAnula(Integer prodAnula) {
        this.prodAnula = prodAnula;
    }

    public List<ProductoVigenciaForm> getVigencias() {
        return vigencias;
    }

    public void setVigencias(List<ProductoVigenciaForm> vigencias) {
        this.vigencias = vigencias;
    }
}
