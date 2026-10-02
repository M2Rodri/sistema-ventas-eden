import 'package:flutter_test/flutter_test.dart';
import 'package:muebleria_eden_app/data/api_client.dart';

void main() {
  test('lee error.mensaje del formato único de la API', () {
    const cuerpo =
        '{"error":{"codigo":"VENTA_NO_ENCONTRADA","mensaje":"No existe la venta 5"}}';
    expect(extraerMensajeError(cuerpo), 'No existe la venta 5');
  });

  test('con errores de validación lee el mensaje y no falla por "campos"', () {
    const cuerpo =
        '{"error":{"codigo":"VALIDACION","mensaje":"Hay campos inválidos","campos":{"nombre":"es obligatorio"}}}';
    expect(extraerMensajeError(cuerpo), 'Hay campos inválidos');
  });

  test('sigue leyendo el formato anterior, el de Spring y el texto plano', () {
    expect(extraerMensajeError('{"error":"texto viejo"}'), 'texto viejo');
    expect(extraerMensajeError('{"message":"de spring"}'), 'de spring');
    expect(extraerMensajeError('Credenciales inválidas'), 'Credenciales inválidas');
    expect(extraerMensajeError(''), isNull);
  });
}
