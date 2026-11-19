package softplasty.marketplace.modules.reports.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportesResponse {
    private ReportesResponse() {}

    public record Admin(LocalDate desde, LocalDate hasta, long usuariosTotales,
                        Map<String, Long> usuariosPorRol, Map<String, Long> cuentasPorEstado,
                        List<MesUsuarios> nuevosUsuariosPorMes,
                        Map<String, Long> publicacionesPorEstado,
                        boolean reservasSimuladas, String notaReservas,
                        List<MesReservasAdmin> reservasPorMes) {}

    public record MesUsuarios(YearMonth mes, long nuevos, Map<String, Long> porRol) {}
    public record MesReservasAdmin(YearMonth mes, long reservas, BigDecimal valorBruto) {}

    public record Host(LocalDate desde, LocalDate hasta, boolean reservasSimuladas,
                       String notaReservas, long reservasConfirmadasYCompletadas,
                       long reservasCanceladas, BigDecimal valorBruto,
                       BigDecimal tarifaFicticia, BigDecimal gananciaNetaEstimada,
                       List<MesReservasHost> reservasPorMes,
                       List<AlojamientoHost> porAlojamiento) {}

    public record MesReservasHost(YearMonth mes, long confirmadasYCompletadas,
                                  long canceladas, BigDecimal valorBruto,
                                  BigDecimal tarifaFicticia, BigDecimal gananciaNetaEstimada) {}

    public record AlojamientoHost(UUID alojamientoId, String titulo, long confirmadasYCompletadas,
                                  long canceladas, BigDecimal valorBruto,
                                  BigDecimal tarifaFicticia, BigDecimal gananciaNetaEstimada) {}

    public record Evento(UUID id, UUID actorId, String accion, String entidad, UUID entidadId,
                         String resultado, java.time.Instant ocurridoEn) {}
    public record Exportacion(UUID id, String tipo, String formato, String filtros,
                              java.time.Instant generadoEn) {}
}
