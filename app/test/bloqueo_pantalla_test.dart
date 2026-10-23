import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:muebleria_eden_app/screens/bloqueo/bloqueo_biometrico.dart';
import 'package:muebleria_eden_app/theme/app_theme.dart';

/// La pantalla del candado: opciones, contraseña sin cerrar sesión y cierre de sesión con confirmación.
void main() {
  late List<String> contrasenasProbadas;
  late int desbloqueos;
  late int cierres;
  late String? respuesta;

  setUp(() {
    contrasenasProbadas = <String>[];
    desbloqueos = 0;
    cierres = 0;
    respuesta = null;
  });

  Future<void> abrir(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(411, 800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.tema,
        home: PantallaDeBloqueo(
          usuarioTexto: 'Ana Pérez (ana)',
          onDesbloquear: () => desbloqueos++,
          onContrasena: (contrasena) async {
            contrasenasProbadas.add(contrasena);
            return respuesta;
          },
          onCerrarSesion: () => cierres++,
        ),
      ),
    );
  }

  testWidgets('al principio ofrece desbloquear, usar la contraseña o cerrar sesión', (tester) async {
    await abrir(tester);

    expect(find.text('Desbloquear'), findsOneWidget);
    expect(find.text('Usar mi contraseña'), findsOneWidget);
    expect(find.text('Cerrar sesión'), findsOneWidget);
    expect(find.byType(TextFormField), findsNothing);
  });

  testWidgets('"Desbloquear" vuelve a pedir la huella', (tester) async {
    await abrir(tester);

    await tester.tap(find.text('Desbloquear'));

    expect(desbloqueos, 1);
  });

  testWidgets('"Usar mi contraseña" muestra el campo y de quién es la sesión', (tester) async {
    await abrir(tester);

    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();

    expect(find.text('Confirma tu contraseña'), findsOneWidget);
    expect(find.text('Ana Pérez (ana)'), findsOneWidget);
    expect(find.byType(TextFormField), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget);
  });

  testWidgets('con la contraseña vacía avisa y no consulta al servidor', (tester) async {
    await abrir(tester);
    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(find.text('Escribe tu contraseña.'), findsOneWidget);
    expect(contrasenasProbadas, isEmpty);
  });

  testWidgets('una contraseña incorrecta muestra el mensaje y deja reintentar', (tester) async {
    respuesta = 'La contraseña no es correcta.';
    await abrir(tester);
    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextFormField), 'mala');
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(contrasenasProbadas, <String>['mala']);
    expect(find.text('La contraseña no es correcta.'), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget); // el botón vuelve a estar disponible
  });

  testWidgets('una contraseña correcta se envía y no queda ningún error', (tester) async {
    await abrir(tester);
    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextFormField), 'clave-buena');
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(contrasenasProbadas, <String>['clave-buena']);
    expect(find.text('La contraseña no es correcta.'), findsNothing);
    expect(cierres, 0, reason: 'entrar con la contraseña no cierra la sesión');
  });

  testWidgets('"Volver" regresa a las opciones y borra lo escrito', (tester) async {
    await abrir(tester);
    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField), 'algo');

    await tester.tap(find.text('Volver'));
    await tester.pumpAndSettle();
    expect(find.text('Desbloquear'), findsOneWidget);

    await tester.tap(find.text('Usar mi contraseña'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(TextFormField, 'algo'), findsNothing);
  });

  testWidgets('cerrar sesión pide confirmación antes de hacerlo', (tester) async {
    await abrir(tester);

    await tester.tap(find.text('Cerrar sesión'));
    await tester.pumpAndSettle();

    expect(find.textContaining('perderás lo que no hayas guardado'), findsOneWidget);
    expect(cierres, 0);

    await tester.tap(find.text('Sí, cerrar sesión'));
    expect(cierres, 1);
  });

  testWidgets('cancelar la confirmación no cierra la sesión', (tester) async {
    await abrir(tester);
    await tester.tap(find.text('Cerrar sesión'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    expect(cierres, 0);
    expect(find.text('Desbloquear'), findsOneWidget);
  });
}
