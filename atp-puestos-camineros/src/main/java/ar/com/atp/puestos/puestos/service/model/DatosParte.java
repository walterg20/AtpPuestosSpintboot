package ar.com.atp.puestos.puestos.service.model;

/**
 * Datos de una parte (vendedor / destinatario / transportista) derivados del CUIT
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, R-ACTA-017, R-ACTA-018).
 *
 * <p>Reproduce {@code F2276_DevRazSocial} (razón social), {@code F2276_BusDom} (domicilio =
 * {@code CallNom + ' ' + manzana}) y {@code F2276_BusLoc} ({@code LocCodR}). La provincia
 * <strong>no se persiste</strong>: no es columna (D-ACTA-17).
 *
 * @param razonSocial razón social del contribuyente (vacía si no existe)
 * @param domicilio domicilio del contribuyente (vacío si no existe)
 * @param codigoLocalidad código de localidad ({@code LocCodR}) o {@code null}
 * @param provincia nombre de la provincia o cadena vacía
 */
public record DatosParte(String razonSocial, String domicilio, Integer codigoLocalidad,
        String provincia) {

    /** Datos vacíos: el contribuyente no existe (paridad del {@code when none} del legacy). */
    public static DatosParte vacio() {
        return new DatosParte("", "", null, "");
    }
}
