import 'package:flutter/material.dart';

import '../home/home_screen.dart';
import 'login_screen.dart';

/// Lleva al login sacando todas las pantallas anteriores; al iniciar sesión entra a la pantalla principal.
void irAlLogin(NavigatorState navigator, {String? aviso}) {
  navigator.pushAndRemoveUntil(
    MaterialPageRoute<void>(
      builder: (routeContext) => LoginScreen(
        aviso: aviso,
        onSesionIniciada: (sesion) {
          Navigator.of(routeContext).pushReplacement(
            MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
          );
        },
      ),
    ),
    (route) => false,
  );
}
