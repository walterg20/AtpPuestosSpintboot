package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Feriado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Feriado} → tabla {@code FERIADOS} (R-006, R-021). PK natural {@code FerFecha}. */
public interface FeriadoRepository extends JpaRepository<Feriado, LocalDateTime> {

    /** Feriados dentro del rango [{@code desde}, {@code hasta}] inclusive, ordenados por fecha. */
    List<Feriado> findByFerFechaBetweenOrderByFerFechaAsc(LocalDateTime desde, LocalDateTime hasta);

    /**
     * ¿La fecha es un feriado? (módulo {@code 06-turnos}, R-TUR-017).
     *
     * <p>Reproduce el {@code For Each where FerFecha = &fechaDesde} del procedimiento legacy
     * {@code Nuc_CalcularVencimientoXDias}: la comparación es por fecha exacta (medianoche), que
     * es como están guardados los feriados ({@code FerFecha} es siempre {@code datetime} a las
     * 00:00:00).
     *
     * @param ferFecha fecha a comprobar (a medianoche)
     * @return {@code true} si la fecha es feriado
     */
    boolean existsByFerFecha(LocalDateTime ferFecha);
}
