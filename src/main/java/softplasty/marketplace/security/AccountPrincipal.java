package softplasty.marketplace.security;

import java.util.UUID;

import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;

public record AccountPrincipal(UUID id, RolUsuario rol, EstadoCuenta estado) {
}
