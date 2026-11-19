package softplasty.marketplace.modules.reports.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record ReservaSimulada(UUID alojamientoId, LocalDate checkIn, Estado estado,
                       BigDecimal valorBruto, BigDecimal tarifa) {
    enum Estado { CONFIRMADA, COMPLETADA, CANCELADA, PENDIENTE_PAGO, FALLIDA, EXPIRADA }
    BigDecimal neto() { return valorBruto.subtract(tarifa); }
    boolean cuentaComoIngreso() { return estado == Estado.CONFIRMADA || estado == Estado.COMPLETADA; }
}
