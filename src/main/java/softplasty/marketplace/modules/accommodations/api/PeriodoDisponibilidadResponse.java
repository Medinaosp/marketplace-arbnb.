package softplasty.marketplace.modules.accommodations.api;

import java.time.LocalDate;
import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.PeriodoDisponibilidad;

public record PeriodoDisponibilidadResponse(UUID id, LocalDate inicio, LocalDate fin, String tipo) {
    public static PeriodoDisponibilidadResponse desde(PeriodoDisponibilidad periodo) {
        return new PeriodoDisponibilidadResponse(periodo.getId(), periodo.getInicio(), periodo.getFin(),
                periodo.getTipo().name());
    }
}
