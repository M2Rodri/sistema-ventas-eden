import 'package:flutter/painting.dart';

import 'catalogo_repository.dart';
import 'clientes_repository.dart';
import 'dashboard_repository.dart';
import 'reporte_repository.dart';
import 'ventas_repository.dart';

/// Ancho al que se decodifican las miniaturas de los productos (lista del catálogo y selector de productos).
const int kAnchoMiniatura = 170;

/// Ancho al que se decodifica la foto del detalle de un producto (catálogo y nueva venta).
const int kAnchoDetalle = 400;

/// Pide por detrás los datos de las pantallas para dejarlos en la caché de ApiClient.
class PrecargaDatos {
  PrecargaDatos._();

  static bool _enCurso = false;

  /// Apenas responde el inicio de sesión, empieza a pedir lo que el Inicio va a mostrar (las tarjetas y la campana).
  static void adelantarInicio(String token) {
    DashboardRepository().obtenerResumenDelDia(token).ignore();
    DashboardRepository().obtenerVentasSemanal(token).ignore();
    CatalogoRepository().obtenerBajoMinimo(token).ignore();
  }

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
          _precargarFotos(productos.map((p) => p.imagenUrl), kAnchoMiniatura, 24);
          _precargarFotos(productos.map((p) => p.imagenUrl), kAnchoDetalle, 8);
        },
        () async => await ClientesRepository().obtenerClientes(token),
        () async => await _gananciasDeHoy(token),
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

  /// Pide el mismo rango que abre Ganancias por defecto (hoy), para que esa pantalla ya lo tenga.
  static Future<void> _gananciasDeHoy(String token) async {
    final dia = DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day);
    final fin = dia.add(const Duration(days: 1)).subtract(const Duration(milliseconds: 1));
    final repositorio = ReporteRepository();
    await repositorio.obtenerFechaInicio(token);
    await repositorio.obtenerGananciasPorProducto(token: token, inicio: dia, fin: fin);
  }

  /// Descarga las primeras fotos de los productos al ancho indicado para que ya estén en memoria al abrirlas.
  static void _precargarFotos(Iterable<String?> urls, int ancho, int maximo) {
    var cuantas = 0;
    for (final url in urls) {
      if (url == null || url.isEmpty) continue;
      if (++cuantas > maximo) break;
      final proveedor = ResizeImage.resizeIfNeeded(
        ancho,
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
