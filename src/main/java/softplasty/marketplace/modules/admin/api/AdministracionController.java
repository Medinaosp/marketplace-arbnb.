package softplasty.marketplace.modules.admin.api;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.accommodations.application.ModeracionPublicacionService;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.admin.application.AdministracionService;
import softplasty.marketplace.modules.users.api.UsuarioResponse;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
@RequestMapping("/api/administracion")
@PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('ACCOUNT_ACTIVA')")
@SecurityRequirement(name = "bearerAuth")
public class AdministracionController {
    private final AdministracionService administracion;
    private final ModeracionPublicacionService moderacion;

    public AdministracionController(AdministracionService administracion,
            ModeracionPublicacionService moderacion) {
        this.administracion = administracion;
        this.moderacion = moderacion;
    }

    @GetMapping("/publicaciones")
    public List<PublicacionAdministracionResponse> publicaciones(
            @RequestParam(required = false) EstadoPublicacion estado) {
        return administracion.publicaciones(estado);
    }

    @PostMapping("/publicaciones/{publicacionId}/aprobar")
    public PublicacionAdministracionResponse aprobar(
            @AuthenticationPrincipal AccountPrincipal administrador,
            @PathVariable UUID publicacionId) {
        moderacion.aprobar(administrador.id(), publicacionId);
        return administracion.publicacion(publicacionId);
    }

    @PostMapping("/publicaciones/{publicacionId}/bloquear")
    public PublicacionAdministracionResponse bloquear(
            @AuthenticationPrincipal AccountPrincipal administrador,
            @PathVariable UUID publicacionId,
            @Valid @RequestBody BloquearPublicacionRequest request) {
        moderacion.bloquear(administrador.id(), publicacionId, request.motivo());
        return administracion.publicacion(publicacionId);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) EstadoCuenta estado) {
        return administracion.usuarios(rol, estado);
    }

    @PatchMapping("/usuarios/{usuarioId}/estado")
    public UsuarioResponse cambiarEstadoCuenta(
            @AuthenticationPrincipal AccountPrincipal administrador,
            @PathVariable UUID usuarioId,
            @Valid @RequestBody CambiarEstadoCuentaRequest request) {
        return administracion.cambiarEstadoCuenta(administrador.id(), usuarioId, request.estado());
    }
}
