package softplasty.marketplace.config;

import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class RuntimeConfigurationValidator implements EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeConfigurationValidator.class);
    private static final ZoneId EXPECTED_TIME_ZONE = ZoneId.of("America/Bogota");
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Bean
    ApplicationRunner verifyRuntimeTimeZone() {
        return args -> {
            ZoneId systemZone = ZoneId.systemDefault();
            if (systemZone.equals(EXPECTED_TIME_ZONE)) {
                LOGGER.info("Zona horaria del sistema verificada: {}", systemZone);
                return;
            }

            String message = "La zona horaria del sistema es " + systemZone
                    + "; el proyecto requiere " + EXPECTED_TIME_ZONE + ".";
            if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                throw new IllegalStateException(message);
            }
            LOGGER.warn("{} Configura el sistema de desarrollo como America/Bogota.", message);
        };
    }
}
