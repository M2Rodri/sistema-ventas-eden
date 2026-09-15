// types/reporte.ts

export type TipoReporte = 
  | 'VENTAS'
  | 'PRODUCTOS_MAS_VENDIDOS'
  | 'CLIENTES_FRECUENTES'
  | 'INVENTARIO_VALORIZADO'
  | 'VENTAS_POR_CATEGORIA'
  | 'VENTAS_POR_METODO_PAGO'
  | 'INVENTARIO_STOCK_BAJO'
  | 'PROVEEDORES'
  | 'TRANSPORTADORAS'
  | 'FINANCIERO';

export type FormatoExportacion = 'PDF' | 'EXCEL' | 'CSV';

export interface ParametrosReporte {
  tipoReporte: TipoReporte;
  fechaInicio?: string;
  fechaFin?: string;
  limite?: number;
  formato?: FormatoExportacion;
}

// ============================================
// REPORTE DE VENTAS
// ============================================
export interface VentaDetalleReporte {
  idVenta: number;
  fechaVenta: string;
  nombreCliente: string;
  montoTotal: number;
  metodoPago: string;
  estado: string;
}

export interface ReporteVentas {
  fechaInicio: string;
  fechaFin: string;
  totalVentas: number;
  montoTotalVentas: number;
  ticketPromedio: number;
  ventas: VentaDetalleReporte[];
}

// ============================================
// REPORTE DE PRODUCTOS MÁS VENDIDOS
// ============================================
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

// ============================================
// REPORTE DE CLIENTES FRECUENTES
// ============================================
export interface ClienteReporte {
  idCliente: number;
  nombreCliente: string;
  celular: string;
  correo: string;
  cantidadCompras: number;
  montoTotalCompras: number;
  promedioCompra: number;
}

export interface ReporteClientes {
  limite: number;
  clientes: ClienteReporte[];
}

// ============================================
// REPORTE DE INVENTARIO VALORIZADO
// ============================================
export interface InventarioReporte {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidadDisponible: number;
  precioUnitario: number;
  valorTotal: number;
  ubicacion: string | null;
}

export interface ReporteInventarioValorizado {
  inventarios: InventarioReporte[];
  valorTotal: number;
  totalProductos: number;
}

// ============================================
// REPORTE DE VENTAS POR CATEGORÍA
// ============================================
export interface VentaPorCategoria {
  categoria: string;
  cantidadVendida: number;
  montoTotal: number;
}

// ============================================
// REPORTE DE VENTAS POR MÉTODO DE PAGO
// ============================================
export interface VentaPorMetodoPago {
  metodoPago: string;
  cantidadVentas: number;
  montoTotal: number;
}

// ============================================
// REPORTE DE INVENTARIO STOCK BAJO
// ============================================
export interface ProductoStockBajo {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidadDisponible: number;
  stockMinimo: number;
  diferencia: number;
}

export interface ReporteInventarioStockBajo {
  totalProductos: number;
  productos: ProductoStockBajo[];
}

// ============================================
// REPORTE DE PROVEEDORES
// ============================================
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

// ============================================
// REPORTE DE TRANSPORTADORAS
// ============================================
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

// ============================================
// REPORTE FINANCIERO
// ============================================
export interface ReporteFinanciero {
  fechaInicio: string;
  fechaFin: string;
  ingresosTotales: number;
  gastosTotales: number;
  gananciaNeta: number;
  margenGanancia: number;
  totalVentas: number;
  totalCompras: number;
  totalPagosRecibidos: number;
  detalleIngresos: Array<{ fecha: string; monto: number }>;
  detalleGastos: Array<{ fecha: string; monto: number }>;
}

// ============================================
// CONFIGURACIÓN DE REPORTES
// ============================================
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