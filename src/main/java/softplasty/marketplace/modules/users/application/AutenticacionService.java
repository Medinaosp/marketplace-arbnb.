package softplasty.marketplace.modules.users.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import softplasty.marketplace.config.JwtProperties;
import softplasty.marketplace.modules.users.api.InicioSesionRequest;
import softplasty.marketplace.modules.users.api.RegistroRequest;
import softplasty.marketplace.modules.users.api.SesionResponse;
import softplasty.marketplace.modules.users.api.UsuarioResponse;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                               PoliticaContrasena politicaContrasena, JwtEncoder jwtEncoder,
                               JwtProperties jwtProperties, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (request.rol() == RolUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El rol ADMINISTRADOR solo se crea mediante la semilla protegida.");
        }
        politicaContrasena.validar(request.contrasena());
        String correo = normalizarCorreo(request.correo());
        if (usuarios.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }
        Usuario usuario = new Usuario(request.nombre().trim(), correo,
                passwordEncoder.encode(request.contrasena()), request.rol(), EstadoCuenta.ACTIVA,
                Instant.now(clock));
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public SesionResponse iniciarSesion(InicioSesionRequest request) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(normalizarCorreo(request.correo()))
                .orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos."));
        if (usuario.getEstado() == EstadoCuenta.DESACTIVADA
                || !passwordEncoder.matches(request.contrasena(), usuario.getHashContrasena())) {
            throw new BadCredentialsException("Correo o contraseña incorrectos.");
        }

        Instant ahora = Instant.now(clock);
        Instant vence = ahora.plus(jwtProperties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(vence)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new SesionResponse(token, "Bearer", vence, usuario.getId(), usuario.getRol(), usuario.getEstado());
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
