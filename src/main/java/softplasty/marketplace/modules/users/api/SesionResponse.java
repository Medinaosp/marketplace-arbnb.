package softplasty.marketplace.modules.users.api;

import java.time.Instant;
import java.util.UUID;

import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;

public record SesionResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID usuarioId,
        RolUsuario rol,
        EstadoCuenta estado
) {
}
