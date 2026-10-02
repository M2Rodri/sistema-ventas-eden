import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// Toda ruta de la API que usa la app va con el prefijo /api/v1; una ruta sin versión
/// ya no existe en el servidor (responde 404).
void main() {
  test('ninguna ruta de la app usa /api/ sin la versión /api/v1', () {
    final sinVersion = RegExp(r"""['"]/api/(?!v1/)""");
    final infractores = <String>[];

    for (final archivo in Directory('lib').listSync(recursive: true).whereType<File>()) {
      if (!archivo.path.endsWith('.dart')) continue;
      final lineas = archivo.readAsLinesSync();
      for (var i = 0; i < lineas.length; i++) {
        if (sinVersion.hasMatch(lineas[i])) {
          infractores.add('${archivo.path}:${i + 1}: ${lineas[i].trim()}');
        }
      }
    }

    expect(infractores, isEmpty, reason: infractores.join('\n'));
  });

  test('la app usa las rutas versionadas que existen en el servidor', () {
    final texto = Directory('lib/data')
        .listSync()
        .whereType<File>()
        .where((f) => f.path.endsWith('.dart'))
        .map((f) => f.readAsStringSync())
        .join('\n');

    for (final ruta in <String>[
      '/api/v1/auth/login',
      '/api/v1/inventario/catalogo',
      '/api/v1/clientes',
      '/api/v1/dashboard/estadisticas',
      '/api/v1/dashboard/ventas-semanal',
      '/api/v1/ventas',
      '/api/v1/pagos',
    ]) {
      expect(texto, contains(ruta), reason: 'Falta $ruta');
    }
  });
}
