package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Comision;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Persona;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Puesto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Receptor;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Usuario;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ComisionRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.PersonaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.PuestoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ReceptorRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada del login: reproduce el algoritmo observable de {@code Seg_ControlAcceso} (perfil
 * INTERNO) y de {@code Seg_ControlAccesoWS} (perfil WS) — spec 03-login §3.3.
 *
 * <p><strong>El servicio no lanza excepciones</strong> (D-LOG-04, R-LOG-019): devuelve el código
 * 0-6 del legacy y el contexto resultante. El mensaje verbatim lo decide
 * {@link MensajesAcceso}.
 *
 * <p>El <strong>orden de las comprobaciones</strong> es el del legacy y es observable:
 *
 * <ol>
 *   <li>codificar el usuario;</li>
 *   <li>buscar en {@code SCRTY} (sin coincidencia ⇒ <strong>2</strong>);</li>
 *   <li>{@code UsuClave} vacía ⇒ <strong>2</strong>;</li>
 *   <li>codificar la clave y compararla ⇒ si no coincide, <strong>2</strong>;</li>
 *   <li>{@code UsuIngreso = 1} ⇒ <strong>5</strong>;</li>
 *   <li>resolver la persona (sin coincidencia ⇒ <strong>3</strong>);</li>
 *   <li>rama operativa ({@code PUE} / receptoría) y, según el caso, <strong>4</strong> o
 *       <strong>6</strong>;</li>
 *   <li>turno y contexto (rama {@code SucNroId >= 0}) o rama "fue responsable".</li>
 * </ol>
 *
 * <p>El código <strong>1 no es producible</strong>: el legacy sólo asigna 0, 2, 3, 4, 5 y 6.
 */
@Service
@Transactional(readOnly = true)
public class ServicioAcceso {

    /** Tipo de acceso del puesto caminerero (comparación exacta, como en el legacy). */
    private static final String TIPO_PUESTO = "PUE";

    /** Longitud de las variables {@code CHARACTER(10)} del legacy. */
    private static final int LARGO_IDENTIFICADOR = 10;

    /** Valor por defecto del responsable cuando la comisión no lo tiene. */
    private static final String SIN_COMISION = "Sin Comisión";

    /** Valor por defecto de la descripción de sucursal cuando el puesto no existe. */
    private static final String SIN_PUESTO_ASIGNADO = "Sin Puesto Asignado";

    /** Valor por defecto de la descripción de sucursal cuando la persona no tiene receptoría. */
    private static final String NO_EXISTE_RECEPTORIA = "No Existe Receptoria";

    private static final int CODIGO_ACCESO_CONCEDIDO = 0;
    private static final int CODIGO_CREDENCIALES_INVALIDAS = 2;
    private static final int CODIGO_SIN_PERSONA = 3;
    private static final int CODIGO_SIN_SUCURSAL = 4;
    private static final int CODIGO_DEBE_CAMBIAR_CLAVE = 5;
    private static final int CODIGO_IP_NO_HABILITADA = 6;

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final ComisionRepository comisionRepository;
    private final ReceptorRepository receptorRepository;
    private final TurnoRepositorio turnoRepository;
    private final PuestoRepository puestoRepository;
    private final VerificadorDeClave verificadorDeClave;
    private final VerificadorIpCliente verificadorIpCliente;

