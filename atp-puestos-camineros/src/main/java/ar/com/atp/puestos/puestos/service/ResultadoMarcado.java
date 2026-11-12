package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;

public record ResultadoMarcado(boolean exito, int duplicado, String mensaje,
        DetalleProductoPrimario detalle) {

    public ResultadoMarcado(boolean exito, int duplicado, String mensaje) {
        this(exito, duplicado, mensaje, null);
    }
}