// types/reporte.ts

export type TipoReporte = 
  | 'VENTAS'
  | 'PRODUCTOS_MAS_VENDIDOS'
  | 'CLIENTES_FRECUENTES'
  | 'INVENTARIO_VALORIZADO'
  | 'VENTAS_POR_CATEGORIA'
  | 'VENTAS_POR_METODO_PAGO'
  | 'VENTAS_POR_PRODUCTO'
  | 'INVENTARIO_STOCK_BAJO'
  | 'PROVEEDORES'
  | 'TRANSPORTADORAS'
  | 'FINANCIERO'
  | 'CUENTAS_POR_COBRAR';

export type FormatoExportacion = 'PDF' | 'EXCEL' | 'CSV';

export interface ParametrosReporte {
  tipoReporte: TipoReporte;
  fechaInicio?: string;
  fechaFin?: string;
  limite?: number;
  formato?: FormatoExportacion;
}

// Reporte de ventas
export interface VentaDetalleReporte {
  idVenta: number;
  fechaVenta: string;
  nombreCliente: string;
  montoTotal: number;
  metodoPago: string;
  estado: string;
  /** Costo total de los productos vendidos; null si no tiene costo asignado. */
  costoTotal?: number | null;
  /** Ganancia de la venta; null si no cuenta o no tiene costo. */
  ganancia: number | null;
}

export interface ReporteVentas {
  fechaInicio: string;
  fechaFin: string;
  totalVentas: number;
  montoTotalVentas: number;
  ticketPromedio: number;
  gananciaVentas: number;
  ventas: VentaDetalleReporte[];
}

// Reporte de productos más vendidos
export interface ProductoReporte {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  categoria: string;
  cantidadVendida: number;
  montoTotal: number;
  stockActual: number;
}

export interface ReporteProductos {
  limite: number;
  productos: ProductoReporte[];
}

// Reporte de clientes frecuentes
export interface ClienteReporte {
  idCliente: number;
  nombreCliente: string;
  telefono: string;
  email: string;
  cantidadCompras: number;
  montoTotalCompras: number;
  promedioCompra: number;
}

export interface ReporteClientes {
  limite: number;
  clientes: ClienteReporte[];
}

// Reporte de inventario valorizado
export interface InventarioReporte {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  categoria?: string;
  cantidadDisponible: number;
  precioCompra: number;
  valorTotal: number;
}

export interface ReporteInventarioValorizado {
  inventarios: InventarioReporte[];
  valorTotal: number;
  totalProductos: number;
}

// Reporte de ventas por categoría
export interface VentaPorCategoria {
  categoria: string;
  cantidadVendida: number;
  montoTotal: number;
}

// Reporte de ventas por método de pago
export interface VentaPorMetodoPago {
  metodoPago: string;
  cantidadVentas: number;
  montoTotal: number;
}

// Reporte de ventas por producto
/** Un producto vendido en una venta: de estas filas salen el resumen por producto y el detalle. */
export interface VentaPorProductoFila {
  idVenta: number;
  fechaVenta: string;
  nombreCliente: string;
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  categoria: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

// Reporte de inventario stock bajo
export interface ProductoStockBajo {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  categoria?: string;
  cantidadDisponible: number;
  stockMinimo: number;
  diferencia: number;
}

export interface ReporteInventarioStockBajo {
  totalProductos: number;
  productos: ProductoStockBajo[];
}

// Reporte de proveedores
export interface ProveedorReporte {
  idProveedor: number;
  nombreProveedor: string;
  nit: string;
  cantidadCompras: number;
  montoTotalCompras: number;
  ultimaCompra: string | null;
  activo: boolean;
}

export interface ReporteProveedores {
  totalProveedores: number;
  proveedoresActivos: number;
  montoTotalCompras: number;
  proveedores: ProveedorReporte[];
}

// Reporte de transportadoras
export interface TransportadoraReporte {
  idTransportadora: number;
  nombreTransportadora: string;
  cantidadEnvios: number;
  enviosEntregados: number;
  enviosPendientes: number;
  tasaEntrega: number;
  activo: boolean;
}

export interface ReporteTransportadoras {
  totalTransportadoras: number;
  transportadorasActivas: number;
  totalEnvios: number;
  transportadoras: TransportadoraReporte[];
}

// Reporte de cuentas por cobrar
export interface VentaPendienteReporte {
  idVenta: number;
  fechaVenta: string;
  nombreCliente: string;
  telefonoCliente: string | null;
  montoTotal: number;
  saldoPendiente: number;
  diasTranscurridos: number;
  fechaLimitePago: string | null;
  fechaLimiteVencida: boolean;
}

export interface ReporteCuentasPorCobrar {
  cantidadVentasPendientes: number;
  totalPorCobrar: number;
  ventas: VentaPendienteReporte[];
}

// Reporte financiero
export interface ReporteFinanciero {
  fechaInicio: string;
  fechaFin: string;
  /** Ganancia real de lo vendido: precio menos costo de cada producto, por la cantidad. */
  gananciaVentas: number;
  /** Flujo de caja del período (ventas contra compras). No es ganancia. */
  ingresosTotales: number;
  gastosTotales: number;
  saldoPeriodo: number;
  detalleIngresos: Array<{ fecha: string; monto: number }>;
  detalleGastos: Array<{ fecha: string; monto: number }>;
}

// Configuración de reportes
export interface ConfiguracionReporte {
  id: TipoReporte;
  titulo: string;
  descripcion: string;
  icono: string;
  color: string;
  requiereFechas: boolean;
  requiereLimite: boolean;
  categorias: string[];
}

// Reportes personalizados y recientes
/** Un reporte personalizado ya generado: guarda lo necesario para volver a abrirlo igual. */
export interface ReporteReciente {
  id: string;
  tipo: TipoReporte;
  titulo: string;
  fechaInicio?: string;
  fechaFin?: string;
  limite?: number;
  /** Criterios aplicados (solo los que tienen valor). */
  criterios: import('@/lib/reporteCriterios').CriteriosReporte;
  /** Cuándo se generó (ISO). */
  generado: string;
}
