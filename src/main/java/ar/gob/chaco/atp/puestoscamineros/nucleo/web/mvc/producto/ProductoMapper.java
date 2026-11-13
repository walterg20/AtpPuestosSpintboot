package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto;

import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ImportesVigentesDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Traducción entre el formulario de la pantalla y los DTO del backend.
 *
 * <p>El mapeo hacia el DTO de escritura es <strong>campo por campo</strong> (D-30): sólo se
 * asignan los atributos que forman parte del contrato de escritura. Por construcción,
 * {@code prodIMax1..4} (derivados que no se persisten) y {@code uMedNombre} (sin columna física)
 * quedan fuera del cuerpo (R-PROD-03, R-PROD-19).
 *
 * <p>Reglas aplicadas (R-PROD-20, R-PROD-23, R-PROD-28, R-PROD-29):
 * <ul>
 *   <li>los textos se recortan (las columnas son {@code char(n)} de ancho fijo);</li>
 *   <li>los enteros opcionales vacíos se envían ausentes ({@code null}), nunca {@code 0};</li>
 *   <li>la fecha de vigencia se convierte a medianoche;</li>
 *   <li>los importes ausentes se normalizan a {@code 0};</li>
 *   <li>la lista de vigencias nunca es {@code null}.</li>
 * </ul>
 */
public final class ProductoMapper {

    private ProductoMapper() {
    }

    /** Construye el formulario de la pantalla a partir de los DTO de lectura. */
    public static ProductoForm aForm(ProductoDto dto, ImportesVigentesDto importes) {
        ProductoForm form = new ProductoForm();
        if (dto == null) {
            return form;
        }
        form.setProdCod(dto.prodCod());
        form.setProdDesc(dto.prodDesc());
        form.setProdTipCal(dto.prodTipCal());
        form.setTpPriID(dto.tpPriID());
        form.setProdIMax1(importes == null ? BigDecimal.ZERO : ceroSiNulo(importes.prodIMax1()));
        form.setProdIMax2(importes == null ? BigDecimal.ZERO : ceroSiNulo(importes.prodIMax2()));
        form.setProdIMax3(importes == null ? BigDecimal.ZERO : ceroSiNulo(importes.prodIMax3()));
        form.setProdIMax4(importes == null ? BigDecimal.ZERO : ceroSiNulo(importes.prodIMax4()));
        form.setUMedNroId(dto.uMedNroId());
        form.setUMedNombre(dto.uMedNombre());
        form.setProdAcop(dto.prodAcop());
        form.setProdAnula(dto.prodAnula());
        form.setVigencias(aFormVigencias(dto.vigencias()));
        return form;
    }

    /** Traduce las vigencias de lectura al formulario, normalizando a {@code 0} los importes nulos. */
    public static List<ProductoVigenciaForm> aFormVigencias(List<ProductoVigenciaDto> vigencias) {
        List<ProductoVigenciaForm> form = new ArrayList<>();
        if (vigencias == null) {
            return form;
        }
        vigencias.forEach(vigencia -> form.add(aFormVigencia(vigencia)));
        return form;
    }

    /** Construye el DTO de escritura desde el formulario (mapeo campo por campo, D-30). */
    public static ProductoEscrituraDto aEscritura(ProductoForm form) {
        return new ProductoEscrituraDto(
                form.getProdCod(),
                recortar(form.getProdDesc()),
                form.getProdTipCal(),
                form.getTpPriID(),
                recortar(form.getUMedNroId()),
                null,
                recortar(form.getProdAcop()),
                form.getProdAnula(),
                aEscrituraVigencias(form.getVigencias()));
    }

    /** Traduce las vigencias del formulario al DTO anidado de escritura. */
    public static List<ProductoVigenciaEscrituraDto> aEscrituraVigencias(
            List<ProductoVigenciaForm> vigencias) {
        if (vigencias == null) {
            return List.of();
        }
        return vigencias.stream()
                .map(ProductoMapper::aEscrituraVigencia)
                .toList();
    }

    private static ProductoVigenciaForm aFormVigencia(ProductoVigenciaDto dto) {
        ProductoVigenciaForm form = new ProductoVigenciaForm();
        form.setProdFchVig(dto.prodFchVig() == null ? null : dto.prodFchVig().toLocalDate());
        form.setProdImpu1(ceroSiNulo(dto.prodImpu1()));
        form.setProdImpu2(ceroSiNulo(dto.prodImpu2()));
        form.setProdImpu3(ceroSiNulo(dto.prodImpu3()));
        form.setProdImpu4(ceroSiNulo(dto.prodImpu4()));
        return form;
    }

    private static ProductoVigenciaEscrituraDto aEscrituraVigencia(ProductoVigenciaForm form) {
        return new ProductoVigenciaEscrituraDto(
                form.getProdFchVig() == null ? null : form.getProdFchVig().atStartOfDay(),
                ceroSiNulo(form.getProdImpu1()),
                ceroSiNulo(form.getProdImpu2()),
                ceroSiNulo(form.getProdImpu3()),
                ceroSiNulo(form.getProdImpu4()));
    }

    private static BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
