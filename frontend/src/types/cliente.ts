// types/cliente.ts

export interface ClienteRequest {
  nombre: string;
  /** Opcional: una empresa no tiene apellido. */
  apellido?: string;
  nitCi?: string;
  telefono?: string;
  email?: string;
  activo?: boolean;
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  apellido?: string;
  nombreCompleto: string;
  nitCi?: string;
  telefono?: string;
  email?: string;
  activo: boolean;
  fechaRegistro: string;
  fechaActualizacion: string;
}

/** Historial de compras de un cliente Para Interfaz P6.3 */
export interface HistorialComprasResponse {
  cliente: ClienteResponse;
  ventas: VentaHistorial[];
  estadisticas: EstadisticasCompra;
}

export interface VentaHistorial {
  id: number;
  fechaVenta: string;
  subtotal: number;
  descuento?: number;
  montoTotal: number;
  saldoPendiente?: number;
  estado: string;
  /** Derivado de los pagos: el método si hay uno solo, "VARIOS" si hay varios. */
  metodoPago?: string;
  nombreUsuario?: string;
  detalles: DetalleVentaHistorial[];
}

export interface DetalleVentaHistorial {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto?: string;
  cantidad: number;
  precioUnitarioOriginal?: number;
  precioUnitario: number;
  subtotal: number;
}

export interface EstadisticasCompra {
  totalCompras: number;
  montoTotal: number;
  ticketPromedio: number;
  ultimaCompra?: string;
  productoMasComprado?: string;
  cantidadProductoMasComprado?: number;
}

/** Cliente con estadísticas para tabla P6.1 */
export interface ClienteConEstadisticas extends ClienteResponse {
  numeroCompras: number;
  montoTotalComprado: number;
  ultimaFechaCompra?: string;
}
