import 'package:flutter/painting.dart';

import 'catalogo_repository.dart';
import 'clientes_repository.dart';
import 'dashboard_repository.dart';
import 'ventas_repository.dart';

/// Ancho al que se decodifican las miniaturas de los productos (lista del
/// catálogo y selector de productos). Las dos pantallas usan el MISMO valor, así
/// que comparten la imagen ya cargada en memoria.
const int kAnchoMiniatura = 170;

/// Pide por detrás los datos que las pantallas van a necesitar, para que al
/// abrirlas ya estén en la caché de ApiClient y aparezcan al instante.
///
/// Se lanza al abrir el inicio y, solo, unos instantes después de guardar algo
/// (ApiClient vacía la caché al escribir y llama a [precargar] para llenarla de
/// nuevo). Va de a una petición para no competir con la pantalla que se está
/// viendo, y un fallo se ignora: la pantalla pedirá el dato cuando lo necesite.
///
/// Para precargar algo nuevo (una pantalla futura), basta con agregar su
/// lectura a la lista de [precargar].
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

  /// Empieza a descargar las fotos de los productos para que, al abrir el
  /// catálogo, ya estén en memoria y aparezcan sin esperar.
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
