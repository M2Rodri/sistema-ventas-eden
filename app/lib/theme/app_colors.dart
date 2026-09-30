import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  static const Color verdeOscuro = Color(0xFF1B4332);
  static const Color verdeSuave = Color(0xFF2D6A4F);
  static const Color verdeAgua = Color(0xFF00897B);
  static const Color fondo = Color(0xFFF7F7F4);

  /// Fondo de las 4 tarjetas del "Resumen de hoy" (Ventas hoy, Ventas
  /// semanales, Por cobrar y Por entregar). Cambiarlo acá cambia las cuatro.
  static const Color fondoResumen = Color(0xFFE8F4EC);

  /// Color del ícono (y del texto "Ver detalle") de esas 4 tarjetas.
  static const Color iconoResumen = verdeOscuro;

  /// Color de la cifra grande de esas 4 tarjetas (el número o el monto).
  static const Color valorResumen = textoPrincipal;

  /// Color del nombre chico de esas 4 tarjetas ("Ventas hoy", "Por cobrar"...).
  static const Color etiquetaResumen = textoSecundario;

  /// Color del borde de esas 4 tarjetas: hoy el verde oscuro al 12 % de
  /// opacidad. Para otro color, reemplazar `verdeOscuro` por el que se quiera.
  static final Color bordeResumen = verdeOscuro.withValues(alpha: 0.12);

  /// Fondo de los 3 botones de módulos (Ventas y entregas, Alertas de stock y
  /// Catálogo). Cambiarlo acá cambia los tres.
  static const Color fondoModulos = Color(0xFFE8F4EC);

  /// Color del ícono de esos 3 botones.
  static const Color iconoModulos = verdeOscuro;

  /// Color del nombre de esos 3 botones.
  static const Color textoModulos = textoPrincipal;

  /// Color del borde de esos 3 botones: hoy el verde oscuro al 12 % de
  /// opacidad. Para otro color, reemplazar `verdeOscuro` por el que se quiera.
  static final Color bordeModulos = verdeOscuro.withValues(alpha: 0.12);

  static const Color textoPrincipal = Color(0xFF1C241F);
  static const Color textoSecundario = Color(0xFF6B7A70);
  static const Color error = Color(0xFFB3261E);
  static const Color errorFondo = Color(0xFFFDECEA);
}
