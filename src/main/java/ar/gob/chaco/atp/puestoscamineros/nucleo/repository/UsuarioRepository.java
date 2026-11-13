package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Usuario} → tabla {@code SCRTY} (sólo lectura).
 *
 * <p><strong>T-102 / D-LOG-05</strong>: la comparación del identificador cifrado se hace con
 * {@code UPPER()} <strong>explícito en ambos lados</strong>
 * ({@code UPPER(UsuId) = UPPER(:usuarioEncrip)}). El legacy compara
 * {@code UsuId.ToUpper() = &UsuarioEncrip} y depende de la collation
 * {@code Latin1_General_CI_AS}; 504 de los 552 {@code UsuId} contienen {@code b} minúscula y 61
 * contienen {@code a} minúscula. Al explicitar el {@code UPPER()} en los dos lados, la paridad no
 * depende de la collation de la base (R-LOG-004).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    /**
     * Busca el usuario por su identificador <strong>cifrado</strong> (el resultado de
     * {@code Seg_Encripta}), sin distinguir mayúsculas.
     *
     * @param usuarioEncrip identificador cifrado (10 caracteres)
     * @return la fila de {@code SCRTY} o {@link Optional#empty()}
     */
    @Query("select u from Usuario u where upper(u.usuId) = upper(:usuarioEncrip)")
    Optional<Usuario> findByUsuId(@Param("usuarioEncrip") String usuarioEncrip);
}
