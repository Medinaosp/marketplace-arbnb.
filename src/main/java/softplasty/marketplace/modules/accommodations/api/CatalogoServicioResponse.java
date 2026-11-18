package softplasty.marketplace.modules.accommodations.api;

import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.ServicioAlojamiento;

public record CatalogoServicioResponse(UUID id, String codigo, String nombre) {
    public static CatalogoServicioResponse desde(ServicioAlojamiento servicio) {
        return new CatalogoServicioResponse(servicio.getId(), servicio.getCodigo(), servicio.getNombre());
    }
}
