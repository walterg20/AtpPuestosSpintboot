package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.Formulario2276Repositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.UsuarioRepository;
public class ReporteTurnoService extends TurnoReporteService {
    public ReporteTurnoService(TurnoRepositorio turnoRepositorio,
            Formulario2276Repositorio formularioRepositorio) {
        super(turnoRepositorio, formularioRepositorio);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReporteTurnoService(TurnoRepositorio turnoRepositorio,
            Formulario2276Repositorio formularioRepositorio, UsuarioRepository usuarioRepository) {
        super(turnoRepositorio, formularioRepositorio, usuarioRepository);
    }
}