# Registro de incrementos y Scrum — Fase 3

Este registro se reconstruyó a partir del historial Git disponible, el proyecto Jira `SP` y la asignación compartida por el equipo. Los sprints de Jira se consideran **planificación**, según la decisión del equipo del 24 de noviembre de 2026. Los commits son evidencia de incrementos del repositorio; no prueban por sí solos que se ejecutaran las ceremonias de cada sprint.

## Planificación registrada en Jira

El tablero [SP board](https://unbosque-team-lfpngtwi.atlassian.net/browse/SP) tiene cuatro sprints. Jira muestra los cuatro en estado `future`, aunque sus fechas planificadas ya transcurrieron. Se conservan como plan, sin presentarlos como sprints iniciados o cerrados.

| Sprint planificado | Inicio | Fin | Estado consultado |
|---|---|---|---|
| Sprint 1 Cuentas-Publicaciones | 13 oct 2026 | 23 oct 2026 | Futuro |
| Sprint 2 | 24 oct 2026 | 3 nov 2026 | Futuro |
| Sprint 3 | 4 nov 2026 | 13 nov 2026 | Futuro |
| Sprint 4 | 14 nov 2026 | 24 nov 2026 | Futuro |

El proyecto contiene nueve épicas y 24 historias; al consultar Jira, todas aparecían en «Por hacer». La siguiente revisión debe relacionar las historias con código y pruebas antes de actualizar estados. Las metas de sprint, estimaciones, reviews y retrospectivas no se deducen de estas fechas.

## Incrementos encontrados en Git

| Fecha del commit | Referencia | Incremento visible en el historial |
|---|---|---|
| 17 nov 2026 | `b4cca59` | Inicialización del backend Marketplace. |
| 19 nov 2026 | `5e94c41` | Incorporación del módulo de reportes y su frontend. |
| 19 nov 2026 | `7f8599a` | Perfil administrativo y exportación de reportes con formato. |
| 24 nov 2026 | `b2075d7` | Integración de módulos del Marketplace y diagramas de diseño. |

## Responsabilidades comunicadas por el equipo

| Trabajo | Responsable informado | Evidencia/resultado que falta asociar |
|---|---|---|
| Artefactos Scrum por sprint | Santiago (según el mensaje compartido) | Capturas/enlace de planificación, tablero, revisión y retrospectiva de cada sprint. |
| Plan de pruebas | Federico | Documento final con alcance, estrategia, criterios de entrada/salida, entorno y riesgos. |
| Diseño de casos de prueba | Nicolás | Casos relacionados con historias y criterios de aceptación; incluir los módulos integrados después del informe v5. |
| Informe de pruebas | Santiago (según el mensaje compartido) | Versión final posterior a las pruebas de reservas/reseñas, defectos y evidencias. |
| Pipeline CI | Sin responsable indicado | Asignar propietario para revisar el workflow, configurar SonarCloud y aportar captura de Actions. |

## Formato para registrar cada sprint con evidencia real

Duplicar esta sección por sprint y completar con datos del tablero:

- Sprint e intervalo real:
- Objetivo del sprint:
- Historias/IDs y criterios de aceptación comprometidos:
- Responsable por historia:
- Estimación inicial y resultado:
- Incremento demostrable (commit/PR/release):
- Resultado de CI y análisis estático:
- Revisión del sprint: qué se terminó, qué quedó pendiente y decisiones:
- Retrospectiva: mantener / cambiar / probar:
- Evidencia de Jira/Projects:

## Definition of Done propuesta para el cierre de construcción

- El cambio está enlazado con una historia o defecto del backlog.
- Se desarrolla en rama y se integra por Pull Request con revisión de otro integrante.
- Backend y frontend compilan; las pruebas automatizadas pasan en GitHub Actions.
- Migraciones son versionadas y no se alteran una vez aplicadas.
- API y configuración local están documentadas; no se suben secretos.
- El incremento se puede mostrar con datos de demostración identificados como tales.
- El resultado de SonarCloud y las limitaciones conocidas se documentan.
