import 'package:flutter/material.dart';

import '../../data/api_exception.dart';
import '../../data/auth_repository.dart';
import '../../data/biometria_service.dart';
import '../../data/token_storage.dart';
import '../../theme/app_colors.dart';
import '../login/login_navegacion.dart';

/// Candado sobre toda la app.
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

  /// Al arrancar todavía no se sabe si hay que bloquear.
  bool _verificando = true;
  bool _bloqueada = false;
  bool _autenticando = false;
  DateTime? _salioEn;

  /// "Ana Pérez (ana)": de quién es la sesión que se está desbloqueando.
  String? _usuarioTexto;

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
    await _leerUsuario();
    if (!mounted) return;
    final activa = BiometriaService.activa.value;
    setState(() {
      _verificando = false;
      _bloqueada = activa;
    });
    if (activa) _pedirHuella();
  }

  Future<void> _leerUsuario() async {
    try {
      final sesion = await TokenStorage().leerSesion();
      if (sesion == null) return;
      final usuario = sesion.usuario;
      _usuarioTexto = '${usuario.nombre} ${usuario.apellido} (${usuario.usuario})';
    } catch (_) {
      // sin datos del usuario, la pantalla solo omite esa línea
    }
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

  /// Entra con la contraseña de la sesión guardada.
  Future<String?> _confirmarContrasena(String contrasena) async {
    try {
      await AuthRepository().reautenticar(contrasena);
    } on ApiException catch (error) {
      // El servidor habla de "usuario o contraseña"; acá el usuario ya se sabe.
      return error.tipo == ApiErrorTipo.credencialesInvalidas
          ? 'La contraseña no es correcta.'
          : error.mensaje;
    } catch (_) {
      return 'No se pudo verificar. Revisa tu conexión.';
    }
    if (!mounted) return null;
    setState(() => _bloqueada = false);
    return null;
  }

  /// Último recurso: cierra la sesión y manda al inicio de sesión. Se pierde lo que estuviera sin guardar.
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
            // Overlay propio: este widget está sobre el Navigator y el campo de contraseña lo necesita.
            child: Overlay(
              key: ValueKey<bool>(_verificando),
              initialEntries: <OverlayEntry>[
                OverlayEntry(
                  builder: (_) => _verificando
                      ? const _Cargando()
                      : PantallaDeBloqueo(
                          usuarioTexto: _usuarioTexto,
                          onDesbloquear: _pedirHuella,
                          onContrasena: _confirmarContrasena,
                          onCerrarSesion: _cerrarSesion,
                        ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _Cargando extends StatelessWidget {
  const _Cargando();

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.verdeOscuro,
      child: const Center(
        child: CircularProgressIndicator(color: Colors.white),
      ),
    );
  }
}

/// Lo que se ve mientras la app está bloqueada.
@visibleForTesting
class PantallaDeBloqueo extends StatefulWidget {
  const PantallaDeBloqueo({
    super.key,
    required this.onDesbloquear,
    required this.onContrasena,
    required this.onCerrarSesion,
    this.usuarioTexto,
  });

  final VoidCallback onDesbloquear;

  /// Verifica la contraseña. Devuelve el mensaje de error, o null si salió bien.
  final Future<String?> Function(String contrasena) onContrasena;
  final VoidCallback onCerrarSesion;

  /// De quién es la sesión (por ejemplo "Ana Pérez (ana)"), o null si no se sabe.
  final String? usuarioTexto;

  @override
  State<PantallaDeBloqueo> createState() => _PantallaDeBloqueoState();
}

class _PantallaDeBloqueoState extends State<PantallaDeBloqueo> {
  final _contrasena = TextEditingController();
  bool _pidiendoContrasena = false;
  bool _confirmandoCierre = false;
  bool _enviando = false;
  bool _ocultarContrasena = true;
  String? _error;

  @override
  void dispose() {
    _contrasena.dispose();
    super.dispose();
  }

  Future<void> _enviarContrasena() async {
    final texto = _contrasena.text;
    if (texto.isEmpty) {
      setState(() => _error = 'Escribe tu contraseña.');
      return;
    }
    if (_enviando) return;
    setState(() {
      _enviando = true;
      _error = null;
    });
    final error = await widget.onContrasena(texto);
    if (!mounted) return;
    // Si salió bien, el candado se levanta y esta pantalla desaparece.
    setState(() {
      _enviando = false;
      _error = error;
    });
  }

  void _volver() {
    setState(() {
      _pidiendoContrasena = false;
      _error = null;
      _contrasena.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.verdeOscuro,
      child: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: EdgeInsets.fromLTRB(
              24,
              24,
              24,
              24 + MediaQuery.viewInsetsOf(context).bottom,
            ),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
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
                  if (!_pidiendoContrasena) ...<Widget>[
                    Text(
                      _confirmandoCierre
                          ? 'Si cierras sesión, perderás lo que no hayas guardado.'
                          : 'Usa tu huella para continuar',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.8),
                        fontSize: 14,
                      ),
                    ),
                    const SizedBox(height: 28),
                  ] else
                    const SizedBox(height: 20),
                  if (_confirmandoCierre)
                    ..._confirmacionDeCierre()
                  else if (_pidiendoContrasena)
                    _tarjetaDeContrasena()
                  else
                    ..._opciones(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _opciones() {
    return <Widget>[
      FilledButton.icon(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.tarjeta,
          foregroundColor: AppColors.verdeOscuro,
        ),
        onPressed: widget.onDesbloquear,
        icon: const Icon(Icons.fingerprint_rounded),
        label: const Text('Desbloquear'),
      ),
      const SizedBox(height: 12),
      TextButton(
        style: TextButton.styleFrom(foregroundColor: Colors.white),
        onPressed: () => setState(() => _pidiendoContrasena = true),
        child: const Text('Usar mi contraseña'),
      ),
      TextButton(
        style: TextButton.styleFrom(foregroundColor: Colors.white70),
        onPressed: () => setState(() => _confirmandoCierre = true),
        child: const Text('Cerrar sesión'),
      ),
    ];
  }

  /// La misma tarjeta blanca y los mismos campos que la pantalla de inicio de sesión.
  Widget _tarjetaDeContrasena() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text(
              'Confirma tu contraseña',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: AppColors.verdeOscuro,
              ),
            ),
            if (widget.usuarioTexto != null) ...<Widget>[
              const SizedBox(height: 4),
              Text(
                widget.usuarioTexto!,
                style: TextStyle(color: AppColors.textoSecundario),
              ),
            ],
            const SizedBox(height: 24),
            TextFormField(
              controller: _contrasena,
              enabled: !_enviando,
              obscureText: _ocultarContrasena,
              autofocus: true,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _enviarContrasena(),
              decoration: InputDecoration(
                labelText: 'Contraseña',
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(
                    _ocultarContrasena
                        ? Icons.visibility_outlined
                        : Icons.visibility_off_outlined,
                  ),
                  onPressed: () =>
                      setState(() => _ocultarContrasena = !_ocultarContrasena),
                ),
              ),
            ),
            if (_error != null) ...<Widget>[
              const SizedBox(height: 16),
              _AvisoError(mensaje: _error!),
            ],
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _enviando ? null : _enviarContrasena,
              child: _enviando
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.4,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Entrar',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
            const SizedBox(height: 4),
            TextButton(
              onPressed: _enviando ? null : _volver,
              child: const Text('Volver'),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _confirmacionDeCierre() {
    return <Widget>[
      FilledButton(
        style: FilledButton.styleFrom(
          backgroundColor: const Color(0xFFC62828),
          foregroundColor: Colors.white,
        ),
        onPressed: widget.onCerrarSesion,
        child: const Text('Sí, cerrar sesión'),
      ),
      const SizedBox(height: 4),
      TextButton(
        style: TextButton.styleFrom(foregroundColor: Colors.white),
        onPressed: () => setState(() => _confirmandoCierre = false),
        child: const Text('Cancelar'),
      ),
    ];
  }
}

/// El mismo aviso de error de la pantalla de inicio de sesión.
class _AvisoError extends StatelessWidget {
  const _AvisoError({required this.mensaje});

  final String mensaje;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.errorFondo,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(Icons.error_outline, color: AppColors.error, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(mensaje, style: TextStyle(color: AppColors.error)),
          ),
        ],
      ),
    );
  }
}
