CREATE TABLE tarifa_servicio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    servicio_id UUID NOT NULL UNIQUE REFERENCES servicio_alojamiento(id),
    monto NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (monto >= 0),
    modalidad VARCHAR(20) NOT NULL DEFAULT 'POR_ESTADIA' CHECK (modalidad IN ('POR_ESTADIA','POR_NOCHE')),
    actualizada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tarifa_servicio (servicio_id, monto, modalidad)
SELECT id, 0, 'POR_ESTADIA' FROM servicio_alojamiento ON CONFLICT (servicio_id) DO NOTHING;

CREATE TABLE descuento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id UUID NOT NULL REFERENCES publicacion(id),
    codigo VARCHAR(40) NOT NULL,
    porcentaje NUMERIC(5,2) NOT NULL CHECK (porcentaje > 0 AND porcentaje <= 100),
    noches_minimas INTEGER NOT NULL DEFAULT 1 CHECK (noches_minimas > 0),
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_descuento_rango CHECK (vigente_desde <= vigente_hasta),
    CONSTRAINT uq_descuento_publicacion_codigo UNIQUE (publicacion_id, codigo)
);
CREATE INDEX ix_descuento_codigo_activo ON descuento (codigo, activo);

CREATE TABLE cotizacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id UUID NOT NULL REFERENCES publicacion(id),
    huesped_id UUID NOT NULL REFERENCES usuario(id),
    checkin DATE NOT NULL,
    checkout DATE NOT NULL,
    huespedes INTEGER NOT NULL CHECK (huespedes > 0),
    noches INTEGER NOT NULL CHECK (noches > 0),
    servicios_ids TEXT NOT NULL DEFAULT '',
    subtotal NUMERIC(14,2) NOT NULL,
    tarifa_servicios NUMERIC(14,2) NOT NULL,
    descuento_id UUID REFERENCES descuento(id),
    valor_descuento NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL,
    creada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vence_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE','USADA','VENCIDA')),
    CONSTRAINT ck_cotizacion_fechas CHECK (checkin < checkout),
    CONSTRAINT ck_cotizacion_vigencia CHECK (vence_en > creada_en)
);
CREATE INDEX ix_cotizacion_huesped ON cotizacion (huesped_id, creada_en DESC);

CREATE TABLE reserva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cotizacion_id UUID NOT NULL UNIQUE REFERENCES cotizacion(id),
    publicacion_id UUID NOT NULL REFERENCES publicacion(id),
    huesped_id UUID NOT NULL REFERENCES usuario(id),
    checkin DATE NOT NULL,
    checkout DATE NOT NULL,
    noches INTEGER NOT NULL,
    total_acordado NUMERIC(14,2) NOT NULL,
    estado VARCHAR(24) NOT NULL CHECK (estado IN ('PENDIENTE_PAGO','PAGO_FALLIDO','CONFIRMADA','CANCELADA','COMPLETADA','EXPIRADA')),
    creada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vence_pago_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    confirmada_en TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT ck_reserva_fechas CHECK (checkin < checkout)
);
CREATE INDEX ix_reserva_publicacion_fechas ON reserva (publicacion_id, checkin, checkout, estado);
CREATE INDEX ix_reserva_huesped ON reserva (huesped_id, creada_en DESC);

CREATE TABLE pago_simulado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL UNIQUE REFERENCES reserva(id),
    monto NUMERIC(14,2) NOT NULL CHECK (monto >= 0),
    referencia VARCHAR(80) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('APROBADO','RECHAZADO')),
    solicitado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    procesado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE cancelacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL UNIQUE REFERENCES reserva(id),
    solicitada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    porcentaje_reembolso NUMERIC(5,2) NOT NULL CHECK (porcentaje_reembolso BETWEEN 0 AND 100),
    devolucion_simulada NUMERIC(14,2) NOT NULL CHECK (devolucion_simulada >= 0),
    politica_aplicada VARCHAR(80) NOT NULL
);

CREATE TABLE resena (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL UNIQUE REFERENCES reserva(id),
    publicacion_id UUID NOT NULL REFERENCES publicacion(id),
    huesped_id UUID NOT NULL REFERENCES usuario(id),
    calificacion INTEGER NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    contenido VARCHAR(2000) NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'PENDIENTE_REVISION' CHECK (estado IN ('PENDIENTE_REVISION','VISIBLE','OCULTA')),
    creada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    actualizada_en TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX ix_resena_publicacion_estado ON resena (publicacion_id, estado, creada_en DESC);
