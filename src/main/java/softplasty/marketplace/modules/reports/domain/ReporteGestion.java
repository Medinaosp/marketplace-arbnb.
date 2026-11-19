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
@Table(name = "reporte_gestion")
public class ReporteGestion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;
    @Column(nullable = false)
    private String tipo;
    @Column(nullable = false)
    private String formato;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String filtros;
    @Column(name = "generado_en", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant generadoEn;

    protected ReporteGestion() {}
    public ReporteGestion(UUID solicitanteId, String tipo, String formato, String filtros, Instant generadoEn) {
        this.solicitanteId = solicitanteId;
        this.tipo = tipo;
        this.formato = formato;
        this.filtros = filtros;
        this.generadoEn = generadoEn;
    }
    public UUID getId() { return id; }
    public UUID getSolicitanteId() { return solicitanteId; }
    public String getTipo() { return tipo; }
    public String getFormato() { return formato; }
    public String getFiltros() { return filtros; }
    public Instant getGeneradoEn() { return generadoEn; }
}
