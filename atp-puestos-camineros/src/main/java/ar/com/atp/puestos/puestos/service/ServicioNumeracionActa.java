package ar.com.atp.puestos.puestos.service;

import ar.com.atp.puestos.puestos.domain.ContadorFormulario;
import ar.com.atp.puestos.puestos.repository.ContadorFormularioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Numeración de Actas sobre el contador persistente {@code GES_SUCN.SucUltNum}
 * (spec {@code 09-formularios-acta} §3.11, plan §3.4, D-ACTA-13, D-ACTA-19, D-ACTA-21,
 * T-ACTA-129).
 *
 * <p>Reproduce el procedimiento legacy {@code F2276_GenNroForm}: lee la fila de {@code GES_SUCN}
 * para ({@code SucNroId}, {@code SucTipo}, {@code SucNuForm = 2276}), devuelve
 * {@code SucUltNum + 1} y escribe ese valor. Si la fila <strong>no existe</strong> (el
 * {@code when none} del legacy) la crea con {@code SucUltNum = 1} y devuelve {@code 1}.
 *
 * <p><strong>Desvío consciente del legacy</strong> (D-ACTA-19): el legacy <strong>no</strong>
 * bloquea la fila al incrementarla, de modo que dos altas simultáneas pueden obtener el mismo
 * {@code F276Id} y violar la PK {@code (F276Id, SucTipo, SucNroId)}. Aquí el incremento es
 * <strong>atómico</strong> ({@code UPDATE ... SET SucUltNum = SucUltNum + 1}) y se ejecuta
 * dentro de la <strong>misma transacción</strong> del alta ({@link Propagation#MANDATORY}), de
 * modo que el número y la cabecera se confirmen o reviertan juntos (R-ACTA-049).
 *
 * <p><strong>No</strong> se usa {@code MAX(F2276.F276Id) + 1}: la fuente autoritativa es el
 * contador (D-ACTA-21, SA-ACTA-15).
 */
@Service
public class ServicioNumeracionActa {

    /** Número de formulario de las Actas ({@code SucNuForm} en {@code GES_SUCN}). */
    public static final int NUMERO_FORMULARIO_ACTAS = 2276;

    /** Valor por defecto de {@code GES_SUCN.SucNuId} al crear la fila (D-ACTA-19, SA-ACTA-14). */
    public static final String VALOR_POR_DEFECTO_SUC_NU_ID = "MAN";

    private final ContadorFormularioRepositorio contadorRepositorio;

    public ServicioNumeracionActa(ContadorFormularioRepositorio contadorRepositorio) {
        this.contadorRepositorio = contadorRepositorio;
    }

    /**
     * Devuelve el próximo número de Acta de la sucursal y deja el contador actualizado.
     *
     * <p>Se exige una transacción en curso ({@link Propagation#MANDATORY}): el número y el Acta
     * deben confirmarse o revertirse juntos (R-ACTA-049).
     *
     * @param sucNroId sucursal de la sesión
     * @param sucTipo tipo de sucursal de la sesión
     * @return el número de Acta asignado (nunca {@code null})
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int siguienteNumeroActa(Integer sucNroId, String sucTipo) {
        String tipo = trim(sucTipo);
        int filas = contadorRepositorio.incrementarAtomico(sucNroId, tipo,
                NUMERO_FORMULARIO_ACTAS);
        if (filas > 0) {
            Integer actual = contadorRepositorio.valorActual(sucNroId, tipo,
                    NUMERO_FORMULARIO_ACTAS);
            return actual == null ? 1 : actual;
        }
        // La fila no existe: el legacy la crea con SucUltNum = 1 y devuelve 1.
        ContadorFormulario contador = new ContadorFormulario();
        contador.setSucNroId(sucNroId);
        contador.setSucTipo(tipo);
        contador.setSucNuForm(NUMERO_FORMULARIO_ACTAS);
        contador.setSucNuId(VALOR_POR_DEFECTO_SUC_NU_ID);
        contador.setSucUltNum(1);
        contadorRepositorio.save(contador);
        return 1;
    }

    /**
     * Próximo número de Acta <strong>sugerido</strong> (informativo, <strong>sin</strong>
     * incrementar el contador).
     *
     * <p>Lo usa {@link ActaService#prepararFormulario} para mostrar el número previsto en el
     * formulario; el número real lo asigna {@link #siguienteNumeroActa(Integer, String)} al
     * guardar.
     *
     * @param sucNroId sucursal de la sesión
     * @param sucTipo tipo de sucursal de la sesión
     * @return el próximo número, o {@code null} si no hay contexto suficiente
     */
    @Transactional(readOnly = true)
    public Integer proximoNumeroActa(Integer sucNroId, String sucTipo) {
        if (sucNroId == null || sucTipo == null) {
            return null;
        }
        Integer actual = contadorRepositorio.valorActual(sucNroId, trim(sucTipo),
                NUMERO_FORMULARIO_ACTAS);
        return actual == null ? 1 : actual + 1;
    }

    /** Recorta el {@code char(3)} del tipo de sucursal (paridad con el legacy). */
    private static String trim(String valor) {
        return valor == null ? null : valor.trim();
    }
}
