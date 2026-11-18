package softplasty.marketplace.modules.accommodations.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "publicacion")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alojamiento_id", nullable = false, unique = true)
    private Alojamiento alojamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_cancelacion_id", nullable = false)
    private PoliticaCancelacion politicaCancelacion;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "precio_noche", nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal precioNoche;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reglas_uso", nullable = false, columnDefinition = "TEXT[]")
    private String[] reglasUso = new String[0];

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPublicacion estado;

    @Column(name = "cambios_pendientes_revision", nullable = false)
    private boolean cambiosPendientesRevision;

    protected Publicacion() {
    }

    public Publicacion(Alojamiento alojamiento, PoliticaCancelacion politicaCancelacion,
                       String titulo, String descripcion, BigDecimal precioNoche,
                       String[] reglasUso, EstadoPublicacion estado) {
        this.alojamiento = alojamiento;
        this.politicaCancelacion = politicaCancelacion;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precioNoche = precioNoche;
        this.reglasUso = reglasUso == null ? new String[0] : reglasUso.clone();
        this.estado = estado;
    }

    public UUID getId() { return id; }
    public Alojamiento getAlojamiento() { return alojamiento; }
    public PoliticaCancelacion getPoliticaCancelacion() { return politicaCancelacion; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecioNoche() { return precioNoche; }
    public String[] getReglasUso() { return reglasUso.clone(); }
    public EstadoPublicacion getEstado() { return estado; }
    public boolean isCambiosPendientesRevision() { return cambiosPendientesRevision; }

    public void editarFicha(String nuevoTitulo, String nuevaDescripcion, BigDecimal nuevoPrecio,
                            String[] nuevasReglas, PoliticaCancelacion nuevaPolitica,
                            int cantidadImagenes) {
        if (nuevoPrecio == null || nuevoPrecio.signum() <= 0) {
            throw new IllegalArgumentException("El precio por noche debe ser mayor que cero.");
        }
        boolean cambioImportante = !Objects.equals(titulo, nuevoTitulo)
                || !Objects.equals(descripcion, nuevaDescripcion)
                || !Arrays.equals(reglasUso, nuevasReglas)
                || !Objects.equals(politicaCancelacion.getId(), nuevaPolitica.getId());
        if (cambioImportante) {
            registrarCambioImportante(cantidadImagenes);
        }
        titulo = nuevoTitulo;
        descripcion = nuevaDescripcion;
        precioNoche = nuevoPrecio;
        reglasUso = nuevasReglas == null ? new String[0] : nuevasReglas.clone();
        politicaCancelacion = nuevaPolitica;
    }

    public void registrarCambioImportante(int cantidadImagenes) {
        switch (estado) {
            case ACTIVA -> {
                exigirImagen(cantidadImagenes);
                estado = EstadoPublicacion.PENDIENTE_REVISION;
                cambiosPendientesRevision = false;
            }
            case PAUSADA -> cambiosPendientesRevision = true;
            default -> { }
        }
    }

    public void enviarARevision(int cantidadImagenes) {
        if (estado != EstadoPublicacion.BORRADOR && estado != EstadoPublicacion.BLOQUEADA) {
            throw new IllegalStateException("Solo un borrador o una publicación bloqueada puede enviarse a revisión.");
        }
        exigirDatosObligatorios();
        exigirImagen(cantidadImagenes);
        estado = EstadoPublicacion.PENDIENTE_REVISION;
        cambiosPendientesRevision = false;
    }

    public void aprobar(int cantidadImagenes) {
        if (estado != EstadoPublicacion.PENDIENTE_REVISION) {
            throw new IllegalStateException("Solo una publicación pendiente puede aprobarse.");
        }
        exigirImagen(cantidadImagenes);
        estado = EstadoPublicacion.ACTIVA;
        cambiosPendientesRevision = false;
    }

    /** The moderation reason is validated but intentionally not persisted by this module. */
    public void bloquear(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del bloqueo es obligatorio.");
        }
        if (estado != EstadoPublicacion.PENDIENTE_REVISION
                && estado != EstadoPublicacion.ACTIVA
                && estado != EstadoPublicacion.PAUSADA) {
            throw new IllegalStateException("La publicación no puede bloquearse desde su estado actual.");
        }
        estado = EstadoPublicacion.BLOQUEADA;
        cambiosPendientesRevision = false;
    }

    public void pausar() {
        if (estado != EstadoPublicacion.ACTIVA) {
            throw new IllegalStateException("Solo una publicación activa puede pausarse.");
        }
        estado = EstadoPublicacion.PAUSADA;
    }

    public void activar(int cantidadImagenes) {
        if (estado != EstadoPublicacion.PAUSADA) {
            throw new IllegalStateException("Solo una publicación pausada puede reactivarse.");
        }
        exigirImagen(cantidadImagenes);
        if (cambiosPendientesRevision) {
            estado = EstadoPublicacion.PENDIENTE_REVISION;
            cambiosPendientesRevision = false;
        } else {
            estado = EstadoPublicacion.ACTIVA;
        }
    }

    private void exigirDatosObligatorios() {
        if (titulo == null || titulo.isBlank() || descripcion == null || descripcion.isBlank()
                || precioNoche == null || precioNoche.signum() <= 0
                || alojamiento == null || politicaCancelacion == null) {
            throw new IllegalStateException("Completa los datos obligatorios antes de enviar a revisión.");
        }
    }

    private void exigirImagen(int cantidadImagenes) {
        if (cantidadImagenes < 1) {
            throw new IllegalStateException("Se requiere al menos una imagen para este cambio de estado.");
        }
    }
}
