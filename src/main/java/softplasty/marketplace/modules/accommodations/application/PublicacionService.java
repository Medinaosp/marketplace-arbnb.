package softplasty.marketplace.modules.accommodations.application;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.api.ActualizarPublicacionRequest;
import softplasty.marketplace.modules.accommodations.api.AgregarImagenRequest;
import softplasty.marketplace.modules.accommodations.api.CrearPublicacionRequest;
import softplasty.marketplace.modules.accommodations.api.PublicacionResponse;
import softplasty.marketplace.modules.accommodations.domain.Alojamiento;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.accommodations.domain.ImagenPublicacion;
import softplasty.marketplace.modules.accommodations.domain.PoliticaCancelacion;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.domain.PublicacionServicio;
import softplasty.marketplace.modules.accommodations.domain.ServicioAlojamiento;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.AlojamientoRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.ImagenPublicacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PoliticaCancelacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionServicioRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.ServicioAlojamientoRepository;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class PublicacionService {

    private final PublicacionRepository publicaciones;
    private final AlojamientoRepository alojamientos;
    private final PoliticaCancelacionRepository politicas;
    private final ImagenPublicacionRepository imagenes;
    private final PublicacionServicioRepository serviciosPublicacion;
    private final ServicioAlojamientoRepository servicios;
    private final UsuarioRepository usuarios;

    public PublicacionService(PublicacionRepository publicaciones, AlojamientoRepository alojamientos,
                              PoliticaCancelacionRepository politicas,
                              ImagenPublicacionRepository imagenes,
                              PublicacionServicioRepository serviciosPublicacion,
                              ServicioAlojamientoRepository servicios, UsuarioRepository usuarios) {
        this.publicaciones = publicaciones;
        this.alojamientos = alojamientos;
        this.politicas = politicas;
        this.imagenes = imagenes;
        this.serviciosPublicacion = serviciosPublicacion;
        this.servicios = servicios;
        this.usuarios = usuarios;
    }

    @Transactional
    public PublicacionResponse crear(UUID anfitrionId, CrearPublicacionRequest request) {
        validarAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = alojamientos.findById(request.alojamientoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El alojamiento no existe."));
        validarPropiedad(alojamiento, anfitrionId);
        if (publicaciones.findByAlojamiento_Id(alojamiento.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El alojamiento ya tiene una publicación.");
        }
        PoliticaCancelacion politica = obtenerPolitica(request.politicaCancelacionId());
        Publicacion publicacion = new Publicacion(alojamiento, politica, request.titulo().trim(),
                request.descripcion().trim(), request.precioNoche(), new String[0], EstadoPublicacion.BORRADOR);
        return respuesta(publicaciones.save(publicacion));
    }

    @Transactional
    public PublicacionResponse actualizarFicha(UUID anfitrionId, UUID publicacionId,
                                               ActualizarPublicacionRequest request) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        PoliticaCancelacion politica = obtenerPolitica(request.politicaCancelacionId());
        String[] reglas = request.reglasUso() == null ? new String[0] : request.reglasUso().stream()
                .map(String::trim).toArray(String[]::new);
        publicacion.editarFicha(request.titulo().trim(), request.descripcion().trim(),
                request.precioNoche(), reglas, politica,
                Math.toIntExact(imagenes.countByPublicacion_Id(publicacionId)));
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse agregarImagen(UUID anfitrionId, UUID publicacionId,
                                             AgregarImagenRequest request) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        validarReferenciaImagen(request.referencia());
        if (imagenes.existsByPublicacion_IdAndOrden(publicacionId, request.orden())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una imagen con ese orden en la publicación.");
        }
        ImagenPublicacion imagen = new ImagenPublicacion(publicacion,
                request.referencia().trim(), request.orden(), request.textoAlternativo().trim());
        imagenes.save(imagen);
        long cantidadDespuesDeAgregar = imagenes.countByPublicacion_Id(publicacionId);
        publicacion.registrarCambioImportante(Math.toIntExact(cantidadDespuesDeAgregar));
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse eliminarImagen(UUID anfitrionId, UUID publicacionId, UUID imagenId) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        ImagenPublicacion imagen = imagenes.findByIdAndPublicacion_Id(imagenId, publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La imagen no existe en esta publicación."));
        long cantidad = imagenes.countByPublicacion_Id(publicacionId);
        if ((publicacion.getEstado() == EstadoPublicacion.PENDIENTE_REVISION
                || publicacion.getEstado() == EstadoPublicacion.ACTIVA) && cantidad <= 1) {
            throw new IllegalStateException(
                    "No se puede quitar la última imagen de una publicación en revisión o activa.");
        }
        publicacion.registrarCambioImportante(Math.toIntExact(cantidad));
        imagenes.delete(imagen);
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse subirImagen(UUID anfitrionId, UUID publicacionId, byte[] contenido, String tipo, String alt) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        if (contenido.length == 0 || contenido.length > 2 * 1024 * 1024 || !("image/jpeg".equals(tipo) || "image/png".equals(tipo))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser JPG o PNG y pesar máximo 2 MB.");
        }
        try (var entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new IllegalArgumentException();
            var lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                if (!("image/jpeg".equals(tipo) && ("jpeg".equals(formato) || "jpg".equals(formato))
                        || "image/png".equals(tipo) && "png".equals(formato))
                        || (long) lector.getWidth(0) * lector.getHeight(0) > 20_000_000L
                        || lector.read(0) == null) throw new IllegalArgumentException();
            } finally { lector.dispose(); }
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen válida.");
        }
        var actuales = imagenes.findAllByPublicacion_IdOrderByOrdenAsc(publicacionId);
        int orden = actuales.stream().mapToInt(ImagenPublicacion::getOrden).max().orElse(0) + 1;
        ImagenPublicacion imagen = new ImagenPublicacion(publicacion, "archivo", orden, alt == null || alt.isBlank() ? "Fotografía del alojamiento" : alt.trim().substring(0, Math.min(alt.trim().length(), 300)));
        imagen.guardarArchivo(contenido, tipo);
        imagenes.saveAndFlush(imagen);
        imagen.asignarReferenciaArchivo();
        publicacion.registrarCambioImportante(actuales.size() + 1);
        return respuesta(publicacion);
    }

    @Transactional(readOnly = true)
    public ImagenPublicacion obtenerArchivo(UUID id) {
        ImagenPublicacion imagen = imagenes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (imagen.getContenido() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return imagen;
    }

    @Transactional
    public PublicacionResponse cambiarOrdenImagen(UUID anfitrionId, UUID publicacionId,
                                                  UUID imagenId, int nuevoOrden) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        ImagenPublicacion imagen = imagenes.findByIdAndPublicacion_Id(imagenId, publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La imagen no existe en esta publicación."));
        if (imagenes.existsByPublicacion_IdAndOrdenAndIdNot(publicacionId, nuevoOrden, imagenId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una imagen con ese orden en la publicación.");
        }
        imagen.cambiarOrden(nuevoOrden);
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse actualizarServicios(UUID anfitrionId, UUID publicacionId,
                                                   List<UUID> servicioIds) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        if (servicioIds.size() != new HashSet<>(servicioIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede asociar dos veces el mismo servicio.");
        }
        List<PublicacionServicio> anteriores = serviciosPublicacion.findAllByPublicacion_Id(publicacionId);
        Set<UUID> idsAnteriores = anteriores.stream()
                .map(link -> link.getServicio().getId()).collect(java.util.stream.Collectors.toSet());
        Set<UUID> idsNuevos = new HashSet<>(servicioIds);
        if (!idsAnteriores.equals(idsNuevos)) {
            List<ServicioAlojamiento> serviciosActivos = idsNuevos.isEmpty()
                    ? List.of() : servicios.findAllByIdInAndActivoTrue(idsNuevos);
            if (serviciosActivos.size() != idsNuevos.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Uno o más servicios no existen o están inactivos.");
            }
            publicacion.registrarCambioImportante(
                    Math.toIntExact(imagenes.countByPublicacion_Id(publicacionId)));
            serviciosPublicacion.deleteAllByPublicacion_Id(publicacionId);
            List<PublicacionServicio> nuevos = serviciosActivos.stream()
                    .map(servicio -> new PublicacionServicio(publicacion, servicio)).toList();
            serviciosPublicacion.saveAll(nuevos);
        }
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse enviarARevision(UUID anfitrionId, UUID publicacionId) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        publicacion.enviarARevision(Math.toIntExact(imagenes.countByPublicacion_Id(publicacionId)));
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse pausar(UUID anfitrionId, UUID publicacionId) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        publicacion.pausar();
        return respuesta(publicacion);
    }

    @Transactional
    public PublicacionResponse activar(UUID anfitrionId, UUID publicacionId) {
        Publicacion publicacion = obtenerPropiaForUpdate(anfitrionId, publicacionId);
        publicacion.activar(Math.toIntExact(imagenes.countByPublicacion_Id(publicacionId)));
        return respuesta(publicacion);
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponse> listarPropias(UUID anfitrionId) {
        validarAnfitrionActivo(anfitrionId);
        return publicaciones.findAllByAlojamiento_Anfitrion_IdOrderByTituloAsc(anfitrionId).stream()
                .map(this::respuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicacionResponse consultarPropia(UUID anfitrionId, UUID publicacionId) {
        return respuesta(obtenerPropia(anfitrionId, publicacionId));
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponse> buscarPublicas(String ciudad) {
        String ciudadNormalizada = ciudad == null ? "" : ciudad.trim();
        if (ciudadNormalizada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ciudad es obligatoria.");
        }
        return publicaciones.findAllByEstadoAndAlojamiento_CiudadIgnoreCaseAndAlojamiento_Anfitrion_EstadoOrderByTituloAsc(
                        EstadoPublicacion.ACTIVA, ciudadNormalizada, EstadoCuenta.ACTIVA).stream()
                .map(this::respuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicacionResponse consultarPublica(UUID publicacionId) {
        Publicacion publicacion = publicaciones.findByIdAndEstadoAndAlojamiento_Anfitrion_Estado(
                        publicacionId, EstadoPublicacion.ACTIVA, EstadoCuenta.ACTIVA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La publicación no existe."));
        return respuesta(publicacion);
    }

    private Publicacion obtenerPropia(UUID anfitrionId, UUID publicacionId) {
        validarAnfitrionActivo(anfitrionId);
        return publicaciones.findByAlojamiento_Anfitrion_IdAndId(anfitrionId, publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La publicación no existe."));
    }

    private Publicacion obtenerPropiaForUpdate(UUID anfitrionId, UUID publicacionId) {
        validarAnfitrionActivo(anfitrionId);
        return publicaciones.findOwnedByIdForUpdate(anfitrionId, publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La publicación no existe."));
    }

    private Usuario validarAnfitrionActivo(UUID anfitrionId) {
        Usuario usuario = usuarios.findById(anfitrionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe."));
        if (usuario.getRol() != RolUsuario.ANFITRION || usuario.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere una cuenta de anfitrión activa.");
        }
        return usuario;
    }

    private PoliticaCancelacion obtenerPolitica(UUID id) {
        return politicas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La política de cancelación no existe."));
    }

    private void validarPropiedad(Alojamiento alojamiento, UUID anfitrionId) {
        if (!alojamiento.getAnfitrion().getId().equals(anfitrionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El alojamiento no existe.");
        }
    }

    private void validarReferenciaImagen(String referencia) {
        try {
            URI uri = new URI(referencia.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || scheme == null
                    || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                    || scheme.toLowerCase(Locale.ROOT).equals("https"))) {
                throw new IllegalArgumentException("La referencia de imagen debe ser una URL http o https válida.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("La referencia de imagen debe ser una URL http o https válida.");
        }
    }

    private PublicacionResponse respuesta(Publicacion publicacion) {
        List<ImagenPublicacion> imagenesPublicacion = publicacion.getId() == null
                ? new ArrayList<>() : imagenes.findAllByPublicacion_IdOrderByOrdenAsc(publicacion.getId());
        List<PublicacionServicio> serviciosDePublicacion = publicacion.getId() == null
                ? new ArrayList<>() : serviciosPublicacion.findAllByPublicacion_Id(publicacion.getId());
        return PublicacionResponse.desde(publicacion, imagenesPublicacion, serviciosDePublicacion);
    }
}
