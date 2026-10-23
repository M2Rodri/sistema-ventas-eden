import 'package:flutter/painting.dart';

import 'catalogo_repository.dart';
import 'clientes_repository.dart';
import 'dashboard_repository.dart';
import 'ventas_repository.dart';

/// Ancho al que se decodifican las miniaturas de los productos (lista del catálogo y selector de productos).
const int kAnchoMiniatura = 170;

/// Pide por detrás los datos de las pantallas para dejarlos en la caché de ApiClient.
class PrecargaDatos {
  PrecargaDatos._();

  static bool _enCurso = false;

  static Future<void> precargar(String token) async {
    if (_enCurso) return;
    _enCurso = true;
    try {
      final lecturas = <Future<void> Function()>[
        () async => await DashboardRepository().obtenerResumenDelDia(token),
        () async => await DashboardRepository().obtenerVentasSemanal(token),
        () async => await CatalogoRepository().obtenerBajoMinimo(token),
        () async => await VentasRepository().obtenerVentas(token),
        () async {
          final productos = await CatalogoRepository().obtenerCatalogo(token);
          _precargarMiniaturas(productos.map((p) => p.imagenUrl));
        },
        () async => await ClientesRepository().obtenerClientes(token),
      ];
      for (final leer in lecturas) {
        try {
          await leer();
        } catch (_) {
          // Solo es una precarga: si falla, la pantalla lo pide cuando haga falta.
        }
      }
    } finally {
      _enCurso = false;
    }
  }

  /// Descarga las fotos de los productos para que el catálogo las tenga en memoria.
  static void _precargarMiniaturas(Iterable<String?> urls) {
    var cuantas = 0;
    for (final url in urls) {
      if (url == null || url.isEmpty) continue;
      if (++cuantas > 24) break;
      final proveedor = ResizeImage.resizeIfNeeded(
        kAnchoMiniatura,
        null,
        NetworkImage(url),
      );
      final flujo = proveedor.resolve(ImageConfiguration.empty);
      late ImageStreamListener oyente;
      oyente = ImageStreamListener(
        (_, _) => flujo.removeListener(oyente),
        onError: (_, _) => flujo.removeListener(oyente),
      );
      flujo.addListener(oyente);
    }
  }
}
