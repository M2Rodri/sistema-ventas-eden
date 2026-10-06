import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  static const Color verdeOscuro = Color(0xFF1B4332);
  static const Color verdeSuave = Color(0xFF2D6A4F);
  static const Color verdeAgua = Color(0xFF00897B);
  static const Color fondo = Color(0xFFF7F7F4);

  /// Fondo de las 4 tarjetas del "Resumen de hoy" (Ventas hoy, Ventas
  /// semanales, Por cobrar y Por entregar): Naranja nítido y vivo.
  static const Color fondoResumen = Color(0xFFE65100);

  /// Color del ícono de esas 4 tarjetas.
  static const Color iconoResumen = Colors.white;

  /// Color de la cifra grande de esas 4 tarjetas (el número o el monto).
  static const Color valorResumen = Colors.white;

  /// Color del nombre chico de esas 4 tarjetas ("Ventas hoy", "Por cobrar"...).
  static const Color etiquetaResumen = Color(0xFFFFF3E0);

  /// Color del borde de esas 4 tarjetas: naranja nítido.
  static const Color bordeResumen = Color(0xFFBF360C);

  /// Fondo de los botones de módulos (Ventas y entregas, Catálogo): Café nítido y elegante.
  static const Color fondoModulos = Color(0xFF5D4037);

  /// Color del ícono de esos botones.
  static const Color iconoModulos = Colors.white;

  /// Color del nombre de esos botones.
  static const Color textoModulos = Colors.white;

  /// Color del borde de esos botones: café oscuro nítido.
  static const Color bordeModulos = Color(0xFF3E2723);

  static const Color textoPrincipal = Color(0xFF1C241F);
  static const Color textoSecundario = Color(0xFF6B7A70);
  static const Color error = Color(0xFFB3261E);
  static const Color errorFondo = Color(0xFFFDECEA);
}
