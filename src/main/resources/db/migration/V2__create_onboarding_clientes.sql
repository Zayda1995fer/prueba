DROP TABLE IF EXISTS datos_biometricos_login CASCADE;
DROP TABLE IF EXISTS login CASCADE;
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS domicilios CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS gestopago_tokens CASCADE;

-- ---------------------------------------------------------------------
-- Tabla: gestopago_tokens (integración externa, módulo base del proyecto)
-- ---------------------------------------------------------------------
CREATE TABLE gestopago_tokens (
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
-- Tabla: clientes (tabla principal del módulo de onboarding)
-- ---------------------------------------------------------------------
CREATE TABLE clientes (
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
                          CONSTRAINT ck_clientes_ingreso_mensual CHECK (ingreso_mensual > 0),
                          CONSTRAINT ck_clientes_telefono_movil
                              CHECK (telefono_movil >= 1000000000 AND telefono_movil <= 9999999999),
                          CONSTRAINT ck_clientes_telefono_alt
                              CHECK (telefono_alternativo IS NULL
                                  OR (telefono_alternativo >= 1000000000 AND telefono_alternativo <= 9999999999))
);

CREATE INDEX idx_clientes_activo        ON clientes (activo);
CREATE INDEX idx_clientes_fecha_creacion ON clientes (fecha_creacion);

-- ---------------------------------------------------------------------
-- Tabla: domicilios (relación 1 a 1 con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE domicilios (
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
-- Tabla: cuentas (relación 1 a N con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE cuentas (
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

CREATE INDEX idx_cuentas_cliente_id ON cuentas (cliente_id);
CREATE INDEX idx_cuentas_estatus     ON cuentas (estatus);

-- ---------------------------------------------------------------------
-- Tabla: login (acceso del cliente, relación 1 a 1 con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE login (
                       id                  SERIAL            PRIMARY KEY,
                       cliente_id          INTEGER           NOT NULL,
                       usuario             TEXT              NOT NULL,
                       contrasena          TEXT              NOT NULL,
                       activo              BOOLEAN           NOT NULL DEFAULT TRUE,
                       fecha_creacion      TIMESTAMP         NOT NULL DEFAULT NOW(),

                       CONSTRAINT fk_login_cliente
                           FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE,
                       CONSTRAINT uq_login_cliente  UNIQUE (cliente_id),
                       CONSTRAINT uq_login_usuario  UNIQUE (usuario)
);

-- ---------------------------------------------------------------------
-- Tabla: datos_biometricos_login (bóveda cifrada, relación 1 a 1 con login)
-- ---------------------------------------------------------------------
CREATE TABLE datos_biometricos_login (
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

CREATE INDEX idx_datos_biometricos_token_hash ON datos_biometricos_login (token_sesion_hash);

-- =====================================================================
-- ALTER: convertir telefono_movil / telefono_alternativo de TEXT a NUMERIC
-- Ejecutar sobre tu base de datos YA CREADA (no borra nada).
-- =====================================================================

-- 1) Quitar el CHECK viejo si existe (no debería, pero por si acaso)
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS ck_clientes_telefono_movil;
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS ck_clientes_telefono_alt;

-- 2) Cambiar el tipo de columna. USING convierte lo que ya esté guardado
--    (texto como '5512345678') a numérico automáticamente.
ALTER TABLE clientes
ALTER COLUMN telefono_movil TYPE NUMERIC(10,0)
    USING telefono_movil::NUMERIC(10,0);

ALTER TABLE clientes
ALTER COLUMN telefono_alternativo TYPE NUMERIC(10,0)
    USING telefono_alternativo::NUMERIC(10,0);

-- 3) Restricción: exactamente 10 dígitos (el móvil es obligatorio,
--    el alternativo puede ser NULL, por eso ese CHECK deja pasar NULL).
ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_telefono_movil
        CHECK (telefono_movil >= 1000000000 AND telefono_movil <= 9999999999);

ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_telefono_alt
        CHECK (telefono_alternativo IS NULL
            OR (telefono_alternativo >= 1000000000 AND telefono_alternativo <= 9999999999));



select * from clientes;


-- =====================================================================
-- Catálogo: nacionalidades
--
-- Antes, clientes.nacionalidad era TEXT libre (cualquier cadena que el
-- cliente escribiera). Se normaliza a un catálogo real: una tabla aparte
-- con las nacionalidades permitidas, y clientes.nacionalidad_id pasa a
-- ser una llave foránea hacia esa tabla. Así se evita capturar la misma
-- nacionalidad de formas distintas ("Mexicana", "mexicana", "MEXICANA",
-- "mexico") y se puede validar contra un catálogo cerrado.
-- =====================================================================

CREATE TABLE IF NOT EXISTS nacionalidades (
                                              id      SERIAL PRIMARY KEY,
                                              nombre  TEXT NOT NULL,

                                              CONSTRAINT uq_nacionalidades_nombre UNIQUE (nombre)
    );

INSERT INTO nacionalidades (nombre) VALUES
                                        ('Mexicana'),
                                        ('Estadounidense'),
                                        ('Canadiense'),
                                        ('Española'),
                                        ('Francesa'),
                                        ('Alemana'),
                                        ('Italiana'),
                                        ('Portuguesa'),
                                        ('Colombiana'),
                                        ('Argentina'),
                                        ('Chilena'),
                                        ('Peruana'),
                                        ('Brasileña'),
                                        ('Japonesa'),
                                        ('China')
    ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- clientes.nacionalidad (TEXT libre) -> clientes.nacionalidad_id (FK)
-- ---------------------------------------------------------------------

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS nacionalidad_id INTEGER;

-- Migración de los datos que ya existieran: intenta emparejar el texto
-- libre que haya (sin importar mayúsculas/espacios) contra el catálogo.
UPDATE clientes c
SET nacionalidad_id = n.id
    FROM nacionalidades n
WHERE c.nacionalidad_id IS NULL
  AND lower(trim(c.nacionalidad)) = lower(n.nombre);

-- Cualquier cliente que ya existiera con una nacionalidad que no está en
-- el catálogo (p. ej. estaba mal escrita) se deja como "Mexicana" por
-- default, para no dejar nacionalidad_id en NULL. Revisa esos registros
-- a mano si tu base de datos ya tenía clientes capturados.
UPDATE clientes
SET nacionalidad_id = (SELECT id FROM nacionalidades WHERE nombre = 'Mexicana')
WHERE nacionalidad_id IS NULL;

ALTER TABLE clientes ALTER COLUMN nacionalidad_id SET NOT NULL;

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_nacionalidad
        FOREIGN KEY (nacionalidad_id) REFERENCES nacionalidades (id);

ALTER TABLE clientes DROP COLUMN IF EXISTS nacionalidad;

CREATE INDEX IF NOT EXISTS idx_clientes_nacionalidad_id ON clientes (nacionalidad_id);