import 'package:flutter/material.dart';

import 'data/auth_repository.dart';
import 'models/sesion.dart';
import 'screens/login/login_screen.dart';
import 'theme/app_colors.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const MuebleriaEdenApp());
}

class MuebleriaEdenApp extends StatelessWidget {
  const MuebleriaEdenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mueblería Edén',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.tema,
      home: const _Arranque(),
    );
  }
}

/// Decide con qué pantalla abre la app: si hay una sesión guardada y su
/// token todavía no venció, entra directo (sin pasar por el formulario de
/// login); si no, muestra el login.
class _Arranque extends StatefulWidget {
  const _Arranque();

  @override
  State<_Arranque> createState() => _ArranqueState();
}

class _ArranqueState extends State<_Arranque> {
  final _authRepository = AuthRepository();

  @override
  void initState() {
    super.initState();
    _verificarSesion();
  }

  Future<void> _verificarSesion() async {
    final sesion = await _authRepository.sesionValidaGuardada();
    if (!mounted) return;

    if (sesion != null) {
      _entrarConSesion(sesion);
    } else {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => LoginScreen(onSesionIniciada: _entrarConSesion),
        ),
      );
    }
  }

  // TODO(pantalla-principal): reemplazar por HomeScreen en el próximo commit.
  void _entrarConSesion(Sesion sesion) {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(
        builder: (_) => _PantallaProvisoria(sesion: sesion, authRepository: _authRepository),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: AppColors.verdeOscuro,
      body: Center(child: CircularProgressIndicator(color: Colors.white)),
    );
  }
}

/// Marcador de lugar solo para este commit (el de la pantalla de login):
/// confirma que la sesión quedó guardada y que el "entrar directo" funciona.
/// Se reemplaza por la pantalla principal real en el próximo commit.
class _PantallaProvisoria extends StatelessWidget {
  const _PantallaProvisoria({required this.sesion, required this.authRepository});

  final Sesion sesion;
  final AuthRepository authRepository;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mueblería Edén')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              const Icon(Icons.check_circle_outline, color: AppColors.verdeOscuro, size: 56),
              const SizedBox(height: 16),
              Text(
                'Sesión iniciada: ${sesion.usuario.nombreCompleto}',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 4),
              Text(sesion.usuario.role, style: const TextStyle(color: AppColors.textoSecundario)),
              const SizedBox(height: 24),
              OutlinedButton(
                onPressed: () async {
                  await authRepository.cerrarSesion();
                  if (context.mounted) {
                    Navigator.of(context).pushReplacement(
                      MaterialPageRoute<void>(builder: (_) => const _Arranque()),
                    );
                  }
                },
                child: const Text('Cerrar sesión'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
