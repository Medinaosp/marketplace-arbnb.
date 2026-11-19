const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request(path, { method = 'GET', body, token, headers = {} } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
  } catch {
    throw new ApiError('No pudimos conectar con el servidor. Verifica que el backend esté iniciado.');
  }

  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('json') ? await response.json().catch(() => null) : await response.text();
  if (!response.ok) {
    const message = data?.detail || data?.message || data?.title || data?.error || (typeof data === 'string' && data) || 'Ocurrió un error al procesar la solicitud.';
    if (response.status === 401 && token) {
      window.dispatchEvent(new CustomEvent('patio:auth-expired', { detail: { token } }));
      throw new ApiError('Tu sesión venció o ya no es válida. Inicia sesión nuevamente.', 401);
    }
    throw new ApiError(message, response.status);
  }
  return data;
}

async function download(path, { body, token } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: { Accept: '*/*', 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(body),
    });
  } catch {
    throw new ApiError('No pudimos conectar con el servidor. Verifica que el backend esté iniciado.');
  }
  if (!response.ok) {
    const type = response.headers.get('content-type') || '';
    const data = type.includes('json') ? await response.json().catch(() => null) : await response.text();
    if (response.status === 401 && token) {
      window.dispatchEvent(new CustomEvent('patio:auth-expired', { detail: { token } }));
      throw new ApiError('Tu sesión venció o ya no es válida. Inicia sesión nuevamente.', 401);
    }
    throw new ApiError(data?.detail || data?.title || 'No fue posible generar el archivo.', response.status);
  }
  const disposition = response.headers.get('content-disposition') || '';
  const filename = disposition.match(/filename="?([^";]+)"?/i)?.[1] || 'reporte';
  return { blob: await response.blob(), filename };
}

const queryString = (params) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') query.set(key, value); });
  const value = query.toString();
  return value ? `?${value}` : '';
};

export const endpoints = {
  login: (body) => request('/api/auth/login', { method: 'POST', body }),
  register: (body) => request('/api/auth/registro', { method: 'POST', body }),
  me: (token) => request('/api/usuarios/me', { token }),
  publicListings: (city) => request(`/api/publicaciones?ciudad=${encodeURIComponent(city)}`),
  publicListing: (id) => request(`/api/publicaciones/${id}`),
  ownListings: (token) => request('/api/publicaciones/mias', { token }),
  ownListing: (id, token) => request(`/api/publicaciones/mias/${id}`, { token }),
  ownLodgings: (token) => request('/api/alojamientos/mios', { token }),
  services: () => request('/api/catalogos/servicios'),
  policies: () => request('/api/catalogos/politicas-cancelacion'),
  createLodging: (body, token) => request('/api/alojamientos', { method: 'POST', body, token }),
  updateLodging: (id, body, token) => request(`/api/alojamientos/${id}`, { method: 'PUT', body, token }),
  createListing: (body, token) => request('/api/publicaciones', { method: 'POST', body, token }),
  updateListing: (id, body, token) => request(`/api/publicaciones/${id}`, { method: 'PUT', body, token }),
  updateServices: (id, serviceIds, token) => request(`/api/publicaciones/${id}/servicios`, { method: 'PUT', body: { servicioIds }, token }),
  addImage: (id, body, token) => request(`/api/publicaciones/${id}/imagenes`, { method: 'POST', body, token }),
  removeImage: (id, imageId, token) => request(`/api/publicaciones/${id}/imagenes/${imageId}`, { method: 'DELETE', token }),
  sendToReview: (id, token) => request(`/api/publicaciones/${id}/enviar-revision`, { method: 'POST', token }),
  pauseListing: (id, token) => request(`/api/publicaciones/${id}/pausar`, { method: 'POST', token }),
  activateListing: (id, token) => request(`/api/publicaciones/${id}/activar`, { method: 'POST', token }),
  ownBlocks: (id, token) => request(`/api/alojamientos/${id}/bloqueos`, { token }),
  addBlock: (id, body, token) => request(`/api/alojamientos/${id}/bloqueos`, { method: 'POST', body, token }),
  removeBlock: (id, start, end, token) => request(`/api/alojamientos/${id}/bloqueos?inicio=${start}&fin=${end}`, { method: 'DELETE', token }),
  adminReport: (filters, token) => request(`/api/reportes/administrador${queryString(filters)}`, { token }),
  hostReport: (filters, token) => request(`/api/reportes/anfitrion${queryString(filters)}`, { token }),
  reportHistory: (token) => request('/api/reportes/historial', { token }),
  adminActivity: (filters, token) => request(`/api/reportes/administrador/actividad${queryString(filters)}`, { token }),
  exportAdminReport: (filters, token) => download(`/api/reportes/administrador/exportaciones${queryString(filters)}`, { token, body: { formato: filters.formato, desde: filters.desde, hasta: filters.hasta } }),
  exportHostReport: (filters, token) => download('/api/reportes/anfitrion/exportaciones', { token, body: { formato: filters.formato, desde: filters.desde, hasta: filters.hasta, ...(filters.alojamientoId ? { alojamientoId: filters.alojamientoId } : {}) } }),
};
