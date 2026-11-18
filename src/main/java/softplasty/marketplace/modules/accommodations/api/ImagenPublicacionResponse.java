package softplasty.marketplace.modules.accommodations.api;

import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.ImagenPublicacion;

public record ImagenPublicacionResponse(UUID id, String referencia, int orden, String textoAlternativo) {
    public static ImagenPublicacionResponse desde(ImagenPublicacion imagen) {
        return new ImagenPublicacionResponse(imagen.getId(), imagen.getReferencia(),
                imagen.getOrden(), imagen.getTextoAlternativo());
    }
}
