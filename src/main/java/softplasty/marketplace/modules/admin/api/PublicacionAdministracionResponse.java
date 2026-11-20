package softplasty.marketplace.modules.admin.api;

import java.util.UUID;
import softplasty.marketplace.modules.accommodations.api.PublicacionResponse;

public record PublicacionAdministracionResponse(
        PublicacionResponse publicacion,
        UUID anfitrionId,
        String anfitrionNombre,
        String anfitrionCorreo
) {
}
