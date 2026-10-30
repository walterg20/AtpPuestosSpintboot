package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Comision;
import ar.com.atp.puestos.nucleo.domain.ComisionId;
import ar.com.atp.puestos.nucleo.domain.ComisionIntegrante;
import ar.com.atp.puestos.nucleo.domain.ComisionIntegranteId;
import ar.com.atp.puestos.nucleo.domain.Persona;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * T-133 / T-305 / R-LOG-033…R-LOG-036: lectura de {@code COMISION} y {@code COMISIO1} contra la
 * base {@code Rentas} (sólo lectura). Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p>Verifica la paridad de los nombres físicos ({@code COMISION}, {@code COMISIO1}), las claves
 * primarias compuestas, el JOIN a {@code PERSONAS} que resuelve el responsable (el atributo
 * calculado {@code CPCResNyA} <strong>no</strong> es una columna) y el orden determinista de
 * {@code Seg_BusComi}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-133 - COMISION y COMISIO1: lectura de solo acceso")
class ComisionRepositoryIT {

    @Autowired
    private ComisionRepository comisionRepository;

    @Autowired
    private ComisionIntegranteRepository comisionIntegranteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Test
    @DisplayName("R-LOG-033/034: la comision se lee con su clave primaria compuesta")
    void laComisionSeLeeConSuClaveComposta() {
        List<Comision> comisiones = comisionRepository.findAll();
        Assumptions.assumeFalse(comisiones.isEmpty(), "COMISION vacia");

        Comision comision = comisiones.get(0);
        ComisionId id = new ComisionId(comision.getPueCod(), comision.getCpcId());

        assertThat(comisionRepository.findById(id)).isPresent();
        assertThat(comisionRepository.findAll()).hasSize(comisiones.size());
    }

    @Test
    @DisplayName("R-LOG-034: el integrante se lee con su clave primaria compuesta de 3 columnas")
    void elIntegranteSeLeeConSuClaveCompostaDeTresColumnas() {
        List<ComisionIntegrante> integrantes = comisionIntegranteRepository.findAll();
        Assumptions.assumeFalse(integrantes.isEmpty(), "COMISIO1 vacia");

        ComisionIntegrante integrante = integrantes.get(0);
        ComisionIntegranteId id = new ComisionIntegranteId(integrante.getPueCod(),
                integrante.getCpcId(), integrante.getCpcPerId());

        assertThat(comisionIntegranteRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("R-LOG-035: el responsable se resuelve por el JOIN a PERSONAS (no es columna)")
    void elResponsableSeResuelvePorElJoinAPersonas() {
        Comision conResponsable = comisionRepository.findAll().stream()
                .filter(c -> c.getCpcResId() != null && c.getCpcResId() != 0)
                .findFirst()
                .orElse(null);
        Assumptions.assumeTrue(conResponsable != null, "ninguna comision tiene responsable");

        List<Object[]> encontrados = comisionRepository.findComisionConResponsable(
                conResponsable.getPueCod(), conResponsable.getCpcId());

        assertThat(encontrados).isNotEmpty();
        Comision comision = (Comision) encontrados.get(0)[0];
        Persona persona = (Persona) encontrados.get(0)[1];
        assertThat(comision.getCpcResId()).isEqualTo(conResponsable.getCpcResId());
        assertThat(persona).isNotNull();
        assertThat(persona.getPerNomyApe()).isNotBlank();
        assertThat(persona.getPerNomyApe()).isNotEqualTo("Sin Comisión");
    }

    @Test
    @DisplayName("R-LOG-036: la comision ganada es la de mayor PERSONAS.PerNomyApe")
    void laOrdenDeSegBusComiEsLaDelMayorNombreYApellido() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        Integer hora = horaActual();

        Map<Integer, Integer> comisionesPorPersona = new LinkedHashMap<>();
        for (ComisionIntegrante integrante : comisionIntegranteRepository.findAll()) {
            comisionesPorPersona.merge(integrante.getCpcPerId(), 1, Integer::sum);
        }

        Integer personaConVarias = null;
        for (Integer perId : comisionesPorPersona.keySet()) {
            Set<String> distintas = clavesDistintas(
                    comisionRepository.findComisionesVigentesComoIntegrante(perId, hoy, hora));
            if (distintas.size() >= 2) {
                personaConVarias = perId;
                break;
            }
        }
        Assumptions.assumeTrue(personaConVarias != null,
                "ninguna persona tiene dos o mas comisiones vigentes");

        List<Comision> candidatas =
                comisionRepository.findComisionesVigentesComoIntegrante(personaConVarias, hoy, hora);
        Comision ganadora = candidatas.get(0);

        Map<String, List<Integer>> integrantesPorComision = new HashMap<>();
        for (ComisionIntegrante integrante : comisionIntegranteRepository.findAll()) {
            integrantesPorComision
                    .computeIfAbsent(clave(integrante.getPueCod(), integrante.getCpcId()),
                            k -> new ArrayList<>())
                    .add(integrante.getCpcPerId());
        }
        Map<Integer, String> nombres = new HashMap<>();
        for (Persona persona : personaRepository.findAll()) {
            nombres.put(persona.getPerId(), persona.getPerNomyApe());
        }

        String nombreGanadora = mayorNombre(integrantesPorComision, nombres,
                clave(ganadora.getPueCod(), ganadora.getCpcId()));

        for (String clave : clavesDistintas(candidatas)) {
            String nombreCandidata = mayorNombre(integrantesPorComision, nombres, clave);
            Assumptions.assumeTrue(nombreCandidata != null && nombreGanadora != null,
                    "candidatas sin nombre de integrante");
            assertThat(nombreGanadora)
                    .as("la comision ganada debe tener el mayor nombre y apellido")
                    .isGreaterThanOrEqualTo(nombreCandidata);
        }
    }

    @Test
    @DisplayName("R-LOG-038: el descarte por hora solo aplica cuando la comision vence hoy")
    void elDescartePorHoraSoloAplicaCuandoLaComisionVenceHoy() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        Integer hora = horaActual();

        int personasProbadas = 0;
        for (ComisionIntegrante integrante : comisionIntegranteRepository.findAll()) {
            if (personasProbadas++ > 40) {
                break;
            }
            List<Comision> candidatas = comisionRepository.findComisionesVigentesComoIntegrante(
                    integrante.getCpcPerId(), hoy, hora);
            for (Comision comision : candidatas) {
                boolean venceHoy = hoy.equals(comision.getCpcFchHas());
                boolean horarioVencido = comision.getCpcHorHas() != null
                        && comision.getCpcHorHas() < hora;
                assertThat(venceHoy && horarioVencido)
                        .as("una comision que vence hoy con la hora vencida debe descartarse")
                        .isFalse();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String clave(Integer pueCod, Integer cpcId) {
        return pueCod + "/" + cpcId;
    }

    private static Set<String> clavesDistintas(List<Comision> comisiones) {
        Set<String> claves = new LinkedHashSet<>();
        for (Comision comision : comisiones) {
            claves.add(clave(comision.getPueCod(), comision.getCpcId()));
        }
        return claves;
    }

    private static String mayorNombre(Map<String, List<Integer>> integrantesPorComision,
            Map<Integer, String> nombres, String clave) {
        List<Integer> integrantes = integrantesPorComision.get(clave);
        if (integrantes == null) {
            return null;
        }
        return integrantes.stream()
                .map(nombres::get)
                .filter(n -> n != null && !n.isBlank())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Integer horaActual() {
        LocalTime ahora = LocalTime.now();
        return ahora.getHour() * 100 + ahora.getMinute();
    }
}
