package softplasty.marketplace.modules.users.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario> findAllByOrderByCreadoEnDesc();

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByRol(RolUsuario rol);

    long countByRol(RolUsuario rol);

    long countByEstado(softplasty.marketplace.modules.users.domain.EstadoCuenta estado);

    long countByRolAndEstado(RolUsuario rol,
            softplasty.marketplace.modules.users.domain.EstadoCuenta estado);

    long countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(Instant desde, Instant hasta);

    long countByRolAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(
            RolUsuario rol, Instant desde, Instant hasta);

    long countByRolAndEstadoAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(
            RolUsuario rol, softplasty.marketplace.modules.users.domain.EstadoCuenta estado,
            Instant desde, Instant hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") UUID id);
}
