package softplasty.marketplace.modules.accommodations.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PublicacionServicioId implements Serializable {

    @Column(name = "publicacion_id", nullable = false)
    private UUID publicacionId;

    @Column(name = "servicio_id", nullable = false)
    private UUID servicioId;

    protected PublicacionServicioId() {
    }

    public PublicacionServicioId(UUID publicacionId, UUID servicioId) {
        this.publicacionId = publicacionId;
        this.servicioId = servicioId;
    }

    public UUID getPublicacionId() { return publicacionId; }
    public UUID getServicioId() { return servicioId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PublicacionServicioId that)) return false;
        return Objects.equals(publicacionId, that.publicacionId)
                && Objects.equals(servicioId, that.servicioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicacionId, servicioId);
    }
}
