class GananciaProducto {
  const GananciaProducto({
    required this.idVenta,
    required this.fechaVenta,
    required this.idProducto,
    required this.nombreProducto,
    required this.skuProducto,
    required this.cantidad,
    required this.precioUnitario,
    required this.costoUnitario,
    required this.subtotal,
    required this.ganancia,
  });

  final int idVenta;
  final DateTime? fechaVenta;
  final int idProducto;
  final String nombreProducto;
  final String? skuProducto;
  final int cantidad;
  final double precioUnitario;
  final double? costoUnitario;
  final double subtotal;
  final double? ganancia;

  factory GananciaProducto.fromJson(Map<String, dynamic> json) {
    return GananciaProducto(
      idVenta: (json['idVenta'] as num?)?.toInt() ?? 0,
      fechaVenta: json['fechaVenta'] is String
          ? DateTime.tryParse(json['fechaVenta'] as String)
          : null,
      idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
      nombreProducto: json['nombreProducto'] as String? ?? 'Producto',
      skuProducto: json['skuProducto'] as String?,
      cantidad: (json['cantidad'] as num?)?.toInt() ?? 0,
      precioUnitario: (json['precioUnitario'] as num?)?.toDouble() ?? 0,
      costoUnitario: (json['costoUnitario'] as num?)?.toDouble(),
      subtotal: (json['subtotal'] as num?)?.toDouble() ?? 0,
      ganancia: (json['ganancia'] as num?)?.toDouble(),
    );
  }
}
