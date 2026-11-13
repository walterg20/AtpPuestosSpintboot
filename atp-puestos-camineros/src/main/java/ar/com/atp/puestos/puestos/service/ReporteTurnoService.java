package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.nucleo.repository.Formulario2276Repositorio;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.repository.UsuarioRepository;
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