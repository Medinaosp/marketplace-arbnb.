package softplasty.marketplace.modules.users.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softplasty.marketplace.modules.users.api.SolicitarRestablecimientoRequest;
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
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private static final Logger log = LoggerFactory.getLogger(RestablecimientoContrasenaService.class);

    private final UsuarioRepository usuarios;
    private final SolicitudRecuperacionRepository solicitudes;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final Clock clock;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String senderAddress;
    private final SecureRandom secureRandom = new SecureRandom();

    public RestablecimientoContrasenaService(UsuarioRepository usuarios,
                                             SolicitudRecuperacionRepository solicitudes,
                                             PasswordEncoder passwordEncoder,
                                             PoliticaContrasena politicaContrasena,
                                             Clock clock,
                                             JavaMailSender mailSender,
                                             @Value("${app.password-reset.frontend-url}") String frontendUrl,
                                             @Value("${app.password-reset.from}") String senderAddress) {
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.clock = clock;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.senderAddress = senderAddress;
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

        Usuario usuarioBloqueado = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La cuenta solicitada no existe."));
        return crearToken(usuarioBloqueado, Instant.now(clock));
    }

    /** Public request always returns the same response to avoid account enumeration. */
    @Transactional
    public void solicitarRestablecimiento(SolicitarRestablecimientoRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        usuarios.findByCorreoIgnoreCase(correo).ifPresent(usuarioEncontrado -> {
            Usuario usuario = usuarios.findByIdForUpdate(usuarioEncontrado.getId()).orElse(null);
            if (usuario == null || usuario.getEstado() != EstadoCuenta.ACTIVA
                    || usuario.getRol() == RolUsuario.ADMINISTRADOR) {
                return;
            }

            Instant ahora = Instant.now(clock);
            Instant ultimaSolicitud = solicitudes
                    .findFirstByUsuario_IdOrderBySolicitadoEnDesc(usuario.getId())
                    .map(SolicitudRecuperacion::getSolicitadoEn)
                    .orElse(Instant.MIN);
            if (ultimaSolicitud.plus(REQUEST_COOLDOWN).isAfter(ahora)) {
                return;
            }

            String token = crearToken(usuario, ahora);
            enviarEnlace(usuario.getCorreo(), token);
        });
    }

    private String crearToken(Usuario usuario, Instant solicitadoEn) {
        solicitudes.findAllByUsuario_IdAndUsadaFalse(usuario.getId())
                .forEach(SolicitudRecuperacion::marcarUsada);
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String tokenClaro = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        solicitudes.save(new SolicitudRecuperacion(usuario, hashToken(tokenClaro), solicitadoEn,
                solicitadoEn.plus(TOKEN_TTL), false));
        return tokenClaro;
    }

    private void enviarEnlace(String correo, String token) {
        String link = frontendUrl.replaceAll("/+$", "")
                + "/recuperar-contrasena#token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(correo);
        message.setSubject("Restablece tu contraseña de Patio");
        message.setText("Recibimos una solicitud para cambiar la contraseña de tu cuenta de Patio.\n\n"
                + "Abre este enlace para elegir una contraseña nueva (vence en 30 minutos y se puede usar una sola vez):\n"
                + link + "\n\nSi no solicitaste este cambio, ignora este mensaje. Tu contraseña actual seguirá vigente.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // Do not expose whether an address exists or include its value in application logs.
            Throwable cause = exception.getMostSpecificCause();
            log.warn("No fue posible entregar un correo de restablecimiento. Fallo: {}; causa: {}.",
                    exception.getClass().getSimpleName(), cause.getClass().getSimpleName());
        }
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
