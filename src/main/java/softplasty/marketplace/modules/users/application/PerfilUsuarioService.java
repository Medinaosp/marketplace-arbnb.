package softplasty.marketplace.modules.users.application;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import softplasty.marketplace.modules.users.api.ActualizarPerfilRequest;
import softplasty.marketplace.modules.users.api.CambiarContrasenaRequest;
import softplasty.marketplace.modules.users.api.UsuarioResponse;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class PerfilUsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;

    public PerfilUsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                PoliticaContrasena politicaContrasena) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultar(UUID usuarioId) {
        return UsuarioResponse.desde(obtener(usuarioId));
    }

    @Transactional
    public UsuarioResponse actualizar(UUID usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = obtener(usuarioId);
        String correo = request.correo().trim().toLowerCase(java.util.Locale.ROOT);
        usuarios.findByCorreoIgnoreCase(correo)
                .filter(otro -> !otro.getId().equals(usuarioId))
                .ifPresent(otro -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El correo ya está registrado."); });
        usuario.actualizarPerfil(request.nombre().trim(), correo);
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public void cambiarContrasena(UUID usuarioId, CambiarContrasenaRequest request) {
        Usuario usuario = obtener(usuarioId);
        if (!passwordEncoder.matches(request.contrasenaActual(), usuario.getHashContrasena())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }
        politicaContrasena.validar(request.contrasenaNueva());
        usuario.cambiarHashContrasena(passwordEncoder.encode(request.contrasenaNueva()));
    }

    @Transactional
    public void desactivar(UUID usuarioId) {
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe."));
        usuario.desactivar();
    }

    private Usuario obtener(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe."));
    }
}
