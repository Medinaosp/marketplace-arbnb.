package softplasty.marketplace.modules.users.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.SolicitudRecuperacion;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.SolicitudRecuperacionRepository;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

/** Recovery token issuer for component 06 and public one-time token redemption. */
@Service
public class RestablecimientoContrasenaService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UsuarioRepository usuarios;
    private final SolicitudRecuperacionRepository solicitudes;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RestablecimientoContrasenaService(UsuarioRepository usuarios,
                                             SolicitudRecuperacionRepository solicitudes,
                                             PasswordEncoder passwordEncoder,
                                             PoliticaContrasena politicaContrasena,
                                             Clock clock) {
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.clock = clock;
    }

    /** Invoked by component 06; the clear-text token is returned once to its caller. */
    @Transactional
    public String emitirToken(UUID administradorId, UUID usuarioId) {
        Usuario administrador = usuarios.findByIdForUpdate(administradorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Se requiere una cuenta administradora activa."));
        if (administrador.getRol() != RolUsuario.ADMINISTRADOR
                || administrador.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere una cuenta administradora activa.");
        }

        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La cuenta solicitada no existe."));
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El restablecimiento administrativo no aplica a cuentas administradoras.");
        }

        solicitudes.findAllByUsuario_IdAndUsadaFalse(usuarioId)
                .forEach(SolicitudRecuperacion::marcarUsada);

        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String tokenClaro = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant solicitadoEn = Instant.now(clock);
        SolicitudRecuperacion solicitud = new SolicitudRecuperacion(
                usuario, hashToken(tokenClaro), solicitadoEn, solicitadoEn.plus(TOKEN_TTL), false);
        solicitudes.save(solicitud);
        return tokenClaro;
    }

    @Transactional
    public void canjearToken(String tokenClaro, String nuevaContrasena) {
        politicaContrasena.validar(nuevaContrasena);
        String hash = hashToken(tokenClaro);
        SolicitudRecuperacion solicitudLeida = solicitudes.findByToken(hash)
                .orElseThrow(this::tokenInvalido);
        Usuario usuario = usuarios.findByIdForUpdate(solicitudLeida.getUsuario().getId())
                .orElseThrow(this::tokenInvalido);
        SolicitudRecuperacion solicitud = solicitudes.findByTokenForUpdate(hash)
                .orElseThrow(this::tokenInvalido);
        if (!solicitud.getUsuario().getId().equals(usuario.getId())) {
            throw tokenInvalido();
        }
        Instant ahora = Instant.now(clock);
        if (!solicitud.estaVigente(ahora)) {
            throw tokenInvalido();
        }
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw tokenInvalido();
        }

        usuario.cambiarHashContrasena(passwordEncoder.encode(nuevaContrasena));
        solicitud.marcarUsada();
    }

    private ResponseStatusException tokenInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El token de restablecimiento no es válido, ya se usó o venció.");
    }

    private String hashToken(String tokenClaro) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(tokenClaro.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No está disponible SHA-256.", exception);
        }
    }
}
