# Matriz preliminar de historias Jira y producto

Consulta del proyecto Jira `SP` realizada el 24 de noviembre de 2026. El proyecto tiene nueve épicas y 24 historias; todas las historias consultadas estaban en «Por hacer». La clasificación de esta matriz es un **diagnóstico del código y los criterios de aceptación**, no una transición de estado en Jira ni una aprobación de pruebas. Una historia solo se cerrará después de ejecutar sus casos, vincular evidencia y registrar el PR y release correspondientes.

| Jira | Capacidad | Diagnóstico | Evidencia en el proyecto y diferencia por comprobar |
|---|---|---|---|
| SP-10 | Cuenta del huésped | Base implementada | Registro, login, perfil, cambio y recuperación de contraseña en `modules/users`; contrastar todos los CA con el informe actualizado. |
| SP-11 | Cuenta del anfitrión | Parcial | Registro y permisos presentes; comprobar el efecto de desactivar la cuenta sobre publicaciones y reservas según CA3. |
| SP-12 | Acceso administrador | Parcial | Admin aprovisionado y rutas protegidas; verificar registro de intentos denegados exigido por CA2. |
| SP-13 | Actualización de alojamientos | Parcial | Edición, imágenes y servicios presentes; confirmar auditoría de cada cambio y respuesta esperada al editar una publicación ajena. |
| SP-14 | Creación de publicaciones | Base implementada | `modules/accommodations`, formulario y estados; completar prueba integral con datos inválidos y transición a revisión. |
| SP-15 | Precios y servicios adicionales | Parcial | Precio y tarifas de servicios presentes; verificar conservación del precio de reservas ya confirmadas y todos los cálculos del CA. |
| SP-17 | Reglas y cancelación | Base implementada | Política asociada, catálogo y reglas visibles; ejecutar casos de política ausente y consulta pública. |
| SP-18 | Estados y pausa | Base implementada | Pausar/reactivar y moderación presentes; ejecutar casos para bloqueo administrativo y reservas existentes. |
| SP-19 | Gestión de disponibilidad | Parcial | Bloqueos y consulta de rangos presentes; no se observó gestión explícita de periodos DISPONIBLE ni calendario completo como pide CA1. |
| SP-20 | Búsqueda con filtros | Parcial | API pública filtra por ciudad; el frontend aplica tipo, precio máximo y servicio a resultados cargados. Faltan filtro por fechas y capacidad y filtro de precio mínimo en la búsqueda. |
| SP-21 | Detalle y comparación | Parcial | Detalle, imágenes, servicios, reglas y reseñas públicas presentes; verificar promedio de calificación y comportamiento de reseñas ocultas. |
| SP-22 | Consulta de disponibilidad | Parcial | API consulta un rango y cotización valida ocupación; comprobar visualización de calendario con fechas ocupadas y contiguas. |
| SP-23 | Cotización detallada | Parcial | Noches, base, servicios, descuentos y total presentes; no se observa tarifa de limpieza ni porcentaje de servicio configurable como en CA1. |
| SP-24 | Reserva inmediata | Parcial | Reserva pendiente, pago demostrativo y control de solapes presentes; verificar reintentos idempotentes y concurrencia antes de marcar CA3 cumplido. |
| SP-25 | Pago con Stripe de prueba | Parcial | Existe pago simulado interno; no se encontró integración Stripe sandbox ni sus tarjetas de prueba y manejo de caída de pasarela. |
| SP-26 | Cancelación y devolución | Parcial | Cancelación y devolución simulada presentes; verificar vista previa del valor antes de confirmar y reglas por política. |
| SP-27 | Historial de reservas del huésped | Parcial | `GET /api/reservas/mias` y pantalla Mis reservas presentes; faltan notificaciones internas persistidas y estados de lectura. |
| SP-28 | Seguimiento del anfitrión | Parcial | Listado de reservas del anfitrión presente; faltan notificaciones de reserva y cancelación y vista de ocupación agrupada verificable. |
| SP-29 | Moderación de publicaciones | Parcial | Aprobación/bloqueo presentes; no se ha acreditado revisión manual de reservas futuras ni notificación al anfitrión tras bloqueo. |
| SP-30 | Control de usuarios | Parcial | Restricción y efecto en operaciones/búsquedas presentes; verificar CA1 sobre bloqueo de login y registro del motivo. |
| SP-31 | Reseñas del huésped | Parcial | Crear reseña vinculada a reserva y consultar reseñas visibles; confirmar condición de reserva COMPLETADA, duplicidad y momento de publicación. |
| SP-32 | Moderación de reseñas | Parcial | Estado de reseña y ocultamiento presentes; verificar recálculo del promedio y evento de auditoría. |
| SP-33 | Indicadores y exportación | Parcial | Dashboards y PDF/CSV presentes; Jira pide PDF/Excel y cifras coherentes con reservas reales. La serie histórica de reservas sigue simulada. |
| SP-34 | Auditoría | Parcial | Eventos y consulta administrativa presentes; verificar filtros por tipo, fecha y usuario, campos requeridos e inmutabilidad. |

## Columnas pendientes para la trazabilidad obligatoria

Para cada fila se deben completar: requisito RF/RNF, criterio de aceptación exacto, sprint planificado, rama y commit, Pull Request y revisión, caso y captura de prueba, resultado de CI y SonarCloud, release. No se asignan PR ni releases ficticios a commits ya integrados en `main`.

## Orden para cerrar las historias críticas

1. Demostrar el flujo de SP-14 → SP-20 → SP-22 → SP-23 → SP-24 → SP-25 con datos controlados.
2. Resolver o declarar las brechas de SP-20, SP-23 y SP-25 frente al MVP y sus criterios.
3. Actualizar casos y resultados de reservas, cancelación y reseñas.
4. Vincular evidencia y revisar los estados de Jira con los responsables antes de mover historias.
