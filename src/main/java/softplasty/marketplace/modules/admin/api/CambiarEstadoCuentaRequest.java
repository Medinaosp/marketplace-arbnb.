package softplasty.marketplace.modules.admin.api;

import jakarta.validation.constraints.NotNull;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;

public record CambiarEstadoCuentaRequest(@NotNull EstadoCuenta estado) {
}
