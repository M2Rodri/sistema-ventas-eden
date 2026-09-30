/// Ventas de una semana calendario (lunes a domingo), leídas de
/// GET /api/dashboard/ventas-semanal (VentasSemanalResponse en el backend).
///
/// Los nombres siguen a los del backend tal cual. Cuenta solo ventas
/// COMPLETADA, igual que la tarjeta "Ventas hoy".
class VentaPorDia {
  const VentaPorDia({required this.fecha, required this.cantidadVentas, required this.montoTotal});

  final DateTime fecha;
  final int cantidadVentas;
  final double montoTotal;

  factory VentaPorDia.fromJson(Map<String, dynamic> json) {
    return VentaPorDia(
      fecha: DateTime.parse(json['fecha'] as String),
      cantidadVentas: (json['cantidadVentas'] as num?)?.toInt() ?? 0,
      montoTotal: (json['montoTotal'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

class VentasSemanal {
  const VentasSemanal({
    required this.fechaInicio,
    required this.fechaFin,
    required this.numeroSemana,
    required this.esSemanaActual,
    required this.totalVentas,
    required this.montoTotal,
    required this.ventasPorDia,
  });

  final DateTime fechaInicio;
  final DateTime fechaFin;
  final int numeroSemana;
  final bool esSemanaActual;
  final int totalVentas;
  final double montoTotal;
  final List<VentaPorDia> ventasPorDia;

  factory VentasSemanal.fromJson(Map<String, dynamic> json) {
    final porDia = json['ventasPorDia'] as List<dynamic>? ?? const <dynamic>[];
    return VentasSemanal(
      fechaInicio: DateTime.parse(json['fechaInicio'] as String),
      fechaFin: DateTime.parse(json['fechaFin'] as String),
      numeroSemana: (json['numeroSemana'] as num?)?.toInt() ?? 0,
      esSemanaActual: json['esSemanaActual'] as bool? ?? false,
      totalVentas: (json['totalVentas'] as num?)?.toInt() ?? 0,
      montoTotal: (json['montoTotal'] as num?)?.toDouble() ?? 0.0,
      ventasPorDia: porDia.map((d) => VentaPorDia.fromJson(d as Map<String, dynamic>)).toList(),
    );
  }
}
