package softplasty.marketplace.modules.accommodations.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import softplasty.marketplace.modules.users.application.PoliticaContrasena;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

/** Optional, idempotent demo data; only active for explicit dev/test profiles. */
@Component
@Profile({"dev", "test"})
public class DatosDemostracion implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatosDemostracion.class);
    private static final UUID FLEXIBLE = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final String DEMO_HOST_EMAIL_DEFAULT = "anfitrion.demo@example.test";
    private static final String DEMO_GUEST_EMAIL_DEFAULT = "huesped.demo@example.test";

    private final UsuarioRepository usuarios;
    private final AlojamientoRepository alojamientos;
    private final PublicacionRepository publicaciones;
    private final ImagenPublicacionRepository imagenes;
    private final PublicacionServicioRepository serviciosPublicacion;
    private final PoliticaCancelacionRepository politicas;
    private final ServicioAlojamientoRepository servicios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final Clock clock;
    private final String hostEmail;
    private final String hostPassword;
    private final String guestEmail;
    private final String guestPassword;

    public DatosDemostracion(UsuarioRepository usuarios,
                             AlojamientoRepository alojamientos,
                             PublicacionRepository publicaciones,
                             ImagenPublicacionRepository imagenes,
                             PublicacionServicioRepository serviciosPublicacion,
                             PoliticaCancelacionRepository politicas,
                             ServicioAlojamientoRepository servicios,
                             PasswordEncoder passwordEncoder,
                             PoliticaContrasena politicaContrasena,
                             Clock clock,
                             @Value("${app.demo.host.email:" + DEMO_HOST_EMAIL_DEFAULT + "}") String hostEmail,
                             @Value("${app.demo.host.password:}") String hostPassword,
                             @Value("${app.demo.guest.email:" + DEMO_GUEST_EMAIL_DEFAULT + "}") String guestEmail,
                             @Value("${app.demo.guest.password:}") String guestPassword) {
        this.usuarios = usuarios;
        this.alojamientos = alojamientos;
        this.publicaciones = publicaciones;
        this.imagenes = imagenes;
        this.serviciosPublicacion = serviciosPublicacion;
        this.politicas = politicas;
        this.servicios = servicios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.clock = clock;
        this.hostEmail = hostEmail;
        this.hostPassword = hostPassword;
        this.guestEmail = guestEmail;
        this.guestPassword = guestPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hostPassword.isBlank() || guestPassword.isBlank()) {
            LOGGER.info("Datos de demostración omitidos. Para habilitarlos en dev/test, configura "
                    + "APP_DEMO_HOST_PASSWORD y APP_DEMO_GUEST_PASSWORD.");
            return;
        }
        String normalizedHostEmail = hostEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedGuestEmail = guestEmail.trim().toLowerCase(Locale.ROOT);
        Usuario anfitrion = usuarios.findByCorreoIgnoreCase(normalizedHostEmail).orElse(null);
        boolean cuentaNueva = anfitrion == null;
        if (cuentaNueva) {
            if (usuarios.findByCorreoIgnoreCase(normalizedGuestEmail).isPresent()) {
                throw new IllegalStateException("DEMO_GUEST_EMAIL ya pertenece a una cuenta existente.");
            }
            politicaContrasena.validar(hostPassword);
            politicaContrasena.validar(guestPassword);
            anfitrion = usuarios.save(new Usuario("Anfitrión de demostración", normalizedHostEmail,
                    passwordEncoder.encode(hostPassword), RolUsuario.ANFITRION,
                    EstadoCuenta.ACTIVA, Instant.now(clock)));
            usuarios.save(new Usuario("Huésped de demostración", normalizedGuestEmail,
                    passwordEncoder.encode(guestPassword), RolUsuario.HUESPED,
                    EstadoCuenta.ACTIVA, Instant.now(clock)));
        }

        PoliticaCancelacion politica = politicas.findById(FLEXIBLE)
                .orElseThrow(() -> new IllegalStateException("Falta el catálogo Flexible de cancelación."));
        List<ServicioAlojamiento> catalogoServicios = servicios.findAllByActivoTrueOrderByNombreAsc();

        if (cuentaNueva) {
            crearPublicacion(anfitrion, politica, "Apartamento de demostración activo",
                    "Alojamiento de ejemplo en Bogotá para explorar el flujo público.",
                    EstadoPublicacion.ACTIVA, "BOGOTA", catalogoServicios, true);
            crearPublicacion(anfitrion, politica, "Casa de demostración pendiente",
                    "Publicación de ejemplo que espera revisión administrativa.",
                    EstadoPublicacion.PENDIENTE_REVISION, "MEDELLIN", catalogoServicios, false);
            crearPublicacion(anfitrion, politica, "Loft de demostración pausado",
                    "Publicación de ejemplo pausada por su anfitrión.",
                    EstadoPublicacion.PAUSADA, "BOGOTA", catalogoServicios, false);
            crearPublicacion(anfitrion, politica, "Cabaña de demostración bloqueada",
                    "Publicación de ejemplo bloqueada para el flujo de corrección.",
                    EstadoPublicacion.BLOQUEADA, "MEDELLIN", catalogoServicios, false);
            crearPublicacion(anfitrion, politica, "Borrador de demostración",
                    "Borrador sin imágenes, válido según la excepción acordada.",
                    EstadoPublicacion.BORRADOR, "BOGOTA", catalogoServicios, false);
        }
        crearCabanasDemostracion(anfitrion, politica);
        LOGGER.info("Datos de demostración revisados para los perfiles dev/test.");
    }

    private void crearCabanasDemostracion(Usuario anfitrion, PoliticaCancelacion politica) {
        List<Publicacion> existentes = publicaciones
                .findAllByAlojamiento_Anfitrion_IdOrderByTituloAsc(anfitrion.getId());
        List<CabanaDemo> cabanas = List.of(
                new CabanaDemo(1, "Bogotá", "Cabaña de montaña · Bogotá 1", 4, "260000", "Zona norte"),
                new CabanaDemo(2, "Bogotá", "Cabaña de montaña · Bogotá 2", 6, "310000", "Zona rural"),
                new CabanaDemo(3, "Medellín", "Cabaña entre verdes · Medellín 1", 4, "280000", "Las afueras"),
                new CabanaDemo(4, "Medellín", "Cabaña entre verdes · Medellín 2", 6, "350000", "Ladera campestre"),
                new CabanaDemo(5, "Cartagena", "Cabaña caribeña · Cartagena", 4, "420000", "Zona campestre"),
                new CabanaDemo(6, "Santa Marta", "Cabaña junto al mar · Santa Marta", 5, "380000", "Corredor turístico"),
                new CabanaDemo(7, "Salento", "Cabaña cafetera · Salento", 4, "290000", "Valle de Cocora"),
                new CabanaDemo(8, "Villa de Leyva", "Cabaña colonial · Villa de Leyva", 5, "320000", "Entorno rural"),
                new CabanaDemo(9, "Guatapé", "Cabaña del embalse · Guatapé", 4, "340000", "Cerca del embalse"),
                new CabanaDemo(10, "Cali", "Cabaña campestre · Cali", 4, "240000", "Zona campestre")
        );
        for (CabanaDemo cabana : cabanas) {
            Publicacion existente = existentes.stream()
                    .filter(p -> p.getTitulo().equals(cabana.titulo()))
                    .findFirst().orElse(null);
            if (existente == null) {
                crearCabana(anfitrion, politica, cabana);
            } else if (existente.getEstado() == EstadoPublicacion.BORRADOR) {
                publicarCabanaDemostracion(existente, cabana);
            }
        }
    }

    private void crearCabana(Usuario anfitrion, PoliticaCancelacion politica, CabanaDemo cabana) {
        Alojamiento alojamiento = alojamientos.save(new Alojamiento(anfitrion, cabana.ciudad(),
                cabana.zona() + " · ubicación de demostración", "Cabaña", cabana.capacidad()));
        Publicacion publicacion = publicaciones.save(new Publicacion(alojamiento, politica, cabana.titulo(),
                "Publicación de demostración. Completa la ubicación y los detalles reales antes de publicarla.",
                new BigDecimal(cabana.precio()), new String[0], EstadoPublicacion.BORRADOR));
        publicarCabanaDemostracion(publicacion, cabana);
    }

    private void publicarCabanaDemostracion(Publicacion publicacion, CabanaDemo cabana) {
        long cantidadImagenes = imagenes.countByPublicacion_Id(publicacion.getId());
        if (cantidadImagenes == 0) {
            imagenes.save(new ImagenPublicacion(publicacion,
                    "http://localhost:5173/alojamientos/cabanas/cabana-%02d.png".formatted(cabana.numero()),
                    1, "Ilustración de cabaña de demostración en " + cabana.ciudad()));
            cantidadImagenes = 1;
        }
        publicacion.enviarARevision(Math.toIntExact(cantidadImagenes));
        publicacion.aprobar(Math.toIntExact(cantidadImagenes));
    }

    private record CabanaDemo(int numero, String ciudad, String titulo, int capacidad,
                              String precio, String zona) { }

    private void crearPublicacion(Usuario anfitrion, PoliticaCancelacion politica, String titulo,
                                  String descripcion, EstadoPublicacion estado, String ciudad,
                                  List<ServicioAlojamiento> catalogoServicios, boolean asociarServicios) {
        Alojamiento alojamiento = alojamientos.save(new Alojamiento(anfitrion, ciudad,
                "Dirección de ejemplo", "Apartamento", 4));
        Publicacion publicacion = publicaciones.save(new Publicacion(alojamiento, politica, titulo,
                descripcion, new BigDecimal("125000.00"), new String[0], EstadoPublicacion.BORRADOR));

        if (estado == EstadoPublicacion.BORRADOR) {
            return;
        }
        imagenes.save(new ImagenPublicacion(publicacion,
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267", 1, "Interior del alojamiento"));
        publicacion.enviarARevision(1);
        switch (estado) {
            case ACTIVA -> publicacion.aprobar(1);
            case PAUSADA -> {
                publicacion.aprobar(1);
                publicacion.pausar();
            }
            case BLOQUEADA -> publicacion.bloquear("Bloqueo de demostración");
            case PENDIENTE_REVISION -> { }
            default -> throw new IllegalStateException("Estado de demostración no soportado: " + estado);
        }
        if (asociarServicios) {
            catalogoServicios.stream().limit(4)
                    .map(servicio -> new PublicacionServicio(publicacion, servicio))
                    .forEach(serviciosPublicacion::save);
        }
    }
}
