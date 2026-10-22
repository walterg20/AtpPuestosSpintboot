package ar.com.atp.puestos.nucleo.service;

import ar.com.atp.puestos.nucleo.domain.Area;
import ar.com.atp.puestos.nucleo.domain.Calle;
import ar.com.atp.puestos.nucleo.domain.Camino;
import ar.com.atp.puestos.nucleo.domain.Feriado;
import ar.com.atp.puestos.nucleo.domain.Jurisdiccion;
import ar.com.atp.puestos.nucleo.domain.Localidad;
import ar.com.atp.puestos.nucleo.domain.Provincia;
import ar.com.atp.puestos.nucleo.repository.AreaRepository;
import ar.com.atp.puestos.nucleo.repository.CalleRepository;
import ar.com.atp.puestos.nucleo.repository.CaminoRepository;
import ar.com.atp.puestos.nucleo.repository.FeriadoRepository;
import ar.com.atp.puestos.nucleo.repository.JurisdiccionRepository;
import ar.com.atp.puestos.nucleo.repository.LocalidadRepository;
import ar.com.atp.puestos.nucleo.repository.ProvinciaRepository;
import ar.com.atp.puestos.nucleo.web.dto.AreaDto;
import ar.com.atp.puestos.nucleo.web.dto.CalleDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoDto;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoDto;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadDto;
import ar.com.atp.puestos.nucleo.web.dto.ProvinciaDto;
import ar.com.atp.puestos.shared.error.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de catálogos maestros (R-020, R-021). Devuelve DTOs de lectura (plan §3.1).
 */
@Service
@Transactional(readOnly = true)
public class NucleoConsultaService {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;
    private final CalleRepository calleRepository;
    private final CaminoRepository caminoRepository;
    private final AreaRepository areaRepository;
    private final JurisdiccionRepository jurisdiccionRepository;
    private final FeriadoRepository feriadoRepository;

    public NucleoConsultaService(
            ProvinciaRepository provinciaRepository,
            LocalidadRepository localidadRepository,
            CalleRepository calleRepository,
            CaminoRepository caminoRepository,
            AreaRepository areaRepository,
            JurisdiccionRepository jurisdiccionRepository,
            FeriadoRepository feriadoRepository) {
        this.provinciaRepository = provinciaRepository;
        this.localidadRepository = localidadRepository;
        this.calleRepository = calleRepository;
        this.caminoRepository = caminoRepository;
        this.areaRepository = areaRepository;
        this.jurisdiccionRepository = jurisdiccionRepository;
        this.feriadoRepository = feriadoRepository;
    }

    public List<ProvinciaDto> listarProvincias() {
        return provinciaRepository.findAll(Sort.by(Sort.Direction.ASC, "provNombre")).stream()
                .map(NucleoMapper::toDto)
                .toList();
    }

    public ProvinciaDto obtenerProvincia(Integer provCod) {
        return provinciaRepository.findById(provCod)
                .map(NucleoMapper::toDto)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Provincia " + provCod + " no encontrada."));
    }

    /**
     * Listado de localidades, con o sin filtro por provincia (R-020, R-021, R-024, R-025).
     *
     * <p>La provincia se resuelve en la misma consulta que trae las localidades: evita el patrón N+1
     * y, sobre todo, tolera la FK lógica huérfana ({@code ProvCod = 0} inexistente en
     * {@code PROVINCI}) exponiéndola en {@code null} en lugar de terminar en {@code 500}.
     */
    public List<LocalidadDto> listarLocalidades(Integer provCod) {
        List<Localidad> localidades = (provCod != null)
                ? localidadRepository.findByProvCodConProvincia(provCod)
                : localidadRepository.findAllConProvincia();
        return localidades.stream().map(NucleoMapper::toDto).toList();
    }

    public LocalidadDto obtenerLocalidad(Integer locCod) {
        return localidadRepository.findByIdConProvincia(locCod)
                .map(NucleoMapper::toDto)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Localidad " + locCod + " no encontrada."));
    }

    public List<CalleDto> listarCalles(String q) {
        List<Calle> calles = (q != null && !q.isBlank())
                ? calleRepository.findByCallNomContainingIgnoreCaseOrderByCallNomAsc(q.trim())
                : calleRepository.findAll(Sort.by(Sort.Direction.ASC, "callNom"));
        return calles.stream().map(NucleoMapper::toDto).toList();
    }

    public CalleDto obtenerCalle(Integer callCod) {
        return calleRepository.findById(callCod)
                .map(NucleoMapper::toDto)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Calle " + callCod + " no encontrada."));
    }

    public List<CaminoDto> listarCaminos() {
        return caminoRepository.findAll(Sort.by(Sort.Direction.ASC, "camID")).stream()
                .map(NucleoMapper::toDto)
                .toList();
    }

    public CaminoDto obtenerCamino(String camID) {
        return caminoRepository.findById(camID)
                .map(NucleoMapper::toDto)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Camino " + camID + " no encontrado."));
    }

    public List<AreaDto> listarAreas() {
        return areaRepository.findAll(Sort.by(Sort.Direction.ASC, "areaId")).stream()
                .map(NucleoMapper::toDto)
                .toList();
    }

    public List<JurisdiccionDto> listarJurisdicciones() {
        return jurisdiccionRepository.findAll(Sort.by(Sort.Direction.ASC, "jurNom")).stream()
                .map(NucleoMapper::toDto)
                .toList();
    }

    public List<FeriadoDto> listarFeriados(LocalDateTime desde, LocalDateTime hasta) {
        return feriadoRepository.findByFerFechaBetweenOrderByFerFechaAsc(desde, hasta).stream()
                .map(NucleoMapper::toDto)
                .toList();
    }
}
