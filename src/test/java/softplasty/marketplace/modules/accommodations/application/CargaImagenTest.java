package softplasty.marketplace.modules.accommodations.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.domain.ImagenPublicacion;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.accommodations.api.ArchivoImagenController;
import softplasty.marketplace.modules.accommodations.api.PublicacionController;
import softplasty.marketplace.security.AccountPrincipal;
import org.springframework.mock.web.MockMultipartFile;
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

    @Test void guardaYEntregaUnaImagenValida() throws Exception {
        UUID host = UUID.randomUUID(), id = UUID.randomUUID(), imagenId = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getRol()).thenReturn(RolUsuario.ANFITRION);
        when(usuario.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
        when(usuarios.findById(host)).thenReturn(Optional.of(usuario));
        Publicacion publicacion = mock(Publicacion.class, RETURNS_DEEP_STUBS);
        when(publicacion.getEstado()).thenReturn(EstadoPublicacion.BORRADOR);
        when(publicacion.getReglasUso()).thenReturn(new String[0]);
        when(publicaciones.findOwnedByIdForUpdate(host, id)).thenReturn(Optional.of(publicacion));
        when(imagenes.findAllByPublicacion_IdOrderByOrdenAsc(id)).thenReturn(List.of());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", buffer);
        byte[] bytes = buffer.toByteArray();
        service.subirImagen(host, id, bytes, "image/png", "Sala");
        var captor = org.mockito.ArgumentCaptor.forClass(ImagenPublicacion.class);
        verify(imagenes).saveAndFlush(captor.capture());
        ImagenPublicacion guardada = captor.getValue();
        assertArrayEquals(bytes, guardada.getContenido());
        assertEquals("Sala", guardada.getTextoAlternativo());
        assertEquals("image/png", guardada.getTipoContenido());
        when(imagenes.findById(imagenId)).thenReturn(Optional.of(guardada));
        var respuesta = new ArchivoImagenController(service).obtener(imagenId);
        assertArrayEquals(bytes, respuesta.getBody());
        assertEquals("image/png", respuesta.getHeaders().getContentType().toString());
    }

    @Test void rechazaFormatoYTamanoInvalidos() {
        UUID host = UUID.randomUUID(), id = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getRol()).thenReturn(RolUsuario.ANFITRION);
        when(usuario.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
        when(usuarios.findById(host)).thenReturn(Optional.of(usuario));
        when(publicaciones.findOwnedByIdForUpdate(host, id)).thenReturn(Optional.of(mock(Publicacion.class)));
        assertThrows(ResponseStatusException.class, () -> service.subirImagen(host, id, new byte[0], "image/png", ""));
        assertThrows(ResponseStatusException.class, () -> service.subirImagen(host, id, new byte[2 * 1024 * 1024 + 1], "image/png", ""));
        assertThrows(ResponseStatusException.class, () -> service.subirImagen(host, id, new byte[]{1}, "image/gif", ""));
        verifyNoInteractions(imagenes);
    }

    @Test void controladorEntregaArchivoAlServicio() throws Exception {
        PublicacionService servicio = mock(PublicacionService.class);
        UUID host = UUID.randomUUID(), id = UUID.randomUUID();
        var principal = new AccountPrincipal(host, RolUsuario.ANFITRION, EstadoCuenta.ACTIVA);
        byte[] contenido = {1, 2};
        var archivo = new MockMultipartFile("archivo", "foto.png", "image/png", contenido);
        new PublicacionController(servicio).subirImagen(principal, id, archivo, "Sala");
        verify(servicio).subirImagen(host, id, contenido, "image/png", "Sala");
    }

    @Test void rechazaImagenInexistenteOSinArchivo() {
        UUID id = UUID.randomUUID();
        when(imagenes.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.obtenerArchivo(id));
        when(imagenes.findById(id)).thenReturn(Optional.of(mock(ImagenPublicacion.class)));
        assertThrows(ResponseStatusException.class, () -> service.obtenerArchivo(id));
    }
}
