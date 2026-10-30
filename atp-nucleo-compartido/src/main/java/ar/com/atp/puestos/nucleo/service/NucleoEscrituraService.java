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
import ar.com.atp.puestos.nucleo.web.dto.AreaEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.CalleDto;
import ar.com.atp.puestos.nucleo.web.dto.CalleEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoDto;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionDto;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.ProvinciaDto;
import ar.com.atp.puestos.nucleo.web.dto.ProvinciaEscrituraDto;
import ar.com.atp.puestos.shared.error.ConflictException;
import ar.com.atp.puestos.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABM de las siete maestras de un solo nivel (R-030, R-031, R-032, R-036, R-037):
 * provincia, localidad, calle, camino, area, jurisdiccion y feriado.
 *
 * <p>El ABM de {@code Producto} (transacción de dos niveles, R-034/R-035) vive en
 * {@link ProductoService}. Las altas reciben la PK en el body (R-037) y responden {@code 409}
 * si ya existe (R-031); las modificaciones/bajas inexistentes responden {@code 404}. Las columnas
 * legacy {@code NOT NULL} sin {@code DEFAULT} se inicializan con {@code 0}/{@code ""} (R-036).
 */
@Service
@Transactional
public class NucleoEscrituraService {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;
    private final CalleRepository calleRepository;
    private final CaminoRepository caminoRepository;
    private final AreaRepository areaRepository;
    private final JurisdiccionRepository jurisdiccionRepository;
    private final FeriadoRepository feriadoRepository;

    /**
     * Usuario de auditoría de {@code Feriado} (R-033).
     *
     * <p><strong>T-130 / R-LOG-032</strong>: con sesión autenticada es el {@code Context.Usuario}
     * de la sesión; sin sesión, el valor por defecto de {@code app.auditoria.usuario}
     * ({@code SISTEMA}).
     */
    private final ResolvedorUsuarioAuditor resolvedorUsuarioAuditor;

    public NucleoEscrituraService(
            ProvinciaRepository provinciaRepository,
            LocalidadRepository localidadRepository,
            CalleRepository calleRepository,
            CaminoRepository caminoRepository,
            AreaRepository areaRepository,
            JurisdiccionRepository jurisdiccionRepository,
            FeriadoRepository feriadoRepository,
            ResolvedorUsuarioAuditor resolvedorUsuarioAuditor) {
        this.provinciaRepository = provinciaRepository;
        this.localidadRepository = localidadRepository;
        this.calleRepository = calleRepository;
        this.caminoRepository = caminoRepository;
        this.areaRepository = areaRepository;
        this.jurisdiccionRepository = jurisdiccionRepository;
        this.feriadoRepository = feriadoRepository;
        this.resolvedorUsuarioAuditor = resolvedorUsuarioAuditor;
    }

    // --- Provincia -----------------------------------------------------------

    public ProvinciaDto crearProvincia(ProvinciaEscrituraDto dto) {
        if (provinciaRepository.existsById(dto.provCod())) {
            throw duplicada("provincia", "ProvCod", dto.provCod());
        }
        Provincia provincia = new Provincia(dto.provCod(), dto.provNombre());
        return NucleoMapper.toDto(provinciaRepository.save(provincia));
    }

    public ProvinciaDto actualizarProvincia(Integer provCod, ProvinciaEscrituraDto dto) {
        Provincia provincia = buscarProvincia(provCod);
        provincia.setProvNombre(dto.provNombre());
        return NucleoMapper.toDto(provinciaRepository.save(provincia));
    }

    public void eliminarProvincia(Integer provCod) {
        provinciaRepository.delete(buscarProvincia(provCod));
    }

    private Provincia buscarProvincia(Integer provCod) {
        return provinciaRepository.findById(provCod)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Provincia " + provCod + " no encontrada."));
    }

    // --- Localidad -----------------------------------------------------------

