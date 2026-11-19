package softplasty.marketplace.modules.reports.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.reports.domain.EventoAuditoria;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {
    List<EventoAuditoria> findAllByOcurridoEnGreaterThanEqualAndOcurridoEnLessThanOrderByOcurridoEnDesc(
            Instant desde, Instant hasta);
}
