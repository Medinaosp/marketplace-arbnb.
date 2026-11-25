package softplasty.marketplace.modules.accommodations.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "imagen_publicacion")
public class ImagenPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "referencia", nullable = false)
    private String referencia;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "texto_alternativo", nullable = false)
    private String textoAlternativo;

    @Column(name = "contenido", columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "tipo_contenido")
    private String tipoContenido;

    protected ImagenPublicacion() {
    }

    public ImagenPublicacion(Publicacion publicacion, String referencia,
                             int orden, String textoAlternativo) {
        this.publicacion = publicacion;
        this.referencia = referencia;
        this.orden = orden;
        this.textoAlternativo = textoAlternativo;
    }

    public UUID getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public String getReferencia() { return referencia; }
    public int getOrden() { return orden; }
    public String getTextoAlternativo() { return textoAlternativo; }
    public byte[] getContenido() { return contenido; }
    public String getTipoContenido() { return tipoContenido; }

    public void guardarArchivo(byte[] datos, String tipo) {
        this.contenido = datos;
        this.tipoContenido = tipo;
    }

    public void asignarReferenciaArchivo() {
        this.referencia = "/api/imagenes/" + id;
    }

    public void cambiarOrden(int nuevoOrden) {
        if (nuevoOrden < 1) {
            throw new IllegalArgumentException("El orden de la imagen debe ser un entero positivo.");
        }
        orden = nuevoOrden;
    }
}
