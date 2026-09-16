import 'package:flutter/material.dart';

import 'data/auth_repository.dart';
import 'models/sesion.dart';
import 'screens/home/home_screen.dart';
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
/// token todavía no venció, entra directo a la pantalla principal (sin
/// pasar por el formulario de login); si no, muestra el login.
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

  void _entrarConSesion(Sesion sesion) {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
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
