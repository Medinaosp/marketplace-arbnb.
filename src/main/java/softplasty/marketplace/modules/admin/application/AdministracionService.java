package softplasty.marketplace.modules.admin.application;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.api.PublicacionResponse;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.ImagenPublicacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionServicioRepository;
import softplasty.marketplace.modules.admin.api.PublicacionAdministracionResponse;
import softplasty.marketplace.modules.reports.application.EventoAuditoriaService;
import softplasty.marketplace.modules.users.api.UsuarioResponse;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class AdministracionService {
    private final UsuarioRepository usuarios;
    private final PublicacionRepository publicaciones;
    private final ImagenPublicacionRepository imagenes;
    private final PublicacionServicioRepository servicios;
    private final EventoAuditoriaService auditoria;

    public AdministracionService(UsuarioRepository usuarios,
            PublicacionRepository publicaciones,
            ImagenPublicacionRepository imagenes,
            PublicacionServicioRepository servicios,
            EventoAuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.publicaciones = publicaciones;
        this.imagenes = imagenes;
        this.servicios = servicios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PublicacionAdministracionResponse> publicaciones(EstadoPublicacion estado) {
        List<Publicacion> filas = estado == null
                ? publicaciones.findAllByOrderByTituloAsc()
                : publicaciones.findAllByEstadoOrderByTituloAsc(estado);
        return filas.stream().map(this::publicacion).toList();
    }

    @Transactional(readOnly = true)
    public PublicacionAdministracionResponse publicacion(UUID id) {
        return publicaciones.findById(id).map(this::publicacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La publicación no existe."));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> usuarios(RolUsuario rol, EstadoCuenta estado) {
        return usuarios.findAllByOrderByCreadoEnDesc().stream()
                .filter(usuario -> usuario.getRol() != RolUsuario.ADMINISTRADOR)
                .filter(usuario -> rol == null || usuario.getRol() == rol)
                .filter(usuario -> estado == null || usuario.getEstado() == estado)
                .map(UsuarioResponse::desde)
                .toList();
    }

    @Transactional
    public UsuarioResponse cambiarEstadoCuenta(UUID administradorId, UUID usuarioId, EstadoCuenta nuevoEstado) {
        Usuario administrador = usuarios.findById(administradorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Se requiere una cuenta administradora activa."));
        if (administrador.getRol() != RolUsuario.ADMINISTRADOR
                || administrador.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere una cuenta administradora activa.");
        }
        if (nuevoEstado != EstadoCuenta.ACTIVA && nuevoEstado != EstadoCuenta.RESTRINGIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El administrador solo puede restringir o rehabilitar cuentas.");
        }
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La cuenta no existe."));
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No se pueden cambiar cuentas administradoras desde este panel.");
        }
        if (usuario.getEstado() == nuevoEstado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta ya tiene ese estado.");
        }
        if (nuevoEstado == EstadoCuenta.RESTRINGIDA) {
            usuario.restringir();
        } else {
            if (usuario.getEstado() != EstadoCuenta.RESTRINGIDA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Solo se pueden rehabilitar cuentas restringidas.");
            }
            usuario.activar();
        }
        auditoria.registrar(administradorId,
                nuevoEstado == EstadoCuenta.RESTRINGIDA ? "CUENTA_RESTRINGIDA" : "CUENTA_REHABILITADA",
                "USUARIO", usuarioId);
        return UsuarioResponse.desde(usuario);
    }

    private PublicacionAdministracionResponse publicacion(Publicacion publicacion) {
        var respuesta = PublicacionResponse.desde(publicacion,
                imagenes.findAllByPublicacion_IdOrderByOrdenAsc(publicacion.getId()),
                servicios.findAllByPublicacion_Id(publicacion.getId()));
        Usuario anfitrion = publicacion.getAlojamiento().getAnfitrion();
        return new PublicacionAdministracionResponse(respuesta, anfitrion.getId(),
                anfitrion.getNombre(), anfitrion.getCorreo());
    }
}
