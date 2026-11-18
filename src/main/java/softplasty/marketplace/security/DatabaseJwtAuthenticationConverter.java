package softplasty.marketplace.security;

import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AccountSecurityLookup accountSecurityLookup;

    public DatabaseJwtAuthenticationConverter(AccountSecurityLookup accountSecurityLookup) {
        this.accountSecurityLookup = accountSecurityLookup;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID accountId;
        try {
            accountId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidBearerTokenException("El token no contiene un identificador de cuenta válido.");
        }

        AccountPrincipal principal = accountSecurityLookup.findById(accountId)
                .orElseThrow(() -> new InvalidBearerTokenException("La cuenta del token no existe."));
        if (principal.estado() == softplasty.marketplace.modules.users.domain.EstadoCuenta.DESACTIVADA) {
            throw new InvalidBearerTokenException("La cuenta está desactivada.");
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + principal.rol().name()),
                new SimpleGrantedAuthority("ACCOUNT_" + principal.estado().name())
        );
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
