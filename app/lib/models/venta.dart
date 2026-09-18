// Modelos de venta: cliente, importes, entrega y sus pagos. Salen de
// GET /api/ventas y GET /api/ventas/{id} (VentaResponse en el backend).

enum EstadoVenta { completada, pendientePago, cancelada }

EstadoVenta estadoVentaDesdeApi(String valor) {
  switch (valor) {
    case 'COMPLETADA':
      return EstadoVenta.completada;
    case 'CANCELADA':
      return EstadoVenta.cancelada;
    case 'PENDIENTE_PAGO':
    default:
      return EstadoVenta.pendientePago;
  }
}

enum EstadoEntrega { pendiente, entregado }

EstadoEntrega estadoEntregaDesdeApi(String valor) {
  return valor == 'ENTREGADO' ? EstadoEntrega.entregado : EstadoEntrega.pendiente;
}

enum ModalidadEntrega { retiro, domicilio, transportadora }

ModalidadEntrega modalidadEntregaDesdeApi(String? valor) {
  switch (valor) {
    case 'DOMICILIO':
      return ModalidadEntrega.domicilio;
    case 'TRANSPORTADORA':
      return ModalidadEntrega.transportadora;
    case 'RETIRO':
    default:
      return ModalidadEntrega.retiro;
  }
}

enum MetodoPago { efectivo, transferencia, qr }

extension MetodoPagoApi on MetodoPago {
  /// Valor que espera el backend (MetodoPago en Java).
  String get valorApi {
    switch (this) {
      case MetodoPago.efectivo:
        return 'EFECTIVO';
      case MetodoPago.transferencia:
        return 'TRANSFERENCIA';
      case MetodoPago.qr:
        return 'QR';
    }
  }

  String get etiqueta {
    switch (this) {
      case MetodoPago.efectivo:
        return 'Efectivo';
      case MetodoPago.transferencia:
        return 'Transferencia';
      case MetodoPago.qr:
        return 'QR';
    }
  }
}

MetodoPago? metodoPagoDesdeApi(String? valor) {
  switch (valor) {
    case 'EFECTIVO':
      return MetodoPago.efectivo;
    case 'TRANSFERENCIA':
      return MetodoPago.transferencia;
    case 'QR':
      return MetodoPago.qr;
    default:
      return null;
  }
}

class DetalleVenta {
  const DetalleVenta({
    required this.nombreProducto,
    required this.skuProducto,
    required this.cantidad,
    required this.precioUnitario,
    required this.subtotal,
  });

  final String nombreProducto;
  final String? skuProducto;
  final int cantidad;
  final double precioUnitario;
  final double subtotal;

  factory DetalleVenta.desdeApi(Map<String, dynamic> json) {
    return DetalleVenta(
      nombreProducto: json['nombreProducto'] as String? ?? '',
      skuProducto: json['skuProducto'] as String?,
      cantidad: (json['cantidad'] as num?)?.toInt() ?? 0,
      precioUnitario: (json['precioUnitario'] as num?)?.toDouble() ?? 0.0,
      subtotal: (json['subtotal'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

class Pago {
  const Pago({
    required this.id,
    required this.monto,
    required this.metodoPago,
    required this.fechaPago,
    required this.referencia,
    required this.nombreUsuario,
  });

  final int id;
  final double monto;
  final MetodoPago? metodoPago;
  final DateTime? fechaPago;
  final String? referencia;
  final String? nombreUsuario;

  factory Pago.desdeApi(Map<String, dynamic> json) {
    return Pago(
      id: json['id'] as int,
      monto: (json['monto'] as num?)?.toDouble() ?? 0.0,
      metodoPago: metodoPagoDesdeApi(json['metodoPago'] as String?),
      fechaPago: json['fechaPago'] != null ? DateTime.tryParse(json['fechaPago'] as String) : null,
      referencia: json['referencia'] as String?,
      nombreUsuario: json['nombreUsuario'] as String?,
    );
  }
}

class Venta {
  const Venta({
    required this.id,
    required this.nombreCliente,
    required this.telefonoCliente,
    required this.fechaVenta,
    required this.montoTotal,
    required this.saldoPendiente,
    required this.estado,
    required this.modalidadEntrega,
    required this.estadoEntrega,
    required this.direccionDestino,
    required this.ciudad,
    required this.transportadora,
    required this.guiaRemision,
    required this.detalles,
    required this.pagos,
  });

  final int id;
  final String nombreCliente;
  final String? telefonoCliente;
  final DateTime? fechaVenta;
  final double montoTotal;
  final double saldoPendiente;
  final EstadoVenta estado;
  final ModalidadEntrega modalidadEntrega;
  final EstadoEntrega estadoEntrega;
  final String? direccionDestino;
  final String? ciudad;
  final String? transportadora;
  final String? guiaRemision;
  final List<DetalleVenta> detalles;
  final List<Pago> pagos;

  bool get tieneSaldoPendiente => saldoPendiente > 0;

  factory Venta.desdeApi(Map<String, dynamic> json) {
    final detallesJson = json['detalles'] as List<dynamic>? ?? const <dynamic>[];
    final pagosJson = json['pagos'] as List<dynamic>? ?? const <dynamic>[];

    return Venta(
      id: json['id'] as int,
      nombreCliente: json['nombreCliente'] as String? ?? 'Cliente no especificado',
      telefonoCliente: json['telefonoCliente'] as String?,
      fechaVenta: json['fechaVenta'] != null ? DateTime.tryParse(json['fechaVenta'] as String) : null,
      montoTotal: (json['montoTotal'] as num?)?.toDouble() ?? 0.0,
      saldoPendiente: (json['saldoPendiente'] as num?)?.toDouble() ?? 0.0,
      estado: estadoVentaDesdeApi(json['estado'] as String? ?? 'PENDIENTE_PAGO'),
      modalidadEntrega: modalidadEntregaDesdeApi(json['modalidadEntrega'] as String?),
      estadoEntrega: estadoEntregaDesdeApi(json['estadoEntrega'] as String? ?? 'PENDIENTE'),
      direccionDestino: json['direccionDestino'] as String?,
      ciudad: json['ciudad'] as String?,
      transportadora: json['transportadora'] as String?,
      guiaRemision: json['guiaRemision'] as String?,
      detalles: detallesJson.map((d) => DetalleVenta.desdeApi(d as Map<String, dynamic>)).toList(),
      pagos: pagosJson.map((p) => Pago.desdeApi(p as Map<String, dynamic>)).toList(),
    );
  }
}
