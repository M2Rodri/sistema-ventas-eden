// types/inventario.ts

export type TipoMovimiento =
  | 'ENTRADA'
  | 'SALIDA'
  | 'COMPRA'
  | 'VENTA'
  | 'DEVOLUCION'
  | 'MERMA'
  | 'AJUSTE_INICIAL';
export type EstadoAlerta = 'PENDIENTE' | 'ATENDIDA';

export interface Inventario {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  /** Categoría real del producto, provista por el backend. */
  idCategoria: number | null;
  nombreCategoria: string | null;
  /** Costo de referencia del producto, para valorizar el inventario. */
  costoReferencial: number | null;
  cantidadDisponible: number;
  stockMinimo: number;
  ubicacion: string | null;
  bajoStockMinimo: boolean;
  fechaActualizacion: string;
}

export interface InventarioRequest {
  idProducto: number;
  cantidadDisponible: number;
  ubicacion?: string;
}

export interface AlertaInventario {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidadActual: number;
  cantidadMinima: number;
  fechaAlerta: string;
  estado: EstadoAlerta;
}

/**
 * Movimiento de inventario.
 *
 * La entidad se llamaba AjusteInventario, pero la tabla siempre fue
 * 'movimientos_inventario': un movimiento puede ser entrada, salida, compra,
 * venta, devolución, merma o ajuste inicial — el ajuste es solo uno de los
 * siete casos. Se unificó el nombre en las tres capas.
 */
export interface MovimientoInventario {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidadAnterior: number;
  cantidadNueva: number;
  /** Diferencia aplicada: positiva en entradas, negativa en salidas. */
  cantidad: number;
  tipoMovimiento: TipoMovimiento;
  motivo: string;
  observacion?: string;
  idUsuario: number | null;
  nombreUsuario: string | null;
  fecha: string;
}

export interface MovimientoInventarioRequest {
  idProducto: number;
  cantidad: number;
  /** Desde la pantalla solo se cargan ajustes manuales de entrada o salida. */
  tipoMovimiento: 'ENTRADA' | 'SALIDA';
  motivo: string;
}