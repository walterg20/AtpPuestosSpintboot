package ar.com.atp.puestos.nucleo.web.dto;

/** DTO de lectura de {@code Localidad}, con la provincia asociada resuelta (plan §3.3, R-003). */
public record LocalidadDto(
        Integer locCod,
        String locNom,
        Integer locCodPost,
        Integer provCod,
        String provNombre) {
}
