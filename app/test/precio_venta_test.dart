import 'package:flutter_test/flutter_test.dart';
import 'package:muebleria_eden_app/data/api_client.dart';
import 'package:muebleria_eden_app/models/venta.dart';

ItemCarrito _item({required double precioFinal}) => ItemCarrito(
      idProducto: 7,
      nombre: 'Colchón prueba',
      skuProducto: 'COL-001',
      precioOriginal: 1800,
      precioFinal: precioFinal,
      cantidad: 1,
      stockDisponible: 5,
    );

Map<String, dynamic> _primerItem(double precioFinal) {
  final pedido = NuevaVentaRequest(
    nombreClienteInvitado: 'Cliente de prueba',
    metodoPago: MetodoPago.efectivo,
    items: <ItemCarrito>[_item(precioFinal: precioFinal)],
    montoPagado: 0,
    modalidadEntrega: ModalidadEntrega.retiro,
  );
  return (pedido.toJson()['items'] as List<dynamic>).first as Map<String, dynamic>;
}

void main() {
  test('sin precio escrito no se manda precio: vale el de catálogo', () {
    expect(_primerItem(1800).containsKey('precioUnitarioConDescuento'), isFalse);
  });

  test('un precio menor se manda con su descuento', () {
    final item = _primerItem(1620);
    expect(item['precioUnitarioConDescuento'], 1620);
    expect(item['descuentoPorcentaje'], closeTo(10, 0.0001));
  });

  test('un precio mayor se manda igual, para que el servidor lo rechace', () {
    final item = _primerItem(1900);
    expect(item['precioUnitarioConDescuento'], 1900);
    expect(item.containsKey('descuentoPorcentaje'), isFalse);
  });

  test('lee el código de negocio del error de la API', () {
    const cuerpo = '{"error":{"codigo":"PRECIO_EXCEDE_CATALOGO","mensaje":"No se puede vender"}}';
    expect(extraerCodigoError(cuerpo), 'PRECIO_EXCEDE_CATALOGO');
    expect(extraerCodigoError('{"error":"texto viejo"}'), isNull);
    expect(extraerCodigoError('no es json'), isNull);
    expect(extraerCodigoError(''), isNull);
  });
}
