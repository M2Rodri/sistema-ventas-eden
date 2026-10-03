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

/**
 * Historial de compras de un cliente
 * Para Interfaz P6.3
 *
 * El backend devuelve las ventas completas (el mismo VentaResponse que usa
 * el módulo de ventas), no un resumen aparte. Antes este archivo declaraba
 * un tipo VentaCliente propio con campos que el backend nunca envió
 * (productosResumen, cantidadTotalProductos), y por eso el historial no
 * se podía renderizar.
 */
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

/**
 * Cliente con estadísticas para tabla P6.1
 */
export interface ClienteConEstadisticas extends ClienteResponse {
  numeroCompras: number;
  montoTotalComprado: number;
  ultimaFechaCompra?: string;
}
