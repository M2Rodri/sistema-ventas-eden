/// Resumen del día de la pantalla principal (GET /api/v1/dashboard/estadisticas).
class DashboardResumen {
  const DashboardResumen({
    required this.totalVentasHoy,
    required this.montoVentasHoy,
    required this.cuotasPendientes,
    required this.montoCuotasPendientes,
    required this.ventasPorEntregar,
  });

  final int totalVentasHoy;
  final double montoVentasHoy;
  final int cuotasPendientes;
  final double montoCuotasPendientes;
  final int ventasPorEntregar;

  bool get sinMovimientoHoy =>
      totalVentasHoy == 0 && cuotasPendientes == 0 && ventasPorEntregar == 0;

  factory DashboardResumen.fromJson(Map<String, dynamic> json) {
    final ventasStats =
        json['ventasStats'] as Map<String, dynamic>? ?? const {};
    final pagosStats = json['pagosStats'] as Map<String, dynamic>? ?? const {};

    return DashboardResumen(
      totalVentasHoy: (ventasStats['totalVentasHoy'] as num?)?.toInt() ?? 0,
      montoVentasHoy:
          (ventasStats['montoVentasHoy'] as num?)?.toDouble() ?? 0.0,
      cuotasPendientes: (pagosStats['cuotasPendientes'] as num?)?.toInt() ?? 0,
      montoCuotasPendientes:
          (pagosStats['montoCuotasPendientes'] as num?)?.toDouble() ?? 0.0,
      ventasPorEntregar: (json['ventasPorEntregar'] as num?)?.toInt() ?? 0,
    );
  }
}
