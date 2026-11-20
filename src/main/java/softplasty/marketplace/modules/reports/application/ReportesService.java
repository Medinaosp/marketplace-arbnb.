package softplasty.marketplace.modules.reports.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.awt.Color;

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
        StringBuilder csv = csvHeader("ADMINISTRADOR", d.desde(), d.hasta(), d.reservasSimuladas(), d.notaReservas());
        csvRow(csv, "resumen", "", "", "usuarios_registrados", d.usuariosTotales(), "cuentas", "");
        d.usuariosPorRol().forEach((k, v) -> csvRow(csv, "usuarios_por_rol", "", k, "cuentas", v, "cuentas", ""));
        d.cuentasPorEstado().forEach((k, v) -> csvRow(csv, "cuentas_por_estado", "", k, "cuentas", v, "cuentas", ""));
        d.publicacionesPorEstado().forEach((k, v) -> csvRow(csv, "publicaciones_por_estado", "", k, "publicaciones", v, "publicaciones", ""));
        Map<String, Long> users = new LinkedHashMap<>();
        d.nuevosUsuariosPorMes().forEach(m -> users.put(m.mes().toString(), m.nuevos()));
        Map<String, Long> bookings = new LinkedHashMap<>();
        d.reservasPorMes().forEach(m -> bookings.put(m.mes().toString(), m.reservas()));
        appendChartRows(csv, "grafica_usuarios_nuevos", users, "cuentas");
        appendChartRows(csv, "grafica_reservas_simuladas", bookings, "reservas");
        d.reservasPorMes().forEach(m -> csvRow(csv, "detalle_mensual", m.mes().toString(), "", "valor_bruto_estimado", m.valorBruto(), "COP", ""));
        return csvBytes(csv);
    }

    private byte[] csvHost(ReportesResponse.Host d) {
        StringBuilder csv = csvHeader("ANFITRION", d.desde(), d.hasta(), d.reservasSimuladas(), d.notaReservas());
        csvRow(csv, "resumen", "", "", "reservas_confirmadas_completadas", d.reservasConfirmadasYCompletadas(), "reservas", "");
        csvRow(csv, "resumen", "", "", "reservas_canceladas", d.reservasCanceladas(), "reservas", "");
        csvRow(csv, "resumen", "", "", "valor_bruto", d.valorBruto(), "COP", "");
        csvRow(csv, "resumen", "", "", "tarifa_ficticia", d.tarifaFicticia(), "COP", "");
        csvRow(csv, "resumen", "", "", "ganancia_neta_estimada", d.gananciaNetaEstimada(), "COP", "");
        Map<String, Long> bookings = new LinkedHashMap<>();
        Map<String, BigDecimal> earnings = new LinkedHashMap<>();
        for (var m : d.reservasPorMes()) {
            String month = m.mes().toString();
            bookings.put(month, m.confirmadasYCompletadas());
            earnings.put(month, m.gananciaNetaEstimada());
            csvRow(csv, "detalle_mensual", month, "", "confirmadas_y_completadas", m.confirmadasYCompletadas(), "reservas", "");
            csvRow(csv, "detalle_mensual", month, "", "canceladas", m.canceladas(), "reservas", "");
            csvRow(csv, "detalle_mensual", month, "", "valor_bruto", m.valorBruto(), "COP", "");
            csvRow(csv, "detalle_mensual", month, "", "tarifa_ficticia", m.tarifaFicticia(), "COP", "");
            csvRow(csv, "detalle_mensual", month, "", "ganancia_neta_estimada", m.gananciaNetaEstimada(), "COP", "");
        }
        appendChartRows(csv, "grafica_reservas_confirmadas", bookings, "reservas");
        appendChartRows(csv, "grafica_ganancia_neta_estimada", earnings, "COP");
        for (var a : d.porAlojamiento()) {
            csvRow(csv, "por_alojamiento", "", a.titulo(), "confirmadas_y_completadas", a.confirmadasYCompletadas(), "reservas", "");
            csvRow(csv, "por_alojamiento", "", a.titulo(), "canceladas", a.canceladas(), "reservas", "");
            csvRow(csv, "por_alojamiento", "", a.titulo(), "valor_bruto", a.valorBruto(), "COP", "");
            csvRow(csv, "por_alojamiento", "", a.titulo(), "tarifa_ficticia", a.tarifaFicticia(), "COP", "");
            csvRow(csv, "por_alojamiento", "", a.titulo(), "ganancia_neta_estimada", a.gananciaNetaEstimada(), "COP", "");
        }
        return csvBytes(csv);
    }

    private StringBuilder csvHeader(String role, LocalDate desde, LocalDate hasta, boolean simulated, String note) {
        StringBuilder csv = new StringBuilder("sep=,\r\nseccion,periodo,categoria,indicador,valor,unidad,barra_visual\r\n");
        csvRow(csv, "reporte", "", "", "rol", role, "", "");
        csvRow(csv, "reporte", "", "", "periodo_desde", desde, "", "");
        csvRow(csv, "reporte", "", "", "periodo_hasta", hasta, "", "");
        csvRow(csv, "reporte", "", "", "datos_simulados", simulated, "", "");
        if (note != null && !note.isBlank()) csvRow(csv, "reporte", "", "", "nota", note, "", "");
        csv.append("\r\n");
        return csv;
    }

    private void appendChartRows(StringBuilder csv, String name, Map<String, ? extends Number> values, String unit) {
        double max = values.values().stream().mapToDouble(Number::doubleValue).max().orElse(0);
        values.forEach((period, value) -> {
            int blocks = max <= 0 ? 0 : (int) Math.round(value.doubleValue() / max * 20);
            csvRow(csv, name, period, "", "serie", value, unit, "█".repeat(blocks));
        });
    }

    private void csvRow(StringBuilder csv, Object section, Object period, Object category, Object metric,
                        Object value, Object unit, Object bar) {
        csv.append(csvCell(String.valueOf(section))).append(',').append(csvCell(String.valueOf(period))).append(',')
                .append(csvCell(String.valueOf(category))).append(',').append(csvCell(String.valueOf(metric))).append(',')
                .append(csvCell(String.valueOf(value))).append(',').append(csvCell(String.valueOf(unit))).append(',')
                .append(csvCell(String.valueOf(bar))).append("\r\n");
    }

    private byte[] csvBytes(StringBuilder csv) { return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8); }
    private String csvCell(String text) {
        String value = text == null || "null".equals(text) ? "" : text;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private byte[] pdfAdmin(ReportesResponse.Admin d) {
        List<PdfMetric> metrics = List.of(new PdfMetric("Cuentas registradas", number(d.usuariosTotales())),
                new PdfMetric("Huespedes", number(d.usuariosPorRol().getOrDefault("HUESPED", 0L))),
                new PdfMetric("Anfitriones", number(d.usuariosPorRol().getOrDefault("ANFITRION", 0L))),
                new PdfMetric("Publicaciones pendientes", number(d.publicacionesPorEstado().getOrDefault("PENDIENTE_REVISION", 0L))));
        List<ChartPoint> users = d.nuevosUsuariosPorMes().stream().map(m -> new ChartPoint(m.mes().toString(), m.nuevos())).toList();
        List<ChartPoint> bookings = d.reservasPorMes().stream().map(m -> new ChartPoint(m.mes().toString(), m.reservas())).toList();
        List<List<String>> table = new ArrayList<>();
        d.usuariosPorRol().forEach((k, v) -> table.add(List.of("Cuentas por rol", k, number(v))));
        d.cuentasPorEstado().forEach((k, v) -> table.add(List.of("Cuentas por estado", k, number(v))));
        d.publicacionesPorEstado().forEach((k, v) -> table.add(List.of("Publicaciones", k.replace('_', ' '), number(v))));
        return buildPdf("REPORTE DE ADMINISTRACION", d.desde(), d.hasta(), metrics,
                d.reservasSimuladas() ? d.notaReservas() : "La serie de reservas no esta incluida en este entorno.",
                List.of(new PdfChart("Nuevas cuentas por mes", "Cuentas", users, GREEN),
                        new PdfChart("Reservas simuladas por mes", "Reservas", bookings, ORANGE)),
                List.of("Indicador", "Categoria", "Total"), table);
    }

    private byte[] pdfHost(ReportesResponse.Host d) {
        List<PdfMetric> metrics = List.of(new PdfMetric("Reservas confirmadas", number(d.reservasConfirmadasYCompletadas())),
                new PdfMetric("Reservas canceladas", number(d.reservasCanceladas())),
                new PdfMetric("Valor bruto estimado", money(d.valorBruto())),
                new PdfMetric("Ganancia neta estimada", money(d.gananciaNetaEstimada())));
        List<ChartPoint> bookings = d.reservasPorMes().stream().map(m -> new ChartPoint(m.mes().toString(), m.confirmadasYCompletadas())).toList();
        List<ChartPoint> earnings = d.reservasPorMes().stream().map(m -> new ChartPoint(m.mes().toString(), m.gananciaNetaEstimada().doubleValue())).toList();
        List<List<String>> table = d.porAlojamiento().stream().map(a -> List.of(a.titulo(), number(a.confirmadasYCompletadas()),
                number(a.canceladas()), money(a.gananciaNetaEstimada()))).toList();
        return buildPdf("REPORTE DE ANFITRION", d.desde(), d.hasta(), metrics, d.notaReservas(),
                List.of(new PdfChart("Reservas confirmadas por mes", "Reservas", bookings, GREEN),
                        new PdfChart("Ganancia neta estimada por mes", "COP", earnings, ORANGE)),
                List.of("Alojamiento", "Confirmadas", "Canceladas", "Ganancia neta estimada"), table);
    }

    private static final Color GREEN = new Color(28, 91, 67);
    private static final Color ORANGE = new Color(193, 86, 40);
    private static final Color INK = new Color(35, 48, 41);
    private static final Color MUTED = new Color(103, 116, 106);
    private static final Color PALE = new Color(242, 246, 241);
    private byte[] buildPdf(String title, LocalDate desde, LocalDate hasta, List<PdfMetric> metrics,
                            String note, List<PdfChart> charts, List<String> headers, List<List<String>> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            drawSummaryPage(document, title, desde, hasta, metrics, note);
            int chartPages = charts.stream().mapToInt(c -> Math.max(1, (c.points().size() + 11) / 12)).max().orElse(1);
            for (int pageIndex = 0; pageIndex < chartPages; pageIndex++) {
                PDPage page = new PDPage(PDRectangle.LETTER); document.addPage(page);
                try (PDPageContentStream s = new PDPageContentStream(document, page)) {
                    drawPageHeader(s, title, desde, hasta);
                    drawText(s, "TENDENCIAS DEL PERIODO", 44, 682, 10, GREEN, true);
                    int chartIndex = 0;
                    for (PdfChart chart : charts) {
                        int start = pageIndex * 12;
                        List<ChartPoint> points = chart.points().stream().skip(start).limit(12).toList();
                        if (!points.isEmpty() || pageIndex == 0 && chart.points().isEmpty()) {
                            drawChart(s, chart, points, 44, 655 - chartIndex * 276, 524, 250);
                            chartIndex++;
                        }
                    }
                    drawFooter(s, document.getNumberOfPages());
                }
            }
            if (rows.isEmpty()) rows = List.of(List.of("No hay registros para mostrar"));
            for (int start = 0; start < rows.size(); start += 22) {
                PDPage page = new PDPage(PDRectangle.LETTER); document.addPage(page);
                try (PDPageContentStream s = new PDPageContentStream(document, page)) {
                    drawPageHeader(s, title, desde, hasta);
                    drawText(s, "DETALLE DEL REPORTE", 44, 682, 10, GREEN, true);
                    drawTable(s, headers, rows.subList(start, Math.min(start + 22, rows.size())), 44, 653, 524);
                    drawFooter(s, document.getNumberOfPages());
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF.", exception);
        }
    }

    private void drawSummaryPage(PDDocument document, String title, LocalDate desde, LocalDate hasta,
                                 List<PdfMetric> metrics, String note) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER); document.addPage(page);
        try (PDPageContentStream s = new PDPageContentStream(document, page)) {
            drawPageHeader(s, title, desde, hasta);
            drawText(s, "RESUMEN EJECUTIVO", 44, 682, 10, GREEN, true);
            float gap = 10, width = (524 - gap * 3) / 4;
            for (int i = 0; i < metrics.size(); i++) {
                float x = 44 + i * (width + gap);
                s.setNonStrokingColor(PALE); s.addRect(x, 586, width, 78); s.fill();
                drawText(s, metrics.get(i).label(), x + 10, 645, 8, MUTED, false);
                drawText(s, fitText(metrics.get(i).value(), width - 20, 13), x + 10, 620, 13, INK, true);
            }
            s.setNonStrokingColor(PALE); s.addRect(44, 470, 524, 91); s.fill();
            drawText(s, "CRITERIOS Y ALCANCE", 58, 540, 9, GREEN, true);
            List<String> noteLines = wrap(note == null || note.isBlank() ? "Indicadores calculados para el periodo indicado." : note, 105);
            int y = 520;
            for (String line : noteLines.stream().limit(3).toList()) { drawText(s, line, 58, y, 9, INK, false); y -= 15; }
            drawText(s, "Generado: " + LocalDate.now(clock.withZone(ZONA)), 44, 80, 8, MUTED, false);
            drawFooter(s, document.getNumberOfPages());
        }
    }

    private void drawPageHeader(PDPageContentStream s, String title, LocalDate desde, LocalDate hasta) throws IOException {
        s.setNonStrokingColor(GREEN); s.addRect(0, 700, 612, 92); s.fill();
        drawText(s, "PATIO  /  CENTRO DE INFORMES", 44, 766, 8, Color.WHITE, true);
        drawText(s, title, 44, 739, 16, Color.WHITE, true);
        drawText(s, "Periodo: " + desde + " a " + hasta, 44, 716, 9, new Color(225, 239, 229), false);
    }

    private void drawChart(PDPageContentStream s, PdfChart chart, List<ChartPoint> points,
                           float x, float top, float width, float height) throws IOException {
        s.setNonStrokingColor(PALE); s.addRect(x, top - height, width, height); s.fill();
        drawText(s, chart.title(), x + 13, top - 22, 11, INK, true);
        double max = points.stream().mapToDouble(ChartPoint::value).max().orElse(0);
        if (max <= 0) { drawText(s, "Sin actividad registrada en este periodo.", x + 14, top - 55, 9, MUTED, false); return; }
        float rowY = top - 48;
        for (ChartPoint point : points) {
            drawText(s, point.label(), x + 13, rowY, 8, MUTED, false);
            float barX = x + 104, barWidth = (float) Math.max(2, (width - 168) * point.value() / max);
            s.setNonStrokingColor(chart.color()); s.addRect(barX, rowY - 3, barWidth, 9); s.fill();
            String value = chart.unit().equals("COP") ? money(BigDecimal.valueOf(point.value())) : number((long) point.value());
            drawText(s, fitText(value, 51, 7), x + width - 55, rowY, 7, INK, true);
            rowY -= 15;
        }
    }

    private void drawTable(PDPageContentStream s, List<String> headers, List<List<String>> rows,
                           float x, float top, float width) throws IOException {
        float[] widths = new float[headers.size()];
        for (int i = 0; i < widths.length; i++) widths[i] = width / headers.size();
        s.setNonStrokingColor(GREEN); s.addRect(x, top - 25, width, 25); s.fill();
        float cx = x;
        for (int i = 0; i < headers.size(); i++) { drawText(s, headers.get(i), cx + 7, top - 16, 8, Color.WHITE, true); cx += widths[i]; }
        float y = top - 25;
        for (int r = 0; r < rows.size(); r++) {
            y -= 23;
            if (r % 2 == 0) { s.setNonStrokingColor(PALE); s.addRect(x, y, width, 23); s.fill(); }
            cx = x; List<String> row = rows.get(r);
            for (int i = 0; i < headers.size(); i++) {
                String value = i < row.size() ? row.get(i) : "";
                drawText(s, fitText(value, widths[i] - 14, 8), cx + 7, y + 8, 8, INK, false); cx += widths[i];
            }
        }
    }

    private void drawFooter(PDPageContentStream s, int pageNumber) throws IOException {
        s.setStrokingColor(new Color(220, 228, 221)); s.moveTo(44, 49); s.lineTo(568, 49); s.stroke();
        drawText(s, "PATIO  |  Documento de uso interno  |  Datos simulados cuando se indique", 44, 34, 7, MUTED, false);
        drawText(s, "Pagina " + pageNumber, 520, 34, 7, MUTED, false);
    }

    private void drawText(PDPageContentStream s, String value, float x, float y, int size, Color color, boolean bold) throws IOException {
        var font = new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace("–", "-").replace("—", "-").replace("·", "|");
        s.beginText(); s.setFont(font, size); s.setNonStrokingColor(color); s.newLineAtOffset(x, y);
        s.showText(normalized.replaceAll("[^\\x20-\\x7E]", "?")); s.endText();
    }

    private String fitText(String value, float maxWidth, int size) throws IOException {
        String safe = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        while (!safe.isEmpty() && font.getStringWidth(safe) / 1000 * size > maxWidth) safe = safe.substring(0, safe.length() - 1);
        return safe;
    }

    private List<String> wrap(String value, int width) {
        List<String> result = new ArrayList<>(); StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (line.length() + word.length() + 1 > width && !line.isEmpty()) { result.add(line.toString()); line.setLength(0); }
            if (!line.isEmpty()) line.append(' '); line.append(word);
        }
        if (!line.isEmpty()) result.add(line.toString()); return result;
    }

    private String number(long value) { return NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value); }
    private String money(BigDecimal value) { return "COP " + number(value == null ? 0 : value.longValue()); }
    private record PdfMetric(String label, String value) {}
    private record ChartPoint(String label, double value) {}
    private record PdfChart(String title, String unit, List<ChartPoint> points, Color color) {}

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
