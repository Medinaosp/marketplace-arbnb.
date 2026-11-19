import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext(null);
const KEY = 'patio.session';

function readSession() {
  try { return JSON.parse(sessionStorage.getItem(KEY) || 'null'); }
  catch { return null; }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);
  const navigate = useNavigate();
  const login = (data) => {
    const next = { token: data.accessToken, role: data.rol, userId: data.usuarioId, expiresAt: data.expiresAt };
    sessionStorage.removeItem('patio.sessionExpired');
    sessionStorage.setItem(KEY, JSON.stringify(next));
    setSession(next);
  };
  const logout = () => { sessionStorage.removeItem(KEY); setSession(null); };
  useEffect(() => {
    if (!session?.expiresAt) return undefined;
    const delay = new Date(session.expiresAt).getTime() - Date.now();
    const onExpiry = () => {
      sessionStorage.removeItem(KEY);
      sessionStorage.setItem('patio.sessionExpired', 'true');
      setSession(null);
      navigate('/login', { replace: true, state: { notice: 'Tu sesión venció. Inicia sesión nuevamente para continuar.' } });
    };
    if (!Number.isFinite(delay) || delay <= 0) { onExpiry(); return undefined; }
    const timer = window.setTimeout(onExpiry, Math.min(delay, 2147483647));
    return () => window.clearTimeout(timer);
  }, [session, navigate]);
  useEffect(() => {
    if (!session?.token) return undefined;
    const onUnauthorized = (event) => {
      if (event.detail?.token !== session.token) return;
      sessionStorage.removeItem(KEY);
      sessionStorage.setItem('patio.sessionExpired', 'true');
      setSession(null);
      navigate('/login', { replace: true, state: { notice: 'Tu sesión venció o ya no es válida. Inicia sesión nuevamente.' } });
    };
    window.addEventListener('patio:auth-expired', onUnauthorized);
    return () => window.removeEventListener('patio:auth-expired', onUnauthorized);
  }, [session, navigate]);
  const value = useMemo(() => ({ session, login, logout }), [session]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
