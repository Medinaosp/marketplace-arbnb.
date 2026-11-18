package softplasty.marketplace.modules.users.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.security.AccountPrincipal;
import softplasty.marketplace.security.AccountSecurityLookup;

@Repository
public class JdbcAccountSecurityLookup implements AccountSecurityLookup {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountSecurityLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AccountPrincipal> findById(UUID accountId) {
        List<AccountPrincipal> accounts = jdbcTemplate.query(
                "SELECT rol, estado FROM usuario WHERE id = ?",
                (resultSet, rowNumber) -> new AccountPrincipal(
                        accountId,
                        RolUsuario.valueOf(resultSet.getString("rol")),
                        EstadoCuenta.valueOf(resultSet.getString("estado"))
                ),
                accountId
        );
        return accounts.stream().findFirst();
    }
}
