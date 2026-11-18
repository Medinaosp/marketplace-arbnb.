package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import softplasty.marketplace.modules.accommodations.domain.Alojamiento;

public interface AlojamientoRepository extends JpaRepository<Alojamiento, UUID> {

    List<Alojamiento> findAllByAnfitrion_IdOrderByCiudadAsc(UUID anfitrionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alojamiento a where a.id = :id")
    Optional<Alojamiento> findByIdForUpdate(@Param("id") UUID id);
}
