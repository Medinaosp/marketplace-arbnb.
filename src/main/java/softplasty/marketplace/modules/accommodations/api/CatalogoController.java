package softplasty.marketplace.modules.accommodations.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PoliticaCancelacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.ServicioAlojamientoRepository;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final ServicioAlojamientoRepository servicios;
    private final PoliticaCancelacionRepository politicas;

    public CatalogoController(ServicioAlojamientoRepository servicios,
                              PoliticaCancelacionRepository politicas) {
        this.servicios = servicios;
        this.politicas = politicas;
    }

    @GetMapping("/servicios")
    public List<CatalogoServicioResponse> servicios() {
        return servicios.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(CatalogoServicioResponse::desde).toList();
    }

    @GetMapping("/politicas-cancelacion")
    public List<CatalogoPoliticaResponse> politicas() {
        return politicas.findAllByOrderByNombreAsc().stream()
                .map(CatalogoPoliticaResponse::desde).toList();
    }
}
