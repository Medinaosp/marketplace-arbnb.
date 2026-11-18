# Marketplace de alojamientos: backend

API REST modular para los módulos de Usuarios (01) y Alojamientos/Publicaciones (02), con disponibilidad manual. Está construida con Java 17, Spring Boot, Spring Security, Spring Data JPA y PostgreSQL 17. Flyway crea el esquema; Hibernate solo lo valida.

## Configuración local

1. Crea en PostgreSQL la base `marketplace_db` y el rol `marketplace_app`, y asigna al rol la propiedad/permisos de esa base.
2. En IntelliJ, en **Run/Debug Configuration > Environment variables**, configura:
   - `DB_PASSWORD`: la contraseña actual del rol `marketplace_app`.
   - `APP_JWT_SECRET`: una clave HMAC aleatoria, en Base64, que represente al menos 32 bytes aleatorios.
   - `ADMIN_EMAIL` y `ADMIN_PASSWORD`: credenciales para crear el primer administrador. La contraseña debe tener al menos 12 caracteres.
3. Inicia la aplicación. Flyway aplicará `V1__create_marketplace_schema.sql` y cargará los catálogos. Si ya hay una cuenta administradora, la semilla no la modifica y no necesita las variables `ADMIN_*`.

No guardes secretos en el repositorio ni los compartas en capturas. El valor de `DB_PASSWORD` debe coincidir con la contraseña que ya tiene el rol en PostgreSQL. La aplicación no cambia contraseñas de base de datos.

Opcionales: `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/marketplace_db`), `DB_USERNAME` (por defecto `marketplace_app`), `APP_JWT_ISSUER`, `APP_CORS_ALLOWED_ORIGINS`. El backend requiere zona horaria `America/Bogota`; en producción el inicio falla si la zona del sistema difiere. Los valores `Instant` se almacenan en UTC.

Para cargar cuentas y publicaciones de ejemplo, inicia con el perfil `dev` o `test` y configura `APP_DEMO_HOST_PASSWORD` y `APP_DEMO_GUEST_PASSWORD` (mínimo 8 caracteres). Los correos demo por defecto son `anfitrion.demo@example.test` y `huesped.demo@example.test`; se pueden cambiar con `APP_DEMO_HOST_EMAIL` y `APP_DEMO_GUEST_EMAIL`. El sembrado es opcional, no corre en el perfil predeterminado ni en producción y no registra las contraseñas.

## API

Con la aplicación iniciada, Swagger UI está en `http://localhost:8080/swagger-ui.html`.

### Usuarios y acceso

- `POST /api/auth/registro`: crea una cuenta `HUESPED` o `ANFITRION`.
- `POST /api/auth/login`: devuelve JWT de acceso válido 60 minutos.
- `GET /api/usuarios/me`, `PUT /api/usuarios/me`: consulta y actualiza el perfil.
- `PUT /api/usuarios/me/contrasena`: cambia la contraseña propia.
- `DELETE /api/usuarios/me`: desactiva la cuenta propia.
- `POST /api/usuarios/restablecimientos/canjear`: canjea un token de un solo uso emitido por el componente 06. La emisión está expuesta como servicio de aplicación para ese componente y exige un administrador activo; no hay endpoint administrativo aquí.

El token de recuperación dura 30 minutos y solo se almacena su hash. Los JWT no tienen refresh ni revocación; rol y estado se consultan en la base en cada solicitud. Una cuenta restringida puede consultar su perfil y cambiar su contraseña; una desactivada no puede acceder.

### Catálogos

- `GET /api/catalogos/servicios`
- `GET /api/catalogos/politicas-cancelacion`

### Alojamientos y disponibilidad (anfitrión)

- `POST /api/alojamientos`, `GET /api/alojamientos/mios`, `PUT /api/alojamientos/{id}`.
- `POST /api/alojamientos/{id}/bloqueos`: agrega un bloqueo `[inicio, fin)`; rechaza fechas pasadas y solapes.
- `GET /api/alojamientos/{id}/bloqueos`: lista los bloqueos propios.
- `DELETE /api/alojamientos/{id}/bloqueos?inicio=AAAA-MM-DD&fin=AAAA-MM-DD`: libera solo un rango exacto existente.
- `GET /api/alojamientos/{id}/disponibilidad?inicio=AAAA-MM-DD&fin=AAAA-MM-DD`.

La disponibilidad solo contempla bloqueos manuales; no hay reservas en estos módulos.

### Publicaciones

- Público: `GET /api/publicaciones?ciudad=...` y `GET /api/publicaciones/{id}`. Solo expone publicaciones `ACTIVA` de anfitriones `ACTIVA`.
- Anfitrión: `GET /api/publicaciones/mias`, `GET /api/publicaciones/mias/{id}`, `POST /api/publicaciones`, `PUT /api/publicaciones/{id}`.
- Fotos: `POST /api/publicaciones/{id}/imagenes`, `DELETE /api/publicaciones/{id}/imagenes/{imagenId}`, `PATCH /api/publicaciones/{id}/imagenes/{imagenId}/orden`.
- Servicios: `PUT /api/publicaciones/{id}/servicios`.
- Flujo: `POST /api/publicaciones/{id}/enviar-revision`, `/pausar` y `/activar`.

Las publicaciones se crean como `BORRADOR`; requieren título, descripción, precio, alojamiento y política. El borrador puede no tener fotos; para revisión y activación se requiere al menos una URL HTTP/HTTPS. Los órdenes de las fotos son únicos por publicación. Los cambios importantes en una activa la devuelven a revisión; los hechos durante una pausa se registran hasta reactivar. La moderación (aprobar/bloquear) corresponde al componente 06 y está disponible aquí como servicio de aplicación, sin endpoint HTTP.

## Persistencia

Las migraciones se ubican en `src/main/resources/db/migration`. No cambies una migración ya aplicada; crea otra versión para cambios posteriores. `V1` contiene las nueve tablas del diseño, el indicador de revisión aprobado por el equipo, catálogos de servicios y las tres políticas provisionales. Los textos de cancelación son informativos, pendientes de validación con el cliente antes de producción.

## Alcance y límites conocidos

- `DISPONIBLE` permanece en el enum del diseño, pero las operaciones solo crean y liberan rangos `BLOQUEADO`.
- Los bloqueos no consideran reservas. No se calcula la devolución de cancelaciones.
- El motivo de moderación no se persiste. Los endpoints administrativos de moderación y emisión de tokens pertenecen al componente 06.
- La emisión del token se devuelve una sola vez al invocador del servicio; no se envía correo en este alcance.
- Configura HTTPS para cualquier despliegue de producción.
