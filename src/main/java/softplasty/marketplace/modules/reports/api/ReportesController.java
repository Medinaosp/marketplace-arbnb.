package softplasty.marketplace.modules.reports.api;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.reports.application.ReportesService;
import softplasty.marketplace.modules.reports.application.EventoAuditoriaService;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.security.AccountPrincipal;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {
    private final ReportesService reportes;
    private final EventoAuditoriaService auditoria;
    public ReportesController(ReportesService reportes, EventoAuditoriaService auditoria) {
        this.reportes = reportes; this.auditoria = auditoria;
    }

    @GetMapping("/administrador")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public ReportesResponse.Admin administrador(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) EstadoCuenta estado,
            @RequestParam(required = false) EstadoPublicacion estadoPublicacion) {
        return reportes.administrador(desde, hasta, ciudad, rol, estado, estadoPublicacion);
    }

    @GetMapping("/anfitrion")
    @PreAuthorize("hasRole('ANFITRION') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public ReportesResponse.Host anfitrion(@AuthenticationPrincipal AccountPrincipal principal,
                                           @RequestParam(required = false) LocalDate desde,
                                           @RequestParam(required = false) LocalDate hasta,
                                           @RequestParam(required = false) java.util.UUID alojamientoId) {
        return reportes.anfitrion(principal.id(), desde, hasta, alojamientoId);
    }

    @GetMapping("/administrador/actividad")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public List<ReportesResponse.Evento> actividad(@RequestParam(required = false) LocalDate desde,
                                                    @RequestParam(required = false) LocalDate hasta,
                                                    @RequestParam(required = false) String accion) {
        return reportes.actividad(desde, hasta, accion);
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANFITRION') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public List<ReportesResponse.Exportacion> historial(@AuthenticationPrincipal AccountPrincipal principal) {
        return reportes.historial(principal.id(), principal.rol().name().equals("ADMINISTRADOR"));
    }

    @PostMapping("/administrador/exportaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> exportarAdministrador(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ExportarReporteRequest request,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) EstadoCuenta estado,
            @RequestParam(required = false) EstadoPublicacion estadoPublicacion) {
        try {
            ReportesService.Exportacion file = reportes.exportar(principal.id(), true, request,
                    request.desde(), request.hasta(), ciudad, rol, estado, estadoPublicacion);
            return archivo(file);
        } catch (RuntimeException exception) {
            auditoria.registrarFallo(principal.id(), "REPORTE_EXPORTADO_" + request.formato(), "REPORTE_GESTION");
            throw exception;
        }
    }

    @PostMapping("/anfitrion/exportaciones")
    @PreAuthorize("hasRole('ANFITRION') and hasAuthority('ACCOUNT_ACTIVA')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> exportarAnfitrion(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ExportarReporteRequest request) {
        try {
            ReportesService.Exportacion file = reportes.exportar(principal.id(), false, request,
                    request.desde(), request.hasta(), null, null, null, null);
            return archivo(file);
        } catch (RuntimeException exception) {
            auditoria.registrarFallo(principal.id(), "REPORTE_EXPORTADO_" + request.formato(), "REPORTE_GESTION");
            throw exception;
        }
    }

    private ResponseEntity<byte[]> archivo(ReportesService.Exportacion file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.nombre()).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .body(file.contenido());
    }
}
