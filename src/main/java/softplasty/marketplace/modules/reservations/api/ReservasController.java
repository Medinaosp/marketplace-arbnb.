package softplasty.marketplace.modules.reservations.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import softplasty.marketplace.modules.reservations.application.ReservasService;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
public class ReservasController {
    private final ReservasService service;
    public ReservasController(ReservasService service) { this.service=service; }
    @PostMapping("/api/publicaciones/{publicacionId}/cotizaciones") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> cotizar(@PathVariable UUID publicacionId,@AuthenticationPrincipal AccountPrincipal user,@Valid @RequestBody NuevaCotizacionRequest request){return service.cotizar(publicacionId,user.id(),request);}
    @PostMapping("/api/reservas/cotizaciones/{cotizacionId}/reservar") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> reservar(@PathVariable UUID cotizacionId,@AuthenticationPrincipal AccountPrincipal user){return service.reservar(cotizacionId,user.id());}
    @PostMapping("/api/reservas/{reservaId}/pago-simulado") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth")
    public Map<String,Object> pagar(@PathVariable UUID reservaId,@AuthenticationPrincipal AccountPrincipal user,@Valid @RequestBody PagoSimuladoRequest request){return service.pagar(reservaId,user.id(),request.resultado());}
    @PostMapping("/api/reservas/{reservaId}/cancelar") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth")
    public Map<String,Object> cancelar(@PathVariable UUID reservaId,@AuthenticationPrincipal AccountPrincipal user,@Valid @RequestBody CancelarReservaRequest request){return service.cancelar(reservaId,user.id(),request.motivo());}
    @GetMapping("/api/reservas/mias") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth")
    public List<Map<String,Object>> misReservas(@AuthenticationPrincipal AccountPrincipal user){return service.misReservas(user.id());}
    @GetMapping("/api/reservas/anfitrion") @PreAuthorize("hasRole('ANFITRION')") @SecurityRequirement(name="bearerAuth")
    public List<Map<String,Object>> reservasAnfitrion(@AuthenticationPrincipal AccountPrincipal user){return service.reservasAnfitrion(user.id());}
    @PostMapping("/api/publicaciones/{publicacionId}/descuentos") @PreAuthorize("hasRole('ANFITRION')") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crearDescuento(@PathVariable UUID publicacionId,@AuthenticationPrincipal AccountPrincipal user,@Valid @RequestBody CrearDescuentoRequest request){return service.crearDescuento(user.id(),publicacionId,request);}
    @GetMapping("/api/publicaciones/{publicacionId}/descuentos") @PreAuthorize("hasRole('ANFITRION')") @SecurityRequirement(name="bearerAuth")
    public List<Map<String,Object>> descuentos(@PathVariable UUID publicacionId,@AuthenticationPrincipal AccountPrincipal user){return service.descuentos(user.id(),publicacionId);}
    @DeleteMapping("/api/publicaciones/{publicacionId}/descuentos/{descuentoId}") @PreAuthorize("hasRole('ANFITRION')") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarDescuento(@PathVariable UUID publicacionId,@PathVariable UUID descuentoId,@AuthenticationPrincipal AccountPrincipal user){service.desactivarDescuento(user.id(),publicacionId,descuentoId);}
    @GetMapping("/api/servicios/tarifas") public List<Map<String,Object>> tarifas(){return service.tarifas();}
    @PutMapping("/api/administracion/servicios/{servicioId}/tarifa") @PreAuthorize("hasRole('ADMINISTRADOR')") @SecurityRequirement(name="bearerAuth")
    public Map<String,Object> actualizarTarifa(@PathVariable UUID servicioId,@Valid @RequestBody TarifaServicioRequest request){return service.actualizarTarifa(servicioId,request);}
    @GetMapping("/api/publicaciones/{publicacionId}/resenas") public List<Map<String,Object>> resenasPublicas(@PathVariable UUID publicacionId){return service.resenasPublicas(publicacionId);}
    @PostMapping("/api/reservas/{reservaId}/resena") @PreAuthorize("hasRole('HUESPED')") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crearResena(@PathVariable UUID reservaId,@AuthenticationPrincipal AccountPrincipal user,@Valid @RequestBody CrearResenaRequest request){return service.crearResena(user.id(),reservaId,request);}
    @GetMapping("/api/administracion/resenas") @PreAuthorize("hasRole('ADMINISTRADOR')") @SecurityRequirement(name="bearerAuth")
    public List<Map<String,Object>> resenasAdmin(){return service.resenasAdmin();}
    @PatchMapping("/api/administracion/resenas/{resenaId}") @PreAuthorize("hasRole('ADMINISTRADOR')") @SecurityRequirement(name="bearerAuth")
    public Map<String,Object> moderar(@PathVariable UUID resenaId,@RequestParam String estado){return service.moderarResena(resenaId,estado);}
}
