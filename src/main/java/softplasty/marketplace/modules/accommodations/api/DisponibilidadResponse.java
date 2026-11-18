package softplasty.marketplace.modules.accommodations.api;

import java.time.LocalDate;
import java.util.UUID;

public record DisponibilidadResponse(UUID alojamientoId, LocalDate inicio, LocalDate fin, boolean disponible) {
}
