package softplasty.marketplace.modules.accommodations.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;

class AlojamientoDisponibilidadTest {

    private Alojamiento alojamiento;
    private final LocalDate hoy = LocalDate.of(2026, 11, 24);

    @BeforeEach
    void prepararAlojamiento() {
        Usuario anfitrion = new Usuario("Anfitrión de prueba", "host@example.test", "hash",
                RolUsuario.ANFITRION, EstadoCuenta.ACTIVA, Instant.parse("2026-11-24T12:00:00Z"));
        alojamiento = new Alojamiento(anfitrion, "Bogotá", "Zona norte", "Apartamento", 4);
    }

    @Test
    void unBloqueoImpideSolapesYPermiteRangosContiguos() {
        LocalDate inicioBloqueo = LocalDate.of(2026, 12, 1);
        LocalDate finBloqueo = LocalDate.of(2026, 12, 5);
        alojamiento.bloquearPeriodo(inicioBloqueo, finBloqueo, hoy);

        assertFalse(alojamiento.estaDisponible(LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 7)));
        assertTrue(alojamiento.estaDisponible(LocalDate.of(2026, 11, 29), inicioBloqueo));
        assertTrue(alojamiento.estaDisponible(finBloqueo, LocalDate.of(2026, 12, 7)));
        assertEquals(TipoPeriodo.BLOQUEADO, alojamiento.getPeriodosDisponibilidad().get(0).getTipo());
    }

    @Test
    void noPermiteCrearUnBloqueoSuperpuesto() {
        alojamiento.bloquearPeriodo(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), hoy);

        assertThrows(IllegalStateException.class, () -> alojamiento.bloquearPeriodo(
                LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 8), hoy));
    }

    @Test
    void soloLiberaElRangoExactoYDevuelveDisponibilidad() {
        LocalDate inicio = LocalDate.of(2026, 12, 1);
        LocalDate fin = LocalDate.of(2026, 12, 5);
        alojamiento.bloquearPeriodo(inicio, fin, hoy);

        assertThrows(IllegalStateException.class,
                () -> alojamiento.liberarPeriodo(inicio, LocalDate.of(2026, 12, 6)));
        alojamiento.liberarPeriodo(inicio, fin);

        assertTrue(alojamiento.estaDisponible(inicio, fin));
        assertTrue(alojamiento.getPeriodosDisponibilidad().isEmpty());
    }

    @Test
    void rechazaRangosInvalidosYFechasPasadas() {
        assertThrows(IllegalArgumentException.class, () -> alojamiento.bloquearPeriodo(
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 5), hoy));
        assertThrows(IllegalArgumentException.class, () -> alojamiento.bloquearPeriodo(
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25), hoy));
    }
}