    public LocalidadDto crearLocalidad(LocalidadEscrituraDto dto) {
        if (localidadRepository.existsById(dto.locCod())) {
            throw duplicada("localidad", "LocCod", dto.locCod());
        }
        Localidad localidad = new Localidad(dto.locCod(), dto.locNom());
        aplicarLocalidad(localidad, dto);
        return NucleoMapper.toDto(localidadRepository.save(localidad));
    }

    public LocalidadDto actualizarLocalidad(Integer locCod, LocalidadEscrituraDto dto) {
        Localidad localidad = buscarLocalidad(locCod);
        localidad.setLocNom(dto.locNom());
        aplicarLocalidad(localidad, dto);
        return NucleoMapper.toDto(localidadRepository.save(localidad));
    }

    public void eliminarLocalidad(Integer locCod) {
        localidadRepository.delete(buscarLocalidad(locCod));
    }

    private void aplicarLocalidad(Localidad localidad, LocalidadEscrituraDto dto) {
        localidad.setLocNomProv(dto.locNomProv());
        localidad.setLocCodPost(dto.locCodPost());
        localidad.setProvincia(resolverProvincia(dto.provCod()));
        // R-207: valor de la columna Localida.ProvCod que se está escribiendo. La columna la
        // persiste la asociación (el mapeo de la columna es de sólo lectura para Hibernate);
        // aquí se mantiene el DTO alineado con la FK que queda en la BD.
        localidad.setProvCod(dto.provCod());
    }

    private Localidad buscarLocalidad(Integer locCod) {
        return localidadRepository.findById(locCod)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Localidad " + locCod + " no encontrada."));
    }

    /** FK lógica {@code ProvCod} (R-003): la integridad la garantiza la aplicación. */
    private Provincia resolverProvincia(Integer provCod) {
        return provinciaRepository.findById(provCod)
                .orElseThrow(() -> new ConflictException("PROVINCIA_INEXISTENTE",
                        "La provincia " + provCod + " no existe."));
    }

    // --- Calle ---------------------------------------------------------------

    public CalleDto crearCalle(CalleEscrituraDto dto) {
        if (calleRepository.existsById(dto.callCod())) {
            throw duplicada("calle", "CallCod", dto.callCod());
        }
        Calle calle = new Calle(dto.callCod(), dto.callNom());
        return NucleoMapper.toDto(calleRepository.save(calle));
    }

    public CalleDto actualizarCalle(Integer callCod, CalleEscrituraDto dto) {
        Calle calle = buscarCalle(callCod);
        calle.setCallNom(dto.callNom());
        return NucleoMapper.toDto(calleRepository.save(calle));
    }

    public void eliminarCalle(Integer callCod) {
        calleRepository.delete(buscarCalle(callCod));
    }

    private Calle buscarCalle(Integer callCod) {
        return calleRepository.findById(callCod)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Calle " + callCod + " no encontrada."));
    }

    // --- Camino --------------------------------------------------------------

    public CaminoDto crearCamino(CaminoEscrituraDto dto) {
        if (caminoRepository.existsById(dto.camID())) {
            throw duplicada("camino", "CamID", dto.camID());
        }
        Camino camino = new Camino(dto.camID(), dto.canRuta());
        return NucleoMapper.toDto(caminoRepository.save(camino));
    }

    public CaminoDto actualizarCamino(String camID, CaminoEscrituraDto dto) {
        Camino camino = buscarCamino(camID);
        camino.setCanRuta(dto.canRuta());
        return NucleoMapper.toDto(caminoRepository.save(camino));
    }

    public void eliminarCamino(String camID) {
        caminoRepository.delete(buscarCamino(camID));
    }

    private Camino buscarCamino(String camID) {
        return caminoRepository.findById(camID)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Camino " + camID + " no encontrado."));
    }

    // --- Area ----------------------------------------------------------------

