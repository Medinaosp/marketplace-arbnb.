CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    nombre VARCHAR NOT NULL,
    correo VARCHAR NOT NULL,
    hash_contrasena VARCHAR NOT NULL,
    rol VARCHAR NOT NULL,
    estado VARCHAR NOT NULL,
    creado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('HUESPED', 'ANFITRION', 'ADMINISTRADOR')),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVA', 'RESTRINGIDA', 'DESACTIVADA'))
);

CREATE TABLE solicitud_recuperacion (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    token VARCHAR NOT NULL,
    solicitado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vence_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    usada BOOLEAN NOT NULL,
    CONSTRAINT fk_solicitud_recuperacion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_solicitud_recuperacion_vigencia CHECK (vence_en > solicitado_en)
);

CREATE INDEX ix_solicitud_recuperacion_usuario
    ON solicitud_recuperacion (usuario_id);
CREATE INDEX ix_solicitud_recuperacion_token
    ON solicitud_recuperacion (token);

CREATE TABLE alojamiento (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    ciudad VARCHAR NOT NULL,
    ubicacion_texto VARCHAR NOT NULL,
    tipo VARCHAR NOT NULL,
    capacidad_maxima INTEGER NOT NULL,
    CONSTRAINT fk_alojamiento_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_alojamiento_capacidad CHECK (capacidad_maxima > 0)
);

CREATE INDEX ix_alojamiento_usuario
    ON alojamiento (usuario_id);
CREATE INDEX ix_alojamiento_ciudad
    ON alojamiento (ciudad);

CREATE TABLE periodo_disponibilidad (
    id UUID PRIMARY KEY,
    alojamiento_id UUID NOT NULL,
    inicio DATE NOT NULL,
    fin DATE NOT NULL,
    tipo VARCHAR NOT NULL,
    CONSTRAINT fk_periodo_disponibilidad_alojamiento
        FOREIGN KEY (alojamiento_id) REFERENCES alojamiento (id),
    CONSTRAINT ck_periodo_disponibilidad_rango CHECK (inicio < fin),
    CONSTRAINT ck_periodo_disponibilidad_tipo CHECK (tipo IN ('DISPONIBLE', 'BLOQUEADO'))
);

CREATE INDEX ix_periodo_disponibilidad_alojamiento_fechas
    ON periodo_disponibilidad (alojamiento_id, inicio, fin);

CREATE TABLE politica_cancelacion (
    id UUID PRIMARY KEY,
    nombre VARCHAR NOT NULL,
    reglas TEXT[] NOT NULL
);

CREATE TABLE publicacion (
    id UUID PRIMARY KEY,
    alojamiento_id UUID NOT NULL,
    politica_cancelacion_id UUID NOT NULL,
    titulo VARCHAR NOT NULL,
    descripcion VARCHAR NOT NULL,
    precio_noche NUMERIC NOT NULL,
    reglas_uso TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    estado VARCHAR NOT NULL,
    cambios_pendientes_revision BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_publicacion_alojamiento UNIQUE (alojamiento_id),
    CONSTRAINT fk_publicacion_alojamiento
        FOREIGN KEY (alojamiento_id) REFERENCES alojamiento (id),
    CONSTRAINT fk_publicacion_politica_cancelacion
        FOREIGN KEY (politica_cancelacion_id) REFERENCES politica_cancelacion (id),
    CONSTRAINT ck_publicacion_estado CHECK (
        estado IN ('BORRADOR', 'PENDIENTE_REVISION', 'ACTIVA', 'PAUSADA', 'BLOQUEADA')
    )
);

CREATE INDEX ix_publicacion_politica_cancelacion
    ON publicacion (politica_cancelacion_id);
CREATE INDEX ix_publicacion_estado
    ON publicacion (estado);

CREATE TABLE imagen_publicacion (
    id UUID PRIMARY KEY,
    publicacion_id UUID NOT NULL,
    referencia VARCHAR NOT NULL,
    orden INTEGER NOT NULL,
    texto_alternativo VARCHAR NOT NULL,
    CONSTRAINT fk_imagen_publicacion_publicacion
        FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT ck_imagen_publicacion_orden CHECK (orden > 0)
);

CREATE INDEX ix_imagen_publicacion_publicacion
    ON imagen_publicacion (publicacion_id);

CREATE TABLE servicio_alojamiento (
    id UUID PRIMARY KEY,
    codigo VARCHAR NOT NULL,
    nombre VARCHAR NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_servicio_alojamiento_codigo UNIQUE (codigo)
);

CREATE TABLE publicacion_servicio (
    publicacion_id UUID NOT NULL,
    servicio_id UUID NOT NULL,
    CONSTRAINT pk_publicacion_servicio PRIMARY KEY (publicacion_id, servicio_id),
    CONSTRAINT fk_publicacion_servicio_publicacion
        FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT fk_publicacion_servicio_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicio_alojamiento (id)
);

CREATE INDEX ix_publicacion_servicio_servicio
    ON publicacion_servicio (servicio_id);

INSERT INTO servicio_alojamiento (id, codigo, nombre, activo) VALUES
('10000000-0000-4000-8000-000000000001', 'WIFI', 'Wi-Fi', TRUE),
('10000000-0000-4000-8000-000000000002', 'COCINA', 'Cocina equipada', TRUE),
('10000000-0000-4000-8000-000000000003', 'AIRE_ACONDICIONADO', 'Aire acondicionado', TRUE),
('10000000-0000-4000-8000-000000000004', 'CALEFACCION', 'Calefacción', TRUE),
('10000000-0000-4000-8000-000000000005', 'AGUA_CALIENTE', 'Agua caliente', TRUE),
('10000000-0000-4000-8000-000000000006', 'TV', 'Televisión', TRUE),
('10000000-0000-4000-8000-000000000007', 'LAVADORA', 'Lavadora', TRUE),
('10000000-0000-4000-8000-000000000008', 'PARQUEADERO', 'Parqueadero', TRUE),
('10000000-0000-4000-8000-000000000009', 'PISCINA', 'Piscina', TRUE),
('10000000-0000-4000-8000-000000000010', 'ESPACIO_TRABAJO', 'Espacio de trabajo', TRUE),
('10000000-0000-4000-8000-000000000011', 'DESAYUNO_INCLUIDO', 'Desayuno incluido', TRUE),
('10000000-0000-4000-8000-000000000012', 'ROPA_DE_CAMA', 'Ropa de cama y toallas', TRUE);

INSERT INTO politica_cancelacion (id, nombre, reglas) VALUES
('20000000-0000-4000-8000-000000000001', 'Flexible', ARRAY[
    'Reembolso del 100 % si se cancela hasta 24 horas antes del check-in.',
    'Con menos de 24 horas de anticipación, se reembolsa el total menos la primera noche.'
]::TEXT[]),
('20000000-0000-4000-8000-000000000002', 'Moderada', ARRAY[
    'Reembolso del 100 % si se cancela con 5 días o más de anticipación al check-in.',
    'Entre 5 días y 24 horas antes, se reembolsa el 50 %.',
    'Con menos de 24 horas de anticipación, no hay reembolso.'
]::TEXT[]),
('20000000-0000-4000-8000-000000000003', 'Estricta', ARRAY[
    'Reembolso del 50 % si se cancela con 14 días o más de anticipación al check-in.',
    'Con menos de 14 días de anticipación, no hay reembolso.'
]::TEXT[]);
