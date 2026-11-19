package softplasty.marketplace.modules.reports.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import softplasty.marketplace.modules.reports.domain.EventoAuditoria;
import softplasty.marketplace.modules.reports.infrastructure.persistence.EventoAuditoriaRepository;

@Service
public class EventoAuditoriaService {
    private final EventoAuditoriaRepository eventos;
    private final Clock clock;
    public EventoAuditoriaService(EventoAuditoriaRepository eventos, Clock clock) {
        this.eventos = eventos; this.clock = clock;
    }
    @Transactional
    public void registrar(UUID actorId, String accion, String entidad, UUID entidadId) {
        eventos.save(new EventoAuditoria(actorId, accion, entidad, entidadId,
                "EXITOSO", Instant.now(clock)));
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(UUID actorId, String accion, String entidad) {
        eventos.save(new EventoAuditoria(actorId, accion, entidad, null,
                "FALLIDO", Instant.now(clock)));
    }
}
