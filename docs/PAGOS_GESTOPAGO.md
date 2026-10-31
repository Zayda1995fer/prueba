# Pago de servicios con GestoPago

## Endpoints (todos `POST` con `@RequestBody` y cabecera `Authorization: Bearer <tokenSesion>`)

| Endpoint | Para qué | Respuesta |
|---|---|---|
| `/pagos/2fa` | Genera el código de un solo uso (vale 5 min, 3 intentos) | 201 |
| `/pagos` | Realiza el pago | 201 confirmado · 202 por conciliar |
| `/pagos/consultar` | Consulta el estado de un pago por su `idSolicitud` | 200 |

Ejemplo de pago de luz (usa `referencia`; una recarga usaría `telefono`, nunca los dos):

```json
{
  "idSolicitud": "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c",
  "numeroCuenta": "1234567890",
  "idUser": 5,
  "idServicio": 1,
  "idProducto": 1,
  "monto": 350.00,
  "referencia": "ABC12345",
  "codigo2fa": "123456"
}
```

`idSolicitud` es un UUID que genera quien llama. Es la llave de **idempotencia**: repetir la misma
solicitud (por un timeout, un doble clic, un reintento) devuelve el resultado original y nunca cobra dos veces.

## Identidad y autorización del cliente

Todas las rutas de `/pagos` y `/cuentas` exigen la cabecera `Authorization: Bearer <tokenSesion>`
(el token que entrega `POST /login`). `SesionInterceptor` la valida **antes** del controller:

1. Sin cabecera o con otro esquema → **401** (`SESION_REQUERIDA`).
2. Token con formato inválido → **401** sin consultar la base de datos.
3. Token inexistente o vencido por inactividad → **401** (`TOKEN_INVALIDO` / `SESION_EXPIRADA`).
4. Sesión válida → se renueva la ventana de inactividad y el id del cliente queda como atributo de la petición.

El `idUser` del body **no se cree por sí solo**: el servicio lo compara con el cliente de la sesión
(`VerificadorTitular`) y responde **403** (`OPERACION_NO_AUTORIZADA`) si no coinciden, antes de validar
el catálogo, tocar el saldo o llamar al proveedor. Después se verifica que la cuenta sea de ese cliente,
que cliente y cuenta estén activos, el saldo, los duplicados y el código 2FA.

Las consultas de cuenta (`/cuentas/consultar`, `/cuentas/saldo`, `/cuentas/activas`) solo devuelven cuentas
del cliente autenticado; una cuenta ajena responde igual que una inexistente (no se revela que existe).

## Estrategia de cobro: descontar primero, confirmar después

```
1. Validar contra el catálogo (servicio, producto, destino, monto)        sin BD
2. TRANSACCIÓN CORTA: bloquear cuenta · validar cliente/saldo/duplicado/2FA
   · descontar saldo · guardar pago PENDIENTE                              BD
3. Llamar a GestoPago (sendTx)                                            red, SIN bloqueos abiertos
4. TRANSACCIÓN CORTA: aplicar el resultado                                 BD
```

Por qué así:
- **Consistencia del saldo:** el dinero sale de la cuenta antes de salir hacia el proveedor; nunca se
  puede pagar con saldo que ya se gastó. Si el pago no se aplica, el saldo se devuelve.
- **Concurrencia:** el paso 2 bloquea la fila de la cuenta (`PESSIMISTIC_WRITE`); dos pagos simultáneos
  se atienden uno por uno y el segundo ya ve el saldo descontado. El bloqueo dura milisegundos porque la
  llamada de red queda **fuera** de la transacción.
- **Sin cargos duplicados:** `id_solicitud` es `UNIQUE`; además se rechaza un pago idéntico (misma cuenta,
  producto, destino y monto) dentro de una ventana de 60 s.

## Qué se hace con cada resultado

