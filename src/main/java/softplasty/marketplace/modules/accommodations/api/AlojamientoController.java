package softplasty.marketplace.modules.accommodations.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.accommodations.application.AlojamientoService;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
@RequestMapping("/api/alojamientos")
@PreAuthorize("hasRole('ANFITRION')")
@SecurityRequirement(name = "bearerAuth")
public class AlojamientoController {

    private final AlojamientoService alojamientoService;

    public AlojamientoController(AlojamientoService alojamientoService) {
        this.alojamientoService = alojamientoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlojamientoResponse crear(@AuthenticationPrincipal AccountPrincipal principal,
                                     @Valid @RequestBody AlojamientoRequest request) {
        return alojamientoService.crear(principal.id(), request);
    }

    @GetMapping("/mios")
    public List<AlojamientoResponse> listarPropios(@AuthenticationPrincipal AccountPrincipal principal) {
        return alojamientoService.listarPropios(principal.id());
    }

    @PutMapping("/{alojamientoId}")
    public AlojamientoResponse actualizar(@AuthenticationPrincipal AccountPrincipal principal,
                                          @PathVariable UUID alojamientoId,
                                          @Valid @RequestBody AlojamientoRequest request) {
        return alojamientoService.actualizar(principal.id(), alojamientoId, request);
    }

    @PostMapping("/{alojamientoId}/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoDisponibilidadResponse bloquear(@AuthenticationPrincipal AccountPrincipal principal,
                                                  @PathVariable UUID alojamientoId,
                                                  @Valid @RequestBody PeriodoBloqueoRequest request) {
        return alojamientoService.bloquear(principal.id(), alojamientoId,
                request.inicio(), request.fin());
    }

    @DeleteMapping("/{alojamientoId}/bloqueos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@AuthenticationPrincipal AccountPrincipal principal,
                        @PathVariable UUID alojamientoId,
                        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        alojamientoService.liberar(principal.id(), alojamientoId, inicio, fin);
    }

    @GetMapping("/{alojamientoId}/bloqueos")
    public List<PeriodoDisponibilidadResponse> listarBloqueos(@AuthenticationPrincipal AccountPrincipal principal,
                                                               @PathVariable UUID alojamientoId) {
        return alojamientoService.listarBloqueos(principal.id(), alojamientoId);
    }

    @GetMapping("/{alojamientoId}/disponibilidad")
    public DisponibilidadResponse consultarDisponibilidad(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID alojamientoId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return alojamientoService.consultarDisponibilidad(principal.id(), alojamientoId, inicio, fin);
    }
}
