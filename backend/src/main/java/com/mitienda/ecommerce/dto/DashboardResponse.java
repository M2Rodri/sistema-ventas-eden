package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO para respuesta del dashboard con todas las estadísticas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // Estadísticas de Ventas
    private VentasStats ventasStats;

    // Estadísticas de Pedidos
    private PedidosStats pedidosStats;

    // Estadísticas de Productos
    private ProductosStats productosStats;

    // Estadísticas de Inventario
    private InventarioStats inventarioStats;

    // Estadísticas de Clientes
    private ClientesStats clientesStats;

    // Estadísticas de Pagos/Cuotas
    private PagosStats pagosStats;

    // Alertas y Notificaciones
    private AlertasStats alertasStats;

    // Productos más vendidos
    private List<ProductoMasVendidoDTO> productosMasVendidos;

    // Ventas por día (últimos 7 días)
    private List<VentaPorDiaDTO> ventasUltimosDias;

    /**
     * Ventas con entrega pendiente y modalidad distinta de RETIRO.
     */
    private Long ventasPorEntregar;

    /**
     * Clase interna para estadísticas de ventas
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentasStats {
        private Long totalVentasHoy;
        private BigDecimal montoVentasHoy;
        private Long totalVentasMes;
        private BigDecimal montoVentasMes;
        private Long totalVentasAño;
        private BigDecimal montoVentasAño;
        private BigDecimal promedioVentaDiaria;
    }

    /**
     * Clase interna para estadísticas de pedidos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidosStats {
        private Long pedidosPendientes;
        private Long pedidosConfirmados;
        private Long pedidosEnviados;
        private Long pedidosEntregados;
        private Long pedidosHoy;
    }

    /**
     * Clase interna para estadísticas de productos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductosStats {
        private Long totalProductos;
        private Long productosActivos;
        private Long productosSinStock;
        private Long productosBajoStock;
    }

    /**
     * Clase interna para estadísticas de inventario
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventarioStats {
        private Long alertasInventario;
        private BigDecimal valorTotalInventario;
        private Integer ajustesDelMes;
    }

    /**
     * Clase interna para estadísticas de clientes
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientesStats {
        private Long totalClientes;
        private Long clientesNuevosHoy;
        private Long clientesNuevosMes;
        private Long clientesActivos;
    }

    /**
     * Clase interna para estadísticas de pagos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagosStats {
        private Long cuotasPendientes;
        private Long cuotasVencidas;
        private BigDecimal montoCuotasPendientes;
        private BigDecimal montoCuotasVencidas;
    }

    /**
     * Clase interna para alertas
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertasStats {
        private Long pedidosPendientes;
        private Long inventarioBajo;
        private Long cuotasVencidas;
        private Long resenasPendientes;
        private Long totalAlertas;
    }

    /**
     * DTO para productos más vendidos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoMasVendidoDTO {
        private Long idProducto;
        private String nombreProducto;
        private String skuProducto;
        private Long cantidadVendida;
        private BigDecimal montoTotal;
    }

    /**
     * DTO para ventas por día
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPorDiaDTO {
        private String fecha; // Formato: "2025-01-15"
        private Long cantidadVentas;
        private BigDecimal montoTotal;
    }
}