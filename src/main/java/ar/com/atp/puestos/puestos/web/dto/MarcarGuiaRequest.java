package ar.gob.chaco.atp.puestoscamineros.web.dto;

import java.math.BigDecimal;

public record MarcarGuiaRequest(
        BigDecimal conCuit,
        Short dppSucu,
        Integer calPerFisc,
        Short dppSecu,
        String dppNroForm,
        String dppPaseOperador,
        String sucTipo,
        Short sucNroId) {
}