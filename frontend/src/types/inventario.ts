// types/inventario.ts

export type TipoMovimiento =
  | 'ENTRADA'
  | 'SALIDA'
  | 'COMPRA'
  | 'VENTA'
  | 'DEVOLUCION'
  | 'MERMA'
  | 'AJUSTE_INICIAL';
export type EstadoAlerta = 'PENDIENTE' | 'ATENDIDA' | 'ATENDIDA_MANUAL';

export interface Inventario {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  /** Categoría real del producto, provista por el backend. */
  idCategoria: number | null;
  nombreCategoria: string | null;
  descripcion: string | null;
  /** Precio de compra del producto, para valorizar el inventario. */
  precioCompra: number | null;
  cantidadDisponible: number;
  stockMinimo: number;
  bajoStockMinimo: boolean;
  /** El dueño marcó como atendida la alerta de este producto: sigue bajo el mínimo pero no se avisa. */
  alertaAtendida?: boolean;
  fechaActualizacion: string;
}

export interface InventarioRequest {
  idProducto: number;
  cantidadDisponible: number;
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

/** Movimiento de inventario. */
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