package softplasty.marketplace.modules.accommodations.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "publicacion_servicio")
public class PublicacionServicio {

    @EmbeddedId
    private PublicacionServicioId id;

    @MapsId("publicacionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @MapsId("servicioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioAlojamiento servicio;

    protected PublicacionServicio() {
    }

    public PublicacionServicio(Publicacion publicacion, ServicioAlojamiento servicio) {
        this.publicacion = publicacion;
        this.servicio = servicio;
        this.id = new PublicacionServicioId(publicacion.getId(), servicio.getId());
    }

    public PublicacionServicioId getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public ServicioAlojamiento getServicio() { return servicio; }
}
