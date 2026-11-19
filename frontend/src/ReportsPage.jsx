import { useEffect, useMemo, useState } from 'react';
import { Download, FileBarChart2, RefreshCw, ShieldCheck, Users, Home, CalendarDays, CircleDollarSign, ClipboardList } from 'lucide-react';
import { endpoints } from './api';
import { useAuth } from './auth';
import { Button, Message } from './components';

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const count = new Intl.NumberFormat('es-CO');
const dateValue = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const today = new Date();
const initialFilters = { desde: `${today.getFullYear()}-01-01`, hasta: dateValue(today), ciudad: '', rol: '', estado: '', estadoPublicacion: '', alojamientoId: '' };

function Metric({ icon: Icon, label, value, detail }) {
  return <article className="report-metric"><span className="report-metric-icon"><Icon size={19}/></span><span className="report-metric-label">{label}</span><strong>{value}</strong>{detail && <small>{detail}</small>}</article>;
}

function BarChart({ title, data, series, format = (value) => count.format(value) }) {
  const max = Math.max(1, ...data.flatMap(row => series.map(item => Number(row[item.key] || 0))));
  return <section className="report-card report-chart-card"><div className="report-card-heading"><div><span className="eyebrow">TENDENCIA MENSUAL</span><h2>{title}</h2></div><div className="report-legend">{series.map(item => <span key={item.key}><i className={item.color}/>{item.label}</span>)}</div></div>
    {data.length ? <div className="report-chart" role="img" aria-label={title}>{data.map((row, index) => <div className="report-month" key={`${row.mes}-${index}`} title={`${row.mes}: ${series.map(item => `${item.label} ${format(row[item.key] || 0)}`).join(', ')}`}><div className="report-bars">{series.map(item => <span key={item.key} className={`report-bar ${item.color}`} style={{ height: `${Math.max(row[item.key] ? 5 : 0, Number(row[item.key] || 0) / max * 100)}%` }}/>)}</div><small>{String(row.mes).slice(5)}</small></div>)}</div> : <p className="report-empty">No hay datos para el periodo seleccionado.</p>}
  </section>;
}

function FilterField({ label, children }) { return <label className="report-filter"><span>{label}</span>{children}</label>; }

function History({ rows, admin, activity }) {
  return <section className="report-card"><div className="report-card-heading"><div><span className="eyebrow">TRAZABILIDAD</span><h2>{admin ? 'Actividad y exportaciones' : 'Mis exportaciones'}</h2></div><ClipboardList size={20}/></div>
    {admin && activity.length > 0 && <><h3 className="report-subtitle">Actividad administrativa reciente</h3><div className="report-table-wrap"><table className="report-table"><thead><tr><th>Acción</th><th>Entidad</th><th>Resultado</th><th>Fecha</th></tr></thead><tbody>{activity.slice(0, 8).map(item => <tr key={item.id}><td>{item.accion}</td><td>{item.entidad}</td><td>{item.resultado}</td><td>{new Date(item.ocurridoEn).toLocaleString('es-CO')}</td></tr>)}</tbody></table></div></>}
    <h3 className="report-subtitle">Archivos generados</h3>{rows.length ? <div className="report-table-wrap"><table className="report-table"><thead><tr><th>Tipo</th><th>Formato</th><th>Filtros</th><th>Fecha</th></tr></thead><tbody>{rows.slice(0, 10).map(row => <tr key={row.id}><td>{row.tipo}</td><td>{row.formato}</td><td>{row.filtros}</td><td>{new Date(row.generadoEn).toLocaleString('es-CO')}</td></tr>)}</tbody></table></div> : <p className="report-empty">Todavía no hay exportaciones registradas.</p>}
  </section>;
}

