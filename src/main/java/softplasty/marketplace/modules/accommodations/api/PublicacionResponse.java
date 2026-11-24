package softplasty.marketplace.modules.accommodations.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import softplasty.marketplace.modules.accommodations.domain.ImagenPublicacion;
import softplasty.marketplace.modules.accommodations.domain.Publicacion;
import softplasty.marketplace.modules.accommodations.domain.PublicacionServicio;

public record PublicacionResponse(
        UUID id,
        UUID alojamientoId,
        String anfitrionNombre,
        String ciudad,
        String ubicacionTexto,
        String tipoAlojamiento,
        int capacidadMaxima,
        UUID politicaCancelacionId,
        String politicaCancelacion,
        String titulo,
        String descripcion,
        BigDecimal precioNoche,
        List<String> reglasUso,
        String estado,
        boolean cambiosPendientesRevision,
        List<ImagenPublicacionResponse> imagenes,
        List<String> servicios
) {
    public static PublicacionResponse desde(Publicacion publicacion,
                                             List<ImagenPublicacion> imagenes,
                                             List<PublicacionServicio> servicios) {
        return new PublicacionResponse(publicacion.getId(), publicacion.getAlojamiento().getId(),
                publicacion.getAlojamiento().getAnfitrion().getNombre(),
                publicacion.getAlojamiento().getCiudad(), publicacion.getAlojamiento().getUbicacionTexto(),
                publicacion.getAlojamiento().getTipo(), publicacion.getAlojamiento().getCapacidadMaxima(),
                publicacion.getPoliticaCancelacion().getId(), publicacion.getPoliticaCancelacion().getNombre(),
                publicacion.getTitulo(), publicacion.getDescripcion(), publicacion.getPrecioNoche(),
                List.of(publicacion.getReglasUso()), publicacion.getEstado().name(),
                publicacion.isCambiosPendientesRevision(),
                imagenes.stream().map(ImagenPublicacionResponse::desde).toList(),
                servicios.stream().map(link -> link.getServicio().getCodigo()).sorted().toList());
    }
}
