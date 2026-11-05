package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.shared.error.NotFoundException;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimarioId;
import ar.com.atp.puestos.puestos.domain.PaseGuia;
import ar.com.atp.puestos.puestos.repository.DetalleProductoPrimarioRepository;
import ar.com.atp.puestos.puestos.repository.PaseGuiaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CargaService {

    public static final String MSG_EXITO = "La guia ha sido marcada con exito";
    public static final String MSG_ANULADA = "No se puede marcar el pase de la guía porque fue anulada";
    public static final String MSG_INTERNA = "No se puede marcar el pase de una guía interna";
    public static final String MSG_DUPLICADA = "Guia Duplicada , ya paso por algun puesto";
    public static final String MSG_USUARIO_SIN_PUESTO = "Usuario sin Puesto Asignado";
    public static final String MSG_CONFIRMACION = "Esta seguro que desea Marcar el formulario?";

    private final DetalleProductoPrimarioRepository detalleRepository;
    private final PaseGuiaRepository paseRepository;

    public CargaService(DetalleProductoPrimarioRepository detalleRepository,
            PaseGuiaRepository paseRepository) {
        this.detalleRepository = detalleRepository;
        this.paseRepository = paseRepository;
    }

    public List<DetalleProductoPrimario> buscarFormularios(String dppNroForm) {
        if (dppNroForm == null || dppNroForm.isBlank()) {
            return List.of();
        }
        List<DetalleProductoPrimario> result = detalleRepository.findByDppNroForm(dppNroForm.trim());
        for (DetalleProductoPrimario d : result) {
            d.setTotalGeneral(calcularTotalGeneral(d));
        }
        return result;
    }

    public BigDecimal calcularTotalGeneral(DetalleProductoPrimario d) {
        if (d == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        sum = sum.add(nz(d.getDppAdCMTot()));
        sum = sum.add(nz(d.getDppAdIBTot()));
        sum = sum.add(nz(d.getDppAdicFle()));
        sum = sum.add(nz(d.getDppImCMTot()));
        sum = sum.add(nz(d.getDppImIBTot()));
        sum = sum.add(nz(d.getDppImpuFle()));
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional
    public ResultadoMarcado marcarGuia(BigDecimal conCuit, Short dppSucu, Integer calPerFisc, Short dppSecu,
            String dppNroForm, String dppPaseOperador, String sucTipo, Short sucNroId,
            ContextoSesion context) {
        DetalleProductoPrimarioId id = new DetalleProductoPrimarioId(conCuit, dppSucu, calPerFisc, dppSecu,
                dppNroForm);
        DetalleProductoPrimario cabecera = detalleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Formulario " + dppNroForm + " no encontrado."));

        if (cabecera.getDppMarcAnu() != null && cabecera.getDppMarcAnu() == 1) {
            return new ResultadoMarcado(false, 0, MSG_ANULADA);
        }
        if ("I".equals(cabecera.getDppClasif())) {
            return new ResultadoMarcado(false, 0, MSG_INTERNA);
        }

        // Actualizar cabecera
        cabecera.setDppUltPSuN(sucNroId);
        cabecera.setDppUltPSuT(sucTipo);
        cabecera.setDppUltPaOp(dppPaseOperador);
        detalleRepository.save(cabecera);

        // Calcular Duplicado
        int previos = (int) paseRepository.countByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroFormAndDppPaseEstAndSucTipo(
                conCuit, dppSucu, calPerFisc, dppSecu, dppNroForm, "C", "PUE");
        int duplicado = 1 + previos;

        // Indice = 1 + cantidad de pases existentes (todos)
        int indice = 1 + paseRepository
                .findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm(conCuit, dppSucu, calPerFisc, dppSecu,
                        dppNroForm)
                .size();

        Integer comision = (context == null) ? 0 : context.comision();
        if (comision == null) {
            comision = 0;
        }

        PaseGuia pase = new PaseGuia();
        pase.setConCuit(conCuit);
        pase.setDppSucu(dppSucu);
        pase.setCalPerFisc(calPerFisc);
        pase.setDppSecu(dppSecu);
        pase.setDppNroForm(dppNroForm);
        pase.setDppPaseId((short) indice);
        pase.setDppPaseFec(LocalDateTime.now());
        pase.setDppPaseOpe(dppPaseOperador);
        pase.setDppPaseHor(LocalDateTime.now());
        pase.setDppPaseEst("C");
        pase.setSucTipo(sucTipo);
        pase.setSucNroId(sucNroId);
        pase.setDppPaseCo(comision);

        paseRepository.save(pase);

        return new ResultadoMarcado(true, duplicado, MSG_EXITO);
    }
}