function MonthlyTable({ rows, admin }) {
  return <section className="report-card"><div className="report-card-heading"><div><span className="eyebrow">DETALLE DEL PERIODO</span><h2>Resumen mes a mes</h2></div><CalendarDays size={20}/></div>
    {rows.length ? <div className="report-table-wrap"><table className="report-table"><thead><tr>{admin ? <><th>Mes</th><th>Reservas simuladas</th><th>Valor bruto estimado</th></> : <><th>Mes</th><th>Confirmadas</th><th>Canceladas</th><th>Valor bruto simulado</th><th>Tarifa ficticia</th><th>Ganancia neta estimada</th></>}</tr></thead><tbody>{rows.map((row, index) => <tr key={`${row.mes}-${index}`}><td>{row.mes}</td>{admin ? <><td>{count.format(row.reservas || 0)}</td><td>{currency.format(row.valorBruto || 0)}</td></> : <><td>{count.format(row.confirmadasYCompletadas || 0)}</td><td>{count.format(row.canceladas || 0)}</td><td>{currency.format(row.valorBruto || 0)}</td><td>{currency.format(row.tarifaFicticia || 0)}</td><td>{currency.format(row.gananciaNetaEstimada || 0)}</td></>}</tr>)}</tbody></table></div> : <p className="report-empty">No hay datos para el periodo seleccionado.</p>}
  </section>;
}

