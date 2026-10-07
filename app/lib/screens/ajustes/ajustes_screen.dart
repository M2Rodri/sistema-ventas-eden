import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../data/actualizacion_repository.dart';
import '../../data/biometria_service.dart';
import '../../data/tema_service.dart';
import '../../models/usuario.dart';
import '../../theme/app_colors.dart';
import '../../widgets/dialogo_actualizacion.dart';

/// Ajustes: la cuenta, la seguridad y, al final, cerrar sesión.
class AjustesScreen extends StatefulWidget {
  AjustesScreen({
    super.key,
    required this.usuario,
    required this.onCerrarSesion,
  });

  final Usuario usuario;
  final VoidCallback onCerrarSesion;

  @override
  State<AjustesScreen> createState() => _AjustesScreenState();
}

class _AjustesScreenState extends State<AjustesScreen> {
  final _biometria = BiometriaService();
  final _temaService = TemaService();
  final _actualizacion = ActualizacionRepository();

  bool _disponible = false;
  bool _cargando = true;
  bool _buscandoActualizacion = false;
  String _versionInstalada = '';

  @override
  void initState() {
    super.initState();
    _cargar();
    _cargarVersion();
  }

  Future<void> _cargarVersion() async {
    final info = await PackageInfo.fromPlatform();
    if (!mounted) return;
    setState(() => _versionInstalada = info.version);
  }

