import 'package:flutter/material.dart';

import '../../data/auth_repository.dart';
import '../../data/biometria_service.dart';
import '../../theme/app_colors.dart';
import '../login/login_navegacion.dart';

/// Candado de huella sobre toda la app.
///
/// Si está activado en Ajustes, tapa la app y pide la huella en dos casos:
///   - siempre que la app se abre de cero;
///   - al volver de segundo plano después de más de [_tiempoDeGracia] fuera
///     (el minuto cubre sacar la foto de un comprobante o atender una llamada).
class BloqueoBiometrico extends StatefulWidget {
  BloqueoBiometrico({
    super.key,
    required this.child,
    required this.navigatorKey,
  });

  final Widget child;
  final GlobalKey<NavigatorState> navigatorKey;

  @override
  State<BloqueoBiometrico> createState() => _BloqueoBiometricoState();
}

class _BloqueoBiometricoState extends State<BloqueoBiometrico>
    with WidgetsBindingObserver {
  static const _tiempoDeGracia = Duration(minutes: 1);

  final _biometria = BiometriaService();

  /// Al arrancar todavía no se sabe si hay que bloquear: mientras se lee el
  /// ajuste, la app queda tapada para que no se vea ni un instante.
  bool _verificando = true;
  bool _bloqueada = false;
  bool _autenticando = false;
  DateTime? _salioEn;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _revisarArranque();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  Future<void> _revisarArranque() async {
    await _biometria.cargar();
    if (!mounted) return;
    final activa = BiometriaService.activa.value;
    setState(() {
      _verificando = false;
      _bloqueada = activa;
    });
    if (activa) _pedirHuella();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) {
      _salioEn = DateTime.now();
    } else if (state == AppLifecycleState.resumed) {
      _alVolver();
    }
  }

  void _alVolver() {
    final salio = _salioEn;
    _salioEn = null;
    if (salio == null || _bloqueada || _autenticando) return;
    if (!BiometriaService.activa.value) return;
    if (DateTime.now().difference(salio) < _tiempoDeGracia) return;

    setState(() => _bloqueada = true);
    _pedirHuella();
  }

  Future<void> _pedirHuella() async {
    if (_autenticando) return;
    _autenticando = true;
    final ok = await _biometria.autenticar('Desbloquea la app para continuar');
    _autenticando = false;
    if (!mounted) return;
    if (ok) setState(() => _bloqueada = false);
  }

  /// Salida para cuando la huella no responde (sensor dañado, huellas
  /// borradas del celular): cerrar sesión y entrar con la contraseña.
  Future<void> _cerrarSesion() async {
    await AuthRepository().cerrarSesion();
    if (!mounted) return;
    setState(() => _bloqueada = false);
    final navigator = widget.navigatorKey.currentState;
    if (navigator != null) irAlLogin(navigator);
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        widget.child,
        if (_verificando || _bloqueada)
          Positioned.fill(
            child: Material(
              color: AppColors.verdeOscuro,
              child: SafeArea(
                child: Center(
                  child: _verificando
                      ? const CircularProgressIndicator(color: Colors.white)
                      : Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 32),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              const Icon(
                                Icons.lock_outline_rounded,
                                size: 56,
                                color: Colors.white,
                              ),
                              const SizedBox(height: 16),
                              const Text(
                                'Mueblería Edén',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 22,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'Usa tu huella para continuar',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.8),
                                  fontSize: 14,
                                ),
                              ),
                              const SizedBox(height: 28),
                              FilledButton.icon(
                                style: FilledButton.styleFrom(
                                  backgroundColor: AppColors.tarjeta,
                                  foregroundColor: AppColors.verdeOscuro,
                                ),
                                onPressed: _pedirHuella,
                                icon: const Icon(Icons.fingerprint_rounded),
                                label: const Text('Desbloquear'),
                              ),
                              const SizedBox(height: 12),
                              TextButton(
                                style: TextButton.styleFrom(
                                  foregroundColor: Colors.white70,
                                ),
                                onPressed: _cerrarSesion,
                                child: const Text('Cerrar sesión'),
                              ),
                            ],
                          ),
                        ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
