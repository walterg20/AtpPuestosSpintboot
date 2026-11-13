package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.nucleo.domain.Formulario2276;
import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.domain.Usuario;
import ar.com.atp.puestos.nucleo.repository.Formulario2276Repositorio;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.repository.UsuarioRepository;
import ar.com.atp.puestos.shared.error.NotFoundException;
import ar.com.atp.puestos.puestos.service.dto.ResumenTurnoFilaDto;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TurnoReporteService {

    private static final String PLANTILLA = "reports/turno-resumen.jrxml";

    private final TurnoRepositorio turnoRepositorio;
    private final Formulario2276Repositorio formularioRepositorio;
    private final UsuarioRepository usuarioRepository;

    public TurnoReporteService(TurnoRepositorio turnoRepositorio,
            Formulario2276Repositorio formularioRepositorio) {
        this(turnoRepositorio, formularioRepositorio, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TurnoReporteService(TurnoRepositorio turnoRepositorio,
            Formulario2276Repositorio formularioRepositorio, UsuarioRepository usuarioRepository) {
        this.turnoRepositorio = turnoRepositorio;
        this.formularioRepositorio = formularioRepositorio;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer sucNroId, String sucTipo, Integer turNroId) {
        Short s = (sucNroId == null) ? null : sucNroId.shortValue();
        return generarPdfImpl(s, sucTipo, turNroId);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Short sucNroId, String sucTipo, Integer turNroId) {
        return generarPdfImpl(sucNroId, sucTipo, turNroId);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(short sucNroId, String sucTipo, int turNroId) {
        return generarPdfImpl(sucNroId, sucTipo, turNroId);
    }

    @Transactional(readOnly = true)
    public byte[] generar(Integer sucNroId, String sucTipo, Integer turNroId) {
        return generarPdf(sucNroId, sucTipo, turNroId);
    }

    @Transactional(readOnly = true)
    public byte[] generarReporte(Integer sucNroId, String sucTipo, Integer turNroId) {
        return generarPdf(sucNroId, sucTipo, turNroId);
    }

    private byte[] generarPdfImpl(Short sucNroId, String sucTipo, Integer turNroId) {
        Integer suc = (sucNroId == null) ? null : sucNroId.intValue();
        String sucTipoNorm = (sucTipo == null) ? null : sucTipo.trim();
        Turno turno = turnoRepositorio.findById(new TurnoId(suc, sucTipoNorm, turNroId))
                .orElseThrow(() -> new NotFoundException("TURNO_NO_ENCONTRADO",
                        "El turno no existe."));

        String usuarioNombre = null;
        if (usuarioRepository != null && turno.getTurUsuario() != null) {
            Optional<Usuario> u = usuarioRepository.findByUsuId(turno.getTurUsuario());
            if (u.isPresent()) {
                usuarioNombre = u.get().getUsuNomYApe();
            }
        }
        if (usuarioNombre == null) {
            usuarioNombre = turno.getTurUsuario();
        }
        String estadoDecod = decodificarEstado(turno.getTurEstado());

        List<Formulario2276> formularios = formularioRepositorio
                .findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(suc, sucTipoNorm, turNroId);
        List<ResumenTurnoFilaDto> filas = new ArrayList<>();
        for (Formulario2276 f : formularios) {
            if ("A".equalsIgnoreCase(trim(f.getF276Estad()))) {
                continue;
            }
            if (!"A".equalsIgnoreCase(trim(f.getF276GenWeb()))) {
                continue;
            }
            filas.add(new ResumenTurnoFilaDto(
                    f.getF276Id(),
                    trim(null),
                    null,
                    null,
                    null,
                    null,
                    null,
                    f.getF276FchMov(),
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    nvl(f.getF276AdIBTo()),
                    nvl(f.getF276AdCMTo()),
                    nvl(f.getF276AdicFl()),
                    nvl(f.getF276ImpuFl())));
        }
        filas.sort((a, b) -> {
            if (a.f276FchMov() == null && b.f276FchMov() == null) return 0;
            if (a.f276FchMov() == null) return 1;
            if (b.f276FchMov() == null) return -1;
            return a.f276FchMov().compareTo(b.f276FchMov());
        });

        try {
            JasperReport report = compile();
            Map<String, Object> params = new HashMap<>();
            params.put("P_TITULO", "Resumen de Turnos");
            params.put("P_FECHA_GENERACION", java.time.LocalDateTime.now());
            params.put("P_VERSION", "0.1.0");
            params.put("P_SUC_NRO_ID", suc);
            params.put("P_SUC_TIPO", sucTipoNorm);
            params.put("P_TUR_NRO_ID", turNroId);
            params.put("P_SUC_DESCR", null);
            params.put("P_TUR_USUARIO", trim(turno.getTurUsuario()));
            params.put("P_TUR_USUARIO_NOMBRE", trim(usuarioNombre));
            params.put("P_TUR_FCH_APE", turno.getTurFchApe());
            params.put("P_TUR_FCH_CIE", turno.getTurFchCie());
            params.put("P_TUR_ESTADO", estadoDecod);
            params.put("P_TUR_CPC_ID", turno.getTurCpcId());
            params.put("P_MENSAJE_VACIO", "Sin formularios para el turno seleccionado");
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(filas, false);
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error al generar el reporte", ex);
        }
    }

    private static JasperReport compile() throws Exception {
        try (InputStream in = new ClassPathResource(PLANTILLA).getInputStream()) {
            return JasperCompileManager.compileReport(in);
        }
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }

    private static String decodificarEstado(String estado) {
        String e = trim(estado);
        if ("A".equalsIgnoreCase(e)) return "Abierto";
        if ("C".equalsIgnoreCase(e)) return "Cerrado";
        return e == null ? "" : e;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}