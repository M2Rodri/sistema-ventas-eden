// types/pago.ts

export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'QR';
export type EstadoPago = 'PENDIENTE' | 'COMPLETADO' | 'RECHAZADO';

export interface Pago {
  id: number;
  idVenta: number;
  monto: number;
  metodoPago: MetodoPago;
  fechaPago: string; // LocalDateTime
  referencia: string | null;
  estado: EstadoPago;
}

export interface PagoRequest {
  idVenta: number;
  monto: number;
  metodoPago: MetodoPago;
  referencia?: string;
}

export interface PagoEstadisticas {
  completados: number;
  pendientes: number;
  rechazados: number;
}

export interface PagoConVenta extends Pago {
  numeroVenta: string;
  nombreCliente: string;
  celularCliente: string;
  montoTotalVenta: number;
}