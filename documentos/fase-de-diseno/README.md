# Entrega de fase de diseño

Esta carpeta conserva el PDF recibido y los nueve diagramas como evidencia del diseño del marketplace. Las imágenes corresponden a la entrega de diseño original; no se editaron para ocultar diferencias con la implementación.

## Diagramas

1. [Diagrama de contexto](01_Diagrama_de_contexto.png): actores y sistemas externos.
2. [Diagrama de despliegue](02_Diagrama_de_despliegue.png): nodos y distribución de componentes.
3. [Diagrama de clases](03_Diagrama_de_clases.png): clases de dominio, atributos, operaciones y relaciones.
4. [Diagrama de arquitectura](04_Diagrama_de_arquitectura.png): capas y dependencias principales.
5. [Diagrama de componentes](05_Diagrama_de_componentes.png): módulos funcionales.
6. [Casos de uso del huésped](06_Casos_de_uso_huesped.png).
7. [Casos de uso del anfitrión](07_Casos_de_uso_anfitrion.png).
8. [Casos de uso del administrador](08_Casos_de_uso_administrador.png).
9. [Modelo relacional](09_Modelo_relacional.png): tablas y relaciones de la base de datos.

## Documento original

[Entrega de fase de diseño (PDF)](Entrega_Fase_de_Diseno_Marketplace_grupo_4.pdf)

## Lectura junto con la implementación

- Los módulos de usuarios y alojamientos/publicaciones están en `src/main/java/softplasty/marketplace/modules/users` y `.../accommodations`.
- Administración y moderación están en `.../modules/admin`; reportes y actividad, en `.../modules/reports`.
- Cotizaciones, reservas, pagos demostrativos, cancelaciones, descuentos, tarifas y reseñas están en `.../modules/reservations` y en la migración `V3__reservations_pricing_reviews.sql`.
- Varias estructuras de reservas se consultan con JDBC en vez de modelarse como entidades JPA individuales. El envío de notificaciones persistentes no está implementado.
- Los pagos y reembolsos son demostrativos. Las reservas de los reportes son simuladas y se identifican como tales.

Consulta el [README principal](../../README.md) para configurar la base de datos, variables y arranque local.