    public ServicioAcceso(UsuarioRepository usuarioRepository,
            PersonaRepository personaRepository,
            ComisionRepository comisionRepository,
            ReceptorRepository receptorRepository,
            TurnoRepositorio turnoRepository,
            PuestoRepository puestoRepository,
            VerificadorDeClave verificadorDeClave,
            VerificadorIpCliente verificadorIpCliente) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.comisionRepository = comisionRepository;
        this.receptorRepository = receptorRepository;
        this.turnoRepository = turnoRepository;
        this.puestoRepository = puestoRepository;
        this.verificadorDeClave = verificadorDeClave;
        this.verificadorIpCliente = verificadorIpCliente;
    }

    /**
     * Ejecuta el login.
     *
     * @param perfil variante del algoritmo (INTERNO o WS); {@code null} ⇒ INTERNO
     * @param credencial usuario, clave y tipo de acceso
     * @param datos datos de la petición (IP del cliente); {@code null} ⇒ sin IP
     * @return el resultado con el código 0-6, el contexto (sólo INTERNO) y la sucursal y la
     *         comisión resueltas
     */
    public ResultadoAcceso autenticar(PerfilAcceso perfil, Credencial credencial, DatosPeticion datos) {
        PerfilAcceso perfilEfectivo = perfil == null ? PerfilAcceso.INTERNO : perfil;
        Credencial credencialEfectiva = credencial == null ? new Credencial("", "", "") : credencial;

        String usuario = normalizarIdentificador(credencialEfectiva.usuario());
        String clave = normalizarClave(credencialEfectiva.clave());
        String tipo = credencialEfectiva.tipo();
        String usuarioMostrado = usuario.trim();

        // (1) codificar el usuario
        String usuarioEncrip = verificadorDeClave.codificar(usuario);

        // (2) buscar en SCRTY — sin coincidencia, usuario inexistente
        Optional<Usuario> encontrado = usuarioRepository.findByUsuId(usuarioEncrip);
        if (encontrado.isEmpty()) {
            return denegado(CODIGO_CREDENCIALES_INVALIDAS);
        }
        Usuario fila = encontrado.get();

        // (3) clave presente
        if (isVacia(fila.getUsuClave())) {
            return denegado(CODIGO_CREDENCIALES_INVALIDAS);
        }

        // (4) la clave coincide
        String claveEncrip = verificadorDeClave.codificar(clave);
        if (!verificadorDeClave.coincideCon(claveEncrip, fila.getUsuClave())) {
            return denegado(CODIGO_CREDENCIALES_INVALIDAS);
        }

        // (5) debe cambiar la clave
        if (Integer.valueOf(1).equals(fila.getUsuIngreso())) {
            return denegado(CODIGO_DEBE_CAMBIAR_CLAVE);
        }

        // A partir de acá el legacy lleva el error en 0.
        int error = CODIGO_ACCESO_CONCEDIDO;

        // (6) persona — sin coincidencia, el usuario no está relacionado con una persona
        List<Persona> personas = personaRepository.findByPerCodUsu(usuario);
        if (personas.isEmpty()) {
            return denegado(CODIGO_SIN_PERSONA);
        }
        Persona persona = personas.get(0);
        Integer perId = persona.getPerId();

        // (7) rama operativa
        int comisionId = 0;
        int sucursal = -1;
        String sucursalNombre = "";
        int responsableId = 0;
        /**
         * El legacy <strong>no inicializa</strong> {@code &CPCResNyA}: sólo la rama {@code PUE} lo
         * asigna con la llamada a {@code Seg_BusResponsable} (que sí arranca con
         * {@code 'Sin Comisión'}). En la rama de receptoría el valor queda <strong>vacío</strong>
         * (valor por defecto de una variable CHARACTER local).
         */
        String responsableNombre = "";
        String prefijoRed = "";

        if (TIPO_PUESTO.equals(tipo)) {
            Comision comision = buscarComisionActiva(perId);
            if (comision == null) {
                // Sin comisión activa: el legacy deja el error en 4 (y la sucursal en -1).
                error = CODIGO_SIN_SUCURSAL;
            } else {
                comisionId = comision.getCpcId() == null ? 0 : comision.getCpcId();
                sucursal = comision.getPueCod() == null ? -1 : comision.getPueCod();

                // Responsable de la comisión (Seg_BusResponsable): JOIN a PERSONAS.
                // Valores iniciales del procedimiento: CPCResId = 0, CPCResNyA = 'Sin Comisión'.
                responsableNombre = SIN_COMISION;
                Object[] responsable = resolverResponsable(comision);
                if (responsable != null) {
                    Comision comisionConResponsable = (Comision) responsable[0];
                    Persona personaResponsable = (Persona) responsable[1];
                    if (comisionConResponsable.getCpcResId() != null) {
                        responsableId = comisionConResponsable.getCpcResId();
                    }
                    if (personaResponsable != null && personaResponsable.getPerNomyApe() != null) {
                        responsableNombre = personaResponsable.getPerNomyApe();
                    }
                }

                // Descripción y prefijo de red del puesto (BuscarNomPuesto).
                Optional<Puesto> puesto = puestoRepository.findById(sucursal);
                if (puesto.isPresent()) {
                    sucursalNombre = puesto.get().getPueDesc() == null
                            ? SIN_PUESTO_ASIGNADO
                            : puesto.get().getPueDesc();
                    prefijoRed = puesto.get().getPueIPRed() == null ? "" : puesto.get().getPueIPRed();
                } else {
                    sucursalNombre = SIN_PUESTO_ASIGNADO;
                    prefijoRed = "";
                }

                // (8) chequeo de IP — sólo en la rama PUE y sólo en el perfil INTERNO
                if (perfilEfectivo == PerfilAcceso.INTERNO
                        && !verificadorIpCliente.esCompatible(
                                datos == null ? null : datos.ipCliente(), prefijoRed)) {
                    return denegado(CODIGO_IP_NO_HABILITADA);
                }
            }
        } else {
            // Rama de receptoría (Seg_BusRece): JOIN a RECEPTOR.
            List<Receptor> receptores = receptorRepository.findReceptoresDePersona(perId);
            if (receptores.isEmpty()) {
                sucursal = -1;
                sucursalNombre = NO_EXISTE_RECEPTORIA;
            } else {
                Receptor receptor = receptores.get(0);
                sucursal = receptor.getReceId() == null ? -1 : receptor.getReceId();
                sucursalNombre = receptor.getReceDsc() == null ? "" : receptor.getReceDsc();
            }
            comisionId = 0;
        }

        // (9) sucursal resuelta: turno + contexto
        if (sucursal >= 0) {
            int turno = resolverTurno(sucursal, tipo, comisionId);
            ContextoSesion contexto = perfilEfectivo == PerfilAcceso.INTERNO
                    ? ContextoSesion.completo(usuarioMostrado, fila.getUsuNomYApe(), perId,
                            comisionId, turno, sucursal, sucursalNombre, tipo, responsableId,
                            responsableNombre)
                    : null;
            return new ResultadoAcceso(error, contexto, sucursal, comisionId);
        }

        // (10) rama "fue responsable de una comisión" (sólo existe en el perfil INTERNO)
        if (perfilEfectivo == PerfilAcceso.INTERNO) {
            if (TIPO_PUESTO.equals(tipo)
                    && comisionRepository.countComisionesCerradasComoResponsable(perId) > 0) {
                // Contexto parcial de 4 campos y acceso concedido.
                ContextoSesion parcial = ContextoSesion.parcial(usuarioMostrado,
                        fila.getUsuNomYApe(), perId, tipo);
                return new ResultadoAcceso(CODIGO_ACCESO_CONCEDIDO, parcial, 0, 0);
            }
            return denegado(CODIGO_SIN_SUCURSAL);
        }

        // Perfil WS: el bloque de historia está comentado ⇒ el código conserva su valor (0 o 4).
        return new ResultadoAcceso(error, null, 0, 0);
    }

    /**
     * Comisión activa de la persona: primero como <strong>integrante</strong> y, si no hay, como
     * <strong>responsable</strong> ({@code Seg_BusComi}).
     */
    private Comision buscarComisionActiva(Integer perId) {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        Integer hora = horaActual();

        List<Comision> comoIntegrante =
                comisionRepository.findComisionesVigentesComoIntegrante(perId, hoy, hora);
        if (!comoIntegrante.isEmpty()) {
            return comoIntegrante.get(0);
        }
        List<Comision> comoResponsable =
                comisionRepository.findComisionesVigentesComoResponsable(perId, hoy, hora);
        return comoResponsable.isEmpty() ? null : comoResponsable.get(0);
    }

    /** Responsable de la comisión con su nombre (JOIN a {@code PERSONAS}). */
    private Object[] resolverResponsable(Comision comision) {
        List<Object[]> encontrados = comisionRepository.findComisionConResponsable(
                comision.getPueCod(), comision.getCpcId());
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    /** Turno activo de la sucursal ({@code PPrim_BusTur}); {@code 0} si no hay. */
    private int resolverTurno(int sucursal, String tipo, int comisionId) {
        List<Turno> turnos = turnoRepository.findTurnosActivos(sucursal, tipo, comisionId);
        if (turnos.isEmpty() || turnos.get(0).getTurNroId() == null) {
            return 0;
        }
        return turnos.get(0).getTurNroId();
    }

    /** Hora actual como número {@code HHMM} (la columna {@code CPCHorHas} es un {@code int}). */
    private static Integer horaActual() {
        LocalTime ahora = LocalTime.now();
        return ahora.getHour() * 100 + ahora.getMinute();
    }

    /**
     * Normaliza el identificador: a mayúsculas y rellenado con espacios hasta 10 caracteres
     * (R-LOG-001). El recorte a 10 reproduce el {@code CHARACTER(10)} del legacy.
     */
    private static String normalizarIdentificador(String texto) {
        return rellenar(texto == null ? "" : texto.toUpperCase(Locale.ROOT));
    }

    /**
     * Normaliza la clave: rellenada con espacios hasta 10 caracteres (el {@code CHARACTER(10)} del
     * legacy). No se convierte a mayúsculas aquí: el codificador lo hace (como {@code Seg_Encripta}).
     */
    private static String normalizarClave(String texto) {
        return rellenar(texto == null ? "" : texto);
    }

    private static String rellenar(String texto) {
        if (texto.length() >= LARGO_IDENTIFICADOR) {
            return texto.substring(0, LARGO_IDENTIFICADOR);
        }
        return texto + " ".repeat(LARGO_IDENTIFICADOR - texto.length());
    }

    /** Vacío o compuesto sólo por espacios (como {@code IsEmpty()} del legacy). */
    private static boolean isVacia(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Resultado denegado: sin contexto y sin sucursal ni comisión resueltas. */
    private static ResultadoAcceso denegado(int codigo) {
        return new ResultadoAcceso(codigo, null, 0, 0);
    }
}