    public AreaDto crearArea(AreaEscrituraDto dto) {
        if (areaRepository.existsById(dto.areaId())) {
            throw duplicada("area", "AreaId", dto.areaId());
        }
        Area area = new Area(dto.areaId(), dto.areaDesc());
        return NucleoMapper.toDto(areaRepository.save(area));
    }

    public AreaDto actualizarArea(BigDecimal areaId, AreaEscrituraDto dto) {
        Area area = buscarArea(areaId);
        area.setAreaDesc(dto.areaDesc());
        return NucleoMapper.toDto(areaRepository.save(area));
    }

    public void eliminarArea(BigDecimal areaId) {
        areaRepository.delete(buscarArea(areaId));
    }

    private Area buscarArea(BigDecimal areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Area " + areaId + " no encontrada."));
    }

    // --- Jurisdiccion --------------------------------------------------------

    public JurisdiccionDto crearJurisdiccion(JurisdiccionEscrituraDto dto) {
        if (jurisdiccionRepository.existsById(dto.jurCod())) {
            throw duplicada("jurisdiccion", "JurCod", dto.jurCod());
        }
        Jurisdiccion jurisdiccion = new Jurisdiccion(dto.jurCod(), dto.jurNom());
        return NucleoMapper.toDto(jurisdiccionRepository.save(jurisdiccion));
    }

    public JurisdiccionDto actualizarJurisdiccion(Integer jurCod, JurisdiccionEscrituraDto dto) {
        Jurisdiccion jurisdiccion = buscarJurisdiccion(jurCod);
        jurisdiccion.setJurNom(dto.jurNom());
        return NucleoMapper.toDto(jurisdiccionRepository.save(jurisdiccion));
    }

    public void eliminarJurisdiccion(Integer jurCod) {
        jurisdiccionRepository.delete(buscarJurisdiccion(jurCod));
    }

    private Jurisdiccion buscarJurisdiccion(Integer jurCod) {
        return jurisdiccionRepository.findById(jurCod)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Jurisdiccion " + jurCod + " no encontrada."));
    }

    // --- Feriado -------------------------------------------------------------

    public FeriadoDto crearFeriado(FeriadoEscrituraDto dto) {
        if (feriadoRepository.existsById(dto.ferFecha())) {
            throw duplicada("feriado", "FerFecha", dto.ferFecha());
        }
        Feriado feriado = new Feriado(dto.ferFecha(), dto.ferDescripcion());
        aplicarFeriado(feriado, dto);
        return NucleoMapper.toDto(feriadoRepository.save(feriado));
    }

    public FeriadoDto actualizarFeriado(LocalDateTime ferFecha, FeriadoEscrituraDto dto) {
        Feriado feriado = buscarFeriado(ferFecha);
        feriado.setFerDescripcion(dto.ferDescripcion());
        aplicarFeriado(feriado, dto);
        return NucleoMapper.toDto(feriadoRepository.save(feriado));
    }

    public void eliminarFeriado(LocalDateTime ferFecha) {
        feriadoRepository.delete(buscarFeriado(ferFecha));
    }

    /** Auditoría en servidor (R-033): {@code FerFchReg} y {@code FerUsuario}. */
    private void aplicarFeriado(Feriado feriado, FeriadoEscrituraDto dto) {
        feriado.setFerTipo(dto.ferTipo());
        feriado.setFerFchReg(LocalDateTime.now());
        feriado.setFerUsuario(resolvedorUsuarioAuditor.usuarioActual());
    }

    private Feriado buscarFeriado(LocalDateTime ferFecha) {
        return feriadoRepository.findById(ferFecha)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Feriado " + ferFecha + " no encontrado."));
    }

    // --- Auxiliares ----------------------------------------------------------

    private static ConflictException duplicada(String entidad, String pk, Object valor) {
        return new ConflictException("PK_DUPLICADA",
                "Ya existe un registro de " + entidad + " con " + pk + " " + valor + ".");
    }
}
