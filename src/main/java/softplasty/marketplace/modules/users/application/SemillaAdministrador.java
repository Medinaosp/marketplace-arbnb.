package softplasty.marketplace.modules.users.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Component
public class SemillaAdministrador implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemillaAdministrador.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final Clock clock;
    private final Environment environment;
    private final String correoConfigurado;
    private final String contrasenaConfigurada;

    public SemillaAdministrador(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                PoliticaContrasena politicaContrasena, Clock clock,
                                Environment environment,
                                @Value("${app.bootstrap.admin.email:}") String correoConfigurado,
                                @Value("${app.bootstrap.admin.password:}") String contrasenaConfigurada) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.clock = clock;
        this.environment = environment;
        this.correoConfigurado = correoConfigurado;
        this.contrasenaConfigurada = contrasenaConfigurada;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByRol(RolUsuario.ADMINISTRADOR)) {
            return;
        }
        if (correoConfigurado.isBlank() || contrasenaConfigurada.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                throw new IllegalStateException(
                        "Faltan ADMIN_EMAIL y ADMIN_PASSWORD para crear el administrador inicial.");
            }
            LOGGER.warn("No se creó la cuenta administradora inicial: faltan sus variables de entorno.");
            return;
        }

        String correo = correoConfigurado.trim().toLowerCase(Locale.ROOT);
        usuarios.findByCorreoIgnoreCase(correo).ifPresent(existing -> {
            throw new IllegalStateException(
                    "El correo configurado para ADMIN_EMAIL ya pertenece a una cuenta que no es administradora.");
        });
        if (contrasenaConfigurada.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 12 caracteres.");
        }
        politicaContrasena.validar(contrasenaConfigurada);

        usuarios.save(new Usuario("Administrador", correo, passwordEncoder.encode(contrasenaConfigurada),
                RolUsuario.ADMINISTRADOR, EstadoCuenta.ACTIVA, Instant.now(clock)));
        LOGGER.info("Se creó la cuenta administradora inicial configurada por ADMIN_EMAIL.");
    }
}
