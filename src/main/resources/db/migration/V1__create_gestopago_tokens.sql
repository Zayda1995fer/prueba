CREATE TABLE IF NOT EXISTS gestopago_tokens (
                                                id                  SERIAL PRIMARY KEY,
                                                id_distribuidor     INTEGER         NOT NULL,
                                                codigo_dispositivo  TEXT            NOT NULL,
                                                token               TEXT            NOT NULL,
                                                token_type          TEXT,
                                                expires_in          BIGINT,
                                                fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
    );
