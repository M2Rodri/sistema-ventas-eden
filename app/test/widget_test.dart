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

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
