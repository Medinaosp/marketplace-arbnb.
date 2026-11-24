package softplasty.marketplace.modules.reservations.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NuevaCotizacionRequest(@NotNull LocalDate checkin, @NotNull LocalDate checkout,
        @Positive int huespedes, List<UUID> serviciosIds, @Size(max=40) String codigoDescuento) { }
