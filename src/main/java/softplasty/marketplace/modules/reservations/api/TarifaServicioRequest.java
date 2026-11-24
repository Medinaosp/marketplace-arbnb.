package softplasty.marketplace.modules.reservations.api;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TarifaServicioRequest(@NotNull @DecimalMin("0.00") BigDecimal monto,
        @NotBlank String modalidad) { }