| Lo que pasó con GestoPago | Estatus | Saldo |
|---|---|---|
| Código de éxito | `EXITOSO` | queda descontado |
| Rechazo definitivo (código de error, 4xx) | `RECHAZADO` | **se devuelve** (una sola vez) |
| Timeout, corte de red, 5xx, 408, 409, respuesta ilegible | `POR_CONCILIAR` | **queda retenido** |
| Se agotaron las consultas de conciliación | `REVISION_MANUAL` | queda retenido |

**Un timeout NO se interpreta como fallo.** Puede que el proveedor sí haya aplicado el pago.

### Conciliación
Una tarea programada (`ConciliacionServiceImpl`, cada 60 s) toma los pagos `PENDIENTE` / `POR_CONCILIAR`
con más de 120 s sin cambios y **consulta** a GestoPago por la referencia propia de la operación
(`idTransaccion`):
- aplicó el pago → `EXITOSO`
- confirma que no lo aplicó → `RECHAZADO` y se devuelve el saldo
- no responde o no es concluyente → sigue `POR_CONCILIAR` (tras 10 intentos pasa a `REVISION_MANUAL`)

El `sendTx` **nunca se reenvía automáticamente** (Feign no reintenta y la conciliación solo consulta).
Reenviar solo sería seguro si el proveedor garantiza idempotencia por `idTransaccion`; en ese caso se
agregaría detrás de una bandera de configuración.

También cubre una caída de la aplicación entre el descuento y la llamada: el pago queda `PENDIENTE` y la
conciliación lo resuelve.

## Seguridad

- **Token Bearer:** se obtiene con `authenticate` y se guarda **cifrado con AES-GCM** en `gestopago_tokens`
  (solo se descifra en memoria al usarlo; un token que no se puede descifrar se renueva); nunca va en el código
  ni en respuestas. La contraseña de autenticación se lee de la variable de entorno `GESTOPAGO_PASSWORD`.
  Si el proveedor responde 401/403 se renueva el token y se reintenta **una** vez (no se procesó nada).
- **Transporte:** la aplicación no arranca si la URL de GestoPago no es `https` (se permite `localhost`).
- **Logs:** solo ids internos (pago, cliente), estatus y códigos. Nunca token, contraseña, teléfono,
  referencia completa, número de cuenta ni cuerpo de la petición. Los errores de Feign se registran solo
  por tipo (su mensaje trae la URL con parámetros).
- **Respuestas:** mensajes del catálogo `CatalogoError`; el destino se devuelve enmascarado (`****2345`);
  los errores inesperados responden un mensaje genérico (la traza queda solo en el servidor).
- **Peticiones:** mapper estricto (un campo no previsto como `saldoActual` se rechaza), montos con máximo
  2 decimales y validados contra el rango/lista del catálogo; el 2FA guarda solo su hash.

## Configuración (variables de entorno)

| Variable | Uso |
|---|---|
| `GESTOPAGO_PASSWORD` | Contraseña de autenticación con GestoPago (obligatoria) |
| `MAIL_USUARIO` / `MAIL_CLAVE` / `NOTIFICACIONES_ACTIVO` | Aviso por correo (contraseña de aplicación de Gmail) |
| `PAGOS_2FA_DEVOLVER_CODIGO` | `false` por defecto; `true` solo en práctica local |
| `APP_AES_KEY` | Llave AES-256 en Base64 (`openssl rand -base64 32`). Obligatoria: sin ella no arranca |
| `DB_URL` / `DB_USUARIO` / `DB_CLAVE` | Conexión a PostgreSQL (`DB_CLAVE` obligatoria) |

Copia `.env.example` como `.env` (está en `.gitignore`) o configura las variables en IntelliJ / Render.

Supuestos pendientes de confirmar con la documentación de GestoPago (`application.properties`):
ruta de `sendTx` y de la consulta, código de éxito, códigos "en proceso" / "no aplicada", significado de `upc`.
