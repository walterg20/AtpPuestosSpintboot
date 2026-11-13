package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.LocalidadRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio del popup de seleccion manual de localidades (modulo 10, spec §5.2).
 *
 * <p>Es un <strong>wrapper</strong> sobre {@link LocalidadRepository}: reutiliza
 * {@code findAllConProvincia()} sin modificar la entidad ni el repositorio, y aplica los filtros
 * del legacy {@code ListadoLocalidades} (LocCod &gt;=, LocNom LIKE, ProvNombre/LocNomProv LIKE,
 * LocCodPost &gt;=) con paginacion (size default 5, sort LocCod asc).
 *
 * <p>La logica vive aca (G7): el controlador {@code web/mvc} solo traduce y delega.
 */
@Service
@Transactional(readOnly = true)
public class LocalidadPopupService {

    private final LocalidadRepository localidadRepository;

    public LocalidadPopupService(LocalidadRepository localidadRepository) {
        this.localidadRepository = localidadRepository;
    }

    /**
     * Listado paginado y filtrado de localidades (R-LOC-003, R-LOC-008, R-LOC-009).
     *
     * @param filtroLocCod     filtro codigo de localidad ({@code >=}); {@code null} = sin filtro
     * @param filtroLocNom     filtro nombre ({@code LIKE}); {@code null}/blank = sin filtro
     * @param filtroProvNombre filtro provincia ({@code LIKE} sobre ProvNombre/LocNomProv); {@code null}/blank = sin filtro
     * @param filtroLocCodPost filtro codigo postal ({@code >=}); {@code null} = sin filtro
     * @param pageable         paginacion (size default 5, sort LocCod asc)
     * @return pagina de localidades
     */
    public Page<Localidad> listar(Integer filtroLocCod, String filtroLocNom, String filtroProvNombre,
            Integer filtroLocCodPost, Pageable pageable) {
        List<Localidad> todas = localidadRepository.findAllConProvincia();
        if (todas == null) {
            todas = List.of();
        }

        String nom = normalizar(filtroLocNom);
        String prov = normalizar(filtroProvNombre);

        List<Localidad> filtradas = todas.stream()
                .filter(l -> filtroLocCod == null
                        || (l.getLocCod() != null && l.getLocCod() >= filtroLocCod))
                .filter(l -> nom == null || contiene(l.getLocNom(), nom)
                        || contiene(l.getLocNomProv(), nom))
                .filter(l -> prov == null || contieneProvincia(l, prov))
                .filter(l -> filtroLocCodPost == null
                        || (l.getLocCodPost() != null && l.getLocCodPost() >= filtroLocCodPost))
                .sorted(Comparator.comparing(Localidad::getLocCod, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        Pageable efectivo = pageable == null
                ? org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by("locCod"))
                : pageable;

        int total = filtradas.size();
        int offset = (int) Math.min(efectivo.getOffset(), total);
        int size = efectivo.getPageSize() <= 0 ? 5 : efectivo.getPageSize();
        int end = Math.min(offset + size, total);
        List<Localidad> content = offset <= end ? filtradas.subList(offset, end) : List.of();
        return new PageImpl<>(content, efectivo, total);
    }

    // ----- alias para compatibilidad con posibles nombres de referencia -----
    public Page<Localidad> buscar(Integer filtroLocCod, String filtroLocNom, String filtroProvNombre,
            Integer filtroLocCodPost, Pageable pageable) {
        return listar(filtroLocCod, filtroLocNom, filtroProvNombre, filtroLocCodPost, pageable);
    }

    public Page<Localidad> listarLocalidades(Integer filtroLocCod, String filtroLocNom,
            String filtroProvNombre, Integer filtroLocCodPost, Pageable pageable) {
        return listar(filtroLocCod, filtroLocNom, filtroProvNombre, filtroLocCodPost, pageable);
    }

    public Page<Localidad> listar(Pageable pageable) {
        return listar(null, null, null, null, pageable);
    }

    public List<Localidad> findAllConProvincia() {
        List<Localidad> todas = localidadRepository.findAllConProvincia();
        return todas == null ? List.of() : todas;
    }

    // ----- helpers -----
    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean contiene(String campo, String filtro) {
        if (campo == null) {
            return false;
        }
        return campo.toLowerCase().contains(filtro.toLowerCase());
    }

    private static boolean contieneProvincia(Localidad l, String filtro) {
        if (l.getProvincia() != null && l.getProvincia().getProvNombre() != null) {
            if (l.getProvincia().getProvNombre().toLowerCase().contains(filtro.toLowerCase())) {
                return true;
            }
        }
        return contiene(l.getLocNomProv(), filtro);
    }
}