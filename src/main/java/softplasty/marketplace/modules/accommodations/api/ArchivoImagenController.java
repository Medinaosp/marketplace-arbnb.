package softplasty.marketplace.modules.accommodations.api;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.accommodations.application.PublicacionService;

@RestController
public class ArchivoImagenController {
    private final PublicacionService service;
    public ArchivoImagenController(PublicacionService service) { this.service = service; }

    @GetMapping("/api/imagenes/{id}")
    public ResponseEntity<byte[]> obtener(@PathVariable UUID id) {
        var imagen = service.obtenerArchivo(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagen.getTipoContenido()))
                .header("X-Content-Type-Options", "nosniff").body(imagen.getContenido());
    }
}
