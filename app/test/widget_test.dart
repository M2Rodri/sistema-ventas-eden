import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:muebleria_eden_app/main.dart';

void main() {
  testWidgets('La app arranca mostrando el indicador de carga inicial', (tester) async {
    // Solo un pump (no pumpAndSettle): la verificación de sesión guardada
    // es async y usa flutter_secure_storage, que no tiene implementación de
    // plataforma en el entorno de test. Alcanza con confirmar que arranca
    // sin explotar y muestra el indicador mientras decide a qué pantalla ir.
    await tester.pumpWidget(const MuebleriaEdenApp());

    // Hay mas de una rueda a la vez: la de _Arranque y la del candado de huella
    // (BloqueoBiometrico), que se dibuja encima con el mismo color y en el mismo
    // lugar, asi que a la vista es una sola. Lo que se comprueba es que haya
    // alguna mientras la app decide a que pantalla ir.
    expect(find.byType(CircularProgressIndicator), findsWidgets);
  });
}
