// types/dashboard.ts
//
// Tipos del panel de inicio. Reflejan DashboardResponse del backend.
// Antes la pantalla de inicio mostraba cifras escritas a mano
// ($250,000 en ventas, 1.200 productos, 350 usuarios y una calificación de
// reseñas) mientras este endpoint existía sin usarse.

export interface VentasStats {
  totalVentasHoy: number;
  montoVentasHoy: number;
  totalVentasMes: number;
  montoVentasMes: number;
  totalVentasAño: number;
  montoVentasAño: number;
  promedioVentaDiaria: number;
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

export interface AlertasStats {
  pedidosPendientes: number;
  inventarioBajo: number;
  cuotasVencidas: number;
  resenasPendientes: number;
  totalAlertas: number;
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
  alertasStats: AlertasStats;
  productosMasVendidos: ProductoMasVendido[];
  ventasUltimosDias: VentaPorDia[];
}
