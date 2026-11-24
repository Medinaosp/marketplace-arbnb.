import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuth } from './auth';
import { Footer, Header, Message } from './components';
import { AuthPage, HomePage, HostDashboard, ListingFormPage, PropertyPage, SearchPage } from './pages';
import ReportsPage from './ReportsPage';
import AdminPage from './AdminPage';
import ProfilePage from './ProfilePage';
import { AdminCommercePage, DiscountsPage, ReservationsPage } from './CommercePages';

function Protected({ children, role }) {
  const { session } = useAuth(); const location = useLocation();
  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }}/>;
  if (role && session.role !== role) return <main className="page-shell"><Message tone="error">No tienes permiso para acceder a esta sección.</Message><Navigate to="/" replace/></main>;
  return children;
}

function ScrollToTop() { const { pathname } = useLocation(); useEffect(() => { window.scrollTo({ top: 0, behavior: 'smooth' }); }, [pathname]); return null; }

function Shell({ children }) { return <><Header/><ScrollToTop/>{children}<Footer/></>; }

export default function App() {
  return <Routes>
    <Route path="/" element={<Shell><HomePage/></Shell>}/>
    <Route path="/buscar" element={<Shell><SearchPage/></Shell>}/>
    <Route path="/alojamientos/:id" element={<Shell><PropertyPage/></Shell>}/>
    <Route path="/login" element={<AuthPage mode="login"/>}/>
    <Route path="/registro" element={<AuthPage mode="register"/>}/>
    <Route path="/recuperar-contrasena" element={<AuthPage mode="recover"/>}/>
    <Route path="/perfil" element={<Shell><Protected><ProfilePage/></Protected></Shell>}/>
    <Route path="/anfitrion" element={<Shell><Protected role="ANFITRION"><HostDashboard/></Protected></Shell>}/>
    <Route path="/anfitrion/reportes" element={<Shell><Protected role="ANFITRION"><ReportsPage role="ANFITRION"/></Protected></Shell>}/>
    <Route path="/administrador" element={<Shell><Protected role="ADMINISTRADOR"><AdminPage/></Protected></Shell>}/>
    <Route path="/administrador/reportes" element={<Shell><Protected role="ADMINISTRADOR"><ReportsPage role="ADMINISTRADOR"/></Protected></Shell>}/>
    <Route path="/administrador/comercial" element={<Shell><Protected role="ADMINISTRADOR"><AdminCommercePage/></Protected></Shell>}/>
    <Route path="/reservas" element={<Shell><Protected role="HUESPED"><ReservationsPage/></Protected></Shell>}/>
    <Route path="/anfitrion/reservas" element={<Shell><Protected role="ANFITRION"><ReservationsPage host/></Protected></Shell>}/>
    <Route path="/anfitrion/descuentos" element={<Shell><Protected role="ANFITRION"><DiscountsPage/></Protected></Shell>}/>
    <Route path="/anfitrion/publicaciones/nueva" element={<Shell><Protected role="ANFITRION"><ListingFormPage/></Protected></Shell>}/>
    <Route path="/anfitrion/publicaciones/:id/editar" element={<Shell><Protected role="ANFITRION"><ListingFormPage/></Protected></Shell>}/>
    <Route path="*" element={<Navigate to="/" replace/>}/>
  </Routes>;
}
