

DROP TABLE IF EXISTS datos_biometricos_login CASCADE;
DROP TABLE IF EXISTS login                   CASCADE;
DROP TABLE IF EXISTS cuentas                 CASCADE;
DROP TABLE IF EXISTS domicilios              CASCADE;
DROP TABLE IF EXISTS clientes                CASCADE;
DROP TABLE IF EXISTS nacionalidades          CASCADE;
DROP TABLE IF EXISTS gestopago_tokens        CASCADE;


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

-- ---------------------------------------------------------------------
-- nacionalidades (catálogo)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS nacionalidades (
    id      SERIAL PRIMARY KEY,
    nombre  TEXT NOT NULL,
    CONSTRAINT uq_nacionalidades_nombre UNIQUE (nombre)
);

INSERT INTO nacionalidades (nombre) VALUES
    ('Mexicana'), ('Estadounidense'), ('Canadiense'), ('Española'),
    ('Francesa'), ('Alemana'), ('Italiana'), ('Portuguesa'),
    ('Colombiana'), ('Argentina'), ('Chilena'), ('Peruana'),
    ('Brasileña'), ('Japonesa'), ('China')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- clientes
-- ---------------------------------------------------------------------
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
    nacionalidad_id       INTEGER        NOT NULL,
    estado_civil          TEXT           NOT NULL,
    correo_electronico    TEXT           NOT NULL,
    telefono_movil        NUMERIC(10,0)  NOT NULL,
    telefono_alternativo  NUMERIC(10,0),
    ocupacion             TEXT           NOT NULL,
    empresa               TEXT           NOT NULL,
    ingreso_mensual       NUMERIC(12,2)  NOT NULL,
    activo                BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion        TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_clientes_curp   UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico),
    CONSTRAINT fk_clientes_nacionalidad
        FOREIGN KEY (nacionalidad_id) REFERENCES nacionalidades (id),

    CONSTRAINT ck_clientes_ingreso_mensual CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_ingreso_maximo  CHECK (ingreso_mensual <= 9999999.99),
    CONSTRAINT ck_clientes_telefono_movil  CHECK (telefono_movil BETWEEN 1000000000 AND 9999999999),
    CONSTRAINT ck_clientes_telefono_alt    CHECK (telefono_alternativo IS NULL
                                                  OR telefono_alternativo BETWEEN 1000000000 AND 9999999999),
    CONSTRAINT ck_clientes_curp_formato    CHECK (curp ~ '^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$'),
    CONSTRAINT ck_clientes_rfc_formato     CHECK (rfc ~ '^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{2,3}$'),
    CONSTRAINT ck_clientes_sexo            CHECK (sexo IN ('H', 'M')),
    CONSTRAINT ck_clientes_estado_civil    CHECK (estado_civil IN
                                                  ('Soltero(a)', 'Casado(a)', 'Divorciado(a)', 'Viudo(a)', 'Unión libre')),
    CONSTRAINT ck_clientes_correo_formato  CHECK (correo_electronico ~* '^[^@[:space:]]+@[^@[:space:]]+\.[a-z]{2,}$'),
    CONSTRAINT ck_clientes_textos_no_vacios CHECK (length(btrim(nombre)) > 0
                                                  AND length(btrim(apellido_paterno)) > 0
                                                  AND length(btrim(apellido_materno)) > 0
                                                  AND length(btrim(ocupacion)) > 0
                                                  AND length(btrim(empresa)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_clientes_activo          ON clientes (activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion  ON clientes (fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_clientes_nacionalidad_id ON clientes (nacionalidad_id);

-- ---------------------------------------------------------------------
-- domicilios (1 a 1 con clientes)
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
    CONSTRAINT ck_domicilios_cp CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT ck_domicilios_textos_no_vacios CHECK (length(btrim(calle)) > 0
                                                    AND length(btrim(numero_exterior)) > 0
                                                    AND length(btrim(colonia)) > 0
                                                    AND length(btrim(municipio)) > 0
                                                    AND length(btrim(estado)) > 0
                                                    AND length(btrim(pais)) > 0)
);

-- ---------------------------------------------------------------------
-- cuentas (1 a N con clientes)
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
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT ck_cuentas_numero_formato CHECK (numero_cuenta ~ '^[0-9]{10}$')
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus     ON cuentas (estatus);

-- ---------------------------------------------------------------------
-- login (1 a 1 con clientes). La contraseña SOLO puede ser un hash BCrypt.
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
    CONSTRAINT uq_login_usuario UNIQUE (usuario),
    CONSTRAINT ck_login_usuario_formato CHECK (usuario ~ '^[A-Za-z0-9._-]{4,50}$'),
    CONSTRAINT ck_login_contrasena_hash CHECK (contrasena ~ '^\$2[aby]\$[0-9]{2}\$.{53}$')
);

-- ---------------------------------------------------------------------
-- datos_biometricos_login (1 a 1 con login). Todo cifrado en Base64.
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
    CONSTRAINT uq_datos_biometricos_login UNIQUE (login_id),
    CONSTRAINT ck_biometricos_dato_cifrado  CHECK (dato_biometrico_cifrado IS NULL
                                                   OR dato_biometrico_cifrado ~ '^[A-Za-z0-9+/]+={0,2}$'),
    CONSTRAINT ck_biometricos_token_cifrado CHECK (token_sesion_cifrado IS NULL
                                                   OR token_sesion_cifrado ~ '^[A-Za-z0-9+/]+={0,2}$'),
    CONSTRAINT ck_biometricos_token_hash    CHECK (token_sesion_hash IS NULL
                                                   OR token_sesion_hash ~ '^[A-Za-z0-9+/]+={0,2}$')
);


select * from clientes;
select * from cuentas;
CREATE INDEX IF NOT EXISTS idx_datos_biometricos_token_hash ON datos_biometricos_login (token_sesion_hash);