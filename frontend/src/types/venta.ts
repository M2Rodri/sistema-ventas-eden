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

// ENTREGADO: el cliente ya recibió el producto. RETIRO nace ENTREGADO; un ADMIN puede corregir un ENTREGADO a PENDIENTE.
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

/** Crear venta: con idCliente (registrado) o con nombreClienteInvitado (mostrador); metodoPago alimenta el primer pago. */
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
  // Estado de entrega al registrar; si falta queda PENDIENTE (RETIRO siempre ENTREGADO).
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
  precioUnitarioOriginal?: number;
  precioUnitario: number;
  subtotal: number;
  costoUnitario?: number | null;
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

  /** Costo total de mercadería (solo ADMIN) */
  costoTotal?: number | null;
  /** Ganancia total de la venta (solo ADMIN) */
  ganancia?: number | null;

  estado: EstadoVenta;

  modalidadEntrega?: ModalidadEntrega;
  estadoEntrega?: EstadoEntrega;
  direccionDestino?: string;
  ciudad?: string;
  transportadora?: string;
  guiaRemision?: string;

  /** Método de pago del listado: el único, o "VARIOS" si hay más de uno (por eso string y no MetodoPago). */
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
  /** Domicilio o transportadora sin entregar, sin acotar a la semana. */
  entregasPendientes: number;
}
