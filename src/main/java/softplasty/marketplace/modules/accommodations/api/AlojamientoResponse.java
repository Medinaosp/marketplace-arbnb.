package softplasty.marketplace.modules.accommodations.api;

import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.Alojamiento;

public record AlojamientoResponse(
        UUID id,
        UUID anfitrionId,
        String ciudad,
        String ubicacionTexto,
        String tipo,
        int capacidadMaxima
) {
    public static AlojamientoResponse desde(Alojamiento alojamiento) {
        return new AlojamientoResponse(alojamiento.getId(), alojamiento.getAnfitrion().getId(),
                alojamiento.getCiudad(), alojamiento.getUbicacionTexto(), alojamiento.getTipo(),
                alojamiento.getCapacidadMaxima());
    }
}
