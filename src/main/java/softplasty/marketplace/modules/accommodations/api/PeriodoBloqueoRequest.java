package softplasty.marketplace.modules.accommodations.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record PeriodoBloqueoRequest(
        @NotNull LocalDate inicio,
        @NotNull LocalDate fin
) {
}
