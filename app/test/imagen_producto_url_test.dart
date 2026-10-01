import 'package:flutter_test/flutter_test.dart';

import 'package:muebleria_eden_app/config/api_config.dart';
import 'package:muebleria_eden_app/models/producto_catalogo.dart';

Map<String, dynamic> _producto(String urlImagen) => <String, dynamic>{
      'id': 1,
      'sku': 'COL-001',
      'nombre': 'Colchón',
      'imagenes': <Map<String, dynamic>>[
        <String, dynamic>{'urlImagen': urlImagen, 'esPrincipal': true},
      ],
    };

void main() {
  group('URL de la foto del producto', () {
    test('una foto nueva viene con la URL completa de Supabase y se usa tal cual', () {
      const url = 'https://proyecto.supabase.co/storage/v1/object/public/productos/a.png';
      expect(ProductoCatalogo.desdeApi(_producto(url)).imagenUrl, url);
    });

    test('una foto vieja viene relativa al backend y se le antepone la URL base', () {
      final producto = ProductoCatalogo.desdeApi(_producto('/uploads/images/a.png'));
      expect(producto.imagenUrl, '${ApiConfig.baseUrl}/uploads/images/a.png');
    });

    test('sin fotos no hay URL', () {
      final producto = ProductoCatalogo.desdeApi(<String, dynamic>{'id': 1});
      expect(producto.imagenUrl, isNull);
    });
  });
}
