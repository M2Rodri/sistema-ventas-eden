import 'package:flutter/material.dart';

import '../data/actualizacion_repository.dart';
import '../theme/app_colors.dart';

/// Busca en silencio si hay una versión nueva y, si la hay, avisa. Se llama al
/// abrir la app: si no hay internet o ya está al día, no muestra nada.
Future<void> revisarActualizacionAlAbrir(
  BuildContext context, {
  ActualizacionRepository? repositorio,
}) async {
  final repo = repositorio ?? ActualizacionRepository();
  final respuesta = await repo.buscar();
  if (!context.mounted) return;
  if (respuesta.resultado == ResultadoBusqueda.hayNueva &&
      respuesta.version != null) {
    await mostrarDialogoActualizacion(context, repo, respuesta.version!);
  }
}

/// Aviso de versión nueva: "Más tarde" lo cierra; "Actualizar" descarga el APK
/// con barra de avance y abre el instalador de Android.
Future<void> mostrarDialogoActualizacion(
  BuildContext context,
  ActualizacionRepository repositorio,
  InfoVersion version,
) {
  return showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (_) =>
        _DialogoActualizacion(repositorio: repositorio, version: version),
  );
}

class _DialogoActualizacion extends StatefulWidget {
  _DialogoActualizacion({required this.repositorio, required this.version});

  final ActualizacionRepository repositorio;
  final InfoVersion version;

  @override
  State<_DialogoActualizacion> createState() => _DialogoActualizacionState();
}

class _DialogoActualizacionState extends State<_DialogoActualizacion> {
  bool _descargando = false;
  double _avance = 0;
  String? _error;

  Future<void> _actualizar() async {
    setState(() {
      _descargando = true;
      _avance = 0;
      _error = null;
    });
    try {
      final apk = await widget.repositorio.descargar(
        widget.version,
        alAvanzar: (avance) {
          if (mounted) setState(() => _avance = avance);
        },
      );
      await widget.repositorio.instalar(apk);
      if (mounted) Navigator.of(context).pop();
    } on ActualizacionException catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    } on Exception {
      if (mounted)
        setState(
          () => _error =
              'No se pudo actualizar. Revisa tu conexión e intenta de nuevo.',
        );
    } finally {
      if (mounted) setState(() => _descargando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final version = widget.version;
    final notas = version.notas?.trim();

    return AlertDialog(
      title: const Text('Hay una versión nueva'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Versión ${version.versionName}',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: AppColors.textoPrincipal,
            ),
          ),
          if (notas != null && notas.isNotEmpty) ...<Widget>[
            const SizedBox(height: 8),
            Text(notas),
          ],
          if (_descargando) ...<Widget>[
            const SizedBox(height: 16),
            LinearProgressIndicator(
              value: _avance > 0 ? _avance : null,
              color: AppColors.verdeOscuro,
            ),
            const SizedBox(height: 6),
            Text(
              _avance > 0
                  ? 'Descargando… ${(_avance * 100).round()}%'
                  : 'Descargando…',
              style: const TextStyle(fontSize: 12),
            ),
          ],
          if (_error != null) ...<Widget>[
            const SizedBox(height: 12),
            Text(
              _error!,
              style: const TextStyle(color: Colors.red, fontSize: 13),
            ),
          ],
          if (!_descargando && _error == null) ...<Widget>[
            const SizedBox(height: 12),
            Text(
              'Se descarga y Android te pide tocar "Instalar". Tus datos y tu sesión se conservan.',
              style: TextStyle(fontSize: 12, color: AppColors.textoSecundario),
            ),
          ],
        ],
      ),
      actions: <Widget>[
        TextButton(
          onPressed: _descargando ? null : () => Navigator.of(context).pop(),
          child: const Text('Más tarde'),
        ),
        FilledButton(
          onPressed: _descargando ? null : _actualizar,
          child: Text(_error != null ? 'Reintentar' : 'Actualizar'),
        ),
      ],
    );
  }
}
