package softplasty.marketplace.modules.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import softplasty.marketplace.modules.users.domain.RolUsuario;

public record RegistroRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(min = 8, max = 64) String contrasena,
        @NotNull RolUsuario rol
) {
}
