import 'package:flutter/material.dart';

import 'data/auth_repository.dart';
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
      // Acá sí es seguro usar el context de _Arranque: todavía no se navegó
      // a ningún lado, esta pantalla sigue siendo la actual.
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
      );
    } else {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          // Mismo cuidado que en HomeScreen._cerrarSesion: el login es
          // async, así que para cuando el usuario complete el formulario,
          // pushReplacement ya sacó a _Arranque del árbol y su context dejó
          // de servir. Se usa el context de esta ruta (el de LoginScreen),
          // que sigue vivo en ese momento.
          builder: (routeContext) => LoginScreen(
            onSesionIniciada: (sesion) {
              Navigator.of(routeContext).pushReplacement(
                MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
              );
            },
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: AppColors.verdeOscuro,
      body: Center(child: CircularProgressIndicator(color: Colors.white)),
    );
  }
}
