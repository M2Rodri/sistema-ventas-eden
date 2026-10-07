import 'package:flutter/material.dart';

import '../data/tema_service.dart';

/// Colores de la app. Los que dependen del tema (claro u oscuro) son valores que
/// se resuelven al dibujar según [TemaService.modoOscuro]: así toda pantalla,
/// actual o futura, que use estos nombres se ve bien en los dos temas sin
/// tener que preguntar por el tema en cada lugar.
///
/// Reglas para usarlos:
///  - Fondos de tarjetas, hojas, diálogos y campos: [tarjeta] / [fondo].
///  - Textos: [textoPrincipal] y [textoSecundario].
///  - Bordes y separadores: [borde].
///  - Verde de la marca como texto, ícono, borde o botón: [verdeOscuro].
///  - Fondos de etiquetas y avisos (ámbar, verde, violeta...): los `tint*`;
///    y su texto o ícono: los `ac*`.
class AppColors {
  AppColors._();

  static bool get _oscuro => TemaService.modoOscuro.value;

  // ------------------------------- Marca -------------------------------------
  static Color get verdeOscuro =>
      _oscuro ? const Color(0xFF3E8E63) : const Color(0xFF1B4332);
  static Color get verdeSuave =>
      _oscuro ? const Color(0xFF58B07F) : const Color(0xFF2D6A4F);
  static const Color verdeAgua = Color(0xFF00897B);

  // ------------------------------ Superficies --------------------------------
  /// Fondo de las pantallas y de los campos de texto.
  static Color get fondo =>
      _oscuro ? const Color(0xFF121815) : const Color(0xFFF7F7F4);

  /// Fondo de tarjetas, hojas, diálogos y listas (lo que antes era blanco).
  static Color get tarjeta => _oscuro ? const Color(0xFF1E2621) : Colors.white;

  /// Líneas y bordes suaves.
  static Color get borde =>
      _oscuro ? const Color(0xFF34413A) : const Color(0xFFE0E0E0);

  // -------------------------------- Textos -----------------------------------
  static Color get textoPrincipal =>
      _oscuro ? const Color(0xFFE8EEE9) : const Color(0xFF1C241F);
  static Color get textoSecundario =>
      _oscuro ? const Color(0xFFA9B7AF) : const Color(0xFF6B7A70);

  // ------------------------------- Errores -----------------------------------
  static Color get error =>
      _oscuro ? const Color(0xFFFF8A80) : const Color(0xFFB3261E);
  static Color get errorFondo =>
      _oscuro ? const Color(0xFF3A1B18) : const Color(0xFFFDECEA);

  // --------------- Tarjetas del "Resumen de hoy" (gris plomo) ----------------
  static Color get fondoResumen =>
      _oscuro ? const Color(0xFF3A444D) : const Color(0xFF55606B);
  static const Color iconoResumen = Colors.white;
  static const Color valorResumen = Colors.white;
  static const Color etiquetaResumen = Color(0xFFE3E8EC);
  static Color get bordeResumen =>
      _oscuro ? const Color(0xFF55606B) : const Color(0xFF3A444D);

  // -------- Botones de módulos (Ventas y entregas, Catálogo): verde agua -----
  static Color get fondoModulos =>
      _oscuro ? const Color(0xFF2F6F6A) : const Color(0xFF88C9C4);
  static Color get iconoModulos => _oscuro ? Colors.white : Colors.black;
  static Color get textoModulos => _oscuro ? Colors.white : Colors.black;
  static Color get bordeModulos =>
      _oscuro ? const Color(0xFF4FA39B) : const Color(0xFF5FA8A2);

  // --------- Fondos suaves de etiquetas y avisos (claro / oscuro) ------------
  static Color get tintAmbar =>
      _oscuro ? const Color(0xFF3A2F12) : const Color(0xFFFEF3C7);
  static Color get tintAmbarSuave =>
      _oscuro ? const Color(0xFF33290F) : const Color(0xFFFFFBEB);
  static Color get tintVerde =>
      _oscuro ? const Color(0xFF1F3228) : const Color(0xFFE8F4EC);
  static Color get tintVerdeFuerte =>
      _oscuro ? const Color(0xFF1F3A2A) : const Color(0xFFDCFCE7);
  static Color get tintVerdeHoy =>
      _oscuro ? const Color(0xFF24402F) : const Color(0xFFDCEFE2);
  static Color get tintNeutro =>
      _oscuro ? const Color(0xFF1A221E) : const Color(0xFFF8FAF9);
  static Color get tintVioleta =>
      _oscuro ? const Color(0xFF2D2347) : const Color(0xFFF3E8FF);
  static Color get tintIndigo =>
      _oscuro ? const Color(0xFF242A4A) : const Color(0xFFE0E7FF);
  static Color get tintAguamarina =>
      _oscuro ? const Color(0xFF1C3532) : const Color(0xFFE6F4F2);

  // ---------- Texto e íconos que van sobre esos fondos suaves ----------------
  static Color get acAmbar =>
      _oscuro ? const Color(0xFFF59E0B) : const Color(0xFFD97706);
  static Color get acVerde =>
      _oscuro ? const Color(0xFF4ADE80) : const Color(0xFF15803D);
  static Color get acVioleta =>
      _oscuro ? const Color(0xFFA78BFA) : const Color(0xFF7C3AED);
  static Color get acIndigo =>
      _oscuro ? const Color(0xFF818CF8) : const Color(0xFF4338CA);
  static Color get acAguamarina =>
      _oscuro ? const Color(0xFF6FD0C6) : const Color(0xFF2B7A74);
}
