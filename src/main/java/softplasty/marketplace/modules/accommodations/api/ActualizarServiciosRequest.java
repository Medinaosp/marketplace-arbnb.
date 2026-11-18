package softplasty.marketplace.modules.accommodations.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarServiciosRequest(
        @NotNull @Size(max = 100) List<@NotNull UUID> servicioIds
) {
}
