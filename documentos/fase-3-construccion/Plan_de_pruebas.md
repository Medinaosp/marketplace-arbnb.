# Plan de pruebas — Fase 3

## Propósito

Definir cómo verificar el incremento construido del Marketplace antes de entregar la Fase 3. El plan cubre usuarios, alojamientos/publicaciones, reservas y reportes, con pruebas automatizadas y pruebas funcionales manuales. Los resultados se registran en el informe de pruebas; este documento no declara aprobados casos que aún no se hayan ejecutado.

## Alcance

- **Usuarios:** registro, inicio/cierre de sesión, perfil y contraseña, recuperación, roles, estados de cuenta y permisos.
- **Alojamientos y publicaciones:** operaciones por rol, validaciones de datos, imágenes/servicios, disponibilidad, bloqueos, moderación y visibilidad pública.
- **Reservas y reportes:** cotizaciones, creación/consulta/cancelación, pago y reembolso simulados, reseñas y moderación, reportes por rol y exportación PDF/CSV.
- **Integración:** migraciones Flyway, autorización, API y recorridos principales del frontend.

## Fuera de alcance o simulado

No hay transacciones reales de pago ni envío de dinero. Las reservas pueden utilizar datos demostrativos para reportes; deben rotularse como simulados. La entrega no certifica seguridad de producción, carga masiva ni compatibilidad con navegadores distintos de los usados en la prueba.

## Entorno de ejecución

- Backend: Java 17, Spring Boot y PostgreSQL; iniciar con configuración local documentada en `README.md`.
- Esquema: aplicar migraciones versionadas con Flyway; no editar migraciones ya aplicadas.
- Frontend: Node 20, instalación reproducible con `npm ci` y compilación de producción.
- Automatización: GitHub Actions inicia PostgreSQL efímero para backend y compila frontend en cada push a `main` y Pull Request.
- Datos: usar cuentas y alojamientos demo del perfil `dev`; no usar credenciales reales en capturas o logs.

## Estrategia

1. **Dominio/unitarias:** reglas de disponibilidad, rangos, estados y cálculos aislados. Se incorporan pruebas de disponibilidad en `src/test/java/`.
2. **Integración backend:** arranque del contexto, migraciones, persistencia, seguridad y endpoints sobre PostgreSQL. La ejecución continua se realiza con `./mvnw verify` en CI.
3. **Frontend:** `npm ci` y `npm run build`; completar con recorridos de interfaz autenticados manualmente.
4. **Aceptación manual:** ejecutar casos del informe con el rol y datos indicados, anotar resultado observado y adjuntar captura que muestre solicitud/contexto y respuesta sin exponer tokens.
5. **Revisión estática:** SonarCloud y Quality Gate una vez configuradas las credenciales del proyecto.

## Criterios de entrada

- Backend y PostgreSQL disponibles con variables válidas y migraciones aplicadas.
- Frontend apunta al backend correcto.
- Cuentas de prueba activas y permisos conocidos.
- Historias y criterios de aceptación acordados para el incremento.

## Criterios de salida

- CI de backend y frontend exitoso en el commit candidato.
- Sin defectos bloqueantes abiertos; cualquier defecto restante tiene impacto y responsable documentados.
- Casos críticos de autenticación/autorización, publicación, disponibilidad y reserva ejecutados y con evidencia.
- Informe actualizado con fecha, versión, ambiente, resultado esperado/obtenido y capturas.
- Limitaciones académicas y datos simulados declarados en la sustentación.

## Riesgos y tratamiento

| Riesgo | Tratamiento |
|---|---|
| Token o cuenta de prueba vencidos | Renovar sesión y registrar la cuenta/rol usados sin copiar el token a la evidencia. |
| Estado compartido entre pruebas | Preparar datos controlados y evitar reutilizar IDs que se borran o cambian de estado. |
| Reportes con reservas simuladas | Identificar claramente las cifras; no presentarlas como transacciones reales. |
| SMTP no configurado | Probar la respuesta funcional y documentar que la entrega del correo depende del SMTP del entorno. |
| Diferencias entre PDF de diseño y alcance construido | Registrar explícitamente módulos parciales y decisiones de alcance en el informe final. |

## Registro por caso

Para cada caso indicar: identificador, historia/diagrama relacionado, precondiciones, rol, pasos, resultado esperado, resultado observado, estado (Aprobado/Fallido/No ejecutado), defecto asociado, fecha y ruta de evidencia. No marcar como aprobado un caso con respuesta distinta a la esperada aunque la pantalla cargue.
