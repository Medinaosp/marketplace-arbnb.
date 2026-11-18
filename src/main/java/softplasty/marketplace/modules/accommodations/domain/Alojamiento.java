package softplasty.marketplace.modules.accommodations.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import softplasty.marketplace.modules.users.domain.Usuario;

@Entity
@Table(name = "alojamiento")
public class Alojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario anfitrion;

    @Column(name = "ciudad", nullable = false)
    private String ciudad;

    @Column(name = "ubicacion_texto", nullable = false)
    private String ubicacionTexto;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "capacidad_maxima", nullable = false)
    private int capacidadMaxima;

    @OneToMany(mappedBy = "alojamiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeriodoDisponibilidad> periodosDisponibilidad = new ArrayList<>();

    @OneToOne(mappedBy = "alojamiento", fetch = FetchType.LAZY)
    private Publicacion publicacion;

    protected Alojamiento() {
    }

    public Alojamiento(Usuario anfitrion, String ciudad, String ubicacionTexto,
                       String tipo, int capacidadMaxima) {
        this.anfitrion = anfitrion;
        this.ciudad = ciudad;
        this.ubicacionTexto = ubicacionTexto;
        this.tipo = tipo;
        this.capacidadMaxima = capacidadMaxima;
    }

    public UUID getId() { return id; }
    public Usuario getAnfitrion() { return anfitrion; }
    public String getCiudad() { return ciudad; }
    public String getUbicacionTexto() { return ubicacionTexto; }
    public String getTipo() { return tipo; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public List<PeriodoDisponibilidad> getPeriodosDisponibilidad() { return List.copyOf(periodosDisponibilidad); }
    public Publicacion getPublicacion() { return publicacion; }

    public void actualizarDatos(String ciudad, String ubicacionTexto, String tipo, int capacidadMaxima) {
        this.ciudad = ciudad;
        this.ubicacionTexto = ubicacionTexto;
        this.tipo = tipo;
        this.capacidadMaxima = capacidadMaxima;
    }

    public boolean estaDisponible(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        return periodosDisponibilidad.stream()
                .filter(periodo -> periodo.getTipo() == TipoPeriodo.BLOQUEADO)
                .noneMatch(periodo -> periodo.seSolapaCon(inicio, fin));
    }

    public PeriodoDisponibilidad bloquearPeriodo(LocalDate inicio, LocalDate fin, LocalDate hoy) {
        validarRango(inicio, fin);
        if (inicio.isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden bloquear fechas que ya pasaron.");
        }
        if (!estaDisponible(inicio, fin)) {
            throw new IllegalStateException("El periodo se cruza con un bloqueo existente.");
        }
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad(this, inicio, fin, TipoPeriodo.BLOQUEADO);
        periodo.validar();
        periodosDisponibilidad.add(periodo);
        return periodo;
    }

    public PeriodoDisponibilidad liberarPeriodo(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        PeriodoDisponibilidad existente = periodosDisponibilidad.stream()
                .filter(periodo -> periodo.getTipo() == TipoPeriodo.BLOQUEADO)
                .filter(periodo -> periodo.getInicio().equals(inicio) && periodo.getFin().equals(fin))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un bloqueo con exactamente esas fechas."));
        periodosDisponibilidad.remove(existente);
        return existente;
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}
