import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:muebleria_eden_app/main.dart';

void main() {
  testWidgets('La app arranca mostrando el indicador de carga inicial', (tester) async {
    // Solo un pump: la sesión guardada es async y flutter_secure_storage no tiene implementación en test.
    await tester.pumpWidget(MuebleriaEdenApp());

    // Hay dos ruedas a la vez (_Arranque y el candado), una encima de la otra, que a la vista es una.
    expect(find.byType(CircularProgressIndicator), findsWidgets);
  });
}
