package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno;

import java.time.LocalDateTime;

public record BitacoraEvento(LocalDateTime fechaHora, String descripcion) {
}