package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.accommodations.domain.PublicacionServicio;
import softplasty.marketplace.modules.accommodations.domain.PublicacionServicioId;

public interface PublicacionServicioRepository extends JpaRepository<PublicacionServicio, PublicacionServicioId> {

    List<PublicacionServicio> findAllByPublicacion_Id(UUID publicacionId);

    void deleteAllByPublicacion_Id(UUID publicacionId);

    boolean existsByPublicacion_IdAndServicio_Id(UUID publicacionId, UUID servicioId);
}
