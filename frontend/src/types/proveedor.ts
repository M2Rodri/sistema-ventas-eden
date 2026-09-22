// types/proveedor.ts

export type EstadoCompra = 'PENDIENTE' | 'RECIBIDA' | 'CANCELADA';

export interface Proveedor {
  id: number;
  nombreEmpresa: string;
  nit: string;
  contacto: string;
  telefono: string;
  direccion: string | null;
  email: string | null;
  notas: string | null;
  activo: boolean;
  fechaRegistro: string;
  fechaActualizacion: string;
}

export interface ProveedorRequest {
  nombreEmpresa: string;
  nit: string;
  contacto: string;
  telefono: string;
  direccion?: string;
  email?: string;
  notas?: string;
  activo: boolean;
}

export interface Compra {
  id: number;
  idProveedor: number;
  nombreProveedor: string;
  nitProveedor: string;
  fechaCompra: string;
  subtotal: number;
  descuento?: number;
  montoTotal: number;
  numeroFactura?: string | null;
  estado: EstadoCompra;
  idUsuario: number | null;
  nombreUsuario: string | null;
  notas: string | null;
  detalles: DetalleCompra[];
  fechaActualizacion: string;
}

export interface DetalleCompra {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface ItemCompraRequest {
  idProducto: number;
  cantidad: number;
  precioUnitario: number;
}

export interface CompraRequest {
  idProveedor: number;
  /** Número de factura del proveedor: respaldo legal del gasto. */
  numeroFactura?: string;
  notas?: string;
  items: ItemCompraRequest[];
}

export interface CompraEstadisticas {
  pendientes: number;
  recibidas: number;
  canceladas: number;
}