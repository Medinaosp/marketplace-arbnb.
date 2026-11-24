# Fase 3 — Construcción

Paquete de entrega para la construcción incremental del Marketplace. Se complementa con el código fuente, las migraciones Flyway, la guía técnica del proyecto y el workflow de GitHub Actions.

## Evidencias y ubicación

| Evidencia solicitada | Ubicación | Estado verificable |
|---|---|---|
| Código versionado | Repositorio raíz | Disponible en `main`; la tarea local actual tiene cambios sin commit. |
| Incrementos funcionales | Historial de Git y aplicación | Se resumen en `Registro_de_incrementos.md`. Para afirmar que hubo demo al cierre de cada sprint se requiere evidencia del equipo. |
| Base de datos, scripts y datos de prueba | `src/main/resources/db/migration/` y el perfil de desarrollo | Migraciones V1–V3 y carga idempotente de datos demo. La contraseña local se configura fuera del repositorio. |
| Pruebas unitarias | `src/test/java/` | Se agregan pruebas de dominio para rangos de disponibilidad. La prueba de contexto requiere PostgreSQL. |
| Integración continua | `.github/workflows/ci.yml` | Construye backend y frontend, ejecuta pruebas y conserva reportes. Para activar SonarCloud hay pasos de configuración descritos abajo. |
| Documentación técnica | `README.md`, `documentos/fase-de-diseno/README.md` y este paquete | Guía de configuración, ejecución, API, datos, límites y diagrams disponible. |
| Artefactos Scrum | `Registro_de_incrementos.md` | Reconstrucción basada en commits y en la asignación compartida. Las fechas de sprint, IDs del backlog, estimaciones, reviews y retrospectivas deben cotejarse con Jira/Projects antes de presentarlas como actas reales. |
| Trazabilidad de historias | `Matriz_historias_Jira.md` | Diagnóstico preliminar de las 24 historias del proyecto Jira `SP` frente al código; faltan PR, pruebas, Quality Gate y release por historia. |
| Plan de pruebas | `Plan_de_pruebas.md` | Alcance, entorno, estrategia, criterios y riesgos propuestos para los tres módulos. |
| Informe de pruebas | `../../../../documentos/entrega v1/Informe de pruebas soft plasty.docx` | Copia identificada como la enviada. Es anterior a reservas/reseñas; se debe anexar una actualización para declarar cobertura completa del código actual. |
| Casos de prueba | `../../../../documentos/Informe_pruebas_usuarios_alojamientos_reportes_v4.docx` y la carpeta de evidencias indicada en el informe | Evidencia manual disponible. Los módulos agregados después requieren casos y resultados propios. |

## Criterios de cierre de Fase 3

La fase queda lista para entrega cuando el equipo adjunte evidencia real de cada punto de esta lista:

1. El workflow de GitHub Actions termina en verde para el commit de entrega.
2. El Pull Request tiene revisión de otro integrante y está fusionado.
3. SonarCloud termina el análisis y el Quality Gate cumple el umbral definido por el curso.
4. Se adjuntan del tablero Scrum las historias, planificación, revisión y retrospectiva de cada sprint; no basta con reconstruir fechas desde Git.
5. Se actualiza el informe de pruebas con los módulos integrados recientemente, los casos ejecutados, los resultados y sus capturas.
6. Se presenta la diferencia entre el alcance del diseño y lo que quedó en el producto; en particular, reservas/pagos/reembolsos son demostrativos y la búsqueda actual no filtra por fechas ni capacidad.

La implementación local de CI y las pruebas automatizadas ya están preparadas, pero el estado de GitHub Actions, PR, SonarCloud y tablero Scrum no puede marcarse como aprobado sin consultar esas plataformas y reunir sus evidencias.

## Ejecutar las verificaciones

Backend, desde la raíz:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress test
```

Frontend:

```powershell
Set-Location frontend
npm ci
npm run build
```

La verificación de contexto del backend requiere PostgreSQL, las migraciones Flyway y las variables descritas en el README principal. GitHub Actions inicia un PostgreSQL temporal para esa ejecución; no utiliza la base local ni sus credenciales.

## SonarCloud

El workflow intenta el análisis si están definidos el secreto `SONAR_TOKEN` y las variables `SONAR_PROJECT_KEY` y `SONAR_ORGANIZATION` del repositorio. Mientras falte alguno, el workflow deja una advertencia y la fase **no debe declararse con Quality Gate aprobado**. Se necesita crear/configurar el proyecto en SonarCloud y capturar el resultado visible del análisis.

## Flujo de integración para los cambios nuevos

Crear una rama por tarea, asociar el cambio con una historia o defecto, abrir Pull Request hacia `main`, esperar CI, pedir revisión a otro integrante y solo entonces fusionar. Esta guía no afirma que los commits históricos ya tengan PR: el clon local muestra únicamente `main`.

## Límites que deben quedar declarados

- La serie histórica de reportes del anfitrión sigue usando datos simulados; no se deriva todavía de las reservas persistidas.
- La configuración de correo SMTP es local y no debe guardarse como secreto en Git.
- La última versión del informe de pruebas que encontramos precede a la integración de reservas y reseñas; hay que sumar los resultados de esos módulos antes de usarlo como cierre de pruebas.
- La barra de búsqueda local se simplificó a ciudad y botón. El documento del curso también pide filtros de fechas y capacidad; registrar esta desviación o restaurar esos filtros para la sustentación.
- El repositorio local no contiene PRs/revisiones, releases ni un tablero de Jira exportado. Agregar capturas/enlaces reales del equipo antes de entregar esos elementos como evidencia.
