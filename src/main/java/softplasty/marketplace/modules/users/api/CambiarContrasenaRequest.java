package softplasty.marketplace.modules.users.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaRequest(
        @NotBlank @Size(max = 64) String contrasenaActual,
        @NotBlank @Size(min = 8, max = 64) String contrasenaNueva
) {
}
