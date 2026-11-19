package softplasty.marketplace.modules.reports.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import softplasty.marketplace.modules.accommodations.domain.Alojamiento;
import softplasty.marketplace.modules.accommodations.domain.EstadoPublicacion;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.AlojamientoRepository;
import softplasty.marketplace.modules.accommodations.infrastructure.persistence.PublicacionRepository;
import softplasty.marketplace.modules.reports.api.ExportarReporteRequest;
import softplasty.marketplace.modules.reports.api.ReportesResponse;
import softplasty.marketplace.modules.reports.domain.EventoAuditoria;
import softplasty.marketplace.modules.reports.domain.ReporteGestion;
import softplasty.marketplace.modules.reports.infrastructure.persistence.EventoAuditoriaRepository;
import softplasty.marketplace.modules.reports.infrastructure.persistence.ReporteGestionRepository;
import softplasty.marketplace.modules.users.domain.EstadoCuenta;
import softplasty.marketplace.modules.users.domain.RolUsuario;
import softplasty.marketplace.modules.users.infrastructure.persistence.UsuarioRepository;

@Service
public class ReportesService {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(0);
    private static final String NOTA_SIMULACION = "Reservas simuladas para demostración; no son transacciones reales. "
            + "La tarifa aplicada es ficticia, configurable y solo ilustra la ganancia neta.";

    private final UsuarioRepository usuarios;
    private final AlojamientoRepository alojamientos;
    private final PublicacionRepository publicaciones;
    private final ReporteGestionRepository reportes;
    private final EventoAuditoriaRepository eventos;
    private final EventoAuditoriaService auditoria;
    private final ObjectProvider<SimuladorReservas> simuladorProvider;
    private final Clock clock;

