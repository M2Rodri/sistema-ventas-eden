import 'package:flutter/material.dart';

import '../../models/venta.dart';
import '../../theme/app_colors.dart';

/// Color del badge de cada estado de entrega, igual en el detalle, en la lista
/// de Ventas y en el inicio.
Color colorEstadoEntrega(EstadoEntrega estado) {
  switch (estado) {
    case EstadoEntrega.pendiente:
      return const Color(0xFF7C3AED);
    case EstadoEntrega.despachado:
      return const Color(0xFFD97706);
    case EstadoEntrega.entregado:
      return AppColors.verdeSuave;
  }
}
