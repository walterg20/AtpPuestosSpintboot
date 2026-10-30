package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link Puesto} → tabla {@code PUEPPRIM} (sólo lectura).
 *
 * <p>El login resuelve el puesto por su PK ({@code PueCod}) para obtener la descripción
 * ({@code PueDesc}) y el prefijo de red habilitada ({@code PueIPRed}) — R-LOG-012.
 */
public interface PuestoRepository extends JpaRepository<Puesto, Integer> {
}
