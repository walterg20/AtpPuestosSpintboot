package ar.com.atp.puestos.nucleo.web.mvc.turno;

import java.time.LocalDateTime;

public record BitacoraEvento(LocalDateTime fechaHora, String descripcion) {
}