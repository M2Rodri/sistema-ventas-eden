import 'package:flutter/material.dart';

import '../../data/api_exception.dart';
import '../../data/auth_repository.dart';
import '../../models/sesion.dart';
import '../../theme/app_colors.dart';

/// Pantalla de inicio de sesión.
///
/// El "entrar directo si el token guardado es válido" se resuelve en
/// main.dart (ahí se decide qué pantalla mostrar al abrir la app), no acá:
/// esta pantalla es específicamente el formulario para cuando hace falta
/// loguearse.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.onSesionIniciada});

  /// Se llama con la sesión recién creada. Quien use LoginScreen decide a
  /// dónde navegar desde ahí.
  final ValueChanged<Sesion> onSesionIniciada;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usuarioController = TextEditingController();
  final _passwordController = TextEditingController();
  final _authRepository = AuthRepository();

  bool _cargando = false;
  bool _ocultarPassword = true;
  String? _errorMensaje;

  @override
  void dispose() {
    _usuarioController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _iniciarSesion() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _cargando = true;
      _errorMensaje = null;
    });

    try {
      final sesion = await _authRepository.iniciarSesion(
        usuario: _usuarioController.text.trim(),
        password: _passwordController.text,
      );
      if (!mounted) return;
      widget.onSesionIniciada(sesion);
    } on ApiException catch (error) {
      setState(() => _errorMensaje = error.mensaje);
    } catch (error, stackTrace) {
      // Cualquier error que no sea de la API (por ejemplo, uno de
      // navegación al volver de esta pantalla) queda en el log en vez de
      // perderse: el mensaje que ve el usuario no dice nada útil para
      // depurar.
      debugPrint('Error inesperado en login: $error\n$stackTrace');
      setState(() => _errorMensaje = 'Ocurrió un error inesperado. Probá de nuevo.');
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.verdeOscuro,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  const _Encabezado(),
                  const SizedBox(height: 28),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: <Widget>[
                            Text(
                              'Iniciar sesión',
                              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: AppColors.verdeOscuro,
                                  ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Ingresá con tu usuario y contraseña',
                              style: TextStyle(color: AppColors.textoSecundario),
                            ),
                            const SizedBox(height: 24),
                            TextFormField(
                              controller: _usuarioController,
                              enabled: !_cargando,
                              keyboardType: TextInputType.text,
                              textInputAction: TextInputAction.next,
                              autofillHints: const <String>[AutofillHints.username],
                              decoration: const InputDecoration(
                                labelText: 'Usuario',
                                prefixIcon: Icon(Icons.person_outline),
                              ),
                              validator: (valor) {
                                final texto = valor?.trim() ?? '';
                                if (texto.isEmpty) return 'Ingresá tu usuario';
                                return null;
                              },
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _passwordController,
                              enabled: !_cargando,
                              obscureText: _ocultarPassword,
                              textInputAction: TextInputAction.done,
                              autofillHints: const <String>[AutofillHints.password],
                              onFieldSubmitted: (_) => _iniciarSesion(),
                              decoration: InputDecoration(
                                labelText: 'Contraseña',
                                prefixIcon: const Icon(Icons.lock_outline),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _ocultarPassword
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined,
                                  ),
                                  onPressed: () =>
                                      setState(() => _ocultarPassword = !_ocultarPassword),
                                ),
                              ),
                              validator: (valor) {
                                if (valor == null || valor.isEmpty) {
                                  return 'Ingresá tu contraseña';
                                }
                                return null;
                              },
                            ),
                            if (_errorMensaje != null) ...<Widget>[
                              const SizedBox(height: 16),
                              _AvisoError(mensaje: _errorMensaje!),
                            ],
                            const SizedBox(height: 24),
                            FilledButton(
                              onPressed: _cargando ? null : _iniciarSesion,
                              child: _cargando
                                  ? const SizedBox(
                                      width: 22,
                                      height: 22,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2.4,
                                        color: Colors.white,
                                      ),
                                    )
                                  : const Text(
                                      'Ingresar',
                                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                                    ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          width: 76,
          height: 76,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(Icons.bed_outlined, color: AppColors.verdeOscuro, size: 38),
        ),
        const SizedBox(height: 16),
        const Text(
          'Mueblería Edén',
          style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        const Text(
          'Gestión de ventas',
          style: TextStyle(color: Colors.white70, fontSize: 14),
        ),
      ],
    );
  }
}

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
          const Icon(Icons.error_outline, color: AppColors.error, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(mensaje, style: const TextStyle(color: AppColors.error)),
          ),
        ],
      ),
    );
  }
}
