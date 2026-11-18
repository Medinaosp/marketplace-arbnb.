package softplasty.marketplace.modules.accommodations.api;

import jakarta.validation.constraints.Min;

public record CambiarOrdenImagenRequest(@Min(1) int orden) {
}
