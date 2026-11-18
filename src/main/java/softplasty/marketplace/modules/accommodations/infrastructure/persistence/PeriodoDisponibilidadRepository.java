package softplasty.marketplace.modules.accommodations.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import softplasty.marketplace.modules.accommodations.domain.PeriodoDisponibilidad;
import softplasty.marketplace.modules.accommodations.domain.TipoPeriodo;

public interface PeriodoDisponibilidadRepository extends JpaRepository<PeriodoDisponibilidad, UUID> {

    List<PeriodoDisponibilidad> findAllByAlojamiento_IdAndTipoOrderByInicioAsc(
            UUID alojamientoId, TipoPeriodo tipo);

    boolean existsByAlojamiento_IdAndTipoAndInicioLessThanAndFinGreaterThan(
            UUID alojamientoId, TipoPeriodo tipo, LocalDate finExclusivo, LocalDate inicioExclusivo);
}
