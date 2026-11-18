package softplasty.marketplace.modules.accommodations.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.accommodations.api.AlojamientoRequest;
import softplasty.marketplace.modules.accommodations.api.AlojamientoResponse;
import softplasty.marketplace.modules.accommodations.api.DisponibilidadResponse;
import softplasty.marketplace.modules.accommodations.api.PeriodoDisponibilidadResponse;
import softplasty.marketplace.modules.accommodations.domain.Alojamiento;
import softplasty.marketplace.modules.accommodations.domain.PeriodoDisponibilidad;
import softplasty.marketplace.modules.accommodations.domain.TipoPeriodo;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.AlojamientoRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PeriodoDisponibilidadRepository;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.domain.Usuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class AlojamientoService {

    private final AlojamientoRepository alojamientos;
    private final PeriodoDisponibilidadRepository periodos;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public AlojamientoService(AlojamientoRepository alojamientos,
                              PeriodoDisponibilidadRepository periodos,
                              UsuarioRepository usuarios, Clock clock) {
        this.alojamientos = alojamientos;
        this.periodos = periodos;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional
    public AlojamientoResponse crear(UUID anfitrionId, AlojamientoRequest request) {
        Usuario anfitrion = obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = new Alojamiento(anfitrion, request.ciudad().trim(),
                request.ubicacionTexto().trim(), request.tipo().trim(), request.capacidadMaxima());
        return AlojamientoResponse.desde(alojamientos.save(alojamiento));
    }

    @Transactional
    public AlojamientoResponse actualizar(UUID anfitrionId, UUID alojamientoId,
                                          AlojamientoRequest request) {
        obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = obtenerPropioForUpdate(anfitrionId, alojamientoId);
        alojamiento.actualizarDatos(request.ciudad().trim(), request.ubicacionTexto().trim(),
                request.tipo().trim(), request.capacidadMaxima());
        return AlojamientoResponse.desde(alojamiento);
    }

    @Transactional(readOnly = true)
    public List<AlojamientoResponse> listarPropios(UUID anfitrionId) {
        obtenerAnfitrionActivo(anfitrionId);
        return alojamientos.findAllByAnfitrion_IdOrderByCiudadAsc(anfitrionId).stream()
                .map(AlojamientoResponse::desde)
                .toList();
    }

    @Transactional
    public PeriodoDisponibilidadResponse bloquear(UUID anfitrionId, UUID alojamientoId,
                                                  LocalDate inicio, LocalDate fin) {
        obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = obtenerPropioForUpdate(anfitrionId, alojamientoId);
        PeriodoDisponibilidad periodo = alojamiento.bloquearPeriodo(inicio, fin, LocalDate.now(clock));
        periodos.save(periodo);
        return PeriodoDisponibilidadResponse.desde(periodo);
    }

    @Transactional
    public void liberar(UUID anfitrionId, UUID alojamientoId, LocalDate inicio, LocalDate fin) {
        obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = obtenerPropioForUpdate(anfitrionId, alojamientoId);
        alojamiento.liberarPeriodo(inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<PeriodoDisponibilidadResponse> listarBloqueos(UUID anfitrionId, UUID alojamientoId) {
        obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = obtenerPropio(anfitrionId, alojamientoId);
        return alojamiento.getPeriodosDisponibilidad().stream()
                .filter(periodo -> periodo.getTipo() == TipoPeriodo.BLOQUEADO)
                .sorted(java.util.Comparator.comparing(PeriodoDisponibilidad::getInicio))
                .map(PeriodoDisponibilidadResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(UUID anfitrionId, UUID alojamientoId,
                                                          LocalDate inicio, LocalDate fin) {
        obtenerAnfitrionActivo(anfitrionId);
        Alojamiento alojamiento = obtenerPropio(anfitrionId, alojamientoId);
        return new DisponibilidadResponse(alojamientoId, inicio, fin,
                alojamiento.estaDisponible(inicio, fin));
    }

    private Usuario obtenerAnfitrionActivo(UUID id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe."));
        if (usuario.getRol() != RolUsuario.ANFITRION || usuario.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere una cuenta de anfitrión activa.");
        }
        return usuario;
    }

    private Alojamiento obtenerPropio(UUID anfitrionId, UUID alojamientoId) {
        Alojamiento alojamiento = alojamientos.findById(alojamientoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El alojamiento no existe."));
        validarPropiedad(alojamiento, anfitrionId);
        return alojamiento;
    }

    private Alojamiento obtenerPropioForUpdate(UUID anfitrionId, UUID alojamientoId) {
        Alojamiento alojamiento = alojamientos.findByIdForUpdate(alojamientoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El alojamiento no existe."));
        validarPropiedad(alojamiento, anfitrionId);
        return alojamiento;
    }

    private void validarPropiedad(Alojamiento alojamiento, UUID anfitrionId) {
        if (!alojamiento.getAnfitrion().getId().equals(anfitrionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El alojamiento no existe.");
        }
    }
}
