package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.accommodations.domain.PoliticaCancelacion;

public interface PoliticaCancelacionRepository extends JpaRepository<PoliticaCancelacion, UUID> {

    List<PoliticaCancelacion> findAllByOrderByNombreAsc();
}
