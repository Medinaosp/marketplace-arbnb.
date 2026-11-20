import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ArrowRight, Bell, ChevronDown, CircleAlert, Compass, FileBarChart2, Heart, Home, Leaf, LogOut, Menu, Search, ShieldCheck, Sparkles, UserRound, X } from 'lucide-react';
import { Component, useState } from 'react';
import { useAuth } from './auth';

export function Brand({ light = false }) {
  return <Link className={`brand ${light ? 'brand-light' : ''}`} to="/" aria-label="Patio, ir al inicio"><span className="brand-mark"><Leaf size={20} strokeWidth={2.2} /></span><span>patio<span className="brand-period">.</span></span></Link>;
}

export function Header({ compact = false }) {
  const { session, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  return <>
    <header className={`topbar ${compact ? 'topbar-compact' : ''}`}>
      <div className="nav-inner"><Brand />
        <nav className="desktop-nav" aria-label="Navegación principal">
          <NavLink to="/buscar?ciudad=Bogotá">Explorar alojamientos</NavLink>
          {session?.role === 'ANFITRION' && <NavLink to="/anfitrion">Panel de anfitrión</NavLink>}
          {session?.role === 'ANFITRION' && <NavLink to="/anfitrion/reportes">Reportes</NavLink>}
          {session?.role === 'ADMINISTRADOR' && <NavLink to="/administrador/reportes">Reportes admin</NavLink>}
          {session?.role === 'ADMINISTRADOR' && <NavLink to="/administrador">Administración</NavLink>}
        </nav>
        <div className="nav-actions">
          {session?.role === 'ANFITRION' ? <Link to="/anfitrion/publicaciones/nueva" className="host-link">Publica tu espacio <ArrowRight size={15} /></Link> : session?.role === 'ADMINISTRADOR' ? <Link to="/administrador" className="host-link">Panel administrador <ArrowRight size={15} /></Link> : <Link to="/registro" className="host-link">Conviértete en anfitrión <ArrowRight size={15} /></Link>}
          <div className="profile-wrap">
            <button className={`profile-button ${menuOpen ? 'is-open' : ''}`} onClick={() => setMenuOpen(!menuOpen)} aria-label="Abrir menú de usuario" aria-expanded={menuOpen}>
              {menuOpen ? <X size={17} /> : <Menu size={17} />}<span className="avatar"><UserRound size={16} /></span>
            </button>
            {menuOpen && <div className="profile-menu">
              {session ? <><div className="menu-user"><span className="avatar avatar-large"><UserRound size={18} /></span><div><strong>Tu cuenta</strong><span>{session.role === 'ANFITRION' ? 'Anfitrión' : session.role === 'ADMINISTRADOR' ? 'Administrador' : 'Huésped'}</span></div></div><Link to="/perfil" onClick={() => setMenuOpen(false)}>Mi perfil</Link>{session.role === 'ANFITRION' ? <><Link to="/anfitrion" onClick={() => setMenuOpen(false)}>Administrar publicaciones</Link><Link to="/anfitrion/reportes" onClick={() => setMenuOpen(false)}>Ver reportes</Link></> : session.role === 'ADMINISTRADOR' ? <><Link to="/administrador" onClick={() => setMenuOpen(false)}>Panel de administración</Link><Link to="/administrador/reportes" onClick={() => setMenuOpen(false)}>Ver reportes</Link></> : <Link to="/" onClick={() => setMenuOpen(false)}>Explorar alojamientos</Link>}<button className="menu-signout" onClick={() => { logout(); setMenuOpen(false); navigate('/'); }}><LogOut size={16} /> Cerrar sesión</button></> : <><Link to="/login" onClick={() => setMenuOpen(false)}>Iniciar sesión</Link><Link to="/registro" onClick={() => setMenuOpen(false)}>Crear cuenta</Link><div className="menu-divider"/><Link to="/registro?rol=ANFITRION" onClick={() => setMenuOpen(false)}>Publica tu alojamiento</Link></>}
            </div>}
          </div>
        </div>
      </div>
    </header>
    <nav className="mobile-nav" aria-label="Navegación móvil"><Link to="/"><Home size={19}/><span>Inicio</span></Link><Link to="/buscar?ciudad=Bogotá"><Search size={19}/><span>Explorar</span></Link>{session?.role === 'ANFITRION' ? <Link to="/anfitrion/reportes"><UserRound size={19}/><span>Reportes</span></Link> : session?.role === 'ADMINISTRADOR' ? <><Link to="/administrador"><ShieldCheck size={19}/><span>Admin</span></Link><Link to="/administrador/reportes"><FileBarChart2 size={19}/><span>Reportes</span></Link></> : <Link to="/registro"><UserRound size={19}/><span>Cuenta</span></Link>}</nav>
  </>;
}

export function Footer() {
  return <footer className="site-footer"><div className="footer-inner"><div><Brand/><p>Un buen lugar se siente como llegar a casa.</p></div><div className="footer-links"><Link to="/buscar?ciudad=Medellín">Explorar</Link><Link to="/registro?rol=ANFITRION">Publica un espacio</Link><a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">API del proyecto</a></div><span className="footer-copy">© 2025 Patio · Hecho en Colombia</span></div></footer>;
}

export function SearchForm({ initialCity = '', variant = 'hero', onSearch }) {
  const [city, setCity] = useState(initialCity);
  const navigate = useNavigate();
  const submit = (event) => {
    event.preventDefault();
    const value = city.trim();
    if (!value) return;
    if (onSearch) onSearch(value); else navigate(`/buscar?ciudad=${encodeURIComponent(value)}`);
  };
  return <form className={`search-form search-${variant}`} onSubmit={submit}>
    <label className="search-destination"><Compass size={18}/><span><small>¿A dónde quieres ir?</small><input aria-label="Ciudad de destino" placeholder="Ciudad de Colombia" value={city} onChange={event => setCity(event.target.value)} list="colombian-cities" required /></span></label>
    <datalist id="colombian-cities"><option>Bogotá</option><option>Medellín</option><option>Cartagena</option><option>Santa Marta</option><option>Salento</option></datalist>
    <div className="search-disabled" aria-label="Fechas próximamente"><span><small>Fechas</small><strong>Próximamente</strong></span></div>
    <div className="search-disabled search-guests" aria-label="Huéspedes próximamente"><span><small>Huéspedes</small><strong>Próximamente</strong></span></div>
    <button className="button button-primary search-button" type="submit" aria-label="Buscar alojamientos"><Search size={18}/><span>Buscar</span></button>
  </form>;
}

export function PropertyCard({ property, index = 0 }) {
  const photo = property.imagenes?.[0]?.referencia;
  const type = property.tipoAlojamiento || 'Alojamiento';
  const formatPrice = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  return <article className="property-card">
    <Link to={`/alojamientos/${property.id}`} className="property-photo" aria-label={`Ver ${property.titulo}`}>
      {photo ? <img src={photo} alt={property.imagenes[0]?.textoAlternativo || property.titulo} loading={index > 5 ? 'lazy' : 'eager'} /> : <div className={`photo-placeholder placeholder-${index % 4}`}><Home size={34}/><span>Foto del alojamiento</span></div>}
      <span className="photo-count"><span className="photo-dot"/><span>{property.imagenes?.length || 0} fotos</span></span>
      <span className="property-kind">{type}</span>
    </Link>
    <div className="property-info">
      <div className="property-title-row"><div><p className="property-location">{property.ubicacionTexto}, {property.ciudad}</p><Link to={`/alojamientos/${property.id}`} className="property-title">{property.titulo}</Link></div><span className="capacity">{property.capacidadMaxima} huéspedes</span></div>
      <div className="property-services">{(property.servicios || []).slice(0, 2).map(service => <span key={service}>{service.replaceAll('_', ' ').toLowerCase()}</span>)}{(!property.servicios || property.servicios.length === 0) && <span>Un espacio para descansar</span>}</div>
      <div className="property-bottom"><p><strong>{formatPrice.format(Number(property.precioNoche))}</strong><span> / noche</span></p><Link to={`/alojamientos/${property.id}`} className="text-link">Ver alojamiento <ArrowRight size={15}/></Link></div>
    </div>
  </article>;
}

export function LoadingGrid() { return <div className="property-grid">{Array.from({ length: 6 }, (_, index) => <div className="property-skeleton" key={index}><div className="skeleton skeleton-image"/><div className="skeleton skeleton-line"/><div className="skeleton skeleton-line short"/><div className="skeleton skeleton-line"/></div>)}</div>; }

export function Message({ tone = 'info', children, onClose }) { return <div className={`message message-${tone}`} role={tone === 'error' ? 'alert' : 'status'}><span>{children}</span>{onClose && <button onClick={onClose} aria-label="Cerrar mensaje"><X size={16}/></button>}</div>; }

export function StatusBadge({ status }) {
  const labels = { BORRADOR: 'Borrador', PENDIENTE_REVISION: 'En revisión', ACTIVA: 'Activa', PAUSADA: 'Pausada', BLOQUEADA: 'Requiere ajustes', RESTRINGIDA: 'Restringida', DESACTIVADA: 'Desactivada' };
  return <span className={`status-badge status-${status?.toLowerCase()}`}>{labels[status] || status}</span>;
}

export function Button({ className = '', variant = 'primary', children, ...props }) { return <button className={`button button-${variant} ${className}`} {...props}>{children}</button>; }

export function Field({ label, hint, error, className = '', as = 'input', ...props }) { return <label className={`field ${className}`}><span className="field-label">{label}</span>{as === 'textarea' ? <textarea {...props} /> : <input {...props} />}{hint && <small className="field-hint">{hint}</small>}{error && <small className="field-error">{error}</small>}</label>; }

export function EmptyState({ icon: Icon = Sparkles, title, detail, action }) { return <div className="empty-state"><span className="empty-icon"><Icon size={26}/></span><h3>{title}</h3><p>{detail}</p>{action}</div>; }

export function TrustNote({ children }) { return <div className="trust-note"><ShieldCheck size={18}/><span>{children}</span></div>; }

export class ErrorBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  render() {
    if (this.state.failed) return <main className="page-shell"><div className="empty-state"><span className="empty-icon"><CircleAlert size={26}/></span><h3>Algo no salió como esperábamos</h3><p>La página encontró un error inesperado. Puedes volver a cargarla e intentarlo de nuevo.</p><Button onClick={() => window.location.reload()}>Volver a cargar</Button></div></main>;
    return this.props.children;
  }
}
