package softplasty.marketplace.modules.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InicioSesionRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(max = 64) String contrasena
) {
}
