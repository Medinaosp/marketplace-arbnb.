package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.accommodations.domain.ServicioAlojamiento;

public interface ServicioAlojamientoRepository extends JpaRepository<ServicioAlojamiento, UUID> {

    Optional<ServicioAlojamiento> findByCodigo(String codigo);

    List<ServicioAlojamiento> findAllByActivoTrueOrderByNombreAsc();

    List<ServicioAlojamiento> findAllByIdInAndActivoTrue(Iterable<UUID> ids);
}
