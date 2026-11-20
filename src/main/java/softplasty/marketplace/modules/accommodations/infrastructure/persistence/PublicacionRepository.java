package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;

public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    Optional<Publicacion> findByAlojamiento_Anfitrion_IdAndId(UUID anfitrionId, UUID publicacionId);

    Optional<Publicacion> findByAlojamiento_Id(UUID alojamientoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Publicacion p where p.id = :id")
    Optional<Publicacion> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Publicacion p where p.id = :id and p.alojamiento.anfitrion.id = :ownerId")
    Optional<Publicacion> findOwnedByIdForUpdate(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

    List<Publicacion> findAllByAlojamiento_Anfitrion_IdOrderByTituloAsc(UUID anfitrionId);

    List<Publicacion> findAllByOrderByTituloAsc();

    List<Publicacion> findAllByEstadoOrderByTituloAsc(EstadoPublicacion estado);

    List<Publicacion> findAllByEstadoAndAlojamiento_Anfitrion_EstadoOrderByTituloAsc(
            EstadoPublicacion estado, softplasty.marketplace.modules.users.domain.EstadoCuenta estadoAnfitrion);

    List<Publicacion> findAllByEstadoAndAlojamiento_CiudadIgnoreCaseAndAlojamiento_Anfitrion_EstadoOrderByTituloAsc(
            EstadoPublicacion estado, String ciudad,
            softplasty.marketplace.modules.users.domain.EstadoCuenta estadoAnfitrion);

    Optional<Publicacion> findByIdAndEstadoAndAlojamiento_Anfitrion_Estado(
            UUID id, EstadoPublicacion estado,
            softplasty.marketplace.modules.users.domain.EstadoCuenta estadoAnfitrion);

    long countByEstado(EstadoPublicacion estado);

    long countByEstadoAndAlojamiento_CiudadIgnoreCase(EstadoPublicacion estado, String ciudad);
}
