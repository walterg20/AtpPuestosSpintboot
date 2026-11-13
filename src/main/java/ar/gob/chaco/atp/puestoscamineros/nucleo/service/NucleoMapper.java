package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Area;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Calle;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Camino;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Feriado;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Jurisdiccion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.AreaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.CalleDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.CaminoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.FeriadoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.JurisdiccionDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProvinciaDto;
import java.util.List;
import org.hibernate.Hibernate;

/** Conversión entidad → DTO de lectura (plan §3.3). Métodos estáticos, sin estado. */
final class NucleoMapper {

    private NucleoMapper() {
    }

    static ProvinciaDto toDto(Provincia p) {
        return new ProvinciaDto(p.getProvCod(), p.getProvNombre());
    }

    /**
     * Conversión de {@link Localidad} a DTO de lectura.
     *
     * <p>Null-safe ante FK lógica huérfana (R-024): {@code Localida} tiene 30 filas con
     * {@code ProvCod = 0}, valor inexistente en {@code PROVINCI} (spec §4.3). Para esas filas la
     * provincia no se puede resolver y el DTO expone {@code provNombre} en {@code null}, sin
     * propagar la excepción que antes terminaba en {@code 500 ERROR_INTERNO}.
     *
     * <p><strong>R-207</strong>: {@code provCod} expone el <em>valor físico</em> de la columna
     * {@code Localida.ProvCod} (leído por {@link Localidad#getProvCod()}), no el de la asociación
     * resuelta. Para las 30 filas huérfanas ese valor es {@code 0}: el cliente puede así
     * representar y conservar el estado huérfano, en lugar de recibir un campo ausente
     * ({@code null} + {@code non_null} ⇒ la clave se omite del JSON).
     *
     * <p>La provincia se resuelve en la misma consulta que trae las localidades (R-025, ver
     * {@code LocalidadRepository#findAllConProvincia}), por lo que la asociación llega
     * pre-inicializada. Aun así se verifica {@link Hibernate#isInitialized(Object)}: leer el nombre
     * de un proxy no inicializado dispararía una inicialización diferida que, para la FK huérfana,
     * lanza {@code EntityNotFoundException}. Con la guarda, cualquier camino de código que mapee
     * entidades cargadas sin {@code join fetch} degrada a provincia {@code null} en vez de fallar.
     */
    static LocalidadDto toDto(Localidad l) {
        Provincia prov = l.getProvincia();
        String provNombre = null;
        // R-207: código FÍSICO de la columna Localida.ProvCod (0 en las 30 filas huérfanas).
        Integer provCod = l.getProvCod();
        if (prov != null && Hibernate.isInitialized(prov)) {
            provNombre = prov.getProvNombre();
            if (provCod == null) {
                // Entidad no refrescada desde la BD (recién creada o modificada): el código
                // sale de la asociación resuelta, que es la que escribe la columna.
                provCod = prov.getProvCod();
            }
        }
        return new LocalidadDto(
                l.getLocCod(),
                l.getLocNom(),
                l.getLocCodPost(),
                provCod,
                provNombre);
    }

    static CalleDto toDto(Calle c) {
        return new CalleDto(c.getCallCod(), c.getCallNom());
    }

    static CaminoDto toDto(Camino c) {
        return new CaminoDto(c.getCamID(), c.getCanRuta());
    }

    static AreaDto toDto(Area a) {
        return new AreaDto(a.getAreaId(), a.getAreaDesc());
    }

    static JurisdiccionDto toDto(Jurisdiccion j) {
        return new JurisdiccionDto(j.getJurCod(), j.getJurNom());
    }

    static FeriadoDto toDto(Feriado f) {
        return new FeriadoDto(
                f.getFerFecha(),
                f.getFerDescripcion(),
                f.getFerTipo(),
                f.getFerFchReg(),
                f.getFerUsuario());
    }

    /**
     * Conversion de una vigencia al DTO de lectura.
     *
     * <p><strong>T-111 / D-38</strong>: los 4 importes se normalizan al tipo logico
     * {@code NUMERIC(12,2)} (escala 2) porque la columna fisica es {@code money} (escala 4) y el
     * driver entrega {@code 12.0000}, que el {@code @Digits(integer = 10, fraction = 2)} del DTO de
     * escritura rechaza. Un importe ausente ({@code NULL}) <strong>sigue ausente</strong>: la
     * normalizacion a {@code 0} es de la vista (R-PROD-20).
     */
    static ProductoVigenciaDto toDto(ProductoVigencia v) {
        return new ProductoVigenciaDto(
                v.getProdFchVig(),
                ImporteNormalizador.aEscalaLogica(v.getProdImpu1()),
                ImporteNormalizador.aEscalaLogica(v.getProdImpu2()),
                ImporteNormalizador.aEscalaLogica(v.getProdImpu3()),
                ImporteNormalizador.aEscalaLogica(v.getProdImpu4()));
    }

    static ProductoDto toDto(Producto p) {
        List<ProductoVigenciaDto> vigencias = p.getVigencias().stream()
                .map(NucleoMapper::toDto)
                .toList();
        return new ProductoDto(
                p.getProdCod(),
                p.getProdDesc(),
                p.getProdTipCal(),
                p.getTpPriID(),
                p.getUMedNroId(),
                p.getUMedNombre(),
                p.getProdAcop(),
                p.getProdAnula(),
                vigencias);
    }
}
