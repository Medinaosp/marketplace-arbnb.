package softplasty.marketplace.modules.reports.infrastructure.persistence;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.reports.domain.ReporteGestion;

public interface ReporteGestionRepository extends JpaRepository<ReporteGestion, UUID> {
    List<ReporteGestion> findAllBySolicitanteIdOrderByGeneradoEnDesc(UUID solicitanteId);
    List<ReporteGestion> findAllByOrderByGeneradoEnDesc();
}
