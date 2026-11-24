package softplasty.marketplace.modules.reservations.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import softplasty.marketplace.modules.reservations.api.*;

@Service
public class ReservasService {
    private final JdbcTemplate db;
    private final Clock clock;
    public ReservasService(JdbcTemplate db, Clock clock) { this.db = db; this.clock = clock; }

    @Transactional
    public Map<String,Object> cotizar(UUID publicacionId, UUID huespedId, NuevaCotizacionRequest r) {
        if (!r.checkin().isAfter(LocalDate.now(clock)) || !r.checkout().isAfter(r.checkin()))
            throw bad("El check-in debe ser futuro y el check-out posterior.");
        if (r.huespedes() < 1) throw bad("Indica al menos un huésped.");
        Map<String,Object> pub = one("select p.id,p.precio_noche,a.id alojamiento_id,a.capacidad_maxima,p.politica_cancelacion_id,pc.nombre politica " +
                "from publicacion p join alojamiento a on a.id=p.alojamiento_id join politica_cancelacion pc on pc.id=p.politica_cancelacion_id " +
                "join usuario u on u.id=a.usuario_id where p.id=? and p.estado='ACTIVA' and u.estado='ACTIVA'", publicacionId);
        if (r.huespedes() > ((Number) pub.get("capacidad_maxima")).intValue()) throw bad("El número de huéspedes supera la capacidad del alojamiento.");
        UUID alojamientoId = (UUID) pub.get("alojamiento_id");
        if (Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from periodo_disponibilidad where alojamiento_id=? and tipo='BLOQUEADO' and inicio < ? and fin > ?)", Boolean.class, alojamientoId,r.checkout(),r.checkin())))
            throw conflict("El alojamiento está bloqueado en esas fechas.");
        if (Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from reserva where publicacion_id=? and checkin < ? and checkout > ? and ((estado='CONFIRMADA') or (estado='PENDIENTE_PAGO' and vence_pago_en > ?)))", Boolean.class,publicacionId,r.checkout(),r.checkin(),Timestamp.from(clock.instant()))))
            throw conflict("Las fechas se cruzan con otra reserva o con un pago pendiente.");
        int nights = Math.toIntExact(ChronoUnit.DAYS.between(r.checkin(),r.checkout()));
        BigDecimal subtotal = ((BigDecimal)pub.get("precio_noche")).multiply(BigDecimal.valueOf(nights));
        List<UUID> serviceIds = r.serviciosIds() == null ? List.of() : r.serviciosIds().stream().distinct().toList();
        BigDecimal serviceTotal = BigDecimal.ZERO;
        for (UUID serviceId : serviceIds) {
            Map<String,Object> rate = one("select t.monto,t.modalidad from publicacion_servicio ps join tarifa_servicio t on t.servicio_id=ps.servicio_id " +
                    "join servicio_alojamiento s on s.id=ps.servicio_id and s.activo=true where ps.publicacion_id=? and ps.servicio_id=?",publicacionId,serviceId);
            BigDecimal amount=(BigDecimal)rate.get("monto");
            serviceTotal=serviceTotal.add("POR_NOCHE".equals(rate.get("modalidad"))?amount.multiply(BigDecimal.valueOf(nights)):amount);
        }
        UUID discountId=null; BigDecimal discountValue=BigDecimal.ZERO;
        if (r.codigoDescuento()!=null && !r.codigoDescuento().isBlank()) {
            List<Map<String,Object>> discounts=db.queryForList("select id,porcentaje,noches_minimas from descuento where publicacion_id=? and upper(codigo)=upper(?) and activo=true and vigente_desde<=? and vigente_hasta>=?",publicacionId,r.codigoDescuento().trim(),r.checkin(),r.checkin());
            if (discounts.isEmpty()) throw bad("El código de descuento no es válido para este alojamiento o fecha.");
            Map<String,Object> d=discounts.get(0);
            if(nights<((Number)d.get("noches_minimas")).intValue()) throw bad("La estadía no cumple las noches mínimas del descuento.");
            discountId=(UUID)d.get("id");
            discountValue=subtotal.multiply((BigDecimal)d.get("porcentaje")).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        }
        BigDecimal total=subtotal.add(serviceTotal).subtract(discountValue).max(BigDecimal.ZERO);
        UUID id=UUID.randomUUID(); var now=Timestamp.from(clock.instant()); var expires=Timestamp.from(clock.instant().plusSeconds(30*60));
        String selectedServices=String.join(",",serviceIds.stream().map(UUID::toString).toList());
        db.update("insert into cotizacion(id,publicacion_id,huesped_id,checkin,checkout,huespedes,noches,servicios_ids,subtotal,tarifa_servicios,descuento_id,valor_descuento,total,creada_en,vence_en,estado) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'VIGENTE')",
                id,publicacionId,huespedId,r.checkin(),r.checkout(),r.huespedes(),nights,selectedServices,subtotal,serviceTotal,discountId,discountValue,total,now,expires);
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("publicacionId", publicacionId),
                Map.entry("checkin", r.checkin()),
                Map.entry("checkout", r.checkout()),
                Map.entry("huespedes", r.huespedes()),
                Map.entry("noches", nights),
                Map.entry("subtotal", subtotal),
                Map.entry("tarifaServicios", serviceTotal),
                Map.entry("valorDescuento", discountValue),
                Map.entry("total", total),
                Map.entry("venceEn", expires),
                Map.entry("pagosSimulados", true));
    }

    @Transactional
    public Map<String,Object> reservar(UUID cotizacionId, UUID guest) {
        Map<String,Object> q=one("select * from cotizacion where id=? and huesped_id=? for update",cotizacionId,guest);
        if(!"VIGENTE".equals(q.get("estado"))) throw conflict("La cotización ya fue utilizada o venció.");
        Timestamp now=Timestamp.from(clock.instant());
        if(((Timestamp)q.get("vence_en")).before(now)){ db.update("update cotizacion set estado='VENCIDA' where id=?",cotizacionId); throw conflict("La cotización venció; genera otra."); }
        UUID pub=(UUID)q.get("publicacion_id"); LocalDate in=((java.sql.Date)q.get("checkin")).toLocalDate(), out=((java.sql.Date)q.get("checkout")).toLocalDate();
        one("select id from publicacion where id=? and estado='ACTIVA' for update",pub);
        db.queryForObject("select a.id from alojamiento a join publicacion p on p.alojamiento_id=a.id where p.id=? for update of a",UUID.class,pub);
        if(Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from periodo_disponibilidad where alojamiento_id=(select alojamiento_id from publicacion where id=?) and tipo='BLOQUEADO' and inicio<? and fin>?)",Boolean.class,pub,out,in)) ||
                Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from reserva where publicacion_id=? and checkin<? and checkout>? and (estado='CONFIRMADA' or (estado='PENDIENTE_PAGO' and vence_pago_en>?)))",Boolean.class,pub,out,in,now)))
            throw conflict("El alojamiento ya no está disponible para esas fechas.");
        UUID id=UUID.randomUUID(); Timestamp hold=Timestamp.from(clock.instant().plusSeconds(15*60));
        db.update("insert into reserva(id,cotizacion_id,publicacion_id,huesped_id,checkin,checkout,noches,total_acordado,estado,creada_en,vence_pago_en) values(?,?,?,?,?,?,?,?, 'PENDIENTE_PAGO',?,?)",id,cotizacionId,pub,guest,in,out,q.get("noches"),q.get("total"),now,hold);
        db.update("update cotizacion set estado='USADA' where id=?",cotizacionId);
        return reserva(id);
    }

    @Transactional
    public Map<String,Object> pagar(UUID id, UUID guest, String result) {
        Map<String,Object> r=one("select * from reserva where id=? and huesped_id=? for update",id,guest);
        if(!"PENDIENTE_PAGO".equals(r.get("estado"))) throw conflict("La reserva no está pendiente de pago.");
        Timestamp now=Timestamp.from(clock.instant());
        if(((Timestamp)r.get("vence_pago_en")).before(now)){ db.update("update reserva set estado='EXPIRADA' where id=?",id); throw conflict("El tiempo para pagar venció; vuelve a cotizar."); }
        String status=result; UUID payment=UUID.randomUUID();
        db.update("insert into pago_simulado(id,reserva_id,monto,referencia,estado,solicitado_en,procesado_en) values(?,?,?,?,?,?,?)",payment,id,r.get("total_acordado"),"SIM-"+payment.toString().substring(0,8).toUpperCase(),status,now,now);
        db.update("update reserva set estado=?,confirmada_en=? where id=?", "APROBADO".equals(status)?"CONFIRMADA":"PAGO_FALLIDO", "APROBADO".equals(status)?now:null,id);
        return reserva(id);
    }

    @Transactional
    public Map<String,Object> cancelar(UUID id, UUID guest, String reason) {
        Map<String,Object> r=one("select r.*,p.nombre politica from reserva r join publicacion pu on pu.id=r.publicacion_id join politica_cancelacion p on p.id=pu.politica_cancelacion_id where r.id=? and r.huesped_id=? for update",id,guest);
        String state=(String)r.get("estado"); if(!List.of("PENDIENTE_PAGO","PAGO_FALLIDO","CONFIRMADA").contains(state)) throw conflict("Esta reserva no se puede cancelar en su estado actual.");
        boolean paid=Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from pago_simulado where reserva_id=? and estado='APROBADO')",Boolean.class,id));
        BigDecimal total=(BigDecimal)r.get("total_acordado"); String policy=(String)r.get("politica"); LocalDate checkin=((java.sql.Date)r.get("checkin")).toLocalDate();
        long hours=ChronoUnit.HOURS.between(clock.instant(),checkin.atStartOfDay(clock.getZone()).toInstant()); int pct;
        if(!paid) pct=0;
        else if(policy.equalsIgnoreCase("Flexible")) pct=hours>=24?100:0;
        else if(policy.equalsIgnoreCase("Moderada")) pct=hours>=24*5?100:hours>=24?50:0;
        else pct=hours>=24*14?50:0;
        BigDecimal refund=total.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        if(paid && policy.equalsIgnoreCase("Flexible") && hours<24) refund=total.subtract((BigDecimal)db.queryForObject("select precio_noche from publicacion where id=?",BigDecimal.class,r.get("publicacion_id"))).max(BigDecimal.ZERO);
        if(total.signum()>0) pct=refund.multiply(BigDecimal.valueOf(100)).divide(total,0,RoundingMode.HALF_UP).intValue();
        UUID cancelId=UUID.randomUUID(); Timestamp now=Timestamp.from(clock.instant());
        db.update("insert into cancelacion(id,reserva_id,solicitada_en,motivo,porcentaje_reembolso,devolucion_simulada,politica_aplicada) values(?,?,?,?,?,?,?)",cancelId,id,now,reason,pct,refund,policy);
        db.update("update reserva set estado='CANCELADA' where id=?",id);
        return Map.of("id",cancelId,"reservaId",id,"estado","CANCELADA","reembolsoSimulado",refund,"porcentajeReembolso",pct,"politica",policy,"aviso","No se realizó ningún movimiento de dinero real.");
    }

    @Transactional
    public List<Map<String,Object>> misReservas(UUID guest) { completarVencidas(); return db.queryForList("select r.id,r.publicacion_id,p.titulo,a.ciudad,r.checkin,r.checkout,r.noches,r.total_acordado total,r.estado,r.vence_pago_en,exists(select 1 from resena x where x.reserva_id=r.id) as \"resenaCreada\" from reserva r join publicacion p on p.id=r.publicacion_id join alojamiento a on a.id=p.alojamiento_id where r.huesped_id=? order by r.creada_en desc",guest); }

    @Transactional
    public List<Map<String,Object>> reservasAnfitrion(UUID host) { completarVencidas(); return db.queryForList("select r.id,r.publicacion_id,p.titulo,r.checkin,r.checkout,r.noches,r.total_acordado total,r.estado,u.nombre huesped,u.correo from reserva r join publicacion p on p.id=r.publicacion_id join alojamiento a on a.id=p.alojamiento_id join usuario u on u.id=r.huesped_id where a.usuario_id=? order by r.creada_en desc",host); }

    @Transactional
    public Map<String,Object> crearDescuento(UUID host, UUID publication, CrearDescuentoRequest r) {
        if(r.vigenteHasta().isBefore(r.vigenteDesde())) throw bad("La fecha final debe ser igual o posterior a la inicial.");
        ownerListing(host,publication);
        UUID id=UUID.randomUUID(); db.update("insert into descuento(id,publicacion_id,codigo,porcentaje,noches_minimas,vigente_desde,vigente_hasta,activo) values(?,?,?,?,?,?,?,true)",id,publication,r.codigo().trim().toUpperCase(),r.porcentaje(),r.nochesMinimas(),r.vigenteDesde(),r.vigenteHasta());
        return one("select * from descuento where id=?",id);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> descuentos(UUID host, UUID publication){ ownerListing(host,publication); return db.queryForList("select * from descuento where publicacion_id=? order by vigente_desde desc",publication); }
    @Transactional
    public void desactivarDescuento(UUID host, UUID publication, UUID discount){ ownerListing(host,publication); if(db.update("update descuento set activo=false where id=? and publicacion_id=?",discount,publication)==0) throw notFound("No encontramos el descuento."); }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> tarifas(){return db.queryForList("select t.servicio_id,s.codigo,s.nombre,t.monto,t.modalidad,t.actualizada_en from tarifa_servicio t join servicio_alojamiento s on s.id=t.servicio_id order by s.nombre");}
    @Transactional
    public Map<String,Object> actualizarTarifa(UUID serviceId,TarifaServicioRequest r){
        if(!List.of("POR_ESTADIA","POR_NOCHE").contains(r.modalidad())) throw bad("Modalidad debe ser POR_ESTADIA o POR_NOCHE.");
        if(db.update("update tarifa_servicio set monto=?,modalidad=?,actualizada_en=? where servicio_id=?",r.monto(),r.modalidad(),Timestamp.from(clock.instant()),serviceId)==0) throw notFound("No encontramos el servicio.");
        return one("select t.servicio_id,s.codigo,s.nombre,t.monto,t.modalidad,t.actualizada_en from tarifa_servicio t join servicio_alojamiento s on s.id=t.servicio_id where t.servicio_id=?",serviceId);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> resenasPublicas(UUID pub){return db.queryForList("select x.id,x.calificacion,x.contenido,x.creada_en,u.nombre huesped from resena x join usuario u on u.id=x.huesped_id where x.publicacion_id=? and x.estado='VISIBLE' order by x.creada_en desc",pub);}
    @Transactional
    public Map<String,Object> crearResena(UUID guest, UUID reservation, CrearResenaRequest r){
        Map<String,Object> res=one("select id,publicacion_id,estado from reserva where id=? and huesped_id=?",reservation,guest);
        completarVencidas();
        if(!"COMPLETADA".equals(one("select estado from reserva where id=?",reservation).get("estado"))) throw conflict("Solo puedes reseñar una reserva completada.");
        if(Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from resena where reserva_id=?)",Boolean.class,reservation))) throw conflict("Ya existe una reseña para esta reserva.");
        UUID id=UUID.randomUUID(); Timestamp now=Timestamp.from(clock.instant());
        db.update("insert into resena(id,reserva_id,publicacion_id,huesped_id,calificacion,contenido,estado,creada_en,actualizada_en) values(?,?,?,?,?,?,'PENDIENTE_REVISION',?,?)",id,reservation,res.get("publicacion_id"),guest,r.calificacion(),r.contenido().trim(),now,now);
        return one("select id,reserva_id,publicacion_id,huesped_id,calificacion,contenido,estado,creada_en from resena where id=?",id);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> resenasAdmin(){return db.queryForList("select x.id,x.calificacion,x.contenido,x.estado,x.creada_en,u.nombre huesped,p.titulo,p.id publicacion_id from resena x join usuario u on u.id=x.huesped_id join publicacion p on p.id=x.publicacion_id order by x.creada_en desc");}
    @Transactional
    public Map<String,Object> moderarResena(UUID id,String state){if(!List.of("VISIBLE","OCULTA").contains(state)) throw bad("El estado debe ser VISIBLE u OCULTA."); if(db.update("update resena set estado=?,actualizada_en=? where id=?",state,Timestamp.from(clock.instant()),id)==0)throw notFound("No encontramos la reseña."); return one("select id,estado from resena where id=?",id);}

    private void completarVencidas(){Timestamp now=Timestamp.from(clock.instant()); db.update("update reserva set estado='EXPIRADA' where estado='PENDIENTE_PAGO' and vence_pago_en<=?",now); db.update("update reserva set estado='COMPLETADA' where estado='CONFIRMADA' and checkout<=?",LocalDate.now(clock));}
    private Map<String,Object> reserva(UUID id){return one("select r.id,r.publicacion_id,p.titulo,r.checkin,r.checkout,r.noches,r.total_acordado total,r.estado,r.vence_pago_en from reserva r join publicacion p on p.id=r.publicacion_id where r.id=?",id);}
    private void ownerListing(UUID host,UUID publication){if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from publicacion p join alojamiento a on a.id=p.alojamiento_id where p.id=? and a.usuario_id=?)",Boolean.class,publication,host)))throw notFound("La publicación no existe.");}
    private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>> rows=db.queryForList(sql,args);if(rows.isEmpty())throw notFound("No encontramos el registro solicitado.");return rows.get(0);}
    private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
    private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
    private ResponseStatusException notFound(String text){return new ResponseStatusException(HttpStatus.NOT_FOUND,text);}
}
