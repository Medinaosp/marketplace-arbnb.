package softplasty.marketplace.modules.users.api;

import java.util.UUID;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.users.application.PerfilUsuarioService;
import softplasty.marketplace.modules.users.application.RestablecimientoContrasenaService;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilUsuarioService perfilUsuarioService;
    private final RestablecimientoContrasenaService restablecimientoService;

    public UsuarioController(PerfilUsuarioService perfilUsuarioService,
                             RestablecimientoContrasenaService restablecimientoService) {
        this.perfilUsuarioService = perfilUsuarioService;
        this.restablecimientoService = restablecimientoService;
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse consultarPerfil(@AuthenticationPrincipal AccountPrincipal principal) {
        return perfilUsuarioService.consultar(principal.id());
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal AccountPrincipal principal,
                                            @Valid @RequestBody ActualizarPerfilRequest request) {
        return perfilUsuarioService.actualizar(principal.id(), request);
    }

    @PutMapping("/me/contrasena")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@AuthenticationPrincipal AccountPrincipal principal,
                                  @Valid @RequestBody CambiarContrasenaRequest request) {
        perfilUsuarioService.cambiarContrasena(principal.id(), request);
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarCuenta(@AuthenticationPrincipal AccountPrincipal principal) {
        perfilUsuarioService.desactivar(principal.id());
    }

    @PostMapping("/restablecimientos/canjear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void canjearRestablecimiento(@Valid @RequestBody CanjearRestablecimientoRequest request) {
        restablecimientoService.canjearToken(request.token(), request.contrasenaNueva());
    }

    @PostMapping("/restablecimientos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarRestablecimiento(@Valid @RequestBody SolicitarRestablecimientoRequest request) {
        restablecimientoService.solicitarRestablecimiento(request);
    }
}
