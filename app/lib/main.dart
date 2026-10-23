import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'data/api_client.dart';
import 'data/auth_repository.dart';
import 'data/precarga_datos.dart';
import 'data/tema_service.dart';
import 'screens/bloqueo/bloqueo_biometrico.dart';
import 'screens/home/home_screen.dart';
import 'screens/login/login_navegacion.dart';
import 'screens/login/login_screen.dart';
import 'theme/app_colors.dart';
import 'theme/app_theme.dart';

/// Llave del navegador: el candado de huella la necesita para llevar al login desde fuera de cualquier pantalla.
final navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await TemaService().cargar();
  ApiClient.alVencerSesion = _alVencerLaSesion;
  ApiClient.precargar = PrecargaDatos.precargar;
  runApp(MuebleriaEdenApp());
}

/// Ante un 401 borra la sesión y lleva al login con un aviso, sacando las pantallas anteriores.
Future<void> _alVencerLaSesion() async {
  await AuthRepository().cerrarSesion();
  final navigator = navigatorKey.currentState;
  if (navigator != null) {
    irAlLogin(navigator, aviso: 'Tu sesión venció. Iniciá sesión de nuevo.');
  }
}

class MuebleriaEdenApp extends StatelessWidget {
  MuebleriaEdenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: TemaService.modoOscuro,
      builder: (context, esOscuro, _) {
        return MaterialApp(
          title: 'Mueblería Edén',
          debugShowCheckedModeBanner: false,
          locale: const Locale('es', 'ES'),
          localizationsDelegates: GlobalMaterialLocalizations.delegates,
          supportedLocales: const [Locale('es', 'ES')],
          theme: AppTheme.tema,
          darkTheme: AppTheme.temaOscuro,
          themeMode: esOscuro ? ThemeMode.dark : ThemeMode.light,
          navigatorKey: navigatorKey,
          builder: (context, child) => BloqueoBiometrico(
            navigatorKey: navigatorKey,
            child: child ?? const SizedBox.shrink(),
          ),
          home: _Arranque(),
        );
      },
    );
  }
}

/// Decide la pantalla de arranque: la principal si hay sesión vigente, el login si no.
class _Arranque extends StatefulWidget {
  _Arranque();

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
      // Aquí el context de _Arranque es seguro: todavía no se navegó a ningún lado.
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
      );
    } else {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          // Mismo cuidado que en HomeScreen._cerrarSesion.
          builder: (routeContext) => LoginScreen(
            onSesionIniciada: (sesion) {
              Navigator.of(routeContext).pushReplacement(
                MaterialPageRoute<void>(
                  builder: (_) => HomeScreen(sesion: sesion),
                ),
              );
            },
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    // Solo el verde de la marca, igual al de la pantalla de entrada de Android y al fondo del login.
    return Scaffold(backgroundColor: AppColors.verdeOscuro);
  }
}
