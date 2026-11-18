package softplasty.marketplace.config;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String issuer, String secretBase64, Duration accessTokenTtl) {

    public SecretKey signingKey() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "Falta APP_JWT_SECRET. Configura una clave HMAC aleatoria codificada en Base64."
            );
        }

        final byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_JWT_SECRET debe estar codificada en Base64.", exception);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET debe contener al menos 32 bytes aleatorios.");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
