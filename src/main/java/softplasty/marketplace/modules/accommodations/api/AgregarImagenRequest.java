package softplasty.marketplace.modules.accommodations.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgregarImagenRequest(
        @NotBlank @Size(max = 2048) String referencia,
        @Min(1) int orden,
        @NotBlank @Size(max = 300) String textoAlternativo
) {
}
