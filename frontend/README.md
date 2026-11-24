# Patio — Frontend

Aplicación React + Vite para el marketplace. Está en una carpeta separada del backend Spring Boot y consume la API existente mediante `/api`.

## Desarrollo

1. Inicia PostgreSQL y el backend Spring Boot en `http://localhost:8080` con las variables de entorno indicadas en el README del proyecto.
2. Desde esta carpeta instala dependencias y ejecuta:

   ```bash
   npm install
   npm run dev
   ```

3. Abre `http://localhost:5173`.

Vite redirige `/api` a `http://localhost:8080`. Si el backend está en otra dirección, define `VITE_API_PROXY_TARGET`. Para despliegue con API en otro origen, define `VITE_API_BASE_URL` y configura `APP_CORS_ALLOWED_ORIGINS` en Spring Boot.

## Flujos conectados

- Registro e inicio de sesión con JWT; el token vive en `sessionStorage` y vence según la expiración que entrega la API.
- Búsqueda pública por ciudad y detalle de publicaciones activas.
- Panel anfitrión: crear/editar alojamiento y publicación, administrar servicios, imágenes por URL, bloqueos manuales, reservas y descuentos.
- Huésped: cotizar por fechas/ocupación/servicios, reservar, ejecutar pagos demostrativos, cancelar con devolución calculada y reseñar estancias completadas.
- Administración comercial: definir tarifas por estadía o por noche, y aprobar u ocultar reseñas.
- Reportes con acceso por rol: indicadores y filtros para administración; ganancias estimadas, tendencias mensuales y desglose por alojamiento para anfitriones. Permite descargar CSV/PDF y consultar el historial de exportaciones. Las reservas se identifican como simuladas.
- Filtros de resultados se aplican sobre la lista que devuelve el endpoint de ciudad.

Los pagos y reembolsos son demostrativos y no realizan movimientos de dinero. La carga binaria de fotos y la mensajería no están implementadas. Las series de reservas de reportes siguen siendo datos simulados y no se agregan desde las reservas guardadas.
