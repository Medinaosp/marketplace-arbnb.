CREATE TABLE reporte_gestion (
    id UUID PRIMARY KEY,
    solicitante_id UUID NOT NULL,
    tipo VARCHAR NOT NULL,
    formato VARCHAR NOT NULL,
    filtros TEXT NOT NULL,
    generado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_reporte_gestion_solicitante
        FOREIGN KEY (solicitante_id) REFERENCES usuario (id),
    CONSTRAINT ck_reporte_gestion_tipo CHECK (tipo IN ('ADMINISTRADOR', 'ANFITRION')),
    CONSTRAINT ck_reporte_gestion_formato CHECK (formato IN ('PDF', 'CSV'))
);
CREATE INDEX ix_reporte_gestion_solicitante_fecha
    ON reporte_gestion (solicitante_id, generado_en DESC);

CREATE TABLE evento_auditoria (
    id UUID PRIMARY KEY,
    actor_id UUID,
    accion VARCHAR NOT NULL,
    entidad VARCHAR NOT NULL,
    entidad_id UUID,
    resultado VARCHAR NOT NULL,
    ocurrido_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_evento_auditoria_actor
        FOREIGN KEY (actor_id) REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT ck_evento_auditoria_resultado CHECK (resultado IN ('EXITOSO', 'FALLIDO'))
);
CREATE INDEX ix_evento_auditoria_fecha ON evento_auditoria (ocurrido_en DESC);
CREATE INDEX ix_evento_auditoria_actor_fecha ON evento_auditoria (actor_id, ocurrido_en DESC);
