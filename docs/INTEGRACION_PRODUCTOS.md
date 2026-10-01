# Integración con el catálogo de productos

## ¿Qué hace esta parte del proyecto?
Se conecta a un servicio externo (GestoPago) para traer la lista de productos
disponibles. La dirección exacta es:

`GET /sistema/service/getProductList.do`

## ¿Cómo funciona, en palabras simples?
1. El programa busca el "pase de entrada" (token) que ya se guardó antes en la
   base de datos. Este token se renueva solo cada cierto tiempo, así que no hay
   que preocuparse por que se venza.
2. Si no hay token guardado, se avisa que no se puede continuar.
3. Si sí hay token, se arma la petición y se le pide al servicio externo la
   lista de productos.
4. Si el servicio externo responde bien, se regresa la lista de productos.
5. Si algo sale mal (el token no sirve, el servicio tarda mucho, o responde con
   un error), se anota en los logs qué pasó y se avisa con un mensaje claro,
   sin mostrar información sensible como el token.

## ¿Por qué se hizo así?
- El token nunca se escribe directamente en el código: siempre se busca en la
  configuración o en la base de datos.
- Se reutiliza el mismo mecanismo de token que el proyecto ya tenía para
  GestoPago, en vez de crear uno nuevo desde cero.
- Los errores se separan en categorías simples (sin token, sin respuesta a
  tiempo, error del servicio) para que el mensaje en el log sea claro y fácil
  de entender.

## Pendiente de revisar
- Los nombres de los campos de un producto (`codigo`, `nombre`, `precio`, etc.)
  son un ejemplo. Hay que confirmarlos cuando se pruebe contra el servicio real.
