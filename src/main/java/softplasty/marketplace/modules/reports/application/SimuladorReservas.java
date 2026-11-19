package softplasty.marketplace.modules.reports.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import softplasty.marketplace.modules.accommodations.domain.Alojamiento;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;

/** Fixed, repeatable chart data. Never inserts reservation rows into the database. */
@Component
@Profile({"dev", "test", "demo"})
public class SimuladorReservas {
    private final BigDecimal tarifaFicticia;

    public SimuladorReservas(@Value("${app.reports.simulation.tarifa-ficticia:0.08}") BigDecimal tarifaFicticia) {
        this.tarifaFicticia = tarifaFicticia;
        if (tarifaFicticia.signum() < 0 || tarifaFicticia.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La tarifa ficticia debe estar entre 0 y 1.");
        }
    }

    List<ReservaSimulada> generar(List<Alojamiento> alojamientos, YearMonth desde, YearMonth hasta) {
        List<ReservaSimulada> result = new ArrayList<>();
        for (Alojamiento alojamiento : alojamientos) {
            if (alojamiento.getPublicacion() == null
                    || alojamiento.getPublicacion().getEstado() != EstadoPublicacion.ACTIVA
                    || alojamiento.getAnfitrion().getEstado() != EstadoCuenta.ACTIVA) {
                continue;
            }
            for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
                result.addAll(generar(alojamiento, mes));
            }
        }
        return result;
    }

    private List<ReservaSimulada> generar(Alojamiento alojamiento, YearMonth mes) {
        UUID id = alojamiento.getId();
        long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits()
                ^ ((long) mes.getYear() * 100 + mes.getMonthValue()) ^ 0x5A17C0DEL;
        Random random = new Random(seed);
        int total = 3 + random.nextInt(5);
        BigDecimal nightly = alojamiento.getPublicacion() == null
                ? new BigDecimal("125000") : alojamiento.getPublicacion().getPrecioNoche();
        List<ReservaSimulada> month = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int day = 1 + random.nextInt(mes.lengthOfMonth());
            ReservaSimulada.Estado status = switch (random.nextInt(10)) {
                case 0 -> ReservaSimulada.Estado.CANCELADA;
                case 1 -> ReservaSimulada.Estado.PENDIENTE_PAGO;
                case 2 -> ReservaSimulada.Estado.FALLIDA;
                case 3 -> ReservaSimulada.Estado.EXPIRADA;
                case 4, 5 -> ReservaSimulada.Estado.COMPLETADA;
                default -> ReservaSimulada.Estado.CONFIRMADA;
            };
            BigDecimal gross = nightly.multiply(BigDecimal.valueOf(1 + random.nextInt(5)));
            BigDecimal fee = gross.multiply(tarifaFicticia).setScale(0, RoundingMode.HALF_UP);
            month.add(new ReservaSimulada(id, mes.atDay(day), status, gross, fee));
        }
        return month;
    }
}
