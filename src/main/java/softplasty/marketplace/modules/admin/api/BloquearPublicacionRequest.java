package softplasty.marketplace.modules.admin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloquearPublicacionRequest(
        @NotBlank @Size(max = 500) String motivo
) {
}
