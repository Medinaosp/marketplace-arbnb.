package softplasty.marketplace.modules.users.domain;

import java.time.Instant;
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
@Table(name = "solicitud_recuperacion")
public class SolicitudRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Stores the one-way hash of the reset token, never the clear-text token. */
    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "solicitado_en", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant solicitadoEn;

    @Column(name = "vence_en", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant venceEn;

    @Column(name = "usada", nullable = false)
    private boolean usada;

    protected SolicitudRecuperacion() {
    }

    public SolicitudRecuperacion(Usuario usuario, String token, Instant solicitadoEn,
                                 Instant venceEn, boolean usada) {
        this.usuario = usuario;
        this.token = token;
        this.solicitadoEn = solicitadoEn;
        this.venceEn = venceEn;
        this.usada = usada;
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getToken() { return token; }
    public Instant getSolicitadoEn() { return solicitadoEn; }
    public Instant getVenceEn() { return venceEn; }
    public boolean isUsada() { return usada; }

    public boolean estaVigente(Instant ahora) {
        return !usada && ahora.isBefore(venceEn);
    }

    public void marcarUsada() {
        usada = true;
    }
}
