import 'package:flutter/material.dart';

import '../../models/venta.dart';
import '../../theme/app_colors.dart';

/// Corregir una entrega de Entregado a Pendiente (tocar la etiqueta, solo ADMIN).
const bool corregirEntregaActivo = false;

/// Color del badge de cada estado de entrega, igual en el detalle, en la lista de Ventas y en el inicio.
Color colorEstadoEntrega(EstadoEntrega estado) {
  switch (estado) {
    case EstadoEntrega.pendiente:
      return AppColors.acVioleta;
    case EstadoEntrega.entregado:
      return AppColors.verdeSuave;
  }
}
