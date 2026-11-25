const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request(path, { method = 'GET', body, token, headers = {} } = {}) {
  const isForm = body instanceof FormData;
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body === undefined || isForm ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      ...(body === undefined ? {} : { body: isForm ? body : JSON.stringify(body) }),
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
  const fixImageUrls = value => {
    if (Array.isArray(value)) return value.map(fixImageUrls);
    if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, key === 'referencia' && typeof item === 'string' && item.startsWith('/api/imagenes/') ? `${API_BASE}${item}` : fixImageUrls(item)]));
    return value;
  };
  return fixImageUrls(data);
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
  requestPasswordReset: (body) => request('/api/usuarios/restablecimientos', { method: 'POST', body }),
  redeemPasswordReset: (body) => request('/api/usuarios/restablecimientos/canjear', { method: 'POST', body }),
  me: (token) => request('/api/usuarios/me', { token }),
  updateMe: (body, token) => request('/api/usuarios/me', { method: 'PUT', body, token }),
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
  uploadImage: (id, file, alt, token) => { const data = new FormData(); data.append('archivo', file); data.append('textoAlternativo', alt); return request(`/api/publicaciones/${id}/imagenes/archivo`, { method: 'POST', body: data, token }); },
  removeImage: (id, imageId, token) => request(`/api/publicaciones/${id}/imagenes/${imageId}`, { method: 'DELETE', token }),
  sendToReview: (id, token) => request(`/api/publicaciones/${id}/enviar-revision`, { method: 'POST', token }),
  pauseListing: (id, token) => request(`/api/publicaciones/${id}/pausar`, { method: 'POST', token }),
  activateListing: (id, token) => request(`/api/publicaciones/${id}/activar`, { method: 'POST', token }),
  quoteStay: (id, body, token) => request(`/api/publicaciones/${id}/cotizaciones`, { method: 'POST', body, token }),
  createReservation: (quoteId, token) => request(`/api/reservas/cotizaciones/${quoteId}/reservar`, { method: 'POST', token }),
  simulatedPayment: (id, resultado, token) => request(`/api/reservas/${id}/pago-simulado`, { method: 'POST', body: { resultado }, token }),
  cancelReservation: (id, motivo, token) => request(`/api/reservas/${id}/cancelar`, { method: 'POST', body: { motivo }, token }),
  guestReservations: (token) => request('/api/reservas/mias', { token }),
  hostReservations: (token) => request('/api/reservas/anfitrion', { token }),
  publicReviews: (id) => request(`/api/publicaciones/${id}/resenas`),
  createReview: (id, body, token) => request(`/api/reservas/${id}/resena`, { method: 'POST', body, token }),
  hostDiscounts: (id, token) => request(`/api/publicaciones/${id}/descuentos`, { token }),
  createDiscount: (id, body, token) => request(`/api/publicaciones/${id}/descuentos`, { method: 'POST', body, token }),
  disableDiscount: (id, discountId, token) => request(`/api/publicaciones/${id}/descuentos/${discountId}`, { method: 'DELETE', token }),
  serviceRates: () => request('/api/servicios/tarifas'),
  updateServiceRate: (id, body, token) => request(`/api/administracion/servicios/${id}/tarifa`, { method: 'PUT', body, token }),
  adminReviews: (token) => request('/api/administracion/resenas', { token }),
  moderateReview: (id, estado, token) => request(`/api/administracion/resenas/${id}?estado=${estado}`, { method: 'PATCH', token }),
  ownBlocks: (id, token) => request(`/api/alojamientos/${id}/bloqueos`, { token }),
  addBlock: (id, body, token) => request(`/api/alojamientos/${id}/bloqueos`, { method: 'POST', body, token }),
  removeBlock: (id, start, end, token) => request(`/api/alojamientos/${id}/bloqueos?inicio=${start}&fin=${end}`, { method: 'DELETE', token }),
  adminReport: (filters, token) => request(`/api/reportes/administrador${queryString(filters)}`, { token }),
  hostReport: (filters, token) => request(`/api/reportes/anfitrion${queryString(filters)}`, { token }),
  reportHistory: (token) => request('/api/reportes/historial', { token }),
  adminActivity: (filters, token) => request(`/api/reportes/administrador/actividad${queryString(filters)}`, { token }),
  adminPublications: (filters, token) => request(`/api/administracion/publicaciones${queryString(filters)}`, { token }),
  approvePublication: (id, token) => request(`/api/administracion/publicaciones/${id}/aprobar`, { method: 'POST', token }),
  blockPublication: (id, motivo, token) => request(`/api/administracion/publicaciones/${id}/bloquear`, { method: 'POST', body: { motivo }, token }),
  adminUsers: (filters, token) => request(`/api/administracion/usuarios${queryString(filters)}`, { token }),
  changeUserState: (id, estado, token) => request(`/api/administracion/usuarios/${id}/estado`, { method: 'PATCH', body: { estado }, token }),
  exportAdminReport: (filters, token) => download(`/api/reportes/administrador/exportaciones${queryString(filters)}`, { token, body: { formato: filters.formato, desde: filters.desde, hasta: filters.hasta } }),
  exportHostReport: (filters, token) => download('/api/reportes/anfitrion/exportaciones', { token, body: { formato: filters.formato, desde: filters.desde, hasta: filters.hasta, ...(filters.alojamientoId ? { alojamientoId: filters.alojamientoId } : {}) } }),
};
