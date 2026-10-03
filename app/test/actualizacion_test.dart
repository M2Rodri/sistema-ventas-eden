import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:muebleria_eden_app/data/actualizacion_repository.dart';
import 'package:package_info_plus/package_info_plus.dart';

/// La app compara la versión publicada (version.json) con la instalada y solo
/// avisa si la publicada es más nueva. Buscar actualizaciones nunca debe fallar
/// ni molestar: sin internet o con un archivo roto, simplemente no hay aviso.
void main() {
  setUp(() {
    PackageInfo.setMockInitialValues(
      appName: 'Muebleria Eden',
      packageName: 'com.muebleriaeden',
      version: '1.0.0',
      buildNumber: '3',
      buildSignature: '',
    );
  });

  ActualizacionRepository repoCon(http.Response Function(http.Request) respuesta) =>
      ActualizacionRepository(
        cliente: MockClient((pedido) async => respuesta(pedido)),
        urlVersion: 'https://ejemplo.test/version.json',
      );

  String json(int codigo) => jsonEncode(<String, dynamic>{
        'versionCode': codigo,
        'versionName': '1.$codigo.0',
        'apkUrl': 'https://ejemplo.test/app.apk',
        'notas': 'Mejoras',
      });

  group('version.json', () {
    test('se lee con sus datos', () {
      final v = InfoVersion.fromJson(<String, dynamic>{
        'versionCode': 5,
        'versionName': '1.5.0',
        'apkUrl': 'https://x.test/a.apk',
        'sha256': 'ABCDEF',
      });
      expect(v.versionCode, 5);
      expect(v.versionName, '1.5.0');
      expect(v.sha256, 'abcdef');
    });

    test('sin versionCode o sin apkUrl no sirve', () {
      expect(() => InfoVersion.fromJson(<String, dynamic>{'apkUrl': 'x'}), throwsFormatException);
      expect(() => InfoVersion.fromJson(<String, dynamic>{'versionCode': 2}), throwsFormatException);
    });
  });

  group('comparaciones', () {
    test('solo una versión mayor es nueva', () {
      expect(hayVersionNueva(4, 3), isTrue);
      expect(hayVersionNueva(3, 3), isFalse);
      expect(hayVersionNueva(2, 3), isFalse);
    });

    test('la huella del APK tiene que coincidir si se publicó', () {
      final calculada = sha256.convert(utf8.encode('abc'));
      expect(coincideHuella(null, calculada), isTrue);
      expect(coincideHuella('', calculada), isTrue);
      expect(coincideHuella(calculada.toString().toUpperCase(), calculada), isTrue);
      expect(coincideHuella('0000', calculada), isFalse);
    });
  });

  group('buscar', () {
    test('hay una versión más nueva', () async {
      final r = await repoCon((_) => http.Response(json(4), 200)).buscar();
      expect(r.resultado, ResultadoBusqueda.hayNueva);
      expect(r.version?.versionName, '1.4.0');
    });

    test('la publicada es la misma que la instalada: está al día', () async {
      final r = await repoCon((_) => http.Response(json(3), 200)).buscar();
      expect(r.resultado, ResultadoBusqueda.alDia);
    });

    test('la publicada es más vieja: está al día', () async {
      final r = await repoCon((_) => http.Response(json(2), 200)).buscar();
      expect(r.resultado, ResultadoBusqueda.alDia);
    });

    test('si el archivo no existe, no molesta', () async {
      final r = await repoCon((_) => http.Response('no existe', 404)).buscar();
      expect(r.resultado, ResultadoBusqueda.sinConexion);
    });

    test('si el archivo está roto, no molesta', () async {
      final r = await repoCon((_) => http.Response('esto no es json', 200)).buscar();
      expect(r.resultado, ResultadoBusqueda.sinConexion);
    });

    test('sin internet, no molesta', () async {
      final repo = ActualizacionRepository(
        cliente: MockClient((_) async => throw http.ClientException('sin red')),
        urlVersion: 'https://ejemplo.test/version.json',
      );
      expect((await repo.buscar()).resultado, ResultadoBusqueda.sinConexion);
    });
  });
}
