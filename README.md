# Patio — Marketplace de alojamientos

Aplicación académica para publicar alojamientos en Colombia. Incluye tres módulos principales: Usuarios (01), Alojamientos y Publicaciones (02), y Reservas y Reportes (03). El administrador dispone además de operaciones de moderación y consulta de actividad. El backend usa Java 17, Spring Boot, Spring Security y PostgreSQL; el frontend usa React y Vite. Flyway crea y actualiza el esquema; Hibernate solo lo valida.

Los diagramas y el PDF de la fase de diseño están en [`documentos/fase-de-diseno/`](documentos/fase-de-diseno/README.md).
Los artefactos y la lista de verificación de construcción (fase 3), incluido el flujo de CI, están en [`documentos/fase-3-construccion/`](documentos/fase-3-construccion/README.md).

## Configuración local

1. Crea en PostgreSQL la base `marketplace_db` y el rol `marketplace_app`, y asigna al rol la propiedad/permisos de esa base.
2. En IntelliJ, en **Run/Debug Configuration > Environment variables**, configura:
   - `DB_PASSWORD`: la contraseña actual del rol `marketplace_app`.
   - `APP_JWT_SECRET`: una clave HMAC aleatoria, en Base64, que represente al menos 32 bytes aleatorios.
   - `ADMIN_EMAIL` y `ADMIN_PASSWORD`: credenciales para crear el primer administrador. La contraseña debe tener al menos 12 caracteres.
3. En IntelliJ, agrega `dev` en **Active profiles** solo si quieres cargar las cuentas y alojamientos demo. Inicia la aplicación. Flyway aplicará las migraciones pendientes y cargará los catálogos. Si ya hay una cuenta administradora, la semilla no la modifica y no necesita las variables `ADMIN_*`.

Para una base de demostración nueva se pueden configurar `ADMIN_EMAIL=admin@marketplace.local` y `ADMIN_PASSWORD=adminmarketplace`. El administrador solo se crea si todavía no existe uno. En Render, agrega estas variables al servicio del backend y reinícialo; la base local y la de Render son independientes. No uses esta contraseña de ejemplo para datos reales.

Para habilitar la recuperación de contraseña por correo, configura también `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` y `APP_FRONTEND_URL` en esa misma configuración de ejecución. `.env.example` muestra el formato; IntelliJ no carga ese archivo automáticamente. Con Gmail, utiliza una contraseña de aplicación. No guardes esas credenciales en el repositorio.

No guardes secretos en el repositorio ni los compartas en capturas. El valor de `DB_PASSWORD` debe coincidir con la contraseña que ya tiene el rol en PostgreSQL. La aplicación no cambia contraseñas de base de datos.

Opcionales: `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/marketplace_db`), `DB_USERNAME` (por defecto `marketplace_app`), `APP_JWT_ISSUER`, `APP_CORS_ALLOWED_ORIGINS`. El backend requiere zona horaria `America/Bogota`; en producción el inicio falla si la zona del sistema difiere. Los valores `Instant` se almacenan en UTC.

Para cargar cuentas y alojamientos de ejemplo, inicia con el perfil `dev` y configura `APP_DEMO_HOST_PASSWORD` y `APP_DEMO_GUEST_PASSWORD` (mínimo 8 caracteres). El cargador idempotente crea los perfiles de demostración, publicaciones de flujo y diez cabañas distribuidas entre ocho ciudades. Los correos demo por defecto son `anfitrion.demo@example.test` y `huesped.demo@example.test`; se pueden cambiar con `APP_DEMO_HOST_EMAIL` y `APP_DEMO_GUEST_EMAIL`. El sembrado es opcional, no corre en el perfil predeterminado ni en producción y no registra las contraseñas. Las cabañas usan ilustraciones y datos de ubicación/precio de demostración; reemplázalos antes de presentar esos anuncios como reales.

## API

Con la aplicación iniciada, Swagger UI está en `http://localhost:8080/swagger-ui.html`.

## Frontend

La interfaz React + Vite está en `frontend/` y consume esta API a través del proxy de desarrollo de Vite.

1. Inicia PostgreSQL y el backend en `http://localhost:8080` con las variables de entorno descritas arriba.
2. En otra terminal, desde `frontend/`, ejecuta `npm install` y luego `npm run dev`.
3. Abre `http://localhost:5173`.

La interfaz cubre búsqueda, detalle, autenticación, flujos de anfitrión, cotizaciones, reservas, pagos demostrativos, cancelaciones, descuentos, tarifas de servicios y reseñas moderadas. No hay cargos ni reembolsos reales; la carga de fotos sigue siendo por URL y la mensajería no está implementada.

### Usuarios y acceso

- `POST /api/auth/registro`: crea una cuenta `HUESPED` o `ANFITRION`.
- `POST /api/auth/login`: devuelve JWT de acceso válido 60 minutos.
- `GET /api/usuarios/me`, `PUT /api/usuarios/me`: consulta y actualiza el perfil.
- `PUT /api/usuarios/me/contrasena`: cambia la contraseña propia.
- `DELETE /api/usuarios/me`: desactiva la cuenta propia.
- `POST /api/usuarios/restablecimientos`: solicita por correo un enlace de recuperación; responde igual si la cuenta no existe.
- `POST /api/usuarios/restablecimientos/canjear`: canjea un token de un solo uso. La emisión administrativa está expuesta como servicio de aplicación y exige un administrador activo; el token se entrega por correo cuando SMTP está configurado.

