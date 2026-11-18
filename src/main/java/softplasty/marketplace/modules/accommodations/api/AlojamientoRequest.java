package softplasty.marketplace.modules.accommodations.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AlojamientoRequest(
        @NotBlank @Size(max = 120) String ciudad,
        @NotBlank @Size(max = 500) String ubicacionTexto,
        @NotBlank @Size(max = 80) String tipo,
        @Min(1) @Positive int capacidadMaxima
) {
}
