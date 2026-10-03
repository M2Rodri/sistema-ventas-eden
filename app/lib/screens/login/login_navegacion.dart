import 'package:flutter/material.dart';

import '../home/home_screen.dart';
import 'login_screen.dart';

/// Lleva al login sacando todas las pantallas anteriores; al iniciar sesión
/// entra a la pantalla principal.
///
/// Importante: dentro del builder no se usa el context de quien llama.
/// pushAndRemoveUntil saca esa pantalla del árbol, así que ese context queda
/// inválido para cuando el usuario inicia sesión (es async, tarda). Se usa el
/// "routeContext" del propio builder, que es el de la pantalla de login recién
/// creada y sigue vivo en ese momento.
///
/// Con [aviso], el login lo muestra arriba del formulario (por ejemplo, cuando
/// la sesión venció).
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
