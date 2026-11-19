package softplasty.marketplace.modules.reports.api;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record ExportarReporteRequest(@NotNull FormatoReporte formato,
                                     LocalDate desde, LocalDate hasta,
                                     UUID alojamientoId) {
    public enum FormatoReporte { PDF, CSV }
}
