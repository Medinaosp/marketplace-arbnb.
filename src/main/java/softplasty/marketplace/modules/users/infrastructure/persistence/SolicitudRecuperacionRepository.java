package softplasty.marketplace.modules.users.infrastructure.persistence;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import softplasty.marketplace.modules.users.domain.SolicitudRecuperacion;

public interface SolicitudRecuperacionRepository extends JpaRepository<SolicitudRecuperacion, UUID> {

    Optional<SolicitudRecuperacion> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudRecuperacion s where s.token = :token")
    Optional<SolicitudRecuperacion> findByTokenForUpdate(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SolicitudRecuperacion> findAllByUsuario_IdAndUsadaFalse(UUID usuarioId);
}
