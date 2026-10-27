-- Tablas del pago de servicios con GestoPago. Se puede ejecutar sin borrar tus datos.

-- ---------------------------------------------------------------------
-- catalogo_productos_pago: qué se puede pagar, qué destino exige
-- (TELEFONO o REFERENCIA) y qué montos acepta. El id_producto debe ser el
-- de GestoPago (GET /productos).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalogo_productos_pago (
    id_producto        INTEGER        PRIMARY KEY,
    id_servicio        INTEGER        NOT NULL,
    nombre             TEXT           NOT NULL,
    tipo_destino       TEXT           NOT NULL,
    monto_minimo       NUMERIC(15,2)  NOT NULL,
    monto_maximo       NUMERIC(15,2)  NOT NULL,
    montos_permitidos  TEXT,
    activo             BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT ck_catalogo_destino CHECK (tipo_destino IN ('TELEFONO', 'REFERENCIA')),
    CONSTRAINT ck_catalogo_montos  CHECK (monto_minimo > 0 AND monto_maximo >= monto_minimo),
    CONSTRAINT ck_catalogo_lista   CHECK (montos_permitidos IS NULL
                                          OR montos_permitidos ~ '^[0-9]+(\.[0-9]{1,2})?(,[0-9]+(\.[0-9]{1,2})?)*$')
);

-- Ajusta ids y servicios a los reales de GestoPago
INSERT INTO catalogo_productos_pago (id_producto, id_servicio, nombre, tipo_destino, monto_minimo, monto_maximo, montos_permitidos)
VALUES (1, 1, 'Pago de luz (CFE)',  'REFERENCIA', 10.00, 10000.00, NULL),
       (2, 2, 'Recarga telefónica', 'TELEFONO',   10.00,   500.00, '10,20,30,50,100,200,300,500')
ON CONFLICT (id_producto) DO NOTHING;

-- ---------------------------------------------------------------------
-- pagos: cada operación con GestoPago. El saldo se descuenta al crear el
-- pago (PENDIENTE) y solo se devuelve si el proveedor confirma que NO lo aplicó.
--   PENDIENTE        saldo retenido, envío en curso
--   EXITOSO          confirmado por el proveedor
--   RECHAZADO        el proveedor no lo aplicó; saldo reintegrado
--   POR_CONCILIAR    no se sabe el resultado (timeout, 5xx); saldo retenido
--   REVISION_MANUAL  agotó las consultas; requiere revisión de una persona
-- id_solicitud (UNIQUE) es la llave de idempotencia: impide cobrar dos veces la misma solicitud.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pagos (
    id                     SERIAL         PRIMARY KEY,
    id_solicitud           TEXT           NOT NULL,
    referencia_proveedor   TEXT           NOT NULL,
    huella_solicitud       TEXT           NOT NULL,
    cuenta_id              INTEGER        NOT NULL,
    cliente_id             INTEGER        NOT NULL,
    id_producto            INTEGER        NOT NULL,
    id_servicio            INTEGER        NOT NULL,
    monto                  NUMERIC(15,2)  NOT NULL,
    destino                TEXT           NOT NULL,
    estatus                TEXT           NOT NULL,
    folio_transaccion      TEXT,
    codigo_respuesta       TEXT,
    intentos_conciliacion  INTEGER        NOT NULL DEFAULT 0,
    saldo_reintegrado      BOOLEAN        NOT NULL DEFAULT FALSE,
    fecha_creacion         TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pagos_cuenta   FOREIGN KEY (cuenta_id)   REFERENCES cuentas (id),
    CONSTRAINT fk_pagos_cliente  FOREIGN KEY (cliente_id)  REFERENCES clientes (id),
    CONSTRAINT fk_pagos_producto FOREIGN KEY (id_producto) REFERENCES catalogo_productos_pago (id_producto),
    CONSTRAINT uq_pagos_solicitud  UNIQUE (id_solicitud),
    CONSTRAINT uq_pagos_referencia UNIQUE (referencia_proveedor),
    CONSTRAINT ck_pagos_monto   CHECK (monto > 0),
    CONSTRAINT ck_pagos_estatus CHECK (estatus IN ('PENDIENTE', 'EXITOSO', 'RECHAZADO', 'POR_CONCILIAR', 'REVISION_MANUAL'))
);

CREATE INDEX IF NOT EXISTS idx_pagos_conciliacion ON pagos (estatus, fecha_actualizacion);
CREATE INDEX IF NOT EXISTS idx_pagos_duplicados   ON pagos (cuenta_id, id_producto, destino, monto, fecha_creacion);

-- ---------------------------------------------------------------------
-- codigos_2fa: códigos de un solo uso para autorizar pagos (solo se guarda el hash)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS codigos_2fa (
    id                 SERIAL     PRIMARY KEY,
    cliente_id         INTEGER    NOT NULL,
    cuenta_id          INTEGER    NOT NULL,
    codigo_hash        TEXT       NOT NULL,
    fecha_creacion     TIMESTAMP  NOT NULL DEFAULT NOW(),
    fecha_expiracion   TIMESTAMP  NOT NULL,
    intentos           INTEGER    NOT NULL DEFAULT 0,
    usado              BOOLEAN    NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_2fa_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT fk_2fa_cuenta  FOREIGN KEY (cuenta_id)  REFERENCES cuentas (id)  ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_2fa_vigente ON codigos_2fa (cliente_id, cuenta_id, usado);
