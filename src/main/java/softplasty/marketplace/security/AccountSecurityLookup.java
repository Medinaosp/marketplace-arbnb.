package softplasty.marketplace.security;

import java.util.Optional;
import java.util.UUID;

public interface AccountSecurityLookup {
    Optional<AccountPrincipal> findById(UUID accountId);
}
