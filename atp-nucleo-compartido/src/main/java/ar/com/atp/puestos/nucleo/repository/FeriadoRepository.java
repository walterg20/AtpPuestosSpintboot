package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Feriado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Feriado} → tabla {@code FERIADOS} (R-006, R-021). PK natural {@code FerFecha}. */
public interface FeriadoRepository extends JpaRepository<Feriado, LocalDateTime> {

    /** Feriados dentro del rango [{@code desde}, {@code hasta}] inclusive, ordenados por fecha. */
    List<Feriado> findByFerFechaBetweenOrderByFerFechaAsc(LocalDateTime desde, LocalDateTime hasta);
}
