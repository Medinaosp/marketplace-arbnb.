package softplasty.marketplace.modules.accommodations.domain;

import java.time.LocalDate;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "periodo_disponibilidad")
public class PeriodoDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alojamiento_id", nullable = false)
    private Alojamiento alojamiento;

    @Column(name = "inicio", nullable = false)
    private LocalDate inicio;

    @Column(name = "fin", nullable = false)
    private LocalDate fin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPeriodo tipo;

    protected PeriodoDisponibilidad() {
    }

    public PeriodoDisponibilidad(Alojamiento alojamiento, LocalDate inicio,
                                 LocalDate fin, TipoPeriodo tipo) {
        this.alojamiento = alojamiento;
        this.inicio = inicio;
        this.fin = fin;
        this.tipo = tipo;
    }

    public UUID getId() { return id; }
    public Alojamiento getAlojamiento() { return alojamiento; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFin() { return fin; }
    public TipoPeriodo getTipo() { return tipo; }

    public void validar() {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        if (tipo == null || alojamiento == null) {
            throw new IllegalArgumentException("El periodo debe tener alojamiento y tipo.");
        }
    }

    public boolean seSolapaCon(LocalDate otraInicio, LocalDate otraFin) {
        validarRango(otraInicio, otraFin);
        return inicio.isBefore(otraFin) && otraInicio.isBefore(fin);
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(inicio) && fecha.isBefore(fin);
    }

    private void validarRango(LocalDate otraInicio, LocalDate otraFin) {
        if (otraInicio == null || otraFin == null || !otraInicio.isBefore(otraFin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}
