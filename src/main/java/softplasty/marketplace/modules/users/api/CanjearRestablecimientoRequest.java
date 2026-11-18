package softplasty.marketplace.modules.users.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CanjearRestablecimientoRequest(
        @NotBlank @Size(min = 40, max = 80) String token,
        @NotBlank @Size(min = 8, max = 64) String contrasenaNueva
) {
}