export default function ReportsPage({ role }) {
  const { session } = useAuth();
  const admin = role === 'ADMINISTRADOR';
  const [filters, setFilters] = useState(initialFilters);
  const [data, setData] = useState(null);
  const [listings, setListings] = useState([]);
  const [history, setHistory] = useState([]);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);
  const [applied, setApplied] = useState(initialFilters);

  useEffect(() => {
    let alive = true;
    setLoading(true); setError('');
    const query = { desde: applied.desde, hasta: applied.hasta };
    if (admin) Object.assign(query, { ciudad: applied.ciudad, rol: applied.rol, estado: applied.estado, estadoPublicacion: applied.estadoPublicacion });
    else if (applied.alojamientoId) query.alojamientoId = applied.alojamientoId;
    const jobs = [admin ? endpoints.adminReport(query, session.token) : endpoints.hostReport(query, session.token), endpoints.reportHistory(session.token)];
    if (admin) jobs.push(endpoints.adminActivity({ desde: applied.desde, hasta: applied.hasta }, session.token));
    if (!admin) jobs.push(endpoints.ownListings(session.token));
    Promise.all(jobs).then(([report, exports, extra, ownedListings]) => {
      if (!alive) return;
      setData(report); setHistory(exports || []);
      if (admin) setActivity(extra || []); else setListings(ownedListings || []);
    }).catch(err => { if (alive) setError(err.message); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [admin, applied, reload, session.token]);

  const setFilter = (key, value) => setFilters(current => ({ ...current, [key]: value }));
  const submit = (event) => { event.preventDefault(); if (filters.desde && filters.hasta && filters.desde > filters.hasta) { setError('La fecha inicial debe ser anterior o igual a la fecha final.'); return; } setError(''); setNotice(''); setApplied({ ...filters }); };
  const clear = () => { setFilters(initialFilters); setApplied({ ...initialFilters }); };
  const exportFile = async (format) => {
    setExporting(format); setError(''); setNotice('');
    try {
      const options = { ...applied, formato: format };
      const file = admin ? await endpoints.exportAdminReport(options, session.token) : await endpoints.exportHostReport(options, session.token);
      const url = URL.createObjectURL(file.blob); const link = document.createElement('a'); link.href = url; link.download = file.filename; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url);
      setNotice(`${format} descargado. La generación quedó registrada en el historial.`); setReload(value => value + 1);
    } catch (err) { setError(err.message); } finally { setExporting(''); }
  };

  const metrics = useMemo(() => {
    if (!data) return [];
    if (admin) return [
      { icon: Users, label: 'Cuentas registradas', value: count.format(data.usuariosTotales || 0), detail: 'Según los filtros aplicados' },
      { icon: ShieldCheck, label: 'Cuentas restringidas', value: count.format(data.cuentasPorEstado?.RESTRINGIDA || 0), detail: 'Incluye cuentas bloqueadas' },
      { icon: ClipboardList, label: 'Publicaciones pendientes', value: count.format(data.publicacionesPorEstado?.PENDIENTE_REVISION || 0), detail: 'Esperando aprobación' },
      { icon: CalendarDays, label: 'Reservas del periodo', value: count.format((data.reservasPorMes || []).reduce((sum, row) => sum + Number(row.reservas || 0), 0)), detail: 'Datos simulados de demostración' },
    ];
    return [
      { icon: CalendarDays, label: 'Confirmadas y completadas', value: count.format(data.reservasConfirmadasYCompletadas || 0), detail: 'Reservas simuladas' },
      { icon: CalendarDays, label: 'Canceladas', value: count.format(data.reservasCanceladas || 0), detail: 'Reservas simuladas' },
      { icon: CircleDollarSign, label: 'Valor bruto estimado', value: currency.format(data.valorBruto || 0), detail: 'Cifras ficticias' },
      { icon: CircleDollarSign, label: 'Ganancia neta estimada', value: currency.format(data.gananciaNetaEstimada || 0), detail: `Tarifa ficticia: ${currency.format(data.tarifaFicticia || 0)}` },
    ];
  }, [admin, data]);

  return <main className="page-shell reports-page">
    <div className="reports-heading"><div><span className="eyebrow">PATIO · CENTRO DE INFORMES</span><h1>{admin ? 'Reportes de administración' : 'Reportes de anfitrión'}<span>.</span></h1><p>{admin ? 'Sigue la actividad de cuentas, publicaciones y tendencias del marketplace.' : 'Consulta el rendimiento estimado de tus alojamientos.'}</p></div><Button variant="outline" onClick={() => setReload(value => value + 1)} disabled={loading}><RefreshCw size={16}/> Actualizar</Button></div>
    <form className={`report-filters ${admin ? 'report-filters-admin' : ''}`} onSubmit={submit}>
      <FilterField label="Desde"><input type="date" value={filters.desde} onChange={event => setFilter('desde', event.target.value)}/></FilterField>
      <FilterField label="Hasta"><input type="date" value={filters.hasta} onChange={event => setFilter('hasta', event.target.value)}/></FilterField>
      {admin ? <>
        <FilterField label="Ciudad"><input placeholder="Todas" value={filters.ciudad} onChange={event => setFilter('ciudad', event.target.value)}/></FilterField>
        <FilterField label="Rol"><select value={filters.rol} onChange={event => setFilter('rol', event.target.value)}><option value="">Todos</option><option>HUESPED</option><option>ANFITRION</option><option>ADMINISTRADOR</option></select></FilterField>
        <FilterField label="Estado de cuenta"><select value={filters.estado} onChange={event => setFilter('estado', event.target.value)}><option value="">Todos</option><option>ACTIVA</option><option>RESTRINGIDA</option><option>DESACTIVADA</option></select></FilterField>
        <FilterField label="Publicación"><select value={filters.estadoPublicacion} onChange={event => setFilter('estadoPublicacion', event.target.value)}><option value="">Todos</option><option>ACTIVA</option><option>PENDIENTE_REVISION</option><option>PAUSADA</option><option>BORRADOR</option><option>BLOQUEADA</option></select></FilterField>
      </> : <FilterField label="Alojamiento"><select value={filters.alojamientoId} onChange={event => setFilter('alojamientoId', event.target.value)}><option value="">Todos mis alojamientos</option>{listings.map(item => <option key={item.id} value={item.alojamientoId}>{item.titulo} · {item.ciudad}</option>)}</select></FilterField>}
      <div className="report-filter-actions"><Button type="submit">Aplicar filtros</Button><Button type="button" variant="outline" onClick={clear}>Limpiar</Button></div>
    </form>
    {error && <Message tone="error">{error}</Message>}{notice && <Message tone="success">{notice}</Message>}
    {loading && <div className="report-loading">Cargando reportes…</div>}
    {!loading && data && <>
      <div className="report-toolbar"><span>Periodo: <strong>{data.desde || applied.desde} — {data.hasta || applied.hasta}</strong></span><div><Button variant="outline" disabled={Boolean(exporting)} onClick={() => exportFile('CSV')}><Download size={16}/>{exporting === 'CSV' ? 'Preparando…' : 'Descargar CSV'}</Button><Button variant="outline" disabled={Boolean(exporting)} onClick={() => exportFile('PDF')}><Download size={16}/>{exporting === 'PDF' ? 'Preparando…' : 'Descargar PDF'}</Button></div></div>
      {data.reservasSimuladas && <Message><strong>Simulación:</strong> {data.notaReservas || 'Las reservas y sus valores son datos demostrativos, no corresponden a transacciones reales.'}</Message>}
      <div className="report-metrics">{metrics.map(metric => <Metric key={metric.label} {...metric}/>)}</div>
      {admin ? <div className="report-grid">
        <BarChart title="Nuevas cuentas por mes" data={data.nuevosUsuariosPorMes || []} series={[{ key: 'nuevos', label: 'Cuentas nuevas', color: 'bar-green' }]}/>
        <BarChart title="Reservas mensuales simuladas" data={data.reservasPorMes || []} series={[{ key: 'reservas', label: 'Reservas', color: 'bar-terracotta' }]} />
        <section className="report-card"><div className="report-card-heading"><div><span className="eyebrow">CUENTAS</span><h2>Distribución por rol</h2></div><Users size={20}/></div><div className="report-breakdown">{Object.entries(data.usuariosPorRol || {}).map(([label, value]) => <div key={label}><span>{label}</span><strong>{count.format(value)}</strong></div>)}</div></section>
        <section className="report-card"><div className="report-card-heading"><div><span className="eyebrow">PUBLICACIONES</span><h2>Estado de revisión</h2></div><Home size={20}/></div><div className="report-breakdown">{Object.entries(data.publicacionesPorEstado || {}).map(([label, value]) => <div key={label}><span>{label.replaceAll('_', ' ')}</span><strong>{count.format(value)}</strong></div>)}</div></section>
      </div> : <div className="report-grid">
        <BarChart title="Reservas simuladas por mes" data={data.reservasPorMes || []} series={[{ key: 'confirmadasYCompletadas', label: 'Confirmadas', color: 'bar-green' }, { key: 'canceladas', label: 'Canceladas', color: 'bar-terracotta' }]}/>
        <section className="report-card"><div className="report-card-heading"><div><span className="eyebrow">RENDIMIENTO</span><h2>Por alojamiento</h2></div><Home size={20}/></div>{(data.porAlojamiento || []).length ? <div className="report-table-wrap"><table className="report-table"><thead><tr><th>Alojamiento</th><th>Confirmadas</th><th>Canceladas</th><th>Ganancia neta estimada</th></tr></thead><tbody>{data.porAlojamiento.map(item => <tr key={item.alojamientoId}><td>{item.titulo}</td><td>{count.format(item.confirmadasYCompletadas)}</td><td>{count.format(item.canceladas)}</td><td>{currency.format(item.gananciaNetaEstimada)}</td></tr>)}</tbody></table></div> : <p className="report-empty">No hay alojamientos para mostrar.</p>}</section>
      </div>}
      <MonthlyTable rows={admin ? data.reservasPorMes || [] : data.reservasPorMes || []} admin={admin}/>
      <History rows={history} admin={admin} activity={activity}/>
      <p className="report-footnote"><FileBarChart2 size={15}/> Los totales reflejan los filtros seleccionados. No se muestran datos personales de huéspedes.</p>
    </>}
  </main>;
}
