package softplasty.marketplace.modules.reservations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarReservaRequest(@NotBlank @Size(max=500) String motivo) { }
