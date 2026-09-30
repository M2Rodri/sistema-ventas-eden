import 'package:flutter_test/flutter_test.dart';
import 'package:muebleria_eden_app/models/venta.dart';

/// Reglas de entrega del lado de la app: la definición única de "por
/// entregar", la etiqueta "Falta completar" y qué datos viajan al backend
/// según la modalidad.
Venta _venta({
  String estado = 'COMPLETADA',
  String modalidad = 'DOMICILIO',
  String estadoEntrega = 'PENDIENTE',
  String? transportadora,
  String? guia,
}) {
  return Venta.desdeApi(<String, dynamic>{
    'id': 1,
    'estado': estado,
    'modalidadEntrega': modalidad,
    'estadoEntrega': estadoEntrega,
    'montoTotal': 100,
    'saldoPendiente': 0,
    'transportadora': transportadora,
    'guiaRemision': guia,
  });
}

NuevaVentaRequest _pedido({
  required ModalidadEntrega modalidad,
  EstadoEntrega? estado,
  String? direccion,
  String? ciudad,
  String? transportadora,
  String? guia,
}) {
  return NuevaVentaRequest(
    nombreClienteInvitado: 'Cliente',
    metodoPago: MetodoPago.efectivo,
    items: const <ItemCarrito>[],
    montoPagado: 100,
    modalidadEntrega: modalidad,
    estadoEntrega: estado,
    direccionDestino: direccion,
    ciudad: ciudad,
    transportadora: transportadora,
    guiaRemision: guia,
  );
}

void main() {
  group('estado de entrega', () {
    test('reconoce los tres valores del backend', () {
      expect(estadoEntregaDesdeApi('PENDIENTE'), EstadoEntrega.pendiente);
      expect(estadoEntregaDesdeApi('DESPACHADO'), EstadoEntrega.despachado);
      expect(estadoEntregaDesdeApi('ENTREGADO'), EstadoEntrega.entregado);
    });

    test('los valores de la API coinciden con los del backend', () {
      expect(EstadoEntrega.pendiente.valorApi, 'PENDIENTE');
      expect(EstadoEntrega.despachado.valorApi, 'DESPACHADO');
      expect(EstadoEntrega.entregado.valorApi, 'ENTREGADO');
    });
  });

  group('por entregar (definición única)', () {
    test('pendiente y despachada cuentan', () {
      expect(_venta(estadoEntrega: 'PENDIENTE').porEntregar, isTrue);
      expect(_venta(estadoEntrega: 'DESPACHADO', modalidad: 'TRANSPORTADORA').porEntregar, isTrue);
    });

    test('entregada no cuenta', () {
      expect(_venta(estadoEntrega: 'ENTREGADO').porEntregar, isFalse);
    });

    test('una venta cancelada no cuenta', () {
      expect(_venta(estado: 'CANCELADA').porEntregar, isFalse);
    });

    test('en tienda no cuenta porque nace entregada', () {
      expect(_venta(modalidad: 'RETIRO', estadoEntrega: 'ENTREGADO').porEntregar, isFalse);
    });
  });

  group('falta completar', () {
    test('transportadora sin nombre o sin guía', () {
      expect(_venta(modalidad: 'TRANSPORTADORA').faltaCompletarEnvio, isTrue);
      expect(_venta(modalidad: 'TRANSPORTADORA', transportadora: 'Flota X').faltaCompletarEnvio, isTrue);
      expect(_venta(modalidad: 'TRANSPORTADORA', guia: 'G-1').faltaCompletarEnvio, isTrue);
    });

    test('transportadora con nombre y guía está completa', () {
      expect(_venta(modalidad: 'TRANSPORTADORA', transportadora: 'Flota X', guia: 'G-1').faltaCompletarEnvio, isFalse);
    });

    test('un espacio en blanco cuenta como vacío', () {
      expect(_venta(modalidad: 'TRANSPORTADORA', transportadora: '  ', guia: 'G-1').faltaCompletarEnvio, isTrue);
    });

    test('domicilio y en tienda no lo necesitan', () {
      expect(_venta(modalidad: 'DOMICILIO').faltaCompletarEnvio, isFalse);
      expect(_venta(modalidad: 'RETIRO', estadoEntrega: 'ENTREGADO').faltaCompletarEnvio, isFalse);
    });

    test('una venta cancelada no avisa', () {
      expect(_venta(estado: 'CANCELADA', modalidad: 'TRANSPORTADORA').faltaCompletarEnvio, isFalse);
    });
  });

  group('lo que viaja al backend al registrar', () {
    test('en tienda no manda datos de entrega ni estado', () {
      final json = _pedido(modalidad: ModalidadEntrega.retiro, estado: EstadoEntrega.pendiente).toJson();
      expect(json['modalidadEntrega'], 'RETIRO');
      expect(json.containsKey('estadoEntrega'), isFalse);
      expect(json.containsKey('direccionDestino'), isFalse);
      expect(json.containsKey('ciudad'), isFalse);
    });

    test('domicilio manda la dirección si la hay y nunca la ciudad', () {
      final conDireccion = _pedido(
        modalidad: ModalidadEntrega.domicilio,
        estado: EstadoEntrega.pendiente,
        direccion: 'Calle 1',
        ciudad: 'Santa Cruz',
      ).toJson();
      expect(conDireccion['direccionDestino'], 'Calle 1');
      expect(conDireccion.containsKey('ciudad'), isFalse);
      expect(conDireccion['estadoEntrega'], 'PENDIENTE');

      final sinDireccion = _pedido(modalidad: ModalidadEntrega.domicilio, direccion: '  ').toJson();
      expect(sinDireccion.containsKey('direccionDestino'), isFalse);
    });

    test('se puede registrar ya entregada', () {
      final json = _pedido(modalidad: ModalidadEntrega.domicilio, estado: EstadoEntrega.entregado).toJson();
      expect(json['estadoEntrega'], 'ENTREGADO');
    });

    test('transportadora manda la ciudad y solo los opcionales que se llenaron', () {
      final json = _pedido(
        modalidad: ModalidadEntrega.transportadora,
        estado: EstadoEntrega.despachado,
        ciudad: 'La Paz',
        transportadora: '',
        guia: 'G-9',
      ).toJson();
      expect(json['ciudad'], 'La Paz');
      expect(json['estadoEntrega'], 'DESPACHADO');
      expect(json.containsKey('transportadora'), isFalse);
      expect(json['guiaRemision'], 'G-9');
    });
  });
}
