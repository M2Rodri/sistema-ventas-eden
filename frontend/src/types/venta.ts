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

// ENTREGADO significa que el cliente ya recibió el producto, en cualquier
// modalidad. RETIRO nace ENTREGADO; DOMICILIO y TRANSPORTADORA van de PENDIENTE
// a ENTREGADO, y un ADMIN puede corregir un ENTREGADO a PENDIENTE.
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
  ciClienteInvitado?: string;

  // Datos comunes
  metodoPago: MetodoPago;
  referenciaPago?: string;
  items: ItemVentaRequest[];

  // Monto efectivamente cobrado. Si no viene, el backend asume pago total.
  montoPagado?: number;

  // Hasta cuándo se espera el pago del saldo pendiente (yyyy-MM-dd). Opcional.
  fechaLimitePago?: string;
  // Alternativa: "dentro de N días". El servidor calcula la fecha con su reloj.
  plazoDiasPago?: number;

  // Entrega. Si no viene, el backend asume RETIRO.
  modalidadEntrega?: ModalidadEntrega;
  // Estado con el que se registra. Si no viene queda PENDIENTE (RETIRO siempre
  // queda ENTREGADO). ENTREGADO: DOMICILIO o TRANSPORTADORA, cualquier rol.
  estadoEntrega?: EstadoEntrega;
  direccionDestino?: string; // opcional en DOMICILIO y TRANSPORTADORA
  ciudad?: string; // obligatoria solo en TRANSPORTADORA
  transportadora?: string; // opcional, solo TRANSPORTADORA
  guiaRemision?: string; // opcional, solo TRANSPORTADORA
}

/** Datos de entrega que el ADMIN puede completar o corregir después. */
export interface DatosEntrega {
  direccionDestino?: string;
  transportadora?: string;
  guiaRemision?: string;
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
  idUsuario?: number | null;
  nombreUsuario?: string | null;
}

export interface Venta {
  id: number;
  idCliente?: number;
  nombreCliente: string;
  telefonoCliente?: string;
  ciCliente?: string;
  fechaVenta: string;

  // Importes: los cuatro que guarda la tabla, no solo el total
  subtotal: number;
  descuento?: number;
  montoTotal: number;
  saldoPendiente?: number;
  fechaLimitePago?: string | null;
  // La calcula el servidor con su reloj: hay pago pendiente y la fecha ya pasó.
  fechaLimiteVencida?: boolean;

  estado: EstadoVenta;

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
  /** Domicilio/Transportadora sin entregar todavía. No acotado a la semana:
   * una entrega atrasada sigue siendo relevante aunque sea de hace tiempo. */
  entregasPendientes: number;
}
