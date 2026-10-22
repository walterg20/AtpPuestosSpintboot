package ar.com.atp.puestos.nucleo.service;

import ar.com.atp.puestos.nucleo.repository.CaminoRepository;
import ar.com.atp.puestos.nucleo.repository.LocalidadRepository;
import ar.com.atp.puestos.nucleo.web.dto.CaminoRutaDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadNombreDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procedimientos de búsqueda legacy expuestos como recursos dedicados (R-010, R-011, R-022):
 * {@code Bus_DesLoc} y {@code BuscarCamino}. Nunca lanzan 404: devuelven resultado vacío si no existe.
 */
@Service
@Transactional(readOnly = true)
public class BusquedaService {

    private final LocalidadRepository localidadRepository;
    private final CaminoRepository caminoRepository;

    public BusquedaService(LocalidadRepository localidadRepository, CaminoRepository caminoRepository) {
        this.localidadRepository = localidadRepository;
        this.caminoRepository = caminoRepository;
    }

    /**
     * Equivalente a {@code Bus_DesLoc} (R-010): devuelve {@code LocNom} o cadena vacía si no existe.
     */
    public LocalidadNombreDto nombreLocalidad(Integer locCod) {
        String locNom = localidadRepository.findById(locCod)
                .map(l -> l.getLocNom())
                .orElse("");
        return new LocalidadNombreDto(locNom != null ? locNom : "");
    }

    /**
     * Equivalente a {@code BuscarCamino} (R-011): devuelve {@code Trim(CanRuta)} o cadena vacía si no
     * existe. El recorte del {@code char(n)} ya lo aplica el mapeo ({@code RTRIM}); se refuerza con
     * {@link String#trim()} por defensa.
     */
    public CaminoRutaDto rutaCamino(String camID) {
        String canRuta = caminoRepository.findById(camID)
                .map(c -> c.getCanRuta())
                .orElse("");
        return new CaminoRutaDto(canRuta != null ? canRuta.trim() : "");
    }
}
