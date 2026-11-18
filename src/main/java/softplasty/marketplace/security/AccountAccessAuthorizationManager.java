package softplasty.marketplace.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;

@Component
public class AccountAccessAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            return new AuthorizationDecision(false);
        }

        if (principal.estado() == EstadoCuenta.ACTIVA) {
            return new AuthorizationDecision(true);
        }
        if (principal.estado() == EstadoCuenta.RESTRINGIDA) {
            return new AuthorizationDecision(isAllowedForRestrictedAccount(context.getRequest()));
        }
        return new AuthorizationDecision(false);
    }

    private boolean isAllowedForRestrictedAccount(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.GET.matches(request.getMethod()) && "/api/usuarios/me".equals(path)) {
            return true;
        }
        return HttpMethod.PUT.matches(request.getMethod())
                && "/api/usuarios/me/contrasena".equals(path);
    }
}
