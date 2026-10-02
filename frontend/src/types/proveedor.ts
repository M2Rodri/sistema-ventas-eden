// types/proveedor.ts

/** Una compra registrada entra al inventario en el acto (CONFIRMADA); anularla la deja CANCELADA. */
export type EstadoCompra = 'CONFIRMADA' | 'CANCELADA';

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
  /** Una compra puede no tener proveedor. */
  idProveedor: number | null;
  nombreProveedor: string | null;
  nitProveedor: string | null;
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
  /** Opcional: una compra puede registrarse sin proveedor. */
  idProveedor?: number;
  /** Número de factura del proveedor: respaldo legal del gasto. */
  numeroFactura?: string;
  notas?: string;
  items: ItemCompraRequest[];
}