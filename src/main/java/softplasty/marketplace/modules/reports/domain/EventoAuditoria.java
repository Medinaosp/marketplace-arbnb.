package softplasty.marketplace.modules.reports.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(nullable = false)
    private String accion;
    @Column(nullable = false)
    private String entidad;
    @Column(name = "entidad_id")
    private UUID entidadId;
    @Column(nullable = false)
    private String resultado;
    @Column(name = "ocurrido_en", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant ocurridoEn;

    protected EventoAuditoria() {}
    public EventoAuditoria(UUID actorId, String accion, String entidad, UUID entidadId,
                           String resultado, Instant ocurridoEn) {
        this.actorId = actorId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.resultado = resultado;
        this.ocurridoEn = ocurridoEn;
    }
    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public UUID getEntidadId() { return entidadId; }
    public String getResultado() { return resultado; }
    public Instant getOcurridoEn() { return ocurridoEn; }
}
