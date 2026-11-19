package softplasty.marketplace.modules.accommodations.application;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.ImagenPublicacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionRepository;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;
import softplasty.marketplace.modules.reports.application.EventoAuditoriaService;

/** Domain operations for the moderation component; this module exposes no admin HTTP endpoints. */
@Service
public class ModeracionPublicacionService {

    private final UsuarioRepository usuarios;
    private final PublicacionRepository publicaciones;
    private final ImagenPublicacionRepository imagenes;
    private final EventoAuditoriaService auditoria;

    public ModeracionPublicacionService(UsuarioRepository usuarios,
                                        PublicacionRepository publicaciones,
                                        ImagenPublicacionRepository imagenes,
                                        EventoAuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.publicaciones = publicaciones;
        this.imagenes = imagenes;
        this.auditoria = auditoria;
    }

    @Transactional
    public void aprobar(UUID administradorId, UUID publicacionId) {
        validarAdministrador(administradorId);
        Publicacion publicacion = obtenerForUpdate(publicacionId);
        publicacion.aprobar(Math.toIntExact(imagenes.countByPublicacion_Id(publicacionId)));
        auditoria.registrar(administradorId, "PUBLICACION_APROBADA", "PUBLICACION", publicacionId);
    }

    @Transactional
    public void bloquear(UUID administradorId, UUID publicacionId, String motivo) {
        validarAdministrador(administradorId);
        obtenerForUpdate(publicacionId).bloquear(motivo);
        auditoria.registrar(administradorId, "PUBLICACION_BLOQUEADA", "PUBLICACION", publicacionId);
    }

    private Publicacion obtenerForUpdate(UUID publicacionId) {
        return publicaciones.findByIdForUpdate(publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La publicación no existe."));
    }

    private void validarAdministrador(UUID administradorId) {
        Usuario administrador = usuarios.findById(administradorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Se requiere una cuenta administradora activa."));
        if (administrador.getRol() != RolUsuario.ADMINISTRADOR
                || administrador.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere una cuenta administradora activa.");
        }
    }
}
