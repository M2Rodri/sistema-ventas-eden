import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:http/http.dart' as http;
import 'package:open_filex/open_filex.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';

import '../config/api_config.dart';

/// Datos de la última versión publicada (el archivo version.json).
class InfoVersion {
  const InfoVersion({
    required this.versionCode,
    required this.versionName,
    required this.apkUrl,
    this.notas,
    this.sha256,
    this.tamanoBytes,
  });

  /// Número de compilación: es lo que se compara con el de la app instalada.
  final int versionCode;

  /// Lo que se le muestra a la persona ("1.1.0").
  final String versionName;
  final String apkUrl;
  final String? notas;

  /// Huella del APK, para comprobar que se descargó completo y sin cambios.
  final String? sha256;
  final int? tamanoBytes;

  factory InfoVersion.fromJson(Map<String, dynamic> json) {
    final code = json['versionCode'];
    final url = json['apkUrl'];
    if (code is! num || url is! String || url.isEmpty) {
      throw const FormatException('version.json no trae versionCode y apkUrl');
    }
    return InfoVersion(
      versionCode: code.toInt(),
      versionName: (json['versionName'] as String?) ?? code.toString(),
      apkUrl: url,
      notas: json['notas'] as String?,
      sha256: (json['sha256'] as String?)?.toLowerCase(),
      tamanoBytes: (json['tamanoBytes'] as num?)?.toInt(),
    );
  }
}

/// Qué pasó al buscar una actualización.
enum ResultadoBusqueda { hayNueva, alDia, sinConexion }

class RespuestaBusqueda {
  const RespuestaBusqueda(this.resultado, {this.version});
  final ResultadoBusqueda resultado;
  final InfoVersion? version;
}

class ActualizacionException implements Exception {
  const ActualizacionException(this.mensaje);
  final String mensaje;

  @override
  String toString() => mensaje;
}

/// Busca, descarga e instala las versiones nuevas de la app, sin pasar por la
/// tienda: la versión publicada vive en un archivo version.json y el APK al lado.
///
/// Android no deja instalar sin que la persona lo confirme: la app descarga el
/// APK y abre el instalador del sistema, donde se toca "Instalar". Solo se
/// actualiza si el APK nuevo está firmado con la misma llave que el instalado.
class ActualizacionRepository {
  ActualizacionRepository({http.Client? cliente, String? urlVersion})
    : _cliente = cliente ?? http.Client(),
      _urlVersion = urlVersion ?? ApiConfig.actualizacionUrl;

  final http.Client _cliente;
  final String _urlVersion;

  /// Número de compilación de la app instalada.
  Future<int> versionInstalada() async {
    final info = await PackageInfo.fromPlatform();
    return int.tryParse(info.buildNumber) ?? 0;
  }

  /// Compara la versión publicada con la instalada. Si no hay internet o el
  /// archivo no se puede leer, devuelve [ResultadoBusqueda.sinConexion] en vez
  /// de fallar: buscar actualizaciones nunca debe molestar al usuario.
  Future<RespuestaBusqueda> buscar() async {
    try {
      final respuesta = await _cliente
          .get(
            Uri.parse(_urlVersion),
            headers: <String, String>{'Cache-Control': 'no-cache'},
          )
          .timeout(const Duration(seconds: 10));
      if (respuesta.statusCode != 200) {
        return const RespuestaBusqueda(ResultadoBusqueda.sinConexion);
      }
      final publicada = InfoVersion.fromJson(
        jsonDecode(utf8.decode(respuesta.bodyBytes)) as Map<String, dynamic>,
      );
      final instalada = await versionInstalada();
      return hayVersionNueva(publicada.versionCode, instalada)
          ? RespuestaBusqueda(ResultadoBusqueda.hayNueva, version: publicada)
          : const RespuestaBusqueda(ResultadoBusqueda.alDia);
    } on Exception {
      return const RespuestaBusqueda(ResultadoBusqueda.sinConexion);
    }
  }

  /// Descarga el APK a una carpeta temporal, avisando el avance (0 a 1), y
  /// comprueba su huella si version.json la trae. Devuelve el archivo.
  Future<File> descargar(
    InfoVersion version, {
    void Function(double avance)? alAvanzar,
  }) async {
    final carpeta = await getTemporaryDirectory();
    final archivo = File(
      '${carpeta.path}/muebleria-eden-${version.versionName}.apk',
    );
    if (await archivo.exists()) await archivo.delete();

    final pedido = http.Request('GET', Uri.parse(version.apkUrl));
    final respuesta = await _cliente
        .send(pedido)
        .timeout(const Duration(seconds: 30));
    if (respuesta.statusCode != 200) {
      throw const ActualizacionException(
        'No se pudo descargar la actualización. Intenta de nuevo más tarde.',
      );
    }

    final total = respuesta.contentLength ?? version.tamanoBytes ?? 0;
    var recibido = 0;
    final salida = archivo.openWrite();
    try {
      await for (final trozo in respuesta.stream) {
        salida.add(trozo);
        recibido += trozo.length;
        if (total > 0) alAvanzar?.call((recibido / total).clamp(0.0, 1.0));
      }
    } finally {
      await salida.close();
    }

    if (!coincideHuella(
      version.sha256,
      await sha256.bind(archivo.openRead()).first,
    )) {
      await archivo.delete();
      throw const ActualizacionException(
        'La descarga llegó dañada. Intenta de nuevo.',
      );
    }
    return archivo;
  }

  /// Abre el instalador de Android con el APK descargado.
  Future<void> instalar(File apk) async {
    final resultado = await OpenFilex.open(
      apk.path,
      type: 'application/vnd.android.package-archive',
    );
    if (resultado.type != ResultType.done) {
      throw ActualizacionException(
        'No se pudo abrir el instalador: ${resultado.message}',
      );
    }
  }
}

/// La publicada es más nueva solo si su número de compilación es mayor.
bool hayVersionNueva(int publicada, int instalada) => publicada > instalada;

/// Sin huella publicada no hay nada que comparar; con huella, tiene que coincidir.
bool coincideHuella(String? esperada, Digest calculada) =>
    esperada == null ||
    esperada.isEmpty ||
    esperada.toLowerCase() == calculada.toString();
