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

/// Un producto elegido en el carrito de Nueva venta, con la cantidad y el
/// precio acordado (que puede venir con descuento respecto al de catálogo).
class ItemCarrito {
  const ItemCarrito({
    required this.idProducto,
    required this.nombre,
    required this.skuProducto,
    required this.precioOriginal,
    required this.precioFinal,
    required this.cantidad,
    required this.stockDisponible,
  });

  final int idProducto;
  final String nombre;
  final String? skuProducto;
  final double precioOriginal;
  final double precioFinal;
  final int cantidad;
  final int stockDisponible;

  double get subtotal => precioFinal * cantidad;

  /// % de descuento respecto al precio de catálogo. Se deriva, igual que en
  /// el formulario web: no se tipea directo.
  double get descuentoPorcentaje {
    if (precioOriginal <= 0 || precioFinal >= precioOriginal) return 0;
    return ((precioOriginal - precioFinal) / precioOriginal) * 100;
  }

  ItemCarrito copyWith({double? precioFinal, int? cantidad}) {
    return ItemCarrito(
      idProducto: idProducto,
      nombre: nombre,
      skuProducto: skuProducto,
      precioOriginal: precioOriginal,
      precioFinal: precioFinal ?? this.precioFinal,
      cantidad: cantidad ?? this.cantidad,
      stockDisponible: stockDisponible,
    );
  }
}

/// Lo que manda POST /api/ventas (VentaRequest en el backend). Mismos
/// campos y las mismas reglas de armado que RegistrarVentaModal.tsx.
class NuevaVentaRequest {
  const NuevaVentaRequest({
    this.idCliente,
    this.nombreClienteInvitado,
    this.telefonoClienteInvitado,
    required this.metodoPago,
    required this.items,
    required this.montoPagado,
    required this.modalidadEntrega,
    this.direccionDestino,
    this.ciudad,
    this.transportadora,
    this.guiaRemision,
  });

  final int? idCliente;
  final String? nombreClienteInvitado;
  final String? telefonoClienteInvitado;
  final MetodoPago metodoPago;
  final List<ItemCarrito> items;
  final double montoPagado;
  final ModalidadEntrega modalidadEntrega;
  final String? direccionDestino;
  final String? ciudad;
  final String? transportadora;
  final String? guiaRemision;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      if (idCliente != null) 'idCliente': idCliente,
      if (idCliente == null) 'nombreClienteInvitado': nombreClienteInvitado,
      if (idCliente == null && telefonoClienteInvitado != null && telefonoClienteInvitado!.isNotEmpty)
        'telefonoClienteInvitado': telefonoClienteInvitado,
      'metodoPago': metodoPago.valorApi,
      'items': items
          .map((item) => <String, dynamic>{
                'idProducto': item.idProducto,
                'cantidad': item.cantidad,
                if (item.descuentoPorcentaje > 0) 'precioUnitarioConDescuento': item.precioFinal,
                if (item.descuentoPorcentaje > 0) 'descuentoPorcentaje': item.descuentoPorcentaje,
              })
          .toList(),
      'montoPagado': montoPagado,
      'modalidadEntrega': _modalidadValorApi(modalidadEntrega),
      if (modalidadEntrega != ModalidadEntrega.retiro) 'direccionDestino': direccionDestino,
      if (modalidadEntrega != ModalidadEntrega.retiro) 'ciudad': ciudad,
      if (modalidadEntrega == ModalidadEntrega.transportadora) 'transportadora': transportadora,
      if (modalidadEntrega == ModalidadEntrega.transportadora) 'guiaRemision': guiaRemision,
    };
  }
}

String _modalidadValorApi(ModalidadEntrega modalidad) {
  switch (modalidad) {
    case ModalidadEntrega.retiro:
      return 'RETIRO';
    case ModalidadEntrega.domicilio:
      return 'DOMICILIO';
    case ModalidadEntrega.transportadora:
      return 'TRANSPORTADORA';
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
