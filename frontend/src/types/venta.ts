// types/venta.ts

export enum EstadoVenta {
  PENDIENTE_PAGO = 'PENDIENTE_PAGO',
  COMPLETADA = 'COMPLETADA',
  CANCELADA = 'CANCELADA'
}

export enum MetodoPago {
  EFECTIVO = 'EFECTIVO',
  TRANSFERENCIA = 'TRANSFERENCIA',
  QR = 'QR'
}

export enum ModalidadEntrega {
  RETIRO = 'RETIRO',
  DOMICILIO = 'DOMICILIO',
  TRANSPORTADORA = 'TRANSPORTADORA'
}

export enum EstadoEntrega {
  PENDIENTE = 'PENDIENTE',
  ENTREGADO = 'ENTREGADO'
}

export interface ItemVentaRequest {
  idProducto: number;
  cantidad: number;
  precioUnitarioConDescuento?: number;
  descuentoPorcentaje?: number;
}

/**
 * Request para crear venta.
 *
 * Dos modos:
 *  1. Cliente registrado: enviar idCliente.
 *  2. Venta de mostrador: enviar nombreClienteInvitado (y opcionalmente el
 *     teléfono). El backend crea un cliente tipo INVITADO con esos datos, en
 *     lugar de guardar el nombre suelto dentro de la venta como antes.
 *
 * El metodoPago viaja acá porque alimenta el primer pago de la venta, no un
 * campo de la venta: una venta admite varios cobros con métodos distintos.
 */
export interface VentaRequest {
  // MODO 1: Cliente registrado
  idCliente?: number;

  // MODO 2: Venta de mostrador
  nombreClienteInvitado?: string;
  telefonoClienteInvitado?: string;

  // Datos comunes
  metodoPago: MetodoPago;
  referenciaPago?: string;
  items: ItemVentaRequest[];

  // Monto efectivamente cobrado. Si no viene, el backend asume pago total.
  montoPagado?: number;

  // Entrega. Si no viene, el backend asume RETIRO.
  modalidadEntrega?: ModalidadEntrega;
  direccionDestino?: string; // obligatorio para DOMICILIO y TRANSPORTADORA
  ciudad?: string; // obligatorio para DOMICILIO y TRANSPORTADORA
  transportadora?: string; // obligatorio para TRANSPORTADORA
  guiaRemision?: string; // obligatorio para TRANSPORTADORA
}

export interface DetalleVenta {
  id: number;
  idProducto: number;
  nombreProducto: string;
  skuProducto?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Pago {
  id: number;
  idVenta?: number;
  monto: number;
  metodoPago: MetodoPago;
  referencia?: string;
  observacion?: string;
  estado: string;
  fechaPago: string;
  urlComprobante?: string | null;
  sinRespaldo?: boolean;
}

export interface Venta {
  id: number;
  idCliente?: number;
  nombreCliente: string;
  telefonoCliente?: string;
  fechaVenta: string;

  // Importes: los cuatro que guarda la tabla, no solo el total
  subtotal: number;
  descuento?: number;
  montoTotal: number;
  saldoPendiente?: number;

  estado: EstadoVenta;
  requiereEnvio?: boolean;

  modalidadEntrega?: ModalidadEntrega;
  estadoEntrega?: EstadoEntrega;
  direccionDestino?: string;
  ciudad?: string;
  transportadora?: string;
  guiaRemision?: string;

  /**
   * Método de pago mostrado en los listados. Ya no es un campo de la venta:
   * el backend lo deriva de los pagos y devuelve el método cuando hay uno
   * solo, o "VARIOS" cuando hay más de uno. Por eso es string y no MetodoPago.
   */
  metodoPago?: string;

  idUsuario: number;
  nombreUsuario: string;
  detalles: DetalleVenta[];
  pagos: Pago[];
  fechaActualizacion: string;
  esClienteRegistrado: boolean;

  /** Al menos un pago QR/transferencia no tiene foto de comprobante todavía. */
  tienePagosSinRespaldo?: boolean;
}

export interface VentaEstadisticas {
  totalVentas: number;
  ventasCompletadas: number;
  ventasPendientes: number;
  ventasCanceladas: number;
  montoTotal: number;
  ventasDelDia: number;
  montoDelDia: number;
}
