import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, Ban, Building2, Check, ClipboardList, FileBarChart2, RefreshCw, ShieldCheck, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { endpoints } from './api';
import { useAuth } from './auth';
import { Button, EmptyState, Field, Message, PropertyImage, StatusBadge } from './components';

const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const listingStates = ['', 'PENDIENTE_REVISION', 'ACTIVA', 'PAUSADA', 'BORRADOR', 'BLOQUEADA'];
const stateLabel = { '': 'Todos los estados', PENDIENTE_REVISION: 'Pendientes de revisión', ACTIVA: 'Activas', PAUSADA: 'Pausadas', BORRADOR: 'Borradores', BLOQUEADA: 'Bloqueadas' };

function AdminMetric({ icon: Icon, label, value, detail }) {
  return <article className="admin-metric"><span className="admin-metric-icon"><Icon size={19}/></span><span><small>{label}</small><strong>{value}</strong><em>{detail}</em></span></article>;
}

function AdminListing({ item, busy, onApprove, onBlock }) {
  const listing = item.publicacion;
  const [reason, setReason] = useState('');
  const [blocking, setBlocking] = useState(false);
  const photo = listing.imagenes?.[0]?.referencia;
  const canBlock = ['PENDIENTE_REVISION', 'ACTIVA', 'PAUSADA'].includes(listing.estado);
  return <article className="admin-listing">
    <div className="admin-listing-photo">{photo ? <PropertyImage src={photo} alt={listing.titulo}/> : <Building2 size={27}/>}</div>
    <div className="admin-listing-copy">
      <div className="admin-listing-kicker"><span>{listing.ciudad} · {listing.tipoAlojamiento}</span><StatusBadge status={listing.estado}/></div>
      <h3>{listing.titulo}</h3>
      <p>{listing.ubicacionTexto} · Hasta {listing.capacidadMaxima} huéspedes</p>
      <p className="admin-owner">Anfitrión: <strong>{item.anfitrionNombre}</strong> · {item.anfitrionCorreo}</p>
      <div className="admin-listing-meta"><strong>{money.format(Number(listing.precioNoche))}</strong><span>{listing.imagenes?.length || 0} fotos</span><span>{listing.servicios?.length || 0} servicios</span></div>
    </div>
    <div className="admin-listing-actions">
      {listing.estado === 'PENDIENTE_REVISION' && <Button disabled={busy} onClick={() => onApprove(listing.id)}><Check size={15}/> Aprobar</Button>}
      {canBlock && <Button variant="outline" disabled={busy} onClick={() => setBlocking(value => !value)}><Ban size={15}/> Bloquear</Button>}
    </div>
    {blocking && <form className="admin-block-form" onSubmit={event => { event.preventDefault(); onBlock(listing.id, reason).then(ok => { if (ok) setBlocking(false); }); }}>
      <Field label="Motivo del bloqueo" as="textarea" value={reason} onChange={event => setReason(event.target.value)} required maxLength={500} placeholder="Explica por qué se bloquea esta publicación"/>
      <div><Button type="submit" disabled={busy || !reason.trim()}>Confirmar bloqueo</Button><Button type="button" variant="outline" onClick={() => setBlocking(false)}>Cancelar</Button></div>
    </form>}
  </article>;
}

