package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.nucleo.domain.Comision;
import ar.com.atp.puestos.nucleo.domain.ComisionIntegrante;
import ar.com.atp.puestos.nucleo.domain.Persona;
import ar.com.atp.puestos.nucleo.repository.ComisionIntegranteRepository;
import ar.com.atp.puestos.nucleo.repository.ComisionRepository;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionFilaView;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionGeneralView;
import ar.com.atp.puestos.puestos.service.dto.comision.IntegranteView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de comisiones del módulo {@code 13-comisiones} (spec §3, plan §2).
 *
 * <p>Reglas de negocio: filtro legacy (D-COM-001), orden {@code CPCId desc} (R-COM-002),
 * paginación de {@link #TAMANIO_PAGINA} filas (R-COM-003), decodificación de Estado
 * (R-COM-006) y resolución de Responsable/Integrantes por JOIN a {@code PERSONAS}.
 */
@Service
@Transactional(readOnly = true)
public class ServicioComisiones {

    /** Filas por página de la grilla (paridad con {@code MaxRows="10"} del legacy, R-COM-003). */
    public static final int TAMANIO_PAGINA = 10;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final ComisionRepository comisionRepository;
    private final ComisionIntegranteRepository comisionIntegranteRepository;

    public ServicioComisiones(ComisionRepository comisionRepository,
            ComisionIntegranteRepository comisionIntegranteRepository) {
        this.comisionRepository = comisionRepository;
        this.comisionIntegranteRepository = comisionIntegranteRepository;
    }

    /**
     * Listado paginado de comisiones con el filtro legacy (R-COM-001, R-COM-002, R-COM-003).
     *
     * @param pueCod      puesto caminerero ({@code Context.Sucursal}); si es null se omite
     * @param personaId   persona responsable ({@code Context.PersonaId}); si es null o 0 se omite
     * @param pagina      página 0-based
     */
    public Page<ComisionFilaView> listar(Integer pueCod, Integer personaId, int pagina) {
        Integer cpcResId = (personaId != null && personaId != 0) ? personaId : null;
        Pageable pageable = PageRequest.of(pagina, TAMANIO_PAGINA,
                Sort.by("id.cpcId").descending());
        Page<Object[]> paginaCruda = comisionRepository.findComisionesConResponsable(pueCod, cpcResId,
                pageable);
        return paginaCruda.map(fila -> {
            Comision c = (Comision) fila[0];
            Persona p = (Persona) fila[1];
            return new ComisionFilaView(
                    c.getPueCod(),
                    c.getCpcId(),
                    formatearFecha(c.getCpcFchDes()),
                    formatearHora(c.getCpcHorDes()),
                    formatearFecha(c.getCpcFchHas()),
                    formatearHora(c.getCpcHorHas()),
                    DecodificadorEstadoComision.decodificar(c.getCpcCierre()),
                    nombrePersona(p));
        });
    }

    /**
     * Detalle de una comisión por ({@code pueCod}, {@code cpcId}) o {@code null} si no existe
     * (R-COM-009, R-COM-013).
     */
    public ComisionGeneralView obtenerDetalle(Integer pueCod, Integer cpcId) {
        List<Object[]> encontrados = comisionRepository.findComisionConResponsable(pueCod, cpcId);
        if (encontrados == null || encontrados.isEmpty()) {
            return null;
        }
        Object[] fila = encontrados.get(0);
        Comision c = (Comision) fila[0];
        Persona p = (Persona) fila[1];

        String pueDesc = comisionRepository.findPueDescByPueCod(pueCod)
                .map(String::trim)
                .orElse("");

        return new ComisionGeneralView(
                c.getPueCod(),
                pueDesc,
                c.getCpcId(),
                formatearFecha(c.getCpcFchDes()),
                formatearHora(c.getCpcHorDes()),
                formatearFecha(c.getCpcFchHas()),
                formatearHora(c.getCpcHorHas()),
                DecodificadorEstadoComision.decodificar(c.getCpcCierre()),
                c.getCpcResId(),
                nombrePersona(p));
    }

    /**
     * Integrantes de una comisión ordenados por {@code PerNomyApe} (trim) ascendente
     * (R-COM-014, R-COM-015, SA-COM-06).
     */
    public List<IntegranteView> listarIntegrantes(Integer pueCod, Integer cpcId) {
        List<Object[]> filas = comisionIntegranteRepository.findIntegrantesConNombre(pueCod, cpcId);
        List<IntegranteView> resultado = new ArrayList<>();
        if (filas == null) {
            return resultado;
        }
        for (Object[] fila : filas) {
            ComisionIntegrante i = (ComisionIntegrante) fila[0];
            Persona p = (Persona) fila[1];
            resultado.add(new IntegranteView(i.getCpcPerId(), nombrePersona(p)));
        }
        return resultado;
    }

    private static String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    private static String formatearHora(Integer hora) {
        if (hora == null) {
            return "";
        }
        return String.format("%04d", hora);
    }

    private static String nombrePersona(Persona p) {
        if (p == null || p.getPerNomyApe() == null) {
            return "";
        }
        return p.getPerNomyApe().trim();
    }
}