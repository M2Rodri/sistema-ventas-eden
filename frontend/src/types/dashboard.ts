// types/dashboard.ts

export interface VentasStats {
  totalVentasHoy: number;
  montoVentasHoy: number;
  totalVentasMes: number;
  montoVentasMes: number;
  totalVentasAño: number;
  montoVentasAño: number;
  promedioVentaDiaria: number;
}

/** Ventas de una semana calendario (lunes a domingo): GET /api/v1/dashboard/ventas-semanal. */
export interface VentasSemanal {
  fechaInicio: string;
  fechaFin: string;
  numeroSemana: number;
  esSemanaActual: boolean;
  totalVentas: number;
  montoTotal: number;
}

export interface ProductosStats {
  totalProductos: number;
  productosActivos: number;
  productosSinStock: number;
  productosBajoStock: number;
}

export interface InventarioStats {
  alertasInventario: number;
  valorTotalInventario: number;
  ajustesDelMes: number;
}

export interface ClientesStats {
  totalClientes: number;
  clientesNuevosHoy: number;
  clientesNuevosMes: number;
  clientesActivos: number;
}

export interface ProductoMasVendido {
  idProducto: number;
  nombreProducto: string;
  skuProducto: string;
  cantidadVendida: number;
  montoTotal: number;
}

export interface VentaPorDia {
  fecha: string;
  cantidadVentas: number;
  montoTotal: number;
}

export interface DashboardEstadisticas {
  ventasStats: VentasStats;
  productosStats: ProductosStats;
  inventarioStats: InventarioStats;
  clientesStats: ClientesStats;
  productosMasVendidos: ProductoMasVendido[];
  ventasUltimosDias: VentaPorDia[];
}