export default function AdminPage() {
  const { session } = useAuth();
  const [tab, setTab] = useState('publicaciones');
  const [listings, setListings] = useState([]);
  const [users, setUsers] = useState([]);
  const [listingFilter, setListingFilter] = useState('PENDIENTE_REVISION');
  const [userRole, setUserRole] = useState('');
  const [userState, setUserState] = useState('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let live = true;
    setLoading(true); setError('');
    Promise.all([endpoints.adminPublications({}, session.token), endpoints.adminUsers({}, session.token)])
      .then(([publicationRows, userRows]) => { if (live) { setListings(publicationRows || []); setUsers(userRows || []); } })
      .catch(err => { if (live) setError(err.message); })
      .finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [reload, session.token]);

  const filteredListings = useMemo(() => listings.filter(item => !listingFilter || item.publicacion.estado === listingFilter), [listings, listingFilter]);
  const filteredUsers = useMemo(() => users.filter(user => (!userRole || user.rol === userRole) && (!userState || user.estado === userState)), [users, userRole, userState]);
  const pending = listings.filter(item => item.publicacion.estado === 'PENDIENTE_REVISION').length;
  const restricted = users.filter(user => user.estado === 'RESTRINGIDA').length;

  const runAction = async (id, action, success) => {
    setBusyId(id); setError(''); setNotice('');
    try { await action(); setNotice(success); setReload(value => value + 1); return true; }
    catch (err) { setError(err.message); return false; }
    finally { setBusyId(''); }
  };

  const approve = id => runAction(id, () => endpoints.approvePublication(id, session.token), 'La publicación fue aprobada y ya puede aparecer en la búsqueda pública.');
  const block = (id, reason) => runAction(id, () => endpoints.blockPublication(id, reason, session.token), 'La publicación quedó bloqueada.');
  const changeAccount = user => {
    const next = user.estado === 'RESTRINGIDA' ? 'ACTIVA' : 'RESTRINGIDA';
    const label = next === 'ACTIVA' ? 'rehabilitada' : 'restringida';
    return runAction(user.id, () => endpoints.changeUserState(user.id, next, session.token), `La cuenta de ${user.nombre} quedó ${label}.`);
  };

  return <main className="page-shell admin-page">
    <section className="admin-heading"><div><span className="eyebrow">PATIO · CENTRO DE ADMINISTRACIÓN</span><h1>Panel administrador<span>.</span></h1><p>Modera publicaciones, gestiona cuentas y consulta la actividad del marketplace.</p></div><Button variant="outline" onClick={() => setReload(value => value + 1)} disabled={loading}><RefreshCw size={16}/> Actualizar</Button></section>
    {error && <Message tone="error">{error}</Message>}{notice && <Message tone="success">{notice}</Message>}
    <section className="admin-metrics">
      <AdminMetric icon={Building2} label="Alojamientos publicados" value={listings.length} detail={`${pending} pendientes de aprobación`}/>
      <AdminMetric icon={Users} label="Cuentas de usuarios" value={users.length} detail={`${restricted} restringidas`}/>
      <AdminMetric icon={ClipboardList} label="Revisión pendiente" value={pending} detail="Requieren decisión administrativa"/>
    </section>
    <nav className="admin-tabs" aria-label="Secciones administrativas">
      <button className={tab === 'publicaciones' ? 'active' : ''} onClick={() => setTab('publicaciones')}><Building2 size={16}/> Alojamientos</button>
      <button className={tab === 'cuentas' ? 'active' : ''} onClick={() => setTab('cuentas')}><Users size={16}/> Cuentas</button>
      <Link to="/administrador/reportes"><FileBarChart2 size={16}/> Reportes y actividad <ArrowRight size={15}/></Link>
    </nav>

    {tab === 'publicaciones' && <section className="admin-section"><div className="admin-section-head"><div><span className="eyebrow">MODERACIÓN</span><h2>Alojamientos y publicaciones</h2><p>Revisa los anuncios de todos los anfitriones, incluidos los que aún no son públicos.</p></div><label className="admin-filter">Estado<select value={listingFilter} onChange={event => setListingFilter(event.target.value)}>{listingStates.map(state => <option key={state} value={state}>{stateLabel[state]}</option>)}</select></label></div>
      {loading ? <p className="admin-loading">Cargando alojamientos…</p> : filteredListings.length ? <div className="admin-listings">{filteredListings.map(item => <AdminListing key={item.publicacion.id} item={item} busy={busyId === item.publicacion.id} onApprove={approve} onBlock={block}/>)}</div> : <EmptyState icon={Building2} title="No hay alojamientos en este estado" detail="Prueba con otro estado o vuelve a consultar más tarde."/>}
    </section>}

    {tab === 'cuentas' && <section className="admin-section"><div className="admin-section-head"><div><span className="eyebrow">ACCESO A LA PLATAFORMA</span><h2>Gestión de cuentas</h2><p>Restringe cuentas activas o rehabilita las que estén restringidas.</p></div><div className="admin-filter-pair"><label className="admin-filter">Rol<select value={userRole} onChange={event => setUserRole(event.target.value)}><option value="">Todos</option><option value="HUESPED">Huésped</option><option value="ANFITRION">Anfitrión</option></select></label><label className="admin-filter">Estado<select value={userState} onChange={event => setUserState(event.target.value)}><option value="">Todos</option><option value="ACTIVA">Activa</option><option value="RESTRINGIDA">Restringida</option><option value="DESACTIVADA">Desactivada</option></select></label></div></div>
      {loading ? <p className="admin-loading">Cargando cuentas…</p> : filteredUsers.length ? <div className="admin-users">{filteredUsers.map(user => <article className="admin-user" key={user.id}><span className="admin-user-icon"><Users size={17}/></span><div><strong>{user.nombre}</strong><span>{user.correo}</span></div><span className="admin-user-role">{user.rol === 'ANFITRION' ? 'Anfitrión' : 'Huésped'}</span><StatusBadge status={user.estado}/><div>{user.estado !== 'DESACTIVADA' && <Button variant={user.estado === 'RESTRINGIDA' ? 'primary' : 'outline'} disabled={busyId === user.id} onClick={() => changeAccount(user)}>{user.estado === 'RESTRINGIDA' ? <><Check size={15}/> Rehabilitar</> : <><Ban size={15}/> Restringir</>}</Button>}</div></article>)}</div> : <EmptyState icon={Users} title="No hay cuentas con esos filtros" detail="Cambia los filtros para consultar otras cuentas."/>}
    </section>}

    <p className="admin-note"><ShieldCheck size={16}/> Los cambios de estado de cuenta y las decisiones de moderación quedan registrados en la actividad administrativa.</p>
  </main>;
}
