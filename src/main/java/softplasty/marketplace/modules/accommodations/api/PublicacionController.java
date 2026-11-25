package softplasty.marketplace.modules.accommodations.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import softplasty.marketplace.modules.accommodations.application.PublicacionService;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @GetMapping
    public List<PublicacionResponse> buscar(@RequestParam String ciudad) {
        return publicacionService.buscarPublicas(ciudad);
    }

    @GetMapping("/{publicacionId}")
    public PublicacionResponse consultarPublica(@PathVariable UUID publicacionId) {
        return publicacionService.consultarPublica(publicacionId);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public List<PublicacionResponse> listarPropias(@AuthenticationPrincipal AccountPrincipal principal) {
        return publicacionService.listarPropias(principal.id());
    }

    @GetMapping("/mias/{publicacionId}")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse consultarPropia(@AuthenticationPrincipal AccountPrincipal principal,
                                               @PathVariable UUID publicacionId) {
        return publicacionService.consultarPropia(principal.id(), publicacionId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse crear(@AuthenticationPrincipal AccountPrincipal principal,
                                     @Valid @RequestBody CrearPublicacionRequest request) {
        return publicacionService.crear(principal.id(), request);
    }

    @PutMapping("/{publicacionId}")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse actualizarFicha(@AuthenticationPrincipal AccountPrincipal principal,
                                                @PathVariable UUID publicacionId,
                                                @Valid @RequestBody ActualizarPublicacionRequest request) {
        return publicacionService.actualizarFicha(principal.id(), publicacionId, request);
    }

    @PostMapping("/{publicacionId}/imagenes")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse agregarImagen(@AuthenticationPrincipal AccountPrincipal principal,
                                             @PathVariable UUID publicacionId,
                                             @Valid @RequestBody AgregarImagenRequest request) {
        return publicacionService.agregarImagen(principal.id(), publicacionId, request);
    }

    @DeleteMapping("/{publicacionId}/imagenes/{imagenId}")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse eliminarImagen(@AuthenticationPrincipal AccountPrincipal principal,
                                              @PathVariable UUID publicacionId,
                                              @PathVariable UUID imagenId) {
        return publicacionService.eliminarImagen(principal.id(), publicacionId, imagenId);
    }

    @PostMapping(value = "/{publicacionId}/imagenes/archivo", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ANFITRION')")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse subirImagen(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID publicacionId,
                                           @RequestParam("archivo") MultipartFile archivo,
                                           @RequestParam(value = "textoAlternativo", required = false) String texto) throws IOException {
        return publicacionService.subirImagen(principal.id(), publicacionId, archivo.getBytes(), archivo.getContentType(), texto);
    }

    @PatchMapping("/{publicacionId}/imagenes/{imagenId}/orden")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse cambiarOrden(@AuthenticationPrincipal AccountPrincipal principal,
                                           @PathVariable UUID publicacionId,
                                           @PathVariable UUID imagenId,
                                           @Valid @RequestBody CambiarOrdenImagenRequest request) {
        return publicacionService.cambiarOrdenImagen(principal.id(), publicacionId,
                imagenId, request.orden());
    }

    @PutMapping("/{publicacionId}/servicios")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse actualizarServicios(@AuthenticationPrincipal AccountPrincipal principal,
                                                    @PathVariable UUID publicacionId,
                                                    @Valid @RequestBody ActualizarServiciosRequest request) {
        return publicacionService.actualizarServicios(principal.id(), publicacionId,
                request.servicioIds());
    }

    @PostMapping("/{publicacionId}/enviar-revision")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse enviarARevision(@AuthenticationPrincipal AccountPrincipal principal,
                                              @PathVariable UUID publicacionId) {
        return publicacionService.enviarARevision(principal.id(), publicacionId);
    }

    @PostMapping("/{publicacionId}/pausar")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse pausar(@AuthenticationPrincipal AccountPrincipal principal,
                                     @PathVariable UUID publicacionId) {
        return publicacionService.pausar(principal.id(), publicacionId);
    }

    @PostMapping("/{publicacionId}/activar")
    @PreAuthorize("hasRole('ANFITRION')")
    @SecurityRequirement(name = "bearerAuth")
    public PublicacionResponse activar(@AuthenticationPrincipal AccountPrincipal principal,
                                      @PathVariable UUID publicacionId) {
        return publicacionService.activar(principal.id(), publicacionId);
    }
}