    public ReportesService(UsuarioRepository usuarios, AlojamientoRepository alojamientos,
                           PublicacionRepository publicaciones, ReporteGestionRepository reportes,
                           EventoAuditoriaRepository eventos, EventoAuditoriaService auditoria,
                           ObjectProvider<SimuladorReservas> simuladorProvider, Clock clock) {
        this.usuarios = usuarios; this.alojamientos = alojamientos; this.publicaciones = publicaciones;
        this.reportes = reportes; this.eventos = eventos; this.auditoria = auditoria;
        this.simuladorProvider = simuladorProvider; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReportesResponse.Admin administrador(LocalDate desde, LocalDate hasta,
                                                  String ciudad, RolUsuario rol,
                                                  EstadoCuenta estado,
                                                  EstadoPublicacion estadoPublicacion) {
        Rango rango = rango(desde, hasta);
        Map<String, Long> porRol = new LinkedHashMap<>();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        long usuariosTotales = 0;
        for (RolUsuario r : RolUsuario.values()) {
            long count = rol != null && rol != r ? 0 : estado == null
                    ? usuarios.countByRol(r) : usuarios.countByRolAndEstado(r, estado);
            porRol.put(r.name(), count);
            usuariosTotales += count;
        }
        for (EstadoCuenta e : EstadoCuenta.values()) {
            long count = estado != null && estado != e ? 0 : rol == null
                    ? usuarios.countByEstado(e) : usuarios.countByRolAndEstado(rol, e);
            porEstado.put(e.name(), count);
        }

        List<ReportesResponse.MesUsuarios> nuevos = new ArrayList<>();
        for (YearMonth mes = YearMonth.from(rango.desde()); !mes.isAfter(YearMonth.from(rango.hasta())); mes = mes.plusMonths(1)) {
            LocalDate inicioMes = mes.atDay(1).isBefore(rango.desde()) ? rango.desde() : mes.atDay(1);
            LocalDate finMesExclusivo = mes.plusMonths(1).atDay(1).isAfter(rango.hasta().plusDays(1))
                    ? rango.hasta().plusDays(1) : mes.plusMonths(1).atDay(1);
            Map<String, Long> porMesRol = new LinkedHashMap<>();
            long total = 0;
            for (RolUsuario r : RolUsuario.values()) {
                long count = rol != null && rol != r ? 0 : estado == null
                        ? usuarios.countByRolAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(
                                r, inicio(inicioMes), inicio(finMesExclusivo))
                        : usuarios.countByRolAndEstadoAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(
                                r, estado, inicio(inicioMes), inicio(finMesExclusivo));
                porMesRol.put(r.name(), count); total += count;
            }
            nuevos.add(new ReportesResponse.MesUsuarios(mes, total, porMesRol));
        }

        Map<String, Long> publicacionesPorEstado = new LinkedHashMap<>();
        for (EstadoPublicacion e : EstadoPublicacion.values()) {
            if (estadoPublicacion != null && estadoPublicacion != e) {
                publicacionesPorEstado.put(e.name(), 0L);
            } else {
                long count = ciudad == null || ciudad.isBlank()
                        ? publicaciones.countByEstado(e)
                        : publicaciones.countByEstadoAndAlojamiento_CiudadIgnoreCase(e, ciudad.trim());
                publicacionesPorEstado.put(e.name(), count);
            }
        }

        SimuladorReservas simulador = simuladorProvider.getIfAvailable();
        if (simulador == null) {
            return new ReportesResponse.Admin(rango.desde(), rango.hasta(), usuariosTotales,
                    porRol, porEstado, nuevos, publicacionesPorEstado, false,
                    "La serie de reservas no se incluye fuera de dev/test/demo.", List.of());
        }
        List<Alojamiento> propiedades = alojamientos.findAll();
        if (ciudad != null && !ciudad.isBlank()) {
            propiedades = propiedades.stream().filter(a -> a.getCiudad().equalsIgnoreCase(ciudad.trim())).toList();
        }
        if (estadoPublicacion != null) {
            propiedades = propiedades.stream().filter(a -> a.getPublicacion() != null
                    && a.getPublicacion().getEstado() == estadoPublicacion).toList();
        }
        List<ReservaSimulada> reservas = simulador.generar(propiedades,
                YearMonth.from(rango.desde()), YearMonth.from(rango.hasta())).stream()
                .filter(r -> !r.checkIn().isBefore(rango.desde()) && !r.checkIn().isAfter(rango.hasta()))
                .toList();
        List<ReportesResponse.MesReservasAdmin> serie = new ArrayList<>();
        for (YearMonth mes = YearMonth.from(rango.desde()); !mes.isAfter(YearMonth.from(rango.hasta())); mes = mes.plusMonths(1)) {
            YearMonth mesActual = mes;
            List<ReservaSimulada> delMes = reservas.stream().filter(r -> YearMonth.from(r.checkIn()).equals(mesActual)).toList();
            serie.add(new ReportesResponse.MesReservasAdmin(mes,
                    delMes.stream().filter(ReservaSimulada::cuentaComoIngreso).count(),
                    suma(delMes.stream().filter(ReservaSimulada::cuentaComoIngreso).map(ReservaSimulada::valorBruto).toList())));
        }
        return new ReportesResponse.Admin(rango.desde(), rango.hasta(), usuariosTotales,
                porRol, porEstado, nuevos, publicacionesPorEstado, true, NOTA_SIMULACION, serie);
    }

    @Transactional(readOnly = true)
    public ReportesResponse.Host anfitrion(UUID anfitrionId, LocalDate desde, LocalDate hasta, UUID alojamientoId) {
        Rango rango = rango(desde, hasta);
        List<Alojamiento> propiedades = alojamientos.findAllByAnfitrion_IdOrderByCiudadAsc(anfitrionId);
        if (alojamientoId != null) {
            propiedades = propiedades.stream().filter(a -> a.getId().equals(alojamientoId)).toList();
            if (propiedades.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El alojamiento no existe o no pertenece a esta cuenta.");
            }
        }
        SimuladorReservas simulador = simuladorProvider.getIfAvailable();
        if (simulador == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Los reportes de reservas simuladas solo están habilitados en dev/test/demo.");
        }
        List<ReservaSimulada> reservas = simulador.generar(propiedades,
                YearMonth.from(rango.desde()), YearMonth.from(rango.hasta())).stream()
                .filter(r -> !r.checkIn().isBefore(rango.desde()) && !r.checkIn().isAfter(rango.hasta()))
                .toList();
        List<ReportesResponse.MesReservasHost> mensual = new ArrayList<>();
        for (YearMonth mes = YearMonth.from(rango.desde()); !mes.isAfter(YearMonth.from(rango.hasta())); mes = mes.plusMonths(1)) {
            YearMonth mesActual = mes;
            List<ReservaSimulada> delMes = reservas.stream().filter(r -> YearMonth.from(r.checkIn()).equals(mesActual)).toList();
            List<ReservaSimulada> ganadoras = delMes.stream().filter(ReservaSimulada::cuentaComoIngreso).toList();
            mensual.add(new ReportesResponse.MesReservasHost(mes, ganadoras.size(),
                    delMes.stream().filter(r -> r.estado() == ReservaSimulada.Estado.CANCELADA).count(),
                    suma(ganadoras.stream().map(ReservaSimulada::valorBruto).toList()),
                    suma(ganadoras.stream().map(ReservaSimulada::tarifa).toList()),
                    suma(ganadoras.stream().map(ReservaSimulada::neto).toList())));
        }
        List<ReportesResponse.AlojamientoHost> porPropiedad = new ArrayList<>();
        for (Alojamiento propiedad : propiedades) {
            List<ReservaSimulada> delAlojamiento = reservas.stream()
                    .filter(r -> r.alojamientoId().equals(propiedad.getId())).toList();
            List<ReservaSimulada> ganadoras = delAlojamiento.stream().filter(ReservaSimulada::cuentaComoIngreso).toList();
            String titulo = propiedad.getPublicacion() == null ? propiedad.getTipo() : propiedad.getPublicacion().getTitulo();
            porPropiedad.add(new ReportesResponse.AlojamientoHost(propiedad.getId(), titulo, ganadoras.size(),
                    delAlojamiento.stream().filter(r -> r.estado() == ReservaSimulada.Estado.CANCELADA).count(),
                    suma(ganadoras.stream().map(ReservaSimulada::valorBruto).toList()),
                    suma(ganadoras.stream().map(ReservaSimulada::tarifa).toList()),
                    suma(ganadoras.stream().map(ReservaSimulada::neto).toList())));
        }
        List<ReservaSimulada> ganadoras = reservas.stream().filter(ReservaSimulada::cuentaComoIngreso).toList();
        return new ReportesResponse.Host(rango.desde(), rango.hasta(), true, NOTA_SIMULACION,
                ganadoras.size(), reservas.stream().filter(r -> r.estado() == ReservaSimulada.Estado.CANCELADA).count(),
                suma(ganadoras.stream().map(ReservaSimulada::valorBruto).toList()),
                suma(ganadoras.stream().map(ReservaSimulada::tarifa).toList()),
                suma(ganadoras.stream().map(ReservaSimulada::neto).toList()), mensual, porPropiedad);
    }

    @Transactional(readOnly = true)
    public List<ReportesResponse.Evento> actividad(LocalDate desde, LocalDate hasta, String accion) {
        Rango rango = rango(desde, hasta);
        return eventos.findAllByOcurridoEnGreaterThanEqualAndOcurridoEnLessThanOrderByOcurridoEnDesc(
                        inicio(rango.desde()), inicio(rango.hasta().plusDays(1))).stream()
                .filter(e -> accion == null || accion.isBlank() || e.getAccion().equalsIgnoreCase(accion.trim()))
                .map(e -> new ReportesResponse.Evento(e.getId(), e.getActorId(), e.getAccion(), e.getEntidad(),
                        e.getEntidadId(), e.getResultado(), e.getOcurridoEn())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportesResponse.Exportacion> historial(UUID solicitanteId, boolean administrador) {
        var guardados = administrador ? reportes.findAllByOrderByGeneradoEnDesc()
                : reportes.findAllBySolicitanteIdOrderByGeneradoEnDesc(solicitanteId);
        return guardados.stream().map(r -> new ReportesResponse.Exportacion(r.getId(), r.getTipo(),
                r.getFormato(), r.getFiltros(), r.getGeneradoEn())).toList();
    }

    @Transactional
    public Exportacion exportar(UUID actorId, boolean admin, ExportarReporteRequest request,
                                LocalDate desde, LocalDate hasta, String ciudad,
                                RolUsuario rol, EstadoCuenta estado, EstadoPublicacion estadoPublicacion) {
        Rango rango = rango(desde, hasta);
        String filtros = "desde=" + rango.desde() + ";hasta=" + rango.hasta()
                + (request.alojamientoId() == null ? "" : ";alojamientoId=" + request.alojamientoId())
                + (ciudad == null ? "" : ";ciudad=" + ciudad)
                + (rol == null ? "" : ";rol=" + rol)
                + (estado == null ? "" : ";estado=" + estado)
                + (estadoPublicacion == null ? "" : ";estadoPublicacion=" + estadoPublicacion);
        byte[] bytes;
        String tipo = admin ? "ADMINISTRADOR" : "ANFITRION";
        String filename = "reporte-" + tipo.toLowerCase() + "-" + rango.desde() + "-" + rango.hasta()
                + (request.formato() == ExportarReporteRequest.FormatoReporte.PDF ? ".pdf" : ".csv");
        if (admin) {
            ReportesResponse.Admin data = administrador(rango.desde(), rango.hasta(), ciudad, rol, estado, estadoPublicacion);
            bytes = request.formato() == ExportarReporteRequest.FormatoReporte.CSV
                    ? csvAdmin(data) : pdfAdmin(data);
        } else {
            ReportesResponse.Host data = anfitrion(actorId, rango.desde(), rango.hasta(), request.alojamientoId());
            bytes = request.formato() == ExportarReporteRequest.FormatoReporte.CSV
                    ? csvHost(data) : pdfHost(data);
        }
        reportes.save(new ReporteGestion(actorId, tipo, request.formato().name(), filtros, Instant.now(clock)));
        auditoria.registrar(actorId, "REPORTE_EXPORTADO_" + request.formato(), "REPORTE_GESTION", null);
        return new Exportacion(filename, request.formato() == ExportarReporteRequest.FormatoReporte.PDF
                ? "application/pdf" : "text/csv; charset=UTF-8", bytes);
    }

    private byte[] csvAdmin(ReportesResponse.Admin d) {
        StringBuilder csv = new StringBuilder("seccion,indicador,periodo,valor\r\n");
        csv.append("usuarios,total,,").append(d.usuariosTotales()).append("\r\n");
        d.usuariosPorRol().forEach((k,v) -> row(csv, "usuarios_por_rol", k, "", v.toString()));
        d.cuentasPorEstado().forEach((k,v) -> row(csv, "cuentas_por_estado", k, "", v.toString()));
        d.publicacionesPorEstado().forEach((k,v) -> row(csv, "publicaciones_por_estado", k, "", v.toString()));
        for (var m : d.nuevosUsuariosPorMes()) row(csv, "usuarios_nuevos", "total", m.mes().toString(), Long.toString(m.nuevos()));
        for (var m : d.reservasPorMes()) {
            row(csv, "reservas_simuladas", "cantidad_confirmadas_completadas", m.mes().toString(), Long.toString(m.reservas()));
            row(csv, "reservas_simuladas", "valor_bruto_COP", m.mes().toString(), m.valorBruto().toPlainString());
        }
        return csvBytes(csv);
    }

    private byte[] csvHost(ReportesResponse.Host d) {
        StringBuilder csv = new StringBuilder("seccion,alojamiento,periodo,reservas,valor_bruto_COP,tarifa_ficticia_COP,ganancia_neta_estimada_COP\r\n");
        for (var m : d.reservasPorMes()) csv.append("mensual,,").append(csvCell(m.mes().toString())).append(',')
                .append(m.confirmadasYCompletadas()).append(',').append(m.valorBruto()).append(',')
                .append(m.tarifaFicticia()).append(',').append(m.gananciaNetaEstimada()).append("\r\n");
        for (var a : d.porAlojamiento()) csv.append("alojamiento,").append(csvCell(a.titulo())).append(",,")
                .append(a.confirmadasYCompletadas()).append(',').append(a.valorBruto()).append(',')
                .append(a.tarifaFicticia()).append(',').append(a.gananciaNetaEstimada()).append("\r\n");
        return csvBytes(csv);
    }

    private byte[] csvBytes(StringBuilder csv) {
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }
    private void row(StringBuilder out, String a, String b, String c, String d) {
        out.append(csvCell(a)).append(',').append(csvCell(b)).append(',').append(csvCell(c))
                .append(',').append(csvCell(d)).append("\r\n");
    }
    private String csvCell(String text) {
        String value = text == null ? "" : text;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private byte[] pdfAdmin(ReportesResponse.Admin d) {
        List<String> lines = new ArrayList<>();
        lines.add("REPORTE ADMINISTRATIVO");
        lines.add("Periodo: " + d.desde() + " a " + d.hasta());
        lines.add("Usuarios registrados: " + d.usuariosTotales());
        d.usuariosPorRol().forEach((k,v) -> lines.add("Usuarios " + k + ": " + v));
        d.cuentasPorEstado().forEach((k,v) -> lines.add("Cuentas " + k + ": " + v));
        d.publicacionesPorEstado().forEach((k,v) -> lines.add("Publicaciones " + k + ": " + v));
        lines.add("Usuarios nuevos por mes:");
        d.nuevosUsuariosPorMes().forEach(m -> lines.add(m.mes() + " - " + m.nuevos()));
        lines.add("Reservas simuladas por mes:");
        d.reservasPorMes().forEach(m -> lines.add(m.mes() + " - " + m.reservas() + " - COP " + m.valorBruto()));
        if (d.reservasSimuladas()) lines.add("NOTA: datos simulados; tarifa ilustrativa configurable, no real.");
        return pdf(lines);
    }
    private byte[] pdfHost(ReportesResponse.Host d) {
        List<String> lines = new ArrayList<>();
        lines.add("REPORTE DEL ANFITRION");
        lines.add("Periodo: " + d.desde() + " a " + d.hasta());
        lines.add("Reservas confirmadas y completadas: " + d.reservasConfirmadasYCompletadas());
        lines.add("Reservas canceladas: " + d.reservasCanceladas());
        lines.add("Valor bruto: COP " + d.valorBruto());
        lines.add("Tarifa ficticia: COP " + d.tarifaFicticia());
        lines.add("Ganancia neta estimada: COP " + d.gananciaNetaEstimada());
        lines.add("Desglose por alojamiento:");
        d.porAlojamiento().forEach(a -> lines.add(a.titulo() + " - " + a.confirmadasYCompletadas()
                + " reservas - neto COP " + a.gananciaNetaEstimada()));
        lines.add("NOTA: datos simulados; tarifa ilustrativa configurable, no real.");
        return pdf(lines);
    }

    private byte[] pdf(List<String> lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int start = 0; start < lines.size(); start += 42) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    stream.setLeading(16);
                    stream.newLineAtOffset(50, 740);
                    for (int i = start; i < Math.min(start + 42, lines.size()); i++) {
                        String safe = lines.get(i).replaceAll("[^\\x20-\\x7E]", "?");
                        if (safe.length() > 95) safe = safe.substring(0, 95);
                        stream.showText(safe);
                        stream.newLine();
                    }
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF.", exception);
        }
    }

    private Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock.withZone(ZONA));
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? YearMonth.from(fin).minusMonths(11).atDay(1) : desde;
        if (inicio.isAfter(fin)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La fecha desde debe ser anterior o igual a hasta.");
        if (inicio.isBefore(fin.minusYears(5))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El rango máximo permitido para un reporte es de cinco años.");
        return new Rango(inicio, fin);
    }
    private Instant inicio(LocalDate date) { return date.atStartOfDay(ZONA).toInstant(); }
    private BigDecimal suma(List<BigDecimal> valores) {
        return valores.stream().reduce(CERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }
    private record Rango(LocalDate desde, LocalDate hasta) {}
    public record Exportacion(String nombre, String contentType, byte[] contenido) {}
}
