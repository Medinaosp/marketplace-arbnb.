package softplasty.marketplace.modules.accommodations.api;

import java.util.List;
import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.PoliticaCancelacion;

public record CatalogoPoliticaResponse(UUID id, String nombre, List<String> reglas) {
    public static CatalogoPoliticaResponse desde(PoliticaCancelacion politica) {
        return new CatalogoPoliticaResponse(politica.getId(), politica.getNombre(),
                List.of(politica.getReglas()));
    }
}