El enlace público de recuperación dura 30 minutos, se puede usar una sola vez y solo se almacena su hash. Se limita a una solicitud por cuenta cada minuto. Los administradores no se recuperan por este flujo. Los JWT no tienen refresh ni revocación; rol y estado se consultan en la base en cada solicitud. Una cuenta restringida puede consultar su perfil y cambiar su contraseña; una desactivada no puede acceder.

### Catálogos

- `GET /api/catalogos/servicios`
- `GET /api/catalogos/politicas-cancelacion`

### Alojamientos y disponibilidad (anfitrión)

- `POST /api/alojamientos`, `GET /api/alojamientos/mios`, `PUT /api/alojamientos/{id}`.
- `POST /api/alojamientos/{id}/bloqueos`: agrega un bloqueo `[inicio, fin)`; rechaza fechas pasadas y solapes.
- `GET /api/alojamientos/{id}/bloqueos`: lista los bloqueos propios.
- `DELETE /api/alojamientos/{id}/bloqueos?inicio=AAAA-MM-DD&fin=AAAA-MM-DD`: libera solo un rango exacto existente.
- `GET /api/alojamientos/{id}/disponibilidad?inicio=AAAA-MM-DD&fin=AAAA-MM-DD`.

La cotización y la creación de reserva comprueban bloqueos y reservas confirmadas o pendientes de pago. Las reservas pendientes retienen las fechas durante 15 minutos.

### Publicaciones

- Público: `GET /api/publicaciones?ciudad=...` y `GET /api/publicaciones/{id}`. Solo expone publicaciones `ACTIVA` de anfitriones `ACTIVA`.
- Anfitrión: `GET /api/publicaciones/mias`, `GET /api/publicaciones/mias/{id}`, `POST /api/publicaciones`, `PUT /api/publicaciones/{id}`.
- Fotos: `POST /api/publicaciones/{id}/imagenes`, `DELETE /api/publicaciones/{id}/imagenes/{imagenId}`, `PATCH /api/publicaciones/{id}/imagenes/{imagenId}/orden`.
- Servicios: `PUT /api/publicaciones/{id}/servicios`.
- Flujo: `POST /api/publicaciones/{id}/enviar-revision`, `/pausar` y `/activar`.

Las publicaciones se crean como `BORRADOR`; requieren título, descripción, precio, alojamiento y política. El borrador puede no tener fotos; para revisión y activación se requiere al menos una imagen. Se pueden subir archivos JPG o PNG de hasta 2 MB, que se guardan en PostgreSQL; las referencias HTTP/HTTPS existentes siguen disponibles. Los órdenes de las fotos son únicos por publicación. Los cambios importantes en una activa la devuelven a revisión; los hechos durante una pausa se registran hasta reactivar. El administrador puede listar, aprobar y bloquear publicaciones desde `/api/administracion/publicaciones`.

## Cotizaciones, reservas y reseñas

- Huésped: `POST /api/publicaciones/{id}/cotizaciones`, `POST /api/reservas/cotizaciones/{id}/reservar`, `POST /api/reservas/{id}/pago-simulado`, `GET /api/reservas/mias`, `POST /api/reservas/{id}/cancelar` y `POST /api/reservas/{id}/resena`.
- Anfitrión: `GET /api/reservas/anfitrion` y administrar descuentos en `/api/publicaciones/{id}/descuentos`.
- Administrador: consultar y cambiar tarifas en `/api/servicios/tarifas` y `/api/administracion/servicios/{id}/tarifa`; moderar reseñas en `/api/administracion/resenas`.
- Los pagos/reembolsos son simulados. Cotizaciones vencen en 30 minutos. Las reseñas quedan pendientes de revisión.

## Persistencia

Todas las migraciones se ubican en la única carpeta `src/main/resources/db/migration`. Flyway aplica sus archivos en orden: V1 crea el esquema inicial, V2 agrega reportes y auditoría, V3 agrega reservas y reseñas, y V4 añade el almacenamiento de imágenes. No elimines ni cambies versiones ya aplicadas: la base existente necesita su historial. Los textos de cancelación son informativos y los pagos/reembolsos son simulados.

## Diagramas y fase de diseño

Consulta [`documentos/fase-de-diseno/README.md`](documentos/fase-de-diseno/README.md) para ver los nueve diagramas y el PDF original. Los diagramas representan el diseño entregado; algunas decisiones de implementación o alcance pueden diferir, como se indica en el índice.

## Alcance y límites conocidos

- El panel de reportes conserva sus series demostrativas y todavía no calcula cifras a partir de las reservas persistidas.
- El motivo de moderación de publicaciones se valida pero no se persiste.
- El envío del enlace requiere que se configure un proveedor SMTP con las variables indicadas arriba.
- Configura HTTPS para cualquier despliegue de producción.
