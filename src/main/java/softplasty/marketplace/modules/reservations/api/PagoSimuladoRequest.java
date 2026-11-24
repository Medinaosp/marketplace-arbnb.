package softplasty.marketplace.modules.reservations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PagoSimuladoRequest(@NotBlank @Pattern(regexp="APROBADO|RECHAZADO") String resultado) { }
