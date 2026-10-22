package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clave compuesta de {@link ProductoVigencia} ({@code PRODUCT1.ProdCod} + {@code ProdFchVig}) —
 * spec §4.3 / §4.5. {@code ProdCod} se deriva de la asociación padre vía {@code @MapsId}.
 */
@Embeddable
public class ProductoVigenciaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ProdCod", nullable = false)
    private Integer prodCod;

    @Column(name = "ProdFchVig", nullable = false)
    private LocalDateTime prodFchVig;

    public ProductoVigenciaId() {
        // requerido por JPA
    }

    public ProductoVigenciaId(Integer prodCod, LocalDateTime prodFchVig) {
        this.prodCod = prodCod;
        this.prodFchVig = prodFchVig;
    }

    public Integer getProdCod() {
        return prodCod;
    }

    public void setProdCod(Integer prodCod) {
        this.prodCod = prodCod;
    }

    public LocalDateTime getProdFchVig() {
        return prodFchVig;
    }

    public void setProdFchVig(LocalDateTime prodFchVig) {
        this.prodFchVig = prodFchVig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductoVigenciaId other)) {
            return false;
        }
        return Objects.equals(prodCod, other.prodCod)
                && Objects.equals(prodFchVig, other.prodFchVig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prodCod, prodFchVig);
    }
}
