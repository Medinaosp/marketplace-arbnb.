import { useEffect, useState } from 'react';
import { ArrowLeft, Check, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import { endpoints } from './api';
import { useAuth } from './auth';
import { Button, Field, Message } from './components';

const roleLabel = { ADMINISTRADOR: 'Administrador', ANFITRION: 'Anfitrión', HUESPED: 'Huésped' };
const homeByRole = { ADMINISTRADOR: '/administrador', ANFITRION: '/anfitrion', HUESPED: '/' };

export default function ProfilePage() {
  const { session } = useAuth();
  const [profile, setProfile] = useState(null);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let active = true;
    endpoints.me(session.token)
      .then(data => {
        if (!active) return;
        setProfile(data);
        setName(data.nombre || '');
        setEmail(data.correo || '');
      })
      .catch(err => { if (active) setError(err.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [session.token]);

  const submit = async event => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);
    try {
      const updated = await endpoints.updateMe({ nombre: name.trim(), correo: email.trim() }, session.token);
      setProfile(updated);
      setName(updated.nombre || '');
      setEmail(updated.correo || '');
      setSuccess('Tus datos de perfil se actualizaron correctamente.');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return <main className="page-shell profile-page">
    <Link className="back-link" to={homeByRole[session.role] || '/'}><ArrowLeft size={16}/> Volver</Link>
    <header className="profile-heading">
      <span className="eyebrow">TU CUENTA</span>
      <h1>Mi perfil<span>.</span></h1>
      <p>Consulta y actualiza tus datos personales.</p>
    </header>
    {error && <Message tone="error">{error}</Message>}
    {success && <Message tone="success"><Check size={16}/> {success}</Message>}
    {loading ? <div className="form-card profile-loading">Cargando tus datos…</div> : profile && <section className="form-card profile-card">
      <div className="form-card-title"><span><UserRound size={17}/></span><div><h2>Información personal</h2><p>Los cambios se guardan en tu cuenta.</p></div></div>
      <form className="profile-form" onSubmit={submit}>
        <Field label="Nombre completo" autoComplete="name" maxLength={120} value={name} onChange={event => setName(event.target.value)} required/>
        <Field label="Correo electrónico" type="email" autoComplete="email" maxLength={254} value={email} onChange={event => setEmail(event.target.value)} required/>
        <div className="profile-readonly"><span>Tipo de cuenta</span><strong>{roleLabel[profile.rol] || profile.rol}</strong></div>
        <div className="profile-readonly"><span>Estado de cuenta</span><strong>{profile.estado}</strong></div>
        <p className="profile-hint">Para cambiar tu contraseña, usa la opción de cambio de contraseña desde tu cuenta.</p>
        <Button type="submit" disabled={saving || !name.trim() || !email.trim()}>{saving ? 'Guardando…' : 'Guardar cambios'}</Button>
      </form>
    </section>}
  </main>;
}
