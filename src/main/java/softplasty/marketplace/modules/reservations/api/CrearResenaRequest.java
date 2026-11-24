package softplasty.marketplace.modules.reservations.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearResenaRequest(@Min(1) @Max(5) int calificacion,
        @NotBlank @Size(max=2000) String contenido) { }
