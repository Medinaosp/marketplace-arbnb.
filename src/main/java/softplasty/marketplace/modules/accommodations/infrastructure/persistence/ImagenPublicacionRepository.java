package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.accommodations.domain.ImagenPublicacion;

public interface ImagenPublicacionRepository extends JpaRepository<ImagenPublicacion, UUID> {

    List<ImagenPublicacion> findAllByPublicacion_IdOrderByOrdenAsc(UUID publicacionId);

    long countByPublicacion_Id(UUID publicacionId);

    boolean existsByPublicacion_IdAndOrden(UUID publicacionId, int orden);

    boolean existsByPublicacion_IdAndOrdenAndIdNot(UUID publicacionId, int orden, UUID imagenId);

    java.util.Optional<ImagenPublicacion> findByIdAndPublicacion_Id(UUID imagenId, UUID publicacionId);
}
