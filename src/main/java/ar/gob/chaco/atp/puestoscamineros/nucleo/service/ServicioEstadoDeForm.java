package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.Formulario2276Repositorio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detección de formularios abiertos del turno (spec {@code 06-turnos} R-TUR-013, T-TUR-B3).
 *
 * <p>Reproduce el procedimiento legacy {@code PPrim_EstadoDeForm}: el turno tiene formularios
 * abiertos si existe un {@code F2276} del turno cuyo lote ({@code LOTES227}, vínculo
 * {@code F2276.LF276ID = LOTES227.LF276ID}) tiene {@code LF276Estad = 'C'} y
 * {@code LF276FchHa <= hoy}.
 *
 * <p>El legacy devuelve {@code &LoteAbierto} con {@code 1} (hay lote abierto) o {@code 0}; aquí se
 * devuelve un {@code boolean} con el mismo sentido.
 */
@Service
public class ServicioEstadoDeForm {

    private final Formulario2276Repositorio formularioRepositorio;

    public ServicioEstadoDeForm(Formulario2276Repositorio formularioRepositorio) {
        this.formularioRepositorio = formularioRepositorio;
    }

    /**
     * ¿El turno tiene formularios abiertos?
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @return {@code true} si hay al menos un formulario con lote cerrado y vencido
     */
    @Transactional(readOnly = true)
    public boolean hayFormularioAbierto(Integer sucNroId, String sucTipo, Integer turNroId) {
        return formularioRepositorio.contarFormulariosAbiertos(sucNroId, TurnoMapper.trim(sucTipo),
                turNroId, LocalDate.now().atStartOfDay()) > 0;
    }

    /**
     * Variante con la fecha de corte explícita (para las pruebas y para el cálculo del
     * vencimiento).
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @param hoy inicio del día a comparar con {@code LF276FchHa}
     * @return {@code true} si hay al menos un formulario con lote cerrado y vencido
     */
    @Transactional(readOnly = true)
    public boolean hayFormularioAbierto(Integer sucNroId, String sucTipo, Integer turNroId,
            LocalDateTime hoy) {
        return formularioRepositorio.contarFormulariosAbiertos(sucNroId, TurnoMapper.trim(sucTipo),
                turNroId, hoy) > 0;
    }
}
