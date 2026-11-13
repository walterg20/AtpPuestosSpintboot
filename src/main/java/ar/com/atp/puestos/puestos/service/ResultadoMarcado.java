package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario;

public record ResultadoMarcado(boolean exito, int duplicado, String mensaje,
        DetalleProductoPrimario detalle) {

    public ResultadoMarcado(boolean exito, int duplicado, String mensaje) {
        this(exito, duplicado, mensaje, null);
    }
}