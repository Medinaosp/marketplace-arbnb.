package softplasty.marketplace.modules.accommodations.api;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearPublicacionRequest(
        @NotNull UUID alojamientoId,
        @NotNull UUID politicaCancelacionId,
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 5000) String descripcion,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioNoche
) {
}
