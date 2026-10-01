CREATE TABLE IF NOT EXISTS clientes (
                                        id                    SERIAL PRIMARY KEY,
                                        nombre                TEXT           NOT NULL,
                                        segundo_nombre        TEXT,
                                        apellido_paterno      TEXT           NOT NULL,
                                        apellido_materno      TEXT           NOT NULL,
                                        fecha_nacimiento      DATE           NOT NULL,
                                        curp                  TEXT           NOT NULL,
                                        rfc                   TEXT           NOT NULL,
                                        sexo                  TEXT           NOT NULL,
                                        nacionalidad          TEXT           NOT NULL,
                                        estado_civil          TEXT           NOT NULL,
                                        correo_electronico    TEXT           NOT NULL,
                                        telefono_movil        TEXT           NOT NULL,
                                        telefono_alternativo  TEXT,
                                        ocupacion             TEXT           NOT NULL,
                                        empresa               TEXT           NOT NULL,
                                        ingreso_mensual       NUMERIC(12,2)  NOT NULL,
    activo                BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion        TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_clientes_curp   UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico),
    CONSTRAINT ck_clientes_ingreso_mensual CHECK (ingreso_mensual > 0)
    );

CREATE INDEX IF NOT EXISTS idx_clientes_activo           ON clientes (activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion    ON clientes (fecha_creacion);

-- ---------------------------------------------------------------------
-- Tabla: domicilios (relación 1 a 1 con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS domicilios (
                                          id                SERIAL PRIMARY KEY,
                                          cliente_id        INTEGER   NOT NULL,
                                          calle             TEXT      NOT NULL,
                                          numero_exterior   TEXT      NOT NULL,
                                          numero_interior   TEXT,
                                          colonia           TEXT      NOT NULL,
                                          municipio         TEXT      NOT NULL,
                                          estado            TEXT      NOT NULL,
                                          codigo_postal     TEXT      NOT NULL,
                                          pais              TEXT      NOT NULL,

                                          CONSTRAINT fk_domicilios_cliente
                                          FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT ck_domicilios_cp CHECK (codigo_postal ~ '^[0-9]{5}$')
    );

-- ---------------------------------------------------------------------
-- Tabla: cuentas (relación 1 a N: un cliente puede tener varias cuentas
-- en el tiempo, aunque el alta automática solo cree una)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuentas (
                                       id               SERIAL PRIMARY KEY,
                                       cliente_id       INTEGER        NOT NULL,
                                       numero_cuenta    TEXT           NOT NULL,
                                       saldo            NUMERIC(15,2)  NOT NULL DEFAULT 0,
    estatus          TEXT           NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cuentas_cliente
    FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta),
    CONSTRAINT ck_cuentas_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA'))
    );

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus     ON cuentas (estatus);

-- ---------------------------------------------------------------------
-- Tabla: login
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login (
                                     id                  SERIAL            PRIMARY KEY,
                                     cliente_id          INTEGER           NOT NULL,
                                     usuario             TEXT              NOT NULL,
                                     contrasena          TEXT              NOT NULL,
                                     activo              BOOLEAN           NOT NULL DEFAULT TRUE,
                                     fecha_creacion      TIMESTAMP         NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_login_cliente
    FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uq_login_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_login_usuario UNIQUE (usuario)
    );

-- ---------------------------------------------------------------------
-- Tabla: datos_biometricos_login (bóveda de datos sensibles, cifrados)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS datos_biometricos_login (
                                                       id                        SERIAL      PRIMARY KEY,
                                                       login_id                  INTEGER     NOT NULL,
                                                       dato_biometrico_cifrado   TEXT,
                                                       token_sesion_cifrado      TEXT,
                                                       token_sesion_hash         TEXT,
                                                       ultima_actividad          TIMESTAMP,
                                                       fecha_actualizacion       TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_datos_biometricos_login
    FOREIGN KEY (login_id) REFERENCES login (id) ON DELETE CASCADE,
    CONSTRAINT uq_datos_biometricos_login UNIQUE (login_id)
    );

CREATE INDEX IF NOT EXISTS idx_datos_biometricos_token_hash ON datos_biometricos_login (token_sesion_hash);