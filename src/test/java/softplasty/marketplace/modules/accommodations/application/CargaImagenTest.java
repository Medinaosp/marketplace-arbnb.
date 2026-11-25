package softplasty.marketplace.modules.accommodations.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.*;
import softplasty.marketplace.modules.users.domain.*;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

class CargaImagenTest {
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final ImagenPublicacionRepository imagenes = mock(ImagenPublicacionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PublicacionService service = new PublicacionService(publicaciones, mock(AlojamientoRepository.class),
            mock(PoliticaCancelacionRepository.class), imagenes, mock(PublicacionServicioRepository.class),
            mock(ServicioAlojamientoRepository.class), usuarios);

    @Test void rechazaUnaPublicacionAjena() {
        UUID host = UUID.randomUUID(), publicacion = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getRol()).thenReturn(RolUsuario.ANFITRION);
        when(usuario.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
        when(usuarios.findById(host)).thenReturn(Optional.of(usuario));
        when(publicaciones.findOwnedByIdForUpdate(host, publicacion)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.subirImagen(host, publicacion, new byte[]{1}, "image/png", "foto"));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(imagenes);
    }

    @Test void rechazaContenidoQueNoEsImagen() {
        UUID host = UUID.randomUUID(), publicacion = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getRol()).thenReturn(RolUsuario.ANFITRION);
        when(usuario.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
        when(usuarios.findById(host)).thenReturn(Optional.of(usuario));
        when(publicaciones.findOwnedByIdForUpdate(host, publicacion)).thenReturn(Optional.of(mock(Publicacion.class)));
        var error = assertThrows(ResponseStatusException.class, () -> service.subirImagen(host, publicacion, new byte[]{1, 2, 3}, "image/png", "foto"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(imagenes);
    }
}
