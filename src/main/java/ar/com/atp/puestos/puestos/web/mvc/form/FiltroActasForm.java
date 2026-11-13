package ar.gob.chaco.atp.puestoscamineros.web.mvc.form;

import ar.gob.chaco.atp.puestoscamineros.service.model.ActaFiltro;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Objeto de binding de los <strong>filtros de búsqueda</strong> del listado de Actas
 * (spec 09 §4.1, R-ACTA-011).
 *
 * <p>Es un <strong>adaptador de presentación</strong>: no contiene reglas de negocio. Traduce los
 * parámetros de la URL al contrato del servicio mediante {@link #aFiltro()} (D-ACTA-22: el
 * contrato del servicio vive en {@code service/model/**}; esta capa sólo traduce).
 *
 * <p>Los campos se nombran como los del legacy: {@code f276Id} (nº de Acta, coincidencia exacta),
 * {@code cuit} (de cualquiera de los tres roles), {@code razonSocial} (parcial, sin distinguir
 * mayúsculas) y el rango {@code desde}/{hasta} sobre {@code F276FchMov}.
 */
public class FiltroActasForm {

    /** Nº de Acta ({@code F276Id}): coincidencia exacta. */
    private Integer f276Id;

    /** CUIT de cualquiera de los tres roles (vendedor, destinatario o transportista). */
    private Long cuit;

    /** Razón Social: coincidencia parcial, sin distinguir mayúsculas. */
    private String razonSocial;

    /** Fecha inicial del rango sobre {@code F276FchMov}. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    /** Fecha final del rango sobre {@code F276FchMov}. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;

    public Integer getF276Id() {
        return f276Id;
    }

    public void setF276Id(Integer f276Id) {
        this.f276Id = f276Id;
    }

    public Long getCuit() {
        return cuit;
    }

    public void setCuit(Long cuit) {
        this.cuit = cuit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    /**
     * Traduce el filtro de la vista al contrato del servicio.
     *
     * @return el filtro equivalente en el contrato {@code service.model.ActaFiltro}
     */
    public ActaFiltro aFiltro() {
        return new ActaFiltro(f276Id, cuit, razonSocial, desde, hasta);
    }
}