  /// Busca a pedido de la persona y, a diferencia de la revisión automática, le
  /// cuenta siempre qué pasó: hay una nueva, ya está al día o no hay conexión.
  Future<void> _buscarActualizacion() async {
    setState(() => _buscandoActualizacion = true);
    final respuesta = await _actualizacion.buscar();
    if (!mounted) return;
    setState(() => _buscandoActualizacion = false);

    if (respuesta.resultado == ResultadoBusqueda.hayNueva &&
        respuesta.version != null) {
      await mostrarDialogoActualizacion(
        context,
        _actualizacion,
        respuesta.version!,
      );
      return;
    }
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            respuesta.resultado == ResultadoBusqueda.alDia
                ? 'Ya tienes la última versión.'
                : 'No se pudo buscar actualizaciones. Revisa tu conexión.',
          ),
        ),
      );
  }

  Future<void> _cargar() async {
    await _biometria.cargar();
    final disponible = await _biometria.disponible();
    if (!mounted) return;
    setState(() {
      _disponible = disponible;
      _cargando = false;
    });
  }

  /// Activar y desactivar piden la huella: así, quien tenga el celular
  /// desbloqueado no puede apagar el candado sin ser el dueño.
  Future<void> _cambiar(bool activar) async {
    final ok = await _biometria.autenticar(
      activar
          ? 'Confirma tu huella para activar el candado'
          : 'Confirma tu huella para desactivarlo',
    );
    if (!mounted) return;

    if (!ok) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(content: Text('No se pudo verificar la huella.')),
        );
      return;
    }

    if (activar) {
      await _biometria.activar();
    } else {
      await _biometria.desactivar();
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = widget.usuario;
    final esOscuro = Theme.of(context).brightness == Brightness.dark;
    final colorTextoPrincipal = esOscuro
        ? Colors.white
        : AppColors.textoPrincipal;
    final colorTextoSecundario = esOscuro
        ? Colors.white70
        : AppColors.textoSecundario;
    final colorIcono = esOscuro
        ? const Color(0xFF8FD1AC)
        : AppColors.verdeOscuro;

    return Scaffold(
      appBar: AppBar(title: const Text('Ajustes')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 32),
        children: <Widget>[
          _TituloSeccion('CUENTA'),
          _Bloque(
            child: ListTile(
              leading: Icon(Icons.person_outline_rounded, color: colorIcono),
              title: Text(
                usuario.nombreCompleto,
                style: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: colorTextoPrincipal,
                ),
              ),
              subtitle: Text(
                usuario.role == 'ADMIN' ? 'Administrador' : 'Empleado',
                style: TextStyle(color: colorTextoSecundario),
              ),
            ),
          ),
          const SizedBox(height: 24),
          _TituloSeccion('SEGURIDAD'),
          _Bloque(
            child: ValueListenableBuilder<bool>(
              valueListenable: BiometriaService.activa,
              builder: (context, activa, _) {
                return ListTile(
                  leading: Icon(Icons.fingerprint_rounded, color: colorIcono),
                  title: Text(
                    'Entrar con huella digital',
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: colorTextoPrincipal,
                    ),
                  ),
                  subtitle: Text(
                    _cargando || _disponible
                        ? 'Pide tu huella al abrir la app y al volver después de 1 minuto.'
                        : 'Este celular no tiene huella o rostro registrados.',
                    style: TextStyle(color: colorTextoSecundario),
                  ),
                  trailing: Switch(
                    value: activa,
                    activeThumbColor: AppColors.verdeOscuro,
                    activeTrackColor: AppColors.verdeOscuro.withValues(
                      alpha: 0.35,
                    ),
                    onChanged: _cargando || (!_disponible && !activa)
                        ? null
                        : _cambiar,
                  ),
                  onTap: _cargando || (!_disponible && !activa)
                      ? null
                      : () => _cambiar(!activa),
                );
              },
            ),
          ),
          const SizedBox(height: 24),
          _TituloSeccion('APARIENCIA'),
          _Bloque(
            child: ValueListenableBuilder<bool>(
              valueListenable: TemaService.modoOscuro,
              builder: (context, oscuro, _) {
                return ListTile(
                  leading: Icon(
                    oscuro
                        ? Icons.dark_mode_outlined
                        : Icons.light_mode_outlined,
                    color: colorIcono,
                  ),
                  title: Text(
                    oscuro ? 'Tema oscuro' : 'Tema claro',
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: colorTextoPrincipal,
                    ),
                  ),
                  subtitle: Text(
                    'Cambia entre claro y oscuro',
                    style: TextStyle(color: colorTextoSecundario),
                  ),
                  trailing: Switch(
                    value: oscuro,
                    activeThumbColor: AppColors.verdeOscuro,
                    activeTrackColor: AppColors.verdeOscuro.withValues(
                      alpha: 0.35,
                    ),
                    onChanged: (valor) => _temaService.alternar(valor),
                  ),
                  onTap: () => _temaService.alternar(!oscuro),
                );
              },
            ),
          ),
          const SizedBox(height: 24),
          _TituloSeccion('ACTUALIZACIONES'),
          _Bloque(
            child: ListTile(
              leading: Icon(Icons.system_update_alt_rounded, color: colorIcono),
              title: Text(
                'Buscar actualización',
                style: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: colorTextoPrincipal,
                ),
              ),
              subtitle: Text(
                _versionInstalada.isEmpty
                    ? 'Versión de la app'
                    : 'Versión instalada: $_versionInstalada',
                style: TextStyle(color: colorTextoSecundario),
              ),
              trailing: _buscandoActualizacion
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2.5),
                    )
                  : Icon(
                      Icons.chevron_right_rounded,
                      color: colorTextoSecundario,
                    ),
              onTap: _buscandoActualizacion ? null : _buscarActualizacion,
            ),
          ),
          const SizedBox(height: 32),
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.error,
              side: BorderSide(color: AppColors.error.withValues(alpha: 0.4)),
              padding: const EdgeInsets.symmetric(vertical: 14),
            ),
            onPressed: widget.onCerrarSesion,
            icon: const Icon(Icons.logout_rounded),
            label: const Text('Cerrar sesión'),
          ),
        ],
      ),
    );
  }
}

class _TituloSeccion extends StatelessWidget {
  _TituloSeccion(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) {
    final esOscuro = Theme.of(context).brightness == Brightness.dark;
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 0, 4, 8),
      child: Text(
        texto,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.8,
          color: esOscuro ? const Color(0xFF8FA89B) : AppColors.textoSecundario,
        ),
      ),
    );
  }
}

class _Bloque extends StatelessWidget {
  _Bloque({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final esOscuro = Theme.of(context).brightness == Brightness.dark;
    return Material(
      color: esOscuro ? const Color(0xFF1E2822) : Colors.white,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: esOscuro
              ? Colors.white.withValues(alpha: 0.08)
              : AppColors.verdeOscuro.withValues(alpha: 0.12),
        ),
      ),
      child: child,
    );
  }
}
