package softplasty.marketplace.modules.users.api;

import java.time.Instant;
import java.util.UUID;

import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;

public record UsuarioResponse(
        UUID id,
        String nombre,
        String correo,
        RolUsuario rol,
        EstadoCuenta estado,
        Instant creadoEn
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getEstado(), usuario.getCreadoEn());
    }
}
